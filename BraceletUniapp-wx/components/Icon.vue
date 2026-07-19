<template>
  <image
    v-if="src"
    :src="src"
    :style="iconStyle"
    mode="aspectFit"
    class="icon-svg"
  />
</template>

<script setup>
import { computed } from 'vue'
import * as icons from '../utils/iconPaths.js'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [String, Number], default: '48rpx' },
  color: { type: String, default: '' }
})

const src = computed(() => {
  const fn = icons[props.name]
  if (typeof fn === 'function') {
    return fn(props.color || undefined)
  }
  console.warn(`Icon "${props.name}" not found`)
  return ''
})

const iconStyle = computed(() => {
  const size = typeof props.size === 'number' ? `${props.size}rpx` : props.size
  return {
    width: size,
    height: size,
    display: 'inline-block',
    verticalAlign: 'middle',
    flexShrink: 0
  }
})
</script>

<style scoped>
.icon-svg {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>
