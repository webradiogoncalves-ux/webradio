package com.webtvfull.app;

public class ContentItem {
    public final String title;
    public final String provider;
    public final String url;
    public final String mime;

    public ContentItem(String title, String provider, String url, String mime) {
        this.title = title; this.provider = provider; this.url = url; this.mime = mime;
    }
}
