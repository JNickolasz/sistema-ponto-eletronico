import api from './api';

export const rhService = {
  /**
   * Consulta os dados do painel do RH
   */
  async getPainel() {
    return await api.get('/rh/painel');
  },

  /**
   * Cadastra um novo funcionário (RH, GESTOR ou COLABORADOR)
   * @param {{nomeCompleto: string, usuario: string, senha: string, perfilAcesso: 'RH'|'GESTOR'|'COLABORADOR'}} dados
   */
  async cadastrarFuncionario(dados) {
    return await api.post('/rh/adicionar', dados);
  },

  /**
   * Lista as empresas cadastradas
   */
  async listarEmpresas() {
    return await api.get('/empresa');
  },

  /**
   * Cadastra uma nova empresa
   */
  async cadastrarEmpresa(dados) {
    return await api.post('/empresa', dados);
  },

  /**
   * Cadastra um novo local de trabalho vinculado à empresa
   */
  async cadastrarLocalTrabalho(dados) {
    return await api.post('/v1/locais-trabalho', dados);
  },

  /**
   * Lista os locais de trabalho cadastrados
   */
  async listarLocaisTrabalho(empresaId) {
    const query = empresaId ? `?empresaId=${empresaId}` : '';
    return await api.get(`/v1/locais-trabalho${query}`);
  },
};

export default rhService;
