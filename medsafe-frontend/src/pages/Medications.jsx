import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { medicationService } from '../services/medicationService';
import MedicationCard from '../components/MedicationCard';
import { Pill, Plus, Search, AlertTriangle, RefreshCw } from 'lucide-react';

export default function Medications() {
  const [medications, setMedications] = useState([]);
  const [duplicates, setDuplicates] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [medsList, dupesList] = await Promise.all([
        medicationService.getAll(),
        medicationService.getDuplicates()
      ]);
      setMedications(medsList);
      setDuplicates(dupesList);
    } catch (err) {
      setError(err.message || 'Failed to load medications.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDelete = async (medicationId) => {
    try {
      await medicationService.delete(medicationId);
      setMedications(prev => prev.filter(m => m.medicationId !== medicationId));
      // Refresh duplicates in case the deleted med removed a duplicate
      const dupes = await medicationService.getDuplicates();
      setDuplicates(dupes);
    } catch (err) {
      alert(err.message || 'Failed to delete medication');
    }
  };

  const filteredMeds = medications.filter(med => {
    const matchesSearch = med.medicineName.toLowerCase().includes(search.toLowerCase()) ||
      (med.activeIngredient && med.activeIngredient.toLowerCase().includes(search.toLowerCase()));
    const matchesStatus = statusFilter === 'ALL' || med.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  if (loading) {
    return (
      <div className="state-container">
        <RefreshCw size={36} className="animate-spin" style={{ color: 'var(--primary)', marginBottom: '1rem' }} />
        <h3 className="state-title">Loading Medications...</h3>
      </div>
    );
  }

  if (error) {
    return (
      <div className="state-container">
        <AlertTriangle size={36} style={{ color: 'var(--danger)', marginBottom: '1rem' }} />
        <h3 className="state-title">Unable to Load Medications</h3>
        <p className="state-description">{error}</p>
        <button onClick={loadData} className="btn btn-primary btn-sm" style={{ marginTop: '0.75rem' }}>
          <RefreshCw size={15} />
          <span>Try Again</span>
        </button>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a' }}>
            My Prescribed Medications
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Manage your prescriptions, active ingredients, and schedules.
          </p>
        </div>

        <Link to="/medications/add" className="btn btn-primary">
          <Plus size={18} />
          <span>Add New Medication</span>
        </Link>
      </div>

      {/* Duplicate Medication Warnings Banner */}
      {duplicates.length > 0 && (
        <div style={{ marginBottom: '1.75rem' }}>
          {duplicates.map((dup, idx) => (
            <div key={idx} className="alert-box warning" style={{ marginBottom: '0.85rem' }}>
              <AlertTriangle size={24} style={{ flexShrink: 0, marginTop: '2px', color: '#b45309' }} />
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '1rem', color: '#92400e', marginBottom: '0.35rem' }}>
                  Safety Alert: Duplicate Active Ingredient Detected ({dup.activeIngredient})
                </div>
                <p style={{ fontSize: '0.875rem', marginBottom: '0.5rem', lineHeight: '1.5' }}>
                  {dup.warningMessage}
                </p>
                <div style={{ backgroundColor: 'rgba(255,255,255,0.7)', padding: '0.65rem 0.85rem', borderRadius: 'var(--radius-sm)', border: '1px solid #fcd34d', marginBottom: '0.5rem' }}>
                  <span style={{ fontWeight: 600, fontSize: '0.8rem', color: '#78350f' }}>Affected Medicines: </span>
                  <span style={{ fontSize: '0.85rem', color: '#1e293b' }}>
                    {dup.medications.map(m => `${m.medicineName} (${m.dosage || 'dosage not specified'})`).join(', ')}
                  </span>
                </div>
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#92400e' }}>
                  Notice: {dup.clinicalRecommendation}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Search and Filter Bar */}
      <div className="card" style={{ padding: '1rem 1.25rem', marginBottom: '1.75rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: '240px', position: 'relative' }}>
            <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Search by medicine name or active ingredient..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ paddingLeft: '2.5rem' }}
            />
          </div>

          <div style={{ width: '180px' }}>
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active Only</option>
              <option value="INACTIVE">Inactive Only</option>
            </select>
          </div>
        </div>
      </div>

      {/* Medication Cards Grid */}
      {filteredMeds.length === 0 ? (
        <div className="state-container">
          <Pill size={44} style={{ color: '#cbd5e1', marginBottom: '1rem' }} />
          <h3 className="state-title">
            {medications.length === 0 ? 'No Medications Added Yet' : 'No matching medications found'}
          </h3>
          <p className="state-description">
            {medications.length === 0 
              ? 'Add your first prescribed medication to start tracking doses and monitoring interactions.'
              : 'Try adjusting your search or status filter.'}
          </p>
          {medications.length === 0 && (
            <Link to="/medications/add" className="btn btn-primary btn-sm">
              <Plus size={16} />
              <span>Add Prescribed Medication</span>
            </Link>
          )}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
          {filteredMeds.map((med) => (
            <MedicationCard
              key={med.medicationId}
              medication={med}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}
