import api from './api'
import type { Feature, DependencyLink } from '../types'

export const featureApi = {
  list: (search?: string) =>
    api.get<Feature[]>('/features', { params: search ? { search } : {} }).then(r => r.data),

  get: (id: string) =>
    api.get<Feature>(`/features/${id}`).then(r => r.data),

  dependencies: (id: string) =>
    api.get<DependencyLink[]>(`/features/${id}/dependencies`).then(r => r.data),
}
