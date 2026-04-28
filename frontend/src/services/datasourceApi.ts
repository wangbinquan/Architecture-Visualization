import api from './api'
import type { DataSourceDef, DependencyLink } from '../types'

export const datasourceApi = {
  list: (search?: string) =>
    api.get<DataSourceDef[]>('/datasources', { params: search ? { search } : {} }).then(r => r.data),

  get: (id: string) =>
    api.get<DataSourceDef>(`/datasources/${id}`).then(r => r.data),

  referencedBy: (id: string) =>
    api.get<DependencyLink[]>(`/datasources/${id}/referenced-by`).then(r => r.data),
}
