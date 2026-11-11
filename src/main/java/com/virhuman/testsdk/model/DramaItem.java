package com.virhuman.testsdk.model;

public class DramaItem {
    private int id;
    private String title;
    private String artist;
    private String description;
    private String coverUrl;
    private String audioUrl;
    // 是否已收藏（客户端本地标记，后端如有同名字段会自动映射）
    private boolean favorite;

    public DramaItem(int id, String title, String artist, String description, String coverUrl, String audioUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.description = description;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}


