package com.admin.service;

import com.admin.dto.LoginDTO;
import com.admin.vo.LoginVO;
import com.admin.vo.MenuVO;
import com.admin.vo.UserInfoVO;

import java.util.List;

/**
 * 认证服务接口
 */
public interface IAuthService {
    
    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);
    
    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUserInfo();
    
    /**
     * 用户登出
     */
    void logout(String token);
    
    /**
     * 获取用户菜单
     */
    List<MenuVO> getUserMenu();
}
