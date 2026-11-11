package com.virhuman.testsdk.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.virhuman.testsdk.model.DramaItem;
import com.virhuman.testsdk.utils.AuthStore;


import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OperaApi {

    private static final String TAG = "OperaApi";


    public interface OperaListCallback {
        void onSuccess(List<DramaItem> operas);
        void onFailure(String errorMessage);
    }

    public static void fetchOperaList(Context context, final OperaListCallback callback) {
        Log.d(TAG, "fetchOperaList called, url=" + ApiConfig.API_OPERA);
        String token = AuthStore.getToken(context);
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录");
            return;
        }


        Request request = new Request.Builder()
                .url(ApiConfig.API_OPERA)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchMusicList onFailure, url=" + ApiConfig.API_MUSIC, e);
                mainHandler.post(() -> callback.onFailure("Network request failed: " + e.getMessage()));
            }



            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                String bodyToLog = responseBody != null && responseBody.length() > 1000
                        ? responseBody.substring(0, 1000) + "...(truncated)"
                        : responseBody;
                Log.d(TAG, "fetchMusicList onResponse, code=" + response.code()
                        + ", body=" + bodyToLog);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("Request failed with code: " + response.code() + " Body: " + responseBody));
                    return;
                }

                try {
                    Gson gson = new Gson();
                    OperaResponse operaResponse = gson.fromJson(responseBody, OperaResponse.class);
                    if (operaResponse != null && operaResponse.code == 200 && operaResponse.data != null) {
                        Log.d(TAG, "fetchMusicList parse success, items=" + operaResponse.data.size());
                        for (int i = 0; i < operaResponse.data.size(); i++) {
                            DramaItem item = operaResponse.data.get(i);
                            if (item != null) {
                                Log.d(TAG, "MusicApi item[" + i + "]: id=" + item.getId()
                                        + ", title=" + item.getTitle()
                                        + ", coverUrl=" + item.getCoverUrl()
                                        + ", audioUrl=" + item.getAudioUrl());
                            }
                        }
                        mainHandler.post(() -> callback.onSuccess(operaResponse.data));
                    } else {
                        Log.w(TAG, "fetchMusicList invalid response, body=" + responseBody);
                        mainHandler.post(() -> callback.onFailure("Failed to parse response or invalid data."));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "fetchMusicList JSON parsing error", e);
                    mainHandler.post(() -> callback.onFailure("JSON parsing error: " + e.getMessage()));
                }
            }


        });
    }

    public static void fetchMusicList(Context context, final OperaListCallback callback) {
        Log.d(TAG, "fetchMusicList called, url=" + ApiConfig.API_MUSIC);
        String token = AuthStore.getToken(context);
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录");
            return;
        }


        Request request = new Request.Builder()
                .url(ApiConfig.API_MUSIC)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchMusicList onFailure, url=" + ApiConfig.API_MUSIC, e);
                mainHandler.post(() -> callback.onFailure("Network request failed: " + e.getMessage()));
            }



            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                String bodyToLog = responseBody != null && responseBody.length() > 1000
                        ? responseBody.substring(0, 1000) + "...(truncated)"
                        : responseBody;
                Log.d(TAG, "fetchMusicList onResponse, code=" + response.code()
                        + ", body=" + bodyToLog);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("Request failed with code: " + response.code() + " Body: " + responseBody));
                    return;
                }

                try {
                    Gson gson = new Gson();
                    OperaResponse operaResponse = gson.fromJson(responseBody, OperaResponse.class);
                    if (operaResponse != null && operaResponse.code == 200 && operaResponse.data != null) {
                        Log.d(TAG, "fetchMusicList parse success, items=" + operaResponse.data.size());
                        for (int i = 0; i < operaResponse.data.size(); i++) {
                            DramaItem item = operaResponse.data.get(i);
                            if (item != null) {
                                Log.d(TAG, "MusicApi item[" + i + "]: id=" + item.getId()
                                        + ", title=" + item.getTitle()
                                        + ", coverUrl=" + item.getCoverUrl()
                                        + ", audioUrl=" + item.getAudioUrl());
                            }
                        }
                        mainHandler.post(() -> callback.onSuccess(operaResponse.data));
                    } else {
                        Log.w(TAG, "fetchMusicList invalid response, body=" + responseBody);
                        mainHandler.post(() -> callback.onFailure("Failed to parse response or invalid data."));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "fetchMusicList JSON parsing error", e);
                    mainHandler.post(() -> callback.onFailure("JSON parsing error: " + e.getMessage()));
                }
            }


        });
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * 收藏戏曲
     */
    public static void favoriteOpera(Context context, int operaId, int userId, final SimpleCallback callback) {
        if (context == null || callback == null) return;

        String token = AuthStore.getToken(context);
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录");
            return;
        }
        if (operaId <= 0 || userId <= 0) {
            callback.onFailure("参数无效，无法收藏");
            return;
        }

        String url = ApiConfig.BASE_URL + "/api/opera/" + operaId + "/favorite?userId=" + userId;
        Log.d(TAG, "favoriteOpera url=" + url);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, new byte[0]))
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "favoriteOpera onFailure", e);
                mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "favoriteOpera onResponse code=" + response.code() + ", body=" + body);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("收藏失败，code=" + response.code()));
                    return;
                }

                mainHandler.post(callback::onSuccess);
            }
        });
    }

    /**
     * 取消收藏戏曲
     */
    public static void unfavoriteOpera(Context context, int operaId, int userId, final SimpleCallback callback) {
        if (context == null || callback == null) return;

        String token = AuthStore.getToken(context);
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录");
            return;
        }
        if (operaId <= 0 || userId <= 0) {
            callback.onFailure("参数无效，无法取消收藏");
            return;
        }

        String url = ApiConfig.BASE_URL + "/api/opera/" + operaId + "/unfavorite?userId=" + userId;
        Log.d(TAG, "unfavoriteOpera url=" + url);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "unfavoriteOpera onFailure", e);
                mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "unfavoriteOpera onResponse code=" + response.code() + ", body=" + body);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("取消收藏失败，code=" + response.code()));
                    return;
                }

                mainHandler.post(callback::onSuccess);
            }
        });
    }

    /**
     * 收藏音乐
     */
    public static void favoriteMusic(Context context, int musicId, int userId, final SimpleCallback callback) {
        if (context == null || callback == null) return;

        String token = AuthStore.getToken(context);
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录");
            return;
        }
        if (musicId <= 0 || userId <= 0) {
            callback.onFailure("参数无效，无法收藏");
            return;
        }

        String url = ApiConfig.BASE_URL + "/api/music/" + musicId + "/favorite?userId=" + userId;
        Log.d(TAG, "favoriteMusic url=" + url);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, new byte[0]))
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "favoriteMusic onFailure", e);
                mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "favoriteMusic onResponse code=" + response.code() + ", body=" + body);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("收藏失败，code=" + response.code()));
                    return;
                }

                mainHandler.post(callback::onSuccess);
            }
        });
    }

    /**
     * 取消收藏音乐
     */
    public static void unfavoriteMusic(Context context, int musicId, int userId, final SimpleCallback callback) {
        if (context == null || callback == null) return;

        String token = AuthStore.getToken(context);
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录");
            return;
        }
        if (musicId <= 0 || userId <= 0) {
            callback.onFailure("参数无效，无法取消收藏");
            return;
        }

        String url = ApiConfig.BASE_URL + "/api/music/" + musicId + "/unfavorite?userId=" + userId;
        Log.d(TAG, "unfavoriteMusic url=" + url);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        ApiClient.get().newCall(request).enqueue(new Callback() {
            private final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "unfavoriteMusic onFailure", e);
                mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "unfavoriteMusic onResponse code=" + response.code() + ", body=" + body);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("取消收藏失败，code=" + response.code()));
                    return;
                }

                mainHandler.post(callback::onSuccess);
            }
        });
    }
}


