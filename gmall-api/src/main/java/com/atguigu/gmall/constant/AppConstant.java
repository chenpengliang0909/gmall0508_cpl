package com.atguigu.gmall.constant;

/**
 * Created by Administrator on 2018/9/5.
 * 定义常量类
 */
public class AppConstant {

    /** 专门用来给浏览器返回的写成json格式的对象  成功时 msg属性值  */
    public static final String SUC_MSG = "操作成功";

    /** 专门用来给浏览器返回的写成json格式的对象  失败时  msg属性值  */
    public static final String FAIL_MSG = "操作失败";

    public static final String FILE_SERVER_URL = "http://192.168.74.200";

    /** 用户登录成功时，生成token时的key */
    public static final String TOKEN_KEY = "atguigugmall0508";

    /** 用户登录成功时，保存用户信息到Redis中的 key 前缀*/
    public static final String USER_KEY_PREFIX_REDIS = "user:";

    /** 用户登录成功时，保存用户信息到Redis中的 key 后缀*/
    public static final String USER_KEY_SUFFIX_REDIS = ":info";


    /** 数据保存到Redis和cookie中的过期时间:一天 24小时  */
    public static final Integer EXPIRED_TIME = 3600*24;


    /** 用户token验证通过后，将token保存到cookie中的名称  */
    public static final String COOKIE_NAME_TOKEN = "oldToken";

    /** 用户没有登录时，将购物车数据保存在cookie中，对应的cookieName  */
    public static final String COOKIE_NAME_CART = "listCartCookie";


}
