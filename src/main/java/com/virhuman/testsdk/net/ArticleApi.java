package com.virhuman.testsdk.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.model.NewsItem;
import com.virhuman.testsdk.utils.AuthStore;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ArticleApi {

    public interface ArticleListCallback {
        void onSuccess(List<NewsItem> articles);
        void onFailure(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }


    public static void fetchArticleList(Context context, final ArticleListCallback callback) {
        String token = AuthStore.getToken(context);
        if (token == null || token.trim().isEmpty()) {
            callback.onFailure("未登录，请先登录");
            return;
        }

        Request request = new Request.Builder()
                .url(ApiConfig.API_ARTICLE)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();


        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("请求失败，code: " + response.code()));
                    return;
                }

                try {
                    Gson gson = new Gson();
                    ArticleResponse articleResponse = gson.fromJson(responseBody, ArticleResponse.class);
                    if (articleResponse != null && articleResponse.code == 200 && articleResponse.data != null) {
                        List<NewsItem> result = new ArrayList<>();
                        for (ArticleResponse.ArticleDto dto : articleResponse.data) {
                            if (dto == null || dto.status != 1) continue; // 只展示已上线文章

                            String displayCategory = mapCategory(dto.category);
                            int colorResId = mapCategoryColor(displayCategory);
                            String summary = buildSummary(dto.content);
                            String audioUrl = buildFullUrl(dto.audioUrl);
                            String coverUrl = buildFullUrl(dto.coverUrl);

                            NewsItem item = new NewsItem(
                                    dto.id,
                                    dto.title,
                                    summary,
                                    displayCategory,
                                    "", // 暂无时间字段，可后续扩展
                                    colorResId,
                                    audioUrl,
                                    coverUrl
                            );

                            result.add(item);
                        }
                        mainHandler.post(() -> callback.onSuccess(result));
                    } else {
                        mainHandler.post(() -> callback.onFailure("数据为空或响应格式错误"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("JSON 解析错误: " + e.getMessage()));
                }
            }

            private String buildFullUrl(String path) {
                if (path == null || path.isEmpty()) return null;
                if (path.startsWith("http")) return path;
                return ApiConfig.ARTICLE_BASE_URL + path;
            }

            private String buildSummary(String content) {
                if (content == null) return "";
                content = content.trim();
                if (content.length() <= 40) return content;
                return content.substring(0, 40) + "…";
            }

            private String mapCategory(String category) {
                if (category == null) return "资讯";
                switch (category) {
                    case "health":
                        return "健康养生";
                    case "pension":
                        return "养老政策";
                    case "community":
                        return "社区活动";
                    default:
                        return "资讯";
                }
            }

            private int mapCategoryColor(String displayCategory) {
                if ("健康养生".equals(displayCategory)) {
                    return R.color.profile_icon_bg_orange;
                } else if ("养老政策".equals(displayCategory)) {
                    return R.color.profile_icon_bg_pink;
                } else if ("社区活动".equals(displayCategory)) {
                    return R.color.profile_icon_bg_blue;
                } else {
                    return R.color.home_chip_bg;
                }
            }
        });
    }

    /**
     * 收藏资讯文章
     */
    public static void favoriteArticle(Context context, int articleId, int userId, final SimpleCallback callback) {
        if (context == null || callback == null) return;

        String token = AuthStore.getToken(context);
        if (token == null || token.trim().isEmpty()) {
            callback.onFailure("未登录，请先登录");
            return;
        }
        if (articleId <= 0 || userId <= 0) {
            callback.onFailure("参数无效，无法收藏");
            return;
        }

        String url = ApiConfig.ARTICLE_BASE_URL + "/api/article/" + articleId + "/favorite?userId=" + userId;
        android.util.Log.d("ArticleApi", "favoriteArticle url=" + url);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, new byte[0]))
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("ArticleApi", "favoriteArticle onFailure", e);
                mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                android.util.Log.d("ArticleApi", "favoriteArticle onResponse code=" + response.code() + ", body=" + body);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("收藏失败，code=" + response.code()));
                    return;
                }

                mainHandler.post(callback::onSuccess);
            }
        });
    }

    /**
     * 取消收藏资讯文章
     */
    public static void unfavoriteArticle(Context context, int articleId, int userId, final SimpleCallback callback) {
        if (context == null || callback == null) return;

        String token = AuthStore.getToken(context);
        if (token == null || token.trim().isEmpty()) {
            callback.onFailure("未登录，请先登录");
            return;
        }
        if (articleId <= 0 || userId <= 0) {
            callback.onFailure("参数无效，无法取消收藏");
            return;
        }

        String url = ApiConfig.ARTICLE_BASE_URL + "/api/article/" + articleId + "/unfavorite?userId=" + userId;
        android.util.Log.d("ArticleApi", "unfavoriteArticle url=" + url);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("ArticleApi", "unfavoriteArticle onFailure", e);
                mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                android.util.Log.d("ArticleApi", "unfavoriteArticle onResponse code=" + response.code() + ", body=" + body);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("取消收藏失败，code=" + response.code()));
                    return;
                }

                mainHandler.post(callback::onSuccess);
            }
        });
    }
}

