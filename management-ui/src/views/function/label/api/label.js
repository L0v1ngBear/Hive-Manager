import request from '@/utils/request'

export function listLabelTemplates(params = {}) {
  return request({
    url: '/label-template/list',
    method: 'get',
    params
  })
}

export function listLabelTemplateVariables(params = {}) {
  return request({
    url: '/label-template/variables',
    method: 'get',
    params
  })
}

export function saveLabelTemplate(data) {
  return request({
    url: '/label-template/save',
    method: 'post',
    data
  })
}

export function uploadLabelTemplate(data) {
  return request({
    url: '/label-template/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function setDefaultLabelTemplate(id) {
  return request({
    url: `/label-template/${id}/default`,
    method: 'post'
  })
}

export function deleteLabelTemplate(id) {
  return request({
    url: `/label-template/${id}`,
    method: 'delete'
  })
}
