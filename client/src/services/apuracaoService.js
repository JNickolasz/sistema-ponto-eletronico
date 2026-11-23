import api, { API_BASE } from './api';

const BACKEND_ROOT = API_BASE.replace(/\/api\/v1\/?$/, '');

export const apuracaoService = {
  consultar: async ({ funcionarioId, dataInicio, dataFim, mesReferencia }) => {
    const params = new URLSearchParams();
    if (funcionarioId) params.append('funcionarioId', funcionarioId);
    if (dataInicio) params.append('dataInicio', dataInicio);
    if (dataFim) params.append('dataFim', dataFim);
    if (mesReferencia) params.append('mesReferencia', mesReferencia);

    const qs = params.toString() ? `?${params.toString()}` : '';
    const response = await api.get(`${BACKEND_ROOT}/api/apuracao${qs}`);
    return response?.data || response;
  },

  recalcular: async ({ funcionarioId, dataInicio, dataFim }) => {
    const response = await api.post(`${BACKEND_ROOT}/api/apuracao/recalcular`, {
      funcionarioId,
      dataInicio,
      dataFim,
    });
    return response?.data || response;
  },
};

export default apuracaoService;
