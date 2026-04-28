import api from './api'

export const syncApi = {
  status: () => api.get('/sync/status').then(r => r.data),
  triggerAll: () => api.post('/sync/trigger').then(r => r.data),
  triggerOne: (repoName: string) => api.post(`/sync/trigger/${repoName}`).then(r => r.data),
}

export const graphApi = {
  full: () => api.get('/graph/full').then(r => r.data),
  forFeature: (id: string) => api.get(`/graph/feature/${id}`).then(r => r.data),
}
