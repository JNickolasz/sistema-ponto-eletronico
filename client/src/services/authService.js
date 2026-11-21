import api from './api';

export const authService = {
  /**
   * Realiza login no backend Spring Boot
   * @param {string} usuario
   * @param {string} senha
   * @returns {Promise<{token: string, tipo: string, usuario: string, perfil: string}>}
   */
  async login(usuario, senha) {
    const trimmedUsuario = (usuario || '').trim();
    const trimmedSenha = (senha || '').trim();

    if (!trimmedUsuario || !trimmedSenha) {
      throw new Error('Usuário e senha são obrigatórios');
    }

    // Verificação de Acesso Admin da Plataforma via Chave Mestra (validada exclusivamente no backend)
    if (trimmedUsuario.toLowerCase() === 'admin') {
      try {
        await api.get('/admin/empresas', {
          headers: { 'X-Admin-Key': trimmedSenha },
        });
      } catch (err) {
        if (err?.status === 401 || err?.status === 403) {
          throw new Error('Chave de Administrador inválida!');
        }
        throw new Error(err?.message || 'Não foi possível validar a chave de administrador no servidor.');
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
