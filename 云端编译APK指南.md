# 云端编译 APK · 逐步操作指南

目标：**电脑上不装任何软件**（不装 Android Studio、不装 git、不装 JDK），只靠浏览器，让 GitHub 的服务器帮你编译出 APK。

全程大约 10 分钟，其中编译本身 3~5 分钟。

---

## 第 0 步：先准备好要上传的文件

需要上传的是 `课表\android` 目录里的这些内容（**注意：是 `android` 目录里面的东西，不是 `android` 这一层**）：

```
.github/            ← 编译脚本（关键，别漏）
app/                ← 应用源码与资源
gradle/             ← wrapper 配置
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
settings.gradle.kts
README.md
```

> 这是全工程**唯一容易搞错**的地方：仓库根目录必须直接看到 `settings.gradle.kts` 和 `.github`。
> 如果上传后变成 `仓库名/android/settings.gradle.kts`（多套了一层），后面的编译会因为"找不到构建脚本"直接失败。

我已经打好了结构正确的压缩包：**`课表\课表App-上传用.zip`**（根目录就是仓库根目录）。

⚠️ **但要让 GitHub 网页真正拿到目录结构，只有一个可靠办法**：网页上传框**不会解压 zip**。
所以第 3 步有两种走法，**推荐 3B**：

- **3A**：用 GitHub Desktop 客户端（见下）—— 它会把整个目录连同 `.github` 一起推上去，一次搞定
- **3B**：纯网页手动建文件（见下）—— 不需要装任何东西，但要手工创建 `.github` 那个隐藏目录

---

## 第 1 步：注册 / 登录 GitHub

打开 https://github.com 注册（邮箱即可）。已有账号直接登录。

> 国内访问 GitHub 有时不稳定。如果打不开，见文末「备用方案」。

## 第 2 步：新建仓库

1. 右上角 `+` → **New repository**
2. **Repository name** 填 `my-timetable`
3. 可见性选 **Public**（公开仓库的 Actions 编译时间**完全免费且不限量**；私有仓库每月只有 2000 分钟额度，虽然也够用，但没必要）
4. 下面的 `Add a README file` 等选项**全部不勾**
5. 点 **Create repository**

## 第 3 步 A：用 GitHub Desktop 上传（推荐，最省事）

1. 下载安装 **GitHub Desktop**：https://desktop.github.com （约 150 MB，比 Android Studio 小得多，且自带 git）
2. 打开后登录你的 GitHub 账号
3. 菜单 `File → Add local repository…` → 选择 `D:\deepseek\课表\android`（如果提示不是仓库，点它给的 **create a repository** 链接）
4. 左下角 Commit 信息随便写 `init`，点 **Commit to main**
5. 点右上角 **Publish repository**，**取消勾选** `Keep this code private`，点确定
6. 完成 —— 文件（含 `.github`）全部上传，随即自动开始编译

> GitHub Desktop 会自动忽略 `build/`、`.gradle/` 等垃圾目录，也不会漏掉点开头的文件，这正是它比网页上传可靠的原因。

## 第 3 步 B：纯网页上传（不装任何软件）

**关键难点**：Windows 资源管理器默认**不显示** `.github` 这个点开头的文件夹，全选时会漏掉它，导致编译不触发。所以 `.github` 我们**手动创建**，其余文件用上传功能。

**3B-1 先上传普通文件和目录**
1. 打开你新建的仓库，点 **uploading an existing file**（或访问 `https://github.com/你的用户名/my-timetable/upload/main`）
2. 先把 Windows 资源管理器的**查看 → 显示 → 隐藏的项目**打开，这样能看到 `.github`
3. 把 `android` 目录**里面的内容**拖进上传框：`app` 文件夹、`gradle` 文件夹、`build.gradle.kts`、`gradle.properties`、`gradlew`、`gradlew.bat`、`settings.gradle.kts`、`README.md`
   - **暂时不要管 `.github`**（下一步单独建）
4. Commit 信息写 `init`，点 **Commit changes**

**3B-2 手动创建编译脚本（这一步千万别省）**
1. 回到仓库首页，点 **Add file → Create new file**
2. 在文件名框里输入（**斜杠会自动创建目录**）：
   ```
   .github/workflows/build-apk.yml
   ```
3. 把下面这段**完整粘贴**进内容区：

```yaml
name: Build APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'

      - name: Build debug APK
        run: gradle assembleDebug --no-daemon --stacktrace

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: error
```

4. 页面底部点 **Commit changes** → 这次提交本身就会触发编译

**3B-3 核对结构**
仓库首页应当直接看到：`app`、`gradle`、`.github`、`build.gradle.kts`、`settings.gradle.kts`、`gradlew`、`gradlew.bat`、`gradle.properties`。
若看到的是一个 `android` 文件夹，说明多套了一层，把里面的内容重新传到根目录。


## 第 4 步：等它自动编译

1. 仓库顶部点 **Actions** 标签
2. 会看到一条 `Build APK` 的任务，状态是黄色小圆点（进行中）
3. 点进去可以看实时日志；等约 3~5 分钟变成 **绿色对勾**
4. 第一次运行时可能会有一个提示要求你确认启用 Actions，点 **I understand my workflows, go ahead and enable them**

## 第 5 步：下载 APK

1. 在刚跑完的那次运行页面，**滚到最底部**
2. 找到 **Artifacts** 区域，点 **app-debug-apk** 下载
3. 下载下来是个 zip，解压后里面就是 **`app-debug.apk`**

> 注意：下载 Artifacts **必须登录 GitHub**，未登录点下载会没反应或跳登录页。

## 第 6 步：装到手机

1. 把 `app-debug.apk` 传到手机（微信「文件传输助手」发送给自己最方便）
2. 手机上点开这个 apk 文件
3. 系统提示「禁止安装未知应用」→ 点**设置** → 允许该来源（微信/文件管理器）安装
4. 返回继续安装，完成后桌面出现「我的课表」图标

装好后第一次打开：进**设置**页 → 打开「开启提醒」→ 允许通知权限 → 点「去授权」把「闹钟和提醒」也允许（部分机型需要，否则提醒可能晚几分钟）。

---

## 出问题了怎么办

| 现象 | 原因与处理 |
|---|---|
| Actions 页面空白的，什么都没有 | `.github/workflows/build-apk.yml` 没上传成功。**最稳的修法**：用网页的 Add file → Create new file，路径填 `.github/workflows/build-apk.yml`，把第 3B-2 步的 YAML 粘进去 |
| 编译报 `Could not find method ...` 或找不到 `settings.gradle.kts` | 仓库根目录多套了一层 `android/`，把内容挪到根目录 |
| 编译报 JDK 版本错误 | 不用管，workflow 里已固定 JDK 17；若你改过 workflow 请改回 |
| 编译红叉，日志里是 `Could not resolve ...` | 网络抖动导致依赖没下全，点 **Re-run all jobs** 重试即可 |
| 编译成功但 Artifacts 是空的 | 看 `Upload APK` 那一步的日志；正常情况下 `app/build/outputs/apk/debug/` 下应有 `app-debug.apk` |
| 手机装不上，提示"应用未安装" | 手机里已装过同包名的旧版本且签名不同 → 先卸载旧的再装 |
| 想改应用名 / 图标 | 改 `app/src/main/res/values/strings.xml` 的 `app_name`，图标改 `res/drawable/ic_launcher_foreground.xml` |

---

## 备用方案：GitHub 打不开或 Actions 不可用

**方案 A：用 Gitee（码云）托管 + Gitee Go 编译**
好处是国内速度快。步骤：
1. https://gitee.com 注册，新建仓库，同样用网页上传同样的文件
2. 顶部进入 **流水线 / Gitee Go**，新建流水线，选择 Java 构建模板
3. 把构建命令设为 `gradle assembleDebug`（**不要用 `./gradlew`**，因为我们没有 wrapper jar；Gitee Go 自带 Gradle）
4. 构建后需要把 `app/build/outputs/apk/debug/app-debug.apk` 上传为**构建产物**才能在网页上下载

> 说实话：Gitee Go 的流水线配置界面和语法改版较频繁，我无法在离线环境下验证具体步骤是否与你看到的界面一致。
> 如果卡住了，最稳的替代是**方案 B**。

**方案 B：先继续用网页版课表**
`..\index.html` 那个单文件应用功能是完整的（除了系统级通知），用微信传到手机就能看课表。Android 版主要是多了"App 被杀也会响的提醒"这一个关键能力。

**方案 C：找一台能装的电脑**
任意一台能装 Android Studio 的电脑（同学/实验室的也行），打开工程 → Build APK，把产物拷过来装上即可。工程本身不绑定环境。

---

## 关于 `gradle-wrapper.jar`

你可能会注意到 `gradle/wrapper/` 里只有 `gradle-wrapper.properties`，没有 `gradle-wrapper.jar`。

- **不影响云端编译**：workflow 用的是 `gradle/actions/setup-gradle` + `gradle` 命令，全程不调用 `./gradlew`，所以不需要这个 jar
- 只有当你想在本地跑 `./gradlew` 时才需要它，届时在本地执行一次 `gradle wrapper` 就会生成
- 我刻意没有伪造一个同名二进制文件放进去，避免来源不明的可执行文件带来的安全风险
