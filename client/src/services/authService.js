import api from './api';

export const authService = {
  /**
   * Realiza login no backend Spring Boot
   * @param {string} usuario
   * @param {string} senha
   * @returns {Promise<{token: string, tipo: string, usuario: string, perfil: string}>}
   */
  async login(usuario, senha, subdominio = '') {
    const trimmedUsuario = (usuario || '').trim();
    const trimmedSenha = (senha || '').trim();
    const trimmedSubdominio = (subdominio || '').trim().toLowerCase();

    if (!trimmedUsuario || !trimmedSenha) {
      throw new Error('Usuário e senha são obrigatórios');
    }

    // Verificação de Acesso Admin da Plataforma via Chave Mestra (validada exclusivamente no backend)
    if (trimmedUsuario.toLowerCase() === 'admin' && !trimmedSubdominio) {
      try {
        await api.get('/admin/empresas', {
          headers: { 'X-Admin-Key': trimmedSenha },
        });
      } catch (err) {
        if (err?.message && (err?.status === 401 || err?.status === 403 || err?.status === 429)) {
          throw new Error(err.message);
        }
        if (err?.status === 401 || err?.status === 403) {
          throw new Error('Chave de Administrador inválida!');
        }
        throw new Error(err?.message || 'Não foi possível validar a chave de administrador no servidor.');
      }

      const adminUser = {
        usuario: 'admin',
        perfil: 'ADMIN',
        tipo: 'Administrador da Plataforma',
        subdominio: null,
      };
      localStorage.setItem('adminKey', trimmedSenha);
      localStorage.setItem('token', 'admin-session-token');
      localStorage.setItem('user', JSON.stringify(adminUser));
      return {
        ...adminUser,
        token: 'admin-session-token',
      };
    }

    const payload = { usuario: trimmedUsuario, senha: trimmedSenha };
    if (trimmedSubdominio) {
      payload.subdominio = trimmedSubdominio;
    }

    const data = await api.post('/auth/login', payload, trimmedSubdominio
      ? { headers: { 'X-Tenant-Subdomain': trimmedSubdominio } }
      : undefined
    );
    if (data.token) {
      localStorage.setItem('token', data.token);
      localStorage.removeItem('adminKey');
      localStorage.setItem('user', JSON.stringify({
        usuario: data.usuario,
        perfil: data.perfil,
        tipo: data.tipo,
        subdominio: trimmedSubdominio || null,
      }));
    }
    return { ...data, subdominio: trimmedSubdominio || null };
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
