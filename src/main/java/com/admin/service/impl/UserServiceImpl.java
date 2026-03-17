package com.admin.service.impl;

import com.admin.dto.UserCreateDTO;
import com.admin.dto.UserPageDTO;
import com.admin.dto.UserUpdateDTO;
import com.admin.entity.User;
import com.admin.entity.UserRole;
import com.admin.mapper.RoleMapper;
import com.admin.mapper.UserMapper;
import com.admin.mapper.UserRoleMapper;
import com.admin.service.IPermissionService;
import com.admin.service.IUserService;
import com.admin.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IPermissionService permissionService;

    @Override
    public Page<UserVO> getPage(UserPageDTO dto) {
        Page<User> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime);

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(User::getUsername, dto.getKeyword())
                    .or().like(User::getEmail, dto.getKeyword())
                    .or().like(User::getNickname, dto.getKeyword())
            );
        }
        if (dto.getStatus() != null) {
            wrapper.eq(User::getStatus, dto.getStatus());
        }

        userMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserVO> records = page.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            // 查询角色
            List<Long> roleIds = ((PermissionServiceImpl) permissionService).getUserRoleIds(user.getId());
            if (!roleIds.isEmpty()) {
                List<String> roleCodes = roleMapper.selectBatchIds(roleIds)
                        .stream().map(r -> r.getRoleCode()).collect(Collectors.toList());
                vo.setRoles(roleCodes);
            }
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional
    public void create(UserCreateDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);

        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), dto.getRoleIds());
        }
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");

        BeanUtils.copyProperties(dto, user, "roleIds");
        userMapper.updateById(user);

        // 更新角色
        if (dto.getRoleIds() != null) {
            userRoleMapper.delete(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id)
            );
            if (!dto.getRoleIds().isEmpty()) {
                assignRoles(id, dto.getRoleIds());
            }
        }

        // 清除缓存
        permissionService.clearUserPermissionCache(id);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id)
        );
        permissionService.clearUserPermissionCache(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
        permissionService.clearUserPermissionCache(id);
    }

    @Override
    public void resetPassword(Long id) {
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode("Admin@123"));
        userMapper.updateById(user);
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        roleIds.forEach(roleId -> {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        });
    }
}
