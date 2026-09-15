import api from './api';

export const adminService = {
  /**
   * Lista todas as empresas cadastradas via rota administrativa
   */
  async listarEmpresas() {
    return await api.get('/admin/empresas');
  },

  /**
   * Cadastra uma nova empresa cliente no sistema
   * @param {{razaoSocial: string, subdominio: string, cnpj: string, endereco: string, logoUrl?: string}} dados
   */
  async cadastrarEmpresa(dados) {
    return await api.post('/admin/empresas', dados);
  },
};

export default adminService;
