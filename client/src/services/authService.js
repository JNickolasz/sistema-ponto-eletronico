import api from './api';

export const authService = {
  /**
   * Realiza login no backend Spring Boot
   * @param {string} usuario
   * @param {string} senha
   * @returns {Promise<{token: string, tipo: string, usuario: string, perfil: string}>}
   */
  async login(usuario, senha) {
    const data = await api.post('/auth/login', { usuario, senha });
    if (data.token) {
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify({
        usuario: data.usuario,
        perfil: data.perfil,
        tipo: data.tipo,
      }));
    }
    return data;
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getCurrentUser() {
    try {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },

  getToken() {
    return localStorage.getItem('token');
  },

  isAuthenticated() {
    return !!localStorage.getItem('token');
  },
};

export default authService;
