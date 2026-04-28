export interface DatabaseTable {
  name: string
  description?: string
}

export interface I18nEntry {
  key: string
  defaultValue: string
}

export interface DataSourceRequirement {
  datasourceId: string
  collectionId: string
  metricId: string
}

export interface FeatureConfig {
  id: string
  name: string
  databaseTables: DatabaseTable[]
  i18nEntries: I18nEntry[]
  dataSourceRequirements: DataSourceRequirement[]
}

export interface Feature {
  id: string
  name: string
  sourceRepo?: string
  configs: FeatureConfig[]
}

export interface BusinessMetric {
  id: string
  name: string
  dataType: string
  description?: string
}

export interface SubCollection {
  id: string
  name: string
  businessMetrics: BusinessMetric[]
}

export interface DataSourceDef {
  id: string
  name: string
  sourceRepo?: string
  subCollections: SubCollection[]
}

export interface DependencyLink {
  featureId: string
  featureConfigId: string
  datasourceId: string
  collectionId: string
  metricId: string
}

export interface GraphNode {
  id: string
  label: string
  type: 'feature' | 'feature-config' | 'datasource' | 'collection' | 'metric'
  group?: string
}

export interface GraphEdge {
  id: string
  source: string
  target: string
  type: 'contains' | 'references'
  label?: string
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export interface SyncEvent {
  event: 'SYNC_STARTED' | 'SYNC_REPO_DONE' | 'SYNC_ALL_DONE' | 'MODEL_REBUILT'
  repoName?: string
  timestamp: string
  totalRepos?: number
  failedRepos?: number
  featureCount?: number
  datasourceCount?: number
}
