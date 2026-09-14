package com.streamx.hub.rag.retrieval;

import com.streamx.hub.rag.Configuration;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.jspecify.annotations.NonNull;

/**
 * Two-stage query transformer applied before vector retrieval:
 * <p>
 * Stage 1 — Contextualization: if the query contains vague pronouns (it, its, this, that, the
 * same…) and there is conversation history, an LLM rewrites the query to be self-contained.
 * Example: "What are its dimensions?" → "dimensions of Nordic Sofa"
 * <p>
 * Stage 2 — Translation: the (possibly rewritten) query is translated to English so it matches the
 * English-only embedding store.
 * <p>
 * Only the retrieval query is transformed — GPT-4o still receives the original user message and
 * responds in the user's language.
 */
@ApplicationScoped
public class TranslatingQueryTransformer implements QueryTransformer {

  private static final Logger LOG = Logger.getLogger(TranslatingQueryTransformer.class);

  /**
   * Words that signal the query may reference something from context.
   */
  private static final Set<String> VAGUE_WORDS = Set.of(
      "it", "its", "this", "that", "they", "them", "those",
      "the same", "the one", "the previous", "previous", "that one",
      "that product", "this product", "these", "those ones"
  );

  /**
   * Pre-compiled word-boundary patterns for each vague word. Compiled once at class load time
   * instead of per-request to avoid allocating a Pattern object on every chat query.
   */
  private static final List<Pattern> VAGUE_PATTERNS = VAGUE_WORDS.stream()
      .map(w -> Pattern.compile("\\b" + Pattern.quote(w) + "\\b"))
      .collect(Collectors.toUnmodifiableList());

  /**
   * Max conversation turns to include in the contextualization prompt.
   */
  private static final int MAX_HISTORY_MESSAGES = 6;

  @Inject
  TranslationAiService translationService;

  @Inject
  ContextualizingAiService contextualizingService;

  @Inject
  Configuration config;

  @Override
  public Collection<Query> transform(Query query) {
    String queryText = query.text();
    if (queryText == null || queryText.isBlank()) {
      return List.of(query);
    }

    // --- Stage 1: Contextualize if the query is vague ---
    if (isVague(queryText)) {
      queryText = contextualize(query, queryText);
    }
    // --- Stage 2: Translate to English for retrieval ---
    return translate(query, queryText);
  }

  private @NonNull List<Query> translate(Query query, String queryText) {
    try {
      String translated = translationService.translate(config.translationPrompt(), queryText)
          .trim();
      if (!translated.isBlank() && !translated.equalsIgnoreCase(queryText)) {
        LOG.debugf("Translated: [%s] → [%s]", queryText, translated);
      }
      return List.of(Query.from(translated, query.metadata()));
    } catch (Exception e) {
      LOG.warnf("Translation failed, using query as-is: %s", e.getMessage());
      return List.of(Query.from(queryText, query.metadata()));
    }
  }

  private @NonNull String contextualize(Query query, String queryText) {
    List<ChatMessage> history = query.metadata().chatMemory();
    if (history != null && !history.isEmpty()) {
      String contextualized = contextualizingService.contextualize(config.contextualizationPrompt(),
              formatHistory(history),
              queryText)
          .trim();
      if (!contextualized.isBlank() && !contextualized.equalsIgnoreCase(queryText)) {
        LOG.debugf("Contextualized: [%s] → [%s]", queryText, contextualized);
        queryText = contextualized;
      }
    }
    return queryText;
  }

  private boolean isVague(String text) {
    return VAGUE_PATTERNS.stream()
        .anyMatch(p -> p.matcher(text.toLowerCase()).find());
  }

  private String formatHistory(List<ChatMessage> history) {
    int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
    return history.subList(fromIndex, history.size()).stream()
        .map(msg -> msg.type().name() + ": " + messageText(msg))
        .collect(Collectors.joining("\n"));
  }

  private String messageText(ChatMessage msg) {
    try {
      return msg.toString();
    } catch (Exception e) {
      return "[message]";
    }
  }
}
