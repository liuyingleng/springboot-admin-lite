# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

SpringBoot Admin Lite - 基于 Spring Boot 2.7.18 的轻量级后台权限管理系统，使用 JWT + Redis 实现无状态认证，采用 RBAC 权限模型。

## 常用命令

### 构建和运行
```bash
# 编译打包
mvn clean package

# 运行项目
mvn spring-boot:run

# 或运行 jar 包
java -jar target/springboot-admin-lite-1.0.0.jar
```

### 测试
```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=类名
```

### 数据库初始化
```bash
# 执行数据库脚本
mysql -u root -p < src/main/resources/db/schema.sql
```

## 核心架构

### 认证流程
1. 用户登录 → `AuthController.login()` 验证用户名密码
2. 生成 JWT Token → `JwtTokenProvider.generateToken()`
3. Token 和用户信息缓存到 Redis → `RedisUtil`
4. 后续请求通过 `JwtAuthenticationFilter` 验证 Token
5. 登出时将 Token 加入黑名单 → Redis Set 存储

### 权限控制
- RBAC 模型：用户(User) → 角色(Role) → 权限(Permission)
- 权限分为菜单权限和按钮权限（通过 `type` 字段区分）
- 使用 Spring Security 的 `@PreAuthorize` 注解进行方法级权限控制
- 权限数据缓存在 Redis 中，key 格式：`user:permissions:{userId}`

### Redis 缓存策略
- Token 缓存：`token:{token}` → 用户ID，过期时间 24 小时
- 用户信息：`user:info:{userId}` → UserInfoVO
- 用户权限：`user:permissions:{userId}` → Set<String>
- 用户菜单：`user:menus:{userId}` → List<MenuVO>
- Token 黑名单：`token:blacklist` → Set<String>

### 分层架构
```
Controller → Service → Mapper → Database
    ↓          ↓
   DTO        Entity
    ↓
   VO
```

- Controller：接收请求，参数校验，返回统一响应格式 `Result<T>`
- Service：业务逻辑处理，事务控制
- Mapper：MyBatis-Plus 数据访问，继承 `BaseMapper<T>`
- DTO：接收前端请求参数
- VO：返回给前端的视图对象
- Entity：数据库实体，使用 MyBatis-Plus 注解

## 关键配置

### JWT 配置
- Secret Key：`application.yml` 中的 `jwt.secret`（生产环境需修改）
- 过期时间：24 小时（86400000 毫秒）

### 数据库配置
- 使用 MyBatis-Plus，自动驼峰命名转换
- 逻辑删除字段：`deleted`（0=未删除，1=已删除）
- SQL 日志：开发环境输出到控制台

### API 文档
- 访问地址：http://localhost:8080/doc.html
- 使用 Knife4j（Swagger 增强版）

## 默认测试账号
- 用户名：`admin`
- 密码：`admin123`

## 添加新功能模块

1. 创建实体类 `entity/XxxEntity.java`（继承 MyBatis-Plus BaseEntity 或添加通用字段）
2. 创建 Mapper 接口 `mapper/XxxMapper.java`（继承 `BaseMapper<XxxEntity>`）
3. 创建 Service 接口和实现 `service/IXxxService.java` 和 `service/impl/XxxServiceImpl.java`
4. 创建 Controller `controller/XxxController.java`
5. 如需权限控制，在 Controller 方法上添加 `@PreAuthorize("hasAuthority('xxx:xxx')")`
