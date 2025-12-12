# Flutter Inspector 选择监听功能

## 功能概述

当用户在Flutter应用中开启Inspector Overlay并选择某个Widget时，IDE会自动检测到这个选择，并在编辑器中打开对应的源码文件。

## 工作原理

1. **启用Inspector Overlay** - 点击工具栏中的眼睛图标
2. **在Flutter应用中选择Widget** - 点击应用中的任意UI元素
3. **自动打开源码** - IDE自动打开对应的Dart源码文件并跳转到具体位置

## 功能特点

### 🎯 自动检测选择
- 每500ms检查一次用户选择的Widget
- 实时监听选择变化
- 避免重复处理相同选择

### 📁 智能文件打开
- 自动解析Widget的创建位置
- 在编辑器中打开对应的Dart文件
- 精确跳转到具体的行和列

### 🔧 可配置选项
- **Auto Open Source** 按钮：控制是否自动打开源码文件
- **Check Selection** 按钮：手动检查当前选中的Widget
- **Sync State** 按钮：同步Inspector状态

### 📊 状态栏提示
- 显示当前选中的Widget类型和描述
- 显示已打开的源码文件信息
- 3秒后自动清除提示信息

## 使用步骤

1. **启动Flutter应用**
2. **打开Widget Tree工具窗口**
3. **点击Inspector Overlay按钮**（眼睛图标）
4. **在Flutter应用中点击任意UI元素**
5. **IDE自动打开对应的源码文件**

## 工具栏按钮说明

| 按钮 | 图标 | 功能 |
|------|------|------|
| Refresh | 🔄 | 刷新Widget Tree |
| Open JSON | 📄 | 在编辑器中查看JSON数据 |
| Toggle Text Preview | 🌳 | 切换Text内容预览 |
| Sync State | 🔄 | 同步Inspector状态 |
| Check Selection | 🔍 | 检查当前选中的Widget |
| Auto Open Source | 📝 | 自动打开源码文件开关 |
| Inspector Overlay | 👁️ | Inspector调试图层开关 |

## 技术实现

### 核心组件

1. **InspectorSelectionManager** - 选择监听管理器
2. **InspectorStateManager** - 状态管理器
3. **InspectorSelectionStatusBar** - 状态栏显示

### VM Service API

- `ext.flutter.inspector.getSelectedRenderObject` - 获取选中的Widget
- `ext.flutter.inspector.setSelectionInspector` - 设置选择监听器
- `ext.flutter.inspector.show` - 控制Inspector Overlay

## 故障排除

### 选择不响应
1. 确认Inspector Overlay已启用
2. 检查Flutter应用是否在debug模式运行
3. 尝试点击"Sync State"按钮同步状态

### 源码文件打开失败
1. 确认源码文件路径正确
2. 检查文件是否存在于项目中
3. 验证创建位置信息是否完整

### 状态不同步
1. 点击"Check Selection"手动检查
2. 重新启动Flutter应用
3. 重新打开Widget Tree工具窗口