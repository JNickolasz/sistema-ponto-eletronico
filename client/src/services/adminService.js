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
    const payload = {
      razaoSocial: dados.razaoSocial?.trim(),
      subdominio: dados.subdominio?.trim()?.toLowerCase(),
      cnpj: dados.cnpj?.trim(),
      endereco: dados.endereco?.trim(),
      logoUrl: dados.logoUrl?.trim() || undefined,
    };
    return await api.post('/admin/empresas', payload);
  },
};

export default adminService;
