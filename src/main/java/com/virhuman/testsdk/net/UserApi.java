package com.virhuman.testsdk.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import android.text.TextUtils;
import com.virhuman.testsdk.utils.AuthStore;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 用户信息接口实现
 * 使用 OkHttp 和 Gson 实现真实的网络请求
 */
public final class UserApi {
    private static final String TAG = "UserApi";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
            .build();
    
    private static final Gson gson = new Gson();
    
    private UserApi() {}

    public interface UserInfoCallback {
        void onSuccess(UserInfoResponse response);
        void onFailure(String errorMessage, Throwable t);
    }

    public interface FavoritesCallback {
        void onSuccess(FavoritesResponse response);
        void onFailure(String errorMessage, Throwable t);
    }


    /**
     * 根据手机号获取用户信息
     * @param phone 手机号
     * @param callback 回调接口
     */
    public static void getUserInfo(String phone, UserInfoCallback callback) {
        getUserInfo(null, phone, callback);
    }
    
    /**
     * 根据手机号获取用户信息（带上下文，用于网络检查）
     * @param context 上下文（可为null）
     * @param phone 手机号
     * @param callback 回调接口
     */
    public static void getUserInfo(Context context, String phone, UserInfoCallback callback) {
        if (callback == null) return;
        
        if (isBlank(phone)) {
            callback.onFailure("手机号不能为空", null);
            return;
        }
        
        // 检查网络连接
        if (context != null && !isNetworkAvailable(context)) {
            callback.onFailure("网络不可用，请检查网络连接", null);
            return;
        }
        
        // 获取token
        String token = context != null ? AuthStore.getToken(context) : null;
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录", null);
            Log.e(TAG, "获取用户信息失败: token为空，用户未登录");
            return;
        }
        
        String url = ApiConfig.GET_USER_INFO_URL + phone;
        Log.d(TAG, "获取用户信息请求: URL=" + url);
        
        // 构建请求，添加Authorization头
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取用户信息请求失败", e);
                String errorMsg = getErrorMessage(e);
                callback.onFailure(errorMsg, e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg;
                    // 获取完整响应体
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.code() == 401) {
                        errorMsg = "未授权，请重新登录";
                        Log.e(TAG, "获取用户信息失败: 401未授权，token可能已过期，响应: " + responseBody);
                    } else {
                        errorMsg = "服务器错误: " + response.code() + ", 响应: " + responseBody;
                        Log.e(TAG, errorMsg);
                    }
                    callback.onFailure(errorMsg, null);
                    return;
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "获取用户信息响应: " + responseBody);
                
                try {
                        // 首先检查响应内容是否为空
                        if (TextUtils.isEmpty(responseBody)) {
                            callback.onFailure("响应解析失败: 返回数据为空", null);
                            return;
                        }
                        
                        // 尝试解析为JSON
                        UserInfoResponse result = gson.fromJson(responseBody, UserInfoResponse.class);
                        if (result == null) {
                            callback.onFailure("响应解析失败: 返回数据为空", null);
                            return;
                        }
                        
                        // 检查服务器响应的success字段
                        if (result.success) {
                            callback.onSuccess(result);
                        } else {
                            // 如果success为false，调用onFailure
                            String errorMessage = result.message != null ? result.message : result.msg;
                            if (TextUtils.isEmpty(errorMessage)) {
                                errorMessage = "服务器返回失败响应";
                            }
                            // 添加完整响应体到错误信息中，方便调试
                            errorMessage += ", 响应: " + responseBody;
                            Log.e(TAG, "服务器返回失败: " + errorMessage);
                            callback.onFailure(errorMessage, null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "响应解析失败", e);
                        // 捕获所有解析异常，添加详细的异常信息
                        String errorMessage = "服务器返回错误: " + responseBody;
                        if (e.getMessage() != null) {
                            errorMessage += " (解析异常: " + e.getMessage() + ")";
                        }
                        callback.onFailure(errorMessage, e);
                    }
            }
        });
    }

    /**
     * 获取用户收藏列表
     */
    public static void getUserFavorites(Context context, int userId, FavoritesCallback callback) {
        if (callback == null) return;

        if (userId <= 0) {
            callback.onFailure("用户ID无效", null);
            return;
        }

        if (context != null && !isNetworkAvailable(context)) {
            callback.onFailure("网络不可用，请检查网络连接", null);
            return;
        }

        String token = context != null ? AuthStore.getToken(context) : null;
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录", null);
            Log.e(TAG, "获取用户收藏失败: token为空，用户未登录");
            return;
        }

        String url = ApiConfig.USER_FAVORITES_URL + userId + "/favorites";
        Log.d(TAG, "获取用户收藏请求: URL=" + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取用户收藏请求失败", e);
                String errorMsg = getErrorMessage(e);
                callback.onFailure(errorMsg, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "获取用户收藏响应: " + responseBody);

                if (!response.isSuccessful()) {
                    String errorMsg = "服务器错误: " + response.code() + ", 响应: " + responseBody;
                    callback.onFailure(errorMsg, null);
                    return;
                }

                try {
                    if (TextUtils.isEmpty(responseBody)) {
                        callback.onFailure("响应解析失败: 返回数据为空", null);
                        return;
                    }

                    FavoritesResponse result = gson.fromJson(responseBody, FavoritesResponse.class);
                    if (result == null) {
                        callback.onFailure("响应解析失败: 返回数据为空", null);
                        return;
                    }

                    if (result.code == 200) {
                        callback.onSuccess(result);
                    } else {
                        String errorMessage = !TextUtils.isEmpty(result.message) ? result.message : "服务器返回失败响应";
                        errorMessage += ", 响应: " + responseBody;
                        callback.onFailure(errorMessage, null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析收藏列表响应失败", e);
                    String errorMessage = "服务器返回错误: " + responseBody;
                    if (e.getMessage() != null) {
                        errorMessage += " (解析异常: " + e.getMessage() + ")";
                    }
                    callback.onFailure(errorMessage, e);
                }
            }
        });
    }

    /**
     * 检查网络是否可用
     */
    private static boolean isNetworkAvailable(Context context) {

        if (context == null) return true; // 如果context为null，跳过检查
        
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "检查网络状态失败", e);
            return true; // 出错时默认允许请求
        }
    }
    
    /**
     * 获取友好的错误信息
     */
    private static String getErrorMessage(Throwable e) {
        if (e == null) {
            return "网络请求失败";
        }
        
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return "无法连接到服务器，请检查：\n1. 设备与服务器是否在同一网络\n2. 服务器地址是否正确\n3. 服务器是否正在运行";
        } else if (e instanceof SocketTimeoutException) {
            return "连接超时，请检查网络连接";
        } else if (e instanceof IOException) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Failed to connect")) {
                return "连接失败，请检查服务器地址和网络连接";
            }
            return "网络错误: " + msg;
        }
        
        return "网络请求失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
    }
    
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    /**
     * 更新用户信息请求模型
     */
    public static class UpdateUserRequest {
        public String username;
        public String account;
        public String password;
        public String phone;
        public String realname;
        public String age;
    }
    
    /**
     * 用户信息响应模型
     */
    public static class UserInfoResponse {
        public boolean success;
        public int code;
        public String message;
        public String msg;
        public UserData data;
        
        public static class UserData {
            public int id;
            public String username;
            public String account;
            public String password;
            public String phone;
            public String loginTime;
            public String loginIp;
            public String realname;
            public String age;
            public String avatar;
            public String createTime;
        }
    }

    /**
     * 用户收藏列表响应模型
     */
    public static class FavoritesResponse {
        public int code;
        public String message;
        public java.util.List<FavoriteRecord> data;

        public static class FavoriteRecord {
            public int id;
            public int userId;
            public String targetType;
            public int targetId;
            public String createdAt;
        }
    }
    
    /**
     * 退出登录响应模型
     */
    public static class LogoutResponse {

        public boolean success;
        public int code;
        public String message;
        public String msg;
        public String data; // logout接口返回的data是字符串类型
    }
    
    /**
     * 通用字符串数据响应模型
     * 用于处理data字段为字符串类型的接口响应
     */
    public static class StringDataResponse {
        public boolean success;
        public int code;
        public String message;
        public String msg;
        public String data; // data字段为字符串类型
    }
    
    /**
     * 退出登录
     * @param context 上下文（可为null）
     * @param callback 回调接口
     */
    public static void logout(Context context, UserInfoCallback callback) {
        if (callback == null) return;
        
        // 检查网络连接
        if (context != null && !isNetworkAvailable(context)) {
            callback.onFailure("网络不可用，请检查网络连接", null);
            return;
        }
        
        // 获取token
        String token = context != null ? AuthStore.getToken(context) : null;
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录", null);
            Log.e(TAG, "退出登录失败: token为空，用户未登录");
            return;
        }
        
        String url = ApiConfig.LOGOUT_URL;
        Log.d(TAG, "退出登录请求: URL=" + url);
        
        // 构建请求，添加Authorization头
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, ""))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();
        
        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "退出登录请求失败", e);
                String errorMsg = getErrorMessage(e);
                callback.onFailure(errorMsg, e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg;
                    // 获取完整响应体
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.code() == 401) {
                        errorMsg = "未授权，请重新登录";
                        Log.e(TAG, "退出登录失败: 401未授权，token可能已过期，响应: " + responseBody);
                    } else {
                        errorMsg = "服务器错误: " + response.code() + ", 响应: " + responseBody;
                        Log.e(TAG, errorMsg);
                    }
                    callback.onFailure(errorMsg, null);
                    return;
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "退出登录响应: " + responseBody);
                
                try {
                        // 首先检查响应内容是否为空
                        if (TextUtils.isEmpty(responseBody)) {
                            Log.e(TAG, "退出登录响应为空");
                            callback.onFailure("响应解析失败: 返回数据为空", null);
                            return;
                        }
                        
                        // 打印完整响应，方便调试
                        Log.d(TAG, "退出登录完整响应: " + responseBody);
                        
                        // 尝试解析为JSON
                        LogoutResponse logoutResult = gson.fromJson(responseBody, LogoutResponse.class);
                        if (logoutResult == null) {
                            Log.e(TAG, "JSON解析结果为空");
                            callback.onFailure("响应解析失败: 返回数据为空", null);
                            return;
                        }
                        
                        // 检查服务器响应的success字段
                        if (logoutResult.success) {
                            // 清除本地存储的token和用户信息
                            if (context != null) {
                                AuthStore.clear(context);
                            }
                            
                            // 创建UserInfoResponse返回给callback
                            UserInfoResponse result = new UserInfoResponse();
                            result.success = logoutResult.success;
                            result.code = logoutResult.code;
                            result.message = logoutResult.message;
                            result.msg = logoutResult.msg;
                            
                            callback.onSuccess(result);
                        } else {
                            // 如果success为false，调用onFailure
                            String errorMessage = logoutResult.message != null ? logoutResult.message : logoutResult.msg;
                            if (TextUtils.isEmpty(errorMessage)) {
                                errorMessage = "服务器返回失败响应";
                            }
                            // 添加完整响应体到错误信息中，方便调试
                            errorMessage += ", 响应: " + responseBody;
                            Log.e(TAG, "服务器返回失败: " + errorMessage);
                            callback.onFailure(errorMessage, null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "响应解析失败，原始响应: " + responseBody, e);
                        // 捕获所有解析异常，包含详细的异常信息
                        String errorMessage = "服务器返回错误: " + responseBody;
                        if (e.getMessage() != null) {
                            errorMessage += " (解析异常: " + e.getMessage() + ")";
                        }
                        callback.onFailure(errorMessage, e);
                    }
            }
        });
    }
    
    /**
     * 更新用户信息
     * @param context 上下文（可为null）
     * @param userId 用户ID
     * @param request 更新请求参数
     * @param callback 回调接口
     */
    public static void updateUserInfo(Context context, int userId, UpdateUserRequest request, UserInfoCallback callback) {
        if (callback == null) return;
        
        if (userId <= 0) {
            callback.onFailure("用户ID无效", null);
            return;
        }
        
        if (request == null) {
            callback.onFailure("请求参数不能为空", null);
            return;
        }
        
        // 检查网络连接
        if (context != null && !isNetworkAvailable(context)) {
            callback.onFailure("网络不可用，请检查网络连接", null);
            return;
        }
        
        // 获取token
        String token = context != null ? AuthStore.getToken(context) : null;
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录", null);
            Log.e(TAG, "更新用户信息失败: token为空，用户未登录");
            return;
        }
        
        String url = ApiConfig.UPDATE_USER_INFO_URL + userId;
        Log.d(TAG, "更新用户信息请求: URL=" + url);
        
        try {
            // 构建请求体
            String jsonBody = gson.toJson(request);
            Log.d(TAG, "更新用户信息请求体: " + jsonBody);
            
            // 构建请求，添加Authorization头
            Request okHttpRequest = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(JSON, jsonBody))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            
            // 异步执行请求
            client.newCall(okHttpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "更新用户信息请求失败", e);
                    String errorMsg = getErrorMessage(e);
                    callback.onFailure(errorMsg, e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorMsg;
                        // 获取完整响应体
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (response.code() == 401) {
                            errorMsg = "未授权，请重新登录";
                            Log.e(TAG, "更新用户信息失败: 401未授权，token可能已过期，响应: " + responseBody);
                        } else {
                            errorMsg = "服务器错误: " + response.code() + ", 响应: " + responseBody;
                            Log.e(TAG, errorMsg);
                        }
                        callback.onFailure(errorMsg, null);
                        return;
                    }
                    
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "更新用户信息响应: " + responseBody);
                    
                    try {
                        // 首先检查响应内容是否为空
                        if (TextUtils.isEmpty(responseBody)) {
                            callback.onFailure("响应解析失败: 返回数据为空", null);
                            return;
                        }
                        
                        // 尝试解析为JSON
                        StringDataResponse updateResult = gson.fromJson(responseBody, StringDataResponse.class);
                        if (updateResult == null) {
                            callback.onFailure("响应解析失败: 返回数据为空", null);
                            return;
                        }
                        
                        // 检查服务器响应的success字段
                        if (updateResult.success) {
                            // 创建UserInfoResponse返回给callback
                            UserInfoResponse result = new UserInfoResponse();
                            result.success = updateResult.success;
                            result.code = updateResult.code;
                            result.message = updateResult.message;
                            result.msg = updateResult.msg;
                            
                            callback.onSuccess(result);
                        } else {
                            // 如果success为false，调用onFailure
                            String errorMessage = updateResult.message != null ? updateResult.message : updateResult.msg;
                            if (TextUtils.isEmpty(errorMessage)) {
                                errorMessage = "服务器返回失败响应";
                            }
                            // 添加完整响应体到错误信息中，方便调试
                            errorMessage += ", 响应: " + responseBody;
                            Log.e(TAG, "服务器返回失败: " + errorMessage);
                            callback.onFailure(errorMessage, null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "响应解析失败", e);
                        // 捕获所有解析异常，包含详细的异常信息
                        String errorMessage = "服务器返回错误: " + responseBody;
                        if (e.getMessage() != null) {
                            errorMessage += " (解析异常: " + e.getMessage() + ")";
                        }
                        callback.onFailure(errorMessage, e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "构建请求失败", e);
            callback.onFailure("请求构建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新用户信息并上传头像（multipart/form-data）
     * 表单字段：
     *  - user: JSON 字符串 (application/json)
     *  - avatar: 文件 (image/*)
     */
    public static void updateUserWithAvatar(Context context, int userId, UpdateUserRequest request, java.io.File avatarFile, UserInfoCallback callback) {
        if (callback == null) return;
        if (userId <= 0) {
            callback.onFailure("用户ID无效", null);
            return;
        }
        if (request == null) {
            callback.onFailure("请求参数不能为空", null);
            return;
        }
        if (avatarFile == null || !avatarFile.exists()) {
            callback.onFailure("头像文件不存在", null);
            return;
        }
        if (context != null && !isNetworkAvailable(context)) {
            callback.onFailure("网络不可用，请检查网络连接", null);
            return;
        }

        String token = context != null ? AuthStore.getToken(context) : null;
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录", null);
            return;
        }

        String url = ApiConfig.UPDATE_USER_INFO_URL + userId + "/with-avatar";
        Log.d(TAG, "上传头像请求: URL=" + url);

        try {
            okhttp3.MediaType jsonType = okhttp3.MediaType.parse("application/json; charset=utf-8");
            okhttp3.MediaType imgType = okhttp3.MediaType.parse("image/jpeg");
            RequestBody userPart = RequestBody.create(jsonType, gson.toJson(request));
            RequestBody filePart = RequestBody.create(imgType, avatarFile);
            okhttp3.MultipartBody requestBody = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("user", null, userPart)
                    .addFormDataPart("avatar", avatarFile.getName(), filePart)
                    .build();

            Request okHttpRequest = new Request.Builder()
                    .url(url)
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(okHttpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "上传头像请求失败", e);
                    String errorMsg = getErrorMessage(e);
                    callback.onFailure(errorMsg, e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        String msg = "服务器返回错误: " + response.code() + " " + responseBody;
                        callback.onFailure(msg, null);
                        return;
                    }
                    try {
                        // 先解析为 JsonObject，判断 data 字段类型，兼容后端返回 data 为字符串或对象的两种情况
                        com.google.gson.JsonElement rootElem = com.google.gson.JsonParser.parseString(responseBody);
                        if (rootElem != null && rootElem.isJsonObject()) {
                            com.google.gson.JsonObject rootObj = rootElem.getAsJsonObject();
                            com.google.gson.JsonElement dataElem = rootObj.get("data");
                            boolean dataIsObject = dataElem != null && dataElem.isJsonObject();

                            if (!dataIsObject) {
                                // data 非对象（如字符串），解析为 StringDataResponse 风格
                                StringDataResponse strResp = gson.fromJson(responseBody, StringDataResponse.class);
                                if (strResp != null && strResp.success) {
                                    UserInfoResponse result = new UserInfoResponse();
                                    result.success = strResp.success;
                                    result.code = strResp.code;
                                    result.message = strResp.message;
                                    result.msg = strResp.msg;
                                    callback.onSuccess(result);
                                    return;
                                } else {
                                    callback.onFailure(strResp != null ? (strResp.message != null ? strResp.message : "上传失败") : "上传失败", null);
                                    return;
                                }
                            } else {
                                // data 为对象，按 UserInfoResponse 解析
                                UserInfoResponse resp = gson.fromJson(responseBody, UserInfoResponse.class);
                                if (resp == null) {
                                    callback.onFailure("服务器返回空响应", null);
                                } else if (!resp.success) {
                                    callback.onFailure(resp.message != null ? resp.message : "上传失败", null);
                                } else {
                                    callback.onSuccess(resp);
                                }
                                return;
                            }
                        } else {
                            callback.onFailure("响应格式错误", null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析上传头像响应失败: " + responseBody, e);
                        callback.onFailure("响应解析失败: " + e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure("构建请求失败: " + e.getMessage(), e);
        }
    }
}