export default {
  onLoad() {
    // 开启分享功能：发送给朋友、分享到朋友圈
    uni.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
  },
  // 发送给朋友
  onShareAppMessage(res) {
    // 获取当前页面栈
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1] || {}
    const route = currentPage.route || 'pages/index/index'
    
    // 构建页面参数
    let queryString = ''
    if (currentPage.options && Object.keys(currentPage.options).length > 0) {
      queryString = '?' + Object.keys(currentPage.options)
        .map(key => `${key}=${currentPage.options[key]}`)
        .join('&')
    }
    
    // 默认标题
    let title = '满彩珠宝'
    
    // 如果是来自于按钮的分享
    if (res.from === 'button') {
      // console.log(res.target)
    }
    
    return {
      title: title,
      path: '/' + route + queryString,
      success: function() {
        console.log('分享成功')
      },
      fail: function() {
        console.log('分享失败')
      }
    }
  },
  // 分享到朋友圈
  onShareTimeline(res) {
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1] || {}
    
    // 构建 query 参数字符串
    let query = ''
    if (currentPage.options) {
      query = Object.keys(currentPage.options)
        .map(key => `${key}=${currentPage.options[key]}`)
        .join('&')
    }
    
    return {
      title: '满彩珠宝',
      query: query
    }
  }
}
