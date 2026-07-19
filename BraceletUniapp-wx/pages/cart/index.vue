<template>
  <view class="page">
    <!-- 未登录提示 -->
    <view v-if="!isLoggedIn" class="login-prompt">
      <view class="icon"><Icon name="lock" size="100rpx" color="#bbb" /></view>
      <view class="tip">请先登录查看购物车</view>
      <button class="login-btn" @click="goToLogin">去登录</button>
    </view>

    <!-- 已登录：购物车列表 -->
    <view v-if="isLoggedIn">
      <view v-if="!items.length" class="empty">购物车空空如也～</view>

      <view v-else class="list">
        <view v-for="i in items" :key="i.productId || i.id" class="row" :class="{updating: updating, deleting: deleting}">
          <view class="thumb">
            <image v-if="i.imageUrl" :src="i.imageUrl" mode="aspectFill" class="thumb-img" />
          </view>
          <view class="meta">
            <view class="title">{{ i.title }}</view>
            <view class="price">¥{{ i.price }}</view>
            <view class="stepper">
              <view class="s-btn" :class="{disabled: updating || deleting}" @click="dec(i)">-</view>
              <input class="ipt" type="number" v-model.number="i.quantity" @blur="apply(i)" :disabled="updating || deleting" />
              <view class="s-btn" :class="{disabled: updating || deleting}" @click="inc(i)">+</view>
            </view>
          </view>
          <view class="remove" :class="{disabled: updating || deleting}" @click="removeItem(i.productId || i.id)">×</view>
        </view>
      </view>

      <view class="bar">
        <view class="total">合计：<text class="money">¥{{ total }}</text></view>
        <button class="checkout" :disabled="!items.length" @click="goCheckout">去结算</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { cartList, cartDelete, orderCreate, cartUpdate, isLoggedIn as checkLogin, requireLogin } from '../../api/index.js'
import { onShow } from '@dcloudio/uni-app'
import { updateCartBadge, updateCartBadgeNow } from '../../utils/cartBadge.js'
import { debugCartBadge } from '../../utils/debugCartBadge.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import Icon from '../../components/Icon.vue'

const isLoggedIn = ref(false)
const items = ref([])
const updating = ref(false) // 正在更新中
const deleting = ref(false) // 正在删除中

const total = computed(() => {
  return items.value.reduce((sum, i) => sum + Number(i.price || 0) * Number(i.quantity || 0), 0).toFixed(2)
})

// 检查登录状态
function checkLoginStatus() {
  isLoggedIn.value = checkLogin()
  console.log('购物车登录状态:', isLoggedIn.value)
}

// 跳转登录
function goToLogin() {
  uni.reLaunch({ url: '/pages/index/index' })
}

async function load() {
  // 先检查登录状态
  checkLoginStatus()
  
  // 如果未登录，不加载购物车数据
  if (!isLoggedIn.value) {
    console.log('未登录，跳过加载购物车数据')
    return
  }
  
  try {
    const res = await cartList()
    console.log('购物车数据:', res)
    
    let list = res.items || []
    // 处理图片链接
    list = list.map(item => {
      let imageUrl = item.coverImage || item.imageUrl || item.image || ''
      imageUrl = resolveImageUrl(imageUrl)
      return { ...item, imageUrl }
    })
    
    items.value = list
    // 立即更新购物车角标
    updateCartBadgeNow()
  } catch (e) {
    console.error('加载购物车失败:', e)
    // 如果是401错误，说明token失效
    if (e.code === 401) {
      isLoggedIn.value = false
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
    }
  }
}

async function removeItem(id) {
  if (deleting.value || updating.value) {
    console.log('操作进行中，请稍后')
    return
  }
  
  deleting.value = true
  try {
    await cartDelete(Number(id))
    await load()
    // 立即更新购物车角标
    updateCartBadgeNow()
    uni.showToast({ title: '已删除', icon: 'success', duration: 1000 })
  } catch (e) {
    console.error('删除失败:', e)
    uni.showToast({ title: '删除失败', icon: 'none' })
  } finally {
    deleting.value = false
  }
}

// 去结算 - 跳转到订单确认页
function goCheckout() {
  if (!items.value || items.value.length === 0) {
    uni.showToast({ title: '购物车为空', icon: 'none' })
    return
  }
  
  uni.navigateTo({ url: '/pages/order/confirm' })
}

async function apply(i) {
  if (updating.value) return
  
  updating.value = true
  const pid = i.productId || i.id
  const qty = Math.max(1, Number(i.quantity || 1))
  i.quantity = qty
  
  try {
    await cartUpdate(pid, qty)
    // 更新购物车角标
    updateCartBadge()
  } catch (e) {
    console.error('更新数量失败:', e)
  } finally {
    updating.value = false
  }
}

async function inc(i) {
  if (updating.value) {
    console.log('更新中，请稍后')
    return
  }
  
  updating.value = true
  const pid = i.productId || i.id
  const qty = Number(i.quantity || 1) + 1
  i.quantity = qty
  
  try {
    await cartUpdate(pid, qty)
    // 更新购物车角标
    updateCartBadge()
  } catch (e) {
    console.error('增加数量失败:', e)
    // 恢复原数量
    i.quantity = qty - 1
  } finally {
    updating.value = false
  }
}

async function dec(i) {
  if (updating.value) {
    console.log('更新中，请稍后')
    return
  }
  
  updating.value = true
  const pid = i.productId || i.id
  const oldQty = Number(i.quantity || 1)
  const qty = Math.max(1, oldQty - 1)
  i.quantity = qty
  
  try {
    await cartUpdate(pid, qty)
    // 更新购物车角标
    updateCartBadge()
  } catch (e) {
    console.error('减少数量失败:', e)
    // 恢复原数量
    i.quantity = oldQty
  } finally {
    updating.value = false
  }
}

onShow(() => {
  load()
})

// 开发环境暴露调试函数
if (typeof window !== 'undefined') {
  window.debugCartBadge = debugCartBadge
}
</script>

<style>
.page { padding-bottom: 160rpx; background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%); min-height: 100vh; box-sizing: border-box; }

/* 登录提示样式 */
.login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 160rpx 48rpx;
  min-height: 60vh;
}
.login-prompt .icon {
  margin-bottom: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-prompt .tip {
  font-size: 32rpx;
  color: #666;
  margin-bottom: 48rpx;
}
.login-prompt .login-btn {
  background: linear-gradient(135deg, #ffd93d, #ffcd00);
  color: #333;
  border-radius: 999rpx;
  padding: 0 64rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(255,205,0,.3);
}

.empty { padding: 120rpx 24rpx; text-align: center; color: #999; }
.list { padding: 24rpx; display: flex; flex-direction: column; gap: 16rpx; }
.row { background: #ffffff; border-radius: 20rpx; padding: 16rpx; display: flex; gap: 16rpx; align-items: center; box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.04), 0 1rpx 4rpx rgba(0,0,0,0.02); border: 1rpx solid rgba(0,0,0,0.03); transition: all 0.3s ease; }
.row.updating { opacity: 0.6; }
.row.deleting { opacity: 0.3; transform: translateX(-20rpx); }
.thumb { width: 140rpx; height: 140rpx; background: #e9eef3; border-radius: 12rpx; overflow: hidden; }
.thumb-img { width: 100%; height: 100%; border-radius: 12rpx; }
.meta { flex: 1; }
.title { font-size: 28rpx; color: #333; }
.price { color: #e54d42; font-weight: 700; margin-top: 4rpx; }
.remove { width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center; border-radius: 50%; background: #f5f5f5; color: #999; cursor: pointer; transition: all 0.2s ease; }
.remove:active { background: #e54d42; color: #fff; }
.remove.disabled { opacity: 0.4; pointer-events: none; }
.stepper { margin-top: 10rpx; display: flex; align-items: center; }
.s-btn { width: 54rpx; height: 54rpx; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 10rpx; font-size: 32rpx; color: #333; cursor: pointer; transition: all 0.2s ease; }
.s-btn:active { background: #ddd; }
.s-btn.disabled { opacity: 0.4; pointer-events: none; }
.ipt { width: 100rpx; margin: 0 10rpx; text-align: center; height: 54rpx; border: 2rpx solid #eee; border-radius: 10rpx; }
.ipt:disabled { background: #f9f9f9; color: #999; }

.bar { position: fixed; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.95); backdrop-filter: blur(20px); padding: 12rpx 24rpx calc(12rpx + env(safe-area-inset-bottom)); display: flex; justify-content: space-between; align-items: center; box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.06); }
.total { color: #333; font-size: 28rpx; }
.money { color: #e54d42; font-weight: 700; }
.checkout { background: linear-gradient(135deg, #ffd84c, #ffca28); color: #333; border-radius: 999rpx; padding: 0 28rpx; height: 72rpx; line-height: 72rpx; font-weight: 600; box-shadow: 0 4rpx 12rpx rgba(255,193,7,0.3); }
</style>
