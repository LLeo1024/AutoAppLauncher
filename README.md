# AutoAppLauncher（定时启动器）

一款 Android 定时任务工具，到点自动打开你指定的 App。支持熄屏触发、节假日识别、随机时间段等高级功能。

专为 MIUI 系统深度适配，解决小米手机后台杀进程、锁屏无法拉起等痛点。

## 下载安装

> 无需自己编译，直接下载 APK 安装即可使用。

**[⬇️ 下载 AutoAppLauncher v1.6 APK (2.2MB)](https://github.com/Leo951024/AutoAppLauncher/raw/main/releases/AutoAppLauncher-v1.6.apk)**

也可以前往 [`releases` 目录](https://github.com/Leo951024/AutoAppLauncher/tree/main/releases) 手动下载。

安装时可能需要开启「允许安装未知来源应用」，安装完成后请按照下方权限配置完成设置。

## 系统权限配置

安装后请务必完成以下权限设置，否则定时任务可能无法正常触发。App 内「权限设置」页面可点击对应项直接跳转到系统设置。

### MIUI 专属权限（小米手机必配）

| 权限项 | 作用 | 手动设置路径 |
|--------|------|-------------|
| 自启动 | 重启手机后自动恢复所有定时任务 | 设置 → 应用设置 → 应用管理 → 定时启动 → 自启动 |
| 省电策略：无限制 | 防止待机时 App 被系统冻结导致闹钟不触发 | 设置 → 应用设置 → 应用管理 → 定时启动 → 省电策略 |
| 后台弹出界面 | 允许从后台拉起目标应用（**核心功能必需**） | 设置 → 应用设置 → 应用管理 → 定时启动 → 后台弹出界面 |
| 锁屏不清理 | 防止锁屏后 App 被清理导致任务失效 | 设置 → 应用设置 → 应用管理 → 定时启动 → 锁屏不清理 |

### Android 标准权限

| 权限项 | 作用 |
|--------|------|
| 电池优化：已忽略 | 允许 App 在后台持续运行，不被电池优化策略限制 |
| 通知权限 | 前台服务需要通知权限才能显示常驻通知保活 |
| 精确闹钟权限 | 确保定时闹钟精确触发，不延迟 |
| 悬浮窗权限 | 熄屏状态下拉起目标应用所需 |
| 开机自启 | 手机重启后自动恢复定时任务 |

> **提示**：以上权限均可在 App 内「权限设置」页面点击对应项直接跳转到系统设置。如果跳转失败，请手动前往：设置 → 应用设置 → 应用管理 → 定时启动 → 权限管理。

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

### 钉钉自动打卡（推荐）

配合钉钉的「极速打卡」功能，可实现到点全自动打卡，无需手动操作：

1. 打开钉钉 → **我的** → **设置** → **考勤设置**
2. 开启 **极速打卡**（上班/下班打卡各一个开关，都打开）
3. 确认钉钉已登录、定位权限已开启、已在考勤组范围内
4. 在 AutoAppLauncher 中创建任务：
   - 目标应用选择 **钉钉**
   - 触发时间设为上班/下班打卡时间（建议提前 1-2 分钟）
   - 重复模式选择 **法定工作日**（调休补班也会自动打卡）

> **使用前请检查**：钉钉极速打卡是否已开启、钉钉是否保持登录状态、手机定位是否打开。极速打卡仅在考勤时间范围内生效，建议触发时间设在考勤时间窗口内。

### 其他场景

- 每天定时打开背单词/学习 App，养成习惯
- 固定时间打开运动 App 提醒锻炼
- 随机时间段打开音乐 App，每天都有小惊喜

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
