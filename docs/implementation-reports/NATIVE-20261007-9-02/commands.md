# 本Run执行命令与重启证据口径

日期：2026-10-07；Run：NATIVE-20261007-9-02；契约#9/R1。命令由主助手实际执行，下面为可复现形式，设备参数以占位表示，不读取正式资料库。

## 编译与安装

JAVA_HOME=%JAVA_HOME%，ANDROID_HOME=%USERPROFILE%/Android。
android/gradlew.bat -p android :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest -PprotocolTest --offline --console=plain

每次安装前用aapt dump badging检查实际applicationId为com.unfinished.app.protocoltest及独立runner com.unfinished.app.protocoltest.test，否则拒绝安装。
adb -s <isolated-test-device> install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s <isolated-test-device> install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

两个目标分别为本Run启动的API35 emulator-5588和已授权的魅族21。正式com.unfinished.app从未作为install、force-stop或测试库目标。

## 正常套件

adb -s <isolated-test-device> shell am instrument -w -r -e class com.unfinished.app.data.ProtocolRuntimeTest,com.unfinished.app.data.VisibilityRuntimeTest com.unfinished.app.protocoltest.test/androidx.test.runner.AndroidJUnitRunner

正常套件注册28项：26成功，2个phase用例显式Assume跳过；不能把OK28当28项实际执行。

## 本次真实进程重启（两端各执行一次）

adb -s <isolated-test-device> shell am instrument -w -r -e class com.unfinished.app.data.ProtocolRuntimeTest#seedProcessRestart -e processPhase seed -e processRunId NATIVE-20261007-9-02 com.unfinished.app.protocoltest.test/androidx.test.runner.AndroidJUnitRunner
检查输出含OK (1 test)，否则不继续。
adb -s <isolated-test-device> shell am force-stop com.unfinished.app.protocoltest
adb -s <isolated-test-device> shell pidof com.unfinished.app.protocoltest
上一步无PID输出；force-stop自身正常无输出。
adb -s <isolated-test-device> shell am instrument -w -r -e class com.unfinished.app.data.ProtocolRuntimeTest#recoverProcessRestart -e processPhase recover -e processRunId NATIVE-20261007-9-02 com.unfinished.app.protocoltest.test/androidx.test.runner.AndroidJUnitRunner
输出OK (1 test)。恢复断言检查私有固定测试库的同一STOP/DIRECT状态、唯一事件、动作ID、无伪曝光/空快照。

库名：protocol-process-stop-NATIVE-20261007-9-02.db、protocol-process-direct-NATIVE-20261007-9-02.db。重复验证必须换processRunId后缀，seed/recover同值；不可删除旧夹具或用户库。

## 断网（仅模拟器）

adb -s emulator-5588 shell cmd connectivity airplane-mode enable
adb -s emulator-5588 shell svc wifi disable
adb -s emulator-5588 shell svc data disable
检查airplane enabled、Wifi disabled、ip route空，再跑完整套件及进程重启。没有修改魅族联网状态。

完成后恢复模拟器airplane disabled、请求wifi/data enable；初次radio状态异步未确认，因此重开并等待Wifi enabled，再adb -s emulator-5588 emu kill关闭本次无窗口实例。mobile_data初值未采集，不据恢复命令推定其与原值一致。

## 魅族启动

adb -s <meizu-test-device> shell am start -W -n com.unfinished.app.protocoltest/com.unfinished.app.MainActivity
结果Status ok、COLD、PID存在。仅只读dumpsys正式包版本元信息：前后versionCode4/versionName0.4.0，不查询旧数据库。
