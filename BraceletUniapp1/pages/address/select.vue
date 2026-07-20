<template>
  <view class="page">
    <!-- 地址列表 -->
    <view v-if="addresses && addresses.length > 0" class="address-list">
      <view 
        v-for="addr in addresses" 
        :key="addr.id" 
        class="addr-item"
        :class="{ selected: selectedId === addr.id }"
        @click="selectAddress(addr)"
      >
        <view class="addr-header">
          <view class="addr-name">{{ addr.name }}</view>
          <view class="addr-phone">{{ addr.phone }}</view>
          <view v-if="addr.isDefault" class="default-tag">默认</view>
        </view>
        <view class="addr-detail">
          {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}
        </view>
        <view class="check-icon" v-if="selectedId === addr.id"><Icon name="check" size="32rpx" color="#fff" /></view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty">
      <view class="empty-icon"><Icon name="location" size="80rpx" color="#ccc" /></view>
      <view class="empty-text">还没有收货地址</view>
      <button class="add-btn" @click="goAddAddress">添加新地址</button>
    </view>

    <!-- 底部添加按钮 -->
    <view v-if="addresses && addresses.length > 0" class="bottom-bar">
      <button class="add-btn" @click="goAddAddress">+ 添加新地址</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addressList } from '../../api/index.js'
import Icon from '../../components/Icon.vue'

const addresses = ref([])
const selectedId = ref(null)
const eventChannel = ref(null)

onLoad(() => {
  // 获取事件通道
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  eventChannel.value = currentPage.getOpenerEventChannel()
  
  loadAddresses()
})

// 加载地址列表
async function loadAddresses() {
  try {
    const res = await addressList()
    addresses.value = Array.isArray(res) ? res : (res.data || [])
    
    // 默认选中第一个或默认地址
    if (addresses.value.length > 0) {
      const defaultAddr = addresses.value.find(addr => addr.isDefault)
      selectedId.value = defaultAddr ? defaultAddr.id : addresses.value[0].id
    }
  } catch (e) {
    console.error('加载地址失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

// 选择地址
function selectAddress(addr) {
  selectedId.value = addr.id
  
  // 触发选择事件，传递地址数据
  if (eventChannel.value) {
    eventChannel.value.emit('selectAddress', addr)
  }
  
  // 延迟返回，让用户看到选中效果
  setTimeout(() => {
    uni.navigateBack()
  }, 300)
}

// 添加新地址
function goAddAddress() {
  uni.navigateTo({ url: '/pages/address/edit' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  padding-bottom: 140rpx;
}

.address-list {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.addr-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 24rpx;
  position: relative;
  border: 2rpx solid transparent;
  transition: all 0.3s;
}

.addr-item.selected {
  border-color: #ffd84c;
  background: #fffef5;
}

.addr-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 12rpx;
}

.addr-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.addr-phone {
  font-size: 28rpx;
  color: #666;
}

.default-tag {
  padding: 4rpx 12rpx;
  background: #ffd84c;
  color: #333;
  font-size: 22rpx;
  border-radius: 8rpx;
  font-weight: 600;
}

.addr-detail {
  font-size: 26rpx;
  color: #999;
  line-height: 1.6;
  padding-right: 60rpx;
}

.check-icon {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #ffd84c;
  color: #333;
  font-size: 32rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 24rpx;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 48rpx;
}

.empty .add-btn {
  width: 400rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #ffd84c, #ffb84d);
  color: #333;
  font-size: 28rpx;
  font-weight: 700;
  border-radius: 40rpx;
  border: none;
}

/* 底部添加按钮 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24rpx;
  background: #fff;
  border-top: 2rpx solid #f0f0f0;
}

.bottom-bar .add-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #ffd84c, #ffb84d);
  color: #333;
  font-size: 28rpx;
  font-weight: 700;
  border-radius: 16rpx;
  border: none;
}
</style>
