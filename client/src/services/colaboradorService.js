import api from './api';

export const colaboradorService = {
  /**
   * Consulta os dados do painel do Colaborador
   */
  async getPainel() {
    return await api.get('/colaborador/painel');
  },

  /**
   * Consulta o espelho de ponto do colaborador
   * @param {number|string} id ID do funcionário
   */
  async getEspelho(id) {
    return await api.get(`/colaborador/${id}/espelho`);
  },
};

export default colaboradorService;
