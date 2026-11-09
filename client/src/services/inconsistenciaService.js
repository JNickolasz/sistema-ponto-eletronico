import api from './api';

export const inconsistenciaService = {
  async listarInconsistencias(filtros = {}) {
    const params = new URLSearchParams();
    if (filtros.tipo) params.append('tipo', filtros.tipo);
    if (filtros.situacao) params.append('situacao', filtros.situacao);
    if (filtros.funcionarioId) params.append('funcionarioId', filtros.funcionarioId);
    if (filtros.dataInicio) params.append('dataInicio', filtros.dataInicio);
    if (filtros.dataFim) params.append('dataFim', filtros.dataFim);
    if (filtros.mesReferencia) params.append('mesReferencia', filtros.mesReferencia);

    const qs = params.toString();
    const url = `/inconsistencias${qs ? `?${qs}` : ''}`;
    return await api.get(url);
  },

  async obterPainel(filtros = {}) {
    const params = new URLSearchParams();
    if (filtros.dataInicio) params.append('dataInicio', filtros.dataInicio);
    if (filtros.dataFim) params.append('dataFim', filtros.dataFim);
    if (filtros.mesReferencia) params.append('mesReferencia', filtros.mesReferencia);

    const qs = params.toString();
    const url = `/inconsistencias/painel${qs ? `?${qs}` : ''}`;
    return await api.get(url);
  },

  async tratarInconsistencia(id, payload) {
    if (!id) throw new Error('ID da inconsistência é obrigatório');
    return await api.post(`/inconsistencias/${encodeURIComponent(id)}/tratar`, payload);
  },

  async fecharPeriodo(payload) {
    return await api.post('/inconsistencias/fechamento', payload);
  },

  async listarFechamentos() {
    return await api.get('/inconsistencias/fechamentos');
  },
};

export default inconsistenciaService;
