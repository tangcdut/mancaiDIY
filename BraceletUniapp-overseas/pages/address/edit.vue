<template>
  <view class="page">
    <view class="form">
      <view class="form-item">
        <view class="label">Full Name / 姓名</view>
        <input
          class="input"
          v-model="form.name"
          placeholder="Enter full name / 请输入姓名"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <view class="label">Phone / 电话</view>
        <input
          class="input"
          v-model="form.phone"
          type="text"
          placeholder="Enter phone number / 请输入电话"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <view class="label">Country / 国家</view>
        <input
          class="input"
          v-model="form.country"
          placeholder="e.g. United States / 美国"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <view class="label">State/Province / 州/省</view>
        <input
          class="input"
          v-model="form.state"
          placeholder="e.g. California / 加利福尼亚"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <view class="label">City / 城市</view>
        <input
          class="input"
          v-model="form.city"
          placeholder="e.g. Los Angeles"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <view class="label">ZIP Code / 邮编</view>
        <input
          class="input"
          v-model="form.zip"
          type="text"
          placeholder="Enter ZIP code / 请输入邮编"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item textarea-item">
        <view class="label">Street Address / 详细地址</view>
        <textarea
          class="textarea"
          v-model="form.address"
          placeholder="Street, building, apartment no. / 街道、楼栋、门牌号等"
          placeholder-class="placeholder"
          maxlength="200"
        />
      </view>

      <view class="form-item switch-item">
        <view class="label">Set as Default / 设为默认</view>
        <switch
          :checked="form.isDefault"
          @change="handleDefaultChange"
          color="#ffd84c"
        />
      </view>
    </view>

    <view class="btn-group">
      <button class="save-btn" @click="handleSave">Save / 保存</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addressDetail, addressAdd, addressUpdate } from '../../api/index.js'

const addressId = ref(null)
const isEdit = ref(false)

const form = ref({
  name: '',
  phone: '',
  country: '',
  state: '',
  city: '',
  zip: '',
  address: '',
  isDefault: false
})

function handleDefaultChange(e) {
  form.value.isDefault = e.detail.value
}

function validateForm() {
  if (!form.value.name || !form.value.name.trim()) {
    uni.showToast({ title: 'Please enter your name / 请输入姓名', icon: 'none' })
    return false
  }

  if (!form.value.phone) {
    uni.showToast({ title: 'Please enter phone / 请输入电话', icon: 'none' })
    return false
  }

  if (!form.value.country || !form.value.country.trim()) {
    uni.showToast({ title: 'Please enter country / 请输入国家', icon: 'none' })
    return false
  }

  if (!form.value.city || !form.value.city.trim()) {
    uni.showToast({ title: 'Please enter city / 请输入城市', icon: 'none' })
    return false
  }

  if (!form.value.address || !form.value.address.trim()) {
    uni.showToast({ title: 'Please enter street address / 请输入详细地址', icon: 'none' })
    return false
  }

  return true
}

async function handleSave() {
  if (!validateForm()) return

  try {
    uni.showLoading({ title: 'Saving... / 保存中...' })

    let res
    if (isEdit.value && addressId.value) {
      res = await addressUpdate(addressId.value, form.value)
    } else {
      res = await addressAdd(form.value)
    }

    uni.hideLoading()

    if (res && res.ok) {
      uni.showToast({
        title: isEdit.value ? 'Updated / 已更新' : 'Added / 已添加',
        icon: 'success'
      })
      setTimeout(() => { uni.navigateBack() }, 1500)
    } else {
      uni.showToast({ title: 'Save failed / 保存失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    console.error('Save address failed', e)
    uni.showToast({ title: 'Save failed / 保存失败', icon: 'none' })
  }
}

async function loadAddress(id) {
  try {
    uni.showLoading({ title: 'Loading... / 加载中...' })
    const res = await addressDetail(id)
    uni.hideLoading()

    if (res) {
      form.value = {
        name: res.name || '',
        phone: res.phone || '',
        country: res.country || '',
        state: res.state || '',
        city: res.city || '',
        zip: res.zip || '',
        address: res.address || res.detail || '',
        isDefault: res.isDefault === 1
      }
    }
  } catch (e) {
    uni.hideLoading()
    console.error('Load address failed', e)
  }
}

onLoad((options) => {
  if (options && options.id) {
    addressId.value = options.id
    isEdit.value = true
    uni.setNavigationBarTitle({ title: 'Edit Address / 编辑地址' })
    loadAddress(options.id)
  } else {
    uni.setNavigationBarTitle({ title: 'Add Address / 添加地址' })
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

.form-item.switch-item {
  justify-content: space-between;
}

.form-item.textarea-item {
  align-items: flex-start;
}

.form-item.textarea-item .label {
  padding-top: 6rpx; /* Align label with the first line of text in textarea */
}

.label {
  width: 280rpx;
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
  padding: 0;
  margin: 0;
  box-sizing: border-box;
  line-height: 1.4;
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
