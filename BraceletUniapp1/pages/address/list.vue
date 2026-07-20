<template>
  <view class="page">
    <!-- 空状态 -->
    <view v-if="!addresses || addresses.length === 0" class="empty">
      <view class="empty-icon"><Icon name="location" size="80rpx" color="#ccc" /></view>
      <view class="empty-text">还没有收货地址</view>
      <view class="empty-tips">添加后可快速填写订单信息</view>
    </view>

    <!-- 地址列表 -->
    <view v-else class="address-list">
      <view 
        v-for="addr in addresses" 
        :key="addr.id" 
        class="addr-item"
        @click="handleEdit(addr.id)"
      >
        <view class="addr-header">
          <view class="addr-name">{{ addr.name }}</view>
          <view class="addr-phone">{{ addr.phone }}</view>
          <view v-if="addr.isDefault" class="default-tag">默认</view>
        </view>
        <view class="addr-detail">
          {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}
        </view>
        <view class="addr-actions">
          <button 
            class="action-btn" 
            @click.stop="handleSetDefault(addr.id)"
            v-if="!addr.isDefault"
          >
            设为默认
          </button>
          <button class="action-btn edit" @click.stop="handleEdit(addr.id)">
            编辑
          </button>
          <button class="action-btn delete" @click.stop="handleDelete(addr.id)">
            删除
          </button>
        </view>
      </view>
    </view>

    <!-- 底部添加按钮 -->
    <view class="bottom-bar">
      <button class="add-btn" @click="handleAdd">+ 添加新地址</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { addressList, addressDelete, addressSetDefault } from '../../api/index.js'
import Icon from '../../components/Icon.vue'

const addresses = ref([])

// 加载地址列表
async function loadAddresses() {
  try {
    const res = await addressList()
    addresses.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    console.error('加载地址失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

// 添加地址
function handleAdd() {
  uni.navigateTo({ url: '/pages/address/edit' })
}

// 编辑地址
function handleEdit(id) {
  uni.navigateTo({ url: `/pages/address/edit?id=${id}` })
}

// 设为默认
async function handleSetDefault(id) {
  try {
    uni.showLoading({ title: '设置中...' })
    const res = await addressSetDefault(id)
    uni.hideLoading()
    
    if (res && res.ok) {
      uni.showToast({ title: '设置成功', icon: 'success' })
      loadAddresses()
    } else {
      uni.showToast({ title: res.message || '设置失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '设置失败', icon: 'none' })
  }
}

// 删除地址
function handleDelete(id) {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个地址吗？',
    confirmColor: '#e54d42',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '删除中...' })
          const result = await addressDelete(id)
          uni.hideLoading()
          
          if (result && result.ok) {
            uni.showToast({ title: '删除成功', icon: 'success' })
            loadAddresses()
          } else {
            uni.showToast({ title: result.message || '删除失败', icon: 'none' })
          }
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onMounted(loadAddresses)
onShow(loadAddresses)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  padding-bottom: 120rpx;
}

.empty {
  padding: 120rpx 24rpx;
  text-align: center;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #333;
  margin-bottom: 12rpx;
}

.empty-tips {
  font-size: 26rpx;
  color: #999;
}

.address-list {
  padding: 24rpx;
}

.addr-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.04);
}

.addr-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.addr-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  margin-right: 16rpx;
}

.addr-phone {
  font-size: 28rpx;
  color: #666;
  margin-right: auto;
}

.default-tag {
  background: #ffd84c;
  color: #333;
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-weight: 600;
}

.addr-detail {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16rpx;
}

.addr-actions {
  display: flex;
  gap: 16rpx;
  justify-content: flex-end;
}

.action-btn {
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  font-size: 24rpx;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 8rpx;
}

.action-btn.edit {
  background: #e3f2fd;
  color: #2196f3;
}

.action-btn.delete {
  background: #ffebee;
  color: #e54d42;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 16rpx 24rpx;
  box-shadow: 0 -4rpx 12rpx rgba(0, 0, 0, 0.04);
}

.add-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #ffd84c;
  color: #333;
  font-size: 32rpx;
  font-weight: 700;
  border-radius: 16rpx;
  border: none;
}
</style>
