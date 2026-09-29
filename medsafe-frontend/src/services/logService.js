import { request } from './api';

export const logService = {
  markTaken(scheduleId, scheduledDatetime = null) {
    return request(`/api/logs/${scheduleId}/taken`, {
      method: 'POST',
      body: JSON.stringify(scheduledDatetime ? { scheduledDatetime } : {})
    });
  },

  markMissed(scheduleId, scheduledDatetime = null) {
    return request(`/api/logs/${scheduleId}/missed`, {
      method: 'POST',
      body: JSON.stringify(scheduledDatetime ? { scheduledDatetime } : {})
    });
  },

  getLogs() {
    return request('/api/logs');
  }
};

export default logService;
