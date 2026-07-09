import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@fontsource/material-symbols-outlined/400.css'
import 'element-plus/dist/index.css'
import './style.css'
import permissionDirective from '@/directives/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.directive('permission', permissionDirective)

app.mount('#app')
