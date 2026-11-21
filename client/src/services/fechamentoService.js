import api from './api';

export const fechamentoService = {
  async consultarPrevia(competencia) {
    const query = competencia ? `?competencia=${encodeURIComponent(competencia)}` : '';
    return await api.get(`/fechamentos/previa${query}`);
  },

  async fecharPeriodo({ competencia, confirmarComPendencias = false, observacao = '' }) {
    return await api.post('/fechamentos/fechar', {
      competencia,
      confirmarComPendencias,
      observacao,
    });
  },

  async reabrirPeriodo({ competencia, motivo }) {
    return await api.post('/fechamentos/reabrir', {
      competencia,
      motivo,
    });
  },

  async listarFechamentos() {
    return await api.get('/fechamentos');
  },
};

export default fechamentoService;
