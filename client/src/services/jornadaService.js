import api from './api';

export const jornadaService = {
  async listarJornadas() {
    return await api.get('/jornadas');
  },

  async cadastrarJornada(dados) {
    return await api.post('/jornadas', dados);
  },

  async buscarJornadaPorId(id) {
    return await api.get(`/jornadas/${id}`);
  },

  async listarEscalas() {
    return await api.get('/escalas');
  },

  async cadastrarEscala(dados) {
    return await api.post('/escalas', dados);
  },

  async buscarEscalaPorId(id) {
    return await api.get(`/escalas/${id}`);
  },

  async vincularRegime(dados) {
    return await api.post('/regimes-trabalho/vincular', dados);
  },

  async listarRegimesFuncionario(funcionarioId) {
    return await api.get(`/regimes-trabalho/funcionario/${funcionarioId}`);
  },

  async obterHorarioPrevisto(funcionarioId, data) {
    const query = data ? `?funcionarioId=${funcionarioId}&data=${data}` : `?funcionarioId=${funcionarioId}`;
    return await api.get(`/regimes-trabalho/horario-previsto${query}`);
  },
};

export default jornadaService;
