import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  baseURL: '',
  timeout: 5000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.authentication = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 1) {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  error => {
    return Promise.reject(error)
  }
)

export default request
