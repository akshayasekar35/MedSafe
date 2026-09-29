import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Medications from './pages/Medications';
import AddMedication from './pages/AddMedication';
import MedicationDetails from './pages/MedicationDetails';
import Schedule from './pages/Schedule';
import SafetyAlerts from './pages/SafetyAlerts';
import InteractionDetails from './pages/InteractionDetails';
import MedicationHistory from './pages/MedicationHistory';
import Profile from './pages/Profile';

export default function App() {
  return (
    <Routes>
      {/* Public Authentication Routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Protected Application Routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/medications" element={<Medications />} />
          <Route path="/medications/add" element={<AddMedication />} />
          <Route path="/medications/:id" element={<MedicationDetails />} />
          <Route path="/medications/:id/edit" element={<AddMedication />} />
          <Route path="/schedule" element={<Schedule />} />
          <Route path="/safety-alerts" element={<SafetyAlerts />} />
          <Route path="/interactions/:id" element={<InteractionDetails />} />
          <Route path="/history" element={<MedicationHistory />} />
          <Route path="/profile" element={<Profile />} />
        </Route>
      </Route>

      {/* Fallback route */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
