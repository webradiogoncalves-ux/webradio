package com.webtv.full;

public class Content {
    public enum PlayerMode { AUTO, EXOPLAYER, MEDIAPLAYER }

    public final String name;
    public final String category;
    public final String type;
    public final String url;
    public final PlayerMode playerMode;

    public Content(String name, String category, String type, String url) {
        this(name, category, type, url, PlayerMode.AUTO);
    }

    public Content(String name, String category, String type, String url, PlayerMode playerMode) {
        this.name = name;
        this.category = category;
        this.type = type;
        this.url = url;
        this.playerMode = playerMode == null ? PlayerMode.AUTO : playerMode;
    }
}
