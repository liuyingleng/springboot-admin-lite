package com.admin.service.impl;

import com.admin.common.RedisConstants;
import com.admin.dto.LoginDTO;
import com.admin.entity.User;
import com.admin.mapper.UserMapper;
import com.admin.security.JwtTokenProvider;
import com.admin.service.IAuthService;
import com.admin.service.IPermissionService;
import com.admin.utils.RedisUtil;
import com.admin.vo.LoginVO;
import com.admin.vo.MenuVO;
import com.admin.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务实现（集成 Redis）
 */
@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IPermissionService permissionService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 token（会自动存入 Redis）
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        // 获取用户信息
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));

        // 构建用户信息 VO
        UserInfoVO userInfo = buildUserInfo(user);

        // 缓存用户信息到 Redis
        String userInfoKey = RedisConstants.USER_INFO_PREFIX + user.getId();
        redisUtil.set(userInfoKey, userInfo, RedisConstants.USER_INFO_EXPIRE_TIME);

        // 记录在线用户
        String onlineKey = RedisConstants.ONLINE_USER_PREFIX + user.getUsername();
        redisUtil.set(onlineKey, user.getId(), RedisConstants.TOKEN_EXPIRE_TIME);

        return new LoginVO(token, userInfo);
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        // 先从 Redis 获取
        String userInfoKey = RedisConstants.USER_INFO_PREFIX + user.getId();
        Object cache = redisUtil.get(userInfoKey);
        if (cache != null) {
            return (UserInfoVO) cache;
        }

        // 构建用户信息
        UserInfoVO userInfo = buildUserInfo(user);

        // 缓存到 Redis
        redisUtil.set(userInfoKey, userInfo, RedisConstants.USER_INFO_EXPIRE_TIME);

        return userInfo;
    }

    @Override
    public void logout(String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // 删除 token（会加入黑名单）
        jwtTokenProvider.deleteToken(username, token);

        // 删除在线用户记录
        String onlineKey = RedisConstants.ONLINE_USER_PREFIX + username;
        redisUtil.del(onlineKey);

        // 获取用户ID并清除缓存
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user != null) {
            String userInfoKey = RedisConstants.USER_INFO_PREFIX + user.getId();
            redisUtil.del(userInfoKey);
            
            // 清除权限缓存
            permissionService.clearUserPermissionCache(user.getId());
        }
    }

    @Override
    public List<MenuVO> getUserMenu() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        return permissionService.getUserMenuTree(user.getId());
    }

    /**
     * 构建用户信息 VO
     */
    private UserInfoVO buildUserInfo(User user) {
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());

        // 获取用户角色和权限
        List<String> roles = permissionService.getUserRoles(user.getId());
        List<String> permissions = permissionService.getUserPermissions(user.getId());

        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        return userInfo;
    }
}
