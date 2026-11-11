package com.virhuman.testsdk.model;

public class NewsItem {
    private int id;             // 文章ID
    private String title;       // 标题
    private String summary;     // 简介
    private String category;    // 分类：健康养生、国家政策、社区活动等
    private String publishTime; // 发布时间文案，如 “2小时前”
    private int bgColorResId;   // 用于卡片左侧色块背景
    private String audioUrl;    // 资讯语音地址
    private String coverUrl;    // 资讯封面图片地址
    // 是否已收藏（客户端本地标记，后端如有同名字段会自动映射）
    private boolean favorite;

    public NewsItem(String title, String summary, String category, String publishTime, int bgColorResId) {
        this(0, title, summary, category, publishTime, bgColorResId, null);
    }

    public NewsItem(String title, String summary, String category, String publishTime, int bgColorResId, String audioUrl) {
        this(0, title, summary, category, publishTime, bgColorResId, audioUrl);
    }

    public NewsItem(int id, String title, String summary, String category, String publishTime, int bgColorResId, String audioUrl) {
        this(id, title, summary, category, publishTime, bgColorResId, audioUrl, null);
    }

    public NewsItem(int id, String title, String summary, String category, String publishTime, int bgColorResId, String audioUrl, String coverUrl) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.category = category;
        this.publishTime = publishTime;
        this.bgColorResId = bgColorResId;
        this.audioUrl = audioUrl;
        this.coverUrl = coverUrl;
        this.favorite = false;
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


    public String getSummary() {
        return summary;
    }

    public String getCategory() {
        return category;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public int getBgColorResId() {
        return bgColorResId;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}


