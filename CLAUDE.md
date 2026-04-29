# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Architecture Visualization Platform — reads 30+ enterprise internal Git repositories (SSH access) as its sole data source. No persistent database; all UI content is parsed in real-time from source files (XML, JSON, properties) in locally-synced repo copies.

Two core capabilities:
1. **Architecture visualization**: real-time dependency graph of Features ↔ DataSources
2. **Business-driven editing**: UI CRUD that auto-generates a Git commit + MR per change

See `design/architecture-design.md` for the full architecture design.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21 + Spring Boot 3.4.x |
| Frontend | TypeScript 5 + React 19 + Vite 6 |
| UI components | Ant Design 5 |
| Graph visualization | AntV G6 5 (dependency graphs) + AntV X6 2 (flow diagrams) |
| Charts | @ant-design/charts (AntV G2) |
| Frontend state | Zustand 5 + TanStack Query 5 |
| Backend build | Maven 3.9 |

## Project Structure

```
Architecture-Visualization/
├── backend/          # Spring Boot — Java source
├── frontend/         # React + Vite — TypeScript source
└── design/           # Architecture and design documents
```

## First-time Setup

```bash
bash setup-demo.sh    # Init demo git repos + generate application-demo.yml
cd frontend && npm install --cache /tmp/npm-fresh-cache   # if npm cache has root-owned files
```

## Commands

### Backend (Java 20 + Spring Boot 3.4.1)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo   # Start with demo repos (port 8080)
mvn spring-boot:run                                    # Start without repos (empty model)
mvn compile                                            # Compile only
mvn test                                               # Run all tests
mvn test -Dtest=ClassName#methodName                   # Run single test
mvn package -DskipTests                                # Build JAR
```

### Frontend (Vite 6 + React 18)
```bash
cd frontend
npm run dev          # Start dev server (port 5173, proxies /api and /ws to :8080)
npm run build        # Production build
./node_modules/.bin/tsc --noEmit   # TypeScript check
```

### Quick API verification
```bash
curl -X POST http://localhost:8080/api/v1/sync/trigger     # Sync + rebuild model
curl http://localhost:8080/api/v1/features                 # List features
curl http://localhost:8080/api/v1/datasources              # List datasources
curl http://localhost:8080/api/v1/graph/full               # Graph nodes+edges
```

## Key Architecture Notes

### No Database — In-Memory Model
The backend maintains a `InMemoryArchModel` rebuilt after every sync. All API queries hit memory, not files. On startup and after each 5-minute sync cycle (`SyncScheduler`), `ModelRebuildService` parses local repo copies and atomically replaces the model (guarded by `ReadWriteLock`).

### Two Domain Threads
- **Feature** → FeatureConfig → [DatabaseTable, I18nEntry, DataSourceRequirement]
- **DataSource** → SubCollection → BusinessMetric (multi-level nesting)
- A `DependencyLink` connects FeatureConfig requirements to specific BusinessMetrics across repos.

### Edit → MR Flow
Every UI edit goes through: `FileChangeGenerator` (translate business edit to file diff) → optional `AiToolAdapter` (Claude Code / opencode CLI for complex changes) → `MrService` (git branch + commit + push + MR).

### Repo Sync
`SyncScheduler` runs every 5 minutes. Git operations use `ProcessBuilder` + git CLI with SSH keys from host `~/.ssh`. Repo list and local base path are configured in `backend/src/main/resources/repos-config.yml`.

### WebSocket
Backend broadcasts sync lifecycle events (`SYNC_STARTED`, `SYNC_REPO_DONE`, `SYNC_ALL_DONE`, `MODEL_REBUILT`) over `/ws/sync-status`. Frontend subscribes via `useRealtimeSync` hook and calls TanStack Query global invalidate on `MODEL_REBUILT`.

## Configuration

`backend/src/main/resources/repos-config.yml` — list of all repos + branches + SSH settings.  
`backend/src/main/resources/application.yml` — port, AI tool CLI paths, config file location.

## Pending (Business Development Phase)

File → Domain mapping rules (Schema Mapping), cross-repo reference format, enterprise Git MR API, and per-field edit permissions are deferred to the business logic phase. See `design/architecture-design.md` § 10 for the full list.

## Contribution Rules

每次代码/配置/结构变更都必须同时产出以下三类产物，并放在**同一个 commit / PR** 中提交，缺一不可：

### 1. 代码变更
正常的功能/修复/重构代码。

### 2. README.md 同步
对应变更必须同步更新 `README.md`（以及存在的 `README.zh-CN.md`）：
- 新增模块、接口、页面 → 更新"功能概述" / "API 文档" / "项目结构"
- 变更技术栈版本 → 更新"技术栈"表
- 变更配置格式 → 更新"配置说明"
- 完成待办功能 → 将"开发状态"中对应条目打勾

### 3. 增量设计文档（仅新增功能/能力时）
新增模块、新增对外能力、对外接口或重大重构时，必须在 `design/` 目录下新增一份 `design/<feature>-design.md`，与既有 `design/architecture-design.md` 并列归档。

文档结构参考首份 MCP Server 设计文档（`design/mcp-server-design.md`）：
背景 / 决策 / 模块结构 / 契约 / 时序 / 配置 / 集成关系 / 安全 / 验证 / 后续工作。

要求：
- 即使是 stub 或骨架级实现，也要写出当前阶段的状态和后续工作清单，避免设计意图丢失
- 不要等到功能完美才写——先落一份 v0.x，随实现迭代更新版本号
- 设计文档进 git，作为团队共享的设计资产；不要依赖 `~/.claude/plans/` 之类仓外位置
