import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useSyncStore } from '../store/syncStore'
import type { SyncEvent } from '../types'

export function useRealtimeSync() {
  const addEvent = useSyncStore((s) => s.addEvent)
  const queryClient = useQueryClient()

  useEffect(() => {
    const wsUrl = `ws://${window.location.host}/ws/sync-status`
    let ws: WebSocket
    let reconnectTimer: ReturnType<typeof setTimeout>

    const connect = () => {
      ws = new WebSocket(wsUrl)

      ws.onmessage = (e) => {
        try {
          const event: SyncEvent = JSON.parse(e.data)
          addEvent(event)
          if (event.event === 'MODEL_REBUILT') {
            queryClient.invalidateQueries()
          }
        } catch {
          // ignore malformed messages
        }
      }

      ws.onclose = () => {
        reconnectTimer = setTimeout(connect, 5000)
      }
    }

    connect()
    return () => {
      clearTimeout(reconnectTimer)
      ws?.close()
    }
  }, [addEvent, queryClient])
}
