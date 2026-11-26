import React, { useState, useEffect } from 'react';
import AppShell from '../layouts/AppShell';
import ImportacaoPosicionalSection from '../components/ImportacaoPosicionalSection';
import BatidasOrfasSection from '../components/BatidasOrfasSection';
import FilaInconsistenciasSection from '../components/FilaInconsistenciasSection';
import ApuracaoEspelhoSection from '../components/ApuracaoEspelhoSection';
import OcorrenciasSection from '../components/OcorrenciasSection';
import BancoHorasSection from '../components/BancoHorasSection';
import FechamentoPeriodoSection from '../features/rh/FechamentoPeriodoSection';
import RelatoriosSection from '../features/rh/RelatoriosSection';
import ExportacaoFolhaSection from '../features/rh/ExportacaoFolhaSection';
import LocaisSection from '../features/rh/LocaisSection';
import EquipamentosSection from '../features/rh/EquipamentosSection';
import ColaboradoresSection from '../features/rh/ColaboradoresSection';
import rhService from '../services/rhService';
import batidaService from '../services/batidaService';
import equipamentoService from '../services/equipamentoService';
import feriadoService from '../services/feriadoService';
import {
  validarPis,
  gerarPisValido,
  validarCpf,
  gerarCpfValido,
  formatarCpf,
  formatarPis,
} from '../utils/brazilianDocuments';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  UserPlus,
  UserMinus,
  Trash2,
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
  HardDrive,
  Clock,
  Monitor,
  Power,
  Hash,
  Filter,
  Calendar,
  CalendarDays,
  CalendarCheck,
  Flag,
  Check,
  Layers,
  Search,
  Upload,
  History,
  Lock,
  BarChart3,
  FileSpreadsheet,
  UserCheck,
} from 'lucide-react';
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

  // Estado dos Equipamentos de Registro (T02)
  const [equipamentos, setEquipamentos] = useState([]);
  const [loadingEquipamentos, setLoadingEquipamentos] = useState(false);
  const [localTrabalhoIdEquipamento, setLocalTrabalhoIdEquipamento] = useState('');
  const [tipoEquipamento, setTipoEquipamento] = useState('RELOGIO');
  const [identificacaoEquipamento, setIdentificacaoEquipamento] = useState('');
  const [codigoEquipamento, setCodigoEquipamento] = useState('');
  const [numFabricacaoEquipamento, setNumFabricacaoEquipamento] = useState('');
  const [submittingEquipamento, setSubmittingEquipamento] = useState(false);
  const [filtroLocalEquipamento, setFiltroLocalEquipamento] = useState('');

  // Modal de Cadastro de Empresa (Área Admin)
  const [modalEmpresaAberto, setModalEmpresaAberto] = useState(false);
  const [adminKey, setAdminKey] = useState('');
  const [razaoSocialNova, setRazaoSocialNova] = useState('');
  const [cnpjNovo, setCnpjNovo] = useState('');
  const [subdominioNovo, setSubdominioNovo] = useState('');
  const [enderecoNovo, setEnderecoNovo] = useState('');
  const [submittingEmpresa, setSubmittingEmpresa] = useState(false);

  // Formulário de cadastro de funcionário (T01 - atualizado com PIS, CPF, Matrícula e vínculos)
  const [nomeCompleto, setNomeCompleto] = useState('');
  const [usuario, setUsuario] = useState('');
  const [senha, setSenha] = useState('');
  const [pisPasep, setPisPasep] = useState('');
  const [cpf, setCpf] = useState('');
  const [matricula, setMatricula] = useState('');
  const [dataAdmissao, setDataAdmissao] = useState(new Date().toISOString().split('T')[0]);
  const [localTrabalhoIdFuncionario, setLocalTrabalhoIdFuncionario] = useState('');
  const [gestorIdFuncionario, setGestorIdFuncionario] = useState('');
  const [perfilAcesso, setPerfilAcesso] = useState('COLABORADOR');
  const [submitting, setSubmitting] = useState(false);

  // Estados de gestão e busca de funcionários na empresa
  const [termoBuscaFuncionario, setTermoBuscaFuncionario] = useState('');
  const [modalDesligamentoAberto, setModalDesligamentoAberto] = useState(false);
  const [funcionarioParaDesligar, setFuncionarioParaDesligar] = useState(null);
  const [dataDesligamentoInput, setDataDesligamentoInput] = useState(new Date().toISOString().split('T')[0]);
  const [submittingDesligar, setSubmittingDesligar] = useState(false);
  const [excluindoFuncionarioId, setExcluindoFuncionarioId] = useState(null);

  // Estados de Feriados e Pontos Facultativos (T05)
  const [feriadosList, setFeriadosList] = useState([]);
  const [loadingFeriados, setLoadingFeriados] = useState(false);
  const [pontosFacultativosList, setPontosFacultativosList] = useState([]);
  const [loadingPontosFacultativos, setLoadingPontosFacultativos] = useState(false);
  const [anoCalendario, setAnoCalendario] = useState('');
  const [ufFiltro, setUfFiltro] = useState('');
  const [municipioFiltro, setMunicipioFiltro] = useState('');

  // Formulário Feriado
  const [descricaoFeriado, setDescricaoFeriado] = useState('');
  const [dataFeriado, setDataFeriado] = useState(new Date().toISOString().split('T')[0]);
  const [alcanceFeriado, setAlcanceFeriado] = useState('ESTADUAL');
  const [ufFeriado, setUfFeriado] = useState('');
  const [municipioFeriado, setMunicipioFeriado] = useState('');
  const [submittingFeriado, setSubmittingFeriado] = useState(false);

  // Formulário Ponto Facultativo
  const [descricaoPF, setDescricaoPF] = useState('');
  const [dataPF, setDataPF] = useState(new Date().toISOString().split('T')[0]);
  const [alcancePF, setAlcancePF] = useState('ESTADUAL');
  const [ufPF, setUfPF] = useState('');
  const [municipioPF, setMunicipioPF] = useState('');
  const [submittingPF, setSubmittingPF] = useState(false);

  // Histórico de cadastrados na sessão
  const [usuariosCadastrados, setUsuariosCadastrados] = useState(() => {
    try {
      const saved = localStorage.getItem('pontoCerto_usuarios_criados');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [jornadas, setJornadas] = useState([]);
  const [loadingJornadas, setLoadingJornadas] = useState(false);
  const [escalas, setEscalas] = useState([]);
  const [loadingEscalas, setLoadingEscalas] = useState(false);
  const [funcionariosEmpresa, setFuncionariosEmpresa] = useState([]);
  const [loadingFuncionarios, setLoadingFuncionarios] = useState(false);

  const [nomeJornada, setNomeJornada] = useState('');
  const [toleranciaJornada, setToleranciaJornada] = useState(10);
  const [diasJornada, setDiasJornada] = useState([
    { diaSemana: 'SEGUNDA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
    { diaSemana: 'TERÇA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
    { diaSemana: 'QUARTA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
    { diaSemana: 'QUINTA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
    { diaSemana: 'SEXTA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
    { diaSemana: 'SÁBADO', diaTrabalho: false, horaEntrada: '08:00', horaSaida: '12:00', intervaloInicio: '10:00', intervaloFim: '10:15' },
    { diaSemana: 'DOMINGO', diaTrabalho: false, horaEntrada: '', horaSaida: '', intervaloInicio: '', intervaloFim: '' },
  ]);
  const [submittingJornada, setSubmittingJornada] = useState(false);

  const [nomeEscala, setNomeEscala] = useState('');
  const [diasTrabalhoEscala, setDiasTrabalhoEscala] = useState(1);
  const [diasFolgaEscala, setDiasFolgaEscala] = useState(1);
  const [horaEntradaEscala, setHoraEntradaEscala] = useState('07:00');
  const [horaSaidaEscala, setHoraSaidaEscala] = useState('19:00');
  const [intervaloInicioEscala, setIntervaloInicioEscala] = useState('12:00');
  const [intervaloFimEscala, setIntervaloFimEscala] = useState('13:00');
  const [toleranciaEscala, setToleranciaEscala] = useState(10);
  const [submittingEscala, setSubmittingEscala] = useState(false);

  const [funcionarioIdRegime, setFuncionarioIdRegime] = useState('');
  const [tipoRegimeSelecionado, setTipoRegimeSelecionado] = useState('JORNADA');
  const [jornadaIdSelecionada, setJornadaIdSelecionada] = useState('');
  const [escalaIdSelecionada, setEscalaIdSelecionada] = useState('');
  const [dataInicioVigenciaRegime, setDataInicioVigenciaRegime] = useState(new Date().toISOString().split('T')[0]);
  const [dataFimVigenciaRegime, setDataFimVigenciaRegime] = useState('');
  const [submittingRegime, setSubmittingRegime] = useState(false);

  const [funcionarioIdConsulta, setFuncionarioIdConsulta] = useState('');
  const [dataConsulta, setDataConsulta] = useState(new Date().toISOString().split('T')[0]);
  const [horarioPrevistoResultado, setHorarioPrevistoResultado] = useState(null);
  const [loadingHorarioPrevisto, setLoadingHorarioPrevisto] = useState(false);
  const [regimesColaborador, setRegimesColaborador] = useState([]);
  const [orfasPendentesCount, setOrfasPendentesCount] = useState(0);
  const [colaboradorApuracaoId, setColaboradorApuracaoId] = useState('');

  const carregarContagemOrfas = async () => {
    try {
      const lista = await batidaService.listarBatidasOrfas('ORFA_PENDENTE');
      setOrfasPendentesCount(Array.isArray(lista) ? lista.length : 0);
    } catch {
      setOrfasPendentesCount(0);
    }
  };

  useEffect(() => {
    carregarContagemOrfas();
  }, []);

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
      setLoadingEquipamentos(true);
      const [listaEmpresas, listaLocais, listaEquipamentos] = await Promise.all([
        rhService.listarEmpresas().catch(() => []),
        rhService.listarLocaisTrabalho().catch(() => []),
        equipamentoService.listarEquipamentos().catch(() => []),
      ]);

      const arrayEmpresas = Array.isArray(listaEmpresas) ? listaEmpresas : [];
      const arrayLocais = Array.isArray(listaLocais) ? listaLocais : [];
      const arrayEquipamentos = Array.isArray(listaEquipamentos) ? listaEquipamentos : [];

      setEmpresas(arrayEmpresas);
      if (arrayEmpresas.length > 0 && !empresaIdSelecionada) {
        setEmpresaIdSelecionada(arrayEmpresas[0].id);
        carregarFuncionarios(arrayEmpresas[0].id);
      }
      setLocaisTrabalho(arrayLocais);
      if (arrayLocais.length > 0 && !localTrabalhoIdEquipamento) {
        setLocalTrabalhoIdEquipamento(arrayLocais[0].id);
      }
      setEquipamentos(arrayEquipamentos);
    } catch (err) {
      console.error('Erro ao carregar empresas, locais e equipamentos:', err);
    } finally {
      setLoadingEmpresas(false);
      setLoadingLocais(false);
      setLoadingEquipamentos(false);
    }
  };

  const carregarEquipamentos = async (filtros = {}) => {
    try {
      setLoadingEquipamentos(true);
      const lista = await equipamentoService.listarEquipamentos(filtros);
      setEquipamentos(Array.isArray(lista) ? lista : []);
    } catch (err) {
      console.error('Erro ao carregar equipamentos:', err);
    } finally {
      setLoadingEquipamentos(false);
    }
  };

  const carregarJornadas = async () => {
    try {
      setLoadingJornadas(true);
      const lista = await rhService.listarJornadas();
      const arr = Array.isArray(lista) ? lista : [];
      setJornadas(arr);
      if (arr.length > 0 && !jornadaIdSelecionada) {
        setJornadaIdSelecionada(arr[0].id);
      }
    } catch (err) {
      console.error('Erro ao carregar jornadas:', err);
    } finally {
      setLoadingJornadas(false);
    }
  };

  const carregarEscalas = async () => {
    try {
      setLoadingEscalas(true);
      const lista = await rhService.listarEscalas();
      const arr = Array.isArray(lista) ? lista : [];
      setEscalas(arr);
      if (arr.length > 0 && !escalaIdSelecionada) {
        setEscalaIdSelecionada(arr[0].id);
      }
    } catch (err) {
      console.error('Erro ao carregar escalas:', err);
    } finally {
      setLoadingEscalas(false);
    }
  };

  const carregarFuncionarios = async (empresaId) => {
    if (!empresaId) return;
    try {
      setLoadingFuncionarios(true);
      const lista = await rhService.buscarFuncionarios(empresaId);
      const arr = Array.isArray(lista) ? lista : [];
      setFuncionariosEmpresa(arr);
      if (arr.length > 0 && !funcionarioIdRegime) {
        setFuncionarioIdRegime(arr[0].id);
      }
      if (arr.length > 0 && !funcionarioIdConsulta) {
        setFuncionarioIdConsulta(arr[0].id);
      }
    } catch (err) {
      console.error('Erro ao carregar funcionários:', err);
    } finally {
      setLoadingFuncionarios(false);
    }
  };

  const handleCadastrarJornada = async (e) => {
    e.preventDefault();
    if (!nomeJornada.trim()) {
      toast.error('Informe o nome da jornada');
      return;
    }
    try {
      setSubmittingJornada(true);
      const payload = {
        nome: nomeJornada.trim(),
        toleranciaMinutos: Number(toleranciaJornada) || 10,
        dias: diasJornada.map((d) => ({
          diaSemana: d.diaSemana,
          diaTrabalho: Boolean(d.diaTrabalho),
          horaEntrada: d.diaTrabalho ? (d.horaEntrada || '08:00') : null,
          horaSaida: d.diaTrabalho ? (d.horaSaida || '17:00') : null,
          intervaloInicio: d.diaTrabalho ? (d.intervaloInicio || '12:00') : null,
          intervaloFim: d.diaTrabalho ? (d.intervaloFim || '13:00') : null,
        })),
      };
      await rhService.cadastrarJornada(payload);
      toast.success('Jornada cadastrada com sucesso!');
      setNomeJornada('');
      carregarJornadas();
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar jornada');
    } finally {
      setSubmittingJornada(false);
    }
  };

  const handleCadastrarEscala = async (e) => {
    e.preventDefault();
    if (!nomeEscala.trim()) {
      toast.error('Informe o nome da escala');
      return;
    }
    try {
      setSubmittingEscala(true);
      const payload = {
        nome: nomeEscala.trim(),
        diasTrabalho: Number(diasTrabalhoEscala) || 1,
        diasFolga: Number(diasFolgaEscala) || 1,
        horaEntrada: horaEntradaEscala || '07:00',
        horaSaida: horaSaidaEscala || '19:00',
        intervaloInicio: intervaloInicioEscala || '12:00',
        intervaloFim: intervaloFimEscala || '13:00',
        toleranciaMinutos: Number(toleranciaEscala) || 10,
      };
      await rhService.cadastrarEscala(payload);
      toast.success('Escala cadastrada com sucesso!');
      setNomeEscala('');
      carregarEscalas();
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar escala');
    } finally {
      setSubmittingEscala(false);
    }
  };

  const handleVincularRegime = async (e) => {
    e.preventDefault();
    if (!funcionarioIdRegime) {
      toast.error('Informe o ID do funcionário');
      return;
    }
    if (tipoRegimeSelecionado === 'JORNADA' && !jornadaIdSelecionada) {
      toast.error('Selecione uma jornada');
      return;
    }
    if (tipoRegimeSelecionado === 'ESCALA' && !escalaIdSelecionada) {
      toast.error('Selecione uma escala');
      return;
    }
    if (!dataInicioVigenciaRegime) {
      toast.error('Informe a data de início de vigência');
      return;
    }
    try {
      setSubmittingRegime(true);
      const payload = {
        funcionarioId: funcionarioIdRegime,
        tipoRegime: tipoRegimeSelecionado,
        jornadaId: tipoRegimeSelecionado === 'JORNADA' ? jornadaIdSelecionada : null,
        escalaId: tipoRegimeSelecionado === 'ESCALA' ? escalaIdSelecionada : null,
        dataInicioVigencia: dataInicioVigenciaRegime,
        dataFimVigencia: dataFimVigenciaRegime || null,
      };
      await rhService.vincularRegime(payload);
      toast.success('Regime de trabalho vinculado com sucesso!');
      carregarRegimesFuncionario(funcionarioIdRegime);
    } catch (err) {
      toast.error(err.message || 'Erro ao vincular regime');
    } finally {
      setSubmittingRegime(false);
    }
  };

  const handleConsultarHorarioPrevisto = async (e) => {
    if (e) e.preventDefault();
    if (!funcionarioIdConsulta) {
      toast.error('Informe o ID do colaborador para consultar');
      return;
    }
    try {
      setLoadingHorarioPrevisto(true);
      const res = await rhService.obterHorarioPrevisto(funcionarioIdConsulta, dataConsulta);
      setHorarioPrevistoResultado(res);
      toast.success('Horário previsto calculado!');
      carregarRegimesFuncionario(funcionarioIdConsulta);
    } catch (err) {
      toast.error(err.message || 'Erro ao consultar horário previsto');
      setHorarioPrevistoResultado(null);
    } finally {
      setLoadingHorarioPrevisto(false);
    }
  };

  const carregarRegimesFuncionario = async (fId) => {
    if (!fId) return;
    try {
      const res = await rhService.listarRegimesFuncionario(fId);
      setRegimesColaborador(Array.isArray(res) ? res : []);
    } catch (err) {
      console.error('Erro ao listar regimes do colaborador:', err);
    }
  };

  const handlePreencherExemploEscala = (tipo) => {
    if (tipo === '12x36') {
      setNomeEscala('Escala 12x36 Diurna');
      setDiasTrabalhoEscala(1);
      setDiasFolgaEscala(1);
      setHoraEntradaEscala('07:00');
      setHoraSaidaEscala('19:00');
      setIntervaloInicioEscala('12:00');
      setIntervaloFimEscala('13:00');
      setToleranciaEscala(10);
    } else if (tipo === '5x2') {
      setNomeEscala('Escala 5x2 Comercial');
      setDiasTrabalhoEscala(5);
      setDiasFolgaEscala(2);
      setHoraEntradaEscala('08:00');
      setHoraSaidaEscala('17:00');
      setIntervaloInicioEscala('12:00');
      setIntervaloFimEscala('13:00');
      setToleranciaEscala(10);
    } else if (tipo === '6x1') {
      setNomeEscala('Escala 6x1 Operacional');
      setDiasTrabalhoEscala(6);
      setDiasFolgaEscala(1);
      setHoraEntradaEscala('09:00');
      setHoraSaidaEscala('17:20');
      setIntervaloInicioEscala('13:00');
      setIntervaloFimEscala('14:00');
      setToleranciaEscala(10);
    }
  };

  const handleAtualizarDiaJornada = (index, campo, valor) => {
    setDiasJornada((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [campo]: valor };
      return copy;
    });
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
    carregarJornadas();
    carregarEscalas();

    return () => {
      isMounted = false;
    };
  }, []);

  // Cadastro de Empresa via API Admin (POST /api/v1/admin/empresas com X-Admin-Key)
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

      if (!adminKey.trim()) {
        toast.error('Informe a chave de administrador para cadastrar uma empresa.');
        return;
      }
      const res = await rhService.cadastrarEmpresaAdmin(payload, adminKey.trim());
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

  // Cadastro de Equipamento (T02 - Issues #16, #17, #18)
  const handleCadastrarEquipamento = async (e) => {
    e.preventDefault();

    if (!localTrabalhoIdEquipamento) {
      toast.error('Selecione um local de trabalho para o equipamento');
      return;
    }

    if (!identificacaoEquipamento.trim()) {
      toast.error('Informe a identificação do equipamento');
      return;
    }

    if (tipoEquipamento === 'RELOGIO' && !numFabricacaoEquipamento.trim()) {
      toast.error('O número de fabricação é obrigatório para relógios físicos');
      return;
    }

    try {
      setSubmittingEquipamento(true);
      const codigo = codigoEquipamento.trim() ||
        (tipoEquipamento === 'RELOGIO' && numFabricacaoEquipamento ? numFabricacaoEquipamento.trim() : null) ||
        `EQ-${Math.floor(100000 + Math.random() * 900000)}`;

      const payload = {
        codigo,
        localTrabalhoId: localTrabalhoIdEquipamento,
        tipo: tipoEquipamento,
        identificacao: identificacaoEquipamento.trim(),
        numFabricacao: tipoEquipamento === 'RELOGIO' ? numFabricacaoEquipamento.trim() : null,
      };

      const novo = await equipamentoService.cadastrarEquipamento(payload);
      toast.success(`Equipamento "${novo.identificacao || novo.codigo}" cadastrado com sucesso!`);

      // Limpa formulário
      setCodigoEquipamento('');
      setIdentificacaoEquipamento('');
      setNumFabricacaoEquipamento('');

      // Atualiza listagem
      const listaAtualizada = await equipamentoService.listarEquipamentos();
      setEquipamentos(Array.isArray(listaAtualizada) ? listaAtualizada : [novo, ...equipamentos]);
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar equipamento');
    } finally {
      setSubmittingEquipamento(false);
    }
  };

  // Desativação Lógica / Alteração de Status de Equipamento (Issue #17)
  const handleAlternarStatusEquipamento = async (equipamento) => {
    const novoStatus = equipamento.status === 'ATIVO' ? 'INATIVO' : 'ATIVO';
    const identificador = equipamento.codigo || equipamento.id;
    try {
      const atualizado = await equipamentoService.alterarStatus(identificador, novoStatus);
      toast.success(
        novoStatus === 'INATIVO'
          ? `Equipamento "${equipamento.identificacao || identificador}" desativado (marcações preservadas)!`
          : `Equipamento "${equipamento.identificacao || identificador}" reativado!`
      );
      setEquipamentos((prev) =>
        prev.map((eq) =>
          eq.id === equipamento.id || eq.codigo === equipamento.codigo
            ? { ...eq, status: atualizado.status }
            : eq
        )
      );
    } catch (err) {
      toast.error(err.message || 'Erro ao alterar status do equipamento');
    }
  };

  const handlePreencherExemploEquipamento = (tipo) => {
    const randomNum = Math.floor(100000 + Math.random() * 900000);
    if (tipo === 'RELOGIO') {
      setTipoEquipamento('RELOGIO');
      setCodigoEquipamento(`REP-${randomNum}`);
      setIdentificacaoEquipamento(`Relógio Biometria Portaria #${Math.floor(Math.random() * 9 + 1)}`);
      setNumFabricacaoEquipamento(`REP-${randomNum}`);
      toast('Exemplo de Relógio físico preenchido!', { icon: '⏰' });
    } else {
      setTipoEquipamento('ESTACAO');
      setCodigoEquipamento(`EST-${randomNum}`);
      setIdentificacaoEquipamento(`Estação Tablet Corredor #${Math.floor(Math.random() * 9 + 1)}`);
      setNumFabricacaoEquipamento('');
      toast('Exemplo de Estação Web preenchido!', { icon: '💻' });
    }
    if (locaisTrabalho.length > 0 && !localTrabalhoIdEquipamento) {
      setLocalTrabalhoIdEquipamento(locaisTrabalho[0].id);
    }
  };

  // Cadastro de Funcionário (T01 - com validação de PIS/PASEP e CPF)
  const handleCadastrar = async (e) => {
    e.preventDefault();

    if (!nomeCompleto.trim() || !usuario.trim() || !senha.trim() || !pisPasep.trim() || !cpf.trim() || !matricula.trim()) {
      toast.error('Preencha todos os campos obrigatórios (Nome, Usuário, Senha, PIS/PASEP, CPF e Matrícula)');
      return;
    }

    if (!validarPis(pisPasep)) {
      toast.error('PIS/PASEP inválido! O dígito verificador calculado não confere com a regra oficial.');
      return;
    }

    if (!validarCpf(cpf)) {
      toast.error('CPF inválido! Verifique a digitação do documento.');
      return;
    }

    try {
      setSubmitting(true);
      const payload = {
        nomeCompleto: nomeCompleto.trim(),
        usuario: usuario.trim(),
        senha: senha.trim(),
        pis_pasep: pisPasep.replace(/\D/g, ''),
        cpf: cpf.replace(/\D/g, ''),
        matricula: matricula.trim(),
        dataAdmissao: dataAdmissao || undefined,
        localTrabalhoId: localTrabalhoIdFuncionario || undefined,
        gestorId: gestorIdFuncionario || undefined,
        perfilAcesso,
      };

      const novo = await rhService.cadastrarFuncionario(payload);
      toast.success(`Funcionário criado com sucesso! ID: ${novo.id}`);

      const novaLista = [novo, ...usuariosCadastrados];
      setUsuariosCadastrados(novaLista);

      // Limpar formulário
      setNomeCompleto('');
      setUsuario('');
      setSenha('');
      setPisPasep('');
      setCpf('');
      setMatricula('');
      setDataAdmissao(new Date().toISOString().split('T')[0]);

      if (empresaIdSelecionada) {
        carregarFuncionarios(empresaIdSelecionada);
      }
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar funcionário');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePreencherExemplo = (tipo) => {
    const timestamp = Math.floor(Math.random() * 899 + 100);
    const cpfValido = formatarCpf(gerarCpfValido());
    const pisValido = formatarPis(gerarPisValido());

    if (tipo === 'COLABORADOR') {
      setNomeCompleto('João da Silva');
      setUsuario(`colaborador.${timestamp}`);
      setSenha('senha123');
      setPerfilAcesso('COLABORADOR');
      setCpf(cpfValido);
      setPisPasep(pisValido);
      setMatricula(`COL-${timestamp}`);
      setDataAdmissao(new Date().toISOString().split('T')[0]);
      if (locaisTrabalho.length > 0 && !localTrabalhoIdFuncionario) {
        setLocalTrabalhoIdFuncionario(locaisTrabalho[0].id);
      }
      const gestorExistente = funcionariosEmpresa.find((f) => f.perfilAcesso === 'GESTOR');
      if (gestorExistente) {
        setGestorIdFuncionario(gestorExistente.id);
      }
    } else if (tipo === 'GESTOR') {
      setNomeCompleto('Mariana Costa');
      setUsuario(`gestor.${timestamp}`);
      setSenha('gestor123');
      setPerfilAcesso('GESTOR');
      setCpf(cpfValido);
      setPisPasep(pisValido);
      setMatricula(`GES-${timestamp}`);
      setDataAdmissao(new Date().toISOString().split('T')[0]);
      if (locaisTrabalho.length > 0 && !localTrabalhoIdFuncionario) {
        setLocalTrabalhoIdFuncionario(locaisTrabalho[0].id);
      }
      setGestorIdFuncionario('');
    }
    toast('Exemplo com CPF e PIS válidos preenchido!', { icon: '📝' });
  };

  const handleAbrirDesligamento = (func) => {
    setFuncionarioParaDesligar(func);
    setDataDesligamentoInput(new Date().toISOString().split('T')[0]);
    setModalDesligamentoAberto(true);
  };

  const handleConfirmarDesligamento = async (e) => {
    e.preventDefault();
    if (!funcionarioParaDesligar || !dataDesligamentoInput) {
      toast.error('Informe a data de desligamento');
      return;
    }
    try {
      setSubmittingDesligar(true);
      await rhService.desligarFuncionario(funcionarioParaDesligar.id, dataDesligamentoInput);
      toast.success(`Colaborador ${funcionarioParaDesligar.nomeCompleto} desligado com sucesso!`);
      setModalDesligamentoAberto(false);
      setFuncionarioParaDesligar(null);
      carregarFuncionarios(empresaIdSelecionada);
    } catch (err) {
      toast.error(err.message || 'Erro ao registrar desligamento');
    } finally {
      setSubmittingDesligar(false);
    }
  };

  const handleExcluirFuncionario = async (func) => {
    if (!window.confirm(`Deseja realmente excluir o colaborador ${func.nomeCompleto}? Atenção: só é permitido excluir se ele não possuir marcações de ponto.`)) {
      return;
    }
    try {
      setExcluindoFuncionarioId(func.id);
      await rhService.excluirFuncionario(func.id);
      toast.success(`Colaborador ${func.nomeCompleto} excluído com sucesso!`);
      carregarFuncionarios(empresaIdSelecionada);
    } catch (err) {
      toast.error(err.message || 'Erro ao excluir colaborador');
    } finally {
      setExcluindoFuncionarioId(null);
    }
  };

  const carregarCalendarioFeriados = async (override = {}) => {
    try {
      setLoadingFeriados(true);
      const anoEfetivo = override.ano !== undefined ? override.ano : anoCalendario;
      const ufEfetiva = override.uf !== undefined ? override.uf : ufFiltro;
      const municipioEfetivo = override.municipio !== undefined ? override.municipio : municipioFiltro;
      const lista = await feriadoService.buscarCalendarioFeriados({
        ano: anoEfetivo,
        uf: ufEfetiva,
        municipio: municipioEfetivo,
        empresaId: empresaIdSelecionada || undefined,
      });
      setFeriadosList(Array.isArray(lista) ? lista : []);
    } catch (err) {
      console.error('Erro ao buscar feriados:', err);
    } finally {
      setLoadingFeriados(false);
    }
  };

  const carregarCalendarioPF = async (override = {}) => {
    try {
      setLoadingPontosFacultativos(true);
      const anoEfetivo = override.ano !== undefined ? override.ano : anoCalendario;
      const ufEfetiva = override.uf !== undefined ? override.uf : ufFiltro;
      const municipioEfetivo = override.municipio !== undefined ? override.municipio : municipioFiltro;
      const lista = await feriadoService.buscarCalendarioPontosFacultativos({
        ano: anoEfetivo,
        uf: ufEfetiva,
        municipio: municipioEfetivo,
      });
      setPontosFacultativosList(Array.isArray(lista) ? lista : []);
    } catch (err) {
      console.error('Erro ao buscar pontos facultativos:', err);
    } finally {
      setLoadingPontosFacultativos(false);
    }
  };

  const handleCadastrarFeriado = async (e) => {
    e.preventDefault();
    if (!descricaoFeriado.trim() || !dataFeriado) {
      toast.error('Informe a descrição e a data do feriado');
      return;
    }
    if (alcanceFeriado === 'ESTADUAL' && !ufFeriado.trim()) {
      toast.error('UF é obrigatória para feriados estaduais');
      return;
    }
    if (alcanceFeriado === 'MUNICIPAL' && (!ufFeriado.trim() || !municipioFeriado.trim())) {
      toast.error('UF e Município são obrigatórios para feriados municipais');
      return;
    }

    try {
      setSubmittingFeriado(true);
      const payload = {
        descricao: descricaoFeriado.trim(),
        data: dataFeriado,
        alcance: alcanceFeriado,
        uf: alcanceFeriado !== 'NACIONAL' ? ufFeriado.trim().toUpperCase() : null,
        municipio: alcanceFeriado === 'MUNICIPAL' ? municipioFeriado.trim() : null,
        empresaId: alcanceFeriado !== 'NACIONAL' && empresaIdSelecionada ? empresaIdSelecionada : null,
      };
      await feriadoService.cadastrarFeriado(payload);
      toast.success('Feriado cadastrado com sucesso!');
      setDescricaoFeriado('');
      setMunicipioFeriado('');
      carregarCalendarioFeriados();
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar feriado');
    } finally {
      setSubmittingFeriado(false);
    }
  };

  const handleInativarFeriado = async (id, descricao) => {
    if (!window.confirm(`Deseja realmente inativar o feriado "${descricao}"?`)) return;
    try {
      await feriadoService.inativarFeriado(id);
      toast.success('Feriado inativado com sucesso!');
      carregarCalendarioFeriados();
    } catch (err) {
      toast.error(err.message || 'Erro ao inativar feriado');
    }
  };

  const handleCadastrarPontoFacultativo = async (e) => {
    e.preventDefault();
    if (!descricaoPF.trim() || !dataPF) {
      toast.error('Informe a descrição e a data do ponto facultativo');
      return;
    }
    if (alcancePF === 'ESTADUAL' && !ufPF.trim()) {
      toast.error('UF é obrigatória para pontos facultativos estaduais');
      return;
    }
    if (alcancePF === 'MUNICIPAL' && (!ufPF.trim() || !municipioPF.trim())) {
      toast.error('UF e Município são obrigatórios para pontos facultativos municipais');
      return;
    }

    try {
      setSubmittingPF(true);
      const payload = {
        descricao: descricaoPF.trim(),
        data: dataPF,
        alcance: alcancePF,
        uf: alcancePF !== 'NACIONAL' ? ufPF.trim().toUpperCase() : null,
        municipio: alcancePF === 'MUNICIPAL' ? municipioPF.trim() : null,
      };
      await feriadoService.cadastrarPontoFacultativo(payload);
      toast.success('Ponto facultativo cadastrado com sucesso!');
      setDescricaoPF('');
      setMunicipioPF('');
      carregarCalendarioPF();
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar ponto facultativo');
    } finally {
      setSubmittingPF(false);
    }
  };

  const handleInativarPF = async (id, descricao) => {
    if (!window.confirm(`Deseja realmente inativar o ponto facultativo "${descricao}"?`)) return;
    try {
      await feriadoService.inativarPontoFacultativo(id);
      toast.success('Ponto facultativo inativado com sucesso!');
      carregarCalendarioPF();
    } catch (err) {
      toast.error(err.message || 'Erro ao inativar ponto facultativo');
    }
  };

  const handleTestarLoginNovo = (usr) => {
    logout();
    navigate('/login');
    toast(`Faça login com o usuário: ${usr.usuario}`, { icon: '🔑', duration: 6000 });
  };

  const handleSelectAba = (novaAba) => {
    setAbaAtiva(novaAba);
    if (novaAba === 'feriados') {
      carregarCalendarioFeriados();
      carregarCalendarioPF();
    } else if (novaAba === 'batidas-orfas') {
      carregarContagemOrfas();
    } else if (novaAba === 'apuracao' && !colaboradorApuracaoId && funcionariosEmpresa.length > 0) {
      setColaboradorApuracaoId(funcionariosEmpresa[0].id);
    }
  };

  const empresaNomeAtual =
    empresas.find((e) => e.id === empresaIdSelecionada)?.razaoSocial || undefined;

  const rhNavItems = [
    { type: 'divider', key: 'div-cad', label: 'Estrutura & Pessoas' },
    { key: 'locais', label: 'Locais de Trabalho', icon: MapPin, badge: locaisTrabalho.length || undefined },
    { key: 'equipamentos', label: 'Equipamentos', icon: HardDrive, badge: equipamentos.length || undefined },
    { key: 'usuarios', label: 'Colaboradores', icon: UserPlus, badge: funcionariosEmpresa.length || undefined },
    { key: 'jornadas', label: 'Jornadas & Escalas', icon: Clock },
    { key: 'feriados', label: 'Feriados & Calendário', icon: CalendarCheck },
    { type: 'divider', key: 'div-op', label: 'Operação & Apuração' },
    { key: 'importacao', label: 'Importação REP / AFD', icon: Upload },
    { key: 'historico-importacao', label: 'Histórico Importação', icon: History },
    { key: 'batidas-orfas', label: 'Batidas Órfãs', icon: AlertCircle, badge: orfasPendentesCount || undefined },
    { key: 'inconsistencias', label: 'Inconsistências', icon: Flag },
    { key: 'apuracao', label: 'Espelho & Apuração', icon: CalendarDays },
    { key: 'ocorrencias', label: 'Ocorrências & Abonos', icon: Calendar },
    { key: 'banco-horas', label: 'Banco de Horas', icon: Layers },
    { type: 'divider', key: 'div-fech', label: 'Fechamento & Folha' },
    { key: 'fechamento', label: 'Fechamento de Período', icon: Lock },
    { key: 'relatorios', label: 'Relatórios Gerenciais', icon: BarChart3 },
    { key: 'folha', label: 'Exportação de Folha', icon: FileSpreadsheet },
  ];

  return (
    <AppShell
      brandName={empresaNomeAtual}
      brandSubtitle="Departamento Pessoal (RH)"
      navItems={rhNavItems}
      activeKey={abaAtiva}
      onSelectNav={handleSelectAba}
    >
      <div className="space-y-6">

        {abaAtiva === 'locais' && (
          <LocaisSection
            empresaId={empresaIdSelecionada}
            empresaNome={empresaNomeAtual}
            locais={locaisTrabalho}
            loading={loadingLocais}
            onAtualizado={carregarEmpresasELocais}
          />
        )}

        {abaAtiva === 'equipamentos' && (
          <EquipamentosSection
            locais={locaisTrabalho}
            equipamentos={equipamentos}
            loading={loadingEquipamentos}
            onAtualizado={() => carregarEquipamentos()}
          />
        )}

        {abaAtiva === 'usuarios' && (
          <ColaboradoresSection
            locais={locaisTrabalho}
            funcionarios={funcionariosEmpresa}
            loading={loadingFuncionarios}
            onAtualizado={() => empresaIdSelecionada && carregarFuncionarios(empresaIdSelecionada)}
          />
        )}

        <div className="theme-harmonized space-y-6">
      {/* Conteúdo da Aba 5: Feriados & Pontos Facultativos (T05) */}
      {abaAtiva === 'feriados' && (
        <div className="space-y-8">
          <div className="rounded-2xl bg-gradient-to-br from-amber-950/20 via-slate-900 to-slate-900 border border-amber-500/30 p-6 shadow-2xl backdrop-blur-xl">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800 pb-5">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
                  <CalendarCheck size={22} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-lg font-bold text-white">
                      Gestão de Feriados e Pontos Facultativos
                    </h3>
                  </div>
                  <p className="text-xs text-slate-400">
                    Feriados nacionais aparecem automaticamente. Cadastre abaixo feriados estaduais, municipais e pontos facultativos da sua empresa.
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 flex-wrap">
                <button
                  type="button"
                  onClick={() => {
                    carregarCalendarioFeriados();
                    carregarCalendarioPF();
                  }}
                  disabled={loadingFeriados || loadingPontosFacultativos}
                  className="text-xs px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer flex items-center gap-1.5 border border-slate-700"
                >
                  <RefreshCw size={13} className={loadingFeriados || loadingPontosFacultativos ? 'animate-spin' : ''} />
                  <span>Recarregar Calendário</span>
                </button>
              </div>
            </div>

            {/* Grid: Formulário Feriado / Ponto Facultativo + Calendário */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-6">
              {/* Coluna 1: Formulários de Cadastro */}
              <div className="lg:col-span-5 space-y-6">
                {/* Card Feriado */}
                <div className="rounded-2xl bg-slate-950/60 border border-slate-800 p-5">
                  <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-800">
                    <div className="flex items-center gap-2">
                      <Flag size={16} className="text-amber-400" />
                      <h4 className="text-sm font-bold text-white">Cadastrar Novo Feriado</h4>
                    </div>
                  </div>

                  <form onSubmit={handleCadastrarFeriado} className="space-y-3">
                    <div>
                      <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Descrição do Feriado *
                      </label>
                      <input
                        type="text"
                        required
                        value={descricaoFeriado}
                        onChange={(e) => setDescricaoFeriado(e.target.value)}
                        placeholder="Ex: Tiradentes / Aniversário da Cidade"
                        className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Data *
                        </label>
                        <input
                          type="date"
                          required
                          value={dataFeriado}
                          onChange={(e) => setDataFeriado(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                        />
                      </div>

                      <div>
                        <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Alcance *
                        </label>
                        <select
                          value={alcanceFeriado === 'NACIONAL' ? 'ESTADUAL' : alcanceFeriado}
                          onChange={(e) => setAlcanceFeriado(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                        >
                          <option value="ESTADUAL">Estadual (UF)</option>
                          <option value="MUNICIPAL">Municipal</option>
                        </select>
                      </div>
                    </div>

                    {alcanceFeriado !== 'NACIONAL' && (
                      <div className="grid grid-cols-2 gap-3 pt-1">
                        <div>
                          <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                            UF *
                          </label>
                          <input
                            type="text"
                            maxLength={2}
                            required
                            value={ufFeriado}
                            onChange={(e) => setUfFeriado(e.target.value.toUpperCase())}
                            placeholder="Ex: PE"
                            className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-amber-500 focus:outline-none font-mono"
                          />
                        </div>

                        {alcanceFeriado === 'MUNICIPAL' && (
                          <div>
                            <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                              Município *
                            </label>
                            <input
                              type="text"
                              required
                              value={municipioFeriado}
                              onChange={(e) => setMunicipioFeriado(e.target.value)}
                              placeholder="Ex: Recife"
                              className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                            />
                          </div>
                        )}
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={submittingFeriado}
                      className="w-full mt-2 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-xs font-semibold text-slate-950 transition-colors cursor-pointer disabled:opacity-50"
                    >
                      {submittingFeriado ? 'Cadastrando...' : '+ Cadastrar Feriado'}
                    </button>
                  </form>
                </div>

                {/* Card Ponto Facultativo */}
                <div className="rounded-2xl bg-slate-950/60 border border-slate-800 p-5">
                  <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-800">
                    <div className="flex items-center gap-2">
                      <Clock size={16} className="text-indigo-400" />
                      <h4 className="text-sm font-bold text-white">Cadastrar Ponto Facultativo</h4>
                    </div>
                  </div>

                  <form onSubmit={handleCadastrarPontoFacultativo} className="space-y-3">
                    <div>
                      <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Descrição *
                      </label>
                      <input
                        type="text"
                        required
                        value={descricaoPF}
                        onChange={(e) => setDescricaoPF(e.target.value)}
                        placeholder="Ex: Carnaval / Corpus Christi"
                        className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Data *
                        </label>
                        <input
                          type="date"
                          required
                          value={dataPF}
                          onChange={(e) => setDataPF(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>

                      <div>
                        <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Alcance *
                        </label>
                        <select
                          value={alcancePF === 'NACIONAL' ? 'ESTADUAL' : alcancePF}
                          onChange={(e) => setAlcancePF(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                        >
                          <option value="ESTADUAL">Estadual (UF)</option>
                          <option value="MUNICIPAL">Municipal</option>
                        </select>
                      </div>
                    </div>

                    {alcancePF !== 'NACIONAL' && (
                      <div className="grid grid-cols-2 gap-3 pt-1">
                        <div>
                          <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                            UF *
                          </label>
                          <input
                            type="text"
                            maxLength={2}
                            required
                            value={ufPF}
                            onChange={(e) => setUfPF(e.target.value.toUpperCase())}
                            placeholder="Ex: PE"
                            className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none font-mono"
                          />
                        </div>

                        {alcancePF === 'MUNICIPAL' && (
                          <div>
                            <label className="block text-[11px] font-semibold uppercase tracking-wider text-slate-300 mb-1">
                              Município *
                            </label>
                            <input
                              type="text"
                              required
                              value={municipioPF}
                              onChange={(e) => setMunicipioPF(e.target.value)}
                              placeholder="Ex: Recife"
                              className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                            />
                          </div>
                        )}
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={submittingPF}
                      className="w-full mt-2 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white transition-colors cursor-pointer disabled:opacity-50"
                    >
                      {submittingPF ? 'Cadastrando...' : '+ Cadastrar Ponto Facultativo'}
                    </button>
                  </form>
                </div>
              </div>

              {/* Coluna 2: Calendário e Listagem */}
              <div className="lg:col-span-7 space-y-4">
                {/* Filtro de Calendário */}
                <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800 grid grid-cols-1 sm:grid-cols-4 gap-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-400 mb-1">Ano (Opcional)</label>
                    <input
                      type="number"
                      min="2020"
                      max="2035"
                      value={anoCalendario}
                      onChange={(e) => setAnoCalendario(e.target.value)}
                      placeholder="Todos"
                      className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-400 mb-1">UF (Opcional)</label>
                    <input
                      type="text"
                      maxLength={2}
                      value={ufFiltro}
                      onChange={(e) => setUfFiltro(e.target.value.toUpperCase())}
                      placeholder="Todas"
                      className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs text-white font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-400 mb-1">Município (Opcional)</label>
                    <input
                      type="text"
                      value={municipioFiltro}
                      onChange={(e) => setMunicipioFiltro(e.target.value)}
                      placeholder="Todos"
                      className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs text-white"
                    />
                  </div>
                  <div className="flex items-end gap-1.5">
                    <button
                      type="button"
                      onClick={() => {
                        carregarCalendarioFeriados();
                        carregarCalendarioPF();
                      }}
                      className="flex-1 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white cursor-pointer"
                    >
                      Filtrar
                    </button>
                    {(anoCalendario !== '' || ufFiltro !== '' || municipioFiltro !== '') && (
                      <button
                        type="button"
                        onClick={() => {
                          setAnoCalendario('');
                          setUfFiltro('');
                          setMunicipioFiltro('');
                          carregarCalendarioFeriados({ ano: '', uf: '', municipio: '' });
                          carregarCalendarioPF({ ano: '', uf: '', municipio: '' });
                        }}
                        className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-medium text-slate-200 cursor-pointer"
                        title="Limpar filtros e exibir todos os feriados cadastrados"
                      >
                        Todos
                      </button>
                    )}
                  </div>
                </div>

                {/* Lista de Feriados */}
                <div className="rounded-2xl bg-slate-950/60 border border-slate-800 p-4">
                  <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-800">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
                      <Flag size={14} /> Feriados Cadastrados ({feriadosList.length})
                    </h4>
                  </div>

                  {loadingFeriados ? (
                    <div className="py-6 text-center text-xs text-slate-400">Consultando feriados...</div>
                  ) : feriadosList.length === 0 ? (
                    <div className="py-6 text-center text-xs text-slate-500">Nenhum feriado localizado para os filtros informados.</div>
                  ) : (
                    <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                      {feriadosList.map((f) => (
                        <div
                          key={f.id}
                          className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between gap-3 text-xs"
                        >
                          <div>
                            <div className="font-semibold text-white flex flex-wrap items-center gap-2">
                              <span>{f.descricao}</span>
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-amber-300 font-mono">
                                {f.alcance}
                              </span>
                              {f.uf && <span className="text-[10px] text-slate-400 font-mono">{f.uf}</span>}
                              {f.municipio && <span className="text-[10px] text-slate-400 font-mono">· {f.municipio}</span>}
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono mt-0.5">
                              Data: {f.data ? f.data.split('-').reverse().join('/') : '—'}
                            </div>
                          </div>
                          <div className="flex items-center gap-2 shrink-0">
                            {f.alcance === 'NACIONAL' ? (
                              <span className="text-[10px] px-2 py-0.5 rounded-md bg-slate-800 text-slate-400 font-medium">
                                Global
                              </span>
                            ) : f.ativo ? (
                              <button
                                type="button"
                                onClick={() => handleInativarFeriado(f.id, f.descricao)}
                                className="text-[10px] px-2 py-1 rounded-lg bg-red-950/60 border border-red-800/40 text-red-300 hover:bg-red-900/50 cursor-pointer"
                                title="Inativar feriado"
                              >
                                Inativar
                              </button>
                            ) : (
                              <span className="text-[10px] text-slate-500 italic">Inativo</span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Lista de Pontos Facultativos */}
                <div className="rounded-2xl bg-slate-950/60 border border-slate-800 p-4">
                  <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-800">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-indigo-400 flex items-center gap-1.5">
                      <Clock size={14} /> Pontos Facultativos ({pontosFacultativosList.length})
                    </h4>
                  </div>

                  {loadingPontosFacultativos ? (
                    <div className="py-6 text-center text-xs text-slate-400">Consultando pontos facultativos...</div>
                  ) : pontosFacultativosList.length === 0 ? (
                    <div className="py-6 text-center text-xs text-slate-500">Nenhum ponto facultativo localizado.</div>
                  ) : (
                    <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                      {pontosFacultativosList.map((pf) => (
                        <div
                          key={pf.id}
                          className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between gap-3 text-xs"
                        >
                          <div>
                            <div className="font-semibold text-white flex flex-wrap items-center gap-2">
                              <span>{pf.descricao}</span>
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-indigo-300 font-mono">
                                {pf.alcance}
                              </span>
                              {pf.uf && <span className="text-[10px] text-slate-400 font-mono">{pf.uf}</span>}
                              {pf.municipio && <span className="text-[10px] text-slate-400 font-mono">· {pf.municipio}</span>}
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono mt-0.5">
                              Data: {pf.data ? pf.data.split('-').reverse().join('/') : '—'}
                            </div>
                          </div>
                          <div className="flex items-center gap-2 shrink-0">
                            {pf.alcance === 'NACIONAL' ? (
                              <span className="text-[10px] px-2 py-0.5 rounded-md bg-slate-800 text-slate-400 font-medium">
                                Global
                              </span>
                            ) : pf.ativo ? (
                              <button
                                type="button"
                                onClick={() => handleInativarPF(pf.id, pf.descricao)}
                                className="text-[10px] px-2 py-1 rounded-lg bg-red-950/60 border border-red-800/40 text-red-300 hover:bg-red-900/50 cursor-pointer"
                                title="Inativar ponto facultativo"
                              >
                                Inativar
                              </button>
                            ) : (
                              <span className="text-[10px] text-slate-500 italic">Inativo</span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {abaAtiva === 'jornadas' && (
          <div className="space-y-8">
            <div className="rounded-2xl bg-gradient-to-br from-indigo-950/40 via-slate-900 to-slate-900 border border-indigo-500/30 p-6 shadow-2xl backdrop-blur-xl">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-800 pb-5">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                    <Clock size={22} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-lg font-bold text-white">Consulta de Horário Previsto</h3>
                    </div>
                    <p className="text-xs text-slate-400">
                      Verifique a jornada, escala ou folga prevista para qualquer colaborador em uma data específica.
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 flex-wrap">
                  <button
                    type="button"
                    onClick={() => carregarFuncionarios(empresaIdSelecionada || (empresas[0] && empresas[0].id))}
                    className="text-xs px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer flex items-center gap-1.5 border border-slate-700"
                  >
                    <RefreshCw size={13} className={loadingFuncionarios ? 'animate-spin' : ''} />
                    <span>Carregar Colaboradores da Empresa</span>
                  </button>
                </div>
              </div>

              <form onSubmit={handleConsultarHorarioPrevisto} className="mt-5 space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
                  <div className="md:col-span-6">
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                      Colaborador *
                    </label>
                    {funcionariosEmpresa.length > 0 ? (
                      <select
                        value={funcionarioIdConsulta}
                        onChange={(e) => {
                          setFuncionarioIdConsulta(e.target.value);
                          setFuncionarioIdRegime(e.target.value);
                        }}
                        className="w-full h-10 rounded-xl border border-slate-700 bg-slate-950/60 px-3 text-sm text-slate-100 focus:outline-none"
                      >
                        <option value="">Selecione um colaborador...</option>
                        {funcionariosEmpresa.map((f) => (
                          <option key={f.id} value={f.id}>
                            {f.nomeCompleto} ({f.usuario || f.matricula}) - {f.perfilAcesso}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <input
                        type="text"
                        required
                        value={funcionarioIdConsulta}
                        onChange={(e) => {
                          setFuncionarioIdConsulta(e.target.value);
                          setFuncionarioIdRegime(e.target.value);
                        }}
                        placeholder="Cole o UUID do funcionário ou clique em 'Carregar Colaboradores'"
                        className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3.5 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:outline-none font-mono"
                      />
                    )}
                  </div>

                  <div className="md:col-span-4">
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
                      Data da Consulta *
                    </label>
                    <input
                      type="date"
                      required
                      value={dataConsulta}
                      onChange={(e) => setDataConsulta(e.target.value)}
                      className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3.5 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                    />
                  </div>

                  <div className="md:col-span-2 flex items-end">
                    <button
                      type="submit"
                      disabled={loadingHorarioPrevisto}
                      className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white shadow-lg shadow-indigo-600/25 transition-all cursor-pointer disabled:opacity-50"
                    >
                      {loadingHorarioPrevisto ? (
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      ) : (
                        <Search size={15} />
                      )}
                      <span>Consultar</span>
                    </button>
                  </div>
                </div>

                <div className="flex items-center gap-2 flex-wrap text-xs text-slate-400">
                  <span className="font-semibold text-slate-300">Atalhos de data:</span>
                  <button
                    type="button"
                    onClick={() => setDataConsulta(new Date().toISOString().split('T')[0])}
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                  >
                    Hoje
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      const d = new Date();
                      d.setDate(d.getDate() + 1);
                      setDataConsulta(d.toISOString().split('T')[0]);
                    }}
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                  >
                    Amanhã
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      const d = new Date();
                      const dist = (6 - d.getDay() + 7) % 7 || 7;
                      d.setDate(d.getDate() + dist);
                      setDataConsulta(d.toISOString().split('T')[0]);
                    }}
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                  >
                    Próximo Sábado
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      const d = new Date();
                      const dist = (7 - d.getDay()) % 7 || 7;
                      d.setDate(d.getDate() + dist);
                      setDataConsulta(d.toISOString().split('T')[0]);
                    }}
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                  >
                    Próximo Domingo
                  </button>
                </div>
              </form>

              {horarioPrevistoResultado && (
                <div className="mt-6 pt-6 border-t border-slate-800/80 animate-in fade-in duration-200">
                  <div className="rounded-xl bg-slate-950/80 border border-slate-800 p-5 space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <span
                          className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold tracking-wide uppercase border ${
                            horarioPrevistoResultado.diaTrabalho
                              ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
                              : 'bg-amber-500/20 text-amber-300 border-amber-500/30'
                          }`}
                        >
                          {horarioPrevistoResultado.diaTrabalho ? (
                            <>
                              <CheckCircle2 size={14} /> Dia de Trabalho Previsto
                            </>
                          ) : (
                            <>
                              <AlertCircle size={14} /> Folga / DSR
                            </>
                          )}
                        </span>
                        <span className="text-xs font-mono text-slate-400">
                          {horarioPrevistoResultado.data} ({horarioPrevistoResultado.diaSemana})
                        </span>
                      </div>

                      <div className="text-xs text-slate-400">
                        Regime: <strong className="text-white font-mono">{horarioPrevistoResultado.tipoRegime || 'Nenhum'}</strong> {horarioPrevistoResultado.descricaoRegime ? `(${horarioPrevistoResultado.descricaoRegime})` : ''}
                      </div>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
                      <div className="p-3 rounded-lg bg-slate-900 border border-slate-800">
                        <span className="text-[11px] text-slate-400 block mb-1">Entrada Prevista</span>
                        <span className="text-base font-bold font-mono text-white">
                          {horarioPrevistoResultado.horaEntrada || '--:--'}
                        </span>
                      </div>

                      <div className="p-3 rounded-lg bg-slate-900 border border-slate-800">
                        <span className="text-[11px] text-slate-400 block mb-1">Saída Prevista</span>
                        <span className="text-base font-bold font-mono text-white">
                          {horarioPrevistoResultado.horaSaida || '--:--'}
                        </span>
                      </div>

                      <div className="p-3 rounded-lg bg-slate-900 border border-slate-800">
                        <span className="text-[11px] text-slate-400 block mb-1">Intervalo</span>
                        <span className="text-xs font-bold font-mono text-white block mt-1">
                          {horarioPrevistoResultado.intervaloInicio
                            ? `${horarioPrevistoResultado.intervaloInicio} às ${horarioPrevistoResultado.intervaloFim}`
                            : 'Sem intervalo'}
                        </span>
                      </div>

                      <div className="p-3 rounded-lg bg-slate-900 border border-slate-800">
                        <span className="text-[11px] text-slate-400 block mb-1">Carga Prevista</span>
                        <span className="text-base font-bold font-mono text-indigo-400">
                          {horarioPrevistoResultado.cargaDiariaMinutos || 0} min
                          {horarioPrevistoResultado.cargaDiariaMinutos > 0 && (
                            <span className="text-[11px] font-normal text-slate-400 ml-1">
                              ({Math.floor(horarioPrevistoResultado.cargaDiariaMinutos / 60)}h{horarioPrevistoResultado.cargaDiariaMinutos % 60 ? ` ${horarioPrevistoResultado.cargaDiariaMinutos % 60}m` : ''})
                            </span>
                          )}
                        </span>
                      </div>
                    </div>

                    <div className="p-3 rounded-lg bg-indigo-950/30 border border-indigo-800/30 text-xs text-indigo-200 flex items-center justify-between flex-wrap gap-2">
                      <span>
                        <strong>Mensagem:</strong> {horarioPrevistoResultado.mensagem}
                      </span>
                      <span>
                        <strong>Tolerância:</strong> {horarioPrevistoResultado.toleranciaMinutos || 10} minutos
                      </span>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
              <div className="lg:col-span-6 space-y-8">
                <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-5">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                        <CalendarDays size={20} />
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-white">Cadastrar Nova Jornada</h3>
                        <p className="text-xs text-slate-400">Define o quadro de horários semanais</p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setDiasJornada([
                          { diaSemana: 'SEGUNDA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
                          { diaSemana: 'TERÇA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
                          { diaSemana: 'QUARTA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
                          { diaSemana: 'QUINTA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
                          { diaSemana: 'SEXTA', diaTrabalho: true, horaEntrada: '08:00', horaSaida: '17:00', intervaloInicio: '12:00', intervaloFim: '13:00' },
                          { diaSemana: 'SÁBADO', diaTrabalho: false, horaEntrada: '', horaSaida: '', intervaloInicio: '', intervaloFim: '' },
                          { diaSemana: 'DOMINGO', diaTrabalho: false, horaEntrada: '', horaSaida: '', intervaloInicio: '', intervaloFim: '' },
                        ]);
                        toast.success('Dias pré-preenchidos como Seg a Sex (8h-17h)!');
                      }}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                    >
                      Seg-Sex Padrão
                    </button>
                  </div>

                  <form onSubmit={handleCadastrarJornada} className="space-y-4">
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                      <div className="sm:col-span-2">
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Nome da Jornada *
                        </label>
                        <input
                          type="text"
                          required
                          value={nomeJornada}
                          onChange={(e) => setNomeJornada(e.target.value)}
                          placeholder="Ex: Comercial 44h, Administrativo 40h"
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3.5 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Tolerância (min)
                        </label>
                        <input
                          type="number"
                          min="0"
                          max="60"
                          value={toleranciaJornada}
                          onChange={(e) => setToleranciaJornada(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3.5 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <div className="space-y-2 border border-slate-800 rounded-xl p-3 bg-slate-950/40">
                      <span className="text-xs font-semibold text-slate-300 block mb-2">
                        Configuração dos Dias da Semana
                      </span>
                      {diasJornada.map((dia, idx) => (
                        <div
                          key={dia.diaSemana}
                          className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2 rounded-lg bg-slate-900/60 border border-slate-800/80 text-xs"
                        >
                          <label className="flex items-center gap-2 cursor-pointer w-28">
                            <input
                              type="checkbox"
                              checked={dia.diaTrabalho}
                              onChange={(e) => handleAtualizarDiaJornada(idx, 'diaTrabalho', e.target.checked)}
                              className="rounded border-slate-700 text-indigo-600 focus:ring-indigo-500 bg-slate-950"
                            />
                            <span className={dia.diaTrabalho ? 'font-bold text-slate-100' : 'text-slate-500'}>
                              {dia.diaSemana}
                            </span>
                          </label>

                          {dia.diaTrabalho ? (
                            <div className="flex items-center gap-2 flex-wrap">
                              <input
                                type="time"
                                value={dia.horaEntrada}
                                onChange={(e) => handleAtualizarDiaJornada(idx, 'horaEntrada', e.target.value)}
                                className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-slate-200"
                              />
                              <span className="text-slate-500">até</span>
                              <input
                                type="time"
                                value={dia.horaSaida}
                                onChange={(e) => handleAtualizarDiaJornada(idx, 'horaSaida', e.target.value)}
                                className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-slate-200"
                              />
                              <span className="text-slate-500">| Intervalo:</span>
                              <input
                                type="time"
                                value={dia.intervaloInicio}
                                onChange={(e) => handleAtualizarDiaJornada(idx, 'intervaloInicio', e.target.value)}
                                className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-slate-200"
                              />
                              <span className="text-slate-500">-</span>
                              <input
                                type="time"
                                value={dia.intervaloFim}
                                onChange={(e) => handleAtualizarDiaJornada(idx, 'intervaloFim', e.target.value)}
                                className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-slate-200"
                              />
                            </div>
                          ) : (
                            <span className="text-slate-500 italic">Folga / DSR</span>
                          )}
                        </div>
                      ))}
                    </div>

                    <button
                      type="submit"
                      disabled={submittingJornada}
                      className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white shadow-lg shadow-indigo-600/25 transition-all cursor-pointer disabled:opacity-50"
                    >
                      {submittingJornada ? (
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      ) : (
                        <Plus size={15} />
                      )}
                      <span>Cadastrar Jornada</span>
                    </button>
                  </form>
                </div>

                <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-white">Jornadas Cadastradas</h4>
                      <span className="px-2 py-0.5 rounded-full text-xs font-mono bg-indigo-500/20 text-indigo-300">
                        {jornadas.length}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={carregarJornadas}
                      className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                    >
                      <RefreshCw size={13} className={loadingJornadas ? 'animate-spin' : ''} />
                    </button>
                  </div>

                  {loadingJornadas ? (
                    <div className="py-6 text-center text-xs text-slate-500">Carregando jornadas...</div>
                  ) : jornadas.length === 0 ? (
                    <div className="py-6 text-center text-xs text-slate-500">Nenhuma jornada cadastrada ainda.</div>
                  ) : (
                    <div className="space-y-3">
                      {jornadas.map((j) => (
                        <div key={j.id} className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800 text-xs space-y-2">
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-slate-100 text-sm">{j.nome}</span>
                            <span className="font-mono px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">
                              Carga: {Math.floor((j.cargaHorariaSemanaMinutos || 0) / 60)}h/sem ({j.cargaHorariaSemanaMinutos || 0}m)
                            </span>
                          </div>
                          <div className="flex items-center gap-1.5 flex-wrap">
                            {j.dias?.map((d) => (
                              <span
                                key={d.id || d.diaSemana}
                                className={`px-2 py-0.5 rounded text-[10px] font-mono ${
                                  d.diaTrabalho
                                    ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                                    : 'bg-slate-800 text-slate-500'
                                }`}
                              >
                                {d.diaSemana?.slice(0, 3)}
                              </span>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="lg:col-span-6 space-y-8">
                <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-5">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-xl bg-purple-500/10 text-purple-400">
                        <Layers size={20} />
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-white">Cadastrar Nova Escala</h3>
                        <p className="text-xs text-slate-400">Regime de revezamento cíclico (ex: 12x36, 5x2)</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <button
                        type="button"
                        onClick={() => handlePreencherExemploEscala('12x36')}
                        className="text-[11px] px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700"
                      >
                        12x36
                      </button>
                      <button
                        type="button"
                        onClick={() => handlePreencherExemploEscala('5x2')}
                        className="text-[11px] px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700"
                      >
                        5x2
                      </button>
                      <button
                        type="button"
                        onClick={() => handlePreencherExemploEscala('6x1')}
                        className="text-[11px] px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700"
                      >
                        6x1
                      </button>
                    </div>
                  </div>

                  <form onSubmit={handleCadastrarEscala} className="space-y-4">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Nome da Escala *
                      </label>
                      <input
                        type="text"
                        required
                        value={nomeEscala}
                        onChange={(e) => setNomeEscala(e.target.value)}
                        placeholder="Ex: Escala 12x36 Diurna"
                        className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3.5 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
                      />
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Dias Trabalho *
                        </label>
                        <input
                          type="number"
                          min="1"
                          required
                          value={diasTrabalhoEscala}
                          onChange={(e) => setDiasTrabalhoEscala(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Dias Folga *
                        </label>
                        <input
                          type="number"
                          min="1"
                          required
                          value={diasFolgaEscala}
                          onChange={(e) => setDiasFolgaEscala(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Tolerância (min)
                        </label>
                        <input
                          type="number"
                          min="0"
                          value={toleranciaEscala}
                          onChange={(e) => setToleranciaEscala(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Entrada *
                        </label>
                        <input
                          type="time"
                          required
                          value={horaEntradaEscala}
                          onChange={(e) => setHoraEntradaEscala(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-2 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Saída *
                        </label>
                        <input
                          type="time"
                          required
                          value={horaSaidaEscala}
                          onChange={(e) => setHoraSaidaEscala(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-2 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Intervalo Início
                        </label>
                        <input
                          type="time"
                          value={intervaloInicioEscala}
                          onChange={(e) => setIntervaloInicioEscala(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-2 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Intervalo Fim
                        </label>
                        <input
                          type="time"
                          value={intervaloFimEscala}
                          onChange={(e) => setIntervaloFimEscala(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-2 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <button
                      type="submit"
                      disabled={submittingEscala}
                      className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-xs font-semibold text-white shadow-lg shadow-purple-600/25 transition-all cursor-pointer disabled:opacity-50"
                    >
                      {submittingEscala ? (
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      ) : (
                        <Plus size={15} />
                      )}
                      <span>Cadastrar Escala</span>
                    </button>
                  </form>
                </div>

                <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-white">Escalas Cadastradas</h4>
                      <span className="px-2 py-0.5 rounded-full text-xs font-mono bg-purple-500/20 text-purple-300">
                        {escalas.length}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={carregarEscalas}
                      className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                    >
                      <RefreshCw size={13} className={loadingEscalas ? 'animate-spin' : ''} />
                    </button>
                  </div>

                  {loadingEscalas ? (
                    <div className="py-6 text-center text-xs text-slate-500">Carregando escalas...</div>
                  ) : escalas.length === 0 ? (
                    <div className="py-6 text-center text-xs text-slate-500">Nenhuma escala cadastrada ainda.</div>
                  ) : (
                    <div className="space-y-3">
                      {escalas.map((esc) => (
                        <div key={esc.id} className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800 text-xs space-y-2">
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-slate-100 text-sm">{esc.nome}</span>
                            <span className="font-mono px-2 py-0.5 rounded bg-purple-500/10 text-purple-300 border border-purple-500/20">
                              {esc.diasTrabalho}T x {esc.diasFolga}F ({esc.cargaDiariaMinutos || 0}m/dia)
                            </span>
                          </div>
                          <div className="text-slate-400 font-mono text-[11px]">
                            Turno: {esc.horaEntrada} às {esc.horaSaida} | Intervalo: {esc.intervaloInicio} às {esc.intervaloFim}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="rounded-2xl bg-gradient-to-br from-indigo-950/30 to-slate-900 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                  <div className="flex items-center gap-2.5 border-b border-slate-800 pb-4 mb-5">
                    <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                      <UserPlus size={20} />
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-white">Vincular Regime ao Colaborador</h3>
                      <p className="text-xs text-slate-400">Atribui uma Jornada ou Escala com período de vigência</p>
                    </div>
                  </div>

                  <form onSubmit={handleVincularRegime} className="space-y-4">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Colaborador *
                      </label>
                      {funcionariosEmpresa.length > 0 ? (
                        <select
                          value={funcionarioIdRegime}
                          onChange={(e) => setFuncionarioIdRegime(e.target.value)}
                          className="w-full h-10 rounded-xl border border-slate-700 bg-slate-950/60 px-3 text-sm text-slate-100 focus:outline-none"
                        >
                          <option value="">Selecione um colaborador...</option>
                          {funcionariosEmpresa.map((f) => (
                            <option key={f.id} value={f.id}>
                              {f.nomeCompleto} ({f.usuario || f.matricula})
                            </option>
                          ))}
                        </select>
                      ) : (
                        <input
                          type="text"
                          required
                          value={funcionarioIdRegime}
                          onChange={(e) => setFuncionarioIdRegime(e.target.value)}
                          placeholder="UUID do Colaborador"
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3.5 py-2 text-sm text-slate-100 font-mono"
                        />
                      )}
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Tipo de Regime *
                        </label>
                        <select
                          value={tipoRegimeSelecionado}
                          onChange={(e) => setTipoRegimeSelecionado(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        >
                          <option value="JORNADA">JORNADA (Semanal)</option>
                          <option value="ESCALA">ESCALA (Cíclica)</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          {tipoRegimeSelecionado === 'JORNADA' ? 'Jornada *' : 'Escala *'}
                        </label>
                        {tipoRegimeSelecionado === 'JORNADA' ? (
                          <select
                            value={jornadaIdSelecionada}
                            onChange={(e) => setJornadaIdSelecionada(e.target.value)}
                            className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                          >
                            <option value="">Selecione uma Jornada...</option>
                            {jornadas.map((j) => (
                              <option key={j.id} value={j.id}>
                                {j.nome}
                              </option>
                            ))}
                          </select>
                        ) : (
                          <select
                            value={escalaIdSelecionada}
                            onChange={(e) => setEscalaIdSelecionada(e.target.value)}
                            className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                          >
                            <option value="">Selecione uma Escala...</option>
                            {escalas.map((esc) => (
                              <option key={esc.id} value={esc.id}>
                                {esc.nome}
                              </option>
                            ))}
                          </select>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Início da Vigência *
                        </label>
                        <input
                          type="date"
                          required
                          value={dataInicioVigenciaRegime}
                          onChange={(e) => setDataInicioVigenciaRegime(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                          Fim da Vigência
                        </label>
                        <input
                          type="date"
                          value={dataFimVigenciaRegime}
                          onChange={(e) => setDataFimVigenciaRegime(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <button
                      type="submit"
                      disabled={submittingRegime}
                      className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold text-white shadow-lg shadow-emerald-600/25 transition-all cursor-pointer disabled:opacity-50"
                    >
                      {submittingRegime ? (
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      ) : (
                        <Check size={15} />
                      )}
                      <span>Vincular Regime ao Colaborador</span>
                    </button>
                  </form>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Conteúdo da Aba Importação Posicional AFD e Histórico (T09) */}
        {(abaAtiva === 'importacao' || abaAtiva === 'historico-importacao') && (
          <ImportacaoPosicionalSection
            equipamentos={equipamentos}
            funcionarios={funcionariosEmpresa}
            modoInicial={abaAtiva === 'historico-importacao' ? 'historico' : 'wizard'}
            onIrParaEquipamentos={() => setAbaAtiva('equipamentos')}
            onIrParaBatidasOrfas={() => {
              carregarContagemOrfas();
              setAbaAtiva('batidas-orfas');
            }}
            onImportacaoConcluida={() => {
              carregarEquipamentos();
              carregarContagemOrfas();
            }}
          />
        )}

        {abaAtiva === 'batidas-orfas' && (
          <BatidasOrfasSection
            funcionarios={funcionariosEmpresa}
            onVinculoConcluido={() => {
              carregarContagemOrfas();
            }}
            onIrParaImportacao={() => setAbaAtiva('importacao')}
          />
        )}

        {abaAtiva === 'inconsistencias' && (
          <FilaInconsistenciasSection
            funcionarios={funcionariosEmpresa}
            equipamentos={equipamentos}
            onAtualizado={() => {
              carregarContagemOrfas();
            }}
          />
        )}

        {abaAtiva === 'apuracao' && (
          <div className="space-y-6">
            <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <UserCheck className="w-5 h-5 text-indigo-400" />
                  Selecionar Colaborador para Apuração
                </h3>
                <p className="text-xs text-slate-400">
                  Selecione um colaborador da empresa para analisar pares, tolerâncias CLT, faixas de HE e espelho do mês.
                </p>
              </div>

              <div className="min-w-[280px]">
                <select
                  value={colaboradorApuracaoId}
                  onChange={(e) => setColaboradorApuracaoId(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 text-white text-sm rounded-xl px-3.5 py-2.5 focus:border-indigo-500 focus:outline-none"
                >
                  <option value="">Selecione um colaborador...</option>
                  {funcionariosEmpresa.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.nomeCompleto} ({f.matricula})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {colaboradorApuracaoId ? (
              <ApuracaoEspelhoSection
                funcionarioId={colaboradorApuracaoId}
                podeRecalcular={true}
              />
            ) : (
              <div className="rounded-2xl bg-slate-900/40 border border-dashed border-slate-800 p-12 text-center text-slate-500 text-sm">
                Selecione um colaborador no menu acima para carregar o espelho de ponto e calcular o período.
              </div>
            )}
          </div>
        )}

        {abaAtiva === 'ocorrencias' && (
          <OcorrenciasSection funcionarios={funcionariosEmpresa} />
        )}

        {abaAtiva === 'banco-horas' && (
          <BancoHorasSection
            funcionarios={funcionariosEmpresa}
            podeRegistrarCompensacao={true}
            exibirVisaoEquipe={true}
          />
        )}

        </div>

        {abaAtiva === 'fechamento' && <FechamentoPeriodoSection />}
        {abaAtiva === 'relatorios' && <RelatoriosSection />}
        {abaAtiva === 'folha' && <ExportacaoFolhaSection />}
      </div>
    </AppShell>
  );
}

export default RhDashboard;
