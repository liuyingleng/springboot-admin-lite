package com.admin.controller;

import com.admin.common.RedisConstants;
import com.admin.common.Result;
import com.admin.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 在线用户管理
 */
@Api(tags = "在线用户管理")
@RestController
@RequestMapping("/api/online")
public class OnlineUserController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisUtil redisUtil;

    @ApiOperation("获取在线用户列表")
    @GetMapping("/users")
    public Result<Map<String, Object>> getOnlineUsers() {
        String pattern = RedisConstants.ONLINE_USER_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        Map<String, Object> result = new HashMap<>();
        result.put("total", keys != null ? keys.size() : 0);
        result.put("users", keys);

        return Result.success(result);
    }

    @ApiOperation("获取在线用户数量")
    @GetMapping("/count")
    public Result<Long> getOnlineUserCount() {
        String pattern = RedisConstants.ONLINE_USER_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        long count = keys != null ? keys.size() : 0;
        return Result.success(count);
    }
}
