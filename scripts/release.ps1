# FlutterX Plugin Release Script for Windows PowerShell
# 用于简化本地发布流程的辅助脚本

param(
    [Parameter(Position=0)]
    [string]$Version
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

# 检查是否在项目根目录
function Test-ProjectRoot {
    if (-not (Test-Path "build.gradle.kts") -or -not (Test-Path "CHANGELOG.md")) {
        Write-Error "请在项目根目录下运行此脚本"
        exit 1
    }
}

# 检查git状态
function Test-GitStatus {
    $status = git status --porcelain
    if ($status) {
        Write-Warning "工作目录有未提交的更改"
        $response = Read-Host "是否继续？(y/N)"
        if ($response -ne "y" -and $response -ne "Y") {
            exit 1
        }
    }
}

# 验证版本号格式
function Test-VersionFormat {
    param([string]$Version)
    if ($Version -notmatch "^\d+\.\d+\.\d+$") {
        Write-Error "版本号格式不正确，应该是 x.y.z 格式（如 5.7.0）"
        exit 1
    }
}

# 检查tag是否已存在
function Test-TagExists {
    param([string]$Tag)
    try {
        git rev-parse $Tag 2>$null | Out-Null
        Write-Error "Tag $Tag 已存在"
        exit 1
    }
    catch {
        # Tag不存在，这是我们期望的
    }
}

# 更新CHANGELOG
function Update-Changelog {
    param([string]$Version)
    
    $date = Get-Date -Format "yyyy-MM-dd"
    Write-Info "更新 CHANGELOG.md..."
    
    # 备份原文件
    Copy-Item "CHANGELOG.md" "CHANGELOG.md.bak"
    
    try {
        # 读取文件内容
        $content = Get-Content "CHANGELOG.md" -Raw
        
        # 替换 "## Unreleased" 部分
        $newContent = $content -replace "(## Unreleased)", "`$1`r`n`r`n## [$Version] - $date"
        
        # 写入新内容
        Set-Content "CHANGELOG.md" $newContent -NoNewline
        
        Write-Success "CHANGELOG.md 已更新"
    }
    finally {
        # 清理备份文件
        Remove-Item "CHANGELOG.md.bak" -ErrorAction SilentlyContinue
    }
}

# 更新版本号
function Update-Version {
    param([string]$Version)
    
    Write-Info "更新 gradle.properties 中的版本号..."
    
    # 备份原文件
    Copy-Item "gradle.properties" "gradle.properties.bak"
    
    try {
        # 读取文件内容
        $content = Get-Content "gradle.properties"
        
        # 替换版本号
        $newContent = $content -replace "pluginVersion=.*", "pluginVersion=$Version."
        
        # 写入新内容
        Set-Content "gradle.properties" $newContent
        
        Write-Success "版本号已更新为 $Version"
    }
    finally {
        # 清理备份文件
        Remove-Item "gradle.properties.bak" -ErrorAction SilentlyContinue
    }
}

# 构建插件
function Build-Plugin {
    Write-Info "开始构建插件..."
    
    try {
        if ($IsWindows -or $env:OS -eq "Windows_NT") {
            & .\gradlew.bat buildPlugin --no-daemon --stacktrace
        } else {
            & ./gradlew buildPlugin --no-daemon --stacktrace
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "插件构建成功"
        } else {
            throw "构建失败"
        }
    }
    catch {
        Write-Error "插件构建失败: $_"
        exit 1
    }
}

# 提交更改
function Commit-Changes {
    param([string]$Version)
    
    Write-Info "提交版本更改..."
    
    git add CHANGELOG.md gradle.properties
    git commit -m "chore: release version $Version"
    
    Write-Success "版本更改已提交"
}

# 创建并推送tag
function New-TagAndPush {
    param([string]$Version)
    
    $tag = "v$Version"
    
    Write-Info "创建 tag: $tag"
    git tag $tag
    
    Write-Info "推送更改和tag到远程仓库..."
    git push origin main
    git push origin $tag
    
    Write-Success "Tag $tag 已创建并推送"
}

# 显示构建信息
function Show-BuildInfo {
    param([string]$Version)
    
    $pluginFiles = Get-ChildItem -Path "build\distributions" -Filter "*.zip" -ErrorAction SilentlyContinue
    
    Write-Host ""
    Write-Success "🎉 发布流程完成！"
    Write-Host ""
    Write-Host "📋 发布信息:"
    Write-Host "   版本: $Version"
    Write-Host "   Tag: v$Version"
    
    if ($pluginFiles) {
        $pluginFile = $pluginFiles[0]
        $fileSize = [math]::Round($pluginFile.Length / 1MB, 2)
        Write-Host "   插件文件: $($pluginFile.FullName)"
        Write-Host "   文件大小: $fileSize MB"
    }
    
    Write-Host ""
    
    # 获取GitHub仓库URL
    try {
        $remoteUrl = git config --get remote.origin.url
        if ($remoteUrl -match "github\.com[:/]([^/]+/[^/]+)") {
            $repoPath = $matches[1] -replace "\.git$", ""
            Write-Info "🔗 GitHub Actions 将自动创建 Release: https://github.com/$repoPath/actions"
        }
    }
    catch {
        Write-Info "🔗 请查看 GitHub Actions 页面了解发布状态"
    }
    
    Write-Host ""
}

# 主函数
function Main {
    Write-Host "🚀 FlutterX Plugin Release Script" -ForegroundColor Cyan
    Write-Host "=================================" -ForegroundColor Cyan
    Write-Host ""
    
    # 检查环境
    Test-ProjectRoot
    Test-GitStatus
    
    # 获取版本号
    if (-not $Version) {
        $Version = Read-Host "请输入新版本号（格式: x.y.z）"
    }
    
    # 验证版本号
    Test-VersionFormat $Version
    
    # 检查tag是否存在
    $tag = "v$Version"
    Test-TagExists $tag
    
    # 确认发布
    Write-Host ""
    Write-Warning "即将发布版本: $Version"
    Write-Warning "这将会:"
    Write-Host "  1. 更新 CHANGELOG.md"
    Write-Host "  2. 更新 gradle.properties 中的版本号"
    Write-Host "  3. 构建插件"
    Write-Host "  4. 提交更改"
    Write-Host "  5. 创建并推送 tag: $tag"
    Write-Host "  6. 触发 GitHub Actions 自动发布"
    Write-Host ""
    
    $response = Read-Host "确认继续？(y/N)"
    
    if ($response -ne "y" -and $response -ne "Y") {
        Write-Info "发布已取消"
        exit 0
    }
    
    # 执行发布流程
    try {
        Update-Changelog $Version
        Update-Version $Version
        Build-Plugin
        Commit-Changes $Version
        New-TagAndPush $Version
        Show-BuildInfo $Version
    }
    catch {
        Write-Error "发布过程中出现错误: $_"
        exit 1
    }
}

# 执行主函数
Main