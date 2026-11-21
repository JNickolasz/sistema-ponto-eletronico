import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider, useTheme } from './context/ThemeContext';
import ProtectedRoute from './components/ProtectedRoute';

import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import TenantPortalSlider from './pages/TenantPortalSlider';

const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));
const RhDashboard = lazy(() => import('./pages/RhDashboard'));
const ColaboradorDashboard = lazy(() => import('./pages/ColaboradorDashboard'));
const GestorDashboard = lazy(() => import('./pages/GestorDashboard'));

function PageFallback() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--surface-page)] text-[var(--text-secondary)]">
      <div className="flex flex-col items-center gap-3">
        <div
          className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent"
          style={{ borderColor: 'var(--brand-600)', borderTopColor: 'transparent' }}
        />
        <span className="text-xs font-medium">Carregando módulo...</span>
      </div>
    </div>
  );
}

/**
 * Roteamento multi-tenant com preservação do Slider horizontal:
 * - Em subdomínio de empresa (ex.: vertice.localhost:5173):
 *   '/' , '/relogio' e '/login' compartilham a MESMA instância montada de TenantPortalSlider,
 *   garantindo transição horizontal fluida (slide) sem remontar a tela!
 * - No domínio raiz (ex.: localhost:5173):
 *   '/' exibe a Landing Page comercial e '/login' exibe o portal raiz (Encontrar Empresa / Admin SaaS).
 */
function AppRoutes() {
  const { hostSubdomain } = useTheme();

  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        {hostSubdomain ? (
          <>
            <Route path="/" element={<TenantPortalSlider />} />
            <Route path="/relogio" element={<TenantPortalSlider />} />
            <Route path="/login" element={<TenantPortalSlider />} />
          </>
        ) : (
          <>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
          </>
        )}

        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/rh"
          element={
            <ProtectedRoute allowedRoles={['RH']}>
              <RhDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/colaborador"
          element={
            <ProtectedRoute allowedRoles={['COLABORADOR']}>
              <ColaboradorDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/gestor"
          element={
            <ProtectedRoute allowedRoles={['GESTOR']}>
              <GestorDashboard />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
