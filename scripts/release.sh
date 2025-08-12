#!/bin/bash

# FlutterX Plugin Release Script
# 用于简化本地发布流程的辅助脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# 检查是否在项目根目录
check_project_root() {
    if [[ ! -f "build.gradle.kts" || ! -f "CHANGELOG.md" ]]; then
        print_error "请在项目根目录下运行此脚本"
        exit 1
    fi
}

# 检查git状态
check_git_status() {
    if [[ -n $(git status --porcelain) ]]; then
        print_warning "工作目录有未提交的更改"
        read -p "是否继续？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 验证版本号格式
validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_error "版本号格式不正确，应该是 x.y.z 格式（如 5.7.0）"
        exit 1
    fi
}

# 检查tag是否已存在
check_tag_exists() {
    local tag=$1
    if git rev-parse "$tag" >/dev/null 2>&1; then
        print_error "Tag $tag 已存在"
        exit 1
    fi
}

# 更新CHANGELOG
update_changelog() {
    local version=$1
    local date=$(date +%Y-%m-%d)
    
    print_info "更新 CHANGELOG.md..."
    
    # 备份原文件
    cp CHANGELOG.md CHANGELOG.md.bak
    
    # 更新CHANGELOG
    awk -v version="$version" -v date="$date" '
    /^## Unreleased$/ {
        print "## Unreleased"
        print ""
        print "## [" version "] - " date
        next
    }
    { print }
    ' CHANGELOG.md.bak > CHANGELOG.md
    
    rm CHANGELOG.md.bak
    print_success "CHANGELOG.md 已更新"
}

# 更新版本号
update_version() {
    local version=$1
    
    print_info "更新 gradle.properties 中的版本号..."
    
    # 备份原文件
    cp gradle.properties gradle.properties.bak
    
    # 更新版本号
    sed -i.tmp "s/pluginVersion=.*/pluginVersion=${version}./" gradle.properties
    rm gradle.properties.tmp gradle.properties.bak
    
    print_success "版本号已更新为 $version"
}

# 构建插件
build_plugin() {
    print_info "开始构建插件..."
    
    if ./gradlew buildPlugin --no-daemon --stacktrace; then
        print_success "插件构建成功"
    else
        print_error "插件构建失败"
        exit 1
    fi
}

# 提交更改
commit_changes() {
    local version=$1
    
    print_info "提交版本更改..."
    
    git add CHANGELOG.md gradle.properties
    git commit -m "chore: release version $version"
    
    print_success "版本更改已提交"
}

# 创建并推送tag
create_and_push_tag() {
    local version=$1
    local tag="v$version"
    
    print_info "创建 tag: $tag"
    git tag "$tag"
    
    print_info "推送更改和tag到远程仓库..."
    git push origin main
    git push origin "$tag"
    
    print_success "Tag $tag 已创建并推送"
}

# 显示构建信息
show_build_info() {
    local version=$1
    local plugin_file=$(find build/distributions -name "*.zip" | head -1)
    
    echo
    print_success "🎉 发布流程完成！"
    echo
    echo "📋 发布信息:"
    echo "   版本: $version"
    echo "   Tag: v$version"
    if [[ -n $plugin_file ]]; then
        echo "   插件文件: $plugin_file"
        echo "   文件大小: $(du -h "$plugin_file" | cut -f1)"
    fi
    echo
    print_info "🔗 GitHub Actions 将自动创建 Release: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:\/]\([^\/]*\/[^\/]*\).*/\1/' | sed 's/\.git$//')/actions"
    echo
}

# 主函数
main() {
    echo "🚀 FlutterX Plugin Release Script"
    echo "================================="
    echo
    
    # 检查环境
    check_project_root
    check_git_status
    
    # 获取版本号
    if [[ -z $1 ]]; then
        echo "请输入新版本号（格式: x.y.z）:"
        read -r version
    else
        version=$1
    fi
    
    # 验证版本号
    validate_version "$version"
    
    # 检查tag是否存在
    local tag="v$version"
    check_tag_exists "$tag"
    
    # 确认发布
    echo
    print_warning "即将发布版本: $version"
    print_warning "这将会:"
    echo "  1. 更新 CHANGELOG.md"
    echo "  2. 更新 gradle.properties 中的版本号"
    echo "  3. 构建插件"
    echo "  4. 提交更改"
    echo "  5. 创建并推送 tag: $tag"
    echo "  6. 触发 GitHub Actions 自动发布"
    echo
    read -p "确认继续？(y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "发布已取消"
        exit 0
    fi
    
    # 执行发布流程
    update_changelog "$version"
    update_version "$version"
    build_plugin
    commit_changes "$version"
    create_and_push_tag "$version"
    show_build_info "$version"
}

# 如果直接执行脚本则运行主函数
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi