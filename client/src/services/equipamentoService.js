import api from './api';

export const equipamentoService = {
  /**
   * Lista todos os equipamentos cadastrados ou filtrados por local/empresa
   * @param {{ localTrabalhoId?: string, empresaId?: string }} [filtros]
   */
  async listarEquipamentos(filtros = {}) {
    const params = new URLSearchParams();
    if (filtros.localTrabalhoId) params.append('localTrabalhoId', filtros.localTrabalhoId);
    if (filtros.empresaId) params.append('empresaId', filtros.empresaId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return await api.get(`/v1/equipamentos${query}`);
  },

  /**
   * Cadastra um novo equipamento (RELOGIO ou ESTACAO)
   * @param {{ tipo: 'RELOGIO' | 'ESTACAO', identificacao: string, localTrabalhoId: string, numFabricacao?: string | null }} dados
   */
  async cadastrarEquipamento(dados) {
    return await api.post('/v1/equipamentos', dados);
  },

  /**
   * Altera o status do equipamento (ATIVO / INATIVO) sem exclusão física
   * @param {string} id
   * @param {'ATIVO' | 'INATIVO'} status
   */
  async alterarStatus(id, status) {
    return await api.patch(`/v1/equipamentos/${id}/status?status=${status}`);
  },

  /**
   * Consulta os dados de um equipamento específico pelo ID
   * @param {string} id
   */
  async buscarPorId(id) {
    return await api.get(`/v1/equipamentos/${id}`);
  },
};

export default equipamentoService;
