/**
 * HTTP请求封装 - 统一处理JWT认证和错误
 */

import { 
  API_BASE_URL, 
  TOKEN_HEADER, 
  STORAGE_TOKEN_KEY,
  REQUEST_TIMEOUT,
  REQUEST_HEADERS,
  RESPONSE_CODE
} from '../config.js'

/**
 * 获取本地存储的Token
 */
function getToken() {
  try {
    let token = uni.getStorageSync(STORAGE_TOKEN_KEY)
    if (!token) {
      // 自动生成游客Token (GUEST_ + 随机uuid字符)
      const uuid = 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
      token = 'GUEST_' + uuid
      uni.setStorageSync(STORAGE_TOKEN_KEY, token)
      console.log('✨ 自动生成游客Token并保存:', token)
    }
    return token
  } catch (e) {
    console.error('获取Token失败:', e)
    return ''
  }
}

/**
 * 统一请求方法
 * @param {Object} options 请求配置
 * @param {String} options.url 接口路径（相对路径）
 * @param {String} options.method 请求方法 GET/POST/PUT/DELETE
 * @param {Object} options.data 请求参数
 * @param {Boolean} options.needAuth 是否需要认证，默认true
 * @param {Object} options.header 自定义请求头
 */
export function request(options) {
  const {
    url,
    method = 'GET',
    data = {},
    needAuth = true,
    header = {},
    timeout
  } = options

  // 构建完整URL
  const fullUrl = `${API_BASE_URL}${url}`
  
  // 添加调试日志
  console.log(`📡 ${method}请求:`, fullUrl)
  if (method !== 'GET' && data && Object.keys(data).length > 0) {
    console.log('📦 请求数据:', data)
  }

  // 构建请求头
  const requestHeader = {
    ...REQUEST_HEADERS,
    ...header
  }

  // 如果需要认证，添加Token
  if (needAuth) {
    const token = getToken()
    if (token) {
      requestHeader[TOKEN_HEADER] = token
    } else {
      console.warn('请求需要认证但Token不存在:', url)
      // 立即拒绝，不再发送注定401的请求
      return Promise.reject({
        code: 401,
        msg: '未登录或登录已过期',
        data: null,
        needLogin: true
      })
    }
  }

  return new Promise((resolve, reject) => {
    // GET请求不应该使用data，参数已经在URL中
    const requestConfig = {
      url: fullUrl,
      method,
      header: requestHeader,
      timeout: timeout || REQUEST_TIMEOUT,
    }
    
    // 只有非GET请求才传递data
    if (method !== 'GET') {
      requestConfig.data = data
    }
    
    uni.request({
      ...requestConfig,
      success: (res) => {
        // 检查HTTP状态码
        if (res.statusCode !== 200) {
          const error = {
            code: res.statusCode,
            msg: `HTTP错误: ${res.statusCode}`,
            data: null
          }
          console.error('HTTP请求失败:', error)
          reject(error)
          return
        }

        // 检查业务状态码
        const responseData = res.data
        
        // 兼容处理：新旧接口成功码不同
        // 旧接口 code=1 为成功，code=0 为失败
        // 新接口(/design/等) code=0 为成功，且 data 不为 null
        const isOldSuccess = responseData.code === RESPONSE_CODE.SUCCESS        // code === 1
        const isNewSuccess = responseData.code === RESPONSE_CODE.SUCCESS_NEW     // code === 0
                             && responseData.data != null                        // 旧接口code=0失败时data为null

        if (isOldSuccess || isNewSuccess) {
          // 成功，返回data字段
          resolve(responseData.data || responseData)
        } else {
          // 业务失败
          const error = {
            code: responseData.code,
            msg: responseData.msg || '请求失败',
            data: responseData.data
          }
          console.error('业务请求失败:', error, 'URL:', url)
          
          // 如果是token失效，清除本地token并跳转登录
          if (responseData.code === 401 || responseData.msg?.includes('token')) {
            handleTokenExpired()
          }
          
          reject(error)
        }
      },
      fail: (err) => {
        const error = {
          code: 0,
          msg: err.errMsg || '网络请求失败',
          data: null
        }
        console.error('网络请求失败:', error)
        reject(error)
      }
    })
  })
}

/**
 * 处理Token过期
 */
function handleTokenExpired() {
  try {
    uni.removeStorageSync(STORAGE_TOKEN_KEY)
    uni.removeStorageSync('user')
    
    // 提示用户重新登录
    uni.showToast({
      title: '登录已过期，请重新登录',
      icon: 'none',
      duration: 2000
    })
    
    // 延迟跳转到登录页
    setTimeout(() => {
      uni.reLaunch({
        url: '/pages/index/index'
      })
    }, 2000)
  } catch (e) {
    console.error('处理Token过期失败:', e)
  }
}

/**
 * GET请求
 */
export function get(url, params = {}, needAuth = true) {
  // GET请求参数需要拼接到URL上
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  
  const fullUrl = queryString ? `${url}?${queryString}` : url
  
  // 调试日志
  console.log('📤 GET请求:', fullUrl, '参数:', params)

  return request({
    url: fullUrl,
    method: 'GET',
    needAuth
  })
}

/**
 * POST请求
 */
export function post(url, data = {}, needAuth = true, timeout = undefined) {
  return request({
    url,
    method: 'POST',
    data,
    needAuth,
    timeout
  })
}

/**
 * PUT请求
 */
export function put(url, data = {}, needAuth = true) {
  return request({
    url,
    method: 'PUT',
    data,
    needAuth
  })
}

/**
 * DELETE请求
 */
export function del(url, data = {}, needAuth = true) {
  return request({
    url,
    method: 'DELETE',
    data,
    needAuth
  })
}
