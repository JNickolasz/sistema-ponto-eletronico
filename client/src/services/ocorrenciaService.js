import api, { API_BASE } from './api';

export const ocorrenciaService = {
  /**
   * Lista tipos de ocorrência cadastrados
   * @param {boolean} todos - se true traz ativos e inativos
   */
  async listarTipos(todos = false) {
    const res = await api.get(`/tipos-ocorrencia?todos=${todos}`);
    return res?.data || res;
  },

  /**
   * Cadastra novo tipo de ocorrência
   * @param {{ nome: string, descricao?: string, tipoAbono: 'INTEGRAL'|'PARCIAL'|'APENAS_REGISTRO', minutosAbonoPadrao?: number, exigeAnexo?: boolean }} dados
   */
  async criarTipo(dados) {
    const res = await api.post('/tipos-ocorrencia', dados);
    return res?.data || res;
  },

  /**
   * Atualiza tipo de ocorrência
   * @param {string} id
   * @param {{ nome: string, descricao?: string, tipoAbono: 'INTEGRAL'|'PARCIAL'|'APENAS_REGISTRO', minutosAbonoPadrao?: number, exigeAnexo?: boolean, ativo?: boolean }} dados
   */
  async atualizarTipo(id, dados) {
    const res = await api.put(`/tipos-ocorrencia/${id}`, dados);
    return res?.data || res;
  },

  /**
   * Lista ocorrências lançadas com filtros opcionais
   */
  async listarOcorrencias({ funcionarioId, dataInicio, dataFim } = {}) {
    const params = new URLSearchParams();
    if (funcionarioId) params.append('funcionarioId', funcionarioId);
    if (dataInicio) params.append('dataInicio', dataInicio);
    if (dataFim) params.append('dataFim', dataFim);

    const qs = params.toString() ? `?${params.toString()}` : '';
    const res = await api.get(`/ocorrencias${qs}`);
    return res?.data || res;
  },

  /**
   * Lança ocorrência para colaborador com suporte opcional a arquivo
   */
  async lancarOcorrencia(dados, arquivo = null) {
    if (arquivo) {
      const formData = new FormData();
      formData.append('funcionarioId', dados.funcionarioId);
      formData.append('tipoOcorrenciaId', dados.tipoOcorrenciaId);
      formData.append('dataInicio', dados.dataInicio);
      formData.append('dataFim', dados.dataFim);
      if (dados.minutosAbonoPadrao != null && dados.minutosAbonoPadrao !== '') {
        formData.append('minutosAbonoPadrao', dados.minutosAbonoPadrao);
      }
      if (dados.observacao) {
        formData.append('observacao', dados.observacao);
      }
      formData.append('arquivo', arquivo);

      const res = await api.postFormData('/ocorrencias', formData);
      return res?.data || res;
    } else {
      const res = await api.post('/ocorrencias', dados);
      return res?.data || res;
    }
  },

  /**
   * Cancela uma ocorrência lançada
   */
  async cancelarOcorrencia(id) {
    return await api.delete(`/ocorrencias/${id}`);
  },

  /**
   * Baixa anexo da ocorrência autenticado
   */
  async baixarAnexo(id, nomeArquivo = 'comprovante') {
    const token = localStorage.getItem('token');
    const headers = token && token !== 'admin-session-token' ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`${API_BASE}/ocorrencias/${id}/anexo`, { headers });
    if (!res.ok) throw new Error('Não foi possível carregar o anexo.');
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nomeArquivo;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  },

  /**
   * Abre anexo em nova aba
   */
  async abrirAnexo(id) {
    const token = localStorage.getItem('token');
    const headers = token && token !== 'admin-session-token' ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`${API_BASE}/ocorrencias/${id}/anexo`, { headers });
    if (!res.ok) throw new Error('Não foi possível abrir o anexo.');
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    window.open(url, '_blank');
  },
};

export default ocorrenciaService;
