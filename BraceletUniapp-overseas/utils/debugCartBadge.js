/**
 * 购物车角标调试工具
 */

import { getCartList, isLoggedIn } from '../api/index.js'

/**
 * 调试购物车角标状态
 */
export async function debugCartBadge() {
  console.log('========== 购物车角标调试 ==========')
  
  // 1. 检查登录状态
  const loggedIn = isLoggedIn()
  console.log('1. 登录状态:', loggedIn)
  
  if (!loggedIn) {
    console.log('❌ 用户未登录，角标应该被清除')
    return
  }
  
  // 2. 获取购物车数据
  try {
    const res = await getCartList()
    console.log('2. 购物车原始数据:', res)
    
    const items = res.items || []
    console.log('3. 购物车商品项:', items)
    console.log('4. 商品项数量:', items.length)
    
    // 3. 计算总数量
    const totalCount = items.reduce((sum, item) => {
      console.log(`   - 商品: ${item.title}, 数量: ${item.quantity}`)
      return sum + (item.quantity || 0)
    }, 0)
    console.log('5. 总商品数量:', totalCount)
    
    // 4. 角标文本
    const badgeText = totalCount > 99 ? '99+' : String(totalCount)
    console.log('6. 角标显示文本:', badgeText)
    
    // 5. TabBar索引
    console.log('7. TabBar索引:', 2)
    
    console.log('✅ 角标应该显示:', totalCount > 0 ? badgeText : '无角标')
  } catch (e) {
    console.error('❌ 获取购物车数据失败:', e)
  }
  
  console.log('====================================')
}

// 在全局暴露调试函数
if (typeof window !== 'undefined') {
  window.debugCartBadge = debugCartBadge
}
