import React, { useState, useEffect, useRef } from 'react';
import importacaoService from '../services/importacaoService';
import {
  Upload,
  FileText,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  ArrowLeft,
  Check,
  X,
  RefreshCw,
  HardDrive,
  History,
  Sparkles,
  Download,
  ShieldAlert,
  Clock,
  User,
} from 'lucide-react';
import toast from 'react-hot-toast';

export default function ImportacaoPosicionalSection({
  equipamentos = [],
  funcionarios = [],
  modoInicial = 'wizard', // 'wizard' | 'historico'
  onIrParaEquipamentos,
  onImportacaoConcluida,
}) {
  // Sub-aba ativa: 'wizard' (Etapa 1 Upload / Etapa 2 Preview) ou 'historico'
  const [subAba, setSubAba] = useState(modoInicial);

  useEffect(() => {
    setSubAba(modoInicial);
  }, [modoInicial]);

  // Estados da Tela 1: Upload
  const [equipamentoId, setEquipamentoId] = useState('');
  const [arquivoSelecionado, setArquivoSelecionado] = useState(null);
  const [conteudoPreviewTexto, setConteudoPreviewTexto] = useState('');
  const [loadingPreview, setLoadingPreview] = useState(false);
  const fileInputRef = useRef(null);

  // Estados da Tela 2: Conferência (Preview)
  const [etapaWizard, setEtapaWizard] = useState(1); // 1 = Upload, 2 = Conferência (Preview)
  const [previewData, setPreviewData] = useState(null);
  const [aceiteRhConfirmado, setAceiteRhConfirmado] = useState(false);
  const [filtroTabelaPreview, setFiltroTabelaPreview] = useState('PROBLEMAS'); // 'PROBLEMAS' | 'TODAS' | 'VALIDAS'
  const [loadingConfirmacao, setLoadingConfirmacao] = useState(false);

  // Estados da Aba de Histórico
  const [historicoList, setHistoricoList] = useState([]);
  const [loadingHistorico, setLoadingHistorico] = useState(false);

  // Seleciona automaticamente o primeiro equipamento ativo se ainda não houver seleção
  useEffect(() => {
    if (!equipamentoId && equipamentos.length > 0) {
      const ativo = equipamentos.find((eq) => eq.status === 'ATIVO') || equipamentos[0];
      if (ativo?.id) {
        setEquipamentoId(ativo.id);
      }
    }
  }, [equipamentos, equipamentoId]);

  const carregarHistorico = async () => {
    try {
      setLoadingHistorico(true);
      const data = await importacaoService.listarHistorico();
      setHistoricoList(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Erro ao carregar histórico de importações:', err);
      toast.error(err.message || 'Erro ao carregar histórico de importações');
    } finally {
      setLoadingHistorico(false);
    }
  };

  useEffect(() => {
    carregarHistorico();
  }, []);

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      setArquivoSelecionado(null);
      setConteudoPreviewTexto('');
      return;
    }

    if (!file.name.toLowerCase().endsWith('.txt')) {
      toast.error('Selecione apenas arquivos de texto posicional (.txt)');
      e.target.value = '';
      return;
    }

    setArquivoSelecionado(file);
    const reader = new FileReader();
    reader.onload = (ev) => {
      setConteudoPreviewTexto(typeof ev.target?.result === 'string' ? ev.target.result : '');
    };
    reader.readAsText(file);
  };

  // Gerador de arquivo .txt posicional de exemplo para facilitar testes da T09
  const handleGerarArquivoExemplo = (tipo = 'MISTO') => {
    const pisColaborador1 =
      funcionarios
        .find((f) => f.pisPasep && f.pisPasep.replace(/\D/g, '').length === 11)
        ?.pisPasep?.replace(/\D/g, '') || '11111111111';
    const pisColaborador2 =
      funcionarios
        .find(
          (f) =>
            f.pisPasep &&
            f.pisPasep.replace(/\D/g, '').length === 11 &&
            f.pisPasep.replace(/\D/g, '') !== pisColaborador1
        )
        ?.pisPasep?.replace(/\D/g, '') || '33333333333';

    const hoje = new Date();
    const dd = String(hoje.getDate()).padStart(2, '0');
    const mm = String(hoje.getMonth() + 1).padStart(2, '0');
    const yyyy = String(hoje.getFullYear());
    const dataPosicional = `${dd}${mm}${yyyy}`;

    let linhas = [];
    if (tipo === 'MISTO') {
      // Cabeçalho (1), 2 válidas, 1 repetida, 1 recusada (PIS não encontrado), Trailer (9)
      linhas = [
        `1000000000CABECALHOAFDPONTOCERTO${dataPosicional}`,
        `3${dataPosicional}0800${pisColaborador1}`,
        `3${dataPosicional}1200${pisColaborador1}`,
        `3${dataPosicional}1300${pisColaborador2}`,
        `3${dataPosicional}0800${pisColaborador1}`, // Repetida (mesmo PIS e data/hora da linha 2)
        `3${dataPosicional}180099999999999`, // Recusada (PIS inexistente no banco)
        `9000000005TRAILERAFD`,
      ];
    } else if (tipo === 'SOMENTE_VALIDAS') {
      linhas = [
        `1000000000CABECALHOAFDPONTOCERTO${dataPosicional}`,
        `3${dataPosicional}0805${pisColaborador1}`,
        `3${dataPosicional}1205${pisColaborador1}`,
        `3${dataPosicional}1305${pisColaborador1}`,
        `3${dataPosicional}1705${pisColaborador1}`,
        `9000000004TRAILERAFD`,
      ];
    } else if (tipo === 'INVALIDO_SEM_TRAILER') {
      linhas = [
        `1000000000CABECALHOAFDPONTOCERTO${dataPosicional}`,
        `3${dataPosicional}0800${pisColaborador1}`,
      ];
    }

    const conteudo = linhas.join('\n');
    const nomeArquivo =
      tipo === 'MISTO'
        ? `afd_lote_misto_${dd}${mm}${yyyy}.txt`
        : tipo === 'SOMENTE_VALIDAS'
        ? `afd_lote_valido_${dd}${mm}${yyyy}.txt`
        : `afd_invalido_sem_trailer.txt`;

    const blob = new Blob([conteudo], { type: 'text/plain;charset=utf-8' });
    const file = new File([blob], nomeArquivo, { type: 'text/plain' });

    setArquivoSelecionado(file);
    setConteudoPreviewTexto(conteudo);
    if (fileInputRef.current) {
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(file);
      fileInputRef.current.files = dataTransfer.files;
    }

    toast.success(`Arquivo "${nomeArquivo}" carregado para teste!`);
  };

  const handleBaixarArquivoAtual = () => {
    if (!conteudoPreviewTexto || !arquivoSelecionado) return;
    const blob = new Blob([conteudoPreviewTexto], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = arquivoSelecionado.name || 'afd_exemplo.txt';
    a.click();
    URL.revokeObjectURL(url);
  };

  // Etapa 1: Enviar para POST /api/v1/importacao/preview
  const handleEnviarParaPreview = async (e) => {
    e.preventDefault();

    if (!equipamentoId) {
      toast.error('Selecione o Equipamento de origem antes de continuar.');
      return;
    }

    if (!arquivoSelecionado) {
      toast.error('Selecione um arquivo posicional (.txt) para importar.');
      return;
    }

    try {
      setLoadingPreview(true);
      const response = await importacaoService.gerarPreview(arquivoSelecionado, equipamentoId);
      setPreviewData(response);
      setAceiteRhConfirmado(false);
      // Se houver problemas (repetidas ou recusadas), mostra a aba de problemas por padrão; senão mostra todas
      const qtdProblemas = (response?.totalRepetidas || 0) + (response?.totalRecusadas || 0);
      setFiltroTabelaPreview(qtdProblemas > 0 ? 'PROBLEMAS' : 'TODAS');
      setEtapaWizard(2);
      toast.success('Arquivo lido e validado! Confira o resultado antes de confirmar a gravação.');
    } catch (err) {
      toast.error(err.message || 'Falha ao processar o preview do arquivo posicional.');
    } finally {
      setLoadingPreview(false);
    }
  };

  // Cancelar na Tela 2 e voltar para a Tela 1 sem gravar
  const handleCancelarImportacao = () => {
    setPreviewData(null);
    setAceiteRhConfirmado(false);
    setEtapaWizard(1);
    toast('Importação cancelada pelo RH. Nenhum dado foi salvo no banco.', { icon: 'ℹ️' });
  };

  // Etapa 2: Confirmar Gravação via POST /api/v1/importacao/confirmar
  const handleConfirmarGravacao = async () => {
    if (!aceiteRhConfirmado) {
      toast.error('Confirme o aceite da conferência antes de gravar os dados.');
      return;
    }

    if (!arquivoSelecionado || !equipamentoId) {
      toast.error('Arquivo ou equipamento ausente. Refaça o envio.');
      setEtapaWizard(1);
      return;
    }

    try {
      setLoadingConfirmacao(true);
      const historicoSalvo = await importacaoService.confirmarImportacao(arquivoSelecionado, equipamentoId);
      toast.success(
        `Importação concluída com sucesso! ${historicoSalvo.totalValidas} batida(s) gravada(s) no banco.`
      );

      // Limpa estados do wizard, atualiza histórico e abre a aba de histórico
      setPreviewData(null);
      setArquivoSelecionado(null);
      setConteudoPreviewTexto('');
      setAceiteRhConfirmado(false);
      setEtapaWizard(1);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }

      await carregarHistorico();
      if (onImportacaoConcluida) {
        onImportacaoConcluida(historicoSalvo);
      }
      setSubAba('historico');
    } catch (err) {
      toast.error(err.message || 'Erro transacional ao gravar importação. Nenhuma batida foi salva (Rollback).');
    } finally {
      setLoadingConfirmacao(false);
    }
  };

  const formatarDataHora = (isoStr) => {
    if (!isoStr) return '—';
    try {
      const dt = new Date(isoStr);
      if (isNaN(dt.getTime())) return isoStr;
      return dt.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoStr;
    }
  };

  // Monta lista combinada de itens para a tabela do Preview
  const obterLinhasPreviewFiltradas = () => {
    if (!previewData) return [];

    const repetidasComTipo = (previewData.repetidas || []).map((item) => ({
      ...item,
      categoria: 'REPETIDA',
    }));
    const recusadasComTipo = (previewData.recusadas || []).map((item) => ({
      ...item,
      categoria: 'RECUSADA',
    }));
    const validasComTipo = (previewData.validas || []).map((item) => ({
      ...item,
      categoria: 'VALIDA',
    }));

    let lista = [];
    if (filtroTabelaPreview === 'PROBLEMAS') {
      lista = [...recusadasComTipo, ...repetidasComTipo];
    } else if (filtroTabelaPreview === 'VALIDAS') {
      lista = [...validasComTipo];
    } else {
      lista = [...recusadasComTipo, ...repetidasComTipo, ...validasComTipo];
    }

    return lista.sort((a, b) => (a.numeroLinha || 0) - (b.numeroLinha || 0));
  };

  const linhasTabela = obterLinhasPreviewFiltradas();
  const totalProblemas = (previewData?.totalRepetidas || 0) + (previewData?.totalRecusadas || 0);

  return (
    <div className="space-y-6">
      {/* Cabeçalho e Sub-abas (Assistente de Importação vs Histórico de Importações) */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-slate-900/80 border border-slate-800 rounded-2xl p-5 backdrop-blur-xl">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <Upload size={22} />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">
              Importação de Arquivo Posicional AFD (T09)
            </h2>
            <p className="text-xs text-slate-400">
              Leitura posicional por <code className="text-indigo-300">substring</code>, validação de cabeçalho/trailer, conferência prévia e gravação <code className="text-emerald-300">@Transactional</code>.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 bg-slate-950/80 p-1.5 rounded-xl border border-slate-800">
          <button
            type="button"
            onClick={() => setSubAba('wizard')}
            className={`px-3.5 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer ${
              subAba === 'wizard'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/25'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Upload size={14} />
            <span>Assistente de Importação</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setSubAba('historico');
              carregarHistorico();
            }}
            className={`px-3.5 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer ${
              subAba === 'historico'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/25'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <History size={14} />
            <span>Histórico de Importações</span>
            <span className="px-1.5 py-0.5 text-[10px] rounded-full bg-slate-800 text-slate-200 font-mono">
              {historicoList.length}
            </span>
          </button>
        </div>
      </div>

      {/* FLUXO WIZARD EM DUAS ETAPAS */}
      {subAba === 'wizard' && (
        <div className="space-y-6">
          {/* Stepper Indicador de Etapa */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div
              className={`p-4 rounded-xl border flex items-center gap-3.5 transition-all ${
                etapaWizard === 1
                  ? 'bg-indigo-950/40 border-indigo-500/60 text-white'
                  : 'bg-slate-900/50 border-emerald-500/30 text-emerald-300'
              }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold ${
                  etapaWizard === 1
                    ? 'bg-indigo-600 text-white'
                    : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                }`}
              >
                {etapaWizard > 1 ? <Check size={16} /> : '1'}
              </div>
              <div>
                <div className="text-xs font-bold uppercase tracking-wider">
                  Tela 1: Upload e Seleção de Equipamento
                </div>
                <div className="text-[11px] text-slate-400">
                  Escolha o equipamento de origem e envie o arquivo .txt posicional
                </div>
              </div>
            </div>

            <div
              className={`p-4 rounded-xl border flex items-center gap-3.5 transition-all ${
                etapaWizard === 2
                  ? 'bg-indigo-950/40 border-indigo-500/60 text-white'
                  : 'bg-slate-900/40 border-slate-800 text-slate-500'
              }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold ${
                  etapaWizard === 2 ? 'bg-indigo-600 text-white' : 'bg-slate-800 text-slate-400'
                }`}
              >
                2
              </div>
              <div>
                <div className="text-xs font-bold uppercase tracking-wider">
                  Tela 2: Conferência (Preview) e Aceite do RH
                </div>
                <div className="text-[11px] text-slate-400">
                  Valide válidas, repetidas, recusadas e motivos antes da gravação final
                </div>
              </div>
            </div>
          </div>

          {/* TELA 1: UPLOAD */}
          {etapaWizard === 1 && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
              <div className="lg:col-span-7">
                <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl space-y-6">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-800 pb-4">
                    <div>
                      <h3 className="text-base font-bold text-white">
                        Etapa 1: Selecionar Equipamento e Arquivo (.txt)
                      </h3>
                      <p className="text-xs text-slate-400 mt-0.5">
                        Aciona <span className="font-mono text-indigo-300">POST /api/v1/importacao/preview</span> (nenhum dado é salvo nesta etapa)
                      </p>
                    </div>

                    {/* Botões de Geração Rápida de Arquivo Posicional para Testes */}
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <button
                        type="button"
                        onClick={() => handleGerarArquivoExemplo('MISTO')}
                        className="text-[11px] px-2.5 py-1.5 rounded-lg bg-indigo-500/15 hover:bg-indigo-500/25 text-indigo-300 border border-indigo-500/30 transition-colors cursor-pointer flex items-center gap-1"
                        title="Gera arquivo com cabeçalho, batidas válidas, 1 repetida, 1 recusada e trailer"
                      >
                        <Sparkles size={12} /> + Lote Misto (Teste)
                      </button>
                      <button
                        type="button"
                        onClick={() => handleGerarArquivoExemplo('SOMENTE_VALIDAS')}
                        className="text-[11px] px-2.5 py-1.5 rounded-lg bg-emerald-500/15 hover:bg-emerald-500/25 text-emerald-300 border border-emerald-500/30 transition-colors cursor-pointer"
                        title="Gera arquivo apenas com batidas válidas"
                      >
                        + Lote 100% Válido
                      </button>
                      <button
                        type="button"
                        onClick={() => handleGerarArquivoExemplo('INVALIDO_SEM_TRAILER')}
                        className="text-[11px] px-2.5 py-1.5 rounded-lg bg-rose-500/15 hover:bg-rose-500/25 text-rose-300 border border-rose-500/30 transition-colors cursor-pointer"
                        title="Gera arquivo sem trailer tipo 9 para testar rejeição"
                      >
                        + Sem Trailer (Erro)
                      </button>
                    </div>
                  </div>

                  <form onSubmit={handleEnviarParaPreview} className="space-y-5">
                    {/* Select de Equipamento */}
                    <div>
                      <div className="flex items-center justify-between mb-1.5">
                        <label
                          htmlFor="select-equipamento-importacao"
                          className="block text-xs font-semibold uppercase tracking-wider text-slate-300"
                        >
                          Equipamento de Origem *
                        </label>
                        {equipamentos.length === 0 && onIrParaEquipamentos && (
                          <button
                            type="button"
                            onClick={onIrParaEquipamentos}
                            className="text-xs text-indigo-400 hover:text-indigo-300 underline cursor-pointer"
                          >
                            Cadastrar equipamento primeiro &rarr;
                          </button>
                        )}
                      </div>

                      <select
                        id="select-equipamento-importacao"
                        required
                        value={equipamentoId}
                        onChange={(e) => setEquipamentoId(e.target.value)}
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/80 px-3.5 py-2.5 text-sm text-slate-100 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      >
                        <option value="">Selecione o equipamento (Relógio AFD / Estação)...</option>
                        {equipamentos.map((eq) => (
                          <option key={eq.id} value={eq.id}>
                            {eq.identificacao || eq.codigo} — Código: {eq.codigo} ({eq.tipo}) [{eq.status}]
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Input File (.txt) */}
                    <div>
                      <label
                        htmlFor="input-arquivo-posicional"
                        className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5"
                      >
                        Arquivo Posicional (.txt) *
                      </label>
                      <div className="rounded-xl border-2 border-dashed border-slate-700/80 bg-slate-950/50 p-5 hover:border-indigo-500/60 transition-colors">
                        <input
                          ref={fileInputRef}
                          id="input-arquivo-posicional"
                          type="file"
                          accept=".txt"
                          onChange={handleFileChange}
                          className="block w-full text-xs text-slate-300 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-indigo-600 file:text-white hover:file:bg-indigo-500 file:cursor-pointer cursor-pointer"
                        />
                        <p className="text-[11px] text-slate-500 mt-2">
                          Formato posicional esperado: Linha inicial tipo <code className="text-slate-300">1</code> (Cabeçalho), linhas de batida tipo <code className="text-slate-300">3</code> (Data <code className="text-slate-300">[1..9]</code>, Hora <code className="text-slate-300">[9..13]</code>, PIS <code className="text-slate-300">[13..24]</code>) e última linha tipo <code className="text-slate-300">9</code> (Trailer).
                        </p>
                      </div>
                    </div>

                    {/* Pré-visualização bruta do arquivo selecionado */}
                    {arquivoSelecionado && (
                      <div className="rounded-xl bg-slate-950/80 border border-slate-800 p-4 space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-semibold text-indigo-300 flex items-center gap-1.5">
                            <FileText size={14} />
                            {arquivoSelecionado.name} ({arquivoSelecionado.size} bytes)
                          </span>
                          <button
                            type="button"
                            onClick={handleBaixarArquivoAtual}
                            className="text-[11px] text-slate-400 hover:text-slate-200 flex items-center gap-1 cursor-pointer"
                          >
                            <Download size={12} /> Baixar .txt
                          </button>
                        </div>
                        {conteudoPreviewTexto && (
                          <pre className="text-[11px] font-mono text-slate-300 bg-slate-900/90 p-3 rounded-lg overflow-x-auto max-h-40 border border-slate-800/80">
                            {conteudoPreviewTexto}
                          </pre>
                        )}
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={loadingPreview || !equipamentoId || !arquivoSelecionado}
                      className="w-full flex items-center justify-center gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-600/25 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {loadingPreview ? (
                        <>
                          <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                          <span>Analisando Arquivo Posicional...</span>
                        </>
                      ) : (
                        <>
                          <Upload size={16} />
                          <span>Analisar Arquivo e Gerar Preview (Etapa 1)</span>
                        </>
                      )}
                    </button>
                  </form>
                </div>
              </div>

              {/* Coluna Explicativa do Layout Posicional AFD */}
              <div className="lg:col-span-5 space-y-4">
                <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl space-y-4">
                  <div className="flex items-center gap-2 text-indigo-400 font-bold text-sm">
                    <HardDrive size={18} />
                    <span>Especificação do Leitor Posicional (Substring)</span>
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    O motor <code className="text-indigo-300">ProcessadorArquivoPosicional</code> lê o arquivo linha a linha usando <code className="text-indigo-300">BufferedReader</code> e extrai os campos por índices fixos:
                  </p>

                  <div className="space-y-2.5 text-xs font-mono">
                    <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800">
                      <span className="text-indigo-400 font-bold">linha.substring(0, 1)</span>
                      <p className="text-slate-400 font-sans text-[11px] mt-0.5">
                        Tipo do Registro: <strong>&apos;1&apos;</strong> = Cabeçalho (1ª linha), <strong>&apos;3&apos;</strong> = Batida de Ponto, <strong>&apos;9&apos;</strong> = Trailer/Totalizador (última linha).
                      </p>
                    </div>
                    <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800">
                      <span className="text-emerald-400 font-bold">linha.substring(1, 9)</span>
                      <p className="text-slate-400 font-sans text-[11px] mt-0.5">
                        Data da batida no formato <code className="text-slate-200">ddMMyyyy</code> (8 caracteres).
                      </p>
                    </div>
                    <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800">
                      <span className="text-amber-400 font-bold">linha.substring(9, 13)</span>
                      <p className="text-slate-400 font-sans text-[11px] mt-0.5">
                        Horário da batida no formato <code className="text-slate-200">HHmm</code> (4 caracteres).
                      </p>
                    </div>
                    <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800">
                      <span className="text-purple-400 font-bold">linha.substring(13, 24)</span>
                      <p className="text-slate-400 font-sans text-[11px] mt-0.5">
                        PIS/PASEP ou Matrícula do colaborador (11 caracteres).
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TELA 2: CONFERÊNCIA (PREVIEW) */}
          {etapaWizard === 2 && previewData && (
            <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl space-y-6">
              {/* Cabeçalho do Preview */}
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800 pb-4">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                      Etapa 2 de 2
                    </span>
                    <h3 className="text-lg font-bold text-white">
                      Tela de Conferência (Preview da Importação)
                    </h3>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">
                    Arquivo: <strong className="text-slate-200 font-mono">{previewData.nomeArquivo}</strong> | Equipamento: <strong className="text-indigo-300">{previewData.identificacaoEquipamento}</strong> | Total de batidas lidas: <strong className="text-white">{previewData.totalLidas}</strong>
                  </p>
                </div>

                <button
                  type="button"
                  onClick={handleCancelarImportacao}
                  disabled={loadingConfirmacao}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-300 transition-colors cursor-pointer"
                >
                  <ArrowLeft size={14} /> Voltar / Trocar Arquivo
                </button>
              </div>

              {/* Cards com os Totais: Válidas (Verde), Repetidas (Amarelo), Recusadas (Vermelho) */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {/* Card Válidas (Verde) */}
                <div className="rounded-2xl bg-emerald-950/30 border border-emerald-500/40 p-5 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
                      Válidas (Prontas p/ Gravar)
                    </span>
                    <div className="text-3xl font-extrabold text-emerald-300 font-mono mt-1">
                      {previewData.totalValidas}
                    </div>
                    <p className="text-[11px] text-emerald-200/70 mt-1">
                      Funcionário identificado e horário inédito
                    </p>
                  </div>
                  <div className="p-3 rounded-2xl bg-emerald-500/20 text-emerald-400">
                    <CheckCircle2 size={28} />
                  </div>
                </div>

                {/* Card Repetidas (Amarelo) */}
                <div className="rounded-2xl bg-amber-950/30 border border-amber-500/40 p-5 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-amber-400">
                      Repetidas (Ignoradas)
                    </span>
                    <div className="text-3xl font-extrabold text-amber-300 font-mono mt-1">
                      {previewData.totalRepetidas}
                    </div>
                    <p className="text-[11px] text-amber-200/70 mt-1">
                      Batida já registrada no sistema ou duplicada
                    </p>
                  </div>
                  <div className="p-3 rounded-2xl bg-amber-500/20 text-amber-400">
                    <AlertTriangle size={28} />
                  </div>
                </div>

                {/* Card Recusadas (Vermelho) */}
                <div className="rounded-2xl bg-rose-950/30 border border-rose-500/40 p-5 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-rose-400">
                      Recusadas (Com Falha)
                    </span>
                    <div className="text-3xl font-extrabold text-rose-300 font-mono mt-1">
                      {previewData.totalRecusadas}
                    </div>
                    <p className="text-[11px] text-rose-200/70 mt-1">
                      Funcionário não encontrado na empresa
                    </p>
                  </div>
                  <div className="p-3 rounded-2xl bg-rose-500/20 text-rose-400">
                    <XCircle size={28} />
                  </div>
                </div>
              </div>

              {/* Filtros e Tabela de Linhas e Motivos */}
              <div className="space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                  <h4 className="text-sm font-bold text-white flex items-center gap-2">
                    <ShieldAlert size={16} className="text-amber-400" />
                    <span>Detalhamento das Linhas e Motivos</span>
                  </h4>

                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => setFiltroTabelaPreview('PROBLEMAS')}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                        filtroTabelaPreview === 'PROBLEMAS'
                          ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                          : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      Linhas com Problemas ({totalProblemas})
                    </button>
                    <button
                      type="button"
                      onClick={() => setFiltroTabelaPreview('VALIDAS')}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                        filtroTabelaPreview === 'VALIDAS'
                          ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                          : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      Válidas ({previewData.totalValidas})
                    </button>
                    <button
                      type="button"
                      onClick={() => setFiltroTabelaPreview('TODAS')}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                        filtroTabelaPreview === 'TODAS'
                          ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40'
                          : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      Todas ({previewData.totalLidas})
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto rounded-xl border border-slate-800 bg-slate-950/60">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 bg-slate-900/90 text-slate-400 uppercase tracking-wider text-[11px]">
                        <th className="py-3 px-4 font-semibold">Linha</th>
                        <th className="py-3 px-4 font-semibold">PIS / Matrícula</th>
                        <th className="py-3 px-4 font-semibold">Colaborador</th>
                        <th className="py-3 px-4 font-semibold">Data e Hora</th>
                        <th className="py-3 px-4 font-semibold">Classificação</th>
                        <th className="py-3 px-4 font-semibold">Motivo</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/70">
                      {linhasTabela.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="py-8 text-center text-slate-400">
                            {filtroTabelaPreview === 'PROBLEMAS'
                              ? 'Nenhuma linha com problema (repetida ou recusada) foi encontrada neste arquivo!'
                              : 'Nenhum registro para exibir neste filtro.'}
                          </td>
                        </tr>
                      ) : (
                        linhasTabela.map((item, idx) => (
                          <tr
                            key={`${item.numeroLinha}-${idx}`}
                            className="hover:bg-slate-900/50 transition-colors"
                          >
                            <td className="py-3 px-4 font-mono text-slate-300">
                              #{item.numeroLinha}
                            </td>
                            <td className="py-3 px-4 font-mono text-slate-200">
                              {item.pis}
                            </td>
                            <td className="py-3 px-4 text-slate-200 font-medium">
                              {item.nomeFuncionario || (
                                <span className="text-rose-400 italic">Não identificado</span>
                              )}
                            </td>
                            <td className="py-3 px-4 font-mono text-slate-300">
                              {formatarDataHora(item.dataHora)}
                            </td>
                            <td className="py-3 px-4">
                              {item.categoria === 'VALIDA' && (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                                  <CheckCircle2 size={11} /> VÁLIDA
                                </span>
                              )}
                              {item.categoria === 'REPETIDA' && (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/15 text-amber-300 border border-amber-500/30">
                                  <AlertTriangle size={11} /> REPETIDA
                                </span>
                              )}
                              {item.categoria === 'RECUSADA' && (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/15 text-rose-300 border border-rose-500/30">
                                  <XCircle size={11} /> RECUSADA
                                </span>
                              )}
                            </td>
                            <td className="py-3 px-4 text-slate-300">
                              {item.motivo}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Aceite do RH e Botões de Ação ("Cancelar Importação" ou "Confirmar Gravação") */}
              <div className="rounded-xl bg-slate-950/90 border border-slate-800 p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <label className="flex items-start sm:items-center gap-3 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={aceiteRhConfirmado}
                    onChange={(e) => setAceiteRhConfirmado(e.target.checked)}
                    className="mt-0.5 sm:mt-0 h-4 w-4 rounded border-slate-700 text-indigo-600 focus:ring-indigo-500 bg-slate-900 cursor-pointer"
                  />
                  <span className="text-xs text-slate-200">
                    Conferi os totais e motivos acima e <strong>autorizo a gravação definitiva</strong> das{' '}
                    <strong className="text-emerald-400">{previewData.totalValidas} batida(s) válida(s)</strong> no banco de dados.
                  </span>
                </label>

                <div className="flex items-center gap-3 shrink-0">
                  <button
                    type="button"
                    onClick={handleCancelarImportacao}
                    disabled={loadingConfirmacao}
                    className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition-colors cursor-pointer disabled:opacity-50"
                  >
                    <X size={15} />
                    <span>Cancelar Importação</span>
                  </button>

                  <button
                    type="button"
                    onClick={handleConfirmarGravacao}
                    disabled={loadingConfirmacao || !aceiteRhConfirmado}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold text-white shadow-lg shadow-emerald-600/25 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {loadingConfirmacao ? (
                      <>
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                        <span>Gravando Lote...</span>
                      </>
                    ) : (
                      <>
                        <Check size={15} />
                        <span>Confirmar Gravação</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ABA DE HISTÓRICO DE IMPORTAÇÕES */}
      {subAba === 'historico' && (
        <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-800 pb-4">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                <History size={20} />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">
                  Histórico de Importações Realizadas
                </h3>
                <p className="text-xs text-slate-400">
                  Consulta <span className="font-mono text-indigo-300">GET /api/v1/importacao/historico</span>
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={carregarHistorico}
                disabled={loadingHistorico}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition-colors cursor-pointer"
              >
                <RefreshCw size={13} className={loadingHistorico ? 'animate-spin' : ''} />
                <span>Atualizar</span>
              </button>
              <button
                type="button"
                onClick={() => setSubAba('wizard')}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white transition-colors cursor-pointer"
              >
                <Upload size={13} />
                <span>Nova Importação</span>
              </button>
            </div>
          </div>

          {loadingHistorico ? (
            <div className="py-12 flex items-center justify-center gap-2 text-sm text-slate-400">
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
              <span>Carregando histórico de importações...</span>
            </div>
          ) : historicoList.length === 0 ? (
            <div className="py-12 text-center border border-dashed border-slate-800 rounded-xl space-y-2">
              <History size={32} className="mx-auto text-slate-600" />
              <p className="text-sm text-slate-300 font-medium">
                Nenhuma importação registrada até o momento.
              </p>
              <p className="text-xs text-slate-500">
                Utilize o Assistente de Importação para enviar um arquivo posicional (.txt).
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-800 bg-slate-950/60">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-800 bg-slate-900/90 text-slate-400 uppercase tracking-wider text-[11px]">
                    <th className="py-3.5 px-4 font-semibold">Data / Hora</th>
                    <th className="py-3.5 px-4 font-semibold">Quem Importou (Autor)</th>
                    <th className="py-3.5 px-4 font-semibold">Equipamento</th>
                    <th className="py-3.5 px-4 font-semibold">Arquivo</th>
                    <th className="py-3.5 px-4 font-semibold text-center">Lidas</th>
                    <th className="py-3.5 px-4 font-semibold text-center text-emerald-400">Válidas</th>
                    <th className="py-3.5 px-4 font-semibold text-center text-amber-400">Repetidas</th>
                    <th className="py-3.5 px-4 font-semibold text-center text-rose-400">Recusadas</th>
                    <th className="py-3.5 px-4 font-semibold text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/70">
                  {historicoList.map((h) => (
                    <tr key={h.id} className="hover:bg-slate-900/50 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-slate-300 whitespace-nowrap">
                        <span className="inline-flex items-center gap-1.5">
                          <Clock size={12} className="text-slate-500" />
                          {formatarDataHora(h.dataImportacao)}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-slate-200 font-medium">
                        <span className="inline-flex items-center gap-1.5">
                          <User size={12} className="text-indigo-400" />
                          {h.nomeAutor || 'RH'}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-slate-300">
                        {h.identificacaoEquipamento || '—'}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-indigo-300">
                        {h.nomeArquivo}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-center text-slate-300">
                        {h.totalLidas}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-center font-bold text-emerald-400">
                        {h.totalValidas}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-center font-bold text-amber-400">
                        {h.totalRepetidas}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-center font-bold text-rose-400">
                        {h.totalRecusadas}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${
                            h.status === 'CONCLUIDA'
                              ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
                              : h.status === 'PENDENTE'
                              ? 'bg-amber-500/15 text-amber-300 border-amber-500/30'
                              : 'bg-rose-500/15 text-rose-300 border-rose-500/30'
                          }`}
                        >
                          {h.status || 'CONCLUIDA'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
