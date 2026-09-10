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
};

export default rhService;
