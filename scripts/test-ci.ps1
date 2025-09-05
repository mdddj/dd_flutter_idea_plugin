# FlutterX Plugin CI Test Script using act (PowerShell version)
# 用于在本地测试 GitHub Actions 工作流

param(
    [Parameter(Position=0)]
    [string]$Action,
    
    [Parameter(Position=1)]
    [string]$Parameter
)

# 设置错误处理
$ErrorActionPreference = "Stop"

# 颜色定义
function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Write-Header {
    param([string]$Message)
    Write-Host $Message -ForegroundColor Cyan
}

# 检查依赖
function Test-Dependencies {
    Write-Info "检查依赖..."
    
    # 检查 act
    try {
        $actVersion = act --version 2>$null
        if (-not $actVersion) {
            throw "act not found"
        }
    }
    catch {
        Write-Error "act 未安装，请先安装 act: https://github.com/nektos/act"
        exit 1
    }
    
    # 检查 Docker
    try {
        $dockerVersion = docker --version 2>$null
        if (-not $dockerVersion) {
            throw "docker not found"
        }
    }
    catch {
        Write-Error "Docker 未安装，act 需要 Docker 运行"
        exit 1
    }
    
    # 检查 Docker 是否运行
    try {
        docker info 2>$null | Out-Null
    }
    catch {
        Write-Error "Docker 未运行，请启动 Docker"
        exit 1
    }
    
    Write-Success "依赖检查通过"
}

# 检查项目结构
function Test-Project {
    if (-not (Test-Path "build.gradle.kts") -or -not (Test-Path "CHANGELOG.md")) {
        Write-Error "请在项目根目录下运行此脚本"
        exit 1
    }
    
    if (-not (Test-Path ".github/workflows/test-release.yml")) {
        Write-Error "测试工作流文件不存在: .github/workflows/test-release.yml"
        exit 1
    }
    
    Write-Success "项目结构检查通过"
}

# 显示可用的工作流
function Show-Workflows {
    Write-Header "📋 可用的工作流:"
    Write-Host "1. test-release.yml    - 测试发布工作流 (推荐)"
    Write-Host "2. release.yml         - 正式发布工作流"
    Write-Host "3. build-test.yml      - 构建测试工作流"
    Write-Host ""
}

# 测试发布工作流
function Test-ReleaseWorkflow {
    param([string]$Version = "1.0.0")
    
    Write-Header "🧪 测试发布工作流"
    Write-Info "测试版本: $Version"
    
    $testTag = "test-v$Version"
    
    Write-Info "运行 act 测试..."
    
    # 创建临时目录
    $tempDir = "$env:TEMP\act-artifacts"
    if (-not (Test-Path $tempDir)) {
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    }
    
    try {
        # 使用 workflow_dispatch 事件
        $env:GITHUB_TOKEN = "fake_token_for_testing"
        
        act workflow_dispatch `
            --workflows .github/workflows/test-release.yml `
            --input version="$Version" `
            --verbose `
            --artifact-server-path $tempDir `
            --platform ubuntu-latest=catthehacker/ubuntu:act-latest
            
        Write-Success "测试完成"
    }
    catch {
        Write-Error "测试失败: $_"
        exit 1
    }
}

# 测试构建工作流
function Test-BuildWorkflow {
    Write-Header "🔨 测试构建工作流"
    
    Write-Info "运行 act 测试..."
    
    # 创建临时目录
    $tempDir = "$env:TEMP\act-artifacts"
    if (-not (Test-Path $tempDir)) {
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    }
    
    try {
        # 模拟 push 事件
        act push `
            --workflows .github/workflows/build-test.yml `
            --verbose `
            --artifact-server-path $tempDir `
            --platform ubuntu-latest=catthehacker/ubuntu:act-latest
            
        Write-Success "测试完成"
    }
    catch {
        Write-Error "测试失败: $_"
        exit 1
    }
}

# 测试特定工作流
function Test-SpecificWorkflow {
    param(
        [string]$WorkflowFile,
        [string]$EventType = "push"
    )
    
    Write-Header "🎯 测试特定工作流: $WorkflowFile"
    
    if (-not (Test-Path ".github/workflows/$WorkflowFile")) {
        Write-Error "工作流文件不存在: .github/workflows/$WorkflowFile"
        exit 1
    }
    
    Write-Info "运行 act 测试..."
    
    # 创建临时目录
    $tempDir = "$env:TEMP\act-artifacts"
    if (-not (Test-Path $tempDir)) {
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    }
    
    try {
        $env:GITHUB_TOKEN = "fake_token_for_testing"
        
        act $EventType `
            --workflows ".github/workflows/$WorkflowFile" `
            --verbose `
            --artifact-server-path $tempDir `
            --platform ubuntu-latest=catthehacker/ubuntu:act-latest
            
        Write-Success "测试完成"
    }
    catch {
        Write-Error "测试失败: $_"
        exit 1
    }
}

# 列出所有工作流
function Get-Workflows {
    Write-Header "📋 项目中的所有工作流:"
    
    if (Test-Path ".github/workflows") {
        $workflowFiles = Get-ChildItem -Path ".github/workflows" -Filter "*.yml", "*.yaml"
        
        foreach ($file in $workflowFiles) {
            $name = $file.Name
            
            # 尝试读取工作流名称
            try {
                $content = Get-Content $file.FullName -Raw
                if ($content -match "name:\s*(.+)") {
                    $workflowName = $matches[1].Trim().Trim('"', "'")
                } else {
                    $workflowName = "未命名"
                }
            }
            catch {
                $workflowName = "未知"
            }
            
            Write-Host "  📄 $name - $workflowName"
        }
    }
    else {
        Write-Warning "未找到 .github/workflows 目录"
    }
    Write-Host ""
}

# 显示 act 信息
function Show-ActInfo {
    Write-Header "🔧 act 环境信息:"
    
    try {
        $actVersion = act --version 2>$null
        Write-Host "  版本: $actVersion"
    }
    catch {
        Write-Host "  版本: 未知"
    }
    
    $configExists = Test-Path ".actrc"
    Write-Host "  配置文件: $(if ($configExists) { '存在' } else { '不存在' })"
    
    try {
        docker info 2>$null | Out-Null
        Write-Host "  Docker 状态: 运行中"
    }
    catch {
        Write-Host "  Docker 状态: 未运行"
    }
    
    Write-Host ""
}

# 清理函数
function Clear-TestData {
    Write-Info "清理临时文件..."
    
    # 清理可能创建的测试 tag
    try {
        $testTags = git tag -l | Where-Object { $_ -match "^test-v" }
        foreach ($tag in $testTags) {
            git tag -d $tag 2>$null | Out-Null
        }
    }
    catch {
        # 忽略错误
    }
    
    # 清理临时目录
    $tempDir = "$env:TEMP\act-artifacts"
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
    
    Write-Success "清理完成"
}

# 显示帮助
function Show-Help {
    Write-Header "🚀 FlutterX Plugin CI 测试脚本"
    Write-Host ""
    Write-Host "用法: .\test-ci.ps1 [选项] [参数]"
    Write-Host ""
    Write-Host "选项:"
    Write-Host "  release [版本号]        测试发布工作流 (默认版本: 1.0.0)"
    Write-Host "  build                   测试构建工作流"
    Write-Host "  workflow <文件名>       测试特定工作流文件"
    Write-Host "  list                    列出所有工作流"
    Write-Host "  info                    显示环境信息"
    Write-Host "  cleanup                 清理测试数据"
    Write-Host "  help                    显示此帮助信息"
    Write-Host ""
    Write-Host "示例:"
    Write-Host "  .\test-ci.ps1 release 5.8.0     # 测试发布工作流，版本 5.8.0"
    Write-Host "  .\test-ci.ps1 build              # 测试构建工作流"
    Write-Host "  .\test-ci.ps1 workflow release.yml  # 测试 release.yml 工作流"
    Write-Host "  .\test-ci.ps1 list               # 列出所有工作流"
    Write-Host "  .\test-ci.ps1 info               # 显示环境信息"
    Write-Host ""
    Write-Host "注意:"
    Write-Host "  - 确保 Docker 正在运行"
    Write-Host "  - 首次运行可能需要下载 Docker 镜像"
    Write-Host "  - 测试不会影响真实的 Git 仓库"
    Write-Host ""
}

# 主函数
function Main {
    # 检查参数
    if (-not $Action) {
        Show-Help
        return
    }
    
    # 检查依赖和项目
    Test-Dependencies
    Test-Project
    
    # 处理参数
    switch ($Action.ToLower()) {
        "release" {
            $version = if ($Parameter) { $Parameter } else { "1.0.0" }
            Test-ReleaseWorkflow $version
        }
        "build" {
            Test-BuildWorkflow
        }
        "workflow" {
            if (-not $Parameter) {
                Write-Error "请指定工作流文件名"
                exit 1
            }
            Test-SpecificWorkflow $Parameter
        }
        "list" {
            Get-Workflows
        }
        "info" {
            Show-ActInfo
            Show-Workflows
        }
        "cleanup" {
            Clear-TestData
        }
        "help" {
            Show-Help
        }
        default {
            Write-Error "未知选项: $Action"
            Write-Host ""
            Show-Help
            exit 1
        }
    }
}

# 设置清理陷阱
try {
    Main
}
finally {
    # 最终清理
    Clear-TestData
}