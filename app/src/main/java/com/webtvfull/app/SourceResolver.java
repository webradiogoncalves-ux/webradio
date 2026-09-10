package com.webtvfull.app;

import java.util.List;

public interface SourceResolver {
    String name();
    List<ContentItem> items();
}
