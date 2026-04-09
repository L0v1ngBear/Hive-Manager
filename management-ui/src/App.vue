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

</style>
