# 未完成：GitHub 图标辅助工具调查

日期：2026-10-11。范围：一手 README、源文件许可证和官方帮助文档的只读调查；没有安装、克隆、接入 MCP、生成图标或修改 App。本报告的“适合”属于工作流判断，不是软件实测结果。

## 结论

优先组合为 **Penpot 做可编辑的轮廓与构图 → 静态 SVG 渲染生成实际小尺寸对照 → Android Studio 检查自适应裁切与主题图标**。SVGO 是定稿后的代码清理，不负责美化。SVG-Edit 是轻量替代编辑器，不必与 Penpot 同时安装。这一组合能减少靠提示词反复碰运气，但不会自动解决主体选错、形状不好看或品牌辨识度不足。

## 四个精确项目

### 1. penpot/penpot — 主编辑工具，最值得优先评估

- 核实：浏览器设计平台，使用 SVG 等开放标准，支持自托管；主项目为 MPL-2.0。[README](https://github.com/penpot/penpot/blob/develop/README.md)、[许可证](https://github.com/penpot/penpot/blob/develop/LICENSE)
- 能直接帮助这次设计：路径节点与贝塞尔控制柄用于精修曲线；对齐、分布、遮罩和布尔操作用于控制留白与形状关系。支持非破坏性布尔组合，而不是只能反复生成整张位图。[官方 Layers 文档](https://help.penpot.app/user-guide/designing/layers/)
- 对 AI 的辅助：官方 MCP 支持读取、创建、变换设计数据，可修改图层、样式与组件并导出素材。远程模式不等于拥有本地文件访问；需要启用账号集成、配置客户端并连接正在使用的设计页。本轮没有接入，也不声称当前可调用。[MCP 源码 README](https://github.com/penpot/penpot/blob/develop/mcp/README.md)、[官方 MCP 帮助](https://help.penpot.app/mcp/)
- 限制：它是编辑环境，不是“自动生成好品牌标志”的模型；也不是 Android 专用图标打包器。增加完整自托管服务对本次单图标任务可能过重，优先评估浏览器工作方式。

### 2. SVG-Edit/svgedit — 轻量 SVG 编辑替代

- 核实：现代浏览器中的 JavaScript SVG 绘图编辑器，包含可嵌入的 svgcanvas，提供在线版本与自托管说明。[README](https://github.com/SVG-Edit/svgedit/blob/master/README.md)
- 用途判断：若只需改一枚 SVG 的形状、位置与配色，比建设完整设计平台更轻；可作为可编辑源图的检查工具，而不是把最终结果限定为 AI 大图。
- 许可证：主许可证 MIT；仓库明确列出部分文件采用 Apache、LGPL、X11 等其他许可证，不能把整个依赖分发包简单描述为“全 MIT”。[MIT 文件](https://github.com/SVG-Edit/svgedit/blob/master/LICENSE-MIT.txt)、[逐文件许可证列表](https://github.com/SVG-Edit/svgedit/blob/master/licenseInfo.json)
- 限制：普通矢量编辑，不自带完整 Android 安全区、裁切、主题图标与品牌辨识度验收；本轮没有核实或接入可供代理直接调用的 MCP。

### 3. linebender/resvg — 静态渲染与批量小尺寸对照

- 核实：支持 Rust/C 库与 CLI 的静态 SVG 渲染器，README 描述跨平台与可重复输出；许可证为 Apache-2.0 或 MIT 二选一。[README](https://github.com/linebender/resvg/blob/main/README.md)、[Apache 文件](https://github.com/linebender/resvg/blob/main/LICENSE-APACHE)、[MIT 文件](https://github.com/linebender/resvg/blob/main/LICENSE-MIT)
- 用途判断：用同一 SVG 自动生成 48px、64px、96px 等像素预览与黑白版，便于并排比较主体、间隙、重心。圆形/圆角裁切、不同壁纸和其他图标并置需要额外的预览布局或脚本，**不是 resvg 已内置的 Android 图标功能**。若现有依赖已能可靠渲染，无需为了名字再增加一套。
- 限制：不支持 SVG 动画，不是 Compose/Launcher 渲染器；也不直接生成 Android 自适应 XML。静态预览不能替代真机裁切与主题图标测试。

### 4. svg/svgo — 导出后清理，不负责审美

- 核实：Node.js 库与 CLI，通过可配置插件清理 SVG 的冗余元数据、无效元素等，主许可证 MIT。[README](https://github.com/svg/svgo/blob/main/README.md)、[许可证](https://github.com/svg/svgo/blob/main/LICENSE)
- 用途判断：保留原始设计文件，另存生产用优化 SVG，再进行优化前后渲染对照。适合清理导出资源，不应拿它来证明“更好看”。
- 限制：路径优化可改变命令形式，并按配置四舍五入数值；不要为了减少体积任意降低精度，也不要在优化版上代替可编辑母版继续设计。[convertPathData 官方说明](https://svgo.dev/docs/plugins/convertPathData/)

## Android 预览与资源生成：优先官方现成工具

Android Studio 的 Image Asset Studio 支持前景、背景、单色层，预览圆形、squircle、圆角方形等裁切，并生成不同密度资源、传统图标及商店图。当前官方文档也提供主题图标与壁纸着色预览。这部分比选择一个旧 GitHub 生成器更直接；它是工程校验与打包工具，不会纠正图形本身不好看的问题。[Android 官方工具说明](https://developer.android.com/studio/write/create-app-icons)

## 排除或降级的候选

- **romannurik/AndroidAssetStudio**：确实是 Android 资源生成器，主许可证 Apache-2.0，但 README 首句已明确停止维护，并建议使用 icon.kitchen 或 Android Studio。仅作旧实现参考，不作本轮主工具。[README](https://github.com/romannurik/AndroidAssetStudio/blob/master/README.md)、[仓库](https://github.com/romannurik/AndroidAssetStudio)
- **IconKitchen 名称陷阱**：AndroidAssetStudio 作者链接的是 [icon.kitchen 官网](https://icon.kitchen/)。本轮 GitHub 搜索发现的 **MarcusA27/iconkitchen** 是另一个 macOS 14+/Swift/Metal 的早期玻璃图标编辑项目，README 仍把图像/SVG 图层、折射等列为未完成项；不能当成前者的源码，更不适合作为当前 Windows + Android 的主工具。[该项目 README](https://github.com/MarcusA27/iconkitchen/blob/main/README.md)。官网本轮文本抓取只返回 JavaScript 提示，因此不对其当前具体预览或导出能力作额外承诺。
- 通用 UI 图标库可用作视觉词汇参考，但不能替代原创品牌轮廓；海浪 UI 库或液态玻璃组件也不能自动赋予桌面图标品牌辨识度。此处是针对未完成工作流的判断，非这些项目的缺陷认定。

## 后续建议（尚未执行）

1. 在可编辑矢量画布中先比较少量、主体轮廓真正不同的概念，不先叠材质。
2. 同步检查实际小尺寸黑白轮廓；先淘汰主体不明、重心失衡与类别误认的方案。
3. 用户认可主体后再精修色彩、曲线与材质，保留明确的 SVG 母版。
4. 用 Android Studio 验证自适应与单色版，再进入真机安装；工具过审不等于用户审美或识别测试通过。

授权边界：安装/接入外部设计平台、修改客户端配置、覆盖正式图标或安装到手机，需要后续明确任务授权。本次仅完成调查与报告。
