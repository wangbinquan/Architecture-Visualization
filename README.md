# 架构可视化平台

以企业内部代码仓为唯一数据源，提供架构可视化与业务化架构编辑能力。

## 功能概述

| 能力 | 说明 |
|------|------|
| **架构可视化** | 实时从代码仓源码解析"特性"、"数据源"及其依赖关系，以图形化方式呈现 |
| **数据源管理** | 支持多层嵌套结构：数据源 → 子集合 → 业务指标 |
| **特性管理** | 特性 → 配置项 → 数据库表 / 国际化 / 数据源引用 |
| **依赖关系图** | 全量架构依赖图，可视化特性与数据源之间的引用关系 |
| **业务化编辑** | 界面增删改查，修改后自动生成 Git commit 并发起 MR |

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 20 · Spring Boot 3.4.1 · Maven |
| 前端 | TypeScript · React 18 · Vite 6 |
| UI 组件 | Ant Design 5 |
| 图形可视化 | AntV G6 5 |
| 前端状态 | Zustand 5 · TanStack Query 5 |

## 快速开始

### 前置条件

- Java 17+（已验证 Java 20）
- Node.js 18+
- Maven 3.9+
- Git（配置好 SSH 密钥以访问企业内部代码仓）

### 1. 初始化 Demo 环境

```bash
bash setup-demo.sh
```

脚本会：
- 在 `demo-repos/` 下初始化两个本地 Git 仓（含示例特性和数据源数据）
- 生成 `backend/src/main/resources/application-demo.yml`

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
# 服务地址: http://localhost:8080
# API 文档: http://localhost:8080/swagger-ui.html
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
# 访问地址: http://localhost:5173
```

### 4. 加载数据

打开浏览器访问 `http://localhost:5173`，点击顶部工具栏的同步按钮，触发首次代码仓同步。

## 项目结构

```
Architecture-Visualization/
├── backend/                        # Spring Boot 后端
│   └── src/main/java/com/archvis/
│       ├── sync/                   # 代码仓同步（git CLI + 5min 定时器）
│       ├── parser/                 # 文件解析（XML / JSON / properties）
│       ├── domain/                 # 领域模型（Feature、DataSourceDef 等）
│       ├── model/                  # 内存模型（无持久化 DB）
│       ├── editor/                 # 编辑提交（git commit + MR + AI工具接口）
│       ├── websocket/              # WebSocket 同步状态推送
│       └── api/                    # REST Controllers
├── frontend/                       # React 前端
│   └── src/
│       ├── pages/                  # Dashboard / 特性 / 数据源 / 关系图 / MR记录
│       ├── components/             # SyncStatus、ArchGraph（G6）
│       ├── services/               # API 客户端
│       ├── store/                  # Zustand 同步状态
│       └── hooks/                  # useRealtimeSync（WebSocket）
├── demo-repos/                     # 本地 Demo 数据（XML + JSON 示例文件）
├── design/                         # 架构设计文档
├── setup-demo.sh                   # Demo 环境初始化脚本
└── CLAUDE.md                       # Claude Code 工作指引
```

## 配置说明

### 代码仓配置

编辑 `backend/src/main/resources/application-demo.yml`（由 `setup-demo.sh` 生成）或自行创建 `application-{profile}.yml`：

```yaml
archvis:
  sync:
    interval-seconds: 300          # 同步频率（秒）
    local-base-path: ~/.archvis/repos  # 本地克隆目录
  repositories:
    - name: your-feature-repo
      url: git@your-git:org/repo.git
      branch: main
    - name: your-datasource-repo
      url: git@your-git:org/ds-repo.git
      branch: release/2.0
```

启动时指定 profile：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=your-profile
```

### 数据文件约定

| 路径模式 | 解析为 |
|---------|--------|
| `{repo}/features/**/*.xml` | Feature（特性） |
| `{repo}/datasources/**/*.json` | DataSourceDef（数据源） |

> 详细 Schema 映射规则在业务开发阶段细化，参见 `design/architecture-design.md` § 10。

## API 文档

后端启动后访问 `http://localhost:8080/swagger-ui.html` 查看完整接口文档。

主要接口：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/sync/trigger` | 手动触发全量同步 |
| GET | `/api/v1/features` | 特性列表 |
| GET | `/api/v1/datasources` | 数据源列表 |
| GET | `/api/v1/graph/full` | 完整依赖关系图数据 |

## 开发状态

- [x] 架构设计
- [x] 后端基础框架（同步、解析、内存模型、REST API、WebSocket）
- [x] 前端基础框架（Dashboard、特性、数据源、关系图、MR记录）
- [ ] 业务 Schema 映射（B-01）
- [ ] 企业 Git 平台 MR API 对接（B-07）
- [ ] 界面编辑提交完整流程
- [ ] AI 工具集成（Claude Code / opencode）
