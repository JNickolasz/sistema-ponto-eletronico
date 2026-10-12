import api from './api';
import batidaService from './batidaService';

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
    if (!id || String(id).trim() === '') {
      throw new Error('ID do colaborador é obrigatório para consultar o espelho');
    }
    return await api.get(`/colaborador/${encodeURIComponent(String(id).trim())}/espelho`);
  },

  /**
   * Registra uma batida de ponto
   */
  async registrarBatida(dados) {
    return await batidaService.registrarBatida(dados);
  },

  /**
   * Lista o histórico de batidas do funcionário
   */
  async listarBatidas(id) {
    return await batidaService.listarBatidasPorFuncionario(id);
  },
};

export default colaboradorService;
