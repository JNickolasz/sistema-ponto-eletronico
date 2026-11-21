import React from 'react';
import { Navigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { Button, Card } from './ui/Primitives';

export function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role, loading, getDashboardPath } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--surface-page)] text-[var(--text-secondary)]">
        <div className="flex flex-col items-center gap-3">
          <div
            className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent"
            style={{ borderColor: 'var(--brand-600)', borderTopColor: 'transparent' }}
          />
          <p className="text-xs font-medium">Verificando autenticação...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--surface-page)] px-4 text-[var(--text-primary)]">
        <Card className="max-w-md w-full p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-red-50 text-red-600 dark:bg-red-950/50 dark:text-red-400">
            <ShieldAlert size={24} />
          </div>
          <h2 className="text-lg font-semibold tracking-tight">Acesso Restrito</h2>
          <p className="mt-2 text-sm text-[var(--text-secondary)]">
            Seu perfil atual (<span className="font-semibold text-[var(--text-primary)]">{role}</span>) não tem permissão para acessar este painel.
          </p>
          <div className="mt-6 flex justify-center">
            <Link to={getDashboardPath()}>
              <Button variant="primary">
                <ArrowLeft size={15} />
                Ir para meu painel ({role})
              </Button>
            </Link>
          </div>
        </Card>
      </div>
    );
  }

  return children;
}

export default ProtectedRoute;
