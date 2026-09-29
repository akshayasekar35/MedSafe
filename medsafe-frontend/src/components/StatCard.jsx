import React from 'react';

export default function StatCard({ label, value, icon: Icon, color = '#0284c7', bgColor = '#e0f2fe' }) {
  return (
    <div className="stat-card">
      <div className="stat-icon" style={{ backgroundColor: bgColor, color }}>
        {Icon && <Icon size={24} />}
      </div>
      <div className="stat-info">
        <span className="stat-value">{value}</span>
        <span className="stat-label">{label}</span>
      </div>
    </div>
  );
}
