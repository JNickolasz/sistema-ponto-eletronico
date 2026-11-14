import api from './api';

export const apuracaoService = {
  consultar: async ({ funcionarioId, dataInicio, dataFim, mesReferencia }) => {
    const params = {};
    if (funcionarioId) params.funcionarioId = funcionarioId;
    if (dataInicio) params.dataInicio = dataInicio;
    if (dataFim) params.dataFim = dataFim;
    if (mesReferencia) params.mesReferencia = mesReferencia;

    const response = await api.get('/api/apuracao', { params });
    return response.data;
  },

  recalcular: async ({ funcionarioId, dataInicio, dataFim }) => {
    const response = await api.post('/api/apuracao/recalcular', {
      funcionarioId,
      dataInicio,
      dataFim,
    });
    return response.data;
  },
};

export default apuracaoService;
