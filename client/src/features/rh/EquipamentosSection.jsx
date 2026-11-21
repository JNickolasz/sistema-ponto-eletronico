import React, { useState } from 'react';
import { HardDrive, Loader2, Plus, Power } from 'lucide-react';
import toast from 'react-hot-toast';
import equipamentoService from '../../services/equipamentoService';
import { Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui/Primitives';

export default function EquipamentosSection({
  locais = [],
  equipamentos = [],
  loading,
  onAtualizado,
}) {
  const [localTrabalhoId, setLocalTrabalhoId] = useState(() => locais[0]?.id || '');
  const [tipo, setTipo] = useState('RELOGIO');
  const [identificacao, setIdentificacao] = useState('');
  const [codigo, setCodigo] = useState('');
  const [numFabricacao, setNumFabricacao] = useState('');
  const [filtroLocal, setFiltroLocal] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const localEscolhido = localTrabalhoId || locais[0]?.id;
    if (!localEscolhido) {
      toast.error('Cadastre e selecione um local de trabalho primeiro.');
      return;
    }
    if (!identificacao.trim()) {
      toast.error('Informe a identificação do equipamento.');
      return;
    }
    if (tipo === 'RELOGIO' && !numFabricacao.trim()) {
      toast.error('Informe o número de fabricação do relógio REP.');
      return;
    }

    try {
      setSubmitting(true);
      const codFinal =
        codigo.trim() ||
        (tipo === 'RELOGIO' ? numFabricacao.trim() : `EQ-${Math.floor(100000 + Math.random() * 900000)}`);

      await equipamentoService.cadastrarEquipamento({
        codigo: codFinal,
        localTrabalhoId: localEscolhido,
        tipo,
        identificacao: identificacao.trim(),
        numFabricacao: tipo === 'RELOGIO' ? numFabricacao.trim() : null,
      });
      toast.success('Equipamento cadastrado com sucesso.');
      setIdentificacao('');
      setCodigo('');
      setNumFabricacao('');
      await onAtualizado?.();
    } catch (err) {
      toast.error(err?.message || 'Erro ao cadastrar equipamento.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAlterarStatus = async (eq) => {
    const novoStatus = eq.status === 'ATIVO' ? 'INATIVO' : 'ATIVO';
    try {
      await equipamentoService.alterarStatus(eq.id, novoStatus);
      toast.success(`Equipamento marcado como ${novoStatus}.`);
      await onAtualizado?.();
    } catch (err) {
      toast.error(err?.message || 'Erro ao alterar status.');
    }
  };

  const filtrados = filtroLocal
    ? equipamentos.filter((e) => e.localTrabalhoId === filtroLocal || e.localDeTrabalho?.id === filtroLocal)
    : equipamentos;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Equipamentos de Registro"
        description="Gerencie relógios físicos (REP) e estações vinculadas às unidades da empresa."
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <Card className="lg:col-span-5 p-6">
          <h2 className="text-sm font-semibold mb-4">Cadastrar Equipamento</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-medium">Local de Trabalho *</label>
              <select
                value={localTrabalhoId || locais[0]?.id || ''}
                onChange={(e) => setLocalTrabalhoId(e.target.value)}
                className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
              >
                {locais.length === 0 && <option value="">Nenhum local cadastrado</option>}
                {locais.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.nome}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="block text-xs font-medium">Tipo *</label>
                <select
                  value={tipo}
                  onChange={(e) => setTipo(e.target.value)}
                  className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
                >
                  <option value="RELOGIO">Relógio Físico (REP)</option>
                  <option value="ESTACAO">Estação Web</option>
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="block text-xs font-medium">Código interno</label>
                <input
                  type="text"
                  value={codigo}
                  onChange={(e) => setCodigo(e.target.value)}
                  placeholder="Ex: REP-01"
                  className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-medium">Identificação *</label>
              <input
                type="text"
                value={identificacao}
                onChange={(e) => setIdentificacao(e.target.value)}
                placeholder="Ex: Relógio Portaria Principal"
                className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
              />
            </div>

            {tipo === 'RELOGIO' && (
              <div className="space-y-1.5">
                <label className="block text-xs font-medium">Número de Fabricação (REP) *</label>
                <input
                  type="text"
                  value={numFabricacao}
                  onChange={(e) => setNumFabricacao(e.target.value)}
                  placeholder="Ex: 00001234567890123"
                  className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                />
              </div>
            )}

            <Button type="submit" variant="primary" size="lg" className="w-full" disabled={submitting}>
              {submitting ? <Loader2 size={15} className="animate-spin" /> : <Plus size={15} />}
              Cadastrar Equipamento
            </Button>
          </form>
        </Card>

        <Card className="lg:col-span-7 p-6">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <h2 className="text-sm font-semibold">Equipamentos Cadastrados</h2>
            <select
              value={filtroLocal}
              onChange={(e) => setFiltroLocal(e.target.value)}
              className="h-8 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] px-2.5 text-xs"
            >
              <option value="">Todos os locais ({equipamentos.length})</option>
              {locais.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.nome}
                </option>
              ))}
            </select>
          </div>

          {loading ? (
            <div className="py-12 flex justify-center">
              <Loader2 size={22} className="animate-spin text-[var(--text-muted)]" />
            </div>
          ) : filtrados.length === 0 ? (
            <EmptyState
              icon={HardDrive}
              title="Nenhum equipamento cadastrado"
              description="Cadastre um relógio REP ou estação para começar a coletar batidas."
            />
          ) : (
            <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-xl overflow-hidden">
              {filtrados.map((eq) => (
                <div
                  key={eq.id}
                  className="p-4 flex items-center justify-between gap-4 hover:bg-[var(--surface-hover)]/50 transition-colors"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold truncate">{eq.identificacao}</span>
                      <Badge tone="neutral">{eq.tipo}</Badge>
                    </div>
                    <div className="text-xs font-mono-tabular text-[var(--text-muted)] mt-0.5">
                      Código: {eq.codigo}
                      {eq.nomeLocalTrabalho ? ` · ${eq.nomeLocalTrabalho}` : ''}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <Badge tone={eq.status === 'ATIVO' ? 'success' : 'danger'}>{eq.status}</Badge>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleAlterarStatus(eq)}
                      title={eq.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
                    >
                      <Power size={13} />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
