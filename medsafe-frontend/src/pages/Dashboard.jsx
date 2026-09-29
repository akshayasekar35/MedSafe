import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { medicationService } from '../services/medicationService';
import { scheduleService } from '../services/scheduleService';
import { logService } from '../services/logService';
import { interactionService } from '../services/interactionService';
import StatCard from '../components/StatCard';
import ScheduleCard from '../components/ScheduleCard';
import SafetyAlert from '../components/SafetyAlert';
import { 
  Pill, 
  CalendarClock, 
  CheckCircle2, 
  XCircle, 
  ShieldAlert, 
  Plus, 
  AlertTriangle,
  RefreshCw,
  Sparkles
} from 'lucide-react';

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [medications, setMedications] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [logs, setLogs] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [duplicates, setDuplicates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // AI Modal State
  const [aiModalAlert, setAiModalAlert] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResponse, setAiResponse] = useState(null);
  const [aiError, setAiError] = useState(null);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Load medications
      const meds = await medicationService.getAll();
      setMedications(meds);

      // Load all schedules for active medications
      const allSchedules = [];
      for (const med of meds) {
        if (med.status === 'ACTIVE') {
          try {
            const medSchedules = await scheduleService.getByMedicationId(med.medicationId);
            allSchedules.push(...medSchedules);
          } catch (e) {
            console.error('Error loading schedules for med:', med.medicationId, e);
          }
        }
      }
      setSchedules(allSchedules);

      // Load logs
      const userLogs = await logService.getLogs();
      setLogs(userLogs);

      // Load interactions & duplicate warnings
      const userAlerts = await interactionService.getAlerts();
      setAlerts(userAlerts);

      const userDuplicates = await medicationService.getDuplicates();
      setDuplicates(userDuplicates);

    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      setError(err.message || 'Unable to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleTaken = async (scheduleId) => {
    await logService.markTaken(scheduleId);
    // Refresh logs to update taken counter
    const userLogs = await logService.getLogs();
    setLogs(userLogs);
  };

  const handleMissed = async (scheduleId) => {
    await logService.markMissed(scheduleId);
    // Refresh logs to update missed counter
    const userLogs = await logService.getLogs();
    setLogs(userLogs);
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

  // Calculate statistics
  const activeMedicationsCount = medications.filter(m => m.status === 'ACTIVE').length;
  const todayDosesCount = schedules.length;
  const takenCount = logs.filter(l => l.status === 'TAKEN').length;
  const missedCount = logs.filter(l => l.status === 'MISSED').length;
  const activeAlertsCount = alerts.length;

  if (loading) {
    return (
      <div className="state-container">
        <RefreshCw size={36} className="animate-spin" style={{ color: 'var(--primary)', marginBottom: '1rem' }} />
        <h3 className="state-title">Loading your medication safety dashboard...</h3>
        <p className="state-description">Retrieving active prescriptions, schedules, and clinical safety alerts.</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="state-container">
        <AlertTriangle size={36} style={{ color: 'var(--danger)', marginBottom: '1rem' }} />
        <h3 className="state-title">Unable to Load Dashboard</h3>
        <p className="state-description">{error}</p>
        <button onClick={fetchDashboardData} className="btn btn-primary btn-sm">
          <RefreshCw size={15} />
          <span>Try Again</span>
        </button>
      </div>
    );
  }

  return (
    <div>
      {/* Header welcome banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.65rem', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.02em' }}>
            Welcome back, {user?.name || 'Patient'}!
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.925rem' }}>
            Your daily medication schedule and interaction safety overview.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/safety-alerts" className="btn btn-secondary btn-sm">
            <ShieldAlert size={16} />
            <span>Check Interactions</span>
          </Link>
          <Link to="/medications/add" className="btn btn-primary btn-sm">
            <Plus size={16} />
            <span>Add Medication</span>
          </Link>
        </div>
      </div>

      {/* Duplicate warning notification if detected */}
      {duplicates.length > 0 && (
        <div className="alert-box warning">
          <AlertTriangle size={22} style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.95rem', marginBottom: '0.2rem' }}>
              Duplicate Active Ingredient Detected
            </div>
            <p style={{ fontSize: '0.875rem', marginBottom: '0.5rem' }}>
              {duplicates[0].warningMessage}
            </p>
            <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>
              {duplicates[0].clinicalRecommendation}
            </div>
          </div>
        </div>
      )}

      {/* Stat Cards */}
      <div className="stats-grid">
        <StatCard 
          label="Active Medications" 
          value={activeMedicationsCount} 
          icon={Pill} 
          color="#0284c7" 
          bgColor="#e0f2fe" 
        />
        <StatCard 
          label="Today's Doses" 
          value={todayDosesCount} 
          icon={CalendarClock} 
          color="#0d9488" 
          bgColor="#ccfbf1" 
        />
        <StatCard 
          label="Doses Taken" 
          value={takenCount} 
          icon={CheckCircle2} 
          color="#16a34a" 
          bgColor="#dcfce7" 
        />
        <StatCard 
          label="Doses Missed" 
          value={missedCount} 
          icon={XCircle} 
          color="#dc2626" 
          bgColor="#fee2e2" 
        />
        <StatCard 
          label="Safety Alerts" 
          value={activeAlertsCount} 
          icon={ShieldAlert} 
          color={activeAlertsCount > 0 ? '#b91c1c' : '#64748b'} 
          bgColor={activeAlertsCount > 0 ? '#fee2e2' : '#f1f5f9'} 
        />
      </div>

      {/* Main 2-column layout */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.75rem' }}>
        
        {/* Today's Medication Doses */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Today's Scheduled Medications</h3>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Track your prescribed daily intake
              </p>
            </div>
            <Link to="/schedule" className="btn btn-outline btn-sm">
              Manage
            </Link>
          </div>

          {schedules.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: 'var(--text-muted)' }}>
              <CalendarClock size={36} style={{ color: '#cbd5e1', marginBottom: '0.75rem' }} />
              <p style={{ fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>No doses scheduled for today</p>
              <p style={{ fontSize: '0.85rem', marginBottom: '1rem' }}>
                Add dosage schedules to your active medications to receive reminders.
              </p>
              <Link to="/schedule" className="btn btn-secondary btn-sm">
                Add Schedule
              </Link>
            </div>
          ) : (
            <div>
              {schedules.map((schedule) => (
                <ScheduleCard
                  key={schedule.scheduleId}
                  schedule={schedule}
                  onTaken={handleTaken}
                  onMissed={handleMissed}
                />
              ))}
            </div>
          )}
        </div>

        {/* Safety Alerts Column */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Active Safety Alerts</h3>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Verified interaction warnings from trusted sources
              </p>
            </div>
            <Link to="/safety-alerts" className="btn btn-outline btn-sm">
              View All
            </Link>
          </div>

          {alerts.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2.5rem 1rem' }}>
              <div style={{ width: 44, height: 44, borderRadius: '50%', backgroundColor: '#f0fdf4', color: '#16a34a', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginBottom: '0.75rem' }}>
                <CheckCircle2 size={24} />
              </div>
              <p style={{ fontWeight: 600, color: '#1e293b', marginBottom: '0.35rem' }}>
                No Active Interaction Alerts
              </p>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', maxWidth: 360, margin: '0 auto 1rem auto' }}>
                No documented interactions have been identified in the consulted reference source for your active medications.
              </p>
              <Link to="/safety-alerts" className="btn btn-secondary btn-sm">
                Check Medications
              </Link>
            </div>
          ) : (
            <div>
              {alerts.slice(0, 3).map((alert) => (
                <SafetyAlert
                  key={alert.alertId}
                  alert={alert}
                  onExplainAI={handleExplainAI}
                />
              ))}
            </div>
          )}
        </div>
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
                <AlertTriangle size={18} />
                <span>{aiError}</span>
              </div>
            ) : aiResponse ? (
              <div>
                <div style={{ padding: '0.75rem 1rem', backgroundColor: '#f8fafc', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '0.35rem' }}>
                    <span style={{ fontWeight: 600 }}>Medicine Combination:</span>
                    <span style={{ color: '#0f172a', fontWeight: 700 }}>{aiResponse.drugA} + {aiResponse.drugB}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                    <span style={{ fontWeight: 600 }}>Verified Source:</span>
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
