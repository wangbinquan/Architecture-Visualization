import { Layout, Menu } from 'antd'
import {
  DashboardOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  ShareAltOutlined,
  BranchesOutlined,
} from '@ant-design/icons'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import Dashboard from './pages/dashboard'
import FeaturesPage from './pages/features'
import DataSourcesPage from './pages/datasources'
import VisualizationPage from './pages/visualization'
import MrsPage from './pages/mrs'
import { SyncStatus } from './components/SyncStatus'
import { useRealtimeSync } from './hooks/useRealtimeSync'

const { Header, Sider, Content } = Layout

const NAV_ITEMS = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '总览' },
  { key: '/features', icon: <AppstoreOutlined />, label: '特性' },
  { key: '/datasources', icon: <DatabaseOutlined />, label: '数据源' },
  { key: '/visualization', icon: <ShareAltOutlined />, label: '依赖关系图' },
  { key: '/mrs', icon: <BranchesOutlined />, label: 'MR 记录' },
]

export default function App() {
  useRealtimeSync()
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
          background: '#001529',
        }}
      >
        <span style={{ color: '#fff', fontWeight: 700, fontSize: 18 }}>架构可视化平台</span>
        <SyncStatus />
      </Header>

      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            style={{ height: '100%', borderRight: 0 }}
            items={NAV_ITEMS}
            onClick={({ key }) => navigate(key)}
          />
        </Sider>

        <Layout style={{ padding: '0 0 24px' }}>
          <Content style={{ background: '#fff', margin: 0, minHeight: 280 }}>
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/features" element={<FeaturesPage />} />
              <Route path="/datasources" element={<DataSourcesPage />} />
              <Route path="/visualization" element={<VisualizationPage />} />
              <Route path="/mrs" element={<MrsPage />} />
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </Layout>
  )
}
