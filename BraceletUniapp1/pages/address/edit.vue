<template>
  <view class="page">
    <view class="form">
      <view class="form-item">
        <view class="label">收货人</view>
        <input 
          class="input" 
          v-model="form.name" 
          placeholder="请输入收货人姓名"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <view class="label">手机号</view>
        <input 
          class="input" 
          v-model="form.phone" 
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item region">
        <view class="label">所在地区</view>
        <picker 
          mode="region" 
          :value="regionArray"
          @change="handleRegionPickerChange"
          class="region-selector"
        >
          <view class="input" :class="{ placeholder: !regionText }">
            {{ regionText || '请选择省市区' }}
          </view>
        </picker>
        <view class="arrow">></view>
      </view>

      <view class="form-item">
        <view class="label">详细地址</view>
        <textarea 
          class="textarea" 
          v-model="form.detail" 
          placeholder="如街道、楼栋号、门牌号等"
          placeholder-class="placeholder"
          maxlength="200"
        />
      </view>

      <view class="form-item switch-item">
        <view class="label">设为默认地址</view>
        <switch 
          :checked="form.isDefault" 
          @change="handleDefaultChange"
          color="#ffd84c"
        />
      </view>
    </view>

    <view class="btn-group">
      <button class="save-btn" @click="handleSave">保存</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addressDetail, addressAdd, addressUpdate } from '../../api/index.js'

const addressId = ref(null)
const isEdit = ref(false)

const form = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

// 地区数组（用于picker组件的初始值）
const regionArray = computed(() => {
  if (form.value.province && form.value.city && form.value.district) {
    return [form.value.province, form.value.city, form.value.district]
  }
  return []
})

const regionText = computed(() => {
  if (form.value.province && form.value.city && form.value.district) {
    return `${form.value.province} ${form.value.city} ${form.value.district}`
  }
  return ''
})

// 处理uni-app地区选择器的change事件
function handleRegionPickerChange(e) {
  const [province, city, district] = e.detail.value
  form.value.province = province
  form.value.city = city
  form.value.district = district
  console.log('选择的地区:', province, city, district)
}

// 处理默认地址开关
function handleDefaultChange(e) {
  form.value.isDefault = e.detail.value
}

// 表单验证
function validateForm() {
  if (!form.value.name || !form.value.name.trim()) {
    uni.showToast({ title: '请输入收货人姓名', icon: 'none' })
    return false
  }
  
  if (!form.value.phone) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return false
  }
  
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return false
  }
  
  if (!form.value.province || !form.value.city || !form.value.district) {
    uni.showToast({ title: '请选择所在地区', icon: 'none' })
    return false
  }
  
  if (!form.value.detail || !form.value.detail.trim()) {
    uni.showToast({ title: '请输入详细地址', icon: 'none' })
    return false
  }
  
  return true
}

// 保存地址
async function handleSave() {
  if (!validateForm()) {
    return
  }
  
  try {
    uni.showLoading({ title: '保存中...' })
    
    let res
    if (isEdit.value && addressId.value) {
      res = await addressUpdate(addressId.value, form.value)
    } else {
      res = await addressAdd(form.value)
    }
    
    uni.hideLoading()
    
    if (res && res.ok) {
      uni.showToast({ 
        title: isEdit.value ? '修改成功' : '添加成功', 
        icon: 'success' 
      })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    } else {
      uni.showToast({ 
        title: res.message || '保存失败', 
        icon: 'none' 
      })
    }
  } catch (e) {
    uni.hideLoading()
    console.error('保存地址失败', e)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

// 加载地址详情
async function loadAddress(id) {
  try {
    uni.showLoading({ title: '加载中...' })
    const res = await addressDetail(id)
    uni.hideLoading()
    
    if (res) {
      form.value = {
        name: res.name || '',
        phone: res.phone || '',
        province: res.province || '',
        city: res.city || '',
        district: res.district || '',
        detail: res.detail || '',
        isDefault: res.isDefault === 1
      }
    } else {
      uni.showToast({ title: '地址不存在', icon: 'none' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }
  } catch (e) {
    uni.hideLoading()
    console.error('加载地址失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

onLoad((options) => {
  if (options && options.id) {
    addressId.value = options.id
    isEdit.value = true
    uni.setNavigationBarTitle({ title: '编辑地址' })
    loadAddress(options.id)
  } else {
    uni.setNavigationBarTitle({ title: '添加地址' })
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f3f0eb 100%);
  padding: 24rpx;
}

.form {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.form-item.region {
  position: relative;
}

.form-item.switch-item {
  justify-content: space-between;
}

.label {
  width: 160rpx;
  font-size: 28rpx;
  color: #333;
  flex-shrink: 0;
}

.input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.input.placeholder,
.placeholder {
  color: #ccc;
}

.textarea {
  flex: 1;
  min-height: 120rpx;
  font-size: 28rpx;
  color: #333;
}

.region-selector {
  flex: 1;
  display: flex;
}

.arrow {
  font-size: 28rpx;
  color: #ccc;
  margin-left: 16rpx;
}

.btn-group {
  margin-top: 48rpx;
}

.save-btn {
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
