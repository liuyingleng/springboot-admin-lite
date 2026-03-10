# Redis 集成说明

## 📦 Redis 使用场景

本项目已完整集成 Redis，用于以下场景：

### 1. Token 管理
- **存储位置**：`auth:token:{username}`
- **过期时间**：24 小时
- **用途**：
  - 存储用户的 JWT token
  - 支持单点登录（同一用户只能有一个有效 token）
  - 登出时删除 token

### 2. Token 黑名单
- **存储位置**：`auth:blacklist:{token}`
- **过期时间**：token 剩余有效期
- **用途**：
  - 登出时将 token 加入黑名单
  - 防止已登出的 token 继续使用
  - token 刷新时将旧 token 加入黑名单

### 3. 用户信息缓存
- **存储位置**：`user:info:{userId}`
- **过期时间**：30 分钟
- **用途**：
  - 缓存用户基本信息
  - 减少数据库查询
  - 提高接口响应速度

### 4. 用户权限缓存
- **存储位置**：`user:permissions:{userId}`
- **过期时间**：30 分钟
- **用途**：
  - 缓存用户权限列表
  - 快速权限校验
  - 减少数据库关联查询

### 5. 用户角色缓存
- **存储位置**：`user:roles:{userId}`
- **过期时间**：30 分钟
- **用途**：
  - 缓存用户角色列表
  - 快速角色校验

### 6. 用户菜单缓存
- **存储位置**：`user:menu:{userId}`
- **过期时间**：30 分钟
- **用途**：
  - 缓存用户菜单树
  - 前端动态菜单生成
  - 减少菜单查询开销

### 7. 在线用户管理
- **存储位置**：`online:user:{username}`
- **过期时间**：24 小时
- **用途**：
  - 记录在线用户
  - 统计在线用户数量
  - 强制用户下线

## 🔧 Redis 配置

### application.yml

```yaml
spring:
  redis:
    host: localhost        # Redis 地址
    port: 6379            # Redis 端口
    password:             # Redis 密码（如果有）
    database: 0           # 数据库索引
    timeout: 5000ms       # 连接超时时间
    lettuce:
      pool:
        max-active: 8     # 最大连接数
        max-wait: -1ms    # 最大等待时间
        max-idle: 8       # 最大空闲连接
        min-idle: 0       # 最小空闲连接
```

## 📝 Redis Key 设计规范

### Key 命名规则
```
{业务模块}:{功能}:{标识}
```

### 示例
```
auth:token:admin              # 用户 admin 的 token
user:info:1                   # 用户ID为1的信息
user:permissions:1            # 用户ID为1的权限
user:menu:1                   # 用户ID为1的菜单
online:user:admin             # 在线用户 admin
auth:blacklist:eyJhbGc...    # token 黑名单
```

## 🚀 使用示例

### 1. 登录流程

```java
@Override
public LoginVO login(LoginDTO loginDTO) {
    // 1. 认证
    Authentication authentication = authenticationManager.authenticate(...);
    
    // 2. 生成 token 并存入 Redis
    String token = jwtTokenProvider.generateToken(userDetails);
    // Redis: SET auth:token:admin {token} EX 86400
    
    // 3. 缓存用户信息
    redisUtil.set(userInfoKey, userInfo, RedisConstants.USER_INFO_EXPIRE_TIME);
    // Redis: SET user:info:1 {userInfo} EX 1800
    
    // 4. 记录在线用户
    redisUtil.set(onlineKey, user.getId(), RedisConstants.TOKEN_EXPIRE_TIME);
    // Redis: SET online:user:admin 1 EX 86400
    
    return new LoginVO(token, userInfo);
}
```

### 2. Token 验证流程

```java
@Override
public boolean validateToken(String token, UserDetails userDetails) {
    String username = getUsernameFromToken(token);
    
    // 1. 检查 token 是否在黑名单
    String blacklistKey = RedisConstants.TOKEN_BLACKLIST_PREFIX + token;
    if (redisUtil.hasKey(blacklistKey)) {
        return false;  // token 已失效
    }
    
    // 2. 检查 Redis 中的 token 是否匹配
    String redisKey = RedisConstants.TOKEN_PREFIX + username;
    Object redisToken = redisUtil.get(redisKey);
    
    return token.equals(redisToken) && !isTokenExpired(token);
}
```

### 3. 登出流程

```java
@Override
public void logout(String token) {
    String username = authentication.getName();
    
    // 1. 删除 Redis 中的 token
    redisUtil.del(RedisConstants.TOKEN_PREFIX + username);
    // Redis: DEL auth:token:admin
    
    // 2. 将 token 加入黑名单
    String blacklistKey = RedisConstants.TOKEN_BLACKLIST_PREFIX + token;
    redisUtil.set(blacklistKey, "1", remainingTime / 1000);
    // Redis: SET auth:blacklist:{token} 1 EX {剩余时间}
    
    // 3. 删除在线用户记录
    redisUtil.del(RedisConstants.ONLINE_USER_PREFIX + username);
    // Redis: DEL online:user:admin
    
    // 4. 清除用户缓存
    redisUtil.del(RedisConstants.USER_INFO_PREFIX + userId);
    permissionService.clearUserPermissionCache(userId);
}
```

### 4. 获取用户菜单

```java
@Override
public List<MenuVO> getUserMenuTree(Long userId) {
    // 1. 先从 Redis 获取
    String key = RedisConstants.USER_MENU_PREFIX + userId;
    Object cache = redisUtil.get(key);
    if (cache != null) {
        return (List<MenuVO>) cache;  // 缓存命中
    }
    
    // 2. 从数据库查询
    List<MenuVO> menuTree = buildMenuTree(...);
    
    // 3. 存入 Redis
    redisUtil.set(key, menuTree, RedisConstants.MENU_EXPIRE_TIME);
    // Redis: SET user:menu:1 {menuTree} EX 1800
    
    return menuTree;
}
```

## 🔍 Redis 监控命令

### 查看所有 key
```bash
redis-cli KEYS *
```

### 查看特定前缀的 key
```bash
redis-cli KEYS "auth:token:*"
redis-cli KEYS "user:info:*"
redis-cli KEYS "online:user:*"
```

### 查看 key 的值
```bash
redis-cli GET "auth:token:admin"
redis-cli GET "user:info:1"
```

### 查看 key 的过期时间
```bash
redis-cli TTL "auth:token:admin"
```

### 删除 key
```bash
redis-cli DEL "auth:token:admin"
```

### 清空所有数据（慎用）
```bash
redis-cli FLUSHDB
```

## 📊 性能优化建议

### 1. 缓存预热
系统启动时预加载常用数据到 Redis：
- 系统配置
- 数据字典
- 常用菜单

### 2. 缓存更新策略
- **用户信息变更**：清除 `user:info:{userId}`
- **角色权限变更**：清除所有相关用户的权限缓存
- **菜单变更**：清除所有用户的菜单缓存

### 3. 缓存穿透防护
对于不存在的数据，缓存空值（短时间）：
```java
if (user == null) {
    redisUtil.set(key, "", 60);  // 缓存空值 60 秒
}
```

### 4. 缓存雪崩防护
设置随机过期时间：
```java
long expireTime = RedisConstants.USER_INFO_EXPIRE_TIME + random.nextInt(300);
redisUtil.set(key, value, expireTime);
```

## ⚠️ 注意事项

### 1. Redis 连接
确保 Redis 服务已启动：
```bash
# 启动 Redis
redis-server

# 检查 Redis 状态
redis-cli ping
# 返回 PONG 表示正常
```

### 2. 密码配置
如果 Redis 设置了密码，需要在 `application.yml` 中配置：
```yaml
spring:
  redis:
    password: your_redis_password
```

### 3. 生产环境建议
- 使用 Redis 集群
- 配置持久化（RDB + AOF）
- 设置最大内存限制
- 配置内存淘汰策略
- 监控 Redis 性能

### 4. 缓存一致性
- 更新数据库后及时清除缓存
- 使用分布式锁防止缓存击穿
- 考虑使用 Canal 监听数据库变更

## 🛠️ 工具类使用

### RedisUtil 常用方法

```java
// String 操作
redisUtil.set(key, value);                    // 设置值
redisUtil.set(key, value, time);              // 设置值并指定过期时间
redisUtil.get(key);                           // 获取值
redisUtil.del(key);                           // 删除
redisUtil.hasKey(key);                        // 判断是否存在

// Hash 操作
redisUtil.hset(key, item, value);             // 设置 hash 值
redisUtil.hget(key, item);                    // 获取 hash 值
redisUtil.hmget(key);                         // 获取所有 hash 值
redisUtil.hdel(key, item);                    // 删除 hash 值

// Set 操作
redisUtil.sSet(key, values);                  // 添加到 set
redisUtil.sGet(key);                          // 获取 set 所有值
redisUtil.sHasKey(key, value);                // 判断 set 中是否存在

// List 操作
redisUtil.lSet(key, value);                   // 添加到 list
redisUtil.lGet(key, start, end);              // 获取 list 范围值
redisUtil.lGetListSize(key);                  // 获取 list 长度
```

## 📚 相关文档

- [Redis 官方文档](https://redis.io/documentation)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Lettuce 文档](https://lettuce.io/)
