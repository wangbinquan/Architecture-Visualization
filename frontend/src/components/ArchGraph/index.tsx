import { useEffect, useRef } from 'react'
import { Graph } from '@antv/g6'
import type { GraphData } from '../../types'

const NODE_COLORS: Record<string, string> = {
  feature: '#1677ff',
  'feature-config': '#69b1ff',
  datasource: '#52c41a',
  collection: '#95de64',
  metric: '#d9f7be',
}

interface Props {
  data: GraphData
  height?: number
}

export function ArchGraph({ data, height = 600 }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const graphRef = useRef<Graph | null>(null)

  useEffect(() => {
    if (!containerRef.current) return

    const graph = new Graph({
      container: containerRef.current,
      autoFit: 'view',
      data: {
        nodes: data.nodes.map((n) => ({
          id: n.id,
          data: { label: n.label, nodeType: n.type },
          style: {
            fill: NODE_COLORS[n.type] ?? '#ccc',
            stroke: '#aaa',
            labelText: n.label,
            labelFontSize: 11,
            labelMaxWidth: 100,
            size: n.type === 'feature' || n.type === 'datasource' ? 36 : 24,
          },
        })),
        edges: data.edges.map((e) => ({
          id: e.id,
          source: e.source,
          target: e.target,
          style: {
            stroke: e.type === 'references' ? '#faad14' : '#d9d9d9',
            lineWidth: e.type === 'references' ? 2 : 1,
            endArrow: true,
            labelText: e.label,
            labelFontSize: 10,
          },
        })),
      },
      layout: {
        type: 'force',
        preventOverlap: true,
        linkDistance: 100,
        nodeStrength: -300,
      },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    })

    graph.render()
    graphRef.current = graph

    return () => {
      graph.destroy()
      graphRef.current = null
    }
  }, [data])

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height, border: '1px solid #f0f0f0', borderRadius: 8 }}
    />
  )
}
