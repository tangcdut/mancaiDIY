<template>
  <view class="customer-service-page">
    <view class="header">
      <text class="title">联系客服</text>
    </view>

    <view class="content">
      <view class="qr-container" v-if="qrCodeUrl">
        <image 
          class="qr-code" 
          :src="qrCodeUrl" 
          mode="aspectFit"
          show-menu-by-longpress="true"
          @error="onImageError"
        ></image>
        <text class="tip">长按识别二维码添加客服微信</text>
      </view>

      <view class="empty" v-else-if="!loading">
        <text class="empty-text">客服二维码暂未配置</text>
        <text class="empty-tip">请联系管理员设置客服二维码</text>
      </view>

      <view class="loading" v-if="loading">
        <text>加载中...</text>
      </view>
    </view>

    <view class="tips">
      <view class="tip-item">
        <Icon name="phone" size="36rpx" color="#666" />
        <text class="tip-text">扫描二维码添加客服微信</text>
      </view>
      <view class="tip-item">
        <Icon name="chat" size="36rpx" color="#666" />
        <text class="tip-text">获取专属购物建议和售后服务</text>
      </view>
      <view class="tip-item">
        <Icon name="orderPending" size="36rpx" color="#666" />
        <text class="tip-text">工作时间：9:00-22:00</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCustomerServiceQRCode } from '../../api/api.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import Icon from '../../components/Icon.vue'

const loading = ref(true)
const qrCodeUrl = ref('')

onMounted(() => {
  loadQRCode()
})

async function loadQRCode() {
  loading.value = true
  try {
    const res = await getCustomerServiceQRCode()
    console.log('客服二维码响应:', res)
    
    if (res) {
      qrCodeUrl.value = resolveImageUrl(res)
      console.log('客服二维码URL:', qrCodeUrl.value)
    } else {
      console.log('未获取到客服二维码')
      qrCodeUrl.value = ''
    }
  } catch (error) {
    console.error('获取客服二维码失败:', error)
    uni.showToast({
      title: '获取客服信息失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function onImageError(e) {
  console.error('图片加载失败:', e)
  uni.showToast({
    title: '二维码加载失败',
    icon: 'none'
  })
}
</script>

<style scoped>
.customer-service-page {
  min-height: 100vh;
  background-color: #f7f7f7;
  padding: 30rpx;
  box-sizing: border-box;
}

.header {
  margin-bottom: 40rpx;
  text-align: center;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.content {
  background: #fff;
  border-radius: 20rpx;
  padding: 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 500rpx;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
  margin-bottom: 40rpx;
}

.qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-code {
  width: 400rpx;
  height: 400rpx;
  margin-bottom: 30rpx;
}

.tip {
  font-size: 28rpx;
  color: #666;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #333;
  font-weight: 500;
}

.empty-tip {
  font-size: 26rpx;
  color: #999;
}

.loading {
  color: #999;
  font-size: 28rpx;
}

.tips {
  padding: 0 20rpx;
}

.tip-item {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  background: #fff;
  padding: 24rpx;
  border-radius: 12rpx;
}

.tip-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.tip-text {
  font-size: 28rpx;
  color: #555;
}
</style>
