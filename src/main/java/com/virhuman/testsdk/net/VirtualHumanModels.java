package com.virhuman.testsdk.net;

/**
 * 虚拟人相关数据模型
 */
public final class VirtualHumanModels {
    private VirtualHumanModels() {}

    /**
     * 获取虚拟人列表响应
     */
    public static final class VirtualHumanListResult {
        public boolean success;
        public int code;
        public String message;
        public String msg;
        public VirtualHumanItem[] data;

        /**
         * 兼容不同字段的消息展示
         */
        public String bestMessage() {
            if (message != null && !message.trim().isEmpty()) return message;
            if (msg != null && !msg.trim().isEmpty()) return msg;
            return null;
        }
    }

    /**
     * 虚拟人数据项
     */
    public static final class VirtualHumanItem {
        public int id;
        public String number;
        public String avatarName;
        public String avatarId;
        public String voiceName;
        public String voiceId;
        public String imageUrl;
        public String createTime;
        public String updateTime;
        // 可选的标签字段（如果后端返回则会被解析）
        public String avatarTags;
        public String voiceTags;
    }
}


