package com.virhuman.testsdk.model;

import java.util.List;

public class PostItem {
    private int id;
    private int authorId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private List<String> images; // 图片URL列表
    private String audioUrl; // 音频URL
    private String audioDuration; // 音频时长
    private String createTime;
    private String postTags; // 话题标签
    private String locationTags; // 位置标签
    private int likeCount;
    private int commentCount;
    private int favoriteCount;
    private boolean isLiked;
    private boolean isFavorited;

    // 辅助字段，用于UI布局判断
    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    public boolean hasAudio() {
        return audioUrl != null && !audioUrl.trim().isEmpty();
    }

    public boolean hasOnlyTextAndAudio() {
        return !hasImages() && hasAudio() && content != null && !content.trim().isEmpty();
    }

    public boolean hasOnlyText() {
        return !hasImages() && !hasAudio() && content != null && !content.trim().isEmpty();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(String audioDuration) {
        this.audioDuration = audioDuration;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPostTags() {
        return postTags;
    }

    public void setPostTags(String postTags) {
        this.postTags = postTags;
    }

    public String getLocationTags() {
        return locationTags;
    }

    public void setLocationTags(String locationTags) {
        this.locationTags = locationTags;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isFavorited() {
        return isFavorited;
    }

    public void setFavorited(boolean favorited) {
        isFavorited = favorited;
    }
}





