import api from './api';

export const jornadaService = {
  async listarJornadas() {
    return await api.get('/jornadas');
  },

  async cadastrarJornada(dados) {
    return await api.post('/jornadas', dados);
  },

  async buscarJornadaPorId(id) {
    if (!id) return null;
    return await api.get(`/jornadas/${encodeURIComponent(id)}`);
  },

  async listarEscalas() {
    return await api.get('/escalas');
  },

  async cadastrarEscala(dados) {
    return await api.post('/escalas', dados);
  },

  async buscarEscalaPorId(id) {
    if (!id) return null;
    return await api.get(`/escalas/${encodeURIComponent(id)}`);
  },

  async vincularRegime(dados) {
    const payload = {
      funcionarioId: dados.funcionarioId?.trim(),
      tipoRegime: dados.tipoRegime,
      jornadaId: dados.tipoRegime === 'JORNADA' && dados.jornadaId ? dados.jornadaId : null,
      escalaId: dados.tipoRegime === 'ESCALA' && dados.escalaId ? dados.escalaId : null,
      dataInicioVigencia: dados.dataInicioVigencia,
      dataFimVigencia: dados.dataFimVigencia ? dados.dataFimVigencia : null,
    };
    return await api.post('/regimes-trabalho/vincular', payload);
  },

  async listarRegimesFuncionario(funcionarioId) {
    if (!funcionarioId || String(funcionarioId).trim() === '') return [];
    return await api.get(`/regimes-trabalho/funcionario/${encodeURIComponent(funcionarioId)}`);
  },

  async obterHorarioPrevisto(funcionarioId, data) {
    if (!funcionarioId || String(funcionarioId).trim() === '') return null;
    const query = data
      ? `?funcionarioId=${encodeURIComponent(funcionarioId)}&data=${encodeURIComponent(data)}`
      : `?funcionarioId=${encodeURIComponent(funcionarioId)}`;
    return await api.get(`/regimes-trabalho/horario-previsto${query}`);
  },
};

export default jornadaService;
