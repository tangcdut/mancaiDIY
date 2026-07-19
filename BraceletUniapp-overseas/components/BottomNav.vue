<template>
  <view class="tabbar">
    <view class="item" :class="{ active: active === 'home' }" @click="go('home')">
      <view class="icon" />
      <text>首页</text>
    </view>
    <view class="item" :class="{ active: active === 'design' }" @click="go('design')">
      <view class="icon" />
      <text>设计图纸</text>
    </view>
    <view class="item" :class="{ active: active === 'cart' }" @click="go('cart')">
      <view class="icon" />
      <text>购物车</text>
    </view>
    <view class="item" :class="{ active: active === 'mine' }" @click="go('mine')">
      <view class="icon" />
      <text>我的</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({ active: { type: String, default: '' } })

function go(key) {
  if (key === 'home') {
    uni.switchTab({ url: '/pages/index/index' })
  } else if (key === 'design') {
    uni.switchTab({ url: '/pages/design/index' })
  } else if (key === 'cart') {
    uni.switchTab({ url: '/pages/cart/index' })
  } else if (key === 'mine') {
    uni.switchTab({ url: '/pages/mine/index' })
  }
}
</script>

<style>
.tabbar { position: fixed; left: 0; right: 0; bottom: 0; height: 110rpx; background: #ffffff; border-top: 1px solid #eee; display: flex; align-items: center; justify-content: space-around; padding-bottom: env(safe-area-inset-bottom); z-index: 999; }
.item { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #666; font-size: 22rpx; }
.item .icon { width: 46rpx; height: 46rpx; border-radius: 50%; background: #d8d8d8; margin-bottom: 6rpx; }
.item.active { color: #f5c93a; }
.item.active .icon { background: #f5c93a; }
</style>
