import { request } from './api';

export const scheduleService = {
  getByMedicationId(medicationId) {
    return request(`/api/medications/${medicationId}/schedules`);
  },

  create(medicationId, scheduleData) {
    return request(`/api/medications/${medicationId}/schedules`, {
      method: 'POST',
      body: JSON.stringify(scheduleData)
    });
  },

  update(scheduleId, scheduleData) {
    return request(`/api/schedules/${scheduleId}`, {
      method: 'PUT',
      body: JSON.stringify(scheduleData)
    });
  },

  delete(scheduleId) {
    return request(`/api/schedules/${scheduleId}`, {
      method: 'DELETE'
    });
  }
};

export default scheduleService;
