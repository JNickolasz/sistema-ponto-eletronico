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
    if (filtros.ano !== undefined && filtros.ano !== null && String(filtros.ano).trim() !== '') {
      params.append('ano', String(filtros.ano).trim());
    }
    if (filtros.uf && String(filtros.uf).trim() !== '') params.append('uf', String(filtros.uf).trim());
    if (filtros.municipio && String(filtros.municipio).trim() !== '') params.append('municipio', String(filtros.municipio).trim());
    if (filtros.empresaId) params.append('empresaId', filtros.empresaId);

    const qs = params.toString();
    return await api.get(`/v1/feriado/calendario${qs ? `?${qs}` : ''}`);
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
    if (filtros.ano !== undefined && filtros.ano !== null && String(filtros.ano).trim() !== '') {
      params.append('ano', String(filtros.ano).trim());
    }
    if (filtros.uf && String(filtros.uf).trim() !== '') params.append('uf', String(filtros.uf).trim());
    if (filtros.municipio && String(filtros.municipio).trim() !== '') params.append('municipio', String(filtros.municipio).trim());

    const qs = params.toString();
    return await api.get(`/v1/ponto-facultativo/calendario${qs ? `?${qs}` : ''}`);
  },
};

export default feriadoService;
