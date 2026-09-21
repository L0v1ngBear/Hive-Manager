import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@fontsource/material-symbols-outlined/400.css'
import 'element-plus/dist/index.css'
import './style.css'
import permissionDirective from '@/directives/permission'
import filterCollapseDirective from '@/directives/filterCollapse'
import { installElementPlusFoundation } from './plugins/elementPlus'
import { ElMessage } from 'element-plus'
import { startReleaseUpdates } from './utils/releaseUpdates'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.directive('permission', permissionDirective)
app.directive('filter-collapse', filterCollapseDirective)
installElementPlusFoundation(app)

app.mount('#app')

if (import.meta.env.PROD) {
  const updates = startReleaseUpdates({
    buildId: import.meta.env.VITE_HIVE_BUILD_ID,
    baseUrl: import.meta.env.BASE_URL,
    notify: message => ElMessage({ message, type: 'warning', duration: 10_000 }),
  })
  void updates.check()
}
