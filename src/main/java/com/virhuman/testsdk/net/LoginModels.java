package com.virhuman.testsdk.net;

/**
 * 登录相关模型（占位实现）
 * <p>
 * 说明：此文件用于修复分支切换后缺失的 com.virhuman.testsdk.net 包导致的编译失败。
 * 你后续接入真实后端接口时，可用真实字段替换这里的占位结构。
 */
public final class LoginModels {
    private LoginModels() {}

    public static final class LoginResult {
        public boolean success;
        public int code;
        public String message;
        public String msg;
        public LoginData data;

        /**
         * 兼容不同字段的消息展示
         */
        public String bestMessage() {
            if (message != null && !message.trim().isEmpty()) return message;
            if (msg != null && !msg.trim().isEmpty()) return msg;
            return null;
        }
    }

    public static final class LoginData {
        public String token;
        public int userId;
        public String account;
        public String username;
        public String realname;
        public Integer age;  // 使用Integer以支持null值
        public String phone;
    }
    
    /**
     * 发送验证码响应
     */
    public static final class SendSmsResult {
        public boolean success;
        public int code;
        public String message;
        public String msg;
        public String data; // 验证码响应消息
        
        /**
         * 兼容不同字段的消息展示
         */
        public String bestMessage() {
            if (message != null && !message.trim().isEmpty()) return message;
            if (msg != null && !msg.trim().isEmpty()) return msg;
            if (data != null && !data.trim().isEmpty()) return data;
            return null;
        }
    }
}



