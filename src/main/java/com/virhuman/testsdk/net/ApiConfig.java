package com.virhuman.testsdk.net;

/**
 * API配置类
 * 存储后端API的基础地址和端点
 */
public final class ApiConfig {
    private ApiConfig() {}
    
    // 后端服务器地址（根据实际情况修改）
    //软件学院A501配置的ip地址：192.168.1.149
    //小米15pro配置的ip地址：192.168.1.20


    public static final String BASE_URL = "http://192.168.64.20:8080";

    // 资讯服务单独的基础地址（根据实际后端部署修改）
    public static final String ARTICLE_BASE_URL = "http://192.168.64.20:8080";

    // Opera API endpoint
    public static final String API_OPERA = BASE_URL + "/api/opera";

    // Music API endpoint
    public static final String API_MUSIC = BASE_URL + "/api/music";

    // Article API endpoint
    public static final String API_ARTICLE = ARTICLE_BASE_URL + "/api/article";


    // User-related endpoints
    public static final String SMS_LOGIN_URL = BASE_URL + "/users/sms-login";
    public static final String USER_FAVORITES_URL = BASE_URL + "/api/user/";
    

    


    
    
    // 帖子列表

    public static final String POSTS_URL = BASE_URL + "/api/posts";
    
    // 密码登录接口
    public static final String PASSWORD_LOGIN_URL = BASE_URL + "/users/login";
    public static final String SEND_SMS_URL = BASE_URL + "/users/send-sms";
    public static final String GET_USER_INFO_URL = BASE_URL + "/users/phone/";
    public static final String UPDATE_USER_INFO_URL = BASE_URL + "/users/";
    public static final String LOGOUT_URL = BASE_URL + "/users/logout";

    // Virtual human endpoint
    public static final String VIRTUAL_HUMANS_URL = BASE_URL + "/virtual-humans";

    // Request timeout (milliseconds)
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 10000;
    public static final int WRITE_TIMEOUT = 10000;
}
