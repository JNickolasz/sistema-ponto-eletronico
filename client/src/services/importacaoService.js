import { api } from './api';

export const importacaoService = {
  /**
   * Etapa 1: Envia o arquivo .txt posicional e o ID do equipamento para validação e preview
   * POST /api/v1/importacao/preview
   */
  gerarPreview: async (arquivo, equipamentoId) => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    formData.append('equipamentoId', equipamentoId);
    return api.postFormData('/importacao/preview', formData);
  },

  /**
   * Etapa 2: Confirma e persiste as batidas válidas e o histórico de forma transacional (@Transactional)
   * POST /api/v1/importacao/confirmar
   */
  confirmarImportacao: async (arquivo, equipamentoId) => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    formData.append('equipamentoId', equipamentoId);
    return api.postFormData('/importacao/confirmar', formData);
  },

  /**
   * Lista o histórico de todas as importações realizadas na empresa do RH logado
   * GET /api/v1/importacao/historico
   */
  listarHistorico: async () => {
    return api.get('/importacao/historico');
  },
};

export default importacaoService;
