/**
 * HTTP request utility - Overseas H5 version
 * Uses real API calls to diy.hk.cn
 */

import { API_BASE_URL, TOKEN_HEADER, STORAGE_TOKEN_KEY } from '../config.js'

const request = (url, method = 'GET', data = {}, requireAuth = true, extraParams = {}) => {
  return new Promise((resolve, reject) => {
    // Build headers
    const header = {
      'Content-Type': 'application/json'
    }

    // Attach auth token if required and available
    if (requireAuth) {
      try {
        const token = uni.getStorageSync(STORAGE_TOKEN_KEY)
        if (token) {
          header[TOKEN_HEADER] = token
        }
      } catch (e) {
        // No token, proceed without
      }
    }

    // Handle extra params for GET requests
    let requestUrl = API_BASE_URL + url
    if (method === 'GET' && Object.keys(extraParams).length > 0) {
      const queryString = Object.entries(extraParams)
        .map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v))
        .join('&')
      requestUrl += '?' + queryString
    }

    uni.request({
      url: requestUrl,
      method,
      data: method === 'GET' ? data : { ...data, ...extraParams },
      header,
      timeout: 10000,
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(`Request failed: ${res.statusCode}`))
        }
      },
      fail: err => {
        console.error('Network request failed:', err)
        reject(err)
      }
    })
  })
}

export const get = (url, params = {}, requireAuth = true) => request(url, 'GET', params, requireAuth)
export const post = (url, data = {}, requireAuth = true, extraParams = {}) => request(url, 'POST', data, requireAuth, extraParams)
export const put = (url, data = {}, requireAuth = true, extraParams = {}) => request(url, 'PUT', data, requireAuth, extraParams)
export const del = (url, data = {}, requireAuth = true, extraParams = {}) => request(url, 'DELETE', data, requireAuth, extraParams)

export default request
