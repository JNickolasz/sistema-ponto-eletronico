import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './hooks/useAuth';
import LoginPage from './pages/LoginPage';

import RhDashboard from './pages/RhDashboard';
import ColaboradorDashboard from './pages/ColaboradorDashboard';
import GestorDashboard from './pages/GestorDashboard';
import ProtectedRoute from './components/ProtectedRoute';

function RootRedirect() {
  const { isAuthenticated, loading, getDashboardPath } = useAuth();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-100">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Navigate to={getDashboardPath()} replace />;
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<LoginPage />} />

          {/* Painel do RH */}
          <Route
            path="/rh"
            element={
              <ProtectedRoute allowedRoles={['RH']}>
                <RhDashboard />
              </ProtectedRoute>
            }
          />

          {/* Painel do Colaborador */}
          <Route
            path="/colaborador"
            element={
              <ProtectedRoute allowedRoles={['COLABORADOR']}>
                <ColaboradorDashboard />
              </ProtectedRoute>
            }
          />

          {/* Painel do Gestor */}
          <Route
            path="/gestor"
            element={
              <ProtectedRoute allowedRoles={['GESTOR']}>
                <GestorDashboard />
              </ProtectedRoute>
            }
          />

          {/* Rota coringa */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

