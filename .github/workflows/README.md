# GitHub Actions CI/CD 工作流说明

本项目包含两个主要的 GitHub Actions 工作流，用于自动化构建和发布 FlutterX IntelliJ IDEA 插件。

## 工作流概述

### 1. 发布工作流 (`release.yml`)

**触发条件**: 当推送以 `v` 开头的 tag 时自动触发（如 `v5.7.0`、`v1.2.3`）

**主要功能**:
- ✅ 自动更新插件版本号
- ✅ 自动更新 CHANGELOG.md
- ✅ 编译构建插件
- ✅ 创建 GitHub Release
- ✅ 上传插件文件到 Release
- ✅ 提交版本变更到主分支

### 2. 测试构建工作流 (`build-test.yml`)

**触发条件**: 推送到主分支或创建 Pull Request 时触发

**主要功能**:
- ✅ 验证代码可以正常编译
- ✅ 运行测试用例
- ✅ 插件验证
- ✅ 上传构建产物

## 使用方法

### 发布新版本

1. **确保代码已准备就绪**
   ```bash
   git add .
   git commit -m "feat: 新功能描述"
   git push origin main
   ```

2. **创建并推送 tag**
   ```bash
   # 创建 tag（版本号格式：v主版本.次版本.修订版本）
   git tag v5.8.0
   
   # 推送 tag 到远程仓库
   git push origin v5.8.0
   ```

3. **等待 CI 自动执行**
   - 工作流会自动触发
   - 在 GitHub 的 Actions 页面可以查看进度
   - 完成后会自动创建 Release

### 版本号规范

推荐使用 [语义化版本控制](https://semver.org/lang/zh-CN/)：

- `v1.0.0` - 主要版本（重大功能变更）
- `v1.1.0` - 次要版本（新功能添加）
- `v1.1.1` - 修订版本（bug 修复）

### CHANGELOG 格式

工作流会自动更新 `CHANGELOG.md` 文件。请确保在发布前手动更新 `## Unreleased` 部分的内容：

```markdown
## Unreleased

### Added
- 新增功能描述

### Fixed
- 修复的 bug 描述

### Changed
- 变更的功能描述
```

## 工作流详细步骤

### 发布工作流步骤：

1. **检出代码** - 获取完整的 git 历史记录
2. **设置 Java 21** - 配置构建环境
3. **缓存 Gradle** - 提高构建速度
4. **解析版本号** - 从 tag 中提取版本信息
5. **更新版本** - 修改 `gradle.properties` 中的版本号
6. **更新变更日志** - 将 Unreleased 部分标记为当前版本
7. **构建插件** - 执行 `./gradlew buildPlugin`
8. **生成发布说明** - 从 CHANGELOG 提取当前版本的变更
9. **提交变更** - 将版本和变更日志的修改提交到主分支
10. **创建 Release** - 在 GitHub 上创建正式发布
11. **上传产物** - 将插件 zip 文件作为附件上传

## 故障排除

### 常见问题

1. **构建失败**
   - 检查 Java 版本是否正确
   - 查看 Gradle 构建日志
   - 确认依赖项是否正确

2. **Tag 推送失败**
   - 确保 tag 格式正确（以 `v` 开头）
   - 检查是否有重复的 tag

3. **Release 创建失败**
   - 检查 GitHub Token 权限
   - 确认仓库设置允许创建 Release

### 查看构建日志

1. 访问 GitHub 仓库页面
2. 点击 "Actions" 标签
3. 选择对应的工作流运行记录
4. 查看详细的构建日志

## 环境变量配置

当前工作流使用以下环境变量：

- `GITHUB_TOKEN` - GitHub 自动提供，用于创建 Release
- 其他敏感信息（如插件签名证书）需要在仓库 Settings > Secrets 中配置

## 本地测试

在推送 tag 之前，可以本地测试构建：

```bash
cd dd_flutter_idea_plugin
./gradlew buildPlugin
```

构建成功后，插件文件会在 `build/distributions/` 目录中生成。

## 注意事项

- 确保 `CHANGELOG.md` 格式正确
- Tag 一旦推送就会触发发布，请谨慎操作
- 发布后的版本无法轻易撤回，请充分测试后再发布
- CI 工作流会自动提交版本变更，确保本地代码与远程同步