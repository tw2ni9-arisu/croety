# 幽灵马达模型与贴图

基于用户提供的 Create 创造马达模型制作，保持原始几何、UV、旋转和显示参数。五张贴图全部改为淡冰蓝色，保留框线、棋盘暗纹、条纹和轴结构，并加入少量灵魂菱形符纹。

- 分辨率：每张 16×16，RGBA PNG。
- 全部可见像素：Alpha = 89/255，即 **34.90% 不透明度**；原本空白的像素仍为 Alpha = 0。
- 资源命名空间：`croety`；横放、竖放和物品模型均引用独立的幽灵贴图。
- 原始素材位于 `../../needed/create_creative_motor`，未覆盖。

## 文件

- `assets/croety/models/block/soul_motor/block.json`：用户指定模型的幽灵版。
- `assets/croety/models/block/soul_motor/block_vertical.json`：竖放模型。
- `assets/croety/models/block/soul_motor/item.json`：包含输出轴的完整物品模型，适合查看整体造型。
- `assets/croety/models/item/soul_motor.json`：物品模型入口。
- `assets/croety/blockstates/soul_motor.json`：六方向模型映射。
- `assets/croety/textures/block/soul_motor/`：五张最终贴图。
- `preview.png`：直接读取最终模型与贴图渲染的静态预览，非游戏截图。
- `qa/`：原素材预览、生成原图、生成提示词、导出脚本和验证记录。

## 使用与透明渲染

用 Blockbench 打开上述 `block.json` 或 `item.json`；若未自动定位资源，手动关联 `textures/block/soul_motor/` 下同名 PNG。透明显示取决于编辑器预览设置，PNG 自身保存了真实 Alpha。

模型设置了 `"render_type": "minecraft:translucent"`，这是 Forge 支持部分透明像素的模型渲染类型：[Forge 1.20.x 官方文档](https://docs.minecraftforge.net/en/1.20.x/rendering/modelextensions/rendertypes/)。

本目录是独立素材交付，还没有注册新的 `croety:soul_motor` 方块、动力逻辑或动画。接入模组时，将 `assets/` 合入对应资源目录并注册方块。动态输出轴也需要使用本目录的轴贴图和支持半透明的渲染方式；仅更换静态模型不会自动改变 Create/Flywheel 的动态轴材质。

34.90% 指每个贴图像素的 Alpha；多个透明表面叠加时，局部视觉不透明度会更高。预览采用静态面排序，实际游戏渲染效果仍需接入后验证。

## 制作方式

使用内置 image_gen 编辑原贴图预览带，再按原 UV 网格进行像素取样、恢复原始空白遮罩并写入 Alpha = 89。图案由图像工具生成，尺寸、遮罩与透明度由导出脚本精确约束。完整提示词见 `qa/imagegen-prompt.txt`。
