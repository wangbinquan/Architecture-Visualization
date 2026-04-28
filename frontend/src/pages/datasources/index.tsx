import { useState } from 'react'
import { Card, Col, Descriptions, Empty, Input, Row, Spin, Table, Tag, Tree, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { datasourceApi } from '../../services/datasourceApi'
import type { DataSourceDef } from '../../types'
import type { DataNode } from 'antd/es/tree'

const { Search } = Input
const { Title, Text } = Typography

function buildTreeData(datasources: DataSourceDef[]): DataNode[] {
  return datasources.map((ds) => ({
    key: ds.id,
    title: (
      <span>
        <Tag color="green">{ds.sourceRepo ?? 'unknown'}</Tag> {ds.name}
      </span>
    ),
    children: (ds.subCollections ?? []).map((col) => ({
      key: `${ds.id}/${col.id}`,
      title: col.name,
      children: (col.businessMetrics ?? []).map((m) => ({
        key: `${ds.id}/${col.id}/${m.id}`,
        title: (
          <span>
            {m.name} <Tag color="default">{m.dataType}</Tag>
          </span>
        ),
        isLeaf: true,
      })),
    })),
  }))
}

export default function DataSourcesPage() {
  const [search, setSearch] = useState('')
  const [selectedDs, setSelectedDs] = useState<DataSourceDef | null>(null)

  const { data: datasources = [], isLoading } = useQuery({
    queryKey: ['datasources', search],
    queryFn: () => datasourceApi.list(search || undefined),
  })

  const handleSelect = (keys: React.Key[]) => {
    const key = String(keys[0] ?? '')
    const dsId = key.split('/')[0]
    const ds = datasources.find((x) => x.id === dsId) ?? null
    setSelectedDs(ds)
  }

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>数据源列表</Title>
      <Row gutter={16}>
        <Col span={8}>
          <Card
            title="数据源树"
            extra={<Search placeholder="搜索" onSearch={setSearch} allowClear style={{ width: 160 }} />}
          >
            {isLoading ? (
              <Spin />
            ) : datasources.length === 0 ? (
              <Empty description="暂无数据，请先触发同步" />
            ) : (
              <Tree treeData={buildTreeData(datasources)} onSelect={handleSelect} defaultExpandAll />
            )}
          </Card>
        </Col>
        <Col span={16}>
          {selectedDs ? (
            <DataSourceDetail ds={selectedDs} />
          ) : (
            <Card>
              <Empty description="请从左侧选择一个数据源" />
            </Card>
          )}
        </Col>
      </Row>
    </div>
  )
}

function DataSourceDetail({ ds }: { ds: DataSourceDef }) {
  const metricColumns = [
    { title: '指标ID', dataIndex: 'id' },
    { title: '指标名', dataIndex: 'name' },
    { title: '数据类型', dataIndex: 'dataType', render: (v: string) => <Tag>{v}</Tag> },
    { title: '描述', dataIndex: 'description' },
  ]

  return (
    <Card title={ds.name} extra={<Tag color="green">{ds.sourceRepo}</Tag>}>
      <Descriptions bordered size="small" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="数据源ID">{ds.id}</Descriptions.Item>
        <Descriptions.Item label="子集合数量">{ds.subCollections?.length ?? 0}</Descriptions.Item>
      </Descriptions>

      {(ds.subCollections ?? []).map((col) => (
        <Card
          key={col.id}
          type="inner"
          title={<Text strong>{col.name}</Text>}
          style={{ marginBottom: 12 }}
        >
          <Table
            dataSource={col.businessMetrics ?? []}
            columns={metricColumns}
            rowKey="id"
            size="small"
            pagination={false}
          />
        </Card>
      ))}
    </Card>
  )
}
