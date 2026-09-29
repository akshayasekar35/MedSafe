import { request } from './api';

export const medicationService = {
  getAll() {
    return request('/api/medications');
  },

  getById(id) {
    return request(`/api/medications/${id}`);
  },

  create(medicationData) {
    return request('/api/medications', {
      method: 'POST',
      body: JSON.stringify(medicationData)
    });
  },

  update(id, medicationData) {
    return request(`/api/medications/${id}`, {
      method: 'PUT',
      body: JSON.stringify(medicationData)
    });
  },

  delete(id) {
    return request(`/api/medications/${id}`, {
      method: 'DELETE'
    });
  },

  getDuplicates() {
    return request('/api/medications/duplicates');
  }
};

export default medicationService;
