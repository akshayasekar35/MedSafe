import React, { useState, useEffect } from 'react';
import { logService } from '../services/logService';
import { 
  History, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Calendar, 
  Filter, 
  RefreshCw 
} from 'lucide-react';

export default function MedicationHistory() {
  const [logs, setLogs] = useState([]);
  const [filter, setFilter] = useState('ALL'); // ALL, TAKEN, MISSED
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await logService.getLogs();
      setLogs(data);
    } catch (err) {
      setError(err.message || 'Failed to load medication logs.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const filteredLogs = logs.filter(log => {
    if (filter === 'ALL') return true;
    return log.status === filter;
  });

  const takenCount = logs.filter(l => l.status === 'TAKEN').length;
  const missedCount = logs.filter(l => l.status === 'MISSED').length;

  if (loading) {
    return (
      <div className="state-container">
        <RefreshCw size={36} className="animate-spin" style={{ color: 'var(--primary)', marginBottom: '1rem' }} />
        <h3 className="state-title">Loading Medication History...</h3>
      </div>
    );
  }

  if (error) {
    return (
      <div className="state-container">
        <XCircle size={36} style={{ color: 'var(--danger)', marginBottom: '1rem' }} />
        <h3 className="state-title">Unable to Load Intake History</h3>
        <p className="state-description">{error}</p>
        <button onClick={fetchLogs} className="btn btn-primary btn-sm" style={{ marginTop: '0.75rem' }}>
          <RefreshCw size={15} />
          <span>Try Again</span>
        </button>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a' }}>
            Medication Intake History
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Audit log of doses marked as taken or missed.
          </p>
        </div>

        {/* Filter buttons */}
        <div style={{ display: 'flex', gap: '0.5rem', backgroundColor: '#f1f5f9', padding: '0.25rem', borderRadius: 'var(--radius-sm)' }}>
          <button
            onClick={() => setFilter('ALL')}
            className="btn btn-sm"
            style={{
              backgroundColor: filter === 'ALL' ? '#ffffff' : 'transparent',
              color: filter === 'ALL' ? 'var(--primary)' : '#64748b',
              boxShadow: filter === 'ALL' ? 'var(--shadow-sm)' : 'none',
              fontWeight: 600
            }}
          >
            All Logs ({logs.length})
          </button>
          <button
            onClick={() => setFilter('TAKEN')}
            className="btn btn-sm"
            style={{
              backgroundColor: filter === 'TAKEN' ? '#ffffff' : 'transparent',
              color: filter === 'TAKEN' ? 'var(--success)' : '#64748b',
              boxShadow: filter === 'TAKEN' ? 'var(--shadow-sm)' : 'none',
              fontWeight: 600
            }}
          >
            Taken ({takenCount})
          </button>
          <button
            onClick={() => setFilter('MISSED')}
            className="btn btn-sm"
            style={{
              backgroundColor: filter === 'MISSED' ? '#ffffff' : 'transparent',
              color: filter === 'MISSED' ? 'var(--danger)' : '#64748b',
              boxShadow: filter === 'MISSED' ? 'var(--shadow-sm)' : 'none',
              fontWeight: 600
            }}
          >
            Missed ({missedCount})
          </button>
        </div>
      </div>

      {filteredLogs.length === 0 ? (
        <div className="state-container">
          <History size={40} style={{ color: '#cbd5e1', marginBottom: '0.75rem' }} />
          <h3 className="state-title">No Medication Logs Found</h3>
          <p className="state-description">
            {logs.length === 0 
              ? 'Doses you mark as TAKEN or MISSED on your Dashboard or Schedule will appear here.'
              : `No doses marked as ${filter}.`}
          </p>
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-color)', color: '#475569', fontWeight: 600 }}>
                <th style={{ padding: '0.85rem 1.25rem' }}>Medicine</th>
                <th style={{ padding: '0.85rem 1.25rem' }}>Scheduled Time</th>
                <th style={{ padding: '0.85rem 1.25rem' }}>Actual Time Recorded</th>
                <th style={{ padding: '0.85rem 1.25rem' }}>Meal Relation</th>
                <th style={{ padding: '0.85rem 1.25rem' }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {filteredLogs.map((log) => {
                const isTaken = log.status === 'TAKEN';
                return (
                  <tr key={log.logId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '1rem 1.25rem', fontWeight: 600, color: '#0f172a' }}>
                      {log.medicineName || 'Medication'}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', color: '#475569' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Clock size={15} color="var(--text-muted)" />
                        <span>{log.scheduledDatetime ? new Date(log.scheduledDatetime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : log.scheduledTime}</span>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          ({log.scheduledDatetime ? new Date(log.scheduledDatetime).toLocaleDateString() : 'Today'})
                        </span>
                      </div>
                    </td>
                    <td style={{ padding: '1rem 1.25rem', color: isTaken ? 'var(--success)' : 'var(--text-muted)' }}>
                      {log.actualDatetime ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <CheckCircle2 size={15} color="var(--success)" />
                          <span>{new Date(log.actualDatetime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                        </div>
                      ) : (
                        <span style={{ fontStyle: 'italic' }}>Not recorded (Missed)</span>
                      )}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', color: '#64748b' }}>
                      {log.mealRelation || '—'}
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span className={`badge ${isTaken ? 'badge-taken' : 'badge-missed'}`}>
                        {log.status}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
