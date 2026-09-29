import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  LayoutDashboard, 
  Pill, 
  CalendarClock, 
  ShieldAlert, 
  History, 
  UserCircle, 
  LogOut 
} from 'lucide-react';

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/medications', label: 'Medications', icon: Pill },
    { to: '/schedule', label: 'Schedule', icon: CalendarClock },
    { to: '/safety-alerts', label: 'Safety Alerts', icon: ShieldAlert },
    { to: '/history', label: 'Medication History', icon: History },
    { to: '/profile', label: 'Profile', icon: UserCircle },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-brand-icon">
          <Pill size={22} />
        </div>
        <div>
          <div className="sidebar-brand-title">MedSafe</div>
          <div className="sidebar-brand-subtitle">Interaction &amp; Safety Assistant</div>
        </div>
      </div>

      <nav className="sidebar-nav">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              <Icon size={18} />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="user-badge">
          <div className="user-avatar">
            {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="user-info">
            <span className="user-name">{user?.name || 'Patient'}</span>
            <span className="user-email">{user?.email || ''}</span>
          </div>
        </div>
        <button 
          onClick={handleLogout} 
          className="btn-logout" 
          title="Log out"
          aria-label="Log out"
        >
          <LogOut size={18} />
        </button>
      </div>
    </aside>
  );
}
