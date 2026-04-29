# MCP Server 增量设计文档

> 版本：v0.1
> 日期：2026-04-29
> 状态：首版已落地（查询工具 + 编辑工具骨架），鉴权/IP 白名单待补
> 关联：`architecture-design.md` § 6（编辑流程）/ § 8（API 层）

---

## 1. 背景与目标

平台后续会作为 AI Agent（Claude Code、opencode 等）的数据底座，让 Agent 直接：

1. **查询架构数据**——features / datasources / 依赖图 / 同步状态
2. **触发代码编辑提交**——生成 git 分支 + commit + push，未来对接 MR 平台

REST API 对 Agent 不友好：Agent 需要标准化的工具发现 (`tools/list`) 与调用 (`tools/call`)、强类型 JSON Schema、流式结果支持。**Model Context Protocol (MCP)** 是 Anthropic 开放的标准协议，已被 Claude Code 等主流 Agent 客户端原生支持。

**目标**：在现有 Spring Boot 后端内嵌 MCP Server，复用既有 Service 层，零侵入暴露给 Agent。

## 2. 关键决策

| 决策项 | 选择 | 备选 | 理由 |
|--------|------|------|------|
| 协议传输 | **Streamable HTTP** | SSE（legacy） / stdio（仅本地 CLI） | MCP 2025-03-26+ 现行标准；与现有 8080 端口共用，远程 Agent 可直连 |
| 集成方式 | **Spring AI starter** (`spring-ai-starter-mcp-server-webmvc`) | 裸 SDK `io.modelcontextprotocol.sdk:mcp` | 注解式 `@Tool` 暴露，自动生成 JSON Schema，样板代码最少 |
| 鉴权 | **首版不加** | API Key / OAuth2 JWT | 当前 demo / 内网阶段；通过 `archvis.mcp.enabled` 开关控制；生产前补 |
| 工具范围 | **查询 + 编辑提交** | + sync trigger | 不暴露 sync 触发，避免 Agent 误操作引发集群级副作用 |
| 部署形态 | **与主进程同体** | 独立微服务 | 无独立进程开销；和 InMemoryArchModel 同生命周期，无序列化跨进程 |

## 3. 模块结构

```
backend/src/main/java/com/archvis/mcp/
├── McpProperties.java              # @ConfigurationProperties(archvis.mcp)
├── McpServerConfig.java            # @Configuration: ToolCallbackProvider Bean
└── tool/
    ├── ArchitectureQueryTools.java # 9 个只读 @Tool（features/datasources/graph/sync）
    └── EditorTools.java            # 3 个编辑 @Tool（preview/submit/list MRs）
```

**职责划分**：

- **McpProperties**：`enabled` 开关，唯一作用是支持运行时关闭 MCP（REST API 不受影响）
- **McpServerConfig**：用 `@ConditionalOnProperty(... matchIfMissing = true)` 守门；构造 `MethodToolCallbackProvider`，把工具对象注册给 Spring AI 自动配置的 MCP Server
- **ArchitectureQueryTools**：只读，注入 `ArchModelQueryService` + `RepoSyncService` + `ReposConfigProperties`，不复制业务逻辑
- **EditorTools**：当前与 `EditorController` 并行，共用 stub 行为；待 B-01 Schema Mapping 完成后两者一起重构到 `EditorService`

## 4. 工具契约

### 4.1 查询工具（9 个）

| 工具名 | 输入 | 输出 | 委托 |
|--------|------|------|------|
| `list_features` | `search?: string` | `List<Feature>` | `ArchModelQueryService.getAllFeatures()` + 子串过滤 |
| `get_feature` | `id: string` | `Feature` | `getFeatureById(id)`（不存在抛 IllegalArgumentException） |
| `get_feature_dependencies` | `id: string` | `List<DependencyLink>` | `getDependenciesForFeature(id)` |
| `list_datasources` | `search?: string` | `List<DataSourceDef>` | `getAllDataSources()` + 子串过滤 |
| `get_datasource` | `id: string` | `DataSourceDef` | `getDataSourceById(id)` |
| `get_datasource_referenced_by` | `id: string` | `List<DependencyLink>` | `getReferencesForDataSource(id)` |
| `get_full_graph` | — | `GraphData` | `getFullGraph()` |
| `get_feature_graph` | `id: string` | `GraphData` | `getFullGraph()` 后按 `id` 过滤（与 `GraphController.featureGraph` 同源） |
| `get_sync_status` | — | `Map<String, Object>` | 复用 `SyncController.GET /status` 的结构 |

### 4.2 编辑工具（3 个）

| 工具名 | 输入 | 输出 | 当前行为 |
|--------|------|------|----------|
| `preview_edit` | entityType / entityId / field / oldValue? / newValue | `String`（占位描述） | 返回 placeholder，与 `EditorController.preview` 一致 |
| `submit_edit` | entityType / entityId / field / oldValue? / newValue / description? / useAiTool? | `EditResult` | **B-01 完成前为 stub**——返回 `success=false, message="Edit submission requires schema mapping (B-01)"`；MR 仍记入会话内存 |
| `list_recent_mrs` | — | `List<EditResult>` | 返回 MCP 会话内的 MR 历史 |

> **stub 不掩盖**：MCP 工具如实透传 `success=false`，Agent 看到的状态等同于人通过 REST 调用看到的状态。这是显式的设计选择，避免给 Agent 错觉。

## 5. 集成时序

```
Agent (Claude Code)
   │ POST /mcp  jsonrpc tools/call  list_features
   ▼
spring-ai-starter-mcp-server-webmvc (auto-configured)
   │ 解析 MCP 帧 → JSON Schema 校验 → 路由到 ToolCallback
   ▼
MethodToolCallbackProvider (我们的 Bean)
   │ 反射调用 @Tool 方法
   ▼
ArchitectureQueryTools.listFeatures(search)
   │
   ▼
ArchModelQueryService.getAllFeatures()
   │
   ▼
InMemoryArchModel.get()  ── ReadWriteLock guarded snapshot
```

**关键点**：

- 与 REST 路径**共享同一份 `InMemoryArchModel`**，所以一致性免费
- 不引入新的并发模型，沿用现有 `ReadWriteLock`
- `submit_edit` 真要落地时，时序末端会接到 `MrService.submitEdit() → GitCommandExecutor → ssh`

## 6. 配置

`backend/src/main/resources/application.yml`：

```yaml
spring:
  ai:
    mcp:
      server:
        name: arch-vis-mcp
        version: 0.1.0
        type: SYNC
        instructions: |
          Architecture Visualization MCP Server.
          ...
archvis:
  mcp:
    enabled: true   # 仅这一项；关掉它即整个 MCP Server 不装配
```

`pom.xml` 关键变更：

```xml
<properties>
    <spring-ai.version>1.0.0</spring-ai.version>
</properties>

<dependencyManagement>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>${spring-ai.version}</version>
        <type>pom</type><scope>import</scope>
    </dependency>
</dependencyManagement>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

## 7. Agent 接入示例

Claude Code 的 `~/.claude.json`：

```json
{
  "mcpServers": {
    "arch-vis": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

接入后，自然语言对 Claude 说"列一下所有 features"，模型会自动选用 `list_features`。

## 8. 与既有架构的关系

- **复用**：`ArchModelQueryService`、`MrService`、`EditRequest`、`EditResult`、`ReposConfigProperties`、`RepoSyncService`——全部既有 Bean，零拷贝
- **不改**：`EditorController`、`FeatureController`、`DataSourceController`、`GraphController`、`SyncController`——REST 路径保持稳定
- **新增包**：`com.archvis.mcp` 与 `com.archvis.api` 平级，作为"另一种 API 形态"
- **未来合并点**：当 B-01 落地，`EditorTools` 与 `EditorController` 都会调用一个共同的 `EditorService`，那时再合并各自的 `mrHistory`

## 9. 安全模型

**当前**：通过 `archvis.mcp.enabled=false` 关闭。无 Token、无 IP 白名单。**仅适用于内网/本地开发。**

**生产前必补**（按优先级）：

1. **API Key**（最低）：在 `archvis.mcp.api-key` 配置，过滤器校验 `Authorization: Bearer <key>`
2. **OAuth2 Resource Server**：使用 Spring AI 的 `McpServerOAuth2Configurer`，对接企业 SSO
3. **IP 白名单**：Spring Security 拦截或前置反向代理（Nginx）处理
4. **细粒度授权**：编辑类工具按 `entityType`/`field` 维度做权限控制（与 design § 10 的"per-field edit permissions"对齐）

## 10. 验证方式

```bash
# 1. 启动
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=demo

# 2. 工具列表（应返回 12 个）
curl -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# 3. 一致性核对（MCP 与 REST 应返回同样的节点数）
curl -s http://localhost:8080/api/v1/graph/full | jq '.nodes | length'
# 通过 Claude Code 调 get_full_graph，比对 nodes 数量
```

## 11. 后续工作

- [ ] 鉴权（API Key 或 OAuth2 二选一）
- [ ] IP 白名单 / 反向代理前置
- [ ] B-01 Schema Mapping 落地后，把 `EditorController` + `EditorTools` 合并到统一的 `EditorService`
- [ ] B-07 MR 平台 API 对接后，`submit_edit` 返回真实 `mrUrl`
- [ ] 工具粒度的 audit log（谁、何时、调用了哪个 tool、传入参数）
- [ ] MCP Server 的 readiness 探针（区别于 Spring Actuator 的全局 health）
