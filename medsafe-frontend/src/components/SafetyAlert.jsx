import React from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, Sparkles, ArrowRight, Shield, Clock, HelpCircle, CheckCircle2 } from 'lucide-react';

export default function SafetyAlert({ alert, onExplainAI }) {
  const rawSeverity = alert.severity?.toUpperCase() || 'DOCUMENTED';
  const evidenceStatus = alert.evidenceStatus?.toUpperCase() || 'VERIFIED';

  const isMock = evidenceStatus === 'DEVELOPMENT_MOCK' || alert.sourceName?.includes('Mock');
  const isAmbiguous = evidenceStatus === 'AMBIGUOUS' || rawSeverity === 'AMBIGUOUS';
  const isUnavailable = evidenceStatus === 'SOURCE_UNAVAILABLE';
  const isNotFound = evidenceStatus === 'NOT_FOUND';

  const getSeverityBadge = () => {
    switch (rawSeverity) {
      case 'CONTRAINDICATED':
      case 'MAJOR':
      case 'SEVERE':
        return <span className="badge badge-severity-major">{rawSeverity} Interaction</span>;
      case 'MODERATE':
        return <span className="badge badge-severity-moderate">Moderate Interaction</span>;
      case 'MINOR':
        return <span className="badge badge-severity-minor">Minor Interaction</span>;
      case 'DOCUMENTED_IN_LABEL':
        return <span className="badge" style={{ backgroundColor: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe' }}>Documented in FDA Label (Unspecified Severity)</span>;
      case 'AMBIGUOUS':
        return <span className="badge" style={{ backgroundColor: '#fef3c7', color: '#92400e', border: '1px solid #fde68a' }}>Ambiguous Match</span>;
      case 'UNAVAILABLE':
        return <span className="badge" style={{ backgroundColor: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1' }}>Source Unavailable</span>;
      case 'NONE_IDENTIFIED':
        return <span className="badge badge-success">No Interaction Identified</span>;
      default:
        return <span className="badge badge-inactive">Clinical Advisory</span>;
    }
  };

  const getEvidenceBadge = () => {
    if (isMock) {
      return (
        <span 
          style={{ 
            fontSize: '0.72rem', 
            backgroundColor: '#f1f5f9', 
            color: '#64748b', 
            padding: '0.2rem 0.55rem', 
            borderRadius: '9999px',
            border: '1px dashed #cbd5e1',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.3rem'
          }}
        >
          <HelpCircle size={12} />
          Development / Mock Test Reference
        </span>
      );
    }
    if (isAmbiguous) {
      return (
        <span 
          style={{ 
            fontSize: '0.72rem', 
            backgroundColor: '#fffbeb', 
            color: '#b45309', 
            padding: '0.2rem 0.55rem', 
            borderRadius: '9999px',
            border: '1px solid #fde68a',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.3rem'
          }}
        >
          <AlertTriangle size={12} />
          Confirmation Required
        </span>
      );
    }
    if (isUnavailable) {
      return (
        <span 
          style={{ 
            fontSize: '0.72rem', 
            backgroundColor: '#fef2f2', 
            color: '#991b1b', 
            padding: '0.2rem 0.55rem', 
            borderRadius: '9999px',
            border: '1px solid #fecaca',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.3rem'
          }}
        >
          <AlertTriangle size={12} />
          Source Unavailable
        </span>
      );
    }
    return (
      <span 
        style={{ 
          fontSize: '0.72rem', 
          backgroundColor: '#f0fdf4', 
          color: '#15803d', 
          padding: '0.2rem 0.55rem', 
          borderRadius: '9999px',
          border: '1px solid #bbf7d0',
          fontWeight: 600,
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.3rem'
        }}
      >
        <CheckCircle2 size={12} />
        Verified Source Information
      </span>
    );
  };

  const borderColor = isAmbiguous ? 'var(--warning)' : isUnavailable ? '#94a3b8' : 'var(--danger)';

  return (
    <div className="card" style={{ borderLeft: `4px solid ${borderColor}`, marginBottom: '1rem' }}>
      <div className="card-header" style={{ marginBottom: '0.65rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', flexWrap: 'wrap' }}>
          <AlertTriangle size={20} color={borderColor} />
          <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a' }}>
            {alert.medicineName1} + {alert.medicineName2}
          </h4>
          {getEvidenceBadge()}
        </div>
        <div>
          {getSeverityBadge()}
        </div>
      </div>

      {isAmbiguous ? (
        <div style={{ padding: '0.75rem 1rem', backgroundColor: '#fffbeb', borderRadius: 'var(--radius-sm)', border: '1px solid #fde68a', marginBottom: '0.85rem' }}>
          <div style={{ fontWeight: 700, color: '#92400e', marginBottom: '0.25rem' }}>
            Please confirm the medicine before checking interactions.
          </div>
          <p style={{ fontSize: '0.85rem', color: '#78350f', marginBottom: '0.5rem' }}>
            Multiple clinical concepts or formulations match this name. Please verify the exact prescription:
          </p>
          {alert.ambiguousOptions && alert.ambiguousOptions.length > 0 && (
            <ul style={{ margin: 0, paddingLeft: '1.25rem', fontSize: '0.825rem', color: '#92400e' }}>
              {alert.ambiguousOptions.map((opt, i) => (
                <li key={i}>{opt}</li>
              ))}
            </ul>
          )}
        </div>
      ) : (
        <p style={{ fontSize: '0.9rem', color: '#334155', marginBottom: '0.85rem', lineHeight: '1.5' }}>
          {alert.description}
        </p>
      )}

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.65rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
            <Shield size={14} />
            <strong>Source:</strong> {alert.sourceName || 'FDA Drug Labeling / RxNorm'}
          </span>
          {alert.sourceReference && (
            <span style={{ backgroundColor: '#f8fafc', padding: '0.15rem 0.45rem', borderRadius: 4, border: '1px solid #e2e8f0', fontSize: '0.75rem' }}>
              {alert.sourceReference}
            </span>
          )}
          {alert.checkedAt && (
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
              <Clock size={13} />
              <span>Checked: {new Date(alert.checkedAt).toLocaleDateString()} {new Date(alert.checkedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
            </span>
          )}
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {onExplainAI && !isAmbiguous && !isUnavailable && !isMock && (
            <button
              onClick={() => onExplainAI(alert)}
              className="btn btn-sm"
              style={{
                backgroundColor: '#f5f3ff',
                color: '#6d28d9',
                border: '1px solid #ddd6fe',
                fontWeight: 600
              }}
            >
              <Sparkles size={14} />
              <span>Explain with AI</span>
            </button>
          )}

          {alert.alertId && (
            <Link
              to={`/interactions/${alert.alertId}`}
              className="btn btn-outline btn-sm"
            >
              <span>View Details</span>
              <ArrowRight size={14} />
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
