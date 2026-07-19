import { API_BASE_URL } from '../config.js'

/**
 * 处理图片URL
 * 统一处理图片路径，确保使用 /admin/common/image 接口
 * @param {string} url - 原始图片路径
 * @returns {string} - 处理后的完整图片URL
 */
export function resolveImageUrl(url) {
  if (!url) return ''
  
  // 去除首尾空格和反引号 (防止复制粘贴或后端数据包含脏字符)
  url = String(url).trim().replace(/^[`'"]+|[`'"]+$/g, '')
  
  // 如果已经是完整的HTTP链接或Base64，直接返回
  if (url.startsWith('http') || url.startsWith('data:')) {
    return url
  }
  
  // 处理相对路径
  let path = url
  
  // 移除开头的斜杠，方便统一处理
  if (path.startsWith('/')) {
    path = path.substring(1)
  }

  // 如果是本地静态资源（static目录开头），直接返回原始路径（带斜杠）
  if (path.startsWith('static/') || url.startsWith('/static/')) {
    return url
  }
  
  // 如果路径已经包含了 admin/common/image，则只拼接 Base URL
  // 注意：这里同时检查带斜杠和不带斜杠的情况
  if (path.startsWith('admin/common/image/')) {
    return `${API_BASE_URL}/${path}`
  }
  
  // 否则，拼接完整的接口路径
  return `${API_BASE_URL}/admin/common/image/${path}`
}
