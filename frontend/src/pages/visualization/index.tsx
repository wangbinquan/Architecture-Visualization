import { Card, Empty, Select, Spin, Space, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { graphApi } from '../../services/syncApi'
import { ArchGraph } from '../../components/ArchGraph'

const { Title } = Typography

const NODE_TYPE_LEGEND = [
  { type: 'feature', label: '特性', color: '#1677ff' },
  { type: 'feature-config', label: '配置项', color: '#69b1ff' },
  { type: 'datasource', label: '数据源', color: '#52c41a' },
  { type: 'collection', label: '子集合', color: '#95de64' },
  { type: 'metric', label: '业务指标', color: '#d9f7be' },
]

export default function VisualizationPage() {
  const { data: graphData, isLoading } = useQuery({
    queryKey: ['graph-full'],
    queryFn: graphApi.full,
  })

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>依赖关系图</Title>
      <Card
        title={
          <Space>
            <span>全量架构依赖图</span>
            {graphData && (
              <span style={{ fontSize: 12, color: '#888' }}>
                {graphData.nodes?.length ?? 0} 节点 / {graphData.edges?.length ?? 0} 边
              </span>
            )}
          </Space>
        }
        extra={
          <Space>
            {NODE_TYPE_LEGEND.map((l) => (
              <Space key={l.type} size={4}>
                <span style={{ display: 'inline-block', width: 12, height: 12, borderRadius: '50%', background: l.color }} />
                <span style={{ fontSize: 12 }}>{l.label}</span>
              </Space>
            ))}
          </Space>
        }
      >
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 80 }}>
            <Spin tip="加载图数据..." />
          </div>
        ) : !graphData || graphData.nodes?.length === 0 ? (
          <Empty description="暂无数据，请先触发同步" style={{ padding: 80 }} />
        ) : (
          <ArchGraph data={graphData} height={600} />
        )}
      </Card>
    </div>
  )
}
