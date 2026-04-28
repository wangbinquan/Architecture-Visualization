# 架构可视化平台 - 架构设计文档

> 版本：v1.0  
> 日期：2026-04-28  
> 状态：架构设计阶段（业务逻辑细节待后续阶段细化）

---

## 1. 项目概述

### 1.1 目标

架构可视化平台以企业内部多个代码仓为唯一数据源，提供两大核心能力：

| 能力 | 描述 |
|------|------|
| **架构可视化** | 实时从代码仓源码解析，将"特性"、"数据源"及其业务依赖关系以图形化方式呈现 |
| **业务化架构编辑** | 以业务语言呈现代码仓中的配置，支持界面增删改查，修改后自动生成代码变更并提交 MR |

### 1.2 核心约束

- **无持久化数据库**：所有展示内容均从代码仓源文件实时解析汇总，后端维护内存级缓存
- **数据源唯一性**：30+ 个企业内部代码仓（通过 SSH 访问）是唯一数据来源
- **每次编辑独立成 MR**：界面修改直接形成一个 Git 提交 + MR，不做临时缓存

---

## 2. 技术选型

### 2.1 版本清单

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 后端运行时 | Java | 21 (LTS) | Spring Boot 3.x 要求 Java 17+ |
| 后端框架 | Spring Boot | 3.4.x | 最新稳定版 |
| 前端框架 | React | 19.x | 最新稳定版 |
| 前端语言 | TypeScript | 5.x | |
| UI 组件库 | Ant Design | 5.x | 用户选定 |
| 图形可视化 | AntV G6 | 5.x | 与 antd 同生态，关系网络图 |
| 流程图编辑 | AntV X6 | 2.x | 可交互的架构流程图 |
| 数据图表 | AntV G2 / @ant-design/charts | 5.x | Dashboard 统计图表 |
| 前端构建 | Vite | 6.x | |
| 前端状态 | Zustand | 5.x | 轻量，适合中等复杂度状态 |
| 前端请求 | TanStack Query | 5.x | 缓存、轮询、乐观更新 |
| 后端构建 | Maven | 3.9.x | |

### 2.2 后端核心依赖

```xml
<!-- Spring Boot Starters -->
spring-boot-starter-web
spring-boot-starter-websocket       <!-- 实时同步状态推送 -->
spring-boot-starter-validation

<!-- 文件解析 -->
jackson-databind                    <!-- JSON 解析 -->
jackson-dataformat-xml              <!-- XML 解析 -->
<!-- Properties 使用 JDK 标准 java.util.Properties -->

<!-- 工具 -->
lombok
springdoc-openapi-starter-webmvc-ui <!-- API 文档 -->
commons-io                          <!-- 文件操作工具 -->
```

---

## 3. 系统架构

### 3.1 总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Browser)                            │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐  ┌──────────────┐ │
│  │ Dashboard │  │ 特性视图  │  │ 数据源视图  │  │ 依赖关系图   │ │
│  └──────────┘  └──────────┘  └────────────┘  └──────────────┘ │
│                    React 19 + TypeScript + Ant Design 5          │
└──────────────────────────┬───────────────┬───────────────────────┘
                           │ REST API      │ WebSocket
┌──────────────────────────▼───────────────▼───────────────────────┐
│                        后端 (Spring Boot 3.4)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────────┐ │
│  │  REST API   │  │  WebSocket  │  │   同步状态广播服务         │ │
│  │  Controllers│  │  Handler    │  │                          │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────────────────────┘ │
│         │                │                                        │
│  ┌──────▼────────────────▼──────────────────────────────────┐   │
│  │                    应用服务层                               │   │
│  │  FeatureService  │  DataSourceService  │  EditService     │   │
│  └──────┬───────────────────┬─────────────────────┬──────────┘  │
│         │                   │                     │              │
│  ┌──────▼──────┐  ┌─────────▼──────┐  ┌──────────▼──────────┐  │
│  │ 聚合与依赖   │  │   内存模型缓存   │  │   编辑提交模块       │  │
│  │ 图构建服务   │  │  (无持久化DB)   │  │  (Git + MR + AI)   │  │
│  └──────┬──────┘  └─────────┬──────┘  └──────────────────────┘  │
│         │                   │                                     │
│  ┌──────▼───────────────────▼──────────────────────────────────┐ │
│  │                      解析层 (Parser)                          │ │
│  │    XmlParser   │   JsonParser   │   PropertiesParser         │ │
│  └──────────────────────────┬───────────────────────────────────┘ │
│                             │                                     │
│  ┌──────────────────────────▼───────────────────────────────────┐ │
│  │                   代码仓同步层                                  │ │
│  │  SyncScheduler (5min)  │  GitCommandExecutor  │  本地文件系统  │ │
│  └──────────────────────────┬───────────────────────────────────┘ │
└─────────────────────────────┼─────────────────────────────────────┘
                              │ SSH / git CLI
┌─────────────────────────────▼─────────────────────────────────────┐
│              企业内部 Git（30+ 代码仓）                              │
│   repo-A/branch  │  repo-B/branch  │  ...  │  repo-N/branch       │
└────────────────────────────────────────────────────────────────────┘
```

### 3.2 内存模型策略（替代数据库）

每次同步完成后，后端触发全量解析，将结果构建为内存中的 Domain Object 树，所有 API 查询均读内存，不重新解析文件：

```
同步完成事件
    └──► 触发 ModelRebuildService.rebuild()
              ├── 遍历所有本地仓目录，调用 ParserFactory 解析文件
              ├── 构建 Feature 树、DataSource 树
              ├── 解析跨仓依赖引用
              └── 原子替换旧内存模型（读写分离，使用 ReadWriteLock）
```

**并发保护**：重建期间读请求继续使用旧模型，重建完成后原子切换，保证查询无感知。

---

## 4. 后端模块设计

### 4.1 模块划分

```
backend/
└── src/main/java/com/archvis/
    ├── sync/           # 代码仓同步模块
    ├── parser/         # 文件解析模块
    ├── domain/         # 领域模型
    ├── model/          # 内存模型与聚合
    ├── editor/         # 编辑提交模块
    ├── api/            # REST Controllers
    ├── websocket/      # WebSocket 推送
    └── config/         # Spring 配置类
```

### 4.2 sync — 代码仓同步模块

**职责**：读取配置文件中的仓列表，定时执行 git 拉取，更新本地副本。

```
sync/
├── RepoConfig.java          # 配置文件绑定 POJO
├── RepoSyncService.java     # 同步逻辑（clone / pull）
├── GitCommandExecutor.java  # 封装 git CLI 命令执行
└── SyncScheduler.java       # @Scheduled 5min 触发器
```

**同步流程**：

```
SyncScheduler.syncAll()
    ├── 遍历 repos-config.yml 中每个 repo
    ├── 若本地不存在 → git clone {url} -b {branch} {localPath}
    ├── 若本地已存在 → git -C {localPath} pull origin {branch}
    ├── 记录每个 repo 的同步时间与状态
    └── 全部完成 → 发布 RepoSyncCompletedEvent
```

**Git 命令执行策略**：使用 `ProcessBuilder` 执行 git CLI，SSH 密钥通过宿主机 `~/.ssh` 配置，`GIT_SSH_COMMAND` 环境变量注入。

### 4.3 parser — 文件解析模块

**职责**：提供统一的文件解析接口，屏蔽 XML/JSON/properties 格式差异。

```
parser/
├── FileParser.java              # 接口：parse(Path) → ParsedDocument
├── XmlFileParser.java           # Jackson XML 实现
├── JsonFileParser.java          # Jackson Databind 实现
├── PropertiesFileParser.java    # java.util.Properties 实现
├── ParserFactory.java           # 根据文件扩展名路由到对应 Parser
└── ParsedDocument.java          # 统一结构：Map<String, Object> 树
```

> **业务阶段细化**：具体哪些文件映射到哪些 Domain 对象，由业务开发阶段定义 Schema 映射规则。

### 4.4 domain — 领域模型

业务核心的两条主线及其关联：

```java
// 主线一：特性
Feature
  ├── id, name, description, sourceRepo
  └── configs: List<FeatureConfig>
          ├── id, name
          ├── databaseTables: List<DatabaseTable>
          │       └── tableName, columns...
          ├── i18nEntries: List<I18nEntry>
          │       └── key, defaultValue, locales...
          └── dataSourceRequirements: List<DataSourceRequirement>
                  └── → 引用 DataSource.SubCollection.BusinessMetric

// 主线二：数据源（多层嵌套）
DataSource
  ├── id, name, type, sourceRepo
  └── subCollections: List<SubCollection>
          ├── id, name
          └── businessMetrics: List<BusinessMetric>
                  └── id, name, dataType, description, properties...

// 跨实体依赖
DependencyLink
  ├── sourceType: FEATURE_CONFIG / DATASOURCE
  ├── sourceId
  ├── targetType
  ├── targetId
  └── linkType: REFERENCES_METRIC / ...
```

### 4.5 model — 内存模型与聚合

```
model/
├── InMemoryArchModel.java       # 全量内存模型（线程安全读写）
├── ModelRebuildService.java     # 监听 SyncCompletedEvent，触发重建
├── DependencyGraphBuilder.java  # 构建跨仓依赖关系图（邻接表）
└── ArchModelQueryService.java   # 提供查询 API（给 Controller 调用）
```

### 4.6 editor — 编辑提交模块

**职责**：将界面上的业务编辑操作转换为文件变更，提交到新分支并发起 MR。

```
editor/
├── EditRequest.java             # 编辑请求 DTO
├── FileChangeGenerator.java     # 业务修改 → 文件 diff（核心翻译层）
├── MrService.java               # 创建分支、commit、push、发起 MR
├── GitMrExecutor.java           # git 命令序列执行
└── ai/
    ├── AiToolAdapter.java       # 接口：generateChange(EditContext) → FileChange
    ├── ClaudeCodeAdapter.java   # 调用 claude CLI
    └── OpenCodeAdapter.java     # 调用 opencode CLI
```

**MR 提交流程**：

```
用户提交编辑
    │
    ▼
FileChangeGenerator.generate(editRequest)
    → 定位目标文件路径（基于 domain 模型反推）
    → 生成修改后的文件内容
    │
    ▼（可选：复杂变更委托 AI 工具）
AiToolAdapter.generateChange(context)
    → 调用外部 AI CLI 工具，获取精确文件变更
    │
    ▼
MrService.submitMr(repoLocalPath, changes)
    1. git checkout -b arch-vis/edit-{timestamp}-{shortDesc}
    2. 写入修改后的文件内容
    3. git add {changed files}
    4. git commit -m "[arch-vis] {businessDescription}"
    5. git push origin {branchName}
    6. 调用 Git 平台 MR 创建接口（预留抽象层，初期可输出 MR URL 供手动创建）
    │
    ▼
返回 MR 信息（分支名、提交 hash、MR URL）
```

**AI 工具集成接口**：

```java
public interface AiToolAdapter {
    /**
     * @param context 包含：文件路径、当前文件内容、业务变更描述
     * @return 修改后的完整文件内容
     */
    FileChange generateChange(EditContext context);
    String toolName(); // "claude-code" | "opencode"
}
```

> 初期可通过 `ProcessBuilder` 调用 CLI 工具，后续可扩展为 API 调用。

---

## 5. 前端模块设计

### 5.1 目录结构

```
frontend/src/
├── pages/
│   ├── dashboard/          # 总览 Dashboard
│   ├── features/           # 特性列表与详情（树形 + 表单编辑）
│   ├── datasources/        # 数据源列表与详情（多层嵌套树）
│   ├── visualization/      # 依赖关系图（G6/X6 图形）
│   └── mrs/                # MR 记录列表
├── components/
│   ├── ArchGraph/          # 基于 AntV G6 的依赖关系图组件
│   ├── DomainTree/         # 特性/数据源通用树形组件
│   ├── EditDrawer/         # 业务编辑侧滑抽屉（表单）
│   └── SyncStatus/         # 代码仓同步状态条
├── services/
│   ├── api.ts              # Axios 实例 + 基础配置
│   ├── featureApi.ts       # 特性相关请求
│   ├── datasourceApi.ts    # 数据源相关请求
│   └── editorApi.ts        # 编辑提交相关请求
├── store/
│   ├── syncStore.ts        # 同步状态（WebSocket 更新）
│   └── modelStore.ts       # 本地缓存的内存模型快照
└── hooks/
    ├── useRealtimeSync.ts  # 订阅 WebSocket 同步事件
    └── useEditorMutation.ts # 编辑提交封装
```

### 5.2 页面与视图规划

#### Dashboard（总览）
- 代码仓同步状态卡片（最近同步时间、状态）
- 特性数量、数据源数量统计
- 最近 MR 列表

#### 特性视图（`/features`）
```
左侧：特性树（Ant Design Tree）
  └── 特性 A
        ├── 配置项 1
        │     ├── 数据库表 (2)
        │     ├── 国际化 (5)
        │     └── 数据源依赖 (3) → 点击展示引用的具体指标
        └── 配置项 2

右侧：选中节点的详情面板 + 编辑按钮
  └── 点击"编辑" → 打开 EditDrawer（业务语言表单）
```

#### 数据源视图（`/datasources`）
```
左侧：数据源嵌套树
  └── 数据源 A
        └── 子集合 1
              ├── 业务指标 α (dataType, description)
              └── 业务指标 β

右侧：选中指标的完整属性面板 + 编辑按钮
  └── 同时展示"被哪些特性引用"反向依赖
```

#### 依赖关系图（`/visualization`）
使用 **AntV G6** 渲染交互式关系图：
- 节点类型：Feature（特性）、FeatureConfig（配置项）、DataSource（数据源）、SubCollection、BusinessMetric
- 边类型：包含关系（树边）、引用关系（依赖边，不同颜色区分）
- 支持：节点筛选、路径高亮、缩放平移、点击跳转详情

#### MR 记录（`/mrs`）
- 本次会话内提交的 MR 列表（分支名、变更描述、提交时间、状态）
- 支持复制 MR URL

### 5.3 实时同步状态

通过 **WebSocket** 订阅后端同步事件：
```
ws://backend/ws/sync-status

消息格式：
{
  "event": "SYNC_STARTED" | "SYNC_REPO_DONE" | "SYNC_ALL_DONE" | "MODEL_REBUILT",
  "repoName": "...",
  "timestamp": "...",
  "summary": { "total": 32, "success": 32, "failed": 0 }
}
```

前端在顶部常驻状态栏显示同步进度，`MODEL_REBUILT` 事件触发 TanStack Query 全局 invalidate，页面数据自动刷新。

---

## 6. 配置文件规范

### 6.1 代码仓配置（`repos-config.yml`）

```yaml
# 放置于 backend/src/main/resources/ 或通过外部配置挂载
sync:
  interval-seconds: 300           # 同步间隔（5分钟）
  local-base-path: /var/archvis/repos  # 本地克隆根目录
  git-ssh-key-path: ~/.ssh/id_rsa      # SSH 私钥路径（可选，默认使用系统配置）

repositories:
  - name: repo-features-core
    url: git@internal-git:company/features-core.git
    branch: main

  - name: repo-datasource-finance
    url: git@internal-git:company/datasource-finance.git
    branch: release/2.0

  # ... 最多 30+ 个
```

### 6.2 应用配置（`application.yml`）

```yaml
server:
  port: 8080

archvis:
  repos-config: classpath:repos-config.yml
  ai-tools:
    enabled: true
    default-tool: claude-code      # claude-code | opencode
    claude-code-cli-path: /usr/local/bin/claude
    opencode-cli-path: /usr/local/bin/opencode
```

---

## 7. REST API 高层设计

所有接口以 `/api/v1` 为前缀，使用 `springdoc-openapi` 自动生成文档（`/swagger-ui.html`）。

### 7.1 同步管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/sync/status` | 获取所有仓最近同步状态 |
| POST | `/sync/trigger` | 手动触发一次全量同步 |
| POST | `/sync/trigger/{repoName}` | 手动触发单仓同步 |

### 7.2 特性查询

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/features` | 特性列表（支持分页/搜索） |
| GET | `/features/{id}` | 特性详情（含所有配置项） |
| GET | `/features/{id}/dependencies` | 该特性引用的所有数据源指标 |

### 7.3 数据源查询

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/datasources` | 数据源列表 |
| GET | `/datasources/{id}` | 数据源详情（含子集合 + 业务指标） |
| GET | `/datasources/{id}/referenced-by` | 引用该数据源指标的特性列表 |

### 7.4 依赖关系图

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/graph/full` | 完整依赖关系图数据（节点 + 边） |
| GET | `/graph/feature/{id}` | 以某特性为中心的局部图 |
| GET | `/graph/datasource/{id}` | 以某数据源为中心的局部图 |

### 7.5 编辑提交

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/editor/preview` | 预览文件变更（不提交） |
| POST | `/editor/submit` | 提交变更，生成 commit + MR |
| GET | `/editor/mrs` | 本次会话提交的 MR 列表 |

---

## 8. 项目工程结构

```
Architecture-Visualization/
├── backend/                        # Spring Boot 工程
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/archvis/
│       │   ├── ArchVisApplication.java
│       │   ├── sync/
│       │   ├── parser/
│       │   ├── domain/
│       │   ├── model/
│       │   ├── editor/
│       │   ├── api/
│       │   ├── websocket/
│       │   └── config/
│       └── resources/
│           ├── application.yml
│           └── repos-config.yml
├── frontend/                       # React + Vite 工程
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── pages/
│       ├── components/
│       ├── services/
│       ├── store/
│       └── hooks/
├── design/                         # 设计文档
│   └── architecture-design.md
└── CLAUDE.md
```

---

## 9. 关键设计决策说明

### 9.1 无数据库 → 内存模型 + 文件系统

```
存储层级：
  企业 Git（远程）
      ↓ 5min git pull
  本地文件系统（/var/archvis/repos/）    ← 持久化层（重启可恢复）
      ↓ 同步完成后全量解析
  JVM 内存（InMemoryArchModel）          ← 查询层（毫秒级响应）
```

应用重启后会触发一次同步 + 重建，恢复时间约等于一次全量解析时间。

### 9.2 MR 创建的 Git 平台抽象

由于企业内部 Git 平台 API 尚未确定，设计预留抽象层：

```java
public interface MrPlatformClient {
    MrResult createMr(String repoUrl, String sourceBranch, String title, String description);
}

// 初期实现：命令行输出 MR 链接，等待用户手动创建
// 后续可接入：GitLab API / Gitea API / 内部 Git 平台 API
```

### 9.3 AI 工具集成设计原则

- AI 工具为**可选增强**，不影响主流程。简单字段修改走 `FileChangeGenerator` 直接生成；复杂结构变更（如新增特性）可委托 AI 工具生成文件内容。
- AI 工具通过标准输入/输出与 CLI 交互，后端通过 `ProcessBuilder` 调用，超时设置为 60s。

---

## 10. 待业务开发阶段细化的事项

以下内容在本次架构设计中预留接口，不做具体实现，留待业务逻辑开发阶段细化：

| 编号 | 待细化内容 |
|------|-----------|
| B-01 | 各代码仓中具体文件路径与 Domain 对象的映射规则（Schema Mapping） |
| B-02 | XML / JSON / properties 文件中各字段与 Feature、DataSource 领域属性的对应关系 |
| B-03 | 特性配置中"数据源引用"的具体表达方式（文件中如何存储引用关系） |
| B-04 | 数据源"子集合"与"业务指标"的完整属性定义 |
| B-05 | 国际化（i18n）条目的文件存储格式与键值规范 |
| B-06 | 数据库表配置的具体字段结构 |
| B-07 | 企业内部 Git 平台的 MR 创建 API 接口 |
| B-08 | 编辑权限控制（哪些字段可在界面编辑，哪些只读） |
| B-10 | 依赖关系图的节点过滤与分组展示策略 |
