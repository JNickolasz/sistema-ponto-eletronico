import api from './api';

export const bancoHorasService = {
  /**
   * Consulta o extrato individual de banco de horas com saldo inicial, progressivo e final.
   */
  async consultarExtrato({ funcionarioId, dataInicio, dataFim, mesReferencia } = {}) {
    const params = new URLSearchParams();
    if (funcionarioId) params.append('funcionarioId', funcionarioId);
    if (dataInicio) params.append('dataInicio', dataInicio);
    if (dataFim) params.append('dataFim', dataFim);
    if (mesReferencia) params.append('mesReferencia', mesReferencia);

    const qs = params.toString() ? `?${params.toString()}` : '';
    const res = await api.get(`/banco-horas/extrato${qs}`);
    return res?.data || res;
  },

  /**
   * Registra uma compensação de horas (abatimento com motivo obrigatório).
   * @param {{ funcionarioId: string, dataReferencia: string, minutos: number, motivo: string }} dados
   */
  async registrarCompensacao(dados) {
    const res = await api.post('/banco-horas/compensacoes', dados);
    return res?.data || res;
  },

  /**
   * Consulta o relatório consolidado de saldos da equipe para o Gestor ou RH.
   */
  async consultarSaldoEquipe({ dataInicio, dataFim, mesReferencia } = {}) {
    const params = new URLSearchParams();
    if (dataInicio) params.append('dataInicio', dataInicio);
    if (dataFim) params.append('dataFim', dataFim);
    if (mesReferencia) params.append('mesReferencia', mesReferencia);

    const qs = params.toString() ? `?${params.toString()}` : '';
    const res = await api.get(`/banco-horas/equipe${qs}`);
    return res?.data || res;
  },
};

export default bancoHorasService;
