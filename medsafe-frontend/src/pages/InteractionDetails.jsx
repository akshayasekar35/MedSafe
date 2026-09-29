import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { interactionService } from '../services/interactionService';
import { 
  ShieldAlert, 
  ArrowLeft, 
  Sparkles, 
  AlertTriangle, 
  RefreshCw, 
  Calendar, 
  FileText,
  ShieldCheck,
  Info
} from 'lucide-react';

export default function InteractionDetails() {
  const { id } = useParams();

  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // AI Explanation State
  const [aiExplanation, setAiExplanation] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);

  useEffect(() => {
    interactionService.getAlertById(id)
      .then((data) => {
        setAlert(data);
      })
      .catch((err) => {
        setError(err.message || 'Failed to load interaction alert details.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [id]);

  const handleExplainAI = async () => {
    if (!alert) return;

    try {
      setAiLoading(true);
      setAiError(null);
      const res = await interactionService.explainWithAI(
        alert.medicineName1,
        alert.medicineName2,
        alert.severity,
        alert.description,
        alert.sourceName,
        alert.evidenceStatus || 'VERIFIED'
      );
      setAiExplanation(res);
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
        <h3 className="state-title">Loading interaction alert...</h3>
      </div>
    );
  }

  if (error || !alert) {
    return (
      <div className="state-container">
        <AlertTriangle size={36} style={{ color: 'var(--danger)', marginBottom: '1rem' }} />
        <h3 className="state-title">Alert Not Found</h3>
        <p className="state-description">{error || 'This interaction alert could not be retrieved.'}</p>
        <Link to="/safety-alerts" className="btn btn-primary btn-sm">
          Return to Safety Alerts
        </Link>
      </div>
    );
  }

  const severity = alert.severity?.toUpperCase() || 'DOCUMENTED';
  const isMock = alert.evidenceStatus === 'DEVELOPMENT_MOCK' || alert.sourceName?.includes('Mock');
  const isDocumentedInLabel = severity === 'DOCUMENTED_IN_LABEL' || severity === 'DOCUMENTED';

  const getSeverityBadgeText = () => {
    if (isDocumentedInLabel) return 'Documented in FDA Label (Unspecified Severity)';
    if (severity === 'CONTRAINDICATED') return 'Contraindicated Interaction';
    if (severity === 'MAJOR' || severity === 'SEVERE') return 'Major Interaction';
    if (severity === 'MODERATE') return 'Moderate Interaction';
    if (severity === 'MINOR') return 'Minor Interaction';
    return `${severity} Interaction`;
  };

  return (
    <div style={{ maxWidth: '840px', margin: '0 auto' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <Link to="/safety-alerts" className="btn btn-outline btn-sm" style={{ display: 'inline-flex', marginBottom: '0.75rem' }}>
          <ArrowLeft size={16} />
          <span>Back to Safety Alerts</span>
        </Link>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <h2 style={{ fontSize: '1.65rem', fontWeight: 800, color: '#0f172a' }}>
            {alert.medicineName1} + {alert.medicineName2}
          </h2>
          <span 
            className="badge" 
            style={{ 
              backgroundColor: isDocumentedInLabel ? '#eff6ff' : (severity === 'MAJOR' || severity === 'SEVERE' || severity === 'CONTRAINDICATED') ? '#fee2e2' : '#fef3c7',
              color: isDocumentedInLabel ? '#1d4ed8' : (severity === 'MAJOR' || severity === 'SEVERE' || severity === 'CONTRAINDICATED') ? '#b91c1c' : '#b45309',
              border: isDocumentedInLabel ? '1px solid #bfdbfe' : 'none'
            }}
          >
            {getSeverityBadgeText()}
          </span>
          {alert.sourceName?.includes('Mock') ? (
            <span 
              style={{ 
                fontSize: '0.75rem', 
                backgroundColor: '#f1f5f9', 
                color: '#64748b', 
                padding: '0.2rem 0.6rem', 
                borderRadius: '9999px',
                border: '1px dashed #cbd5e1',
                fontWeight: 600
              }}
            >
              Development / Mock Test Reference
            </span>
          ) : (
            <span 
              style={{ 
                fontSize: '0.75rem', 
                backgroundColor: '#f0fdf4', 
                color: '#15803d', 
                padding: '0.2rem 0.6rem', 
                borderRadius: '9999px',
                border: '1px solid #bbf7d0',
                fontWeight: 600
              }}
            >
              Verified Source Information
            </span>
          )}
        </div>
      </div>

      {/* Verified Medical Details Card */}
      <div className="card" style={{ marginBottom: '1.75rem', borderLeft: '4px solid var(--danger)' }}>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.75rem' }}>
          Verified Clinical Information
        </h3>

        <p style={{ fontSize: '0.95rem', color: '#1e293b', lineHeight: '1.6', marginBottom: '1.25rem' }}>
          {alert.description}
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', borderTop: '1px solid var(--border-color)', paddingTop: '1rem', fontSize: '0.85rem' }}>
          <div>
            <span style={{ color: 'var(--text-muted)', fontWeight: 600, display: 'block' }}>Information Source</span>
            <span style={{ color: '#0f172a', fontWeight: 600 }}>{alert.sourceName}</span>
          </div>

          {alert.sourceReference && (
            <div>
              <span style={{ color: 'var(--text-muted)', fontWeight: 600, display: 'block' }}>Reference Identifier</span>
              <span style={{ color: 'var(--primary-dark)', fontWeight: 600 }}>{alert.sourceReference}</span>
            </div>
          )}

          {alert.checkedAt && (
            <div>
              <span style={{ color: 'var(--text-muted)', fontWeight: 600, display: 'block' }}>Screened At</span>
              <span style={{ color: '#475569' }}>{new Date(alert.checkedAt).toLocaleString()}</span>
            </div>
          )}
        </div>
      </div>

      {/* AI Explanation Section */}
      <div className="card" style={{ marginBottom: '1.75rem' }}>
        <div className="card-header" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <div style={{ width: 34, height: 34, borderRadius: 8, backgroundColor: '#f5f3ff', color: '#7c3aed', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Sparkles size={20} />
            </div>
            <div>
              <h3 className="card-title">AI Explanation</h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Evidence-grounded patient guidance based strictly on verified medical information
              </p>
            </div>
          </div>

          {!aiExplanation && !isMock && (
            <button
              onClick={handleExplainAI}
              disabled={aiLoading}
              className="btn btn-sm"
              style={{
                backgroundColor: '#7c3aed',
                color: '#ffffff',
                fontWeight: 600
              }}
            >
              <Sparkles size={16} />
              <span>{aiLoading ? 'Explaining...' : 'Explain with AI'}</span>
            </button>
          )}
        </div>

        {isMock && (
          <div style={{ padding: '1rem', backgroundColor: '#f8fafc', borderRadius: 'var(--radius-sm)', border: '1px dashed #cbd5e1', fontSize: '0.85rem', color: '#64748b' }}>
            <strong>Development Reference Note: </strong>
            Clinical AI explanation is disabled for development mock data. Real clinical sources (openFDA / RxNorm) are required for evidence-grounded AI translation.
          </div>
        )}

        {aiLoading && (
          <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
            <RefreshCw size={30} className="animate-spin" style={{ color: '#7c3aed', marginBottom: '0.75rem' }} />
            <p style={{ fontWeight: 600, color: '#475569' }}>Generating patient-friendly explanation...</p>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Translating clinical findings without inventing interactions.</p>
          </div>
        )}

        {aiError && (
          <div className="alert-box danger">
            <AlertTriangle size={18} />
            <span>{aiError}</span>
          </div>
        )}

        {aiExplanation && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ whiteSpace: 'pre-line', fontSize: '0.925rem', color: '#1e293b', lineHeight: '1.65', backgroundColor: '#faf5ff', padding: '1.25rem', borderRadius: 'var(--radius-sm)', border: '1px solid #e9d5ff' }}>
              {aiExplanation.patientFriendlyExplanation}
            </div>

            <div style={{ padding: '0.85rem 1rem', backgroundColor: '#fffbeb', borderRadius: 'var(--radius-sm)', border: '1px solid #fde68a', fontSize: '0.825rem', color: '#92400e' }}>
              <strong>Safety Standard: </strong>{aiExplanation.disclaimer}
            </div>
          </div>
        )}

        {!aiExplanation && !aiLoading && !isMock && (
          <div style={{ textAlign: 'center', padding: '1.5rem', backgroundColor: '#f8fafc', borderRadius: 'var(--radius-sm)', border: '1px dashed var(--border-color)' }}>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
              Click "Explain with AI" to receive a simplified, patient-friendly breakdown of this documented interaction.
            </p>
            <button
              onClick={handleExplainAI}
              className="btn btn-sm"
              style={{
                backgroundColor: '#f5f3ff',
                color: '#6d28d9',
                border: '1px solid #ddd6fe',
                fontWeight: 600
              }}
            >
              <Sparkles size={15} />
              <span>Explain with AI</span>
            </button>
          </div>
        )}
      </div>

      {/* Safety Notice Footer */}
      <div className="alert-box info">
        <Info size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
        <div style={{ fontSize: '0.825rem', lineHeight: '1.5' }}>
          <strong>Notice: </strong>
          The absence of a documented interaction in the consulted reference source does not guarantee that no risk exists.
          Do not discontinue or alter medications without consulting your prescribing healthcare professional.
        </div>
      </div>
    </div>
  );
}
