#!/usr/bin/env python3
"""
CHANGELOG 更新脚本
用于在 CI/CD 流程中更新 CHANGELOG.md 文件
"""

import re
import sys
import argparse
from datetime import datetime
from pathlib import Path


def update_changelog(changelog_path, version, date=None, test_mode=False):
    """
    更新 CHANGELOG.md 文件
    
    Args:
        changelog_path: CHANGELOG.md 文件路径
        version: 新版本号
        date: 发布日期，默认为今天
        test_mode: 测试模式，会在版本号后添加 (TEST RELEASE)
    """
    if date is None:
        date = datetime.now().strftime('%Y-%m-%d')
    
    changelog_file = Path(changelog_path)
    if not changelog_file.exists():
        print(f"错误: 找不到文件 {changelog_path}")
        sys.exit(1)
    
    # 读取文件内容
    content = changelog_file.read_text(encoding='utf-8')
    
    # 构建新版本行
    version_suffix = " (TEST RELEASE)" if test_mode else ""
    new_version_line = f"## [{version}] - {date}{version_suffix}"
    
    # 在 Unreleased 后添加新版本
    unreleased_pattern = r'(## Unreleased\s*\n)'
    replacement = f'\\1\n{new_version_line}\n'
    
    if re.search(unreleased_pattern, content):
        new_content = re.sub(unreleased_pattern, replacement, content)
        
        # 写回文件
        changelog_file.write_text(new_content, encoding='utf-8')
        print(f"✅ 已更新 CHANGELOG.md，添加版本 {version}")
        return True
    else:
        print("⚠️ 未找到 '## Unreleased' 部分")
        return False


def extract_changelog(changelog_path, version, output_path=None):
    """
    提取指定版本的变更日志
    
    Args:
        changelog_path: CHANGELOG.md 文件路径
        version: 要提取的版本号
        output_path: 输出文件路径，默认为 release_notes.md
    """
    changelog_file = Path(changelog_path)
    if not changelog_file.exists():
        print(f"错误: 找不到文件 {changelog_path}")
        sys.exit(1)
    
    content = changelog_file.read_text(encoding='utf-8')
    
    # 查找指定版本的变更日志
    pattern = rf'## \[{re.escape(version)}\].*?\n(.*?)(?=\n## \[|\Z)'
    match = re.search(pattern, content, re.DOTALL)
    
    if match:
        changelog_content = match.group(1).strip()
        
        if output_path:
            output_file = Path(output_path)
            # 如果文件已存在，追加内容
            if output_file.exists():
                existing_content = output_file.read_text(encoding='utf-8')
                new_content = existing_content + '\n\n' + changelog_content + '\n'
            else:
                new_content = changelog_content + '\n'
            
            output_file.write_text(new_content, encoding='utf-8')
            print(f"✅ 已提取版本 {version} 的变更日志到 {output_path}")
        else:
            # 输出到标准输出
            print(changelog_content)
        
        return True
    else:
        print(f"⚠️ 未找到版本 {version} 的变更日志")
        return False


def validate_changelog(changelog_path):
    """
    验证 CHANGELOG.md 文件格式
    
    Args:
        changelog_path: CHANGELOG.md 文件路径
    """
    changelog_file = Path(changelog_path)
    if not changelog_file.exists():
        print(f"错误: 找不到文件 {changelog_path}")
        return False
    
    content = changelog_file.read_text(encoding='utf-8')
    issues = []
    
    # 检查是否有 Unreleased 部分
    if not re.search(r'## Unreleased', content):
        issues.append("缺少 '## Unreleased' 部分")
    
    # 检查版本格式
    version_pattern = r'## \[\d+\.\d+\.\d+\]'
    versions = re.findall(version_pattern, content)
    
    if not versions:
        issues.append("未找到有效的版本号格式 (应为 ## [x.y.z])")
    
    # 检查日期格式
    date_pattern = r'## \[.*?\] - \d{4}-\d{2}-\d{2}'
    dates = re.findall(date_pattern, content)
    
    if len(dates) < len(versions):
        issues.append("某些版本缺少日期信息")
    
    if issues:
        print("❌ CHANGELOG.md 验证失败:")
        for issue in issues:
            print(f"  - {issue}")
        return False
    else:
        print("✅ CHANGELOG.md 格式正确")
        return True


def main():
    parser = argparse.ArgumentParser(description='CHANGELOG.md 管理工具')
    subparsers = parser.add_subparsers(dest='command', help='可用命令')
    
    # 更新命令
    update_parser = subparsers.add_parser('update', help='更新 CHANGELOG.md')
    update_parser.add_argument('version', help='版本号 (如 1.0.0)')
    update_parser.add_argument('--date', help='发布日期 (YYYY-MM-DD)')
    update_parser.add_argument('--test', action='store_true', help='测试模式')
    update_parser.add_argument('--file', default='CHANGELOG.md', help='CHANGELOG 文件路径')
    
    # 提取命令
    extract_parser = subparsers.add_parser('extract', help='提取版本变更日志')
    extract_parser.add_argument('version', help='版本号 (如 1.0.0)')
    extract_parser.add_argument('--output', help='输出文件路径')
    extract_parser.add_argument('--file', default='CHANGELOG.md', help='CHANGELOG 文件路径')
    
    # 验证命令
    validate_parser = subparsers.add_parser('validate', help='验证 CHANGELOG.md 格式')
    validate_parser.add_argument('--file', default='CHANGELOG.md', help='CHANGELOG 文件路径')
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        sys.exit(1)
    
    success = True
    
    if args.command == 'update':
        success = update_changelog(args.file, args.version, args.date, args.test)
    elif args.command == 'extract':
        success = extract_changelog(args.file, args.version, args.output)
    elif args.command == 'validate':
        success = validate_changelog(args.file)
    
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()