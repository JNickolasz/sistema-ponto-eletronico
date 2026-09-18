import api from './api';

export const localTrabalhoService = {
  /**
   * Lista as empresas cadastradas no sistema
   */
  async listarEmpresas() {
    return await api.get('/empresa');
  },

  /**
   * Cadastra uma nova empresa via API administrativa (POST /api/v1/admin/empresas)
   * @param {{razaoSocial: string, subdominio: string, cnpj: string, endereco: string, logoUrl?: string}} dados
   * @param {string} [adminKey]
   */
  async cadastrarEmpresaAdmin(dados, adminKey = 'admin123') {
    return await api.post('/admin/empresas', dados, {
      headers: { 'X-Admin-Key': adminKey },
    });
  },

  /**
   * Cadastra um novo local de trabalho vinculado à empresa
   * @param {{empresaId: string, nome: string, endereco: string, municipio?: string, uf?: string, raioMetros?: number, ipEsperado?: string, latitude?: number, longitude?: number}} dados
   */
  async cadastrarLocalTrabalho(dados) {
    return await api.post('/v1/locais-trabalho', dados);
  },

  /**
   * Lista os locais de trabalho (opcionalmente filtrados por empresa)
   * @param {string} [empresaId]
   */
  async listarLocaisTrabalho(empresaId) {
    const query = empresaId ? `?empresaId=${empresaId}` : '';
    return await api.get(`/v1/locais-trabalho${query}`);
  },
};

export default localTrabalhoService;
