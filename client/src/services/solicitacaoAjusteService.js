import api from './api';

export const solicitacaoAjusteService = {
  /**
   * Colaborador abre uma solicitação de ajuste de ponto
   * @param {{ dataAlvo: string, horarioPretendido: string, tipoRegistro: 'ENTRADA' | 'SAIDA', motivo: string }} dados
   */
  async abrirSolicitacao(dados) {
    return await api.post('/solicitacoes-ajuste', dados);
  },

  /**
   * Colaborador consulta seu histórico de solicitações
   */
  async listarMinhas() {
    return await api.get('/solicitacoes-ajuste/minhas');
  },

  /**
   * Gestor consulta as solicitações da sua equipe (com ou sem filtro de status)
   * @param {'PENDENTE' | 'DEFERIDO' | 'INDEFERIDO'} [status]
   */
  async listarEquipe(status) {
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    return await api.get(`/solicitacoes-ajuste/equipe${query}`);
  },

  /**
   * Gestor defere ou indefere uma solicitação com justificativa
   * @param {string} id
   * @param {{ status: 'DEFERIDO' | 'INDEFERIDO', justificativaGestor: string }} dados
   */
  async analisarSolicitacao(id, dados) {
    return await api.post(`/solicitacoes-ajuste/${id}/analisar`, dados);
  },
};

export default solicitacaoAjusteService;
