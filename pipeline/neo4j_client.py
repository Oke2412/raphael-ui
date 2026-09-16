"""
Neo4j Cosmos Graph Client
Kết nối cơ sở dữ liệu đồ thị tri thức Neo4j và cung cấp dữ liệu đồ thị dạng chòm sao (Cosmos Constellation)
cho giao diện Raphael hiển thị thời gian thực phía sau Orb chính.
Tự động fallback sang Đồ thị Tri thức Tensura Cosmos nếu chưa kết nối cơ sở dữ liệu thật.
"""

import os
from typing import Dict, Any, List, Optional


class Neo4jCosmosClient:
    def __init__(self, config: Dict[str, Any]):
        self.enabled = config.get("enabled", True)
        self.uri = config.get("uri", "bolt://localhost:7687")
        self.user = config.get("user", "neo4j")
        self.password = config.get("password", "password")
        self.database = config.get("database", "neo4j")
        self.max_nodes = config.get("max_nodes_render", 150)
        self.driver = None
        self._init_driver()

    def _init_driver(self):
        if not self.enabled:
            print("[Neo4j] Đang tắt trong cấu hình.")
            return

        try:
            from neo4j import GraphDatabase
            self.driver = GraphDatabase.driver(
                self.uri,
                auth=(self.user, self.password),
                connection_timeout=2
            )
            # Thử verify kết nối
            self.driver.verify_connectivity()
            print(f"[Neo4j] Kết nối thành công tới Neo4j tại {self.uri}!")
        except Exception as e:
            print(f"[Neo4j] Chưa kết nối được Neo4j database ({e}). Kích hoạt Chế độ Mô phỏng Tensura Cosmos Graph.")
            self.driver = None

    def get_cosmos_graph(self) -> Dict[str, Any]:
        """
        Lấy toàn bộ đồ thị tri thức để render mạng lưới sao trong vũ trụ (Cosmos Stars).
        Nếu có Neo4j thật, truy vấn node & relationships.
        Nếu không, trả về chòm sao tri thức Tensura mẫu siêu đẹp.
        """
        if self.driver is not None:
            try:
                with self.driver.session(database=self.database) as session:
                    result = session.run(
                        f"MATCH (n)-[r]->(m) RETURN n, r, m LIMIT {self.max_nodes}"
                    )
                    nodes_map = {}
                    links = []
                    for record in result:
                        n, r, m = record["n"], record["r"], record["m"]
                        n_id = str(n.element_id if hasattr(n, "element_id") else n.id)
                        m_id = str(m.element_id if hasattr(m, "element_id") else m.id)

                        if n_id not in nodes_map:
                            nodes_map[n_id] = {
                                "id": n_id,
                                "name": n.get("name", list(n.labels)[0] if n.labels else n_id),
                                "labels": list(n.labels),
                                "properties": dict(n),
                                "val": 10
                            }
                        if m_id not in nodes_map:
                            nodes_map[m_id] = {
                                "id": m_id,
                                "name": m.get("name", list(m.labels)[0] if m.labels else m_id),
                                "labels": list(m.labels),
                                "properties": dict(m),
                                "val": 8
                            }
                        links.append({
                            "source": n_id,
                            "target": m_id,
                            "type": r.type,
                            "properties": dict(r)
                        })

                    return {
                        "is_live_db": True,
                        "nodes": list(nodes_map.values()),
                        "links": links
                    }
            except Exception as e:
                print(f"[Neo4j] Lỗi truy vấn đồ thị: {e}. Dùng đồ thị vũ trụ Tensura fallback.")

        # Fallback: Tensura Universe Celestial Constellation Graph
        return self._get_tensura_mock_cosmos_graph()

    def query_subgraph(self, search_text: str) -> Dict[str, Any]:
        """
        Tìm kiếm các node và cạnh liên quan đến từ khóa của người dùng để làm nổi bật (Highlight)
        các ngôi sao trong vũ trụ khi câu hỏi được gửi tới.
        """
        full_graph = self.get_cosmos_graph()
        nodes = full_graph.get("nodes", [])
        links = full_graph.get("links", [])

        keywords = search_text.lower().split()
        active_node_ids = set()

        for node in nodes:
            node_name = str(node.get("name", "")).lower()
            if any(kw in node_name for kw in keywords if len(kw) > 1):
                active_node_ids.add(node["id"])

        # Nếu không khớp cụ thể, chọn ngẫu nhiên chùm sao trung tâm để kích hoạt hiệu ứng sáng
        if not active_node_ids and nodes:
            active_node_ids = {nodes[0]["id"], nodes[1]["id"] if len(nodes) > 1 else nodes[0]["id"]}

        active_links = []
        for link in links:
            src = link["source"] if isinstance(link["source"], str) else link["source"].get("id")
            tgt = link["target"] if isinstance(link["target"], str) else link["target"].get("id")
            if src in active_node_ids or tgt in active_node_ids:
                active_links.append({"source": src, "target": tgt, "type": link.get("type", "")})
                active_node_ids.add(src)
                active_node_ids.add(tgt)

        return {
            "query": search_text,
            "highlighted_nodes": list(active_node_ids),
            "highlighted_links": active_links,
            "timestamp": round(os.times().system, 3)
        }

    def _get_tensura_mock_cosmos_graph(self) -> Dict[str, Any]:
        """Đồ thị chòm sao vũ trụ Tensura với các thực thể cốt lõi."""
        nodes = [
            {"id": "raphael", "name": "Đại Hiền Triết / Raphael", "group": "CoreSkill", "color": "#00f0ff", "val": 22},
            {"id": "ciel", "name": "Ciel (Thần Trí Hạch)", "group": "CoreSkill", "color": "#ff007f", "val": 24},
            {"id": "rimuru", "name": "Rimuru Tempest", "group": "Character", "color": "#38bdf8", "val": 26},
            {"id": "veldora", "name": "Veldora Tempest (Bạo Phong Long)", "group": "Dragon", "color": "#facc15", "val": 20},
            {"id": "tempest", "name": "Liên Bang Jura Tempest", "group": "Location", "color": "#4ade80", "val": 18},
            {"id": "beelzebuth", "name": "Bạo Thực Chi Vương (Beelzebuth)", "group": "UltimateSkill", "color": "#a855f7", "val": 16},
            {"id": "uriel", "name": "Thệ Ước Chi Vương (Uriel)", "group": "UltimateSkill", "color": "#f43f5e", "val": 15},
            {"id": "magicules", "name": "Ma Tố (Magicules)", "group": "Energy", "color": "#818cf8", "val": 14},
            {"id": "benimaru", "name": "Benimaru", "group": "Subordinate", "color": "#ef4444", "val": 13},
            {"id": "shion", "name": "Shion", "group": "Subordinate", "color": "#c084fc", "val": 12},
            {"id": "diablo", "name": "Diablo (Hắc Sắc Thủy Tổ)", "group": "Demon", "color": "#1e293b", "val": 17},
            {"id": "milim", "name": "Milim Nava (Long Ma Vương)", "group": "DemonLord", "color": "#fb7185", "val": 19},
            {"id": "ramiris", "name": "Ramiris (Mê Cung Ma Vương)", "group": "Fairy", "color": "#a3e635", "val": 13},
            {"id": "guy", "name": "Guy Crimson (Xích Sắc Thủy Tổ)", "group": "DemonLord", "color": "#b91c1c", "val": 21}
        ]

        links = [
            {"source": "rimuru", "target": "raphael", "type": "POSSESSES"},
            {"source": "raphael", "target": "ciel", "type": "EVOLVES_INTO"},
            {"source": "rimuru", "target": "beelzebuth", "type": "ACQUIRED"},
            {"source": "rimuru", "target": "uriel", "type": "ACQUIRED"},
            {"source": "rimuru", "target": "veldora", "type": "SWORN_FRIEND"},
            {"source": "rimuru", "target": "tempest", "type": "RULES"},
            {"source": "tempest", "target": "magicules", "type": "SATURATED_WITH"},
            {"source": "rimuru", "target": "benimaru", "type": "COMMANDS"},
            {"source": "rimuru", "target": "shion", "type": "COMMANDS"},
            {"source": "rimuru", "target": "diablo", "type": "SUMMONS"},
            {"source": "rimuru", "target": "milim", "type": "BEST_FRIEND"},
            {"source": "tempest", "target": "ramiris", "type": "HOSTS_LABYRINTH"},
            {"source": "guy", "target": "milim", "type": "OCTAGRAM_PEER"},
            {"source": "guy", "target": "diablo", "type": "RIVAL"}
        ]

        return {
            "is_live_db": False,
            "nodes": nodes,
            "links": links
        }
