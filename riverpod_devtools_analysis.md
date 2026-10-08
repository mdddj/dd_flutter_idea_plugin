# Riverpod DevTools 实现原理深度分析

## 一、整体架构

Riverpod DevTools 由三个核心包组成：

| 包名 | 路径 | 职责 |
|------|------|------|
| **协议层** (内嵌于 `riverpod`) | `packages/riverpod/lib/src/core/devtool.dart` | 嵌入用户 App 的观察者，记录 provider 生命周期事件 |
| **UI 界面** (`riverpod_devtool`) | `packages/riverpod_devtool/` | Flutter Web 应用，作为 DevTools Extension 运行 |
| **代码生成器** (`riverpod_devtool_generator`) | `packages/riverpod_devtool_generator/` | 自动生成序列化/反序列化代码，保持协议与框架同步 |

三者关系：

```
┌──────────────────────────────────────────────────────────────────┐
│                      用户 App (被调试)                             │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  DevtoolObserver  │  RiverpodDevtool (单例)  │  @devtool 注解 │  │
│  │  (ProviderObserver)│  (cache / frames 管理)   │  (标记协议类)  │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
         │                                          ▲
         │  developer.postEvent()                   │ EvalOnDartLibrary
         │  ("riverpod:new_event")                  │
         ▼                                          │
┌──────────────────────────────────────────────────────────────────┐
│                       Dart VM Service                            │
│  onExtensionEvent 监听  │  EvalOnDartLibrary (在 Isolate 执行代码)  │
└──────────────────────────────────────────────────────────────────┘
         │                                          │
         ▼                                          │
┌──────────────────────────────────────────────────────────────────┐
│                  DevTools UI (Flutter Web)                        │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  vm_service.dart  │  frames.dart  │  inspector.dart         │ │
│  │  (VM 连接管理)      │  (帧数据管理)   │  (状态树检查器)          │ │
│  │                    │               │                        │ │
│  │  terminal.dart     │  provider_list.dart │  ide.dart        │ │
│  │  (交互终端)         │  (Provider 列表)     │  (IDE 跳转)       │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                vm_service.g.dart (代码生成器产出)              │ │
│  │  镜像类: Frame / ProviderMeta / Event 层级 ... 的反序列化      │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

## 二、通信机制：基于 Dart VM Service

整个 DevTools 通信不走 HTTP/WebSocket/TCP，而是完全依赖 **Dart VM Service Protocol**。

### 2.1 两条通信路径

#### 路径 1：事件通知（Push 模型）—— App → DevTools

```
用户 App                          VM Service                      DevTools UI
  │                                   │                                │
  │ DevtoolObserver 记录生命周期事件    │                                │
  │ → Event 对象放入 Frame             │                                │
  │ → Frame 结束时调用                │                                │
  │   developer.postEvent()           │                                │
  │   事件名: "riverpod:new_event"     │                                │
  │   负载: {"offset": frame_index}    │                                │
  │ ═════════════════════════════════>│                                │
  │                                   │  onExtensionEvent 回调         │
  │                                   │  ═════════════════════════════>│
  │                                   │                                │ framesProvider
  │                                   │                                │ 收到通知，触发刷新
```

核心代码 (`packages/riverpod/lib/src/core/devtool.dart`)：

```dart
class DevtoolObserver extends ProviderObserver {
  @override
  void didAddProvider(provider, container, element) {
    _addEvent(ProviderElementAddEvent(...));
  }

  void _addEvent(Event event) {
    final frames = RiverpodDevtool.instance.frames;
    // 新 build frame → 创建新 Frame
    if (frames.isEmpty || _frame!.sealed) {
      _frame = Frame();
      frames.add(_frame);
      // 在本 frame 结束后触发通知
      container.scheduler.debugScheduleFrame(() {
        _frame!.seal();
        _notify(frames.length - 1);
      });
    }
    _frame!.events.add(event);
  }

  void _notify(int offset) {
    developer.postEvent('riverpod', {
      'new_event': {'offset': offset},
    });
  }
}
```

#### 路径 2：数据拉取（Pull 模型）—— DevTools → App

DevTools 通过 VM Service 的 `EvalOnDartLibrary` 在用户 Isolate 中执行 Dart 代码：

```dart
// DevTools 侧: vm_service.dart
final result = await service.callServiceExtension(
  'evaluateInFrame',
  isolateId: isolateId,
  args: {
    'expression': 'RiverpodDevtool.instance.frames[...].toBytes()',
  },
);
```

框架侧通过代码生成器产出的 `toBytes()` 方法将数据序列化为 `Map<String, Object?>`：

```dart
// 框架侧: framework.g.dart (代码生成器产出)
extension FrameToBytes on Frame {
  Map<String, Object?> toBytes() {
    return {
      'events': events.map((e) => e.toBytes()).toList(),
      'sealed': sealed,
    };
  }
}
```

### 2.2 为什么用 VM Service 而不是自定义协议？

| 方案 | 优点 | 缺点 |
|------|------|------|
| **VM Service** (当前) | 无需端口占用、无需防火墙配置、天然隔离安全、复用 IDE 已有连接 | 依赖 VM Service 可用性、性能受限于 eval 开销 |
| WebSocket/TCP | 性能高、双向实时 | 需要端口、安全配置、多连接管理 |

---

## 三、协议层详解：DevtoolObserver & Event 体系

### 3.1 事件类型层次

```
Event (基类)
├── ProviderContainerAddEvent      # ProviderContainer 创建
├── ProviderContainerDisposeEvent  # ProviderContainer 销毁
├── ProviderElementAddEvent        # Provider 元素创建
├── ProviderElementUpdateEvent     # Provider 状态更新
├── ProviderElementDisposeEvent    # Provider 元素销毁
└── ProviderDependencyChangeEvent  # Provider 依赖关系变更
```

每个事件都携带：
- `containerId` / `elementId` / `originId`（唯一标识）
- 关联的 `ProviderMeta`（包含名称、类型、创建堆栈）
- 状态引用 `ProviderStateRef`（缓存 UUID，用于后续读取）

### 3.2 Frame 批处理机制

```
┌──────── Frame 0 ────────┐  ┌──────── Frame 1 ────────┐
│ Event: add counter      │  │ Event: update counter    │
│ Event: add computed     │  │ Event: update computed   │
│ (frame sealed → notify) │  │ (frame sealed → notify)  │
└─────────────────────────┘  └─────────────────────────┘
```

- 每个 build frame 内的所有事件归入同一个 `Frame`
- Frame 在 `debugScheduleFrame` 回调中 sealing（关闭）
- sealing 后发送一次通知（避免频繁通知）
- `FoldedFrame`（DevTools 侧）关联当前帧与前一帧，用于差分计算

### 3.3 FoldedFrame 差分模型

```dart
// DevTools 侧: frames.dart
class FoldedFrame {
  final Frame frame;
  final FoldedFrame? previous;

  // 通过对比前后帧，自动分类 provider
  // - Modified: 当前帧有 event，前一帧无 → 新增
  // - Modified: 两帧都有 event → 修改
  // - Disposed: 前一帧有，当前帧无 → 已销毁
}
```

---

## 四、数据序列化：代码生成驱动

### 4.1 @devtool 注解

框架中所有需要传输给 DevTools 的类都标注 `@devtool`：

```dart
// packages/riverpod/lib/src/core/devtool.dart
@devtool
class Frame {
  final List<Event> events;
  bool sealed;
}

@devtool
class ProviderMeta {
  final ProviderId id;
  final OriginId originId;
  final String name;
  final String? creationStackTrace;
}
```

### 4.2 代码生成器工作流程

`riverpod_devtool_generator` 使用 `source_gen` 和 `analyzer` 扫描源码：

```
framework.dart
     │
     │ source_gen 分析 @devtool 注解的类
     ▼
┌─────────────────────────────────────────┐
│  Builder (lib/builder.dart)              │
│                                          │
│  1. 解析类的字段类型                       │
│  2. 递归解析嵌套 @devtool 类型              │
│  3. 生成 toBytes() 序列化代码               │
│  4. 生成 from() 反序列化代码 (镜像类)        │
│                                          │
│  输出:                                    │
│  - framework.g.dart (框架侧)              │
│  - vm_service.g.dart (DevTools 侧)        │
└─────────────────────────────────────────┘
```

### 4.3 序列化策略

```dart
// 简单值直接传递
'id': id,           // String → String
'sealed': sealed,   // bool → bool

// 嵌套 devtool 对象递归 toBytes()
'events': events.map((e) => e.toBytes()).toList(),

// 复杂对象通过 RiverpodDevtool.cache() 存储，传递 UUID
'state': RiverpodDevtool.instance.cache(state),  // → "uuid-xxx"

// 长字符串分块 (128 字符)
'creationStackTrace': [
  'chunk1...',
  'chunk2...',
],
```

### 4.4 镜像类反序列化

DevTools 侧生成的镜像类提供 `from()` 工厂方法：

```dart
// DevTools 侧: vm_service.g.dart
class ProviderMeta {
  final String id;
  final String originId;
  final String name;

  factory ProviderMeta.from(Map<String, VmInstanceRef> map) {
    return ProviderMeta(
      id: map['id']!.stringValue!,
      originId: map['originId']!.stringValue!,
      name: map['name']!.stringValue!,
    );
  }
}
```

**这就是为什么 DevTools 协议能始终保持同步** —— 序列化和反序列化代码都从同一个 `@devtool` 注解的类自动生成。

---

## 五、对象缓存 & Sentinel 处理

### 5.1 缓存架构

VM Service 的 `InstanceRef` 有生命周期限制（sentinel 过期），因此设计了双层缓存：

```
用户 App 侧 (RiverpodDevtool)
  ┌──────────────────────────────────────┐
  │  _cache: Map<String, Object>          │
  │  cache(obj) → 返回 UUID               │
  │  用于: ProviderState, 大型对象        │
  └──────────────────────────────────────┘
       │
       │ 传递 UUID + 分块数据
       ▼
DevTools 侧 (CachedObject)
  ┌──────────────────────────────────────┐
  │  RootCachedObject (uuid)              │  ← 通过 cache() 注册的根对象
  │  ├── DerivedCachedObject (getter)     │  ← 对象 getter 懒求值
  │  ├── DerivedCachedObject (field)      │  ← 对象字段直接访问
  │  ├── DerivedCachedObject (list item)  │  ← 集合元素
  │  └── DerivedCachedObject (map entry)  │  ← Map 键值对
  └──────────────────────────────────────┘
```

### 5.2 懒加载 & Sentinel 重试

```dart
// DevTools 侧: vm_service/cache.dart
class CachedObject {
  Future<VmInstance> read() async {
    try {
      return await eval.safeGetInstance(uuid);
    } on SentinelException {
      // sentinel 过期 → 重新从框架侧拉取
      return await eval.reloadFromCache(uuid);
    } on RpcError {
      // RPC 错误 → 等待后重试
      await Future.delayed(Duration(seconds: 1));
      return read();
    }
  }
}
```

所有的 child lazy resolution 都通过 `Riverpod` provider 实现，确保只有展开的节点才会真正加载数据。

---

## 六、DevTools UI 架构

### 6.1 整体布局

```
┌─────────────────────────────────────────────────────────────┐
│  FrameStepper (← Frame →)           Search Bar              │
├─────────────────────────┬───────────────────────────────────┤
│   Provider List         │  State Inspector  │   Terminal     │
│                         │                   │               │
│  ▼ Modified (2)         │  ▼ counter        │  > $state     │
│    └ counter            │    state: 3        │    3          │
│    └ computed           │    ...             │    >          │
│                         │                   │               │
│  ▼ Disposed (0)         │  ▼ computed       │  History:     │
│                         │    state: 6        │   ↑ 上一条    │
│  ▼ Unchanged (15)       │    notifier:       │   ↓ 下一条    │
│    └ xxxProvider        │      ...           │               │
│    └ yyyProvider        │                   │               │
├─────────────────────────┴───────────────────┴───────────────┤
│  Status Bar: connected / hot restart detected                │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 核心组件详解

#### Provider 列表 (`provider_list.dart`)

```
功能:
- 按状态分组: Modified / Disposed / Unchanged
- 模糊搜索 (FuzzyMatch 算法 + 高亮)
- Family provider 展开/折叠
- 点击选中 → 联动右侧状态检查器
- Invalidate 按钮 (每个 provider)
- 跳转 IDE 按钮
```

#### 状态树检查器 (`inspector.dart`)

```
功能:
- 递归展开所有 Dart 类型的值
- 支持类型:
  - 基本类型: null, bool, int, double, String
  - 集合类型: List, Map, Set
  - 特殊类型: Record, AsyncValue (data/loading/error)
  - 自定义对象: 通过 VM Service 反射获取 getter
- 懒加载: 展开时才求值子节点
- TreeList 数据结构: 支持展开/折叠状态管理
```

#### 交互终端 (`terminal.dart`)

```
功能:
- 在选中 provider 的上下文中执行任意 Dart 代码
- 内置变量: $state, $notifier, $previous
- 命令历史 (↑↓ 导航)
- 侧边栏历史列表
- Mutation 支持状态修改

示例:
> $state
3
> $state * 2
6
> $notifier.runtimeType
Counter
```

#### Frame 步进器 (`frames.dart`)

```
功能:
- ← → 切换不同 frame
- 灰色数字 = 有变更的 frame
- 显示当前 frame 时间戳 / 序号
```

### 6.3 UI 自身也用 Riverpod

DevTools UI 内部全面使用 Riverpod 管理状态：

```dart
// 核心 provider 依赖图:
serviceManagerProvider     // VM Service 连接管理
  ├── vmServiceProvider    // VmService 实例
  ├── hotRestartEventProvider // Hot restart 检测
  │
framesProvider             // 所有 frame 数据 (监听 extension event)
  ├── filteredFramesProvider  // 过滤后的 frame
  │
selectedFrameIdProvider    // 当前选中的 frame
  ├── selectedFrameProvider   // 当前 frame 对象
  │   └── computeElementsForFrame()  // 重建 element 状态
  │
selectedProviderIdProvider // 当前选中的 provider
  ├── selectedProviderProvider // 当前 provider 的 ElementMeta
  │
filteredProvidersProvider  // 搜索过滤后的 provider 列表
```

---

## 七、关键设计模式总结

### 7.1 代码生成保证协议同步

```
framework.dart                    vm_service.g.dart
@devtool class Frame    ─┐       ┌─ class Frame
                         │ build │  factory Frame.from(...)
@devtool class Event     ├───────┤  class Event
                         │       │  factory Event.from(...)
@devtool class Meta      ─┘       └─ class Meta
                         
         框架侧重: toBytes()         DevTools 侧重: from()
```

**最大的价值：** 当框架新增/修改字段时，只需改 `@devtool` 注解的类，两边代码自动生成，零手动同步成本。

### 7.2 VM Service 作为万能通道

DevTools 不需要任何自定义网络层，所有通信复用 Dart VM Service：
- 数据传输：`EvalOnDartLibrary` 调用 `toBytes()`
- 状态修改：`EvalOnDartLibrary` 调用 `container.invalidate()`
- IDE 跳转：`EvalOnDartLibrary` 调用 `openInIDE()`
- 任意代码执行：终端通过 eval 实现

### 7.3 分层缓存 + 懒加载

```
RootCachedObject  (状态根对象, 通过 UUID 获取)
  └→ DerivedCachedObject (展开时才懒加载)
      └→ DerivedCachedObject
```

- 不是一次性拉取所有状态，而是按需加载
- sentinel 过期自动重试，对用户透明

### 7.4 FoldedFrame 差分

通过关联前后帧，自动分类 provider 状态变更：
- 新创建 → Modified
- 值变更 → Modified
- 销毁 → Disposed
- 无变更 → Unchanged

### 7.5 Hot Restart 容错

多个 provider 监听 VM isolate ID 变化：
- 检测到 isolate ID 变化 → 重置所有 UI 状态
- 重新建立连接 → 从头拉取 frame 数据

---

## 八、数据流完整时序图

```
时间 ──────────────────────────────────────────────────────────────>

用户 App                    VM Service                   DevTools UI
  │                            │                              │
  │① App 启动, 创建              │                              │
  │   ProviderContainer          │                              │
  │   → DevtoolObserver 注册     │                              │
  │                              │                              │
  │② 用户操作触发 provider 更新    │                              │
  │   → DevtoolObserver         │                              │
  │     .didUpdateProvider()     │                              │
  │   → 创建 Event 加入 Frame    │                              │
  │                              │                              │
  │                              │                              │
  │③ Frame sealing              │                              │
  │   → developer.postEvent()   │                              │
  │   ●════════════════════════>│                              │
  │                              │ onExtensionEvent             │
  │                              │ ●═══════════════════════════>│
  │                              │                              │④ framesProvider
  │                              │                              │  收到通知
  │                              │                              │
  │                              │  EvalOnDartLibrary            │⑤ 执行 eval
  │                              │<════════════════════════════● │
  │  ⑥ 执行 RiverpodDevtool      │                              │
  │    .instance.frames[...]     │                              │
  │    .toBytes()               │                              │
  │  ●════════════════════════>│                              │
  │                              │  返回 Map<String, InstanceRef>│
  │                              │ ●══════════════════════════>│
  │                              │                              │⑦ vm_service.g.dart
  │                              │                              │  from() 反序列化
  │                              │                              │
  │                              │  EvalOnDartLibrary           │⑧ 用户点击展开状态
  │                              │<════════════════════════════● │
  │  ⑨ CachedObject.read()      │                              │
  │    → 从 cache 获取实际对象    │                              │
  │  ●════════════════════════>│                              │
  │                              │  返回 VmInstance              │
  │                              │ ●══════════════════════════>│
  │                              │                              │⑩ ResolvedVariable
  │                              │                              │  .fromInstance()
  │                              │                              │  渲染状态树
  │                              │                              │
  │                              │  EvalOnDartLibrary           │⑪ 用户在终端输入
  │                              │<════════════════════════════● │  $state * 2
  │  ⑫ eval("$state * 2")       │                              │
  │  ●════════════════════════>│                              │
  │                              │  返回结果: 6                  │
  │                              │ ●══════════════════════════>│
```

---

## 九、对 IntelliJ 插件的借鉴意义

如果你正在开发一个类似的 IntelliJ 插件，以下模式值得参考：

### 9.1 进程间通信方案

| 方案 | 适用场景 |
|------|---------|
| **DAP (Debug Adapter Protocol)** | 如果你的工具需要调试能力，复用 IDE 已有的 DAP 连接（类似于 Riverpod 复用 VM Service） |
| **VM Service WebSocket** | 直接连接 Dart VM Service 获取 isolate 信息、执行 eval |
| **自定义 WebSocket** | 适用于非调试场景，需要 App 侧主动连接到插件 |

Riverpod DevTools 最巧妙的地方：**复用 IDE 已有的 debug 通道，零额外端口/协议成本**。

### 9.2 协议设计

- 用注解标记协议类 → 代码生成 → 双端自动同步
- 复杂对象用 UUID 引用 + 懒加载，而非一次性传输
- 事件通知用 push（低延迟），数据拉取用 pull（按需）

### 9.3 状态管理

DevTools UI 自身使用 Riverpod（即被调试框架和调试工具使用同一套状态管理），这对理解框架内部行为极有帮助。

### 9.4 容错设计

- Sentinel 过期 → 自动重试
- Hot restart → 自动重置并重连
- Eval 错误 → 优雅降级，非致命

---

## 十、扩展加载机制：Enable 按钮 & devtools_options.yaml

### 10.1 Enable 按钮在哪儿？

Riverpod DevTools 页面上的 "Enable" 按钮**不属于 Riverpod 代码**，而是 **Dart DevTools 扩展框架**提供的。它在 DevTools 主界面的 Extensions 列表中出现：

```
┌────────────────────────────────────────────┐
│  DevTools Extensions                       │
│  ┌──────────────────────────────────────┐  │
│  │  Riverpod                    [Enable] │  │  ← 框架层按钮
│  └──────────────────────────────────────┘  │
│  ┌──────────────────────────────────────┐  │
│  │  Provider                      [X]   │  │  ← 已启用，显示关闭按钮
│  └──────────────────────────────────────┘  │
└────────────────────────────────────────────┘
```

### 10.2 Enable 按钮做了什么？

```
用户点击 [Enable]
     │
     ▼
① 读取项目的 devtools_options.yaml
     │
     ▼
② 修改或创建配置项:
     extensions:
       - riverpod: true     ← 从 false 变为 true
     │
     ▼
③ DevTools 框架加载扩展的 Web 产物
   加载路径: packages/riverpod/extension/devtools/
   包含文件:
     - config.yaml          ← 扩展元信息
     - main.dart.js          ← riverpod_devtool 编译后的 JS
     - index.html
     - 其他静态资源
     │
     ▼
④ 加载 Web 页面 → riverpod_devtool 启动
     │
     ▼
⑤ ServiceManagerNotifier 轮询 VM Service
   连接成功后 → 开始监听 "riverpod:new_event" 扩展事件
```

### 10.3 关键配置文件

#### `devtools_options.yaml`（用户项目根目录）

```yaml
# 此文件存储 Dart & Flutter DevTools 的设置
# 文档: https://docs.flutter.dev/tools/devtools/extensions#configure-extension-enablement-states
extensions:
  - riverpod: true    # true = 启用 / false = 禁用
```

这是 DevTools 框架统一管理的扩展开关文件。Riverpod 包安装后，`pub get` 或 `flutter pub get` 会自动更新此文件。

#### `packages/riverpod/extension/devtools/config.yaml`（扩展元信息）

```yaml
name: riverpod
issueTracker: https://github.com/rrousselGit/riverpod/issues
version: 1.0.0
materialIconCodePoint: '0xf39f'
requiresConnection: true    # 需要 VM Service 连接才能工作
```

`requiresConnection: true` 意味着只有连接到运行中的 App 时，该扩展才会激活。

### 10.4 扩展的编译与部署

Riverpod DevTools 本身是一个 **Flutter Web 应用**（`riverpod_devtool` 包），最终编译为 JS 部署到 `packages/riverpod/extension/devtools/`：

```
开发流程:
  1. 修改 riverpod_devtool 代码
  2. 运行 flutter build web
  3. 将产物复制到 packages/riverpod/extension/devtools/
  4. 或者运行: devtools_extensions build_and_copy

发布流程 (scripts/publish.sh):
  1. 构建 Web 产物
  2. 复制到 packages/riverpod/extension/devtools/
  3. 验证 config.yaml 完整性
```

### 10.5 重要设计洞察

**DevtoolObserver 始终在运行：**

即使 DevTools 扩展未启用（`riverpod: false`），`DevtoolObserver` 在 debug 模式下**始终工作**：

```dart
// packages/riverpod/lib/src/core/provider_container.dart:904
if (kDebugMode && parent == null) const DevtoolObserver(),
```

这意味着：
- **事件一直在记录**，只是没人读取
- 点 Enable 后**无需重连或重启 App**，DevTools 直接拉取已累积的 Frame 数据
- `debugTrackProviderCreation` 若未开启，历史 Frame 中缺少创建堆栈（无法跳转 IDE），需设 `true` 后 hot restart

### 10.6 时序：从 Disabled 到 Connected

```
用户 App                   VM Service          DevTools Framework        Riverpod DevTools
   │                           │                       │                       │
   │① debug 模式启动             │                       │                       │
   │  DevtoolObserver 自动运行   │                       │                       │
   │  记录 Frame 0,1,2...       │                       │                       │
   │                           │                       │                       │
   │                           │                       │② 用户点 [Enable]       │
   │                           │                       │  加载 Web 页面 ───────>│
   │                           │                       │                       │
   │                           │<──────────────────────│③ ServiceManagerNotifier│
   │                           │  轮询 VM Service       │  .build() 开始轮询     │
   │                           │──────────────────────>│                       │
   │                           │                       │④ 连接成功             │
   │                           │  onExtensionEvent     │  vmServiceProvider    │
   │                           │<──────────────────────│  监听 "riverpod:"     │
   │                           │                       │                       │
   │⑤ DevTools 主动拉取:        │                       │                       │
   │  RiverpodDevtool          │                       │                       │
   │  .instance.frames.toBytes()│                      │                       │
   │ ═════════════════════════>│                       │                       │
   │                           │══════════════════════>│══════════════════════>│
   │                           │                       │                       │⑥ 渲染 UI
   │                           │                       │                       │
   │⑦ 之后的新 Frame            │                       │                       │
   │  .postEvent() 推送通知     │                       │                       │
   │ ═════════════════════════>│══════════════════════>│══════════════════════>│
```

---

## 十一、关键文件索引

### 框架侧 (packages/riverpod/)
| 文件 | 内容 |
|------|------|
| `lib/src/core/devtool.dart` | DevtoolObserver、RiverpodDevtool、Frame、Event、ProviderMeta |
| `lib/src/framework.g.dart` | 代码生成器产出：所有 `@devtool` 类的 `toBytes()` |

### DevTools UI (packages/riverpod_devtool/)
| 文件 | 内容 |
|------|------|
| `lib/main.dart` | 入口，ProviderScope + DevToolsExtension |
| `lib/src/frame_view.dart` | 主布局（Provider 列表 + 状态检查器 + 终端） |
| `lib/src/frames.dart` | Frame 管理、FoldedFrame 差分、FrameStepper UI |
| `lib/src/elements.dart` | ElementMeta 计算、事件回放构建状态 |
| `lib/src/provider_list.dart` | Provider 列表 UI（分组 + 搜索） |
| `lib/src/providers/providers.dart` | Provider 过滤/选择逻辑 |
| `lib/src/state_inspector/inspector.dart` | 状态树检查器（递归展开所有类型） |
| `lib/src/terminal.dart` | 交互终端 |
| `lib/src/ide.dart` | IDE 跳转 |
| `lib/src/vm_service.dart` | VM Service 连接管理 |
| `lib/src/vm_service/eval.dart` | EvalFactory / Eval 代码执行引擎 |
| `lib/src/vm_service/cache.dart` | CachedObject 缓存层 |
| `lib/src/vm_service/instance.dart` | ResolvedVariable 类型系统 |
| `lib/src/vm_service.g.dart` | 代码生成器产出：所有镜像类的 `from()` |

### 代码生成器 (packages/riverpod_devtool_generator/)
| 文件 | 内容 |
|------|------|
| `lib/builder.dart` | source_gen Builder：扫描 @devtool → 生成代码 |
| `build.yaml` | Builder 配置 |
