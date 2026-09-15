import api from './api';

export const localTrabalhoService = {
  /**
   * Lista as empresas cadastradas no sistema
   */
  async listarEmpresas() {
    return await api.get('/empresa');
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
