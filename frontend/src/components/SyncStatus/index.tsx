import { Badge, Space, Spin, Tag, Tooltip } from 'antd'
import { SyncOutlined } from '@ant-design/icons'
import { useSyncStore } from '../../store/syncStore'
import { syncApi } from '../../services/syncApi'
import { useQueryClient } from '@tanstack/react-query'

export function SyncStatus() {
  const { latestEvent, isRebuilding } = useSyncStore()
  const queryClient = useQueryClient()

  const handleManualSync = async () => {
    await syncApi.triggerAll()
    queryClient.invalidateQueries()
  }

  const statusColor = () => {
    if (!latestEvent) return 'default'
    if (latestEvent.event === 'MODEL_REBUILT') return 'success'
    if (latestEvent.event === 'SYNC_STARTED') return 'processing'
    if (latestEvent.failedRepos && latestEvent.failedRepos > 0) return 'warning'
    return 'success'
  }

  return (
    <Space>
      {isRebuilding && <Spin size="small" />}
      <Badge status={statusColor()} />
      <span style={{ color: '#fff', fontSize: 12 }}>
        {latestEvent?.event === 'MODEL_REBUILT'
          ? `${latestEvent.featureCount} 特性 / ${latestEvent.datasourceCount} 数据源`
          : latestEvent?.event === 'SYNC_STARTED'
          ? '同步中...'
          : '等待同步'}
      </span>
      <Tooltip title="手动触发同步">
        <SyncOutlined
          style={{ color: '#fff', cursor: 'pointer' }}
          spin={isRebuilding}
          onClick={handleManualSync}
        />
      </Tooltip>
      {latestEvent && (
        <Tag color="default" style={{ fontSize: 11 }}>
          {new Date(latestEvent.timestamp).toLocaleTimeString()}
        </Tag>
      )}
    </Space>
  )
}
