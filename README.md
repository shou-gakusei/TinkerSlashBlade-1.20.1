# Tinker's SlashBlade / 刀锻冶匠魂 · 1.20.1 移植

把**匠魂（Tinkers' Construct 3）的材料体系**接进**拔刀剑（SlashBlade Resharped）**：
用匠魂的材料铸刀、装配、研磨，让每一把刀都带着材料自己的数值、刀性与刃色。

这是 1.12.2 时代 **刀锻冶匠魂（TinkerSlashBlade）** 在 Minecraft 1.20.1 上的重写移植。

---

## 特性

### 材料决定一切
- **37 种 TAIGA 材料** × 2 种刀型（打刀 / 白鞘），数值取自匠魂的 `HeadMaterialStats`：
  耐久 = 材料耐久 × 刀型系数 × 刀柄倍率，攻击 = 材料攻击 × 刀柄攻击倍率 + 刀鞘加成。
- 每种材料一套**刃纹贴图**（按材料色漂染刀身），换材料一眼能看出来。
- 刀技（SA）按材料分配：火系横扫、穿刺突刺、沉重圆斩、噬魂虚空斩、狂战樱花落……

### 刀匠台
- **拔刀剑锻造台 `blade_forge`**：刀胚 + 1~3 份同材料锭 → 刀。多放锭只加耐久，每份 +10%。
- **拔刀剑装配台 `blade_station`**：刀身 + 刀柄 + 刀鞘 → 刀，三件可以是**不同材料**。
- 两个台子都有完整界面（背包/快捷栏槽位对齐）、掉落内容物、比较器输出。

### 刀部件（走匠魂产线）
- `刀身 / 刀柄 / 刀鞘` 都是匠魂的 **`ToolPartItem`**，绑定材料属性类型（刀身=Head，刀柄=Handle），
  因此能吃匠魂的两条产线：
  - **浇筑**：金模 + `taiga:molten_*` 熔融材料 → 部件，材料由流体决定；
  - **部件制造台**：材料 + 图案 → 部件（木头、石头这类不能浇的材料走这条）。
- **铸模**：`tsb_part_*_cast`，用「部件浇金」得到（`cast_consumed` + `switch_slots`），和匠魂原生铸模一致。
- 部件贴图是**白膜**，运行时用 `ItemColor` 按材料色相乘，一张贴图出 37 种颜色。

### 刀性（Trait）
匠魂特性只对匠魂工具生效，所以本模组按特性 id 重做了近战效果，并且**三个部件都参与**：

| 部件 | 刀性强度 |
|---|---|
| 刀身 | 100% |
| 刀柄 | 50% |
| 刀鞘 | 50% |

已实现：灼热点燃、锯齿追伤、穿刺破甲、沉重击退+迟滞、轻盈攻速、噬魂收魂、狂战残血加成、不稳随机效果等；
与战斗无关的特性（鞣制、培育、时运、矮人、崩解……）仅作显示。

### 其他
- **研磨**：白鞘 + 3 锭同材料 → 打刀，保留附魔/耀魂/击杀数，耐久按比例换算。
- **修复**：刀 + 同材料锭（每锭 25%）。
- **手册**《刀匠标准手册》：基于 Mantle 书系统，和《材料与你》同一套界面。
- **JEI 集成**：「刀匠锻造」74 条（刀胚 × 材料 × 刀型）、「刀匠浇筑」111 条（金模 × 材料 × 部件）。

---

## 依赖

| 依赖 | 版本 | 说明 |
|---|---|---|
| Minecraft | 1.20.1 | |
| Forge | 47.x（开发用 47.4.8） | |
| [SlashBlade Resharped](https://github.com/0999312/SlashBlade_Resharped) | 1.9.x | 刀本体（`ItemSlashBlade`），**必需** |
| TConstruct | 3.10+ | 材料 / 属性 / 浇筑 / 部件制造，**必需** |
| Mantle | 1.11+ | 书系统与数据加载，**必需** |
| TAIGA | 1.x | 37 种材料与熔融流体，**必需** |
| JEI | 15.x | 可选，装了才有配方分类 |

---

## 上手

```
材料锭 + 刀胚  →  锻造台  →  刀
   锭 → 部件制造台/浇筑（配铸模）→ 刀身/刀柄/刀鞘 → 装配台 → 刀
白鞘 → 研磨（+3 锭）→ 打刀（刀技显现）
```

1. **做刀胚**：工作台合成 `tsb_blade_blank`（打刀胚）或 `tsb_blade_blank_white`（白鞘胚）。
2. **直接锻造**：锻造台放刀胚 + 1~3 锭同材料，产物格取刀。
3. **走部件路线**：浇筑/部件制造台做出三件部件，再进装配台装配（可混材料）。
4. **妖刀化**：给刀附魔——刀本身已经带名字，附魔后即满足拔刀剑的「妖刀」条件，可放刀技。

---

## 构建

需要 **JDK 17**。

第三方 mod 的 jar 不进仓库，构建脚本默认从 `../taiga3/libs` 读取（也可用 `-PlocalLibs=<目录>` 指定）：

```
SlashBladeResharped-1.20.1-1.9.65.jar
TConstruct-1.20.1-3.10.2.92.jar
Mantle-1.20.1-1.11.79.jar
taiga3-1.20.1-1.3.3.jar        （放在 ../taiga3/build/libs）
```

```bash
./gradlew build          # 产物：build/libs/tinkersslashblade-1.20.1-<ver>.jar
./gradlew runClient      # 开发环境客户端
```

---

## 仓库结构

```
src/main/java/com/whfwtf/tsb/
  TSB.java                 主类与注册
  TsbItems/TsbBlocks/...   物品、方块、方块实体、菜单、界面
  TsbBladeItem.java        刀（继承拔刀剑的 ItemSlashBlade）
  TsbPartItem.java         刀部件（匠魂 ToolPartItem）
  TsbAssembly.java         锻造/研磨/修复/装配的统一算法
  TsbTrait.java            刀性效果
  TsbMaterials.java        材料解析（TAIGA 表 + 匠魂注册表兜底）
  compat/jei/              JEI 插件与两个自定义分类
  client/                  界面、书、染色

src/main/resources/
  assets/tinkersslashblade/   贴图、模型、语言、Mantle 书
  data/tinkersslashblade/     配方（浇筑/部件制造/铸模/锻造台等）
  data/tconstruct/tags/       把部件挂进匠魂的 parts 标签

tools/                    数据与贴图生成脚本（改了数据重跑即可）
  gen_tsb_materials.py      从 TAIGA 材料定义生成 37 种材料表
  gen_blade_textures.py     按材料色生成刃纹
  gen_station_textures.py   刀匠台/部件/铸模贴图
  gen_casting_recipes.py    按材料生成浇筑配方
  gen_book.py               生成手册数据
```

---

## 已知限制

- 部件目前只做了**刀身 / 刀柄 / 刀鞘**三件；匠魂的「绑定」类小件没有对应位置。
- 匠魂自带的浇筑/部件制造 JEI 分类不收录第三方部件，因此浇筑配方由本模组**自建分类**展示。
- 非 TAIGA 的匠魂材料也能装配（数值走匠魂注册表兜底），但没有刀性与专属刃纹色。

## 致谢

- **TConstruct / Mantle**（MIT）：材料体系、浇筑与部件制造、书系统；手册内页边框贴图取自 TConstruct，铸模画法亦参考其原生贴图。
- **SlashBlade Resharped**：刀本体与刀技机制。
- **TAIGA**：37 种材料的数值与熔融流体。
- 1.12.2 原版 **刀锻冶匠魂** 提供了整体设计思路。

## 许可

MIT，见 [LICENSE](LICENSE)。
