import { useState } from 'react'
import { Card, Col, Descriptions, Empty, Input, Row, Spin, Table, Tag, Tree, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { featureApi } from '../../services/featureApi'
import type { Feature } from '../../types'
import type { DataNode } from 'antd/es/tree'

const { Search } = Input
const { Title, Text } = Typography

function buildTreeData(features: Feature[]): DataNode[] {
  return features.map((f) => ({
    key: f.id,
    title: (
      <span>
        <Tag color="blue">{f.sourceRepo ?? 'unknown'}</Tag> {f.name}
      </span>
    ),
    children: (f.configs ?? []).map((c) => ({
      key: `${f.id}/${c.id}`,
      title: c.name,
      isLeaf: true,
    })),
  }))
}

export default function FeaturesPage() {
  const [search, setSearch] = useState('')
  const [selectedFeature, setSelectedFeature] = useState<Feature | null>(null)

  const { data: features = [], isLoading } = useQuery({
    queryKey: ['features', search],
    queryFn: () => featureApi.list(search || undefined),
  })

  const handleSelect = (keys: React.Key[]) => {
    const key = String(keys[0] ?? '')
    const featureId = key.includes('/') ? key.split('/')[0] : key
    const f = features.find((x) => x.id === featureId) ?? null
    setSelectedFeature(f)
  }

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>特性列表</Title>
      <Row gutter={16}>
        <Col span={8}>
          <Card
            title="特性树"
            extra={<Search placeholder="搜索" onSearch={setSearch} allowClear style={{ width: 160 }} />}
          >
            {isLoading ? (
              <Spin />
            ) : features.length === 0 ? (
              <Empty description="暂无数据，请先触发同步" />
            ) : (
              <Tree
                treeData={buildTreeData(features)}
                onSelect={handleSelect}
                defaultExpandAll
              />
            )}
          </Card>
        </Col>
        <Col span={16}>
          {selectedFeature ? (
            <FeatureDetail feature={selectedFeature} />
          ) : (
            <Card>
              <Empty description="请从左侧选择一个特性" />
            </Card>
          )}
        </Col>
      </Row>
    </div>
  )
}

function FeatureDetail({ feature }: { feature: Feature }) {
  const tableColumns = [{ title: '表名', dataIndex: 'name' }, { title: '说明', dataIndex: 'description' }]
  const i18nColumns = [{ title: 'Key', dataIndex: 'key' }, { title: '默认值', dataIndex: 'defaultValue' }]
  const reqColumns = [
    { title: '数据源', dataIndex: 'datasourceId' },
    { title: '子集合', dataIndex: 'collectionId' },
    { title: '业务指标', dataIndex: 'metricId' },
  ]

  return (
    <Card title={feature.name} extra={<Tag color="blue">{feature.sourceRepo}</Tag>}>
      <Descriptions bordered size="small" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="特性ID">{feature.id}</Descriptions.Item>
        <Descriptions.Item label="配置项数量">{feature.configs?.length ?? 0}</Descriptions.Item>
      </Descriptions>

      {(feature.configs ?? []).map((cfg) => (
        <Card
          key={cfg.id}
          type="inner"
          title={<Text strong>{cfg.name}</Text>}
          style={{ marginBottom: 12 }}
        >
          {cfg.databaseTables?.length > 0 && (
            <>
              <Text type="secondary">数据库表</Text>
              <Table dataSource={cfg.databaseTables} columns={tableColumns} rowKey="name" size="small" pagination={false} style={{ marginBottom: 8 }} />
            </>
          )}
          {cfg.i18nEntries?.length > 0 && (
            <>
              <Text type="secondary">国际化</Text>
              <Table dataSource={cfg.i18nEntries} columns={i18nColumns} rowKey="key" size="small" pagination={false} style={{ marginBottom: 8 }} />
            </>
          )}
          {cfg.dataSourceRequirements?.length > 0 && (
            <>
              <Text type="secondary">数据源引用</Text>
              <Table dataSource={cfg.dataSourceRequirements} columns={reqColumns} rowKey={(r) => r.datasourceId + r.metricId} size="small" pagination={false} />
            </>
          )}
        </Card>
      ))}
    </Card>
  )
}
