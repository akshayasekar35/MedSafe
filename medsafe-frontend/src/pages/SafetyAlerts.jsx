import React, { useState, useEffect } from 'react';
import { interactionService } from '../services/interactionService';
import { medicationService } from '../services/medicationService';
import SafetyAlert from '../components/SafetyAlert';
import { 
  ShieldAlert, 
  ShieldCheck, 
  Sparkles, 
  RefreshCw, 
  AlertCircle, 
  CheckCircle2,
  Info
} from 'lucide-react';

export default function SafetyAlerts() {
  const [alerts, setAlerts] = useState([]);
  const [medications, setMedications] = useState([]);
  const [selectedMedIds, setSelectedMedIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);
  const [checkResult, setCheckResult] = useState(null);
  const [error, setError] = useState(null);

  // AI Modal
  const [aiModalAlert, setAiModalAlert] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResponse, setAiResponse] = useState(null);
  const [aiError, setAiError] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [alertsData, medsData] = await Promise.all([
        interactionService.getAlerts(),
        medicationService.getAll()
      ]);
      setAlerts(alertsData);
      setMedications(medsData.filter(m => m.status === 'ACTIVE'));
    } catch (err) {
      setError(err.message || 'Failed to load safety alerts.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRunCheck = async () => {
    try {
      setChecking(true);
      setCheckResult(null);

      const payload = selectedMedIds.length > 0 ? selectedMedIds : null;
      const result = await interactionService.checkInteractions(payload);
      setCheckResult(result);

      // Refresh alerts list
      const updatedAlerts = await interactionService.getAlerts();
      setAlerts(updatedAlerts);
    } catch (err) {
      alert(err.message || 'Interaction check failed.');
    } finally {
      setChecking(false);
    }
  };

  const toggleMedSelection = (medId) => {
    setSelectedMedIds(prev =>
      prev.includes(medId) ? prev.filter(id => id !== medId) : [...prev, medId]
    );
  };

  const handleExplainAI = async (alert) => {
    setAiModalAlert(alert);
    setAiResponse(null);
    setAiError(null);
    setAiLoading(true);

    try {
      const result = await interactionService.explainWithAI(
        alert.medicineName1,
        alert.medicineName2,
        alert.severity,
        alert.description,
        alert.sourceName,
        alert.evidenceStatus || 'VERIFIED'
      );
      setAiResponse(result);
    } catch (err) {
      setAiError(err.message || 'Failed to generate AI explanation.');
    } finally {
      setAiLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="state-container">
        <RefreshCw size={36} className="animate-spin" style={{ color: 'var(--primary)', marginBottom: '1rem' }} />
        <h3 className="state-title">Checking clinical interaction databases...</h3>
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a' }}>
          Medication Interaction &amp; Safety Alerts
        </h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Screening pairwise interactions against clinical reference sources.
        </p>
      </div>

      {/* Safety Notice Banner */}
      <div className="alert-box info" style={{ marginBottom: '1.75rem' }}>
        <Info size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
        <div style={{ fontSize: '0.875rem', lineHeight: '1.5' }}>
          <strong>Healthcare Safety Standard: </strong>
          The absence of a documented interaction in the consulted reference source does not guarantee the complete absence of all health risks.
          Always consult a qualified healthcare professional or pharmacist before altering any prescribed medication.
        </div>
      </div>

      {/* Interaction Checker Tool Card */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <div className="card-header">
          <div>
            <h3 className="card-title">Run Interaction Check</h3>
            <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Check for documented interactions across your active medications
            </p>
          </div>

          <button
            onClick={handleRunCheck}
            disabled={checking || medications.length < 2}
            className="btn btn-primary btn-sm"
          >
            <ShieldAlert size={16} />
            <span>{checking ? 'Checking Reference Database...' : 'Run Interaction Check'}</span>
          </button>
        </div>

        {medications.length < 2 ? (
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            You need at least 2 active medications to perform a pairwise drug interaction check.
          </p>
        ) : (
          <div>
            <p style={{ fontSize: '0.85rem', color: '#334155', marginBottom: '0.75rem', fontWeight: 500 }}>
              Select specific medications to check (or leave all unselected to check all {medications.length} active prescriptions):
            </p>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '1rem' }}>
              {medications.map(med => {
                const isSelected = selectedMedIds.includes(med.medicationId);
                return (
                  <button
                    key={med.medicationId}
                    type="button"
                    onClick={() => toggleMedSelection(med.medicationId)}
                    style={{
                      padding: '0.4rem 0.85rem',
                      borderRadius: '9999px',
                      fontSize: '0.85rem',
                      fontWeight: 600,
                      border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border-color)',
                      backgroundColor: isSelected ? 'var(--primary-light)' : '#ffffff',
                      color: isSelected ? 'var(--primary-dark)' : '#475569'
                    }}
                  >
                    {isSelected ? '✓ ' : '+ '}{med.medicineName} ({med.activeIngredient || 'Generic'})
                  </button>
                );
              })}
            </div>

            {checkResult && (
              <div 
                style={{ 
                  padding: '1rem', 
                  borderRadius: 'var(--radius-sm)', 
                  backgroundColor: checkResult.totalInteractionsFound > 0 ? '#fef2f2' : '#f0fdf4',
                  border: `1px solid ${checkResult.totalInteractionsFound > 0 ? '#fecaca' : '#bbf7d0'}`,
                  fontSize: '0.9rem',
                  color: checkResult.totalInteractionsFound > 0 ? '#991b1b' : '#166534'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                  {checkResult.totalInteractionsFound > 0 ? <AlertCircle size={18} /> : <CheckCircle2 size={18} />}
                  <span>{checkResult.safetySummary}</span>
                </div>
                <p style={{ fontSize: '0.8rem', opacity: 0.9 }}>{checkResult.disclaimer}</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Verified Active Alerts List */}
      <div>
        <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
          Documented Safety Alerts ({alerts.length})
        </h3>

        {alerts.length === 0 ? (
          <div className="state-container">
            <ShieldCheck size={44} style={{ color: '#16a34a', marginBottom: '0.75rem' }} />
            <h3 className="state-title">No Documented Interactions Found</h3>
            <p className="state-description">
              No documented drug-to-drug interactions were identified between your prescribed medications in the consulted clinical reference source.
            </p>
          </div>
        ) : (
          <div>
            {alerts.map((alert) => (
              <SafetyAlert
                key={alert.alertId}
                alert={alert}
                onExplainAI={handleExplainAI}
              />
            ))}
          </div>
        )}
      </div>

      {/* AI Explanation Modal */}
      {aiModalAlert && (
        <div className="modal-overlay" onClick={() => setAiModalAlert(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                <div style={{ width: 34, height: 34, borderRadius: 8, backgroundColor: '#f5f3ff', color: '#7c3aed', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Sparkles size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a' }}>
                    AI Explanation
                  </h3>
                  <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    Evidence-grounded explanation based on verified medical data
                  </p>
                </div>
              </div>
              <button onClick={() => setAiModalAlert(null)} className="btn-icon" style={{ color: 'var(--text-muted)' }}>
                ✕
              </button>
            </div>

            {aiLoading ? (
              <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
                <RefreshCw size={28} className="animate-spin" style={{ color: '#7c3aed', marginBottom: '0.75rem' }} />
                <p style={{ fontWeight: 600, color: '#475569' }}>Translating clinical findings into plain language...</p>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Strictly grounded in verified source information.</p>
              </div>
            ) : aiError ? (
              <div className="alert-box danger">
                <AlertCircle size={18} />
                <span>{aiError}</span>
              </div>
            ) : aiResponse ? (
              <div>
                <div style={{ padding: '0.75rem 1rem', backgroundColor: '#f8fafc', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '0.35rem' }}>
                    <span style={{ fontWeight: 600 }}>Medicine Pair:</span>
                    <span style={{ color: '#0f172a', fontWeight: 700 }}>{aiResponse.drugA} + {aiResponse.drugB}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                    <span style={{ fontWeight: 600 }}>Verified Reference:</span>
                    <span style={{ color: 'var(--primary-dark)' }}>{aiResponse.verifiedSource}</span>
                  </div>
                </div>

                <div style={{ whiteSpace: 'pre-line', fontSize: '0.9rem', color: '#1e293b', lineHeight: '1.6', marginBottom: '1.25rem', backgroundColor: '#faf5ff', padding: '1rem', borderRadius: 'var(--radius-sm)', border: '1px solid #e9d5ff' }}>
                  {aiResponse.patientFriendlyExplanation}
                </div>

                <div style={{ padding: '0.75rem 1rem', backgroundColor: '#fffbeb', borderRadius: 'var(--radius-sm)', border: '1px solid #fde68a', fontSize: '0.8rem', color: '#92400e', marginBottom: '1rem' }}>
                  <strong>Safety Notice: </strong>{aiResponse.disclaimer}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button onClick={() => setAiModalAlert(null)} className="btn btn-secondary btn-sm">
                    Close
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
