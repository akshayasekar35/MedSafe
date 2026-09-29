import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Plus, ShieldCheck } from 'lucide-react';

export default function Navbar({ title }) {
  const location = useLocation();

  const getTitle = () => {
    if (title) return title;
    const p = location.pathname;
    if (p === '/') return 'Dashboard';
    if (p === '/medications') return 'Medications';
    if (p === '/medications/add') return 'Add Medication';
    if (p.startsWith('/medications/') && p.endsWith('/edit')) return 'Edit Medication';
    if (p.startsWith('/medications/')) return 'Medication Details';
    if (p === '/schedule') return 'Medication Schedule';
    if (p === '/safety-alerts') return 'Safety & Interaction Alerts';
    if (p.startsWith('/interactions/')) return 'Clinical Interaction Details';
    if (p === '/history') return 'Intake History';
    if (p === '/profile') return 'Patient Profile';
    return 'Medication Safety';
  };
  return (
    <header className="navbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <h1 className="navbar-title">{title}</h1>
        <span 
          style={{ 
            fontSize: '0.75rem', 
            backgroundColor: '#f0fdf4', 
            color: '#15803d', 
            padding: '0.2rem 0.6rem', 
            borderRadius: '9999px',
            border: '1px solid #bbf7d0',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.35rem',
            fontWeight: 600
          }}
        >
          <ShieldCheck size={13} />
          Safety Verified System
        </span>
      </div>

      <div className="navbar-actions">
        <Link to="/medications/add" className="btn btn-primary btn-sm">
          <Plus size={16} />
          <span>Add Medication</span>
        </Link>
      </div>
    </header>
  );
}
