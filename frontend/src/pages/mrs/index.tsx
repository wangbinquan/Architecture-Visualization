import { Card, Empty, Table, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import api from '../../services/api'

const { Title } = Typography

export default function MrsPage() {
  const { data: mrs = [] } = useQuery({
    queryKey: ['mrs'],
    queryFn: () => api.get('/editor/mrs').then((r) => r.data),
  })

  const columns = [
    { title: '分支名', dataIndex: 'branchName', key: 'branchName' },
    { title: '提交', dataIndex: 'commitHash', key: 'commitHash' },
    {
      title: '状态',
      dataIndex: 'success',
      key: 'success',
      render: (v: boolean) => <Tag color={v ? 'success' : 'error'}>{v ? '成功' : '失败'}</Tag>,
    },
    { title: '备注', dataIndex: 'message', key: 'message' },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Title level={4}>MR 记录</Title>
      <Card>
        {mrs.length === 0 ? (
          <Empty description="暂无 MR 记录" />
        ) : (
          <Table dataSource={mrs} columns={columns} rowKey={(_, i) => String(i)} size="small" />
        )}
      </Card>
    </div>
  )
}
