package com.webtvfull.app;

import java.util.*;

public final class Providers {
    private Providers() {}

    public static List<ContentItem> canais() {
        List<ContentItem> x = new ArrayList<>();
        x.add(new ContentItem(
            "Teste PlayTV — MPEG-TS",
            "PlayTV",
            "http://79.127.238.228:14551/",
            "video/mp2t"));
        return x;
    }

    public static List<ContentItem> novelas() {
        return Collections.singletonList(new ContentItem(
            "Novecalizando — integração pendente",
            "Novecalizando", "", ""));
    }

    public static List<ContentItem> jogos() {
        return Collections.singletonList(new ContentItem(
            "GetFut — integração pendente",
            "GetFut", "", ""));
    }
}
