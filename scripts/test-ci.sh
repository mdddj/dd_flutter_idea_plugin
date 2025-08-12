#!/bin/bash

# FlutterX Plugin CI Test Script using act
# 用于在本地测试 GitHub Actions 工作流

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    echo -e "${CYAN}$1${NC}"
}

# 检查依赖
check_dependencies() {
    print_info "检查依赖..."
    
    if ! command -v act &> /dev/null; then
        print_error "act 未安装，请先安装 act: https://github.com/nektos/act"
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，act 需要 Docker 运行"
        exit 1
    fi
    
    # 检查 Docker 是否运行
    if ! docker info &> /dev/null; then
        print_error "Docker 未运行，请启动 Docker"
        exit 1
    fi
    
    print_success "依赖检查通过"
}

# 检查项目结构
check_project() {
    if [[ ! -f "build.gradle.kts" || ! -f "CHANGELOG.md" ]]; then
        print_error "请在项目根目录下运行此脚本"
        exit 1
    fi
    
    if [[ ! -f ".github/workflows/test-release.yml" ]]; then
        print_error "测试工作流文件不存在: .github/workflows/test-release.yml"
        exit 1
    fi
    
    print_success "项目结构检查通过"
}

# 显示可用的工作流
show_workflows() {
    print_header "📋 可用的工作流:"
    echo "1. test-release.yml    - 测试发布工作流 (推荐)"
    echo "2. release.yml         - 正式发布工作流"
    echo "3. build-test.yml      - 构建测试工作流"
    echo
}

# 测试发布工作流
test_release_workflow() {
    local version=${1:-"1.0.0"}
    
    print_header "🧪 测试发布工作流"
    print_info "测试版本: $version"
    
    # 创建测试 tag
    local test_tag="test-v$version"
    
    print_info "运行 act 测试..."
    
    # 使用 workflow_dispatch 事件
    act workflow_dispatch \
        --workflows .github/workflows/test-release.yml \
        --input version="$version" \
        --verbose \
        --artifact-server-path /tmp/act-artifacts \
        --env GITHUB_TOKEN=fake_token_for_testing \
        --platform ubuntu-latest=catthehacker/ubuntu:act-latest
}

# 测试构建工作流
test_build_workflow() {
    print_header "🔨 测试构建工作流"
    
    print_info "运行 act 测试..."
    
    # 模拟 push 事件
    act push \
        --workflows .github/workflows/build-test.yml \
        --verbose \
        --artifact-server-path /tmp/act-artifacts \
        --platform ubuntu-latest=catthehacker/ubuntu:act-latest
}

# 测试特定工作流
test_specific_workflow() {
    local workflow_file=$1
    local event_type=${2:-"push"}
    
    print_header "🎯 测试特定工作流: $workflow_file"
    
    if [[ ! -f ".github/workflows/$workflow_file" ]]; then
        print_error "工作流文件不存在: .github/workflows/$workflow_file"
        exit 1
    fi
    
    print_info "运行 act 测试..."
    
    act "$event_type" \
        --workflows ".github/workflows/$workflow_file" \
        --verbose \
        --artifact-server-path /tmp/act-artifacts \
        --env GITHUB_TOKEN=fake_token_for_testing \
        --platform ubuntu-latest=catthehacker/ubuntu:act-latest
}

# 列出所有工作流
list_workflows() {
    print_header "📋 项目中的所有工作流:"
    
    if [[ -d ".github/workflows" ]]; then
        find .github/workflows -name "*.yml" -o -name "*.yaml" | while read -r file; do
            local name=$(basename "$file")
            local workflow_name=$(grep -m 1 "^name:" "$file" 2>/dev/null | sed 's/name: *//' | tr -d '"' || echo "未命名")
            echo "  📄 $name - $workflow_name"
        done
    else
        print_warning "未找到 .github/workflows 目录"
    fi
    echo
}

# 显示 act 信息
show_act_info() {
    print_header "🔧 act 环境信息:"
    echo "  版本: $(act --version 2>/dev/null || echo '未知')"
    echo "  配置文件: $(test -f .actrc && echo '存在' || echo '不存在')"
    echo "  Docker 状态: $(docker info &>/dev/null && echo '运行中' || echo '未运行')"
    echo
}

# 清理函数
cleanup() {
    print_info "清理临时文件..."
    # 清理可能创建的测试 tag
    git tag -d test-v* 2>/dev/null || true
    print_success "清理完成"
}

# 显示帮助
show_help() {
    print_header "🚀 FlutterX Plugin CI 测试脚本"
    echo
    echo "用法: $0 [选项] [参数]"
    echo
    echo "选项:"
    echo "  -r, --release [版本号]     测试发布工作流 (默认版本: 1.0.0)"
    echo "  -b, --build                测试构建工作流"
    echo "  -w, --workflow <文件名>    测试特定工作流文件"
    echo "  -l, --list                 列出所有工作流"
    echo "  -i, --info                 显示环境信息"
    echo "  -c, --cleanup              清理测试数据"
    echo "  -h, --help                 显示此帮助信息"
    echo
    echo "示例:"
    echo "  $0 -r 5.8.0                # 测试发布工作流，版本 5.8.0"
    echo "  $0 --build                 # 测试构建工作流"
    echo "  $0 -w release.yml          # 测试 release.yml 工作流"
    echo "  $0 --list                  # 列出所有工作流"
    echo "  $0 --info                  # 显示环境信息"
    echo
    echo "注意:"
    echo "  - 确保 Docker 正在运行"
    echo "  - 首次运行可能需要下载 Docker 镜像"
    echo "  - 测试不会影响真实的 Git 仓库"
    echo
}

# 主函数
main() {
    # 设置清理陷阱
    trap cleanup EXIT
    
    # 检查参数
    if [[ $# -eq 0 ]]; then
        show_help
        exit 0
    fi
    
    # 检查依赖和项目
    check_dependencies
    check_project
    
    # 处理参数
    case "$1" in
        -r|--release)
            local version=${2:-"1.0.0"}
            test_release_workflow "$version"
            ;;
        -b|--build)
            test_build_workflow
            ;;
        -w|--workflow)
            if [[ -z "$2" ]]; then
                print_error "请指定工作流文件名"
                exit 1
            fi
            test_specific_workflow "$2" "${3:-push}"
            ;;
        -l|--list)
            list_workflows
            ;;
        -i|--info)
            show_act_info
            show_workflows
            ;;
        -c|--cleanup)
            cleanup
            ;;
        -h|--help)
            show_help
            ;;
        *)
            print_error "未知选项: $1"
            echo
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"