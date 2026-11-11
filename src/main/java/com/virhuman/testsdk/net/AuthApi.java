package com.virhuman.testsdk.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
 * 登录接口实现
 * 使用 OkHttp 和 Gson 实现真实的网络请求
 */
public final class AuthApi {
    private static final String TAG = "AuthApi";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
            .build();
    
    private static final Gson gson = new Gson();
    
    private AuthApi() {}

    public interface LoginCallback {
        void onSuccess(LoginModels.LoginResult result);

        void onFailure(String errorMessage, Throwable t);
    }
    
    public interface SendSmsCallback {
        void onSuccess(LoginModels.SendSmsResult result);
        
        void onFailure(String errorMessage, Throwable t);
    }

    /**
     * 登录接口（验证码登录）
     * @param phone 手机号
     * @param code 验证码
     * @param callback 回调接口
     */
    public static void login(String phone, String code, LoginCallback callback) {
        loginBySms(phone, code, callback);
    }
    
    /**
     * 验证码登录接口
     * @param phone 手机号
     * @param code 验证码
     * @param callback 回调接口
     */
    public static void loginBySms(String phone, String code, LoginCallback callback) {
        if (callback == null) return;

        if (isBlank(phone) || isBlank(code)) {
            callback.onFailure("手机号或验证码不能为空", null);
            return;
        }

        // 构建请求体
        LoginRequest requestBody = new LoginRequest();
        requestBody.phone = phone;
        requestBody.code = code;
        
        Log.d(TAG, "登录请求: URL=" + ApiConfig.SMS_LOGIN_URL + ", phone=" + phone);
        String json = gson.toJson(requestBody);
        Log.d(TAG, "请求体: " + json);
        RequestBody body = RequestBody.create(JSON, json);
        
        // 构建请求
        Request request = new Request.Builder()
                .url(ApiConfig.SMS_LOGIN_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        
        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "登录请求失败", e);
                String errorMsg = getErrorMessage(e);
                callback.onFailure(errorMsg, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "登录响应: code=" + response.code() + ", body=" + responseBody);

                if (!response.isSuccessful()) {
                    // 尝试解析错误响应
                    String errorMsg = "服务器错误: " + response.code();
                    try {
                        LoginModels.LoginResult errorResult = gson.fromJson(responseBody, LoginModels.LoginResult.class);
                        if (errorResult != null) {
                            String msg = errorResult.bestMessage();
                            if (msg != null && !msg.trim().isEmpty()) {
                                errorMsg = msg;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误响应失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onFailure(errorMsg, null);
                    return;
                }

                try {
                    LoginModels.LoginResult result = gson.fromJson(responseBody, LoginModels.LoginResult.class);
                    if (result == null) {
                        callback.onFailure("响应解析失败: 返回数据为空", null);
                        return;
                    }
                    callback.onSuccess(result);
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON解析失败: " + responseBody, e);
                    callback.onFailure("响应解析失败: " + e.getMessage() + "\n响应内容: " + responseBody, e);
                }
            }
        });
    }
    
    /**
     * 密码登录接口
     * @param account 账号（手机号或用户名）
     * @param password 密码
     * @param callback 回调接口
     */
    public static void loginByPassword(String account, String password, LoginCallback callback) {
        if (callback == null) return;

        if (isBlank(account) || isBlank(password)) {
            callback.onFailure("账号或密码不能为空", null);
            return;
        }

        // 构建请求体
        PasswordLoginRequest requestBody = new PasswordLoginRequest();
        requestBody.account = account;
        requestBody.password = password;
        
        Log.d(TAG, "密码登录请求: URL=" + ApiConfig.PASSWORD_LOGIN_URL + ", account=" + account);
        String json = gson.toJson(requestBody);
        Log.d(TAG, "请求体: " + json);
        RequestBody body = RequestBody.create(JSON, json);
        
        // 构建请求
        Request request = new Request.Builder()
                .url(ApiConfig.PASSWORD_LOGIN_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        
        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "密码登录请求失败", e);
                String errorMsg = getErrorMessage(e);
                callback.onFailure(errorMsg, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "密码登录响应: code=" + response.code() + ", body=" + responseBody);

                if (!response.isSuccessful()) {
                    // 尝试解析错误响应
                    String errorMsg = "服务器错误: " + response.code();
                    try {
                        LoginModels.LoginResult errorResult = gson.fromJson(responseBody, LoginModels.LoginResult.class);
                        if (errorResult != null) {
                            String msg = errorResult.bestMessage();
                            if (msg != null && !msg.trim().isEmpty()) {
                                errorMsg = msg;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误响应失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onFailure(errorMsg, null);
                    return;
                }

                try {
                    LoginModels.LoginResult result = gson.fromJson(responseBody, LoginModels.LoginResult.class);
                    if (result == null) {
                        callback.onFailure("响应解析失败: 返回数据为空", null);
                        return;
                    }
                    callback.onSuccess(result);
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON解析失败: " + responseBody, e);
                    callback.onFailure("响应解析失败: " + e.getMessage() + "\n响应内容: " + responseBody, e);
                }
            }
        });
    }

    /**
     * 发送短信验证码接口
     * @param phone 手机号
     * @param callback 回调接口
     */
    public static void sendSms(String phone, SendSmsCallback callback) {
        sendSms(null, phone, callback);
    }
    
    /**
     * 发送短信验证码接口（带上下文，用于网络检查）
     * @param context 上下文（可为null）
     * @param phone 手机号
     * @param callback 回调接口
     */
    public static void sendSms(Context context, String phone, SendSmsCallback callback) {
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
        
        Log.d(TAG, "发送验证码请求: URL=" + ApiConfig.SEND_SMS_URL + ", phone=" + phone);
        
        // 构建请求体
        SendSmsRequest requestBody = new SendSmsRequest();
        requestBody.phone = phone;
        
        String json = gson.toJson(requestBody);
        Log.d(TAG, "请求体: " + json);
        RequestBody body = RequestBody.create(JSON, json);
        
        // 构建请求
        Request request = new Request.Builder()
                .url(ApiConfig.SEND_SMS_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "发送验证码请求失败", e);
                String errorMsg = getErrorMessage(e);
                callback.onFailure(errorMsg, e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "发送验证码响应: code=" + response.code() + ", body=" + responseBody);
                
                if (!response.isSuccessful()) {
                    // 尝试解析错误响应
                    String errorMsg = "服务器错误: " + response.code();
                    try {
                        LoginModels.SendSmsResult errorResult = gson.fromJson(responseBody, LoginModels.SendSmsResult.class);
                        if (errorResult != null) {
                            String msg = errorResult.bestMessage();
                            if (msg != null && !msg.trim().isEmpty()) {
                                errorMsg = msg;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误响应失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onFailure(errorMsg, null);
                    return;
                }
                
                try {
                    LoginModels.SendSmsResult result = gson.fromJson(responseBody, LoginModels.SendSmsResult.class);
                    if (result == null) {
                        callback.onFailure("响应解析失败: 返回数据为空", null);
                        return;
                    }
                    callback.onSuccess(result);
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON解析失败: " + responseBody, e);
                    callback.onFailure("响应解析失败: " + e.getMessage() + "\n响应内容: " + responseBody, e);
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
     * 登录请求体（验证码登录）
     */
    private static class LoginRequest {
        public String phone;
        public String code;
    }
    
    /**
     * 发送验证码请求体
     */
    private static class SendSmsRequest {
        public String phone;
    }
    
    /**
     * 密码登录请求体
     */
    private static class PasswordLoginRequest {
        public String account;
        public String password;
    }
}



