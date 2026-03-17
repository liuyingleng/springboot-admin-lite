package com.admin.service;

import com.admin.dto.UserCreateDTO;
import com.admin.dto.UserPageDTO;
import com.admin.dto.UserUpdateDTO;
import com.admin.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface IUserService {

    Page<UserVO> getPage(UserPageDTO dto);

    void create(UserCreateDTO dto);

    void update(Long id, UserUpdateDTO dto);

    void remove(Long id);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id);
}
