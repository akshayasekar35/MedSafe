import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { medicationService } from '../services/medicationService';
import { scheduleService } from '../services/scheduleService';
import { logService } from '../services/logService';
import ScheduleCard from '../components/ScheduleCard';
import { CalendarClock, Plus, Clock, Utensils, RefreshCw, AlertCircle } from 'lucide-react';

export default function Schedule() {
  const [searchParams] = useSearchParams();
  const preselectedMedId = searchParams.get('medicationId');

  const [medications, setMedications] = useState([]);
  const [schedulesByMed, setSchedulesByMed] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Add Schedule Modal
  const [showAddModal, setShowAddModal] = useState(Boolean(preselectedMedId));
  const [selectedMedId, setSelectedMedId] = useState(preselectedMedId || '');
  const [scheduledTime, setScheduledTime] = useState('08:00');
  const [mealRelation, setMealRelation] = useState('After food');
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const meds = await medicationService.getAll();
      setMedications(meds);

      const schedMap = {};
      for (const med of meds) {
        try {
          const scheds = await scheduleService.getByMedicationId(med.medicationId);
          schedMap[med.medicationId] = scheds;
        } catch (e) {
          console.error(e);
        }
      }
      setSchedulesByMed(schedMap);

      if (preselectedMedId && meds.some(m => m.medicationId.toString() === preselectedMedId)) {
        setSelectedMedId(preselectedMedId);
      } else if (meds.length > 0 && !selectedMedId) {
        setSelectedMedId(meds[0].medicationId);
      }
    } catch (err) {
      setError(err.message || 'Failed to load schedules.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [preselectedMedId]);

  const handleAddSchedule = async (e) => {
    e.preventDefault();
    if (!selectedMedId) return;

    try {
      setSaving(true);
      const formattedTime = scheduledTime.length === 5 ? `${scheduledTime}:00` : scheduledTime;
      const created = await scheduleService.create(selectedMedId, {
        scheduledTime: formattedTime,
        mealRelation
      });

      setSchedulesByMed(prev => ({
        ...prev,
        [selectedMedId]: [...(prev[selectedMedId] || []), created]
      }));

      setShowAddModal(false);
      setScheduledTime('08:00');
    } catch (err) {
      alert(err.message || 'Failed to add schedule.');
    } finally {
      setSaving(false);
    }
  };

  const handleTaken = async (scheduleId) => {
    await logService.markTaken(scheduleId);
  };

  const handleMissed = async (scheduleId) => {
    await logService.markMissed(scheduleId);
  };

  const handleDelete = async (medId, scheduleId) => {
    try {
      await scheduleService.delete(scheduleId);
      setSchedulesByMed(prev => ({
        ...prev,
        [medId]: prev[medId].filter(s => s.scheduleId !== scheduleId)
      }));
    } catch (err) {
      alert(err.message || 'Failed to delete schedule.');
    }
  };

  if (loading) {
    return (
      <div className="state-container">
        <RefreshCw size={36} className="animate-spin" style={{ color: 'var(--primary)', marginBottom: '1rem' }} />
        <h3 className="state-title">Loading Medication Schedules...</h3>
      </div>
    );
  }

  if (error) {
    return (
      <div className="state-container">
        <AlertCircle size={36} style={{ color: 'var(--danger)', marginBottom: '1rem' }} />
        <h3 className="state-title">Unable to Load Schedules</h3>
        <p className="state-description">{error}</p>
        <button onClick={loadData} className="btn btn-primary btn-sm" style={{ marginTop: '0.75rem' }}>
          <RefreshCw size={15} />
          <span>Try Again</span>
        </button>
      </div>
    );
  }

  const activeMeds = medications.filter(m => m.status === 'ACTIVE');

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a' }}>
            Medication Schedules
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Configure daily dosage times and track dose adherence.
          </p>
        </div>

        <button onClick={() => setShowAddModal(true)} disabled={activeMeds.length === 0} className="btn btn-primary">
          <Plus size={18} />
          <span>Add Schedule Time</span>
        </button>
      </div>

      {activeMeds.length === 0 ? (
        <div className="state-container">
          <CalendarClock size={40} style={{ color: '#cbd5e1', marginBottom: '0.75rem' }} />
          <h3 className="state-title">No Active Medications Available</h3>
          <p className="state-description">Please add active medications first before creating schedules.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {activeMeds.map((med) => {
            const medSchedules = schedulesByMed[med.medicationId] || [];

            return (
              <div key={med.medicationId} className="card">
                <div className="card-header" style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
                  <div>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a' }}>
                      {med.medicineName}
                    </h3>
                    <span style={{ fontSize: '0.85rem', color: '#0d9488', fontWeight: 500 }}>
                      {med.dosage ? `${med.dosage} • ` : ''}{med.frequency || 'Daily'}
                    </span>
                  </div>

                  <button
                    onClick={() => {
                      setSelectedMedId(med.medicationId);
                      setShowAddModal(true);
                    }}
                    className="btn btn-outline btn-sm"
                  >
                    <Plus size={14} />
                    <span>Add Time</span>
                  </button>
                </div>

                {medSchedules.length === 0 ? (
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', padding: '0.5rem 0' }}>
                    No dosage times scheduled for this medication yet.
                  </p>
                ) : (
                  <div>
                    {medSchedules.map((s) => (
                      <div key={s.scheduleId} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
                        <div style={{ flex: 1 }}>
                          <ScheduleCard
                            schedule={s}
                            onTaken={handleTaken}
                            onMissed={handleMissed}
                          />
                        </div>
                        <button
                          onClick={() => handleDelete(med.medicationId, s.scheduleId)}
                          className="btn-icon"
                          style={{ color: '#ef4444', marginBottom: '0.75rem' }}
                          title="Remove time"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Add Schedule Modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '440px' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Add Dose Schedule
            </h3>

            <form onSubmit={handleAddSchedule} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  Select Medication
                </label>
                <select
                  value={selectedMedId}
                  onChange={(e) => setSelectedMedId(e.target.value)}
                  required
                >
                  {activeMeds.map((m) => (
                    <option key={m.medicationId} value={m.medicationId}>
                      {m.medicineName} ({m.dosage || 'Prescribed'})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  Scheduled Dose Time
                </label>
                <input
                  type="time"
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  Meal Relation
                </label>
                <select value={mealRelation} onChange={(e) => setMealRelation(e.target.value)}>
                  <option value="Before food">Before food</option>
                  <option value="After food">After food</option>
                  <option value="With food">With food</option>
                  <option value="On empty stomach">On empty stomach</option>
                  <option value="No food relation">No food relation</option>
                </select>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="btn btn-secondary btn-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="btn btn-primary btn-sm"
                >
                  {saving ? 'Saving...' : 'Add Schedule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
