/**
 * 购物车角标管理
 */

import { getCartList, isLoggedIn } from '../api/index.js'

// 购物车在TabBar中的索引
const CART_TAB_INDEX = 2

// 防抖定时器
let updateTimer = null

/**
 * 更新购物车角标（带防抖）
 */
export function updateCartBadge() {
  // 清除之前的定时器
  if (updateTimer) {
    clearTimeout(updateTimer)
  }
  
  // 设置新的定时器，300ms后执行
  updateTimer = setTimeout(() => {
    doUpdateCartBadge()
  }, 300)
}

/**
 * 立即更新购物车角标（不防抖）
 */
export function updateCartBadgeNow() {
  doUpdateCartBadge()
}

/**
 * 检查当前页面是否在TabBar页面列表中
 */
function isInTabBarPages() {
  try {
    const pages = getCurrentPages()
    if (pages.length === 0) return false
    
    const currentPage = pages[pages.length - 1]
    const route = currentPage.route || ''
    
    // TabBar页面列表
    const tabBarPages = [
      'pages/index/index',
      'pages/design/index',
      'pages/cart/index',
      'pages/mine/index'
    ]
    
    return tabBarPages.includes(route)
  } catch (e) {
    console.error('检查TabBar页面失败:', e)
    return false
  }
}

/**
 * 实际执行更新购物车角标的函数
 */
async function doUpdateCartBadge() {
  try {
    // 先检查登录状态
    if (!isLoggedIn()) {
      console.log('用户未登录，清除购物车角标')
      clearCartBadge()
      return
    }
    
    const res = await getCartList()
    const items = res.items || []
    const totalCount = items.reduce((sum, item) => sum + (item.quantity || 0), 0)
    
    console.log('更新购物车角标，商品数量:', totalCount, '商品项:', items.length)
    
    if (totalCount > 0) {
      // 使用try-catch包裹，避免在非TabBar页面报错
      try {
        uni.setTabBarBadge({
          index: CART_TAB_INDEX,
          text: totalCount > 99 ? '99+' : String(totalCount),
          success: () => {
            console.log('角标更新成功')
          },
          fail: (err) => {
            // 如果不是TabBar页面，会失败，这是正常的
            if (err.errMsg && err.errMsg.includes('not TabBar page')) {
              console.log('当前不是TabBar页面，跳过角标更新')
            } else {
              console.error('角标更新失败:', err)
            }
          }
        })
      } catch (e) {
        console.log('setTabBarBadge调用异常（可能不在TabBar页面）:', e.message)
      }
    } else {
      try {
        uni.removeTabBarBadge({
          index: CART_TAB_INDEX,
          fail: (err) => {
            // 忽略非TabBar页面的错误
            if (!err.errMsg || !err.errMsg.includes('not TabBar page')) {
              console.error('清除角标失败:', err)
            }
          }
        })
      } catch (e) {
        console.log('removeTabBarBadge调用异常:', e.message)
      }
    }
  } catch (e) {
    console.error('更新购物车角标失败:', e)
    // 如果失败（如未登录或401错误），清除角标
    clearCartBadge()
  }
}

/**
 * 清除购物车角标
 */
export function clearCartBadge() {
  try {
    uni.removeTabBarBadge({
      index: CART_TAB_INDEX,
      fail: (err) => {
        // 忽略非TabBar页面的错误
        if (err.errMsg && !err.errMsg.includes('not TabBar page')) {
          console.error('清除角标失败:', err)
        }
      }
    })
  } catch (e) {
    // 忽略异常，可能不在TabBar页面
    console.log('clearCartBadge调用异常:', e.message)
  }
}
