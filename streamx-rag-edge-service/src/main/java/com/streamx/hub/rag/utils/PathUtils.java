package com.streamx.hub.rag.utils;

import static com.streamx.hub.rag.utils.CloudEventUtils.getSubjectNamespace;

public class PathUtils {

  private PathUtils() {
    // no instance
  }

  public static String getPathFrom(String subject, String defaultNamespace) {
    String namespace = getSubjectNamespace(subject)
        .orElse(defaultNamespace);
    return namespace + "/" + CloudEventUtils.getSubjectWithoutNamespace(subject);
  }
}
