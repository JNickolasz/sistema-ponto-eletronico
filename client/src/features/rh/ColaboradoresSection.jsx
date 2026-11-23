import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, Search, Trash2, UserMinus, UserPlus, Users } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../../hooks/useAuth';
import rhService from '../../services/rhService';
import { formatarCpf, formatarPis, validarCpf, validarPis } from '../../utils/brazilianDocuments';
import { Badge, Button, Card, EmptyState, PageHeader } from '../../components/ui/Primitives';
import Modal from '../../components/ui/Modal';

export default function ColaboradoresSection({
  locais = [],
  funcionarios = [],
  loading,
  onAtualizado,
}) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [nomeCompleto, setNomeCompleto] = useState('');
  const [usuario, setUsuario] = useState('');
  const [senha, setSenha] = useState('');
  const [cpf, setCpf] = useState('');
  const [pisPasep, setPisPasep] = useState('');
  const [matricula, setMatricula] = useState('');
  const [dataAdmissao, setDataAdmissao] = useState(() => new Date().toISOString().split('T')[0]);
  const [perfilAcesso, setPerfilAcesso] = useState('COLABORADOR');
  const [localTrabalhoId, setLocalTrabalhoId] = useState('');
  const [gestorId, setGestorId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [busca, setBusca] = useState('');
  const [modalDesligar, setModalDesligar] = useState(null);
  const [dataDesligamento, setDataDesligamento] = useState(() => new Date().toISOString().split('T')[0]);
  const [processandoAcao, setProcessandoAcao] = useState(false);

  const gestoresDisponiveis = funcionarios.filter(
    (f) => f.perfilAcesso === 'GESTOR' || f.perfilAcesso === 'RH'
  );

  const handleCadastrar = async (e) => {
    e.preventDefault();
    if (!nomeCompleto.trim() || !usuario.trim() || !senha.trim() || !cpf.trim() || !pisPasep.trim() || !matricula.trim()) {
      toast.error('Preencha todos os campos obrigatórios.');
      return;
    }
    if (!validarCpf(cpf)) {
      toast.error('CPF inválido.');
      return;
    }
    if (!validarPis(pisPasep)) {
      toast.error('PIS/PASEP inválido.');
      return;
    }

    try {
      setSubmitting(true);
      await rhService.cadastrarFuncionario({
        nomeCompleto: nomeCompleto.trim(),
        usuario: usuario.trim(),
        senha: senha.trim(),
        cpf: cpf.replace(/\D/g, ''),
        pis_pasep: pisPasep.replace(/\D/g, ''),
        matricula: matricula.trim(),
        dataAdmissao: dataAdmissao || undefined,
        localTrabalhoId: localTrabalhoId || locais[0]?.id || undefined,
        gestorId: gestorId || undefined,
        perfilAcesso,
      });
      toast.success('Colaborador cadastrado com sucesso.');
      setNomeCompleto('');
      setUsuario('');
      setSenha('');
      setCpf('');
      setPisPasep('');
      setMatricula('');
      await onAtualizado?.();
    } catch (err) {
      toast.error(err?.message || 'Erro ao cadastrar colaborador.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmarDesligamento = async (e) => {
    e.preventDefault();
    if (!modalDesligar || !dataDesligamento) return;
    try {
      setProcessandoAcao(true);
      await rhService.desligarFuncionario(modalDesligar.id, dataDesligamento);

      const hojeIso = new Date().toISOString().split('T')[0];
      const ehAutoDesligamento =
        user?.usuario &&
        modalDesligar.usuario &&
        modalDesligar.usuario.toLowerCase() === user.usuario.toLowerCase();

      setModalDesligar(null);

      if (ehAutoDesligamento && dataDesligamento <= hojeIso) {
        toast('Seu desligamento foi registrado. Seu acesso foi revogado imediatamente.');
        logout();
        navigate('/login', { replace: true });
        return;
      }

      toast.success(`Desligamento de ${modalDesligar.nomeCompleto} registrado.`);
      await onAtualizado?.();
    } catch (err) {
      toast.error(err?.message || 'Erro ao desligar colaborador.');
    } finally {
      setProcessandoAcao(false);
    }
  };

  const handleExcluir = async (func) => {
    if (!window.confirm(`Excluir permanentemente ${func.nomeCompleto}? Só é permitido se não houver marcações.`)) {
      return;
    }
    try {
      await rhService.excluirFuncionario(func.id);
      toast.success('Colaborador excluído.');
      await onAtualizado?.();
    } catch (err) {
      toast.error(err?.message || 'Não foi possível excluir este colaborador.');
    }
  };

  const filtrados = funcionarios.filter((f) => {
    if (!busca.trim()) return true;
    const t = busca.trim().toLowerCase();
    return (
      (f.nomeCompleto || '').toLowerCase().includes(t) ||
      (f.usuario || '').toLowerCase().includes(t) ||
      (f.matricula || '').toLowerCase().includes(t) ||
      (f.cpf || '').includes(t)
    );
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Colaboradores & Acessos"
        description="Gerencie o quadro funcional, vínculos hierárquicos, desligamentos e credenciais de acesso."
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Formulário */}
        <Card className="lg:col-span-5 p-6">
          <h2 className="text-sm font-semibold mb-4">Novo Colaborador</h2>
          <form onSubmit={handleCadastrar} className="space-y-3.5">
            <div className="space-y-1">
              <label className="text-xs font-medium">Nome Completo *</label>
              <input
                type="text"
                value={nomeCompleto}
                onChange={(e) => setNomeCompleto(e.target.value)}
                placeholder="Nome completo do colaborador"
                className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-medium">Usuário *</label>
                <input
                  type="text"
                  value={usuario}
                  onChange={(e) => setUsuario(e.target.value)}
                  placeholder="nome.sobrenome"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">Senha inicial *</label>
                <input
                  type="password"
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  placeholder="••••••••"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-medium">CPF *</label>
                <input
                  type="text"
                  value={formatarCpf(cpf)}
                  onChange={(e) => setCpf(e.target.value)}
                  placeholder="000.000.000-00"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">PIS/PASEP *</label>
                <input
                  type="text"
                  value={formatarPis(pisPasep)}
                  onChange={(e) => setPisPasep(e.target.value)}
                  placeholder="000.00000.00-0"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-medium">Matrícula *</label>
                <input
                  type="text"
                  value={matricula}
                  onChange={(e) => setMatricula(e.target.value)}
                  placeholder="Ex: MAT-105"
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">Admissão</label>
                <input
                  type="date"
                  value={dataAdmissao}
                  onChange={(e) => setDataAdmissao(e.target.value)}
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-medium">Perfil</label>
                <select
                  value={perfilAcesso}
                  onChange={(e) => setPerfilAcesso(e.target.value)}
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-2.5 text-sm"
                >
                  <option value="COLABORADOR">Colaborador</option>
                  <option value="GESTOR">Gestor</option>
                  <option value="RH">RH</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">Local</label>
                <select
                  value={localTrabalhoId}
                  onChange={(e) => setLocalTrabalhoId(e.target.value)}
                  className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-2.5 text-sm"
                >
                  <option value="">Padrão da empresa</option>
                  {locais.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.nome}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-medium">Gestor Direto (opcional)</label>
              <select
                value={gestorId}
                onChange={(e) => setGestorId(e.target.value)}
                className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-2.5 text-sm"
              >
                <option value="">Sem gestor vinculado</option>
                {gestoresDisponiveis.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.nomeCompleto} ({g.perfilAcesso})
                  </option>
                ))}
              </select>
            </div>

            <Button type="submit" variant="primary" size="lg" className="w-full mt-1" disabled={submitting}>
              {submitting ? <Loader2 size={15} className="animate-spin" /> : <UserPlus size={15} />}
              Cadastrar Colaborador
            </Button>
          </form>
        </Card>

        {/* Quadro de Colaboradores */}
        <Card className="lg:col-span-7 p-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
            <h2 className="text-sm font-semibold">Quadro Funcional ({funcionarios.length})</h2>
            <div className="relative w-full sm:w-60">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
              <input
                type="text"
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                placeholder="Buscar nome, matrícula ou CPF..."
                className="w-full h-9 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-page)] pl-8 pr-3 text-xs"
              />
            </div>
          </div>

          {loading ? (
            <div className="py-12 flex justify-center">
              <Loader2 size={22} className="animate-spin text-[var(--text-muted)]" />
            </div>
          ) : filtrados.length === 0 ? (
            <EmptyState
              icon={Users}
              title="Nenhum colaborador encontrado"
              description="Cadastre colaboradores no formulário ao lado ou ajuste sua busca."
            />
          ) : (
            <div className="divide-y divide-[var(--border-subtle)] border border-[var(--border-subtle)] rounded-xl overflow-hidden max-h-[560px] overflow-y-auto">
              {filtrados.map((f) => {
                const desligado = !!f.dataDesligamento;
                return (
                  <div
                    key={f.id}
                    className="p-3.5 flex items-center justify-between gap-3 hover:bg-[var(--surface-hover)]/50 transition-colors"
                  >
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold truncate">{f.nomeCompleto}</span>
                        <Badge tone={f.perfilAcesso === 'RH' ? 'brand' : 'neutral'}>
                          {f.perfilAcesso}
                        </Badge>
                        {desligado && <Badge tone="danger">Desligado</Badge>}
                      </div>
                      <div className="text-xs text-[var(--text-secondary)] font-mono-tabular mt-0.5">
                        {f.usuario} · Mat. {f.matricula || '—'} · CPF {formatarCpf(f.cpf)}
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {!desligado && (
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => setModalDesligar(f)}
                          title="Registrar desligamento"
                        >
                          <UserMinus size={13} />
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleExcluir(f)}
                        title="Excluir (somente sem marcações)"
                      >
                        <Trash2 size={13} />
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      </div>

      <Modal
        open={!!modalDesligar}
        onClose={() => !processandoAcao && setModalDesligar(null)}
        title="Registrar Desligamento"
        subtitle={modalDesligar?.nomeCompleto}
      >
        <form onSubmit={handleConfirmarDesligamento} className="space-y-4">
          <p className="text-xs text-[var(--text-secondary)]">
            O histórico de marcações será preservado conforme a Portaria 671/MTP e o acesso do colaborador será revogado imediatamente.
          </p>
          <div className="space-y-1.5">
            <label className="block text-xs font-medium">Data de desligamento *</label>
            <input
              type="date"
              required
              autoFocus
              value={dataDesligamento}
              onChange={(e) => setDataDesligamento(e.target.value)}
              className="w-full h-10 rounded-lg border border-[var(--border-subtle)] bg-[var(--surface-card)] px-3 text-sm font-mono-tabular"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setModalDesligar(null)}>
              Cancelar
            </Button>
            <Button type="submit" variant="danger" disabled={processandoAcao}>
              {processandoAcao && <Loader2 size={14} className="animate-spin" />}
              Confirmar Desligamento
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
