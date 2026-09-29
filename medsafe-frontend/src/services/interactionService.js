import { request } from './api';

export const interactionService = {
  checkInteractions(medicationIds = null) {
    return request('/api/interactions/check', {
      method: 'POST',
      body: JSON.stringify(medicationIds ? { medicationIds } : {})
    });
  },

  getAlerts() {
    return request('/api/interactions');
  },

  getAlertById(id) {
    return request(`/api/interactions/${id}`);
  },

  explainWithAI(drugA, drugB, severity, verifiedInformation, source, evidenceStatus = 'VERIFIED') {
    return request('/api/ai/explain-interaction', {
      method: 'POST',
      body: JSON.stringify({
        drugA,
        drugB,
        severity,
        verifiedInformation,
        source,
        evidenceStatus
      })
    });
  }
};

export default interactionService;
