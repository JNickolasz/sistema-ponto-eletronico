import api from './api';

function adminHeaders() {
  const key = localStorage.getItem('adminKey') || '';
  return { headers: { 'X-Admin-Key': key } };
}

export const adminService = {
  async listarEmpresas() {
    return await api.get('/admin/empresas', adminHeaders());
  },

  async cadastrarEmpresa(dados) {
    return await api.post('/admin/empresas', dados, adminHeaders());
  },

  async obterEstatisticas() {
    return await api.get('/admin/estatisticas', adminHeaders());
  },

  async obterDetalhesEmpresa(empresaId) {
    return await api.get(`/admin/empresas/${empresaId}/detalhes`, adminHeaders());
  },

  async atualizarBranding(empresaId, { logoUrl, corPrimaria, corSecundaria }) {
    return await api.patch(
      `/admin/empresas/${empresaId}/branding`,
      { logoUrl, corPrimaria, corSecundaria },
      adminHeaders()
    );
  },

  async cadastrarFeriadoGlobal(dto) {
    return await api.post('/admin/feriados', dto, adminHeaders());
  },
};

export default adminService;
