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
