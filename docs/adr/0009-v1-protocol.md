# ADR 0009 — 本地 V1-R1 协议底座

状态：已实现并通过#9/R1 android-runtime验收（NATIVE-20261007-9-02）；不是生产评分或UI验收。

## 决定

沿用 Room/LocalCipher/匿名vault，扩展现有ResurfaceEvent/RecallSession。单个Repository是迁移后状态所有者；StateFlow只投影事实，Compose只发送命令和真实可见回执。

每日(scope,date)行分开auto评估与成功quota，唯一约束与整事务保护并发。首次窗口不是到期保底。固定due扫描先于active/NO早退，按原定due排序；一个plan一条事件，延期不重建。

生产RandomEvaluator默认Unavailable，确定性selector仅测试构造注入；不得把它当正式概率算法。自动资格门槛的NO可缓存，模型Unavailable本身不写NO。时区只留证，命令捕获同一Instant/Zone。

## 内容、快照和曝光

正文/回忆/回应/Cue仍用既有AES-GCM Keystore密文格式。分阶段快照在下一条Cue实际可见之前锁定旧输入和旧Cue集合；原照片改变后续模式，不回写blind。停止事实使用RECALL_STOPPED＋cueEngineStopped，结果单独为NO_RECALL或CONTENT_PROVIDED；DIRECT_REVEAL为未提交回忆的揭晓结局。

请求Reveal先持久意图/稳定动作ID，再只读解密；只有成功可见回执标REVEALED。Complete/Defer/已成功回执可幂等重试；新媒体真实显示也延长30天，短Cue和暂缓不能缩短。

## 迁移

v1/v2→v3→v4保留ID/密文/旧时间/title和legacyState。旧假日期、标题、曝光、Cue授权保留LEGACY_UNKNOWN；缺失政策只从原mode回填，不补虚构日期。多个旧未完成事件串行成为legacy pending，恢复原会话，不静默丢历史。

v4追加RecallSession.inputRecallMode（旧行默认LEGACY_UNKNOWN），保存输入实际采集时的模式。归档/原照片曝光后原输入仍保留，但恢复/重复flush不能据此生成POST快照；只有内容真正改变或显式提交新回忆才更新输入模式。已锁定快照不再采集。删除墓碑不能被过时archive命令回退。

## 边界与验证

不改变同步payload/server schema，也不把新表冒充已同步。保留原型、字体、母图、折船与洋流。隔离验证包必须经aapt核对，独立数据库夹具；不覆盖com.unfinished.app、不清手机资料。结果和环境失败均记录实施报告。
