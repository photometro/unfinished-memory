# 潮汐树：形成机制与抽象首页生成约束

- 研究日期：2026-10-03（Asia/Shanghai）
- 对象：抽象版首页的潮沟地貌与退潮表达；不修改真实版、不涉及记忆浮现算法。
- 证据范围：一手研究论文；既有照片仅供形态参考。
- 定位：**机制启发的可视化生成器，不是经验证的潮汐水动力或地貌演化模拟。**

## 一、来源支持的事实

1. 潮沟首先是被水流切开的地形，不是生物树。初始微地形会使水流集中；在人工恢复潮汐的实地观测中，新沟优先出现于低高程区，涨潮和落潮流都在那里集中，松散新沉积物也更易被侵蚀。这是该场地的观测结论，不应把“所有低处必定成沟”当定律。[Vandenbruwaene 等，2012，摘要与第 5 节，原论文 PDF](https://www.vliz.be/imisdocs/publications/257513.pdf)

2. 早期潮沟发展包含局地切入、沟头向内陆的溯源延伸与支沟形成。D'Alpaos 等的模型以水面高程梯度产生的底部剪应力及侵蚀阈值控制切沟，并非每隔固定距离、固定角度分出两条枝。[D'Alpaos 等，2005，摘要及第 3 节](https://agupubs.onlinelibrary.wiley.com/doi/full/10.1029/2004JF000182)

3. 潮沟承担涨、落潮交换。2024 年水槽实验观察到海口附近的小沟向内陆扩展，主沟逐渐加宽；落潮阶段继续溯源侵蚀，涨潮阶段也出现内陆端沉积。不能把潮汐树简化成只有永久向海流动的河流。[Hautekiet 等，2024，第 3.1 节](https://esurf.copernicus.org/articles/12/601/2024/)

4. 沟的断面与通过的潮汐水量相关，宽度不是与“递归深度”简单成比例。相关性不意味着任何场地每个合流点都遵守同一像素宽度公式。[D'Alpaos 等，2005，第 2 节](https://agupubs.onlinelibrary.wiley.com/doi/full/10.1029/2004JF000182)

5. 单次退潮与多年地貌演化是不同时间尺度。实验中的网络会扩展、退缩并再利用旧沟，且并非所有路径保持不变。因此首页应表现已有地貌被潮水显露，不能暗示每次开屏几秒就冲刷出整棵树。[Stefanon 等，2012，实验结果](https://agupubs.onlinelibrary.wiley.com/doi/abs/10.1029/2012gl051953)

## 二、翻译为 UI 规则（设计推断，不是论文结论）

| 生成环节 | 本原型的规则 | 应避免 |
| --- | --- | --- |
| 边界 | 先给出潮滩、海域与少量可交换水的海口；主沟清晰通向海侧 | 漂在页面中央、没有岸海关系的树图标 |
| 微地形 | 固定种子的低坡度高程场加入多尺度起伏，给不同汇水区不同形状 | 对称树冠、均匀扇形、重复锯齿 |
| 网络 | 从微地形取得汇水路径与合流关系，再用示意阈值筛选成沟 | 随机画线后仅凭相邻距离接到中央主干 |
| 层级 | 海口主沟较宽，支沟多级汇入，末梢变细；宽度连续变化 | 等宽面条、所有支沟粗细一样、合流突然断开 |
| 地貌 | 沟心低、沟岸略高，潮滩保留完整沙泥面，颜色和阴影表达“切入” | 浮在背景上的发光装饰线、满屏树枝 |
| 潮位 | 同一高程场决定淹没与显露；高处先露，低沟较晚退水，深沟保留水 | 整张图等速透明、单条水平擦除、把沟像枝条一样画出来 |
| 状态 | 地形固定；正常开屏只动水位与水流方向，减少动画时直接显示终态 | 每次切换页面重抽一棵树、动效期间挪动功能按钮 |

实现流程应能被说明为：**微地形 → 示意排水路径 → 汇流与阈值筛选 → 连续沟宽／切沟高程 → 潮位覆盖 → 首页构图**。潮沟应在关掉动画、只看静帧时仍然可辨认。

## 三、简化模型的科学边界

- **地形下降方向不等于真实潮汐流向。**D'Alpaos 等使用水面梯度定义排水路径，潮汐分水界还可能随时间变化。D8 最陡地形下降、优先洪水填洼或最短路径算法只能作为图形生成代理，不可称为论文模型复现。[D'Alpaos 等，2005，第 2、3.1 节](https://agupubs.onlinelibrary.wiley.com/doi/full/10.1029/2004JF000182)
- “汇水量 × 坡度”的阈值不是有物理单位的底部剪应力；图形阈值只能调形态，不能据此报告真实侵蚀速度、沟深或潮周期。
- 单一出边的栅格网络天然偏向无环树状结构；真实潮沟会受双向流、沉积、植被、岸坡与水沙反馈影响。本原型只选取易辨认的树状潮沟形态，不代表所有潮滩。[Hautekiet 等，2024，第 1、3 节](https://esurf.copernicus.org/articles/12/601/2024/)
- 一侧海域、少量海口、沟宽上下限与留白是手机构图选择，不应写作钱塘江的普适规律。上述研究主要来自威尼斯潟湖、斯海尔德河口及水槽，不能把场地参数直接挪用为钱塘江数据。
- 对外文案应使用“参考潮沟形成机制的抽象表现”；不要使用“真实模拟钱塘江潮汐”“按记忆数量科学生成地貌”等未经验证的表述。

## 四、形态验收

本轮实现选择不规则平面三角网格上的加权排水图，避免 D8 的八方向视觉偏差。空间阻力、水量累计、反馈与沟宽都属于图形代理；没有复现论文的水面梯度／剪应力模型，也未把真实溯源侵蚀过程作时间积分。退潮使用同一图形模型派生的示意切沟高度场，不能据此计算自然潮位或淹没范围。

- 不依赖名称或文案，能看出“浅滩上被水切出的、汇向海侧的多级潮沟”。
- 主沟、支沟、末梢连续连接；不出现孤立树枝、交叉穿线和匀称圣诞树。
- 同一构图重放退潮时地形不变；终态仍有低处沟水与露出的滩面。
- 窄屏也能保留海口与主要汇流关系，文字区、导航与触控范围不被细沟干扰。
- 保留抽象版的淡雅材质；形态表达先成立，再增加少量水面微动。

## 原始来源

- D'Alpaos, A. 等（2005）：[Tidal network ontogeny: Channel initiation and early development](https://agupubs.onlinelibrary.wiley.com/doi/full/10.1029/2004JF000182)，JGR Earth Surface，DOI 10.1029/2004JF000182。
- Vandenbruwaene, W., Meire, P., Temmerman, S.（2012）：[Formation and evolution of a tidal channel network within a constructed tidal marsh](https://www.vliz.be/imisdocs/publications/257513.pdf)，Geomorphology 151–152，114–125，DOI 10.1016/j.geomorph.2012.01.022。
- Hautekiet, S. 等（2024）：[On the relative role of abiotic and biotic controls in channel network development: insights from scaled tidal flume experiments](https://esurf.copernicus.org/articles/12/601/2024/)，Earth Surface Dynamics 12，601–619，DOI 10.5194/esurf-12-601-2024。
- Stefanon, L. 等（2012）：[Signatures of sea level changes on tidal geomorphology: Experiments on network incision and retreat](https://agupubs.onlinelibrary.wiley.com/doi/abs/10.1029/2012gl051953)，Geophysical Research Letters，DOI 10.1029/2012GL051953。
