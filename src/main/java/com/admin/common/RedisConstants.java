package com.admin.common;

/**
 * Redis 键常量
 */
public class RedisConstants {

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "auth:token:";

    /**
     * 用户信息前缀
     */
    public static final String USER_INFO_PREFIX = "user:info:";

    /**
     * 用户权限前缀
     */
    public static final String USER_PERMISSIONS_PREFIX = "user:permissions:";

    /**
     * 用户角色前缀
     */
    public static final String USER_ROLES_PREFIX = "user:roles:";

    /**
     * 用户菜单前缀
     */
    public static final String USER_MENU_PREFIX = "user:menu:";

    /**
     * 在线用户前缀
     */
    public static final String ONLINE_USER_PREFIX = "online:user:";

    /**
     * Token 黑名单前缀
     */
    public static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";

    /**
     * 验证码前缀
     */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * Token 过期时间（秒）- 24小时
     */
    public static final long TOKEN_EXPIRE_TIME = 24 * 60 * 60;

    /**
     * 用户信息缓存过期时间（秒）- 30分钟
     */
    public static final long USER_INFO_EXPIRE_TIME = 30 * 60;

    /**
     * 权限缓存过期时间（秒）- 30分钟
     */
    public static final long PERMISSION_EXPIRE_TIME = 30 * 60;

    /**
     * 菜单缓存过期时间（秒）- 30分钟
     */
    public static final long MENU_EXPIRE_TIME = 30 * 60;

    /**
     * 验证码过期时间（秒）- 5分钟
     */
    public static final long CAPTCHA_EXPIRE_TIME = 5 * 60;
}
