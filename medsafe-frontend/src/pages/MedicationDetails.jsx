import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { medicationService } from '../services/medicationService';
import { scheduleService } from '../services/scheduleService';
import { 
  Pill, 
  ArrowLeft, 
  Clock, 
  Calendar, 
  Edit3, 
  Trash2, 
  Plus, 
  ShieldAlert,
  Utensils
} from 'lucide-react';

export default function MedicationDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [medication, setMedication] = useState(null);
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // New schedule form modal
  const [showAddSchedule, setShowAddSchedule] = useState(false);
  const [scheduledTime, setScheduledTime] = useState('08:00');
  const [mealRelation, setMealRelation] = useState('After food');
  const [savingSchedule, setSavingSchedule] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [medData, schedData] = await Promise.all([
        medicationService.getById(id),
        scheduleService.getByMedicationId(id)
      ]);
      setMedication(medData);
      setSchedules(schedData);
    } catch (err) {
      setError(err.message || 'Failed to load medication details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id]);

  const handleAddSchedule = async (e) => {
    e.preventDefault();
    try {
      setSavingSchedule(true);
      const formattedTime = scheduledTime.length === 5 ? `${scheduledTime}:00` : scheduledTime;
      const newSched = await scheduleService.create(id, {
        scheduledTime: formattedTime,
        mealRelation
      });
      setSchedules(prev => [...prev, newSched]);
      setShowAddSchedule(false);
      setScheduledTime('08:00');
    } catch (err) {
      alert(err.message || 'Failed to add schedule.');
    } finally {
      setSavingSchedule(false);
    }
  };

  const handleDeleteSchedule = async (scheduleId) => {
    try {
      await scheduleService.delete(scheduleId);
      setSchedules(prev => prev.filter(s => s.scheduleId !== scheduleId));
    } catch (err) {
      alert(err.message || 'Failed to delete schedule.');
    }
  };

  const handleDeleteMedication = async () => {
    if (window.confirm(`Are you sure you want to delete ${medication.medicineName}?`)) {
      try {
        await medicationService.delete(id);
        navigate('/medications');
      } catch (err) {
        alert(err.message || 'Failed to delete medication.');
      }
    }
  };

  if (loading) {
    return (
      <div className="state-container">
        <p>Loading medication information...</p>
      </div>
    );
  }

  if (error || !medication) {
    return (
      <div className="state-container">
        <h3 className="state-title">Medication Not Found</h3>
        <p className="state-description">{error || 'This medication may have been removed.'}</p>
        <Link to="/medications" className="btn btn-primary btn-sm">
          Return to Medications
        </Link>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <Link to="/medications" className="btn btn-outline btn-sm" style={{ display: 'inline-flex', marginBottom: '0.5rem' }}>
            <ArrowLeft size={16} />
            <span>Back to Medications</span>
          </Link>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#0f172a' }}>
              {medication.medicineName}
            </h2>
            <span className={`badge ${medication.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'}`}>
              {medication.status}
            </span>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Link to={`/medications/${id}/edit`} className="btn btn-outline btn-sm">
            <Edit3 size={15} />
            <span>Edit</span>
          </Link>
          <button onClick={handleDeleteMedication} className="btn btn-outline btn-sm" style={{ color: 'var(--danger)', borderColor: '#fca5a5' }}>
            <Trash2 size={15} />
            <span>Delete</span>
          </button>
        </div>
      </div>

      {/* Medication Details Card */}
      <div className="card" style={{ marginBottom: '1.75rem' }}>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
          Prescription Information
        </h3>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem' }}>
          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Active Ingredient</span>
            <p style={{ fontSize: '1rem', fontWeight: 600, color: '#0d9488', marginTop: '0.2rem' }}>
              {medication.activeIngredient || 'Not specified'}
            </p>
          </div>

          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Dosage</span>
            <p style={{ fontSize: '1rem', fontWeight: 600, color: '#1e293b', marginTop: '0.2rem' }}>
              {medication.dosage || 'Not specified'}
            </p>
          </div>

          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Frequency</span>
            <p style={{ fontSize: '1rem', fontWeight: 600, color: '#1e293b', marginTop: '0.2rem' }}>
              {medication.frequency || 'Not specified'}
            </p>
          </div>

          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Course Duration</span>
            <p style={{ fontSize: '0.925rem', color: '#1e293b', marginTop: '0.2rem' }}>
              {medication.startDate ? `Started: ${medication.startDate}` : 'Ongoing'}
              {medication.endDate ? ` — Ends: ${medication.endDate}` : ''}
            </p>
          </div>
        </div>
      </div>

      {/* Schedules Section */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">Dosage Schedules</h3>
            <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Times at which this medication should be taken
            </p>
          </div>
          <button onClick={() => setShowAddSchedule(true)} className="btn btn-primary btn-sm">
            <Plus size={16} />
            <span>Add Dose Time</span>
          </button>
        </div>

        {schedules.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
            <Clock size={36} style={{ color: '#cbd5e1', marginBottom: '0.5rem' }} />
            <p style={{ fontWeight: 600, color: '#475569' }}>No dosage schedules configured</p>
            <p style={{ fontSize: '0.85rem', marginBottom: '1rem' }}>
              Add scheduled times to receive dose reminders on your dashboard.
            </p>
            <button onClick={() => setShowAddSchedule(true)} className="btn btn-secondary btn-sm">
              Add Schedule
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {schedules.map((s) => (
              <div
                key={s.scheduleId}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.85rem 1.25rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <Clock size={18} style={{ color: 'var(--primary)' }} />
                  <div>
                    <span style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a' }}>
                      {s.scheduledTime}
                    </span>
                    {s.mealRelation && (
                      <span style={{ marginLeft: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                        <Utensils size={13} />
                        {s.mealRelation}
                      </span>
                    )}
                  </div>
                </div>

                <button
                  onClick={() => handleDeleteSchedule(s.scheduleId)}
                  className="btn-icon"
                  style={{ color: '#ef4444' }}
                  title="Remove Schedule"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add Schedule Modal */}
      {showAddSchedule && (
        <div className="modal-overlay" onClick={() => setShowAddSchedule(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '440px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a', marginBottom: '1rem' }}>
              Add Dose Schedule for {medication.medicineName}
            </h3>

            <form onSubmit={handleAddSchedule} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  Scheduled Time
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
                  <option value="Empty stomach">On an empty stomach</option>
                  <option value="No food relation">No food relation</option>
                </select>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  onClick={() => setShowAddSchedule(false)}
                  className="btn btn-secondary btn-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingSchedule}
                  className="btn btn-primary btn-sm"
                >
                  {savingSchedule ? 'Saving...' : 'Add Schedule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
