import { Card, Col, Row, Statistic, Table, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { featureApi } from '../../services/featureApi'
import { datasourceApi } from '../../services/datasourceApi'
import { syncApi } from '../../services/syncApi'

const { Title } = Typography

export default function Dashboard() {
  const { data: features = [] } = useQuery({ queryKey: ['features'], queryFn: () => featureApi.list() })
  const { data: datasources = [] } = useQuery({ queryKey: ['datasources'], queryFn: () => datasourceApi.list() })
  const { data: syncStatus } = useQuery({ queryKey: ['sync-status'], queryFn: syncApi.status, refetchInterval: 30_000 })

  const totalConfigs = features.reduce((sum, f) => sum + (f.configs?.length ?? 0), 0)
  const totalMetrics = datasources.reduce(
    (sum, ds) => sum + (ds.subCollections?.reduce((s, c) => s + (c.businessMetrics?.length ?? 0), 0) ?? 0),
    0
  )

  const repoColumns = [
    { title: '代码仓', dataIndex: 'name', key: 'name' },
    { title: '分支', dataIndex: 'branch', key: 'branch' },
    {
      title: '状态',
      key: 'status',
      render: (_: unknown, r: { lastSync?: { success: boolean; syncedAt: string } }) =>
        r.lastSync ? (
          <Tag color={r.lastSync.success ? 'success' : 'error'}>
            {r.lastSync.success ? '正常' : '失败'}
          </Tag>
        ) : (
          <Tag>未同步</Tag>
        ),
    },
    {
      title: '最近同步',
      key: 'syncedAt',
      render: (_: unknown, r: { lastSync?: { syncedAt: string } }) =>
        r.lastSync?.syncedAt ? new Date(r.lastSync.syncedAt).toLocaleString() : '-',
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>总览</Title>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic title="特性总数" value={features.length} valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="配置项总数" value={totalConfigs} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="数据源总数" value={datasources.length} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="业务指标总数" value={totalMetrics} />
          </Card>
        </Col>
      </Row>

      <Card title="代码仓同步状态">
        <Table
          dataSource={syncStatus?.repos ?? []}
          columns={repoColumns}
          rowKey="name"
          size="small"
          pagination={false}
        />
      </Card>
    </div>
  )
}
