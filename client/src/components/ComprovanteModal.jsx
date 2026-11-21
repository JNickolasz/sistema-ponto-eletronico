import React from 'react';
import {
  Building2,
  Clock,
  FileCheck2,
  Hash,
  Printer,
  ShieldCheck,
  User,
} from 'lucide-react';
import {
  formatarCnpj,
  formatarCpf,
  formatarDataHora,
  formatarNsr,
} from '../utils/brazilianDocuments';
import Modal from './ui/Modal';
import { Badge, Button } from './ui/Primitives';

export function ComprovanteModal({ isOpen, onClose, comprovante }) {
  if (!comprovante) return null;

  const handlePrint = () => {
    window.print();
  };

  const isEntrada = comprovante.tipoRegistro === 'ENTRADA';
  const tipoOrigemFormatada =
    comprovante.tipoOrigem === 'APLICATIVO_WEB'
      ? 'Aplicativo Web (Navegador)'
      : comprovante.tipoOrigem === 'ESTACAO'
        ? 'Estação de Ponto Física'
        : comprovante.tipoOrigem === 'ARQUIVO_IMPORTADO'
          ? 'Relógio de Ponto Físico (AFD)'
          : comprovante.tipoOrigem === 'AJUSTE_MANUAL'
            ? 'Ajuste Manual Aprovado'
            : comprovante.tipoOrigem || 'Sistema Web';

  return (
    <Modal
      open={Boolean(isOpen)}
      onClose={onClose}
      title="Comprovante de Registro de Ponto"
      subtitle="Emitido conforme Portaria 671/MTP · REP-P"
      maxWidth="max-w-lg"
    >
      <div className="space-y-5">
        <div
          id="comprovante-print-card"
          className="rounded-2xl border border-[var(--border-subtle)] bg-[var(--surface-page)] p-5 sm:p-6 space-y-5"
        >
          {/* Cabeçalho Oficial do Comprovante */}
          <div className="text-center pb-4 border-b border-dashed border-[var(--border-subtle)]">
            <div
              className="mx-auto inline-flex h-11 w-11 items-center justify-center rounded-xl mb-2"
              style={{ backgroundColor: 'var(--brand-50)', color: 'var(--brand-600)' }}
            >
              <FileCheck2 size={22} />
            </div>
            <h3 className="text-base font-bold tracking-tight text-[var(--text-primary)] uppercase">
              Comprovante de Registro de Ponto
            </h3>
            <p className="text-xs font-mono-tabular text-[var(--text-muted)] mt-0.5 uppercase tracking-wider">
              Portaria 671/MTP · REP-P
            </p>
          </div>

          {/* Destaque do NSR */}
          <div className="p-4 rounded-xl bg-[var(--surface-card)] border border-[var(--border-subtle)] text-center">
            <span className="text-xs uppercase tracking-wider text-[var(--text-muted)] font-semibold flex items-center justify-center gap-1.5">
              <Hash size={14} style={{ color: 'var(--brand-600)' }} />
              Número Sequencial de Registro (NSR)
            </span>
            <div
              className="font-mono-tabular text-2xl sm:text-3xl font-extrabold tracking-wider mt-1"
              style={{ color: 'var(--brand-600)' }}
            >
              #{formatarNsr(comprovante.nsr)}
            </div>
          </div>

          {/* Dados do Empregador */}
          <div className="space-y-2.5 pb-4 border-b border-dashed border-[var(--border-subtle)] text-xs">
            <div className="flex items-center gap-1.5 font-bold text-[var(--text-primary)]">
              <Building2 size={15} style={{ color: 'var(--brand-600)' }} />
              <span>Dados do Empregador</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pl-5">
              <div>
                <span className="text-[var(--text-muted)]">Razão Social:</span>
                <p
                  className="font-semibold text-[var(--text-primary)] truncate"
                  title={comprovante.nomeEmpresa}
                >
                  {comprovante.nomeEmpresa || '—'}
                </p>
              </div>
              <div>
                <span className="text-[var(--text-muted)]">CNPJ:</span>
                <p className="font-mono-tabular font-semibold text-[var(--text-primary)]">
                  {formatarCnpj(comprovante.cnpjEmpresa) || '—'}
                </p>
              </div>
            </div>
          </div>

          {/* Dados do Empregado */}
          <div className="space-y-2.5 pb-4 border-b border-dashed border-[var(--border-subtle)] text-xs">
            <div className="flex items-center gap-1.5 font-bold text-[var(--text-primary)]">
              <User size={15} style={{ color: 'var(--brand-600)' }} />
              <span>Dados do Trabalhador</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pl-5">
              <div>
                <span className="text-[var(--text-muted)]">Nome Completo:</span>
                <p
                  className="font-semibold text-[var(--text-primary)] truncate"
                  title={comprovante.nomeFuncionario}
                >
                  {comprovante.nomeFuncionario || '—'}
                </p>
              </div>
              <div>
                <span className="text-[var(--text-muted)]">CPF:</span>
                <p className="font-mono-tabular font-semibold text-[var(--text-primary)]">
                  {formatarCpf(comprovante.cpfFuncionario) || '—'}
                </p>
              </div>
            </div>
          </div>

          {/* Detalhes da Marcação */}
          <div className="space-y-2.5 text-xs">
            <div className="flex items-center gap-1.5 font-bold text-[var(--text-primary)]">
              <Clock size={15} style={{ color: 'var(--brand-600)' }} />
              <span>Dados da Marcação</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pl-5">
              <div>
                <span className="text-[var(--text-muted)]">Data e Hora Oficial:</span>
                <p className="font-mono-tabular font-bold text-[var(--text-primary)] text-sm mt-0.5">
                  {formatarDataHora(comprovante.dataHoraBatida || comprovante.dataHoraServidor)}
                </p>
              </div>
              <div>
                <span className="text-[var(--text-muted)] block">Tipo de Registro:</span>
                <div className="mt-1">
                  <Badge tone={isEntrada ? 'success' : 'brand'}>
                    {comprovante.tipoRegistro || 'ENTRADA'}
                  </Badge>
                </div>
              </div>
              <div className="sm:col-span-2">
                <span className="text-[var(--text-muted)]">Origem do Registro:</span>
                <p className="font-semibold text-[var(--text-primary)]">{tipoOrigemFormatada}</p>
              </div>
            </div>
          </div>

          {/* Rodapé Jurídico */}
          <div className="pt-4 border-t border-dashed border-[var(--border-subtle)] text-center">
            <div className="flex items-center justify-center gap-1.5 text-xs text-[var(--text-secondary)] font-semibold">
              <ShieldCheck size={14} style={{ color: 'var(--brand-600)' }} />
              <span>Registro Eletrônico Imutável · Portaria MTE 671</span>
            </div>
          </div>
        </div>

        <div className="no-print flex items-center justify-end gap-2.5 pt-2 border-t border-[var(--border-subtle)]">
          <Button variant="secondary" onClick={onClose}>
            Fechar
          </Button>
          <Button variant="primary" onClick={handlePrint}>
            <Printer size={16} />
            Imprimir Comprovante
          </Button>
        </div>
      </div>
    </Modal>
  );
}

export default ComprovanteModal;
