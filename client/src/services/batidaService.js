import api from './api';

export const batidaService = {
  /**
   * Registra uma nova batida de ponto para o colaborador.
   * Não envia dataHora no body (regra antifraude - a data/hora é gerada no backend).
   *
   * @param {{
   *   funcionarioId: string,
   *   tipoRegistro: 'ENTRADA' | 'SAIDA',
   *   equipamentoId?: string | null,
   *   latitude?: number | null,
   *   longitude?: number | null,
   *   ip?: string | null
   * }} dados
   * @returns {Promise<{
   *   id: string,
   *   nsr: number,
   *   nomeEmpresa: string,
   *   cnpjEmpresa: string,
   *   nomeFuncionario: string,
   *   cpfFuncionario: string,
   *   dataHoraBatida: string,
   *   tipoRegistro: 'ENTRADA' | 'SAIDA',
   *   tipoOrigem: string
   * }>}
   */
  async registrarBatida(dados) {
    if (!dados || !dados.funcionarioId) {
      throw new Error('Identificador do colaborador é obrigatório para registrar ponto');
    }
    const payload = {
      funcionarioId: String(dados.funcionarioId).trim(),
      tipoRegistro: dados.tipoRegistro || 'ENTRADA',
      equipamentoId: dados.equipamentoId || null,
      latitude: dados.latitude != null && dados.latitude !== '' ? Number(dados.latitude) : null,
      longitude: dados.longitude != null && dados.longitude !== '' ? Number(dados.longitude) : null,
      ip: dados.ip?.trim() || null,
    };

    return await api.post('/batidas', payload);
  },

  /**
   * Lista o histórico de batidas de um funcionário ordenado por dataHora decrescente.
   *
   * @param {string} funcionarioId UUID do colaborador
   * @returns {Promise<Array<{
   *   id: string,
   *   nsr: number,
   *   nomeEmpresa: string,
   *   cnpjEmpresa: string,
   *   nomeFuncionario: string,
   *   cpfFuncionario: string,
   *   dataHoraBatida: string,
   *   tipoRegistro: 'ENTRADA' | 'SAIDA',
   *   tipoOrigem: string
   * }>>}
   */
  async listarBatidasPorFuncionario(funcionarioId) {
    if (!funcionarioId || String(funcionarioId).trim() === '') return [];
    const cleanId = String(funcionarioId).trim();
    return await api.get(`/funcionarios/${encodeURIComponent(cleanId)}/batidas`);
  },

  async listarBatidasOrfas(status = 'ORFA_PENDENTE') {
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    return await api.get(`/batidas/orfas${query}`);
  },

  async vincularBatidaOrfa(batidaId, { funcionarioId, motivo }) {
    if (!batidaId) {
      throw new Error('ID da batida órfã é obrigatório');
    }
    if (!funcionarioId) {
      throw new Error('Selecione um colaborador para vincular');
    }
    if (!motivo || !motivo.trim()) {
      throw new Error('O motivo do vínculo é obrigatório');
    }
    return await api.post(`/batidas/${encodeURIComponent(batidaId)}/vincular`, {
      funcionarioId,
      motivo: motivo.trim(),
    });
  },
};

export default batidaService;
