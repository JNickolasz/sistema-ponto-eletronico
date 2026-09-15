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
   * Cadastra uma nova empresa via API administrativa (POST /api/admin/empresas)
   */
  async cadastrarEmpresaAdmin(dados, adminKey = 'admin123') {
    return await api.post('/admin/empresas', dados, {
      headers: { 'X-Admin-Key': adminKey },
    });
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

  /**
   * Lista os equipamentos de registro cadastrados
   */
  async listarEquipamentos(filtros = {}) {
    const params = new URLSearchParams();
    if (filtros.localTrabalhoId) params.append('localTrabalhoId', filtros.localTrabalhoId);
    if (filtros.empresaId) params.append('empresaId', filtros.empresaId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return await api.get(`/v1/equipamentos${query}`);
  },

  /**
   * Cadastra um novo equipamento (RELOGIO ou ESTACAO)
   */
  async cadastrarEquipamento(dados) {
    return await api.post('/v1/equipamentos', dados);
  },

  /**
   * Altera o status do equipamento (ATIVO / INATIVO)
   */
  async alterarStatusEquipamento(id, status) {
    return await api.patch(`/v1/equipamentos/${id}/status?status=${status}`);
  },
};

export default rhService;
