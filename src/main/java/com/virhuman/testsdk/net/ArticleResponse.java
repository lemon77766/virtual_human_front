package com.virhuman.testsdk.net;

import java.util.List;

public class ArticleResponse {
    public int code;
    public String message;
    public List<ArticleDto> data;

    public static class ArticleDto {
        public int id;
        public String title;
        public String category;
        public String content;
        public String author;
        public String audioUrl;
        public String coverUrl;
        public int viewCount;
        public int status;
    }
}
