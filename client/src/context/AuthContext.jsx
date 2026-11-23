import React, { useEffect, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import authService from '../services/authService';
import { AuthContext } from './authContextInstance';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => authService.getCurrentUser());
  const [token, setToken] = useState(() => authService.getToken());
  const [loading] = useState(false);
  const lastRevokedToastRef = useRef(0);

  const login = async (usuario, senha, subdominio = '') => {
    const data = await authService.login(usuario, senha, subdominio);
    setUser({
      usuario: data.usuario,
      perfil: data.perfil,
      tipo: data.tipo,
      subdominio: data.subdominio || null,
    });
    setToken(data.token);
    return data;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    setToken(null);
  };

  useEffect(() => {
    const handleSessionRevoked = (event) => {
      authService.logout();
      setUser(null);
      setToken(null);

      const agora = Date.now();
      if (agora - lastRevokedToastRef.current > 3000) {
        lastRevokedToastRef.current = agora;
        toast.error(
          event?.detail?.message || 'Seu acesso foi revogado ou a sessão expirou. Faça login novamente.'
        );
      }
    };

    window.addEventListener('auth:session-revoked', handleSessionRevoked);
    return () => window.removeEventListener('auth:session-revoked', handleSessionRevoked);
  }, []);

  const getDashboardPath = (perfil = user?.perfil) => {
    switch (perfil) {
      case 'ADMIN':
        return '/admin';
      case 'RH':
        return '/rh';
      case 'GESTOR':
        return '/gestor';
      case 'COLABORADOR':
        return '/colaborador';
      default:
        return '/login';
    }
  };

  const value = {
    user,
    token,
    role: user?.perfil,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    logout,
    getDashboardPath,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthProvider;
