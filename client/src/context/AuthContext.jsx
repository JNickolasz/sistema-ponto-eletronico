import React, { useState } from 'react';
import authService from '../services/authService';
import { AuthContext } from './authContextInstance';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => authService.getCurrentUser());
  const [token, setToken] = useState(() => authService.getToken());
  const [loading] = useState(false);

  const login = async (usuario, senha) => {
    const data = await authService.login(usuario, senha);
    setUser({
      usuario: data.usuario,
      perfil: data.perfil,
      tipo: data.tipo,
    });
    setToken(data.token);
    return data;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    setToken(null);
  };

  const getDashboardPath = (perfil = user?.perfil) => {
    switch (perfil) {
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
