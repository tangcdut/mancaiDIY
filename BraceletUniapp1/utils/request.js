// 注意：当前项目使用本地模拟数据，不进行真实网络请求
// 如需启用后端API，请配置正确的BASE_URL并修改 api/index.js

const BASE_URL = '' // 实际项目中填写后端API地址，如：'https://your-api.com'

const request = (url, method = 'GET', data = {}, header = {}) => {
  // 开发模式：不进行真实请求，直接返回模拟数据
  const isDevelopment = true // 改为 false 启用真实API调用
  
  if (isDevelopment || !BASE_URL) {
    console.warn('⚠️ 当前使用模拟数据模式，未进行真实网络请求:', url)
    return Promise.reject(new Error('当前使用本地模拟数据，请使用 api/index.js 中的模拟API'))
  }
  
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...header
      },
      timeout: 10000,
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(`请求失败: ${res.statusCode}`))
        }
      },
      fail: err => {
        console.error('网络请求失败:', err)
        reject(err)
      }
    })
  })
}

export const get = (url, params = {}) => request(url, 'GET', params)
export const post = (url, data = {}) => request(url, 'POST', data)
export const put = (url, data = {}) => request(url, 'PUT', data)
export const del = (url, data = {}) => request(url, 'DELETE', data)

export default request
