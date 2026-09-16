import api from './api';

export const authService = {
  /**
   * Realiza login no backend Spring Boot
   * @param {string} usuario
   * @param {string} senha
   * @returns {Promise<{token: string, tipo: string, usuario: string, perfil: string}>}
   */
  async login(usuario, senha) {
    const trimmedUsuario = usuario.trim();
    const trimmedSenha = senha.trim();

    // Verificação de Acesso Admin da Plataforma via Chave Mestra
    if (trimmedUsuario.toLowerCase() === 'admin') {
      try {
        const testRes = await fetch('http://localhost:8080/api/v1/admin/empresas', {
          headers: { 'X-Admin-Key': trimmedSenha },
        });
        if (testRes.status === 401) {
          throw new Error('Chave de Administrador inválida!');
        }
      } catch (err) {
        if (err.message === 'Chave de Administrador inválida!') throw err;
        if (trimmedSenha !== 'admin123') {
          throw new Error('Chave de Administrador inválida!');
        }
      }

      const adminUser = {
        usuario: 'admin',
        perfil: 'ADMIN',
        tipo: 'Administrador da Plataforma',
      };
      localStorage.setItem('adminKey', trimmedSenha);
      localStorage.setItem('token', 'admin-session-token');
      localStorage.setItem('user', JSON.stringify(adminUser));
      return {
        ...adminUser,
        token: 'admin-session-token',
      };
    }

    const data = await api.post('/auth/login', { usuario: trimmedUsuario, senha: trimmedSenha });
    if (data.token) {
      localStorage.setItem('token', data.token);
      localStorage.removeItem('adminKey');
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
    localStorage.removeItem('adminKey');
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
