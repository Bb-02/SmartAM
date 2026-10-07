# SmartAM

SaaS 企业级资产清查与工单流转系统。面向中大型公司的固定资产管理后台，提供多租户、多分区、多部门的数据隔离，覆盖资产入库、领用、维修、报废的全生命周期管理，以及报修工单从提交到确认结单的完整流转闭环。

## 功能特性

- **多租户 SaaS**：公司自助注册，注册时自动创建默认分区（总分区）和租户管理员账号
- **四种角色 RBAC**：租户管理员（ADMIN_TENANT）/ 区域管理员（ADMIN_REGION）/ 工程师（ENGINEER）/ 员工（EMPLOYEE）
- **多级数据隔离**：公司 → 分区 → 部门 → 用户，接口按登录身份自动过滤数据，无需前端额外传参
- **资产全生命周期**：在库 / 使用中 / 维修中 / 已报废，状态流转全程记录日志（AssetLog）
- **资产申领审批**：申领（APPLY）/ 报废（SCRAP）/ 调拨（TRANSFER）三类申请单，含审批日志
- **报修工单流转**：提交 → 受理 → 处理 → 确认结单的完整状态机，支持受理、释放、驳回、取消
- **站内消息**：工单与申领的关键节点自动生成站内通知
- **统计看板**：资产与工单的汇总统计接口
- **工程化配套**：统一响应封装、全局异常处理、JWT 无状态认证、逻辑删除、审计字段自动填充

## 角色与数据隔离

```
公司（Tenant）
  └── 分区 A（Region）
        ├── 部门 1（Department，支持层级）
        │     └── 员工（EMPLOYEE）
        └── 工程师（ENGINEER）
```

| 角色 | 数据可见范围 | 核心能力 |
|------|-------------|---------|
| ADMIN_TENANT | 全公司 | 管分区、管所有管理员账号、查看全公司数据 |
| ADMIN_REGION | 本分区 | 管本区用户/部门/资产、驳回工单 |
| ENGINEER | 本分区 | 受理并处理本分区工单 |
| EMPLOYEE | 本部门 | 查看本部门资产、提交工单、确认结单 |

## 核心业务流程

### 资产生命周期

```
  [新资产入库]
      │
      ▼
  IN_STORAGE ──────► SCRAPPED（报废，终态）
  （在库/空闲）  ▲
      │ 分配     │ 归还
      ▼         │
  IN_USE ───────┴───► IN_REPAIR（报修中）
  （使用中）           │
      ▲               │ 修复完成
      └──────────── IN_USE
```

每次状态变更生成一条资产日志，可在资产详情中追溯。

### 报修工单状态机

```
  EMPLOYEE             ENGINEER            EMPLOYEE
  提交工单              受理+处理            确认
  ───────► PENDING ──► IN_WORK ──► RESOLVED ──► CLOSED
               ▲        │  │                    │
               │        │  │  管理员驳回         │ 员工驳回
               │        └──┼──► CLOSED          └──► IN_WORK
               │  release  │
               └───────────┘ 工程师可释放回待处理池
```

- 工单自动归属提交员工所在分区，工程师只能受理本分区的待处理工单
- 一个工单同一时间只绑定一名工程师

### 资产申领审批

申请类型：申领（APPLY）/ 报废（SCRAP）/ 调拨（TRANSFER）；
状态流转：`PENDING → APPROVED / REJECTED / CANCELLED`，全过程记录审批日志。

## 技术栈

| 分类 | 技术 |
|------|------|
| 语言 / 运行时 | Java 17 |
| 核心框架 | Spring Boot 3.4.5（Web、Security、Validation） |
| ORM | MyBatis-Plus 3.5.9（分页插件、逻辑删除、字段自动填充） |
| 认证 | Spring Security + JJWT 0.12.6（JWT 无状态认证） |
| 数据库 | MySQL（utf8mb4） |
| 构建 | Maven 多模块，自带 mvnw 包装器 |
| 其他 | Lombok |

## 项目结构

```
smartam/
├── pom.xml                    父 POM，集中管理依赖版本
├── smartam-common/            公共模块
│   └── 统一响应（ApiResponse）、全局异常处理、JWT 工具、基础实体
└── smartam-tenant/            业务模块
    └── src/main/java/com/chengmaomao/smartam/tenant/
        ├── config/            Spring Security 配置、JWT 过滤器、MyBatis-Plus 配置
        ├── controller/        REST 接口层（11 个控制器）
        ├── service/           业务逻辑层
        ├── mapper/            MyBatis-Plus 数据访问层
        ├── entity/            实体类、状态与角色常量
        └── dto/               请求 / 响应 DTO
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 5.7+（建议 8.0）
- Maven 无需单独安装，项目自带 `mvnw` 包装器

### 1. 初始化数据库

```sql
CREATE DATABASE smartam DEFAULT CHARACTER SET utf8mb4;
```

### 2. 配置本地环境

```bash
cd smartam-tenant/src/main/resources
cp application-local.yml.example application-local.yml
# 编辑 application-local.yml，填入数据库用户名和密码
```

默认激活 `local` profile，`application-local.yml` 已被 gitignore，敏感信息不会入库。

### 3. 构建并启动

```bash
# Linux / macOS
./mvnw spring-boot:run -pl smartam-tenant -am

# Windows
mvnw.cmd spring-boot:run -pl smartam-tenant -am
```

启动成功后服务运行在 `http://localhost:8080`。

### 4. 注册公司并登录

```bash
# 1. 注册公司（首次使用，公开接口）
curl -X POST http://localhost:8080/api/tenant/register \
  -H "Content-Type: application/json" \
  -d '{"companyName":"示例科技","username":"admin","password":"你的密码","realName":"管理员"}'

# 2. 登录获取 JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"你的密码"}'
```

登录成功后，后续请求在 Header 中携带 `Authorization: Bearer <token>`。

## API 概览

- **Base URL**：`http://localhost:8080`
- **认证方式**：JWT Bearer Token（公开接口：`/api/tenant/register`、`/api/auth/login`）
- **统一响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

| 模块 | 路由前缀 | 说明 |
|------|---------|------|
| 租户注册 | `/api/tenant` | 公司自助注册 |
| 认证 | `/api/auth` | 登录、修改密码、重置密码 |
| 用户管理 | `/api/users` | 用户 CRUD、个人信息 |
| 分区管理 | `/api/regions` | 分区（Region）CRUD、启停 |
| 部门管理 | `/api/departments` | 层级部门树 CRUD |
| 资产管理 | `/api/assets` | 资产 CRUD、状态流转、日志 |
| 资产申领 | `/api/asset-applications` | 申领/报废/调拨审批 |
| 工单管理 | `/api/work-orders` | 报修工单提交与流转 |
| 站内消息 | `/api/messages` | 消息列表、已读 |
| 数据字典 | `/api/dict` | 字典数据 |
| 统计 | `/api/statistics` | 资产/工单概览统计 |

## 配置说明

| 配置项 | 说明 |
|--------|------|
| `spring.profiles.active` | 默认 `local`，本地敏感配置放在 `application-local.yml`（不提交） |
| `jwt.secret` | JWT 签名密钥，生产环境通过环境变量 `JWT_SECRET` 覆盖 |
| `jwt.expiration-ms` | Token 有效期，默认 24 小时 |
| `server.port` | 服务端口，默认 8080 |
| `mybatis-plus.global-config.db-config` | 逻辑删除字段 `deleted`（1 删除 / 0 未删除） |
