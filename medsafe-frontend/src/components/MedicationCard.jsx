import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Pill, Calendar, Clock, Trash2, Edit3, Eye, PlusCircle } from 'lucide-react';

export default function MedicationCard({ medication, onDelete, onScheduleClick }) {
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);

  const handleDelete = () => {
    onDelete(medication.medicationId);
    setShowConfirmDelete(false);
  };

  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="card-header" style={{ alignItems: 'flex-start', marginBottom: '0.75rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a' }}>
              {medication.medicineName}
            </h3>
            <span className={`badge ${medication.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'}`}>
              {medication.status}
            </span>
          </div>
          {medication.activeIngredient && (
            <div style={{ fontSize: '0.85rem', color: '#0d9488', fontWeight: 600 }}>
              Active ingredient: {medication.activeIngredient}
            </div>
          )}
        </div>
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '0.4rem', fontSize: '0.875rem', color: '#475569', marginBottom: '1.25rem' }}>
        {medication.dosage && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontWeight: 600, color: '#334155' }}>Dosage:</span> {medication.dosage}
          </div>
        )}
        {medication.frequency && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock size={15} style={{ color: 'var(--text-muted)' }} />
            <span>{medication.frequency}</span>
          </div>
        )}
        {(medication.startDate || medication.endDate) && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            <Calendar size={14} />
            <span>
              {medication.startDate || 'Started'} — {medication.endDate || 'Ongoing'}
            </span>
          </div>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--border-color)', paddingTop: '0.85rem' }}>
        <div style={{ display: 'flex', gap: '0.35rem' }}>
          <Link 
            to={`/medications/${medication.medicationId}`} 
            className="btn btn-outline btn-sm"
            title="View Details"
          >
            <Eye size={15} />
            <span>View</span>
          </Link>
          <Link 
            to={`/medications/${medication.medicationId}/edit`} 
            className="btn btn-outline btn-sm"
            title="Edit Medication"
          >
            <Edit3 size={15} />
          </Link>
          <Link
            to={`/schedule?medicationId=${medication.medicationId}`}
            className="btn btn-outline btn-sm"
            title="Schedules"
          >
            <PlusCircle size={15} />
            <span>Schedule</span>
          </Link>
        </div>

        <div>
          {showConfirmDelete ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <button onClick={handleDelete} className="btn btn-danger btn-sm" style={{ padding: '0.3rem 0.6rem' }}>
                Confirm
              </button>
              <button onClick={() => setShowConfirmDelete(false)} className="btn btn-secondary btn-sm" style={{ padding: '0.3rem 0.6rem' }}>
                Cancel
              </button>
            </div>
          ) : (
            <button 
              onClick={() => setShowConfirmDelete(true)} 
              className="btn btn-outline btn-sm"
              style={{ color: 'var(--danger)', borderColor: '#fecaca' }}
              title="Delete Medication"
            >
              <Trash2 size={15} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
