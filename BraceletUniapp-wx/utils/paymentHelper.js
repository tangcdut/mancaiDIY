import { orderPay, checkPaymentStatus } from '../api/index.js'

/**
 * 统一支付处理逻辑
 * @param {String|Object} order 订单号或订单对象
 * @param {Function} onSuccess 支付成功回调
 * @param {Function} onFail 支付失败回调
 */
export async function handleOrderPayment(order, onSuccess, onFail) {
  // Extract orderNo
  let orderNo
  if (typeof order === 'object' && order !== null) {
    orderNo = order.orderNo || order.order_no || order.id || order.orderId
  } else {
    orderNo = order
  }
  
  // Ensure orderNo is string
  orderNo = String(orderNo)

  if (!orderNo || orderNo === 'undefined' || orderNo === 'null') {
    uni.showToast({ title: '订单号无效', icon: 'none' })
    if (onFail) onFail(new Error('订单号无效'))
    return
  }

  uni.showLoading({ title: '正在调起支付...', mask: true })

  try {
    // 1. Get payment params from backend
    // Hardcode payMethod=1 for WeChat Pay as per current implementation
    console.log('准备发起支付，订单号:', orderNo)
    const paymentRes = await orderPay(orderNo, 1)
    console.log('支付API返回原始数据:', JSON.stringify(paymentRes))
    
    const paymentData = paymentRes.data || paymentRes
    console.log('解析后的支付参数:', paymentData)
    
    // Check if paymentData is valid
    if (!paymentData || (!paymentData.timeStamp && !paymentData.paySign)) {
      console.error('支付参数缺失:', paymentData)
      throw new Error('获取支付参数失败: ' + (JSON.stringify(paymentData) || '空数据'))
    }

    uni.hideLoading()

    // 2. Call WeChat Payment
    await new Promise((resolve, reject) => {
      uni.requestPayment({
        timeStamp: paymentData.timeStamp,
        nonceStr: paymentData.nonceStr,
        package: paymentData.packageStr || paymentData.package,
        signType: paymentData.signType || 'RSA',
        paySign: paymentData.paySign,
        success: (res) => {
          console.log('支付成功：', res)
          resolve(res)
        },
        fail: (err) => {
          console.log('支付失败：', err)
          reject(err)
        }
      })
    })

    uni.showToast({ title: '支付成功', icon: 'success' })

    // Double check status (optional but recommended)
    try {
      await checkPaymentStatus(orderNo)
    } catch (e) {
      console.warn('支付状态查询忽略错误', e)
    }

    if (onSuccess) onSuccess()

  } catch (error) {
    uni.hideLoading()
    console.error('支付流程错误：', error)
    
    const errorMsg = error.errMsg || error.message || '支付失败'
    
    // Handle user cancellation specifically
    if (errorMsg.includes('cancel') || errorMsg.includes('取消')) {
      uni.showToast({ title: '已取消支付', icon: 'none' })
    } else {
      uni.showModal({
        title: '支付失败',
        content: errorMsg + '，是否重试？',
        confirmText: '重试',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            handleOrderPayment(orderNo, onSuccess, onFail)
          }
        }
      })
    }
    
    if (onFail) onFail(error)
  }
}
