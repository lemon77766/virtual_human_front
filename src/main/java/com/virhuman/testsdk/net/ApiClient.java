package com.virhuman.testsdk.net;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public final class ApiClient {
    private static volatile OkHttpClient client;

    private ApiClient() {}

    public static OkHttpClient get() {
        if (client != null) return client;
        synchronized (ApiClient.class) {
            if (client == null) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .addInterceptor(logging)
                        .build();
            }
        }
        return client;
    }
}


