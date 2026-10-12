import api from './api';

export const feriadoService = {
  /**
   * Cadastra um novo feriado (Nacional, Estadual ou Municipal)
   * @param {{
   *   descricao: string,
   *   data: string,
   *   alcance: 'NACIONAL'|'ESTADUAL'|'MUNICIPAL',
   *   uf?: string,
   *   municipio?: string,
   *   empresaId?: string
   * }} dados
   */
  async cadastrarFeriado(dados) {
    return await api.post('/v1/feriado', dados);
  },

  /**
   * Inativa um feriado por ID (inativação lógica)
   * @param {string} id UUID do feriado
   */
  async inativarFeriado(id) {
    return await api.delete(`/v1/feriado/${id}`);
  },

  /**
   * Consulta o calendário anual de feriados
   * @param {{
   *   ano: number,
   *   uf?: string,
   *   municipio?: string,
   *   empresaId?: string
   * }} filtros
   */
  async buscarCalendarioFeriados(filtros = {}) {
    const params = new URLSearchParams();
    params.append('ano', filtros.ano || new Date().getFullYear());
    if (filtros.uf) params.append('uf', filtros.uf);
    if (filtros.municipio) params.append('municipio', filtros.municipio);
    if (filtros.empresaId) params.append('empresaId', filtros.empresaId);

    return await api.get(`/v1/feriado/calendario?${params.toString()}`);
  },

  /**
   * Cadastra um novo ponto facultativo
   * @param {{
   *   descricao: string,
   *   data: string,
   *   alcance: 'NACIONAL'|'ESTADUAL'|'MUNICIPAL',
   *   uf?: string,
   *   municipio?: string
   * }} dados
   */
  async cadastrarPontoFacultativo(dados) {
    return await api.post('/v1/ponto-facultativo', dados);
  },

  /**
   * Inativa um ponto facultativo por ID
   * @param {string} id UUID do ponto facultativo
   */
  async inativarPontoFacultativo(id) {
    return await api.delete(`/v1/ponto-facultativo/${id}`);
  },

  /**
   * Consulta o calendário anual de pontos facultativos
   * @param {{
   *   ano: number,
   *   uf?: string,
   *   municipio?: string
   * }} filtros
   */
  async buscarCalendarioPontosFacultativos(filtros = {}) {
    const params = new URLSearchParams();
    params.append('ano', filtros.ano || new Date().getFullYear());
    if (filtros.uf) params.append('uf', filtros.uf);
    if (filtros.municipio) params.append('municipio', filtros.municipio);

    return await api.get(`/v1/ponto-facultativo/calendario?${params.toString()}`);
  },
};

export default feriadoService;
