import { createApp } from 'vue';
import App from './App.vue';
import { createPinia } from 'pinia' // 如果你用了 Pinia
import router from './router' // 引入路由实例
import './style.css' // Tailwind CSS 的全局样式

const app = createApp(App)

app.use(createPinia())
app.use(router) // 👈 挂载路由实例

app.mount('#app')
