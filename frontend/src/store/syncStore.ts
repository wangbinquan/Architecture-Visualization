import { create } from 'zustand'
import type { SyncEvent } from '../types'

interface SyncStore {
  events: SyncEvent[]
  latestEvent: SyncEvent | null
  isRebuilding: boolean
  addEvent: (event: SyncEvent) => void
  clearEvents: () => void
}

export const useSyncStore = create<SyncStore>((set) => ({
  events: [],
  latestEvent: null,
  isRebuilding: false,
  addEvent: (event) =>
    set((state) => ({
      events: [...state.events.slice(-50), event],
      latestEvent: event,
      isRebuilding: event.event === 'SYNC_STARTED',
    })),
  clearEvents: () => set({ events: [], latestEvent: null, isRebuilding: false }),
}))
