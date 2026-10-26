import React, { useState, useEffect, useRef, useId } from 'react';
import batidaService from '../services/batidaService';
import {
  UserCheck,
  AlertTriangle,
  Clock,
  HardDrive,
  FileText,
  RefreshCw,
  Search,
  CheckCircle2,
  Link2,
  X,
  User,
  ShieldCheck,
  Sparkles,
  Download,
} from 'lucide-react';
import toast from 'react-hot-toast';

export default function BatidasOrfasSection({
  funcionarios = [],
  onVinculoConcluido,
  onIrParaImportacao,
}) {
  const [statusFiltro, setStatusFiltro] = useState('ORFA_PENDENTE');
  const [batidasOrfas, setBatidasOrfas] = useState([]);
  const [loading, setLoading] = useState(false);
  const [termoBusca, setTermoBusca] = useState('');

  const [batidaSelecionada, setBatidaSelecionada] = useState(null);
  const [funcionarioIdEscolhido, setFuncionarioIdEscolhido] = useState('');
  const [motivoVinculo, setMotivoVinculo] = useState('');
  const [submittingVinculo, setSubmittingVinculo] = useState(false);

  const dialogRef = useRef(null);
  const dialogTitleId = useId();

  const carregarBatidas = async (status = statusFiltro) => {
    try {
      setLoading(true);
      const data = await batidaService.listarBatidasOrfas(status);
      setBatidasOrfas(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.message || 'Erro ao carregar batidas órfãs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarBatidas(statusFiltro);
  }, [statusFiltro]);

  const encontrarColaboradorSugerido = (identificador) => {
    if (!identificador || !Array.isArray(funcionarios)) return null;
    const limpo = String(identificador).trim();
    const digitos = limpo.replace(/\D/g, '');
    const semZeros = limpo.replace(/^0+(?!$)/, '');

    return (
      funcionarios.find((f) => {
        const pisLimpo = (f.pisPasep || '').replace(/\D/g, '');
        const cpfLimpo = (f.cpf || '').replace(/\D/g, '');
        const matLimpa = String(f.matricula || '').trim();
        return (
          (digitos && (pisLimpo === digitos || cpfLimpo === digitos)) ||
          matLimpa === limpo ||
          (semZeros && matLimpa === semZeros)
        );
      }) || null
    );
  };

  const abrirModalVinculo = (batida) => {
    setBatidaSelecionada(batida);
    const sugerido = encontrarColaboradorSugerido(batida.identificadorOriginal);
    setFuncionarioIdEscolhido(sugerido?.id || '');
    setMotivoVinculo(
      sugerido
        ? 'Colaborador recém-cadastrado após importação do arquivo AFD'
        : ''
    );
    if (dialogRef.current && !dialogRef.current.open) {
      dialogRef.current.showModal();
    }
  };

  const fecharModalVinculo = () => {
    if (dialogRef.current && dialogRef.current.open) {
      dialogRef.current.close();
    }
    setBatidaSelecionada(null);
    setFuncionarioIdEscolhido('');
    setMotivoVinculo('');
  };

  const handleConfirmarVinculo = async (e) => {
    e.preventDefault();
    if (!batidaSelecionada) return;

    if (!funcionarioIdEscolhido) {
      toast.error('Selecione o colaborador para vincular a batida.');
      return;
    }

    if (!motivoVinculo || !motivoVinculo.trim()) {
      toast.error('Informe o motivo do vínculo.');
      return;
    }

    try {
      setSubmittingVinculo(true);
      const resultado = await batidaService.vincularBatidaOrfa(batidaSelecionada.id, {
        funcionarioId: funcionarioIdEscolhido,
        motivo: motivoVinculo.trim(),
      });

      toast.success(
        `Batida NSR #${resultado.nsr} vinculada a ${resultado.nomeFuncionarioVinculado}!`
      );
      fecharModalVinculo();
      await carregarBatidas(statusFiltro);
      if (onVinculoConcluido) {
        onVinculoConcluido(resultado);
      }
    } catch (err) {
      toast.error(err.message || 'Não foi possível vincular a batida órfã.');
    } finally {
      setSubmittingVinculo(false);
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

  const handleBaixarArquivo = (batida) => {
    if (!batida?.conteudoArquivo) {
      toast.error('Conteúdo do arquivo não disponível para download.');
      return;
    }
    const blob = new Blob([batida.conteudoArquivo], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = batida.nomeArquivo || 'importacao_afd.txt';
    a.click();
    URL.revokeObjectURL(url);
  };

  const batidasFiltradas = batidasOrfas.filter((b) => {
    if (!termoBusca.trim()) return true;
    const termo = termoBusca.toLowerCase().trim();
    return (
      String(b.identificadorOriginal || '').toLowerCase().includes(termo) ||
      String(b.identificacaoEquipamento || '').toLowerCase().includes(termo) ||
      String(b.codigoEquipamento || '').toLowerCase().includes(termo) ||
      String(b.nomeArquivo || '').toLowerCase().includes(termo) ||
      String(b.nomeFuncionarioVinculado || '').toLowerCase().includes(termo) ||
      String(b.vinculadoPor || '').toLowerCase().includes(termo) ||
      String(b.nsr || '').includes(termo)
    );
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-slate-900/80 border border-slate-800 rounded-2xl p-5 backdrop-blur-xl">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <AlertTriangle size={22} />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">
              Tratamento de Batidas Órfãs (T10)
            </h2>
            <p className="text-xs text-slate-400">
              Marcações recebidas sem colaborador identificado ficam guardadas como órfãs até confirmação manual do RH com registro de autor, data e motivo.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 bg-slate-950/80 p-1.5 rounded-xl border border-slate-800">
          <button
            type="button"
            onClick={() => setStatusFiltro('ORFA_PENDENTE')}
            className={`px-3.5 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer ${
              statusFiltro === 'ORFA_PENDENTE'
                ? 'bg-amber-600 text-white shadow-md shadow-amber-600/25'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <AlertTriangle size={14} />
            <span>Pendentes de Vínculo</span>
          </button>
          <button
            type="button"
            onClick={() => setStatusFiltro('VINCULADA')}
            className={`px-3.5 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer ${
              statusFiltro === 'VINCULADA'
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/25'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <CheckCircle2 size={14} />
            <span>Histórico de Vinculadas</span>
          </button>
        </div>
      </div>

      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl space-y-5">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800 pb-4">
          <div className="relative flex-1 max-w-md">
            <Search
              size={15}
              className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500"
            />
            <input
              type="search"
              value={termoBusca}
              onChange={(e) => setTermoBusca(e.target.value)}
              placeholder="Buscar por PIS/CPF/Matrícula, equipamento, arquivo ou NSR..."
              className="w-full rounded-xl border border-slate-700/80 bg-slate-950/80 pl-9 pr-3.5 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => carregarBatidas(statusFiltro)}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition-colors cursor-pointer"
            >
              <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
              <span>Atualizar Lista</span>
            </button>
            {onIrParaImportacao && (
              <button
                type="button"
                onClick={onIrParaImportacao}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/30 border border-indigo-500/30 text-xs font-semibold text-indigo-300 transition-colors cursor-pointer"
              >
                <FileText size={13} />
                <span>Importar Arquivo AFD</span>
              </button>
            )}
          </div>
        </div>

        {loading ? (
          <div className="py-12 flex items-center justify-center gap-2 text-sm text-slate-400">
            <div className="h-4 w-4 animate-spin rounded-full border-2 border-amber-500 border-t-transparent" />
            <span>Carregando marcações...</span>
          </div>
        ) : batidasFiltradas.length === 0 ? (
          <div className="py-12 text-center border border-dashed border-slate-800 rounded-xl space-y-2">
            <UserCheck size={32} className="mx-auto text-slate-600" />
            <p className="text-sm text-slate-300 font-medium">
              {statusFiltro === 'ORFA_PENDENTE'
                ? 'Nenhuma batida órfã pendente de vínculo no momento.'
                : 'Nenhuma batida órfã vinculada encontrada neste filtro.'}
            </p>
            <p className="text-xs text-slate-500">
              {statusFiltro === 'ORFA_PENDENTE'
                ? 'Quando um arquivo AFD trouxer um PIS, CPF ou Matrícula ainda não cadastrado, a batida aparecerá aqui sem ser descartada.'
                : 'As batidas órfãs associadas manualmente pelo RH ficam registradas aqui com autor, data e motivo.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-slate-800 bg-slate-950/60">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/90 text-slate-400 uppercase tracking-wider text-[11px]">
                  <th className="py-3.5 px-4 font-semibold">NSR</th>
                  <th className="py-3.5 px-4 font-semibold">Data / Hora</th>
                  <th className="py-3.5 px-4 font-semibold">Número Recebido</th>
                  <th className="py-3.5 px-4 font-semibold">Equipamento</th>
                  <th className="py-3.5 px-4 font-semibold">Arquivo</th>
                  {statusFiltro === 'ORFA_PENDENTE' ? (
                    <>
                      <th className="py-3.5 px-4 font-semibold">Cadastro Posterior</th>
                      <th className="py-3.5 px-4 font-semibold text-right">Ação do RH</th>
                    </>
                  ) : (
                    <>
                      <th className="py-3.5 px-4 font-semibold">Colaborador Vinculado</th>
                      <th className="py-3.5 px-4 font-semibold">Auditoria (Autor / Data)</th>
                      <th className="py-3.5 px-4 font-semibold">Motivo</th>
                    </>
                  )}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/70">
                {batidasFiltradas.map((b) => {
                  const colaboradorCoincidente =
                    statusFiltro === 'ORFA_PENDENTE'
                      ? encontrarColaboradorSugerido(b.identificadorOriginal)
                      : null;

                  return (
                    <tr key={b.id} className="hover:bg-slate-900/50 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-slate-300">
                        #{b.nsr}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-200 whitespace-nowrap">
                        <span className="inline-flex items-center gap-1.5">
                          <Clock size={12} className="text-amber-400" />
                          {formatarDataHora(b.dataHora)}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="px-2.5 py-1 rounded-lg bg-amber-500/10 border border-amber-500/30 font-mono font-bold text-amber-300">
                          {b.identificadorOriginal || '—'}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-slate-300">
                        <div className="flex items-center gap-1.5">
                          <HardDrive size={12} className="text-indigo-400" />
                          <span>{b.identificacaoEquipamento}</span>
                          {b.codigoEquipamento && b.codigoEquipamento !== 'N/A' && (
                            <span className="text-[10px] font-mono text-slate-500">
                              ({b.codigoEquipamento})
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 font-mono text-indigo-300">
                        <div className="flex items-center gap-2">
                          <span>{b.nomeArquivo || '—'}</span>
                          {b.conteudoArquivo && (
                            <button
                              type="button"
                              onClick={() => handleBaixarArquivo(b)}
                              className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-indigo-500/15 hover:bg-indigo-500/25 border border-indigo-500/30 text-[11px] font-sans font-semibold text-indigo-300 transition-colors cursor-pointer"
                              title="Baixar arquivo .txt original"
                            >
                              <Download size={12} />
                              <span>.txt</span>
                            </button>
                          )}
                        </div>
                      </td>

                      {statusFiltro === 'ORFA_PENDENTE' ? (
                        <>
                          <td className="py-3.5 px-4">
                            {colaboradorCoincidente ? (
                              <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-indigo-500/15 border border-indigo-500/30 text-indigo-200 text-[11px]">
                                <Sparkles size={12} className="text-indigo-400" />
                                <span>
                                  Encontrado: <strong>{colaboradorCoincidente.nomeCompleto}</strong> (aguardando confirmação do RH)
                                </span>
                              </div>
                            ) : (
                              <span className="text-slate-500 italic">
                                Sem cadastro correspondente ainda
                              </span>
                            )}
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <button
                              type="button"
                              onClick={() => abrirModalVinculo(b)}
                              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-xs font-semibold text-white shadow-md shadow-amber-600/20 transition-all cursor-pointer"
                            >
                              <Link2 size={13} />
                              <span>Vincular Colaborador</span>
                            </button>
                          </td>
                        </>
                      ) : (
                        <>
                          <td className="py-3.5 px-4 font-semibold text-emerald-300">
                            <span className="inline-flex items-center gap-1.5">
                              <User size={13} />
                              {b.nomeFuncionarioVinculado || '—'}
                            </span>
                          </td>
                          <td className="py-3.5 px-4 text-slate-300">
                            <div className="font-medium text-slate-200">
                              {b.vinculadoPor || 'RH'}
                            </div>
                            <div className="text-[11px] font-mono text-slate-400">
                              {formatarDataHora(b.dataVinculo)}
                            </div>
                          </td>
                          <td className="py-3.5 px-4 text-slate-200">
                            {b.motivoVinculo || '—'}
                          </td>
                        </>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <dialog
        ref={dialogRef}
        aria-labelledby={dialogTitleId}
        onClose={() => {
          setBatidaSelecionada(null);
        }}
        className="m-auto w-full max-w-lg rounded-2xl bg-slate-900 border border-slate-700 p-0 text-slate-100 shadow-2xl"
      >
        {batidaSelecionada && (
          <form onSubmit={handleConfirmarVinculo} className="p-6 space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-amber-500/15 text-amber-400 border border-amber-500/30">
                  <Link2 size={18} />
                </div>
                <div>
                  <h3 id={dialogTitleId} className="text-base font-bold text-white">
                    Vincular Batida Órfã (NSR #{batidaSelecionada.nsr})
                  </h3>
                  <p className="text-xs text-slate-400">
                    Confirmação manual do RH — passa a contar na apuração do colaborador
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={fecharModalVinculo}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X size={16} />
              </button>
            </div>

            <div className="rounded-xl bg-slate-950/80 border border-slate-800 p-4 grid grid-cols-2 gap-3 text-xs">
              <div>
                <span className="text-slate-500 block">Número Recebido</span>
                <span className="font-mono font-bold text-amber-300 text-sm">
                  {batidaSelecionada.identificadorOriginal}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Data e Hora da Batida</span>
                <span className="font-mono text-slate-200">
                  {formatarDataHora(batidaSelecionada.dataHora)}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Equipamento</span>
                <span className="text-slate-200">
                  {batidaSelecionada.identificacaoEquipamento}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Arquivo de Origem</span>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className="font-mono text-indigo-300">
                    {batidaSelecionada.nomeArquivo || '—'}
                  </span>
                  {batidaSelecionada.conteudoArquivo && (
                    <button
                      type="button"
                      onClick={() => handleBaixarArquivo(batidaSelecionada)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-indigo-500/20 hover:bg-indigo-500/30 text-[10px] font-semibold text-indigo-200 cursor-pointer"
                    >
                      <Download size={11} />
                      <span>Baixar .txt</span>
                    </button>
                  )}
                </div>
              </div>
            </div>

            <div>
              <label
                htmlFor="select-colaborador-vinculo-orfa"
                className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5"
              >
                Colaborador *
              </label>
              <select
                id="select-colaborador-vinculo-orfa"
                required
                value={funcionarioIdEscolhido}
                onChange={(e) => setFuncionarioIdEscolhido(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 focus:border-amber-500 focus:outline-none"
              >
                <option value="">Selecione o colaborador...</option>
                {funcionarios.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.nomeCompleto} — Matrícula: {f.matricula || '—'} | PIS: {f.pisPasep || '—'} | CPF: {f.cpf || '—'}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label
                htmlFor="input-motivo-vinculo-orfa"
                className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5"
              >
                Motivo do Vínculo (Auditoria Obrigatória) *
              </label>
              <textarea
                id="input-motivo-vinculo-orfa"
                required
                rows={3}
                value={motivoVinculo}
                onChange={(e) => setMotivoVinculo(e.target.value)}
                placeholder="Ex: Colaborador admitido ontem, cujo cadastro foi concluído após a importação do relógio."
                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
              />
            </div>

            <div className="rounded-xl bg-indigo-950/30 border border-indigo-500/30 p-3 flex items-start gap-2.5 text-[11px] text-indigo-200">
              <ShieldCheck size={16} className="text-indigo-400 shrink-0 mt-0.5" />
              <span>
                Este vínculo ficará registrado com seu usuário como <strong>autor</strong>, a <strong>data/hora atual</strong> e o <strong>motivo</strong> informado acima.
              </span>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2 border-t border-slate-800">
              <button
                type="button"
                onClick={fecharModalVinculo}
                disabled={submittingVinculo}
                className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-300 transition-colors cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={submittingVinculo || !funcionarioIdEscolhido || !motivoVinculo.trim()}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-xs font-semibold text-white shadow-lg shadow-amber-600/25 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submittingVinculo ? (
                  <>
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                    <span>Vinculando...</span>
                  </>
                ) : (
                  <>
                    <UserCheck size={15} />
                    <span>Confirmar Vínculo</span>
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </dialog>
    </div>
  );
}
