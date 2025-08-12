#!/bin/bash

# GitHub Actions Workflow Validation Script
# 验证 GitHub Actions 工作流文件语法和配置

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

# 检查是否在项目根目录
check_project_root() {
    if [[ ! -f "build.gradle.kts" || ! -f "CHANGELOG.md" ]]; then
        print_error "请在项目根目录下运行此脚本"
        exit 1
    fi
}

# 检查工作流目录
check_workflows_dir() {
    if [[ ! -d ".github/workflows" ]]; then
        print_error "未找到 .github/workflows 目录"
        exit 1
    fi
}

# 验证 YAML 语法
validate_yaml_syntax() {
    local file=$1
    local basename_file=$(basename "$file")
    
    print_info "验证 YAML 语法: $basename_file"
    
    # 检查是否有 yq 或 python
    if command -v yq &> /dev/null; then
        if yq eval '.' "$file" &> /dev/null; then
            print_success "YAML 语法正确"
        else
            print_error "YAML 语法错误"
            return 1
        fi
    elif command -v python3 &> /dev/null; then
        if python3 -c "import yaml; yaml.safe_load(open('$file'))" &> /dev/null; then
            print_success "YAML 语法正确"
        else
            print_error "YAML 语法错误"
            return 1
        fi
    elif command -v python &> /dev/null; then
        if python -c "import yaml; yaml.safe_load(open('$file'))" &> /dev/null; then
            print_success "YAML 语法正确"
        else
            print_error "YAML 语法错误"
            return 1
        fi
    else
        print_warning "未找到 YAML 验证工具 (yq/python)，跳过语法检查"
    fi
}

# 验证工作流基本结构
validate_workflow_structure() {
    local file=$1
    local basename_file=$(basename "$file")
    
    print_info "验证工作流结构: $basename_file"
    
    # 检查必需的顶级字段
    local has_name=$(grep -q "^name:" "$file" && echo "true" || echo "false")
    local has_on=$(grep -q "^on:" "$file" && echo "true" || echo "false")
    local has_jobs=$(grep -q "^jobs:" "$file" && echo "true" || echo "false")
    
    if [[ "$has_name" == "true" ]]; then
        print_success "包含 name 字段"
    else
        print_warning "缺少 name 字段"
    fi
    
    if [[ "$has_on" == "true" ]]; then
        print_success "包含 on 字段"
    else
        print_error "缺少必需的 on 字段"
        return 1
    fi
    
    if [[ "$has_jobs" == "true" ]]; then
        print_success "包含 jobs 字段"
    else
        print_error "缺少必需的 jobs 字段"
        return 1
    fi
}

# 检查常见的 Actions
validate_actions() {
    local file=$1
    local basename_file=$(basename "$file")
    
    print_info "检查 Actions 版本: $basename_file"
    
    # 检查常用 Actions 的版本
    while IFS= read -r line; do
        if [[ "$line" =~ uses:.*actions/checkout@(.+) ]]; then
            local version="${BASH_REMATCH[1]}"
            if [[ "$version" =~ ^v[4-9] ]]; then
                print_success "checkout action 版本 $version (推荐)"
            else
                print_warning "checkout action 版本 $version (建议升级到 v4+)"
            fi
        fi
        
        if [[ "$line" =~ uses:.*actions/setup-java@(.+) ]]; then
            local version="${BASH_REMATCH[1]}"
            if [[ "$version" =~ ^v[4-9] ]]; then
                print_success "setup-java action 版本 $version (推荐)"
            else
                print_warning "setup-java action 版本 $version (建议升级到 v4+)"
            fi
        fi
        
        if [[ "$line" =~ uses:.*actions/cache@(.+) ]]; then
            local version="${BASH_REMATCH[1]}"
            if [[ "$version" =~ ^v[4-9] ]]; then
                print_success "cache action 版本 $version (推荐)"
            else
                print_warning "cache action 版本 $version (建议升级到 v4+)"
            fi
        fi
        
        if [[ "$line" =~ uses:.*actions/upload-artifact@(.+) ]]; then
            local version="${BASH_REMATCH[1]}"
            if [[ "$version" =~ ^v[4-9] ]]; then
                print_success "upload-artifact action 版本 $version (推荐)"
            else
                print_warning "upload-artifact action 版本 $version (建议升级到 v4+)"
            fi
        fi
    done < "$file"
}

# 检查项目特定配置
validate_project_specific() {
    local file=$1
    local basename_file=$(basename "$file")
    
    print_info "检查项目特定配置: $basename_file"
    
    # 检查 Java 版本
    if grep -q "java-version.*21" "$file"; then
        print_success "使用 Java 21"
    elif grep -q "java-version" "$file"; then
        local java_version=$(grep "java-version" "$file" | head -1 | sed 's/.*java-version[[:space:]]*:[[:space:]]*["\x27]*//' | sed 's/["\x27].*//')
        print_warning "使用 Java $java_version (项目需要 Java 21)"
    fi
    
    # 检查 Gradle 构建命令
    if grep -q "gradlew.*buildPlugin" "$file"; then
        print_success "包含插件构建命令"
    elif grep -q "gradlew" "$file"; then
        print_warning "包含 Gradle 命令但可能缺少 buildPlugin"
    fi
    
    # 检查工作目录设置
    if grep -q "working-directory:.*dd_flutter_idea_plugin" "$file"; then
        print_success "正确设置工作目录"
    elif grep -q "working-directory" "$file"; then
        print_warning "工作目录可能设置不正确"
    fi
}

# 检查安全性
validate_security() {
    local file=$1
    local basename_file=$(basename "$file")
    
    print_info "检查安全配置: $basename_file"
    
    # 检查是否硬编码了敏感信息
    if grep -qi "password\|secret\|key" "$file" | grep -v "secrets\." | grep -v "env\." > /dev/null; then
        print_warning "可能包含硬编码的敏感信息，请检查"
    else
        print_success "未发现明显的硬编码敏感信息"
    fi
    
    # 检查是否正确使用 secrets
    if grep -q "secrets\." "$file"; then
        print_success "正确使用 GitHub Secrets"
    fi
    
    # 检查权限设置
    if grep -q "permissions:" "$file"; then
        print_success "定义了权限设置"
    else
        print_warning "未明确定义权限，将使用默认权限"
    fi
}

# 生成报告
generate_report() {
    local total_files=$1
    local valid_files=$2
    local issues_count=$3
    
    echo
    print_header "📊 验证报告"
    echo "======================================"
    echo "总工作流文件数: $total_files"
    echo "通过验证: $valid_files"
    echo "发现问题: $issues_count"
    echo
    
    if [[ $issues_count -eq 0 ]]; then
        print_success "🎉 所有工作流文件验证通过！"
    elif [[ $issues_count -lt 3 ]]; then
        print_warning "⚠️ 发现少量问题，建议修复"
    else
        print_error "❌ 发现多个问题，需要修复"
    fi
}

# 主函数
main() {
    print_header "🔍 GitHub Actions 工作流验证"
    echo "====================================="
    echo
    
    # 检查环境
    check_project_root
    check_workflows_dir
    
    local total_files=0
    local valid_files=0
    local total_issues=0
    
    # 遍历所有工作流文件
    for workflow_file in .github/workflows/*.yml .github/workflows/*.yaml; do
        [[ -f "$workflow_file" ]] || continue
        
        local basename_file=$(basename "$workflow_file")
        local file_issues=0
        
        echo
        print_header "📄 验证文件: $basename_file"
        echo "----------------------------------------"
        
        # YAML 语法验证
        if ! validate_yaml_syntax "$workflow_file"; then
            ((file_issues++))
        fi
        
        # 工作流结构验证
        if ! validate_workflow_structure "$workflow_file"; then
            ((file_issues++))
        fi
        
        # Actions 版本检查
        validate_actions "$workflow_file"
        
        # 项目特定验证
        validate_project_specific "$workflow_file"
        
        # 安全性检查
        validate_security "$workflow_file"
        
        ((total_files++))
        
        if [[ $file_issues -eq 0 ]]; then
            ((valid_files++))
            print_success "文件 $basename_file 验证通过"
        else
            print_error "文件 $basename_file 存在 $file_issues 个问题"
        fi
        
        ((total_issues += file_issues))
    done
    
    # 检查是否存在 .actrc 配置
    echo
    print_header "🔧 Act 配置检查"
    echo "----------------------------------------"
    
    if [[ -f ".actrc" ]]; then
        print_success "找到 .actrc 配置文件"
        
        if grep -q "ubuntu-latest.*catthehacker" ".actrc"; then
            print_success "使用推荐的 Docker 镜像"
        else
            print_warning "可能未使用推荐的 Docker 镜像"
        fi
        
        if grep -q "GRADLE_OPTS" ".actrc"; then
            print_success "配置了 Gradle 选项"
        else
            print_warning "未配置 Gradle 选项"
        fi
    else
        print_warning "未找到 .actrc 配置文件"
    fi
    
    if [[ -f ".env.act" ]]; then
        print_success "找到 .env.act 环境变量文件"
    else
        print_warning "未找到 .env.act 环境变量文件"
    fi
    
    # 生成最终报告
    generate_report $total_files $valid_files $total_issues
    
    # 返回适当的退出码
    if [[ $total_issues -eq 0 ]]; then
        exit 0
    else
        exit 1
    fi
}

# 显示帮助
show_help() {
    print_header "🔍 GitHub Actions 工作流验证脚本"
    echo
    echo "用法: $0 [选项]"
    echo
    echo "此脚本会验证项目中的所有 GitHub Actions 工作流文件"
    echo
    echo "验证内容:"
    echo "  ✅ YAML 语法正确性"
    echo "  ✅ 工作流基本结构"
    echo "  ✅ Actions 版本检查"
    echo "  ✅ 项目特定配置"
    echo "  ✅ 安全性检查"
    echo "  ✅ Act 配置检查"
    echo
    echo "选项:"
    echo "  -h, --help     显示此帮助信息"
    echo
    echo "依赖 (可选):"
    echo "  - yq 或 python (用于 YAML 语法验证)"
    echo
}

# 处理参数
case "${1:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    "")
        main
        ;;
    *)
        print_error "未知选项: $1"
        echo
        show_help
        exit 1
        ;;
esac