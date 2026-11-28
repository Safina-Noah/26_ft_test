import React, { useMemo, useState } from "react";
import {
  ReactFlow,
  Background,
  Controls,
  Handle,
  Position,
} from "@xyflow/react";
import dagre from "@dagrejs/dagre";
import "@xyflow/react/dist/style.css";

function StructureNode({ data }) {
  return (
    <div className="graph-node">
      <Handle type="target" position={Position.Top} />
      <strong>{data.title}</strong>
      <small>{data.file}</small>
      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}
const nodeTypes = { structure: StructureNode };
export function ArchitectureGraph({ analysis, explain }) {
  const [selected, setSelected] = useState(null);
  const graph = useMemo(() => {
    const layout = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));
    layout.setGraph({ rankdir: "TB", nodesep: 55, ranksep: 85 });
    analysis.architecture.forEach((n) =>
      layout.setNode(n.id, { width: 250, height: 95 }),
    );
    analysis.edges.forEach((e) => layout.setEdge(e.source, e.target));
    dagre.layout(layout);
    return {
      nodes: analysis.architecture.map((n) => {
        const p = layout.node(n.id);
        return {
          id: n.id,
          type: "structure",
          data: n,
          position: { x: p.x - 125, y: p.y - 47.5 },
        };
      }),
      edges: analysis.edges.map((e, i) => ({
        id: "edge-" + i,
        ...e,
        type: "smoothstep",
        markerEnd: { type: "arrowclosed" },
      })),
    };
  }, [analysis]);
  if (!graph.nodes.length)
    return <p>수집한 코드에서 확인할 수 있는 구조 정보가 부족해요.</p>;
  const item = selected || analysis.architecture[0];
  return (
    <>
      <div className="architecture-canvas" aria-label="프로젝트 아키텍처 도표">
        <ReactFlow
          nodes={graph.nodes}
          edges={graph.edges}
          nodeTypes={nodeTypes}
          fitView
          nodesDraggable={false}
          nodesConnectable={false}
          onNodeClick={(_, node) => setSelected(node.data)}
          minZoom={0.2}
          maxZoom={1.6}
        >
          <Background />
          <Controls showInteractive={false} />
        </ReactFlow>
      </div>
      <div className="graph-accessible">
        <label>
          구성 요소
          <select
            value={item.id}
            onChange={(e) =>
              setSelected(
                analysis.architecture.find((n) => n.id === e.target.value),
              )
            }
          >
            {analysis.architecture.map((n) => (
              <option key={n.id} value={n.id}>
                {n.title}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="structure-explanation">
        <h3>{item.title}</h3>
        <p>{explain(item.description)}</p>
        {item.code && (
          <a href={"#/project/" + analysis.projectId + "/code/" + item.code}>
            연결된 코드 보기
          </a>
        )}
      </div>
    </>
  );
}
