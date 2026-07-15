# 邑资源制作与应用

本文记录当前真正会被运行时读取的贴图、模型和接线方式。

## 创造标签命名

- 城池：`civitas:city` / `itemGroup.civitas.city` / “邑：城池”。
- 居民：`civitas:residents` / `itemGroup.civitas.residents` / “邑：居民”。
- 后续标签独立注册，例如“邑：征伐”“邑：经济”；不要重新使用笼统的 `civitas:civitas`。

## 当前资源

| 用途 | 路径 | 规格与说明 |
|---|---|---|
| 模组图标 | `src/main/resources/civitas.png` | 由 `neoforge.mods.toml` 的 `logoFile="civitas.png"` 读取。 |
| 人居民实体 | `assets/civitas/textures/entity/citizen/human_0.png` 至 `human_3.png` | 64×64 RGBA，原版宽臂玩家 UV。 |
| 猪人、牛人、羊人实体 | `assets/civitas/textures/entity/citizen/<race>_0.png` | 64×64 RGBA，人形玩家 UV。 |
| 居民蛋代表皮肤 | `assets/civitas/textures/item/citizen/<race>_0.png` | 物品纹理图集专用；修改实体代表皮肤时同步更新。 |
| 居民蛋模型 | `assets/civitas/models/item/*_spawn_egg.json` | 各年龄段缩放；公共六面 UV 与手持变换在 `citizen_spawn_egg_base.json`。 |
| 通用界面 | `assets/civitas/textures/gui/sprites/stone_city_panel.png` | 32×32、完全不透明；同目录 `.png.mcmeta` 定义 7 像素九宫格边框。 |

居民蛋模型中的所有纹理必须来自物品图集。不能同时引用 `minecraft:block/...` 和 `civitas:item/...`，否则模型烘焙会因跨图集失败并显示黑紫格。

## 更换居民皮肤

1. 保持 64×64 RGBA、原版宽臂玩家 UV、透明通道和像素绘制方式。
2. 实体皮肤放入 `textures/entity/citizen/`。
3. 创造标签居民蛋使用的代表皮肤还要同步放入 `textures/item/citizen/`。
4. 不要把脸部 UV 复制给头部六面；物品模型已经分别映射脸、后脑、左右侧、头顶和底部。

增加第五套人皮肤时，应先把外观数量提取为集中常量，再同步修改并测试：

- `CitizenRenderer` 的纹理选择；
- `ChildIdentityRules` 的出生外观生成；
- `CreateMigrationGroupService` 与管理员测试居民创建逻辑。

为猪人、牛人或羊人增加多套外观时，还要让其 `appearanceKey` 参与选择；只增加 PNG 不会被游戏引用。

## 仍可自行设计

- 城池核心目前复用原版方块贴图。独立贴图放在 `textures/block/`，并修改 `models/block/city_core.json`。
- 城池核心物品目前使用方块模型。独立图标应放在 `textures/item/` 并接入单独物品模型。
- 守卫显示真实原版武器、护甲和盾牌；自制服装或年龄服装需要新增实体渲染层。
- 建筑用途、治安、家庭、地图、巡逻和渗透提示目前主要使用文字、色块或世界线框。新增精灵放在 `textures/gui/sprites/`，并由 presentation 类通过 `blitSprite` 接线。
- 儿童差异目前由模型和体型完成。儿童专属皮肤需要让渲染器按 `AgeStage` 选择资源。

## 应用与检查

1. 文件名只使用小写字母、数字和下划线，路径与资源标识完全一致。
2. 运行 `.\gradlew.bat processResources`，确认文件进入 `build/resources/main/`。
3. 运行 `.\gradlew.bat build`，用 `jar tf build/libs/civitas-1.0.jar` 确认资源已打包。
4. 客户端验证时检查日志中的 `missing texture`、`Unable to bake item model`、资源重载和渲染错误。
