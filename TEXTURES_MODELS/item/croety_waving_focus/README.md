# 涌动聚晶贴图

2026-09-16，用户已确认最终贴图。

青蓝色竖向晶体，两侧环绕波纹，用于 `croety:waving_focus`。
最终 PNG 由用户提供并作为发布资源使用；用户参考图和生成草稿只保留在本地 QA 目录，不进入发布资源。
导出脚本按最近邻采样到16×16，收敛为8色，并将低透明边缘像素归为透明背景。

- `waving_focus.png`：最终16×16 RGBA贴图，用户提供，156个可见像素、100个透明像素，无越界内容。
- `preview.png`：实际贴图的32倍最近邻放大预览，棋盘背景只用于预览。
- `export-texture.ps1`：可重复执行的导出脚本。
- 游戏资源：`src/main/resources/assets/croety/textures/item/waving_focus.png`，与最终 PNG 相同。
- 物品模型已指向 `croety:item/waving_focus`，物品ID和行为不变。

该贴图已用于工作区整理和首个 release。
