# SpringBoot Admin Lite

轻量级后台权限管理系统 - 后端项目

## 技术栈

- Spring Boot 2.7.18 (兼容 JDK 8)
- Spring Security + JWT
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Redis (Lettuce)
- Knife4j (API 文档)
- Maven

## 功能特性

- ✅ JWT 无状态认证
- ✅ Redis 缓存（Token、用户信息、权限、菜单）
- ✅ Token 黑名单机制
- ✅ 在线用户管理
- ✅ RBAC 权限模型（用户-角色-权限）
- ✅ 用户管理
- ✅ 角色管理
- ✅ 权限管理（菜单+按钮）
- ✅ 统一响应格式
- ✅ 全局异常处理
- ✅ 跨域配置
- ✅ API 文档（Knife4j）

## 快速开始

### 1. 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+

### 2. 创建数据库

执行 `src/main/resources/db/schema.sql` 文件创建数据库和表：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 启动 Redis

```bash
# 启动 Redis 服务
redis-server

# 检查 Redis 状态
redis-cli ping
# 返回 PONG 表示正常
```

### 4. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/admin_lite?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的数据库密码
  
  redis:
    host: localhost
    port: 6379
    password:  # 如果 Redis 有密码，在这里配置
```

### 5. 启动项目
    username: root
    password: your_password  # 修改为你的数据库密码
```

### 4. 启动项目

```bash
# 编译
mvn clean package

# 运行
java -jar target/springboot-admin-lite-1.0.0.jar

# 或者直接运行
mvn spring-boot:run
```

启动成功后访问：
- API 文档：http://localhost:8080/doc.html

### 5. 测试登录

默认测试账号：
- 用户名：`admin`
- 密码：`admin123`

## API 接口

### 认证接口

#### 登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

响应：
```json
{
  "code": 200,
  "message": "登录成功",
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "nickname": "管理员"
    }
  }
}
```

#### 获取当前用户信息
```
GET /api/auth/info
Authorization: Bearer {token}
```

#### 登出
```
POST /api/auth/logout
Authorization: Bearer {token}
```

## 项目结构

```
src/main/java/com/admin/
├── AdminApplication.java       # 启动类
├── config/                     # 配置类
│   ├── SecurityConfig.java     # Security 配置
│   ├── CorsConfig.java         # 跨域配置
│   ├── SwaggerConfig.java      # API 文档配置
│   └── MybatisPlusConfig.java  # MyBatis-Plus 配置
├── controller/                 # 控制器
│   └── AuthController.java     # 认证控制器
├── service/                    # 业务接口
│   ├── IAuthService.java
│   └── impl/                   # 业务实现
│       └── AuthServiceImpl.java
├── mapper/                     # 数据访问层
│   ├── UserMapper.java
│   ├── RoleMapper.java
│   └── PermissionMapper.java
├── entity/                     # 实体类
│   ├── User.java
│   ├── Role.java
│   └── Permission.java
├── dto/                        # 数据传输对象
│   └── LoginDTO.java
├── vo/                         # 视图对象
│   ├── LoginVO.java
│   └── UserInfoVO.java
├── security/                   # 安全相关
│   ├── JwtTokenProvider.java  # JWT 工具类
│   ├── JwtAuthenticationFilter.java  # JWT 过滤器
│   └── UserDetailsServiceImpl.java   # 用户详情服务
├── common/                     # 公共类
│   ├── Result.java             # 统一响应
│   └── GlobalExceptionHandler.java  # 全局异常处理
└── utils/                      # 工具类
```

## 数据库设计

### 核心表

- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_permission` - 权限表
- `sys_user_role` - 用户角色关联表
- `sys_role_permission` - 角色权限关联表

## 前端对接

### 响应格式

所有接口统一返回格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": {},
  "extra": null
}
```

### 请求头

需要认证的接口需要在请求头中携带 token：

```
Authorization: Bearer {token}
```

## 开发指南

### 添加新接口

1. 在 `entity` 包创建实体类
2. 在 `mapper` 包创建 Mapper 接口
3. 在 `service` 包创建 Service 接口和实现
4. 在 `controller` 包创建 Controller

### 密码加密

使用 BCrypt 加密密码：

```java
@Autowired
private PasswordEncoder passwordEncoder;

String encodedPassword = passwordEncoder.encode("plainPassword");
```

### 权限控制

使用 `@PreAuthorize` 注解控制接口权限：

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/data")
public Result<String> getAdminData() {
    return Result.success("管理员数据");
}
```

## 许可证

MIT
