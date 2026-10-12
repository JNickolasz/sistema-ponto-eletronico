import api from './api';

export const equipamentoService = {
  /**
   * Lista todos os equipamentos cadastrados ou filtrados por local/empresa
   * @param {{ localDeTrabalhoId?: string, localTrabalhoId?: string, empresaId?: string, tipo?: string, status?: string, numFabricacao?: string }} [filtros]
   */
  async listarEquipamentos(filtros = {}) {
    const params = new URLSearchParams();
    const localId = filtros.localDeTrabalhoId || filtros.localTrabalhoId;
    if (localId) params.append('localDeTrabalhoId', localId);
    if (filtros.tipo || filtros.tipoEquipamento) {
      params.append('tipoEquipamento', filtros.tipo || filtros.tipoEquipamento);
    }
    if (filtros.status) params.append('status', filtros.status);
    if (filtros.numFabricacao) params.append('numFabricacao', filtros.numFabricacao);

    const query = params.toString() ? `?${params.toString()}` : '';
    return await api.get(`/v1/equipamentos${query}`);
  },

  /**
   * Cadastra um novo equipamento (RELOGIO ou ESTACAO)
   * Garante a estrutura exigida pelo EquipamentoDTO.Criar do backend
   * @param {{
   *   codigo?: string,
   *   localTrabalhoId: string,
   *   tipo: 'RELOGIO' | 'ESTACAO',
   *   identificacao: string,
   *   numFabricacao?: string | null,
   *   linhasImportadas?: number | null,
   *   credentialId?: string | null,
   *   publicKey?: string | null,
   *   relogio?: { numFabricacao: string, linhasImportadas?: number },
   *   estacao?: { credentialId: string, publicKey: string }
   * }} dados
   */
  async cadastrarEquipamento(dados) {
    const tipo = dados.tipo || 'RELOGIO';
    const codigoGerado = dados.codigo?.trim() ||
      (tipo === 'RELOGIO' && dados.numFabricacao ? dados.numFabricacao.trim() : null) ||
      `EQ-${Math.floor(100000 + Math.random() * 900000)}`;

    const payload = {
      codigo: codigoGerado,
      localTrabalhoId: dados.localTrabalhoId,
      tipo,
      identificacao: dados.identificacao?.trim() || codigoGerado,
      relogio:
        tipo === 'RELOGIO'
          ? {
              numFabricacao:
                dados.numFabricacao?.trim() ||
                dados.relogio?.numFabricacao?.trim() ||
                codigoGerado,
              linhasImportadas:
                dados.linhasImportadas ?? dados.relogio?.linhasImportadas ?? 0,
            }
          : null,
      estacao:
        tipo === 'ESTACAO'
          ? {
              credentialId:
                dados.credentialId?.trim() ||
                dados.estacao?.credentialId?.trim() ||
                `CRED-${codigoGerado}`,
              publicKey:
                dados.publicKey?.trim() ||
                dados.estacao?.publicKey?.trim() ||
                `KEY-${Math.random().toString(36).substring(2, 12).toUpperCase()}`,
            }
          : null,
    };

    return await api.post('/v1/equipamentos', payload);
  },

  /**
   * Altera o status do equipamento (ATIVO / INATIVO) sem exclusão física.
   * O backend busca pelo campo 'codigo' do equipamento no controller @PatchMapping("/{codigo}/status").
   * @param {string} codigoOuId Código identificador do equipamento (ou ID como fallback)
   * @param {'ATIVO' | 'INATIVO'} status
   */
  async alterarStatus(codigoOuId, status) {
    if (!codigoOuId) throw new Error('Código do equipamento é obrigatório para alteração de status');
    return await api.patch(`/v1/equipamentos/${encodeURIComponent(codigoOuId)}/status?status=${status}`);
  },

  /**
   * Consulta os dados de um equipamento específico pelo ID ou código
   * @param {string} id
   */
  async buscarPorId(id) {
    if (!id) return null;
    try {
      const lista = await this.listarEquipamentos();
      return Array.isArray(lista)
        ? lista.find((eq) => eq.id === id || eq.codigo === id) || null
        : null;
    } catch {
      return null;
    }
  },
};

export default equipamentoService;
