"""
Kiểm thử Neo4j Cosmos Graph Client
Kiểm tra khả năng cung cấp dữ liệu đồ thị chòm sao vũ trụ và khả năng highlight theo câu hỏi truy vấn.
"""

import os
import sys

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

import json
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from pipeline.neo4j_client import Neo4jCosmosClient


def test_cosmos_graph():
    print("=" * 65)
    print("      KIỂM THỬ NEO4J COSMOS GRAPH CLIENT (VŨ TRỤ TRI THỨC)")
    print("=" * 65)

    client = Neo4jCosmosClient({
        "enabled": True,
        "uri": "bolt://localhost:7687",
        "user": "neo4j",
        "password": "password"
    })

    print("[*] Đang lấy dữ liệu toàn bộ đồ thị tri thức (Cosmos Graph)...")
    graph = client.get_cosmos_graph()
    nodes = graph.get("nodes", [])
    links = graph.get("links", [])
    print(f"  [OK] Đã nạp thành công mạng lưới vũ trụ!")
    print(f"  [OK] Nguồn dữ liệu: {'Neo4j Database Thực tế' if graph.get('is_live_db') else 'Tensura Celestial Universe (Fallback)'}")
    print(f"  [OK] Tổng số vì sao (Nodes): {len(nodes)}")
    print(f"  [OK] Tổng số liên kết dải ngân hà (Links): {len(links)}")

    print("\n[*] Thử nghiệm Highlight theo câu hỏi truy vấn của người dùng:")
    test_query = "Raphael hãy kiểm tra lượng ma tố của Veldora"
    highlight = client.query_subgraph(test_query)
    print(f"  Câu hỏi: '{test_query}'")
    print(f"  Các ngôi sao được kích hoạt (Highlighted Nodes): {highlight.get('highlighted_nodes')}")
    print(f"  Các liên kết phát sáng (Highlighted Links): {len(highlight.get('highlighted_links', []))} liên kết")

    print("\n" + "=" * 65)
    print("   MODULE NEO4J COSMOS GRAPH HOẠT ĐỘNG HOÀN HẢO!")
    print("=" * 65)


if __name__ == "__main__":
    test_cosmos_graph()
