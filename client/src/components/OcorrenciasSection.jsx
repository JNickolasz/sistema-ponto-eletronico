import React, { useState, useEffect } from 'react';
import ocorrenciaService from '../services/ocorrenciaService';
import {
  FileText,
  Plus,
  Trash2,
  Paperclip,
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  Calendar,
  Clock,
  Settings,
  Filter,
  Upload,
  X,
  ExternalLink,
  ShieldAlert,
  UserCheck
} from 'lucide-react';

function formatMinutos(minutos) {
  if (!minutos || minutos === 0) return '0h00';
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return `${h}h${m.toString().padStart(2, '0')}`;
}

function formatDataBr(dataStr) {
  if (!dataStr) return '';
  const [ano, mes, dia] = dataStr.split('-');
  return `${dia}/${mes}/${ano}`;
}

export default function OcorrenciasSection({ funcionarios = [] }) {
  // Aba interna da seção: 'lancamento' | 'historico' | 'tipos'
  const [abaInterna, setAbaInterna] = useState('lancamento');

  // Tipos de ocorrência
  const [tipos, setTipos] = useState([]);
  const [loadingTipos, setLoadingTipos] = useState(false);

  // Histórico de Ocorrências
  const [ocorrencias, setOcorrencias] = useState([]);
  const [loadingOcorrencias, setLoadingOcorrencias] = useState(false);

  // Filtros de listagem
  const [filtroFuncionarioId, setFiltroFuncionarioId] = useState('');
  const [filtroDataInicio, setFiltroDataInicio] = useState('');
  const [filtroDataFim, setFiltroDataFim] = useState('');

  // Formulário de Lançamento
  const [formFuncionarioId, setFormFuncionarioId] = useState('');
  const [formTipoId, setFormTipoId] = useState('');
  const [formDataInicio, setFormDataInicio] = useState('');
  const [formDataFim, setFormDataFim] = useState('');
  const [formMinutos, setFormMinutos] = useState('');
  const [formObservacao, setFormObservacao] = useState('');
  const [formArquivo, setFormArquivo] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Formulário de Novo Tipo
  const [modalNovoTipoAberto, setModalNovoTipoAberto] = useState(false);
  const [novoTipoNome, setNovoTipoNome] = useState('');
  const [novoTipoDescricao, setNovoTipoDescricao] = useState('');
  const [novoTipoAbono, setNovoTipoAbono] = useState('INTEGRAL');
  const [novoTipoMinutos, setNovoTipoMinutos] = useState(240);
  const [novoTipoExigeAnexo, setNovoTipoExigeAnexo] = useState(false);
  const [salvandoTipo, setSalvandoTipo] = useState(false);

  // Feedback
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  // Carregar tipos
  const carregarTipos = async () => {
    setLoadingTipos(true);
    try {
      const data = await ocorrenciaService.listarTipos(true);
      setTipos(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Erro ao carregar tipos de ocorrência', err);
    } finally {
      setLoadingTipos(false);
    }
  };

  // Carregar ocorrências
  const carregarOcorrencias = async () => {
    setLoadingOcorrencias(true);
    setErro('');
    try {
      const data = await ocorrenciaService.listarOcorrencias({
        funcionarioId: filtroFuncionarioId || undefined,
        dataInicio: filtroDataInicio || undefined,
        dataFim: filtroDataFim || undefined,
      });
      setOcorrencias(Array.isArray(data) ? data : []);
    } catch (err) {
      setErro(err.message || 'Erro ao carregar lista de ocorrências.');
    } finally {
      setLoadingOcorrencias(false);
    }
  };

  useEffect(() => {
    carregarTipos();
    carregarOcorrencias();
  }, []);

  const tipoSelecionadoObj = tipos.find((t) => t.id === formTipoId);

  // Submissão de Lançamento de Ocorrência
  const handleLancar = async (e) => {
    e.preventDefault();
    setErro('');
    setSucesso('');

    if (!formFuncionarioId) {
      setErro('Selecione o colaborador.');
      return;
    }
    if (!formTipoId) {
      setErro('Selecione o tipo de ocorrência.');
      return;
    }
    if (!formDataInicio || !formDataFim) {
      setErro('Informe as datas de início e término.');
      return;
    }
    if (formDataFim < formDataInicio) {
      setErro('A data final não pode ser anterior à data inicial.');
      return;
    }
    if (tipoSelecionadoObj?.exigeAnexo && !formArquivo) {
      setErro(`O tipo "${tipoSelecionadoObj.nome}" exige documento comprobatório em anexo.`);
      return;
    }

    setSubmitting(true);
    try {
      await ocorrenciaService.lancarOcorrencia(
        {
          funcionarioId: formFuncionarioId,
          tipoOcorrenciaId: formTipoId,
          dataInicio: formDataInicio,
          dataFim: formDataFim,
          minutosAbonoPadrao:
            tipoSelecionadoObj?.tipoAbono === 'PARCIAL' ? Number(formMinutos) : undefined,
          observacao: formObservacao || undefined,
        },
        formArquivo
      );

      setSucesso('Ocorrência lançada com sucesso! Os espelhos do período foram recalculados automaticamente.');
      setFormDataInicio('');
      setFormDataFim('');
      setFormMinutos('');
      setFormObservacao('');
      setFormArquivo(null);
      carregarOcorrencias();
    } catch (err) {
      setErro(err.message || 'Falha ao lançar ocorrência.');
    } finally {
      setSubmitting(false);
    }
  };

  // Cancelar Ocorrência
  const handleCancelarOcorrencia = async (id, colaboradorNome) => {
    if (!window.confirm(`Tem certeza que deseja cancelar a ocorrência de ${colaboradorNome}? O período correspondente será recalculado.`)) {
      return;
    }
    setErro('');
    setSucesso('');
    try {
      await ocorrenciaService.cancelarOcorrencia(id);
      setSucesso('Ocorrência cancelada e espelho recalculado com sucesso.');
      carregarOcorrencias();
    } catch (err) {
      setErro(err.message || 'Erro ao cancelar ocorrência.');
    }
  };

  // Criar Novo Tipo de Ocorrência
  const handleCriarTipo = async (e) => {
    e.preventDefault();
    if (!novoTipoNome.trim()) {
      setErro('O nome do tipo de ocorrência é obrigatório.');
      return;
    }
    setSalvandoTipo(true);
    setErro('');
    try {
      await ocorrenciaService.criarTipo({
        nome: novoTipoNome.trim(),
        descricao: novoTipoDescricao || undefined,
        tipoAbono: novoTipoAbono,
        minutosAbonoPadrao: novoTipoAbono === 'PARCIAL' ? Number(novoTipoMinutos) : undefined,
        exigeAnexo: novoTipoExigeAnexo,
      });

      setSucesso(`Tipo de ocorrência "${novoTipoNome}" cadastrado com sucesso!`);
      setNovoTipoNome('');
      setNovoTipoDescricao('');
      setNovoTipoAbono('INTEGRAL');
      setNovoTipoMinutos(240);
      setNovoTipoExigeAnexo(false);
      setModalNovoTipoAberto(false);
      carregarTipos();
    } catch (err) {
      setErro(err.message || 'Erro ao cadastrar tipo de ocorrência.');
    } finally {
      setSalvandoTipo(false);
    }
  };

  // Alternar Ativo/Inativo Tipo
  const handleToggleTipoAtivo = async (tipo) => {
    try {
      await ocorrenciaService.atualizarTipo(tipo.id, {
        nome: tipo.nome,
        descricao: tipo.descricao,
        tipoAbono: tipo.tipoAbono,
        minutosAbonoPadrao: tipo.minutosAbonoPadrao,
        exigeAnexo: tipo.exigeAnexo,
        ativo: !tipo.ativo,
      });
      carregarTipos();
    } catch (err) {
      setErro(err.message || 'Erro ao atualizar tipo de ocorrência.');
    }
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-100 pb-4">
        <div>
          <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            <FileText className="w-6 h-6 text-indigo-600" />
            Ocorrências, Licenças e Abonos
          </h2>
          <p className="text-sm text-gray-500">
            Atestados médicos, férias e faltas justificadas com cálculo de abono integral ou parcial e auditoria.
          </p>
        </div>

        {/* Botões de Ação / Abas Internas */}
        <div className="flex items-center gap-2">
          <div className="bg-gray-100 p-1 rounded-xl flex gap-1 text-xs font-semibold">
            <button
              onClick={() => setAbaInterna('lancamento')}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                abaInterna === 'lancamento'
                  ? 'bg-white text-indigo-600 shadow-xs'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Lançar Ocorrência
            </button>
            <button
              onClick={() => setAbaInterna('historico')}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                abaInterna === 'historico'
                  ? 'bg-white text-indigo-600 shadow-xs'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Histórico & Filtros
            </button>
            <button
              onClick={() => setAbaInterna('tipos')}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                abaInterna === 'tipos'
                  ? 'bg-white text-indigo-600 shadow-xs'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Tipos Cadastrados
            </button>
          </div>
        </div>
      </div>

      {/* Alertas */}
      {erro && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-sm flex items-start gap-2.5">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 text-rose-500 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold">Não foi possível concluir a ação:</p>
            <p>{erro}</p>
          </div>
          <button onClick={() => setErro('')} className="text-rose-400 hover:text-rose-600">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {sucesso && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm flex items-center justify-between gap-2.5">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
            <span>{sucesso}</span>
          </div>
          <button onClick={() => setSucesso('')} className="text-emerald-400 hover:text-emerald-600">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* ======================= ABA 1: LANÇAMENTO ======================= */}
      {abaInterna === 'lancamento' && (
        <form onSubmit={handleLancar} className="space-y-6 max-w-3xl">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Colaborador */}
            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1.5">
                Colaborador *
              </label>
              <select
                value={formFuncionarioId}
                onChange={(e) => setFormFuncionarioId(e.target.value)}
                required
                className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2.5 focus:border-indigo-500 focus:bg-white focus:outline-none transition-colors"
              >
                <option value="">Selecione o colaborador...</option>
                {funcionarios.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.nomeCompleto} ({f.matricula})
                  </option>
                ))}
              </select>
            </div>

            {/* Tipo de Ocorrência */}
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block text-xs font-semibold text-gray-700 uppercase">
                  Tipo de Ocorrência *
                </label>
                <button
                  type="button"
                  onClick={() => setModalNovoTipoAberto(true)}
                  className="text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-0.5"
                >
                  <Plus className="w-3 h-3" /> Novo Tipo
                </button>
              </div>
              <select
                value={formTipoId}
                onChange={(e) => {
                  setFormTipoId(e.target.value);
                  const selecionado = tipos.find((t) => t.id === e.target.value);
                  if (selecionado?.tipoAbono === 'PARCIAL' && selecionado.minutosAbonoPadrao) {
                    setFormMinutos(selecionado.minutosAbonoPadrao);
                  }
                }}
                required
                className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2.5 focus:border-indigo-500 focus:bg-white focus:outline-none transition-colors"
              >
                <option value="">Selecione o tipo...</option>
                {tipos.filter((t) => t.ativo).map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome} (
                    {t.tipoAbono === 'INTEGRAL'
                      ? 'Integral'
                      : t.tipoAbono === 'PARCIAL'
                      ? 'Parcial'
                      : 'Registro'}
                    )
                  </option>
                ))}
              </select>
            </div>

            {/* Data Inicial */}
            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1.5">
                Data Inicial *
              </label>
              <input
                type="date"
                value={formDataInicio}
                onChange={(e) => setFormDataInicio(e.target.value)}
                required
                className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2.5 focus:border-indigo-500 focus:bg-white focus:outline-none"
              />
            </div>

            {/* Data Final */}
            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1.5">
                Data Final *
              </label>
              <input
                type="date"
                value={formDataFim}
                onChange={(e) => setFormDataFim(e.target.value)}
                required
                className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2.5 focus:border-indigo-500 focus:bg-white focus:outline-none"
              />
            </div>
          </div>

          {/* Campo Condicional: Minutos Abonados por Dia se Parcial */}
          {tipoSelecionadoObj?.tipoAbono === 'PARCIAL' && (
            <div className="bg-amber-50/60 border border-amber-200/80 rounded-xl p-4">
              <label className="block text-xs font-semibold text-amber-900 uppercase mb-1.5 flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-amber-700" />
                Minutos Abonados por Dia (Abono Parcial) *
              </label>
              <div className="flex items-center gap-3">
                <input
                  type="number"
                  min="1"
                  max="1440"
                  value={formMinutos}
                  onChange={(e) => setFormMinutos(e.target.value)}
                  placeholder="Ex: 240 (para 4 horas)"
                  required
                  className="w-48 bg-white border border-amber-300 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-amber-500 focus:outline-none"
                />
                <span className="text-xs text-amber-800">
                  {formMinutos ? `Equivale a ${formatMinutos(Number(formMinutos))}` : 'Informe a quantidade diária em minutos'}
                </span>
              </div>
            </div>
          )}

          {/* Observação / Justificativa */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1.5">
              Observação / Descrição
            </label>
            <textarea
              value={formObservacao}
              onChange={(e) => setFormObservacao(e.target.value)}
              rows="2"
              placeholder="Ex: Atestado emitido por Dr. Fulano, CID Z00..."
              className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl p-3 focus:border-indigo-500 focus:bg-white focus:outline-none"
            />
          </div>

          {/* Upload de Comprovante */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1.5 flex items-center gap-1">
              <Paperclip className="w-3.5 h-3.5 text-gray-500" />
              Documento Comprobatório (PDF, JPG, PNG)
              {tipoSelecionadoObj?.exigeAnexo && (
                <span className="text-rose-500 text-[11px] font-bold ml-1">(Obrigatório para este tipo)</span>
              )}
            </label>
            <div className="flex items-center gap-3">
              <label className="cursor-pointer inline-flex items-center gap-2 px-4 py-2.5 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-xl text-sm font-medium text-gray-700 transition-colors">
                <Upload className="w-4 h-4 text-indigo-600" />
                {formArquivo ? 'Alterar Arquivo' : 'Selecionar Arquivo'}
                <input
                  type="file"
                  accept="application/pdf,image/jpeg,image/png"
                  onChange={(e) => setFormArquivo(e.target.files[0] || null)}
                  className="hidden"
                />
              </label>
              {formArquivo && (
                <span className="text-xs text-gray-600 font-mono flex items-center gap-1 bg-gray-100 px-2.5 py-1 rounded-lg">
                  {formArquivo.name} ({(formArquivo.size / 1024).toFixed(0)} KB)
                  <button
                    type="button"
                    onClick={() => setFormArquivo(null)}
                    className="text-gray-400 hover:text-rose-600 ml-1"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </span>
              )}
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition-colors shadow-sm disabled:opacity-50"
          >
            {submitting ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" /> Lançando e recalculando...
              </>
            ) : (
              <>
                <Plus className="w-4 h-4" /> Lançar Ocorrência
              </>
            )}
          </button>
        </form>
      )}

      {/* ======================= ABA 2: HISTÓRICO & FILTROS ======================= */}
      {abaInterna === 'historico' && (
        <div className="space-y-4">
          {/* Barra de Filtros */}
          <div className="bg-gray-50 border border-gray-200/80 rounded-xl p-4 flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 min-w-[200px] flex-1">
              <UserCheck className="w-4 h-4 text-gray-500" />
              <select
                value={filtroFuncionarioId}
                onChange={(e) => setFiltroFuncionarioId(e.target.value)}
                className="w-full bg-white border border-gray-200 text-xs rounded-lg px-2.5 py-1.5 focus:outline-none"
              >
                <option value="">Todos os colaboradores</option>
                {funcionarios.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.nomeCompleto}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-500">De:</span>
              <input
                type="date"
                value={filtroDataInicio}
                onChange={(e) => setFiltroDataInicio(e.target.value)}
                className="bg-white border border-gray-200 text-xs rounded-lg px-2 py-1.5 focus:outline-none"
              />
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-500">Até:</span>
              <input
                type="date"
                value={filtroDataFim}
                onChange={(e) => setFiltroDataFim(e.target.value)}
                className="bg-white border border-gray-200 text-xs rounded-lg px-2 py-1.5 focus:outline-none"
              />
            </div>

            <button
              onClick={carregarOcorrencias}
              disabled={loadingOcorrencias}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-medium rounded-lg transition-colors cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingOcorrencias ? 'animate-spin' : ''}`} />
              Filtrar
            </button>
          </div>

          {/* Tabela de Ocorrências */}
          <div className="overflow-x-auto border border-gray-100 rounded-xl shadow-2xs">
            <table className="w-full text-left text-xs text-gray-600">
              <thead className="bg-gray-50 text-gray-700 uppercase font-semibold text-[11px] border-b border-gray-200">
                <tr>
                  <th className="py-3 px-3">Colaborador</th>
                  <th className="py-3 px-3">Tipo / Regra</th>
                  <th className="py-3 px-3 text-center">Período</th>
                  <th className="py-3 px-3 text-center">Abono Diário</th>
                  <th className="py-3 px-3">Comprovante</th>
                  <th className="py-3 px-3">Lançado Por</th>
                  <th className="py-3 px-3 text-center">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {loadingOcorrencias ? (
                  <tr>
                    <td colSpan="7" className="text-center py-8 text-gray-400">
                      <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2 text-indigo-500" />
                      Carregando ocorrências...
                    </td>
                  </tr>
                ) : ocorrencias.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="text-center py-8 text-gray-400">
                      Nenhuma ocorrência encontrada para os filtros selecionados.
                    </td>
                  </tr>
                ) : (
                  ocorrencias.map((o) => (
                    <tr key={o.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="py-3 px-3 font-medium text-gray-900 whitespace-nowrap">
                        <div>{o.nomeFuncionario}</div>
                        <span className="text-[10px] text-gray-400">{o.matriculaFuncionario}</span>
                      </td>

                      <td className="py-3 px-3 whitespace-nowrap">
                        <span className="font-semibold text-gray-800">{o.nomeTipoOcorrencia}</span>
                        <span className="block text-[10px] text-gray-500">
                          {o.tipoAbono === 'INTEGRAL'
                            ? 'Abono Integral (100%)'
                            : o.tipoAbono === 'PARCIAL'
                            ? 'Abono Parcial'
                            : 'Apenas Registro'}
                        </span>
                      </td>

                      <td className="py-3 px-3 text-center whitespace-nowrap font-mono text-[11px]">
                        {formatDataBr(o.dataInicio)} a {formatDataBr(o.dataFim)}
                      </td>

                      <td className="py-3 px-3 text-center whitespace-nowrap">
                        {o.tipoAbono === 'PARCIAL' ? (
                          <span className="text-sky-700 font-semibold">
                            {formatMinutos(o.minutosAbonadosPorDia)}
                          </span>
                        ) : o.tipoAbono === 'INTEGRAL' ? (
                          <span className="text-emerald-700 font-medium">Integral</span>
                        ) : (
                          <span className="text-gray-400">—</span>
                        )}
                      </td>

                      <td className="py-3 px-3 whitespace-nowrap">
                        {o.possuiAnexo ? (
                          <button
                            onClick={() => ocorrenciaService.abrirAnexo(o.id)}
                            className="inline-flex items-center gap-1 px-2 py-1 rounded bg-indigo-50 text-indigo-700 hover:bg-indigo-100 text-[11px] font-medium transition-colors cursor-pointer"
                            title={o.nomeArquivo}
                          >
                            <Paperclip className="w-3 h-3" />
                            {o.nomeArquivo?.length > 18 ? o.nomeArquivo.slice(0, 15) + '...' : o.nomeArquivo}
                          </button>
                        ) : (
                          <span className="text-gray-400 text-[11px]">Sem anexo</span>
                        )}
                      </td>

                      <td className="py-3 px-3 whitespace-nowrap text-gray-500 text-[11px]">
                        <div>{o.lancadoPor}</div>
                        <span className="text-[10px] text-gray-400">
                          {o.lancadoEm?.slice(0, 16)?.replace('T', ' ')}
                        </span>
                      </td>

                      <td className="py-3 px-3 text-center whitespace-nowrap">
                        <button
                          onClick={() => handleCancelarOcorrencia(o.id, o.nomeFuncionario)}
                          className="p-1.5 text-gray-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                          title="Cancelar ocorrência e recalcular espelho"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ======================= ABA 3: TIPOS CADASTRADOS ======================= */}
      {abaInterna === 'tipos' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <p className="text-xs text-gray-500">
              Gerencie as regras de abono da sua empresa e exigências de comprovante médico/legal.
            </p>
            <button
              onClick={() => setModalNovoTipoAberto(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg shadow-xs transition-colors cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" /> Novo Tipo
            </button>
          </div>

          <div className="overflow-x-auto border border-gray-100 rounded-xl shadow-2xs">
            <table className="w-full text-left text-xs text-gray-600">
              <thead className="bg-gray-50 text-gray-700 uppercase font-semibold text-[11px] border-b border-gray-200">
                <tr>
                  <th className="py-3 px-3">Nome</th>
                  <th className="py-3 px-3">Regra de Abono</th>
                  <th className="py-3 px-3 text-center">Minutos Padrão</th>
                  <th className="py-3 px-3 text-center">Exige Anexo</th>
                  <th className="py-3 px-3 text-center">Status</th>
                  <th className="py-3 px-3 text-center">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {tipos.map((t) => (
                  <tr key={t.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-3 px-3 font-semibold text-gray-900">
                      <div>{t.nome}</div>
                      {t.descricao && <div className="text-[10px] text-gray-400 font-normal">{t.descricao}</div>}
                    </td>

                    <td className="py-3 px-3 whitespace-nowrap">
                      {t.tipoAbono === 'INTEGRAL' && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-100 text-emerald-800">
                          Abono Integral (100%)
                        </span>
                      )}
                      {t.tipoAbono === 'PARCIAL' && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-sky-100 text-sky-800">
                          Abono Parcial
                        </span>
                      )}
                      {t.tipoAbono === 'APENAS_REGISTRO' && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-gray-100 text-gray-800">
                          Apenas Registro
                        </span>
                      )}
                    </td>

                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      {t.tipoAbono === 'PARCIAL' && t.minutosAbonoPadrao
                        ? formatMinutos(t.minutosAbonoPadrao)
                        : '—'}
                    </td>

                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      {t.exigeAnexo ? (
                        <span className="text-amber-700 font-semibold">Sim</span>
                      ) : (
                        <span className="text-gray-400">Não</span>
                      )}
                    </td>

                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                          t.ativo ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'
                        }`}
                      >
                        {t.ativo ? 'Ativo' : 'Inativo'}
                      </span>
                    </td>

                    <td className="py-3 px-3 text-center whitespace-nowrap">
                      <button
                        onClick={() => handleToggleTipoAtivo(t)}
                        className={`text-xs font-medium cursor-pointer ${
                          t.ativo ? 'text-amber-600 hover:text-amber-800' : 'text-emerald-600 hover:text-emerald-800'
                        }`}
                      >
                        {t.ativo ? 'Desativar' : 'Ativar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ======================= MODAL NOVO TIPO ======================= */}
      {modalNovoTipoAberto && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl shadow-xl border border-gray-100 w-full max-w-md p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h3 className="text-base font-bold text-gray-800 flex items-center gap-2">
                <Settings className="w-5 h-5 text-indigo-600" />
                Cadastrar Tipo de Ocorrência
              </h3>
              <button
                onClick={() => setModalNovoTipoAberto(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCriarTipo} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                  Nome da Regra *
                </label>
                <input
                  type="text"
                  value={novoTipoNome}
                  onChange={(e) => setNovoTipoNome(e.target.value)}
                  placeholder="Ex: Atestado Médico, Férias, Licença Casamento"
                  required
                  className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-indigo-500 focus:bg-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                  Regra de Abono *
                </label>
                <select
                  value={novoTipoAbono}
                  onChange={(e) => setNovoTipoAbono(e.target.value)}
                  className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-indigo-500 focus:bg-white focus:outline-none"
                >
                  <option value="INTEGRAL">Integral (Abona 100% da jornada prevista)</option>
                  <option value="PARCIAL">Parcial (Abona quantidade específica de minutos)</option>
                  <option value="APENAS_REGISTRO">Apenas Registro (Sem alteração no saldo de horas)</option>
                </select>
              </div>

              {novoTipoAbono === 'PARCIAL' && (
                <div>
                  <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                    Minutos Padrão de Abono *
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="1440"
                    value={novoTipoMinutos}
                    onChange={(e) => setNovoTipoMinutos(e.target.value)}
                    required
                    className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-indigo-500 focus:bg-white focus:outline-none"
                  />
                  <span className="text-[11px] text-gray-400 mt-0.5 block">
                    Equivale a {formatMinutos(Number(novoTipoMinutos))} por dia
                  </span>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                  Descrição (Opcional)
                </label>
                <input
                  type="text"
                  value={novoTipoDescricao}
                  onChange={(e) => setNovoTipoDescricao(e.target.value)}
                  placeholder="Ex: Atestados médicos até 15 dias"
                  className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-xl px-3.5 py-2 focus:border-indigo-500 focus:bg-white focus:outline-none"
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="exigeAnexoCheck"
                  checked={novoTipoExigeAnexo}
                  onChange={(e) => setNovoTipoExigeAnexo(e.target.checked)}
                  className="rounded text-indigo-600 focus:ring-indigo-500 w-4 h-4 cursor-pointer"
                />
                <label htmlFor="exigeAnexoCheck" className="text-xs text-gray-700 font-medium cursor-pointer">
                  Exigir anexo de comprovante no lançamento
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setModalNovoTipoAberto(false)}
                  className="px-4 py-2 text-xs font-medium text-gray-600 hover:bg-gray-100 rounded-xl transition-colors cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={salvandoTipo}
                  className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl transition-colors shadow-xs disabled:opacity-50 cursor-pointer"
                >
                  {salvandoTipo ? 'Salvando...' : 'Salvar Tipo'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
