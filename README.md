# AutoAppLauncher（定时启动器）

一款 Android 定时任务工具，到点自动打开你指定的 App。支持熄屏触发、节假日识别、随机时间段等高级功能。

专为 MIUI 系统深度适配，解决小米手机后台杀进程、锁屏无法拉起等痛点。

## 📲 下载安装

> 无需自己编译，直接下载 APK 安装即可使用。

**[⬇️ 下载 AutoAppLauncher v1.6 APK (2.2MB)](https://github.com/Leo951024/AutoAppLauncher/raw/main/releases/AutoAppLauncher-v1.6.apk)**

也可以前往 [`releases` 目录](https://github.com/Leo951024/AutoAppLauncher/tree/main/releases) 手动下载。

安装时可能需要开启「允许安装未知来源应用」，安装完成后请按照 App 内引导开启 MIUI 相关权限。

## 核心功能

| 功能 | 说明 |
|------|------|
| 定时拉起 App | 自定义时间，到点后自动打开目标应用 |
| 随机时间段触发 | 选取一个时间段，系统在区间内随机选一个时间点触发 |
| 熄屏也能触发 | 锁屏/屏幕关闭状态下可靠拉起目标 App |
| 任务查看与修改 | 随时查看和编辑已创建的任务 |
| 开机自动启动 | 手机重启后自动恢复所有定时任务 |
| 后台常驻保活 | 前台服务 + 常驻通知，防止被系统杀死 |

## 6 种重复模式

- **仅一次** — 触发后自动取消
- **每天** — 每天固定时间触发
- **每周** — 每周指定星期几触发
- **法定节假日** — 仅在春节、国庆等法定放假期间触发（联网获取）
- **非法定节假日** — 法定放假期间不触发，其余日子正常执行
- **法定工作日** — 仅在法定工作日触发，调休补班也算工作日（联网获取）

## 技术亮点

- **MIUI 深度适配** — 引导开启自启动、省电无限制、后台弹窗、锁屏不清理 4 项关键权限
- **三重熄屏保障** — 全屏通知 + 屏幕唤醒 + 透明桥接 Activity，确保锁屏状态下可靠拉起
- **节假日智能识别** — 联网获取法定节假日数据，本地缓存 30 天，断网时 fail-open 不漏触发
- **超小体积** — APK 仅 2.2MB

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **调度**: AlarmManager (setExactAndAllowWhileIdle)
- **后台**: Foreground Service + 常驻通知
- **存储**: Room Database
- **App拉起**: PackageManager.getLaunchIntentForPackage()
- **开机自启**: BOOT_COMPLETED Receiver
- **构建**: AGP 8.7.3, Kotlin 2.0.21, KSP

## 运行环境

- 最低支持: Android 11 (API 30)
- 目标设备: 小米9 / MIUI 12.5 / Android 11
- 已适配 MIUI 12.5 系统

## 项目结构

```
app/src/main/java/com/leo/autoapplaucher/
├── data/                    # 数据层
│   ├── TaskEntity.kt        # 任务实体
│   ├── ExecutionLogEntity.kt # 执行日志
│   ├── HolidayEntity.kt     # 节假日缓存
│   ├── TaskDao.kt           # 任务DAO
│   ├── HolidayDao.kt        # 节假日DAO
│   ├── AppDatabase.kt       # Room数据库
│   └── HolidayRepository.kt # 节假日数据源
├── scheduler/               # 调度层
│   ├── AlarmScheduler.kt    # 闹钟调度器
│   └── AlarmReceiver.kt     # 闹钟接收器
├── service/
│   └── AppLauncherService.kt # 前台服务
├── receiver/
│   └── BootReceiver.kt      # 开机自启
├── ui/                      # UI层
│   ├── screen/              # 页面
│   ├── component/           # 组件
│   ├── viewmodel/           # ViewModel
│   ├── theme/               # 主题
│   └── LaunchBridgeActivity.kt # 透明桥接Activity
└── util/
    └── MiuiUtils.kt         # MIUI适配工具
```

## 使用场景

- 工作日自动打开钉钉/企业微信打卡
- 每天定时打开背单词/学习 App
- 固定时间打开运动 App 提醒锻炼
- 随机时间段打开音乐 App

## 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建（已配置R8压缩+资源压缩）
./gradlew assembleRelease
```

APK 输出路径: `app/build/outputs/apk/release/app-release.apk`

## License

MIT License - 可自由使用、修改、分发
