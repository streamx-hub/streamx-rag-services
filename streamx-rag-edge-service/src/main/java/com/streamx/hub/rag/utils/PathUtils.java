package com.streamx.hub.rag.utils;

import org.apache.commons.io.FilenameUtils;

public class PathUtils {

  private static final String INDEX_HTML_SUFFIX = "index.html";

  private PathUtils() {
    // no instance
  }

  public static String getPathFrom(String subject, boolean isHtmlResource,
      String defaultNamespace) {
    String namespace = com.streamx.hub.rag.utils.CloudEventUtils.getSubjectNamespace(subject)
        .orElse(defaultNamespace);
    String path = namespace + "/" + CloudEventUtils.getSubjectWithoutNamespace(subject);
    return isHtmlResource ? computeHtmlResourcePath(path) : path;
  }

  public static String computeHtmlResourcePath(String path) {
    if (path.endsWith("/")) {
      return path + INDEX_HTML_SUFFIX;
    }
    if (FilenameUtils.getExtension(path).isEmpty()) {
      return path + "/" + INDEX_HTML_SUFFIX;
    }
    return path;
  }
}
