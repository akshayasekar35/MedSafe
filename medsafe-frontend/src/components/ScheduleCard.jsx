import React, { useState } from 'react';
import { Clock, CheckCircle2, XCircle, Utensils } from 'lucide-react';

export default function ScheduleCard({ schedule, onTaken, onMissed, initialStatus = null }) {
  const [status, setStatus] = useState(initialStatus);
  const [loading, setLoading] = useState(false);

  const handleTaken = async () => {
    try {
      setLoading(true);
      await onTaken(schedule.scheduleId);
      setStatus('TAKEN');
    } finally {
      setLoading(false);
    }
  };

  const handleMissed = async () => {
    try {
      setLoading(true);
      await onMissed(schedule.scheduleId);
      setStatus('MISSED');
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    try {
      const parts = timeStr.split(':');
      let hour = parseInt(parts[0], 10);
      const min = parts[1] || '00';
      const ampm = hour >= 12 ? 'PM' : 'AM';
      hour = hour % 12 || 12;
      return `${hour}:${min} ${ampm}`;
    } catch {
      return timeStr;
    }
  };

  return (
    <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '1rem 1.25rem', marginBottom: '0.75rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <div style={{ width: 42, height: 42, borderRadius: '50%', backgroundColor: '#f0f9ff', color: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Clock size={20} />
        </div>
        <div>
          <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a' }}>
            {schedule.medicineName || 'Scheduled Medication'}
          </h4>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            <span style={{ fontWeight: 600, color: 'var(--primary-dark)' }}>
              {formatTime(schedule.scheduledTime)}
            </span>
            {schedule.mealRelation && (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                <Utensils size={13} />
                {schedule.mealRelation}
              </span>
            )}
          </div>
        </div>
      </div>

      <div>
        {status === 'TAKEN' ? (
          <span className="badge badge-taken" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', padding: '0.4rem 0.85rem' }}>
            <CheckCircle2 size={15} />
            TAKEN
          </span>
        ) : status === 'MISSED' ? (
          <span className="badge badge-missed" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', padding: '0.4rem 0.85rem' }}>
            <XCircle size={15} />
            MISSED
          </span>
        ) : (
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              onClick={handleTaken}
              disabled={loading}
              className="btn btn-success btn-sm"
              title="Mark as Taken"
            >
              <CheckCircle2 size={16} />
              <span>TAKEN</span>
            </button>
            <button
              onClick={handleMissed}
              disabled={loading}
              className="btn btn-danger btn-sm"
              title="Mark as Missed"
            >
              <XCircle size={16} />
              <span>MISSED</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
