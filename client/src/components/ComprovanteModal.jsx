import React, { useEffect } from 'react';
import {
  X,
  Printer,
  CheckCircle2,
  Building2,
  User,
  Clock,
  Hash,
  ShieldCheck,
  FileCheck2,
} from 'lucide-react';
import {
  formatarCpf,
  formatarCnpj,
  formatarNsr,
  formatarDataHora,
} from '../utils/brazilianDocuments';

/**
 * Modal e Visualizador do Comprovante de Registro de Ponto
 * Conforme exigências da Portaria 671 MTE (Comprovante de Registro de Ponto do Trabalhador)
 *
 * @param {{
 *   isOpen: boolean,
 *   onClose: () => void,
 *   comprovante: {
 *     id?: string,
 *     nsr?: number,
 *     nomeEmpresa?: string,
 *     cnpjEmpresa?: string,
 *     nomeFuncionario?: string,
 *     cpfFuncionario?: string,
 *     dataHoraBatida?: string,
 *     tipoRegistro?: 'ENTRADA' | 'SAIDA',
 *     tipoOrigem?: string
 *   } | null
 * }} props
 */
export function ComprovanteModal({ isOpen, onClose, comprovante }) {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !comprovante) return null;

  const handlePrint = () => {
    window.print();
  };

  const isEntrada = comprovante.tipoRegistro === 'ENTRADA';
  const tipoOrigemFormatada =
    comprovante.tipoOrigem === 'APLICATIVO_WEB'
      ? 'Aplicativo Web (Navegador)'
      : comprovante.tipoOrigem === 'ESTACAO'
      ? 'Estação de Trabalho Física'
      : comprovante.tipoOrigem || 'Sistema Web';

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="comprovante-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-950/85 backdrop-blur-md animate-fadeIn"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Barra superior de controle (não impressa) */}
        <div className="no-print flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
          <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
            <CheckCircle2 size={18} />
            <span>Comprovante Emitido com Sucesso</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar comprovante"
            className="p-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Corpo do Comprovante - Formatado para Tela e Impressão */}
        <div className="overflow-y-auto p-6 sm:p-8 space-y-6">
          <div
            id="comprovante-print-card"
            className="rounded-2xl bg-slate-950/80 border border-slate-800/90 p-6 sm:p-7 relative shadow-inner text-slate-200"
          >
            {/* Cabeçalho Oficial do Comprovante */}
            <div className="text-center pb-5 border-b border-dashed border-slate-700/80 print-border-dashed">
              <div className="inline-flex items-center justify-center p-2 rounded-xl bg-emerald-500/10 text-emerald-400 mb-2 border border-emerald-500/20">
                <FileCheck2 size={24} />
              </div>
              <h2
                id="comprovante-title"
                className="text-base sm:text-lg font-bold tracking-tight text-white uppercase"
              >
                Comprovante de Registro de Ponto
              </h2>
              <p className="text-[11px] font-mono text-slate-400 mt-0.5 uppercase tracking-wider">
                Portaria 671 MTE • REP-P
              </p>
            </div>

            {/* Destaque do NSR */}
            <div className="my-5 p-4 rounded-xl bg-slate-900/90 border border-slate-800 text-center">
              <span className="text-[10px] sm:text-[11px] uppercase tracking-wider text-slate-400 font-semibold flex items-center justify-center gap-1">
                <Hash size={13} className="text-emerald-400" />
                Número Sequencial de Registro (NSR)
              </span>
              <div className="font-mono text-2xl sm:text-3xl font-black text-emerald-400 tracking-wider mt-1">
                #{formatarNsr(comprovante.nsr)}
              </div>
            </div>

            {/* Dados do Empregador */}
            <div className="space-y-3 py-3 border-b border-dashed border-slate-800 print-border-dashed text-xs">
              <div className="flex items-center gap-1.5 font-semibold text-slate-300">
                <Building2 size={14} className="text-indigo-400" />
                <span>Dados do Empregador</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pl-5">
                <div>
                  <span className="text-slate-400 text-[11px]">Razão Social:</span>
                  <p className="font-semibold text-white truncate" title={comprovante.nomeEmpresa}>
                    {comprovante.nomeEmpresa || '—'}
                  </p>
                </div>
                <div>
                  <span className="text-slate-400 text-[11px]">CNPJ:</span>
                  <p className="font-mono text-slate-200">
                    {formatarCnpj(comprovante.cnpjEmpresa) || '—'}
                  </p>
                </div>
              </div>
            </div>

            {/* Dados do Empregado */}
            <div className="space-y-3 py-3 border-b border-dashed border-slate-800 print-border-dashed text-xs">
              <div className="flex items-center gap-1.5 font-semibold text-slate-300">
                <User size={14} className="text-indigo-400" />
                <span>Dados do Empregado</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pl-5">
                <div>
                  <span className="text-slate-400 text-[11px]">Nome Completo:</span>
                  <p className="font-semibold text-white truncate" title={comprovante.nomeFuncionario}>
                    {comprovante.nomeFuncionario || '—'}
                  </p>
                </div>
                <div>
                  <span className="text-slate-400 text-[11px]">CPF:</span>
                  <p className="font-mono text-slate-200">
                    {formatarCpf(comprovante.cpfFuncionario) || '—'}
                  </p>
                </div>
              </div>
            </div>

            {/* Detalhes da Marcação */}
            <div className="space-y-3 pt-3 text-xs">
              <div className="flex items-center gap-1.5 font-semibold text-slate-300">
                <Clock size={14} className="text-indigo-400" />
                <span>Dados da Marcação</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pl-5">
                <div>
                  <span className="text-slate-400 text-[11px]">Data e Hora Oficial:</span>
                  <p className="font-mono font-bold text-white text-sm mt-0.5">
                    {formatarDataHora(comprovante.dataHoraBatida)}
                  </p>
                  <span className="text-[10px] text-emerald-400/90 font-medium">
                    (Timestamp emitido pelo servidor)
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 text-[11px]">Tipo de Registro:</span>
                  <div className="mt-1">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        isEntrada
                          ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                          : 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                      }`}
                    >
                      <span className="h-1.5 w-1.5 rounded-full bg-current" />
                      {comprovante.tipoRegistro || 'ENTRADA'}
                    </span>
                  </div>
                </div>
                <div className="sm:col-span-2">
                  <span className="text-slate-400 text-[11px]">Origem do Registro:</span>
                  <p className="font-medium text-slate-300">{tipoOrigemFormatada}</p>
                </div>
                {comprovante.id && (
                  <div className="sm:col-span-2">
                    <span className="text-slate-500 text-[10px]">Identificador Único (ID):</span>
                    <p className="font-mono text-[10px] text-slate-400 truncate" title={comprovante.id}>
                      {comprovante.id}
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Rodapé Jurídico */}
            <div className="mt-6 pt-4 border-t border-dashed border-slate-800 print-border-dashed text-center">
              <div className="flex items-center justify-center gap-1 text-[11px] text-slate-400 font-medium">
                <ShieldCheck size={13} className="text-emerald-400" />
                <span>Registro Eletrônico Imutável • Portaria MTE 671</span>
              </div>
              <p className="text-[10px] text-slate-500 mt-1 max-w-xs mx-auto">
                Este comprovante tem validade jurídica e comprova o registro de ponto no sistema oficial.
              </p>
            </div>
          </div>
        </div>

        {/* Ações Inferiores (não impressas) */}
        <div className="no-print p-4 sm:px-8 sm:py-5 border-t border-slate-800 bg-slate-950/70 flex flex-col sm:flex-row items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="w-full sm:w-auto px-5 py-2.5 rounded-xl border border-slate-700/80 bg-slate-900 hover:bg-slate-800 text-xs sm:text-sm font-semibold text-slate-300 hover:text-white transition-colors cursor-pointer text-center"
          >
            Fechar
          </button>
          <button
            type="button"
            onClick={handlePrint}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-xs sm:text-sm font-bold text-white shadow-lg shadow-emerald-700/20 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer"
          >
            <Printer size={16} />
            <span>Imprimir Comprovante</span>
          </button>
        </div>
      </div>
    </div>
  );
}

export default ComprovanteModal;
