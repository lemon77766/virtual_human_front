package com.virhuman.testsdk.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.virhuman.testsdk.utils.AuthStore;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 虚拟人接口实现
 * 使用 OkHttp 和 Gson 实现真实的网络请求
 */
public final class VirtualHumanApi {
    private static final String TAG = "VirtualHumanApi";
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
            .build();
    
    private static final Gson gson = new Gson();
    
    private VirtualHumanApi() {}

    public interface VirtualHumanListCallback {
        void onSuccess(VirtualHumanModels.VirtualHumanListResult result);

        void onFailure(String errorMessage, Throwable t);
    }

    /**
     * 获取虚拟人列表接口
     * @param context 上下文（必需，用于获取token）
     * @param callback 回调接口
     */
    public static void getVirtualHumanList(Context context, VirtualHumanListCallback callback) {
        if (callback == null) return;
        
        if (context == null) {
            callback.onFailure("上下文不能为空", null);
            return;
        }
        
        // 检查网络连接
        if (!isNetworkAvailable(context)) {
            callback.onFailure("网络不可用，请检查网络连接", null);
            return;
        }
        
        // 获取token
        String token = AuthStore.getToken(context);
        if (TextUtils.isEmpty(token)) {
            callback.onFailure("未登录，请先登录", null);
            Log.e(TAG, "获取虚拟人列表失败: token为空，用户未登录");
            return;
        }
        
        Log.d(TAG, "获取虚拟人列表请求: URL=" + ApiConfig.VIRTUAL_HUMANS_URL);
        
        // 构建GET请求，添加Authorization头
        Request.Builder requestBuilder = new Request.Builder()
                .url(ApiConfig.VIRTUAL_HUMANS_URL)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + token);
        
        Request request = requestBuilder.build();

        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取虚拟人列表请求失败", e);
                String errorMsg = getErrorMessage(e);
                callback.onFailure(errorMsg, e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg;
                    if (response.code() == 401) {
                        errorMsg = "未授权，请重新登录";
                        Log.e(TAG, "获取虚拟人列表失败: 401未授权，token可能已过期");
                    } else if (response.code() == 403) {
                        errorMsg = "无权限访问";
                        Log.e(TAG, "获取虚拟人列表失败: 403无权限");
                    } else {
                        errorMsg = "服务器错误: " + response.code();
                        Log.e(TAG, errorMsg);
                    }
                    callback.onFailure(errorMsg, null);
                    return;
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "获取虚拟人列表响应: " + responseBody);
                
                try {
                    VirtualHumanModels.VirtualHumanListResult result = 
                        gson.fromJson(responseBody, VirtualHumanModels.VirtualHumanListResult.class);
                    if (result == null) {
                        callback.onFailure("响应解析失败: 返回数据为空", null);
                        return;
                    }
                    callback.onSuccess(result);
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON解析失败", e);
                    callback.onFailure("响应解析失败: " + e.getMessage(), e);
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
}

