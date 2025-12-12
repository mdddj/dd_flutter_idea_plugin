# ADB Tool

这是一个通过 ADB 与 Android 设备进行交互的命令行工具。它使用 `forensic-adb` crate 进行设备通信，并使用 `clap` 进行参数解析。

## 功能

*   列出已连接的 Android 设备。
*   在指定设备上执行任意 ADB shell 命令。
*   在指定设备上安装 APK。

## 安装

1.  克隆仓库：
    ```bash
    git clone <repository_url>
    cd adb_tool
    ```

2.  构建项目：
    ```bash
    cargo build --release
    ```

## 使用方法

您可以使用 `cargo run` 命令运行该工具，或者将编译后的二进制文件复制到您的 PATH 环境变量中以直接运行。

### 列出设备

要列出所有已连接的 Android 设备：

```bash
cargo run -- list
```

### 执行 ADB 命令

要在特定设备上执行命令，请使用 `exec` 子命令。您需要提供设备序列号、命令以及命令的可选参数。

```bash
cargo run -- -s <device_serial> -c <command> [--arg <argument>]
```

**参数：**

*   `-s <device_serial>` 或 `--serial <device_serial>`：目标 Android 设备的序列号。（`exec` 命令必需）
*   `-c <command>` 或 `--command <command>`：要执行的 ADB 命令。（必需）
*   `--arg <argument>`：命令的可选参数。

**示例：**

*   列出设备上所有已安装的软件包：
    ```bash
    cargo run -- -s YOUR_DEVICE_SERIAL -c "pm list packages"
    ```

*   安装 APK 文件：
    ```bash
    cargo run -- -s YOUR_DEVICE_SERIAL -c install --arg /path/to/your/app.apk
    ```

*   重启设备：
    ```bash
    cargo run -- -s YOUR_DEVICE_SERIAL -c "reboot"
    ```

### 帮助

要查看帮助信息，请运行：

```bash
cargo run -- --help
```

或者

```bash
./your_binary_name --help
```

（将 `your_binary_name` 替换为编译后的可执行文件的实际名称）。

## 贡献

欢迎贡献！请随时提交 Pull Request。

## 许可证

本项目采用 MIT 许可证。