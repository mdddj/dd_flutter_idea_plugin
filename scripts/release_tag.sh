#!/bin/bash

# 当任何命令失败时，立即退出脚本
set -e

# --- 配置 ---
# 检查 git 命令是否存在
if ! command -v git &> /dev/null
then
    echo "错误: git 命令未找到. 请先安装 Git."
    exit 1
fi


# --- 函数定义 ---

# 显示用法说明
usage() {
  echo "用法: $0 <tag_name>"
  echo "例如: $0 v1.2.3"
  exit 1
}

# 创建并推送标签
create_and_push_tag() {
  local TAG_NAME=$1
  echo "➡️ 正在创建本地标签: $TAG_NAME..."
  git tag "$TAG_NAME"
  echo "✅ 本地标签创建成功."

  echo "➡️ 正在推送标签到远程仓库..."
  git push origin "$TAG_NAME"
  echo "✅ 标签 '$TAG_NAME' 已成功推送到远程仓库."
}

# 删除本地和远程的标签
delete_tag() {
  local TAG_NAME=$1
  echo "➡️ 正在删除本地标签: $TAG_NAME..."
  git tag -d "$TAG_NAME" || echo "ℹ️ 本地标签不存在, 跳过删除."

  echo "➡️ 正在删除远程标签: $TAG_NAME..."
  git push --delete origin "$TAG_NAME" || echo "ℹ️ 远程标签不存在, 跳过删除."
  
  echo "✅ 标签 '$TAG_NAME' 已从本地和远程仓库删除."
}

# --- 主逻辑 ---

# 检查是否提供了标签名
if [ -z "$1" ]; then
  usage
fi

TAG_NAME=$1

# 主菜单
echo "🚀 Git 标签发布助手"
echo "----------------------------------"
echo "操作的标签: $TAG_NAME"
echo "----------------------------------"
echo "1. 发布新标签"
echo "2. 删除并重试"
echo "3. 仅删除标签"
echo "4. 退出"
echo "----------------------------------"

read -p "请输入你的选择 [1-4]: " choice

case $choice in
  1)
    echo "--- 开始发布新标签 ---"
    create_and_push_tag "$TAG_NAME"
    ;;
  2)
    echo "--- 开始删除并重试 ---"
    delete_tag "$TAG_NAME"
    echo "🔄 准备重试..."
    create_and_push_tag "$TAG_NAME"
    ;;
  3)
    echo "--- 开始仅删除标签 ---"
    delete_tag "$TAG_NAME"
    ;;
  4)
    echo "👋 已取消操作."
    exit 0
    ;;
  *)
    echo "❌ 无效的选择. 脚本退出."
    exit 1
    ;;
esac

echo "🎉 操作完成!"
