import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import rhService from '../services/rhService';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  UserPlus,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Sparkles,
  KeyRound,
  Briefcase,
  Terminal,
  Building2,
  MapPin,
  Plus,
  X,
} from 'lucide-react';
import { FormControl, InputLabel, Select, MenuItem } from '@mui/material';
import toast from 'react-hot-toast';

export function RhDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Controle de Abas: 'locais' ou 'usuarios'
  const [abaAtiva, setAbaAtiva] = useState('locais');

  // Estado do painel do backend
  const [painelData, setPainelData] = useState(null);
  const [loadingPainel, setLoadingPainel] = useState(true);
  const [painelError, setPainelError] = useState(null);

  // Estado das Empresas e Locais de Trabalho
  const [empresas, setEmpresas] = useState([]);
  const [loadingEmpresas, setLoadingEmpresas] = useState(false);
  const [locaisTrabalho, setLocaisTrabalho] = useState([]);
  const [loadingLocais, setLoadingLocais] = useState(false);

  // Formulário de Cadastro de Local de Trabalho
  const [empresaIdSelecionada, setEmpresaIdSelecionada] = useState('');
  const [nomeLocal, setNomeLocal] = useState('');
  const [enderecoLocal, setEnderecoLocal] = useState('');
  const [municipioLocal, setMunicipioLocal] = useState('');
  const [ufLocal, setUfLocal] = useState('');
  const [submittingLocal, setSubmittingLocal] = useState(false);

  // Modal de Cadastro de Empresa (Área Admin)
  const [modalEmpresaAberto, setModalEmpresaAberto] = useState(false);
  const [adminKey, setAdminKey] = useState('admin123');
  const [razaoSocialNova, setRazaoSocialNova] = useState('');
  const [cnpjNovo, setCnpjNovo] = useState('');
  const [subdominioNovo, setSubdominioNovo] = useState('');
  const [enderecoNovo, setEnderecoNovo] = useState('');
  const [submittingEmpresa, setSubmittingEmpresa] = useState(false);

  // Formulário de cadastro de funcionário
  const [nomeCompleto, setNomeCompleto] = useState('');
  const [usuario, setUsuario] = useState('');
  const [senha, setSenha] = useState('');
  const [perfilAcesso, setPerfilAcesso] = useState('COLABORADOR');
  const [submitting, setSubmitting] = useState(false);

  // Histórico de cadastrados na sessão
  const [usuariosCadastrados, setUsuariosCadastrados] = useState(() => {
    try {
      const saved = localStorage.getItem('pontoCerto_usuarios_criados');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const recarregarPainel = async () => {
    try {
      setLoadingPainel(true);
      setPainelError(null);
      const data = await rhService.getPainel();
      setPainelData(data);
    } catch (err) {
      setPainelError(err.message || 'Erro ao carregar dados do painel do RH');
    } finally {
      setLoadingPainel(false);
    }
  };

  const carregarEmpresasELocais = async () => {
    try {
      setLoadingEmpresas(true);
      setLoadingLocais(true);
      const [listaEmpresas, listaLocais] = await Promise.all([
        rhService.listarEmpresas().catch(() => []),
        rhService.listarLocaisTrabalho().catch(() => []),
      ]);

      const arrayEmpresas = Array.isArray(listaEmpresas) ? listaEmpresas : [];
      const arrayLocais = Array.isArray(listaLocais) ? listaLocais : [];

      setEmpresas(arrayEmpresas);
      if (arrayEmpresas.length > 0 && !empresaIdSelecionada) {
        setEmpresaIdSelecionada(arrayEmpresas[0].id);
      }
      setLocaisTrabalho(arrayLocais);
    } catch (err) {
      console.error('Erro ao carregar empresas e locais:', err);
    } finally {
      setLoadingEmpresas(false);
      setLoadingLocais(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    rhService
      .getPainel()
      .then((data) => {
        if (isMounted) setPainelData(data);
      })
      .catch((err) => {
        if (isMounted) setPainelError(err.message || 'Erro ao carregar dados do painel do RH');
      })
      .finally(() => {
        if (isMounted) setLoadingPainel(false);
      });

    carregarEmpresasELocais();

    return () => {
      isMounted = false;
    };
  }, []);

  // Cadastro de Empresa via API Admin (POST /api/admin/empresas com X-Admin-Key)
  const handleCadastrarEmpresaAdmin = async (e) => {
    e.preventDefault();

    if (!razaoSocialNova.trim() || !cnpjNovo.trim() || !subdominioNovo.trim() || !enderecoNovo.trim()) {
      toast.error('Preencha todos os campos obrigatórios da empresa');
      return;
    }

    try {
      setSubmittingEmpresa(true);
      const payload = {
        razaoSocial: razaoSocialNova.trim(),
        cnpj: cnpjNovo.trim(),
        subdominio: subdominioNovo.trim().toLowerCase(),
        endereco: enderecoNovo.trim(),
      };

      const res = await rhService.cadastrarEmpresaAdmin(payload, adminKey.trim() || 'admin123');
      const novaEmpresa = res?.data || res;
      toast.success(`Empresa cadastrada via Admin com sucesso!`);

      // Recarrega empresas no select
      const listaAtualizada = await rhService.listarEmpresas();
      const arrayAtualizado = Array.isArray(listaAtualizada) ? listaAtualizada : [];
      setEmpresas(arrayAtualizado);

      if (novaEmpresa?.id) {
        setEmpresaIdSelecionada(novaEmpresa.id);
      } else if (arrayAtualizado.length > 0) {
        setEmpresaIdSelecionada(arrayAtualizado[arrayAtualizado.length - 1].id);
      }

      // Limpar formulário e fechar modal
      setRazaoSocialNova('');
      setCnpjNovo('');
      setSubdominioNovo('');
      setEnderecoNovo('');
      setModalEmpresaAberto(false);
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar empresa. Verifique a chave de Admin e o CNPJ.');
    } finally {
      setSubmittingEmpresa(false);
    }
  };

  const handlePreencherExemploEmpresa = () => {
    const timestamp = Math.floor(Math.random() * 899 + 100);
    setRazaoSocialNova(`Empresa Exemplo ${timestamp} S/A`);
    setCnpjNovo('33.000.167/0001-01'); // CNPJ válido (Petrobras)
    setSubdominioNovo(`empresa${timestamp}`);
    setEnderecoNovo('Av. das Nações Unidas, 12901 - Brooklin Paulista, SP');
    toast('Exemplo de empresa preenchido!', { icon: '🏢' });
  };

  // Cadastro de Local de Trabalho (Issue 2)
  const handleCadastrarLocal = async (e) => {
    e.preventDefault();

    if (!empresaIdSelecionada) {
      toast.error('Selecione uma empresa vinculada');
      return;
    }

    if (!nomeLocal.trim() || !enderecoLocal.trim()) {
      toast.error('Preencha o nome do local e o endereço');
      return;
    }

    try {
      setSubmittingLocal(true);
      const payload = {
        empresaId: empresaIdSelecionada,
        nome: nomeLocal.trim(),
        endereco: enderecoLocal.trim(),
        municipio: municipioLocal.trim() || 'São Paulo',
        uf: ufLocal.trim().toUpperCase() || 'SP',
      };

      const novoLocal = await rhService.cadastrarLocalTrabalho(payload);
      toast.success(`Local de trabalho "${novoLocal.nome}" cadastrado com sucesso!`);

      // Atualiza listagem
      const locaisAtualizados = await rhService.listarLocaisTrabalho();
      setLocaisTrabalho(Array.isArray(locaisAtualizados) ? locaisAtualizados : [novoLocal, ...locaisTrabalho]);

      // Limpar formulário
      setNomeLocal('');
      setEnderecoLocal('');
      setMunicipioLocal('');
      setUfLocal('');
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar local de trabalho');
    } finally {
      setSubmittingLocal(false);
    }
  };

  const handlePreencherExemploLocal = (tipo) => {
    if (tipo === 'MATRIZ') {
      setNomeLocal('Matriz Central');
      setEnderecoLocal('Av. Paulista, 1000, Bela Vista');
      setMunicipioLocal('São Paulo');
      setUfLocal('SP');
    } else if (tipo === 'FILIAL') {
      setNomeLocal('Filial Sul');
      setEnderecoLocal('Rua das Flores, 450, Centro');
      setMunicipioLocal('Curitiba');
      setUfLocal('PR');
    } else if (tipo === 'OBRA') {
      setNomeLocal('Obra Complexo Logístico');
      setEnderecoLocal('Rodovia BR-101, Km 42');
      setMunicipioLocal('Joinville');
      setUfLocal('SC');
    }
    toast(`Exemplo de ${tipo} preenchido no formulário!`, { icon: '🏢' });
  };

  // Cadastro de Funcionário (T01)
  const handleCadastrar = async (e) => {
    e.preventDefault();

    if (!nomeCompleto.trim() || !usuario.trim() || !senha.trim()) {
      toast.error('Preencha todos os campos obrigatórios');
      return;
    }

    try {
      setSubmitting(true);
      const payload = {
        nomeCompleto: nomeCompleto.trim(),
        usuario: usuario.trim(),
        senha: senha.trim(),
        perfilAcesso,
      };

      const novo = await rhService.cadastrarFuncionario(payload);
      toast.success(`Funcionário criado com sucesso! ID: ${novo.id}`);

      const novaLista = [novo, ...usuariosCadastrados];
      setUsuariosCadastrados(novaLista);
      localStorage.setItem('pontoCerto_usuarios_criados', JSON.stringify(novaLista));

      // Limpar formulário
      setNomeCompleto('');
      setUsuario('');
      setSenha('');
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar funcionário');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePreencherExemplo = (tipo) => {
    const timestamp = Math.floor(Math.random() * 899 + 100);
    if (tipo === 'COLABORADOR') {
      setNomeCompleto('João da Silva');
      setUsuario(`colaborador.${timestamp}`);
      setSenha('senha123');
      setPerfilAcesso('COLABORADOR');
    } else if (tipo === 'GESTOR') {
      setNomeCompleto('Mariana Costa');
      setUsuario(`gestor.${timestamp}`);
      setSenha('gestor123');
      setPerfilAcesso('GESTOR');
    }
    toast('Exemplo preenchido no formulário!', { icon: '📝' });
  };

  const handleTestarLoginNovo = (usr) => {
    logout();
    navigate('/login');
    toast(`Faça login com o usuário: ${usr.usuario}`, { icon: '🔑', duration: 6000 });
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Header do Painel */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400">
                <ShieldCheck size={24} />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
                  Painel de Recursos Humanos (RH)
                </h1>
                <p className="text-sm text-slate-400 mt-0.5">
                  Gerenciamento corporativo, locais de trabalho e credenciais de acesso.
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={recarregarPainel}
              disabled={loadingPainel}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-900 border border-slate-700/80 px-4 py-2 text-xs font-semibold text-slate-200 hover:bg-slate-800 transition-all cursor-pointer disabled:opacity-50"
            >
              <RefreshCw size={14} className={loadingPainel ? 'animate-spin' : ''} />
              Recarregar Painel
            </button>
          </div>
        </div>

        {/* Backend Response Card (GET /api/rh/painel) */}
        <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-6 backdrop-blur-xl">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Terminal size={18} className="text-purple-400" />
              <h2 className="text-base font-semibold text-slate-100">
                Resposta do Endpoint: <span className="font-mono text-purple-300">GET /api/rh/painel</span>
              </h2>
            </div>
            {painelData && (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-medium text-emerald-400 border border-emerald-500/20">
                <CheckCircle2 size={12} /> 200 OK - Autorizado
              </span>
            )}
            {painelError && (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-400 border border-red-500/20">
                <AlertCircle size={12} /> Erro de Comunicação
              </span>
            )}
          </div>

          {loadingPainel ? (
            <div className="flex items-center gap-3 py-4 text-slate-400 text-sm">
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-purple-500 border-t-transparent" />
              <span>Consultando endpoint do backend...</span>
            </div>
          ) : painelError ? (
            <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-300 text-sm">
              {painelError}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Mensagem da API</span>
                <p className="text-sm font-semibold text-purple-300 mt-1">
                  {painelData?.mensagem || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Usuário Autenticado</span>
                <p className="text-sm font-semibold text-slate-200 mt-1 font-mono">
                  {painelData?.usuarioLogado || user?.usuario || '—'}
                </p>
              </div>
              <div className="bg-slate-950/60 rounded-xl p-3.5 border border-slate-800">
                <span className="text-xs text-slate-400">Permissões (Authorities)</span>
                <div className="flex flex-wrap gap-1.5 mt-1">
                  {painelData?.permissoes?.map((p, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 rounded bg-purple-900/40 text-purple-200 text-xs font-mono border border-purple-700/30"
                    >
                      {p.authority}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Abas de Navegação */}
        <div className="flex border-b border-slate-800 space-x-6">
          <button
            type="button"
            onClick={() => setAbaAtiva('locais')}
            className={`pb-3.5 px-1 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
              abaAtiva === 'locais'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Building2 size={18} />
            <span>Locais de Trabalho</span>
            {locaisTrabalho.length > 0 && (
              <span className="px-2 py-0.5 text-xs rounded-full bg-indigo-500/20 text-indigo-300 font-mono">
                {locaisTrabalho.length}
              </span>
            )}
          </button>
          <button
            type="button"
            onClick={() => setAbaAtiva('usuarios')}
            className={`pb-3.5 px-1 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
              abaAtiva === 'usuarios'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <UserPlus size={18} />
            <span>Usuários e Acessos</span>
            {usuariosCadastrados.length > 0 && (
              <span className="px-2 py-0.5 text-xs rounded-full bg-slate-800 text-slate-300 font-mono">
                {usuariosCadastrados.length}
              </span>
            )}
          </button>
        </div>

        {/* Conteúdo da Aba 1: Locais de Trabalho (Issue 2) */}
        {abaAtiva === 'locais' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            {/* Coluna do Formulário de Local de Trabalho */}
            <div className="lg:col-span-7">
              <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                      <Building2 size={20} />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white">Cadastrar Local de Trabalho</h3>
                      <p className="text-xs text-slate-400">
                        Dispara requisição para <span className="font-mono text-indigo-300">POST /api/v1/locais-trabalho</span>
                      </p>
                    </div>
                  </div>

                  {/* Exemplos Rápidos */}
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <button
                      type="button"
                      onClick={() => handlePreencherExemploLocal('MATRIZ')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                    >
                      + Matriz
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePreencherExemploLocal('FILIAL')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                    >
                      + Filial
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePreencherExemploLocal('OBRA')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                    >
                      + Obra
                    </button>
                  </div>
                </div>

                <form onSubmit={handleCadastrarLocal} className="space-y-4">
                  {/* Select da Empresa com botão de Admin */}
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                        Empresa Vinculada (MUI Select) *
                      </label>
                      <button
                        type="button"
                        onClick={() => setModalEmpresaAberto(true)}
                        className="inline-flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 font-semibold transition-colors cursor-pointer"
                        title="Cadastrar nova empresa cliente via API Admin"
                      >
                        <Plus size={14} /> + Nova Empresa (Admin)
                      </button>
                    </div>

                    <FormControl fullWidth size="small">
                      <Select
                        displayEmpty
                        value={empresaIdSelecionada}
                        onChange={(e) => setEmpresaIdSelecionada(e.target.value)}
                        renderValue={(selected) => {
                          if (!selected) {
                            return <span className="text-slate-500 text-sm">Selecione a empresa...</span>;
                          }
                          const found = empresas.find((e) => e.id === selected);
                          return found ? found.razaoSocial : selected;
                        }}
                        sx={{
                          color: '#f8fafc',
                          backgroundColor: 'rgba(2, 6, 23, 0.6)',
                          borderRadius: '0.75rem',
                          '.MuiOutlinedInput-notchedOutline': {
                            borderColor: 'rgba(51, 65, 85, 0.8)',
                          },
                          '&:hover .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#6366f1',
                          },
                          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#6366f1',
                          },
                          '.MuiSvgIcon-root': {
                            color: '#94a3b8',
                          },
                        }}
                        MenuProps={{
                          PaperProps: {
                            sx: {
                              bgcolor: '#0f172a',
                              color: '#f8fafc',
                              border: '1px solid #334155',
                              borderRadius: '0.75rem',
                            },
                          },
                        }}
                      >
                        {empresas.length === 0 ? (
                          <MenuItem disabled value="">
                            Nenhuma empresa encontrada (clique em "+ Nova Empresa" acima)
                          </MenuItem>
                        ) : (
                          empresas.map((emp) => (
                            <MenuItem key={emp.id} value={emp.id} sx={{ '&:hover': { bgcolor: '#1e293b' } }}>
                              {emp.razaoSocial}
                            </MenuItem>
                          ))
                        )}
                      </Select>
                    </FormControl>
                  </div>

                  {/* Nome do Local */}
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Nome do Local de Trabalho *
                    </label>
                    <input
                      type="text"
                      required
                      value={nomeLocal}
                      onChange={(e) => setNomeLocal(e.target.value)}
                      placeholder="Ex: Matriz Centro, Filial Zona Sul, Obra Residencial Parque"
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                    />
                  </div>

                  {/* Endereço */}
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Endereço Completo *
                    </label>
                    <input
                      type="text"
                      required
                      value={enderecoLocal}
                      onChange={(e) => setEnderecoLocal(e.target.value)}
                      placeholder="Ex: Av. Paulista, 1000 - Bela Vista"
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                    />
                  </div>

                  {/* Município e UF */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="sm:col-span-2">
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Município
                      </label>
                      <input
                        type="text"
                        value={municipioLocal}
                        onChange={(e) => setMunicipioLocal(e.target.value)}
                        placeholder="Ex: São Paulo"
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        UF (Estado)
                      </label>
                      <input
                        type="text"
                        maxLength={2}
                        value={ufLocal}
                        onChange={(e) => setUfLocal(e.target.value)}
                        placeholder="SP"
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none uppercase"
                      />
                    </div>
                  </div>

                  <div className="pt-3">
                    <button
                      type="submit"
                      disabled={submittingLocal}
                      className="w-full flex items-center justify-center gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-600/25 focus:outline-none transition-all cursor-pointer disabled:opacity-50"
                    >
                      {submittingLocal ? (
                        <>
                          <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                          <span>Cadastrando Local...</span>
                        </>
                      ) : (
                        <>
                          <Plus size={16} />
                          <span>Cadastrar Local de Trabalho</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>

            {/* Coluna da Listagem de Locais Cadastrados */}
            <div className="lg:col-span-5 space-y-4">
              <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <Building2 size={18} className="text-indigo-400" />
                    <h3 className="text-base font-bold text-white">Locais de Trabalho Cadastrados</h3>
                  </div>
                  <span className="text-xs text-slate-400 bg-slate-800 px-2.5 py-1 rounded-full font-mono">
                    Total: {locaisTrabalho.length}
                  </span>
                </div>

                {loadingLocais ? (
                  <div className="flex items-center justify-center gap-2 py-8 text-slate-400 text-sm">
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
                    <span>Carregando locais de trabalho...</span>
                  </div>
                ) : locaisTrabalho.length === 0 ? (
                  <div className="text-center py-8 px-4 rounded-xl border border-dashed border-slate-800 text-slate-400 text-sm">
                    <MapPin size={32} className="mx-auto mb-2 text-slate-600" />
                    <p>Nenhum local de trabalho cadastrado ainda.</p>
                    <p className="text-xs text-slate-500 mt-1">
                      Use o formulário ao lado para cadastrar a Matriz, Filiais ou Obras da empresa!
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                    {locaisTrabalho.map((local) => (
                      <div
                        key={local.id}
                        className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 hover:border-slate-700 transition-colors"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-semibold text-sm text-slate-200 flex items-center gap-1.5">
                            <Building2 size={14} className="text-indigo-400" />
                            {local.nome}
                          </span>
                          <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                            {local.razaoSocialEmpresa || 'Empresa Vinculada'}
                          </span>
                        </div>
                        <div className="text-xs text-slate-400 flex items-center gap-1 mt-1.5">
                          <MapPin size={12} className="text-slate-500 shrink-0" />
                          <span className="truncate">
                            {local.endereco}
                            {local.municipio ? ` - ${local.municipio}/${local.uf}` : ''}
                          </span>
                        </div>
                        <div className="text-[10px] text-slate-500 font-mono mt-1">
                          ID: #{local.id}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Card Explicativo da Tarefa T02 */}
              <div className="rounded-2xl bg-indigo-950/30 border border-indigo-900/40 p-5 text-xs text-indigo-300/90 space-y-2">
                <div className="font-semibold text-indigo-200 flex items-center gap-1.5 text-sm">
                  <Briefcase size={16} /> Tarefa T02 - Papel dos Locais:
                </div>
                <p className="text-slate-300 leading-relaxed">
                  Toda marcação de ponto deve ter uma origem física conhecida. Cada local cadastrado (Matriz, Filial, Obra) receberá seus <strong>Equipamentos</strong> (Relógio AFD ou Estação Web) para registrar as batidas.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Conteúdo da Aba 2: Usuários e Acessos (T01) */}
        {abaAtiva === 'usuarios' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            {/* Coluna do Formulário de Usuários */}
            <div className="lg:col-span-7">
              <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                      <UserPlus size={20} />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white">Cadastrar Novo Usuário</h3>
                      <p className="text-xs text-slate-400">
                        Dispara requisição real para <span className="font-mono text-indigo-300">POST /api/rh/adicionar</span>
                      </p>
                    </div>
                  </div>

                  {/* Botões de Preenchimento Rápido */}
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => handlePreencherExemplo('COLABORADOR')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                    >
                      + Colaborador
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePreencherExemplo('GESTOR')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                    >
                      + Gestor
                    </button>
                  </div>
                </div>

                <form onSubmit={handleCadastrar} className="space-y-4 mt-6">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Nome Completo *
                    </label>
                    <input
                      type="text"
                      required
                      value={nomeCompleto}
                      onChange={(e) => setNomeCompleto(e.target.value)}
                      placeholder="Ex: Carlos Eduardo de Oliveira"
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                    />
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Usuário (Username) *
                      </label>
                      <input
                        type="text"
                        required
                        value={usuario}
                        onChange={(e) => setUsuario(e.target.value)}
                        placeholder="Ex: carlos.oliveira"
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Senha Inicial *
                      </label>
                      <input
                        type="password"
                        required
                        value={senha}
                        onChange={(e) => setSenha(e.target.value)}
                        placeholder="••••••••"
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Perfil de Acesso (Role) *
                    </label>
                    <select
                      value={perfilAcesso}
                      onChange={(e) => setPerfilAcesso(e.target.value)}
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none cursor-pointer"
                    >
                      <option value="COLABORADOR">COLABORADOR - Registro de ponto e consulta de espelho</option>
                      <option value="GESTOR">GESTOR - Supervisão de equipes e espelhos</option>
                      <option value="RH">RH - Gestão global e cadastro de funcionários</option>
                    </select>
                  </div>

                  <div className="pt-3">
                    <button
                      type="submit"
                      disabled={submitting}
                      className="w-full flex items-center justify-center gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-600/25 focus:outline-none transition-all cursor-pointer disabled:opacity-50"
                    >
                      {submitting ? (
                        <>
                          <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                          <span>Enviando para o Backend...</span>
                        </>
                      ) : (
                        <>
                          <UserPlus size={16} />
                          <span>Criar Funcionário no Backend</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>

            {/* Coluna de Usuários Cadastrados para Teste Rápido */}
            <div className="lg:col-span-5 space-y-4">
              <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <Sparkles size={18} className="text-amber-400" />
                    <h3 className="text-base font-bold text-white">Usuários Criados para Teste</h3>
                  </div>
                  <span className="text-xs text-slate-400 bg-slate-800 px-2.5 py-1 rounded-full">
                    Total: {usuariosCadastrados.length}
                  </span>
                </div>

                {usuariosCadastrados.length === 0 ? (
                  <div className="text-center py-8 px-4 rounded-xl border border-dashed border-slate-800 text-slate-400 text-sm">
                    <UserPlus size={32} className="mx-auto mb-2 text-slate-600" />
                    <p>Nenhum novo funcionário criado nesta sessão ainda.</p>
                    <p className="text-xs text-slate-500 mt-1">
                      Cadastre um COLABORADOR ou GESTOR ao lado para testar as outras telas!
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                    {usuariosCadastrados.map((item, idx) => (
                      <div
                        key={idx}
                        className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 hover:border-slate-700 transition-colors flex items-center justify-between"
                      >
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-sm text-slate-200">
                              {item.nomeCompleto}
                            </span>
                            <span
                              className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                                item.perfilAcesso === 'COLABORADOR'
                                  ? 'bg-emerald-500/20 text-emerald-400'
                                  : item.perfilAcesso === 'GESTOR'
                                  ? 'bg-blue-500/20 text-blue-400'
                                  : 'bg-purple-500/20 text-purple-400'
                              }`}
                            >
                              {item.perfilAcesso}
                            </span>
                          </div>
                          <div className="text-xs text-slate-400 font-mono mt-0.5">
                            Login: <span className="text-indigo-300 font-semibold">{item.usuario}</span> | ID #{item.id}
                          </div>
                        </div>

                        <button
                          type="button"
                          onClick={() => handleTestarLoginNovo(item)}
                          className="inline-flex items-center gap-1 rounded-lg bg-indigo-600/20 border border-indigo-500/30 px-2.5 py-1.5 text-xs font-medium text-indigo-300 hover:bg-indigo-600 hover:text-white transition-all cursor-pointer"
                          title="Desconectar e logar como este usuário"
                        >
                          <KeyRound size={12} />
                          Testar
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Guia de Acessos rápidos */}
              <div className="rounded-2xl bg-indigo-950/30 border border-indigo-900/40 p-5 text-xs text-indigo-300/90 space-y-2">
                <div className="font-semibold text-indigo-200 flex items-center gap-1.5 text-sm">
                  <Briefcase size={16} /> Fluxo de Teste Recomendado:
                </div>
                <ol className="list-decimal list-inside space-y-1 text-slate-300 pl-1">
                  <li>Cadastre um <strong>COLABORADOR</strong> com o formulário ao lado.</li>
                  <li>Cadastre um <strong>GESTOR</strong> com o formulário ao lado.</li>
                  <li>Clique em <strong>Testar</strong> no card do usuário para ir à tela de login.</li>
                  <li>Faça login e veja a tela do painel exclusiva do perfil correspondente!</li>
                </ol>
              </div>
            </div>
          </div>
        )}

        {/* Modal de Cadastro de Nova Empresa (Admin) */}
        {modalEmpresaAberto && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="w-full max-w-lg rounded-2xl bg-slate-900 border border-slate-800 p-6 shadow-2xl space-y-5">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-xl bg-purple-500/10 text-purple-400">
                    <ShieldCheck size={20} />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white">Cadastrar Empresa (Admin)</h3>
                    <p className="text-xs text-slate-400">
                      Dispara <span className="font-mono text-purple-300">POST /api/admin/empresas</span> com <span className="font-mono text-purple-300">X-Admin-Key</span>
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setModalEmpresaAberto(false)}
                  className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition-colors cursor-pointer"
                >
                  <X size={20} />
                </button>
              </div>

              <div className="flex justify-end">
                <button
                  type="button"
                  onClick={handlePreencherExemploEmpresa}
                  className="text-xs px-3 py-1 rounded-lg bg-indigo-600/20 text-indigo-300 hover:bg-indigo-600 hover:text-white border border-indigo-500/30 transition-all cursor-pointer"
                >
                  Preencher com Dados Válidos (Exemplo)
                </button>
              </div>

              <form onSubmit={handleCadastrarEmpresaAdmin} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                    Chave Administrativa (Header X-Admin-Key) *
                  </label>
                  <input
                    type="text"
                    required
                    value={adminKey}
                    onChange={(e) => setAdminKey(e.target.value)}
                    placeholder="admin123"
                    className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                    Razão Social *
                  </label>
                  <input
                    type="text"
                    required
                    value={razaoSocialNova}
                    onChange={(e) => setRazaoSocialNova(e.target.value)}
                    placeholder="Ex: Construtora Horizonte S/A"
                    className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      CNPJ (Válido) *
                    </label>
                    <input
                      type="text"
                      required
                      value={cnpjNovo}
                      onChange={(e) => setCnpjNovo(e.target.value)}
                      placeholder="Ex: 33.000.167/0001-01"
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none font-mono"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Subdomínio *
                    </label>
                    <input
                      type="text"
                      required
                      value={subdominioNovo}
                      onChange={(e) => setSubdominioNovo(e.target.value)}
                      placeholder="Ex: horizonte"
                      className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none lowercase"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                    Endereço Completo *
                  </label>
                  <input
                    type="text"
                    required
                    value={enderecoNovo}
                    onChange={(e) => setEnderecoNovo(e.target.value)}
                    placeholder="Ex: Av. Brasil, 1500 - Centro, Rio de Janeiro/RJ"
                    className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setModalEmpresaAberto(false)}
                    className="px-4 py-2.5 rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-300 transition-colors cursor-pointer"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={submittingEmpresa}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-xs font-semibold text-white shadow-lg shadow-purple-600/25 transition-all cursor-pointer disabled:opacity-50"
                  >
                    {submittingEmpresa ? (
                      <>
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                        <span>Cadastrando...</span>
                      </>
                    ) : (
                      <>
                        <Plus size={14} />
                        <span>Cadastrar Empresa (Admin)</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default RhDashboard;
