import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { medicationService } from '../services/medicationService';
import { Pill, ArrowLeft, Save, AlertCircle } from 'lucide-react';

export default function AddMedication() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditMode = Boolean(id);

  const [formData, setFormData] = useState({
    medicineName: '',
    activeIngredient: '',
    dosage: '',
    frequency: '',
    startDate: '',
    endDate: '',
    status: 'ACTIVE'
  });

  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(isEditMode);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isEditMode) {
      medicationService.getById(id)
        .then((data) => {
          setFormData({
            medicineName: data.medicineName || '',
            activeIngredient: data.activeIngredient || '',
            dosage: data.dosage || '',
            frequency: data.frequency || '',
            startDate: data.startDate || '',
            endDate: data.endDate || '',
            status: data.status || 'ACTIVE'
          });
        })
        .catch((err) => {
          setError(err.message || 'Failed to fetch medication details.');
        })
        .finally(() => {
          setInitialLoading(false);
        });
    }
  }, [id, isEditMode]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!formData.medicineName.trim()) {
      setError('Medicine name is required.');
      return;
    }

    if (formData.startDate && formData.endDate && formData.endDate < formData.startDate) {
      setError('Prescription end date cannot be earlier than start date.');
      return;
    }

    try {
      setLoading(true);
      const payload = {
        medicineName: formData.medicineName.trim(),
        activeIngredient: formData.activeIngredient.trim() || null,
        dosage: formData.dosage.trim() || null,
        frequency: formData.frequency.trim() || null,
        startDate: formData.startDate || null,
        endDate: formData.endDate || null,
        status: formData.status
      };

      if (isEditMode) {
        await medicationService.update(id, payload);
      } else {
        await medicationService.create(payload);
      }

      navigate('/medications');
    } catch (err) {
      setError(err.message || 'Failed to save medication.');
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading) {
    return (
      <div className="state-container">
        <p>Loading medication details...</p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '720px', margin: '0 auto' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <Link to="/medications" className="btn btn-outline btn-sm" style={{ display: 'inline-flex', marginBottom: '0.75rem' }}>
          <ArrowLeft size={16} />
          <span>Back to Medications</span>
        </Link>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a' }}>
          {isEditMode ? 'Edit Prescribed Medication' : 'Add Prescribed Medication'}
        </h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Provide the medicine details as indicated on your prescription or packaging.
        </p>
      </div>

      <div className="card" style={{ padding: '2rem' }}>
        {error && (
          <div className="alert-box danger" style={{ marginBottom: '1.5rem' }}>
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Medicine Name <span style={{ color: 'var(--danger)' }}>*</span>
            </label>
            <input
              type="text"
              name="medicineName"
              placeholder="e.g. Coumadin, Tylenol, Cardicor, Amoxicillin"
              value={formData.medicineName}
              onChange={handleChange}
              required
            />
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginTop: '0.25rem' }}>
              The brand name or packaging name of your medication.
            </span>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Active Ingredient
            </label>
            <input
              type="text"
              name="activeIngredient"
              placeholder="e.g. Warfarin, Acetaminophen, Bisoprolol"
              value={formData.activeIngredient}
              onChange={handleChange}
            />
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginTop: '0.25rem' }}>
              Crucial for accurate drug-to-drug interaction and duplicate therapy detection.
            </span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                Dosage
              </label>
              <input
                type="text"
                name="dosage"
                placeholder="e.g. 5mg, 500mg, 10ml"
                value={formData.dosage}
                onChange={handleChange}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                Frequency
              </label>
              <input
                type="text"
                name="frequency"
                placeholder="e.g. Once daily, Twice daily with food"
                value={formData.frequency}
                onChange={handleChange}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                Start Date
              </label>
              <input
                type="date"
                name="startDate"
                value={formData.startDate}
                onChange={handleChange}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                End Date (Optional)
              </label>
              <input
                type="date"
                name="endDate"
                value={formData.endDate}
                onChange={handleChange}
              />
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Prescription Status
            </label>
            <select name="status" value={formData.status} onChange={handleChange}>
              <option value="ACTIVE">ACTIVE (Currently Taking)</option>
              <option value="INACTIVE">INACTIVE (Discontinued / Finished Course)</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
            <button
              type="button"
              onClick={() => navigate('/medications')}
              className="btn btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
            >
              <Save size={18} />
              <span>{loading ? 'Saving...' : isEditMode ? 'Update Medication' : 'Save Medication'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
