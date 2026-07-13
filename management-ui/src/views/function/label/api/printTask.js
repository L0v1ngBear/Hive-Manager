import request from '@/utils/request'

export function listPendingPrintTasks(params = {}) {
  return request({
    url: '/print-task/pending',
    method: 'get',
    params
  })
}

export function getPendingPrintTaskCount(printTypes = []) {
  return request({
    url: '/print-task/pending-count',
    method: 'get',
    params: {
      printTypes: printTypes.join(',')
    }
  })
}

export function reportPrintTask(data) {
  return request({
    url: '/print-task/report',
    method: 'post',
    data
  })
}
