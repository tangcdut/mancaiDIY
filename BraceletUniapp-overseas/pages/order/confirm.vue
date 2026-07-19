<template>
  <view class="page">
    <!-- 收货地址 -->
    <view class="section">
      <view class="section-title">Shipping Address / 收货地址</view>
      <view v-if="!selectedAddress" class="no-address" @click="goSelectAddress">
        <view class="icon-box"><Icon name="location" size="36rpx" color="#999" /></view>
        <text class="tip">Select Address / 请选择收货地址</text>
        <text class="arrow">›</text>
      </view>
      <view v-else class="address-card" @click="goSelectAddress">
        <view class="icon-box"><Icon name="location" size="36rpx" color="#999" /></view>
        <view class="addr-info">
          <view class="addr-header">
            <text class="name">{{ selectedAddress.name }}</text>
            <text class="phone">{{ selectedAddress.phone }}</text>
          </view>
          <view class="addr-detail">
            {{ selectedAddress.address || selectedAddress.detail }}, {{ selectedAddress.city }}{{ selectedAddress.state ? ', ' + selectedAddress.state : '' }} {{ selectedAddress.zip || '' }}, {{ selectedAddress.country || '' }}
          </view>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 联系邮箱 -->
    <view class="section">
      <view class="section-title">联系邮箱 / Email Address</view>
      <input class="email-input" type="text" v-model="email" placeholder="Please enter your email for order tracking" />
    </view>

    <!-- 商品列表 -->
    <view class="section">
      <view class="section-title">Items / 商品清单</view>
      
      <!-- DIY 模式显示单一汇总项 -->
      <view v-if="orderMode === 'diy'" class="goods-list">
        <view class="goods-item">
          <image class="goods-thumb" :src="designImage" mode="aspectFit" @click="previewDesign" />
          <view class="goods-info">
            <view class="goods-title">DIY Bracelet / DIY商品：{{ diyName || '未命名' }}</view>
            <view class="goods-meta">
              <text class="price">${{ totalAmount }}</text>
              <text class="qty">x1</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 普通模式显示商品列表 -->
      <view v-else class="goods-list">
        <view v-for="item in cartItems" :key="item.id" class="goods-item">
          <image v-if="item.imageUrl || item.image" class="goods-thumb" :src="item.imageUrl || item.image" mode="aspectFill" />
          <view v-else-if="item.color" class="goods-thumb" :style="{background: item.color}"></view>
          <view v-else class="goods-thumb"></view>
          <view class="goods-info">
            <view class="goods-title">{{ item.title }}</view>
            <view class="goods-meta">
              <text class="price">${{ item.price }}</text>
              <text class="qty">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- DIY信息 (仅DIY模式显示) -->
    <view class="section" v-if="orderMode === 'diy'">
      <view class="section-title">DIY Details / DIY信息</view>
      
      <view class="diy-info-row">
        <text class="label">Name / 作品名称</text>
        <input class="diy-input" type="text" v-model="diyName" placeholder="Please enter a name for your design" maxlength="20" />
      </view>
      
      <view class="diy-info-row design-row">
        <text class="label">Design / 设计图</text>
        <image :src="designImage" mode="aspectFit" class="small-preview-img" @click="previewDesign" />
      </view>
    </view>

    <!-- 订单备注 -->
    <view class="section">
      <view class="section-title">Order Notes / 订单备注</view>
      <input class="remark-input" type="text" v-model="remark" placeholder="Optional, notes for your order" />
    </view>

    <!-- 订单金额 -->
    <view class="section">
      <view class="amount-row">
        <text class="label">Subtotal / 商品金额</text>
        <text class="value">${{ totalAmount }}</text>
      </view>
      <view class="amount-row">
        <text class="label">Shipping / 运费</text>
        <text class="value">$0.00</text>
      </view>
      <view class="amount-row total">
        <text class="label">Total / 实付款</text>
        <text class="value">${{ totalAmount }}</text>
      </view>
    </view>

    <!-- 底部提交栏 -->
    <view class="submit-bar">
      <view class="total-info">
        <text class="label">Total / 合计：</text>
        <text class="amount">${{ totalAmount }}</text>
      </view>
      <view class="btn-group">
        <button class="action-btn submit-btn" :disabled="!selectedAddress || submitting" @click="submitOrder">
          {{ submitting ? 'Submitting... / 提交中...' : 'Place Order / 提交订单' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addressList, createDiyOrder, createGuestOrder, getGuestInfo, setGuestInfo } from '../../api/index.js'
import { resolveImageUrl } from '../../utils/imageHelper.js'
import Icon from '../../components/Icon.vue'

const cartItems = ref([])
const selectedAddress = ref(null)
const submitting = ref(false)
const orderMode = ref('cart') // cart or diy
const diyItems = ref([])
const designImage = ref('')
const remark = ref('')
const diyName = ref('')
const email = ref('')

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0).toFixed(2)
})

const classificationDetails = ref({})
const classificationDetailKeys = ref({})
const orderCategories = ref('')
const beadDescription = ref('')

onLoad(async (options) => {
  // 检查是否是DIY订单
  if (options.mode === 'diy') {
    orderMode.value = 'diy'
    try {
      // 优先尝试获取完整数据
      const data = uni.getStorageSync('diy_order_data')
      if (data && data.items) {
        diyItems.value = data.items
        cartItems.value = data.items
        designImage.value = data.designImage
        beadDescription.value = data.beadDescription || '' // 获取珠子描述
        // 获取分类详情 (1-8)
        for (let i = 1; i <= 8; i++) {
            if (data[`classificationDetail${i}`]) {
                classificationDetails.value[i] = data[`classificationDetail${i}`]
            }
            if (data[`classificationDetailKey${i}`]) {
                classificationDetailKeys.value[i] = data[`classificationDetailKey${i}`]
            }
        }
        orderCategories.value = data.categories || ''
      } else {
        // 兼容旧格式
        const items = uni.getStorageSync('diy_order_items')
        if (items && Array.isArray(items)) {
          diyItems.value = items
          cartItems.value = items
        }
      }
      
      // 如果传递了尺寸参数，自动填充到备注
      if (options.size) {
        remark.value = `Wrist size / 手链尺寸: ${options.size}cm`
      }
      
    } catch (e) {
      console.error('获取DIY订单数据失败', e)
    }
  } else if (options.mode === 'direct') {
    orderMode.value = 'direct'
    try {
      const item = uni.getStorageSync('direct_buy_item')
      if (item) {
        // 确保图片路径处理
        item.imageUrl = resolveImageUrl(item.imageUrl)
        item.image = resolveImageUrl(item.image)
        cartItems.value = [item]
      }
    } catch (e) {
      console.error('获取购买商品失败', e)
    }
  } else {
    await loadCartItems()
  }
  await loadDefaultAddress()
  email.value = uni.getStorageSync('guest_email') || ''
})

function previewDesign() {
  if (designImage.value) {
    uni.previewImage({ urls: [designImage.value] })
  }
}



// 加载购物车商品（海外版：从 localStorage 读取）
async function loadCartItems() {
  try {
    const stored = uni.getStorageSync('overseas_cart')
    const items = Array.isArray(stored) ? stored : []

    cartItems.value = items.map(item => {
      let imageUrl = item.imageUrl || item.image || ''
      return {
        ...item,
        imageUrl: resolveImageUrl(imageUrl),
        image: resolveImageUrl(imageUrl)
      }
    })

    if (cartItems.value.length === 0) {
      uni.showToast({ title: 'Cart is empty / 购物车为空', icon: 'none' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }
  } catch (e) {
    console.error('加载购物车失败:', e)
    uni.showToast({ title: 'Load failed / 加载失败', icon: 'none' })
  }
}

// 加载默认地址
async function loadDefaultAddress() {
  try {
    const res = await addressList()
    const addresses = Array.isArray(res) ? res : (res.data || [])
    // 查找默认地址
    selectedAddress.value = addresses.find(addr => addr.isDefault) || addresses[0] || null
  } catch (e) {
    console.error('加载地址失败:', e)
  }
}

// 选择地址
function goSelectAddress() {
  uni.navigateTo({ 
    url: '/pages/address/select',
    events: {
      // 监听地址选择事件
      selectAddress: (address) => {
        selectedAddress.value = address
      }
    }
  })
}

// 提交订单 (海外版：使用 guest order API)
async function submitOrder() {
  if (!selectedAddress.value) {
    uni.showToast({ title: 'Please select shipping address / 请选择收货地址', icon: 'none' })
    return
  }

  if (!email.value || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    uni.showToast({ title: 'Please enter a valid email / 请输入有效的联系邮箱', icon: 'none' })
    return
  }

  if (submitting.value) return

  submitting.value = true
  uni.showLoading({ title: 'Submitting... / 提交中...' })

  try {
    const addr = selectedAddress.value

    // Build order items
    const items = orderMode.value === 'diy'
      ? diyItems.value.map(item => ({
          productId: item.productId || item.id || 0,
          title: diyName.value || item.title || 'DIY Bracelet',
          price: item.price,
          quantity: item.quantity,
          image: item.imageUrl || designImage.value || ''
        }))
      : cartItems.value.map(item => ({
          productId: item.productId || item.id || 0,
          title: item.title || 'Bracelet',
          price: item.price,
          quantity: item.quantity || 1,
          image: item.imageUrl || item.image || ''
        }))

    // Build guest order DTO matching backend GuestOrderCreateDTO
    const orderData = {
      items: items,
      address: {
        name: addr.name || '',
        phone: addr.phone || '',
        address: addr.address || addr.detail || '',
        city: addr.city || '',
        state: addr.state || '',
        zip: addr.zip || '',
        country: addr.country || ''
      },
      amount: parseFloat(totalAmount.value),
      currency: 'USD',
      isOverseas: 1,
      remark: remark.value || (orderMode.value === 'diy' ? 'DIY Design Order' : ''),
      email: email.value.trim()
    }

    // Save guest info for order lookup
    setGuestInfo(addr.phone || addr.name || '', addr.name || '')

    const res = await createGuestOrder(orderData)

    uni.hideLoading()

    const orderNo = res.orderNo || res.order?.orderNo
    if (!orderNo) {
      console.error('Order created but no order number:', res)
      uni.showToast({ title: 'Order creation failed / 订单创建失败', icon: 'none' })
      return
    }

    // Store email and orderNo for tracking
    try {
      uni.setStorageSync('guest_email', email.value.trim())
      uni.setStorageSync('last_order_no', orderNo)
    } catch (e) {}

    uni.showToast({ title: 'Order Created! / 订单创建成功', icon: 'success' })

    // Navigate to order detail
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/order/detail?orderNo=' + orderNo })
    }, 1500)

  } catch (e) {
    uni.hideLoading()
    console.error('Create order failed:', e)
    uni.showToast({ title: e.message || 'Order failed / 创建订单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  padding-bottom: 140rpx;
}

.section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.03), 0 1rpx 4rpx rgba(0,0,0,0.02);
  border: 1rpx solid rgba(0,0,0,0.03);
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 24rpx;
}

/* 收货地址 */
.no-address, .address-card {
  display: flex;
  align-items: center;
  padding: 0;
  background: #fff;
}

.icon-box {
  width: 60rpx;
  height: 60rpx;
  background: #ffb84d;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.no-address .icon, .address-card .icon {
  font-size: 32rpx;
  color: #fff;
}

.no-address .tip {
  flex: 1;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.addr-info {
  flex: 1;
}

.addr-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 8rpx;
}

.addr-header .name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.addr-header .phone {
  font-size: 28rpx;
  color: #666;
}

.addr-detail {
  font-size: 26rpx;
  color: #999;
}

.arrow {
  font-size: 32rpx;
  color: #ccc;
  margin-left: 20rpx;
}

/* DIY 信息 */
.diy-info-row {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.diy-info-row:last-child {
  margin-bottom: 0;
}

.diy-info-row .label {
  width: 140rpx;
  font-size: 28rpx;
  color: #666;
}

.diy-input {
  flex: 1;
  height: 80rpx;
  background: #f9f9f9;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
}

.small-preview-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 8rpx;
  background: #f9f9f9;
}

/* 商品列表 */
.goods-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
}

.goods-thumb {
  width: 120rpx;
  height: 120rpx;
  background: #f0f0f0;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4rpx 0;
}

.goods-title {
  font-size: 28rpx;
  color: #333;
  line-height: 1.4;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.goods-meta .price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ff6b6b;
}

.goods-meta .qty {
  font-size: 24rpx;
  color: #999;
}

/* 备注输入框 */
.remark-input {
  width: 100%;
  height: 80rpx;
  background: #f9f9f9;
  border-radius: 8rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

/* 金额 */
.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  font-size: 28rpx;
}

.amount-row .label {
  color: #666;
}

.amount-row .value {
  color: #333;
}

.amount-row.total {
  border-top: 2rpx solid #f0f0f0;
  padding-top: 20rpx;
  margin-top: 10rpx;
}

.amount-row.total .label {
  font-size: 30rpx;
  font-weight: 700;
}

.amount-row.total .value {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b6b;
}

/* 提交栏 */
.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-top: 2rpx solid #f0f0f0;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.total-info {
  display: flex;
  align-items: baseline;
}

.total-info .label {
  font-size: 28rpx;
  color: #666;
}

.total-info .amount {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b6b;
}

.btn-group {
  display: flex;
  gap: 20rpx;
}

.action-btn {
  padding: 0 40rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 36rpx;
  border: none;
  margin: 0;
}

.cart-btn {
  background: #333;
  color: #fff;
}

.submit-btn {
  background: linear-gradient(135deg, #ffd84c, #ffca28);
  color: #333;
  box-shadow: 0 4rpx 12rpx rgba(255,193,7,0.3);
}

.submit-btn[disabled] {
  background: #ddd;
  color: #999;
}

.email-input {
  width: 100%;
  height: 80rpx;
  background: #f9f9f9;
  border-radius: 8rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}
</style>
