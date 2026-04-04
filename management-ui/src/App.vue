<template>
  <el-config-provider :locale="zhCn">

    <router-view />

  </el-config-provider>
</template>

<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
// 引入 Element Plus 的官方中文语言包
import zhCn from 'element-plus/es/locale/lang/zh-cn'

defineOptions({ name: 'App' })
</script>

<style>
/* 注意：由于我们使用了 Tailwind v4 的全局 style.css
  且没有特殊的定制要求，这里的 style 标签内可以直接留空。
  Tailwind 已经帮我们在 style.css 里面做了 html, body 100% 的重置。
*/
</style>
