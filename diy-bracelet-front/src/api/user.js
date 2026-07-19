import request from './request'

// 获取 SMTP 服务器列表
export function getSmtpServers () {
  return request({
    url: '/mail-servers/smtp-servers',
    method: 'get'
  })
}

// 用户登录
export function login (data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}
