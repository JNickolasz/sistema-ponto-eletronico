import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import ImportacaoPosicionalSection from '../components/ImportacaoPosicionalSection';
import rhService from '../services/rhService';
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
} from 'lucide-react';
import { FormControl, Select, MenuItem } from '@mui/material';
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
  const [adminKey, setAdminKey] = useState('admin123');
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
  const [anoCalendario, setAnoCalendario] = useState(new Date().getFullYear());
  const [ufFiltro, setUfFiltro] = useState('SP');
  const [municipioFiltro, setMunicipioFiltro] = useState('São Paulo');

  // Formulário Feriado
  const [descricaoFeriado, setDescricaoFeriado] = useState('');
  const [dataFeriado, setDataFeriado] = useState(new Date().toISOString().split('T')[0]);
  const [alcanceFeriado, setAlcanceFeriado] = useState('NACIONAL');
  const [ufFeriado, setUfFeriado] = useState('SP');
  const [municipioFeriado, setMunicipioFeriado] = useState('');
  const [submittingFeriado, setSubmittingFeriado] = useState(false);

  // Formulário Ponto Facultativo
  const [descricaoPF, setDescricaoPF] = useState('');
  const [dataPF, setDataPF] = useState(new Date().toISOString().split('T')[0]);
  const [alcancePF, setAlcancePF] = useState('NACIONAL');
  const [ufPF, setUfPF] = useState('SP');
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
      localStorage.setItem('pontoCerto_usuarios_criados', JSON.stringify(novaLista));

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

  const carregarCalendarioFeriados = async () => {
    try {
      setLoadingFeriados(true);
      const lista = await feriadoService.buscarCalendarioFeriados({
        ano: anoCalendario,
        uf: ufFiltro,
        municipio: municipioFiltro,
        empresaId: empresaIdSelecionada || undefined,
      });
      setFeriadosList(Array.isArray(lista) ? lista : []);
    } catch (err) {
      console.error('Erro ao buscar feriados:', err);
    } finally {
      setLoadingFeriados(false);
    }
  };

  const carregarCalendarioPF = async () => {
    try {
      setLoadingPontosFacultativos(true);
      const lista = await feriadoService.buscarCalendarioPontosFacultativos({
        ano: anoCalendario,
        uf: ufFiltro,
        municipio: municipioFiltro,
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

        {/* Backend Response Card (GET /api/v1/rh/painel) */}
        <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-6 backdrop-blur-xl">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Terminal size={18} className="text-purple-400" />
              <h2 className="text-base font-semibold text-slate-100">
                Resposta do Endpoint: <span className="font-mono text-purple-300">GET /api/v1/rh/painel</span>
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
            onClick={() => setAbaAtiva('equipamentos')}
            className={`pb-3.5 px-1 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
              abaAtiva === 'equipamentos'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <HardDrive size={18} />
            <span>Equipamentos (T02)</span>
            {equipamentos.length > 0 && (
              <span className="px-2 py-0.5 text-xs rounded-full bg-indigo-500/20 text-indigo-300 font-mono">
                {equipamentos.length}
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
          <button
            type="button"
            onClick={() => setAbaAtiva('jornadas')}
            className={`pb-3.5 px-1 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
              abaAtiva === 'jornadas'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Calendar size={18} />
            <span>Jornadas & Escalas (T04)</span>
            {(jornadas.length > 0 || escalas.length > 0) && (
              <span className="px-2 py-0.5 text-xs rounded-full bg-emerald-500/20 text-emerald-300 font-mono">
                {jornadas.length + escalas.length}
              </span>
            )}
          </button>
          <button
            type="button"
            onClick={() => {
              setAbaAtiva('feriados');
              carregarCalendarioFeriados();
              carregarCalendarioPF();
            }}
            className={`pb-3.5 px-1 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
              abaAtiva === 'feriados'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <CalendarCheck size={18} />
            <span>Feriados & Facultativos (T05)</span>
            {(feriadosList.length > 0 || pontosFacultativosList.length > 0) && (
              <span className="px-2 py-0.5 text-xs rounded-full bg-amber-500/20 text-amber-300 font-mono">
                {feriadosList.length + pontosFacultativosList.length}
              </span>
            )}
          </button>
          <button
            type="button"
            onClick={() => setAbaAtiva('importacao')}
            className={`pb-3.5 px-1 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
              abaAtiva === 'importacao'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Upload size={18} />
            <span>Importação AFD (T09)</span>
          </button>
          <button
            type="button"
            onClick={() => setAbaAtiva('historico-importacao')}
            className={`pb-3.5 px-1 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
              abaAtiva === 'historico-importacao'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <History size={18} />
            <span>Histórico de Importações</span>
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
                          slotProps: {
                            paper: {
                              sx: {
                                bgcolor: '#0f172a',
                                color: '#f8fafc',
                                border: '1px solid #334155',
                                borderRadius: '0.75rem',
                              },
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
                <div className="font-semibold text-indigo-200 flex items-center justify-between text-sm">
                  <span className="flex items-center gap-1.5">
                    <Briefcase size={16} /> Tarefa T02 - Papel dos Locais:
                  </span>
                  <button
                    type="button"
                    onClick={() => setAbaAtiva('equipamentos')}
                    className="text-xs text-indigo-400 hover:text-indigo-300 underline font-normal cursor-pointer"
                  >
                    Ir para Equipamentos &rarr;
                  </button>
                </div>
                <p className="text-slate-300 leading-relaxed">
                  Toda marcação de ponto deve ter uma origem física conhecida. Cada local cadastrado (Matriz, Filial, Obra) receberá seus <strong>Equipamentos</strong> (Relógio AFD ou Estação Web) para registrar as batidas.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Conteúdo da Aba Equipamentos (T02 - Issues #16, #17, #18) */}
        {abaAtiva === 'equipamentos' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            {/* Coluna do Formulário de Equipamento */}
            <div className="lg:col-span-7">
              <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                      <HardDrive size={20} />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white">Cadastrar Equipamento de Registro</h3>
                      <p className="text-xs text-slate-400">
                        Dispara requisição para <span className="font-mono text-indigo-300">POST /api/v1/equipamentos</span>
                      </p>
                    </div>
                  </div>

                  {/* Exemplos Rápidos */}
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <button
                      type="button"
                      onClick={() => handlePreencherExemploEquipamento('RELOGIO')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer flex items-center gap-1"
                    >
                      <Clock size={12} /> + Exemplo Relógio
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePreencherExemploEquipamento('ESTACAO')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer flex items-center gap-1"
                    >
                      <Monitor size={12} /> + Exemplo Estação
                    </button>
                  </div>
                </div>

                <form onSubmit={handleCadastrarEquipamento} className="space-y-4">
                  {/* Select do Local de Trabalho */}
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                        Local de Trabalho Vinculado *
                      </label>
                      {locaisTrabalho.length === 0 && (
                        <button
                          type="button"
                          onClick={() => setAbaAtiva('locais')}
                          className="text-xs text-indigo-400 hover:text-indigo-300 underline cursor-pointer"
                        >
                          Cadastre um local primeiro
                        </button>
                      )}
                    </div>

                    <FormControl fullWidth size="small">
                      <Select
                        displayEmpty
                        value={localTrabalhoIdEquipamento}
                        onChange={(e) => setLocalTrabalhoIdEquipamento(e.target.value)}
                        renderValue={(selected) => {
                          if (!selected) {
                            return <span className="text-slate-500 text-sm">Selecione o local de instalação...</span>;
                          }
                          const found = locaisTrabalho.find((l) => l.id === selected);
                          return found
                            ? `${found.nome} (${found.razaoSocialEmpresa || 'Empresa'})`
                            : selected;
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
                          slotProps: {
                            paper: {
                              sx: {
                                bgcolor: '#0f172a',
                                color: '#f8fafc',
                                border: '1px solid #334155',
                                borderRadius: '0.75rem',
                              },
                            },
                          },
                        }}
                      >
                        {locaisTrabalho.length === 0 ? (
                          <MenuItem disabled value="">
                            Nenhum local cadastrado. Acesse a aba "Locais de Trabalho" para criar um.
                          </MenuItem>
                        ) : (
                          locaisTrabalho.map((local) => (
                            <MenuItem key={local.id} value={local.id} sx={{ '&:hover': { bgcolor: '#1e293b' } }}>
                              {local.nome} — {local.razaoSocialEmpresa || 'Empresa'} ({local.municipio || ''}/{local.uf || ''})
                            </MenuItem>
                          ))
                        )}
                      </Select>
                    </FormControl>
                  </div>

                  {/* Seleção do Tipo de Equipamento (Cards clicáveis) */}
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                      Tipo de Equipamento *
                    </label>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <button
                        type="button"
                        onClick={() => setTipoEquipamento('RELOGIO')}
                        className={`p-3.5 rounded-xl border text-left transition-all cursor-pointer flex items-start gap-3 ${
                          tipoEquipamento === 'RELOGIO'
                            ? 'bg-indigo-950/60 border-indigo-500 ring-1 ring-indigo-500/50'
                            : 'bg-slate-950/50 border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div
                          className={`p-2 rounded-lg ${
                            tipoEquipamento === 'RELOGIO'
                              ? 'bg-indigo-500 text-white'
                              : 'bg-slate-800 text-slate-400'
                          }`}
                        >
                          <Clock size={18} />
                        </div>
                        <div>
                          <div className="font-semibold text-sm text-slate-100 flex items-center gap-1.5">
                            Relógio Físico (AFD)
                            {tipoEquipamento === 'RELOGIO' && (
                              <span className="text-[10px] bg-indigo-500/20 text-indigo-300 px-1.5 py-0.5 rounded">
                                Selecionado
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-slate-400 mt-0.5">
                            Aparelho de parede. Exige número de fabricação único por empresa.
                          </p>
                        </div>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setTipoEquipamento('ESTACAO');
                          setNumFabricacaoEquipamento('');
                        }}
                        className={`p-3.5 rounded-xl border text-left transition-all cursor-pointer flex items-start gap-3 ${
                          tipoEquipamento === 'ESTACAO'
                            ? 'bg-sky-950/60 border-sky-500 ring-1 ring-sky-500/50'
                            : 'bg-slate-950/50 border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div
                          className={`p-2 rounded-lg ${
                            tipoEquipamento === 'ESTACAO'
                              ? 'bg-sky-500 text-white'
                              : 'bg-slate-800 text-slate-400'
                          }`}
                        >
                          <Monitor size={18} />
                        </div>
                        <div>
                          <div className="font-semibold text-sm text-slate-100 flex items-center gap-1.5">
                            Estação Web (Tablet/PC)
                            {tipoEquipamento === 'ESTACAO' && (
                              <span className="text-[10px] bg-sky-500/20 text-sky-300 px-1.5 py-0.5 rounded">
                                Selecionado
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-slate-400 mt-0.5">
                            Terminal de corredor ou browser. Serial de fabricação nulo (Issue #16).
                          </p>
                        </div>
                      </button>
                    </div>
                  </div>

                  {/* Código Único do Equipamento */}
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                        Código do Equipamento
                      </label>
                      <span className="text-[11px] text-slate-400">
                        Opcional (gerado automaticamente se vazio)
                      </span>
                    </div>
                    <input
                      type="text"
                      value={codigoEquipamento}
                      onChange={(e) => setCodigoEquipamento(e.target.value)}
                      placeholder="Ex: EQ-01, REP-10023, EST-05"
                      className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950/60 border border-slate-700/80 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 font-mono"
                    />
                  </div>

                  {/* Identificação do Equipamento */}
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Identificação do Equipamento *
                    </label>
                    <input
                      type="text"
                      required
                      value={identificacaoEquipamento}
                      onChange={(e) => setIdentificacaoEquipamento(e.target.value)}
                      placeholder="Ex: Catraca Entrada Principal, REP-001, Tablet Recepção B"
                      className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950/60 border border-slate-700/80 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>

                  {/* Número de Fabricação */}
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                        Número de Fabricação (Serial) {tipoEquipamento === 'RELOGIO' ? '*' : ''}
                      </label>
                      <span className="text-[11px] text-slate-400">
                        {tipoEquipamento === 'RELOGIO' ? (
                          <span className="text-amber-400/90 font-medium">Obrigatório e único por empresa</span>
                        ) : (
                          <span className="text-slate-500">Ignorado para Estação Web (Issue #16)</span>
                        )}
                      </span>
                    </div>
                    <input
                      type="text"
                      disabled={tipoEquipamento === 'ESTACAO'}
                      required={tipoEquipamento === 'RELOGIO'}
                      value={tipoEquipamento === 'ESTACAO' ? '' : numFabricacaoEquipamento}
                      onChange={(e) => setNumFabricacaoEquipamento(e.target.value)}
                      placeholder={
                        tipoEquipamento === 'RELOGIO'
                          ? 'Ex: REP-987654321, 000140028900012'
                          : 'Não aplicável para Estação Web (será salvo como null)'
                      }
                      className={`w-full px-3.5 py-2.5 rounded-xl border text-sm text-slate-100 focus:outline-none transition-all ${
                        tipoEquipamento === 'ESTACAO'
                          ? 'bg-slate-950/30 border-slate-800/60 text-slate-500 cursor-not-allowed italic'
                          : 'bg-slate-950/60 border-slate-700/80 placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500'
                      }`}
                    />
                  </div>

                  {/* Botão de Envio */}
                  <div className="pt-2">
                    <button
                      type="submit"
                      disabled={submittingEquipamento || locaisTrabalho.length === 0}
                      className="w-full flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 active:bg-indigo-700 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {submittingEquipamento ? (
                        <>
                          <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                          <span>Cadastrando Equipamento...</span>
                        </>
                      ) : (
                        <>
                          <Plus size={16} />
                          <span>Cadastrar Equipamento</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>

            {/* Coluna da Listagem de Equipamentos Cadastrados */}
            <div className="lg:col-span-5 space-y-4">
              <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <HardDrive size={18} className="text-indigo-400" />
                    <h3 className="text-base font-bold text-white">Equipamentos Cadastrados</h3>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => carregarEquipamentos()}
                      disabled={loadingEquipamentos}
                      title="Recarregar equipamentos"
                      className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
                    >
                      <RefreshCw size={13} className={loadingEquipamentos ? 'animate-spin' : ''} />
                    </button>
                    <span className="text-xs text-slate-400 bg-slate-800 px-2.5 py-1 rounded-full font-mono">
                      Total: {equipamentos.length}
                    </span>
                  </div>
                </div>

                {/* Filtro por Local */}
                {locaisTrabalho.length > 0 && (
                  <div className="flex items-center gap-2 mb-3 px-1">
                    <Filter size={13} className="text-slate-500 shrink-0" />
                    <select
                      value={filtroLocalEquipamento}
                      onChange={(e) => setFiltroLocalEquipamento(e.target.value)}
                      className="w-full text-xs bg-slate-950/80 border border-slate-800 rounded-lg px-2.5 py-1.5 text-slate-300 focus:outline-none focus:border-indigo-500 cursor-pointer"
                    >
                      <option value="">Todos os locais de trabalho</option>
                      {locaisTrabalho.map((l) => (
                        <option key={l.id} value={l.id}>
                          {l.nome} ({l.razaoSocialEmpresa || 'Empresa'})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {loadingEquipamentos ? (
                  <div className="flex items-center justify-center gap-2 py-8 text-slate-400 text-sm">
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
                    <span>Carregando equipamentos...</span>
                  </div>
                ) : equipamentos.length === 0 ? (
                  <div className="text-center py-8 px-4 rounded-xl border border-dashed border-slate-800 text-slate-400 text-sm">
                    <HardDrive size={32} className="mx-auto mb-2 text-slate-600" />
                    <p>Nenhum equipamento cadastrado ainda.</p>
                    <p className="text-xs text-slate-500 mt-1">
                      Cadastre o primeiro relógio ou estação web usando o formulário ao lado!
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3 max-h-[520px] overflow-y-auto pr-1">
                    {(filtroLocalEquipamento
                      ? equipamentos.filter((eq) => eq.localTrabalhoId === filtroLocalEquipamento)
                      : equipamentos
                    ).map((eq) => {
                      const isAtivo = eq.status === 'ATIVO';
                      const isRelogio = eq.tipo === 'RELOGIO';

                      return (
                        <div
                          key={eq.id}
                          className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 hover:border-slate-700 transition-all space-y-2.5"
                        >
                          {/* Cabeçalho do Card */}
                          <div className="flex items-center justify-between gap-2">
                            {/* Badge Tipo */}
                            <span
                              className={`inline-flex items-center gap-1.5 text-[11px] font-semibold px-2 py-0.5 rounded-md ${
                                isRelogio
                                  ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                                  : 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                              }`}
                            >
                              {isRelogio ? <Clock size={12} /> : <Monitor size={12} />}
                              {isRelogio ? 'Relógio AFD' : 'Estação Web'}
                            </span>

                            {/* Badge Status */}
                            <span
                              className={`inline-flex items-center gap-1.5 text-[10px] font-bold px-2 py-0.5 rounded-full ${
                                isAtivo
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-slate-800 text-slate-400 border border-slate-700'
                              }`}
                            >
                              <span
                                className={`w-1.5 h-1.5 rounded-full ${
                                  isAtivo ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'
                                }`}
                              />
                              {eq.status}
                            </span>
                          </div>

                          {/* Identificação e Local */}
                          <div>
                            <h4 className="font-bold text-sm text-slate-100">{eq.identificacao}</h4>
                            <div className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                              <Building2 size={12} className="text-slate-500 shrink-0" />
                              <span className="truncate">
                                {eq.localTrabalhoNome || 'Local não identificado'}
                                {eq.empresaRazaoSocial ? ` • ${eq.empresaRazaoSocial}` : ''}
                              </span>
                            </div>
                          </div>

                          {/* Serial & Marcações */}
                          <div className="grid grid-cols-2 gap-2 pt-1">
                            <div className="p-2 rounded-lg bg-slate-900/90 border border-slate-800/80">
                              <span className="text-[10px] text-slate-500 block uppercase font-semibold">
                                Número Fabricação
                              </span>
                              <span className="text-xs font-mono font-medium text-slate-200 truncate flex items-center gap-1 mt-0.5">
                                <Hash size={11} className="text-slate-500 shrink-0" />
                                {eq.numFabricacao || '— (Nulo / Estação)'}
                              </span>
                            </div>

                            <div className="p-2 rounded-lg bg-slate-900/90 border border-slate-800/80">
                              <span className="text-[10px] text-slate-500 block uppercase font-semibold">
                                Marcações (Issue #18)
                              </span>
                              <span className="text-xs font-mono font-bold text-indigo-300 block mt-0.5">
                                {eq.totalMarcacoes ?? 0} marcações
                              </span>
                            </div>
                          </div>

                          {/* Ações (Desativação Lógica - Issue #17) */}
                          <div className="flex items-center justify-between pt-1 border-t border-slate-800/60 text-[11px]">
                            <span className="text-[10px] text-slate-500 font-mono">
                              ID: #{eq.id?.slice(0, 8)}...
                            </span>

                            <button
                              type="button"
                              onClick={() => handleAlternarStatusEquipamento(eq)}
                              className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg font-medium transition-all cursor-pointer ${
                                isAtivo
                                  ? 'bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/20'
                                  : 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/20'
                              }`}
                              title={
                                isAtivo
                                  ? 'Desativar equipamento sem apagar marcações (PATCH status=INATIVO)'
                                  : 'Reativar equipamento (PATCH status=ATIVO)'
                              }
                            >
                              <Power size={11} />
                              {isAtivo ? 'Desativar (Soft Delete)' : 'Reativar'}
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Card Explicativo das Regras da Tarefa T02 */}
              <div className="rounded-2xl bg-indigo-950/30 border border-indigo-900/40 p-5 text-xs text-indigo-300/90 space-y-2">
                <div className="font-semibold text-indigo-200 flex items-center gap-1.5 text-sm">
                  <Briefcase size={16} /> Tarefa T02 - Critérios Atendidos:
                </div>
                <ul className="list-disc list-inside space-y-1 text-slate-300 text-[11px] leading-relaxed">
                  <li>
                    <strong>Critério 4 (Issue #16):</strong> Relógio exige número de fabricação único por empresa; Estações Web ignoram e salvam como nulo.
                  </li>
                  <li>
                    <strong>Critério 5 (Issue #17):</strong> Equipamentos são desativados via <code>PATCH /status</code> sem exclusão física, preservando histórico de marcações.
                  </li>
                  <li>
                    <strong>Critério 6 (Issue #18):</strong> A listagem exibe o contador <code>totalMarcacoes</code> para cada equipamento cadastrado.
                  </li>
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* Conteúdo da Aba 2: Usuários e Acessos (T01) */}
        {abaAtiva === 'usuarios' && (
          <div className="space-y-8">
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
                        Dispara requisição real para <span className="font-mono text-indigo-300">POST /api/v1/rh/adicionar</span>
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

                  {/* Linha PIS/PASEP e CPF com geradores rápidos */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                          PIS / PASEP * (11 Dígitos)
                        </label>
                        <button
                          type="button"
                          onClick={() => setPisPasep(formatarPis(gerarPisValido()))}
                          className="text-[10px] text-amber-400 hover:text-amber-300 underline cursor-pointer"
                        >
                          Gerar Válido
                        </button>
                      </div>
                      <input
                        type="text"
                        required
                        value={pisPasep}
                        onChange={(e) => setPisPasep(e.target.value)}
                        placeholder="000.00000.00-0"
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 font-mono placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                          CPF * (Receita Federal)
                        </label>
                        <button
                          type="button"
                          onClick={() => setCpf(formatarCpf(gerarCpfValido()))}
                          className="text-[10px] text-amber-400 hover:text-amber-300 underline cursor-pointer"
                        >
                          Gerar Válido
                        </button>
                      </div>
                      <input
                        type="text"
                        required
                        value={cpf}
                        onChange={(e) => setCpf(e.target.value)}
                        placeholder="000.000.000-00"
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 font-mono placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      />
                    </div>
                  </div>

                  {/* Linha Matrícula e Data de Admissão */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                          Matrícula Única *
                        </label>
                        <button
                          type="button"
                          onClick={() => setMatricula(`MAT-${Math.floor(1000 + Math.random() * 9000)}`)}
                          className="text-[10px] text-indigo-400 hover:text-indigo-300 underline cursor-pointer"
                        >
                          Gerar Código
                        </button>
                      </div>
                      <input
                        type="text"
                        required
                        value={matricula}
                        onChange={(e) => setMatricula(e.target.value)}
                        placeholder="Ex: MAT-1024"
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 font-mono placeholder-slate-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Data de Admissão
                      </label>
                      <input
                        type="date"
                        value={dataAdmissao}
                        onChange={(e) => setDataAdmissao(e.target.value)}
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-sm text-slate-100 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                      />
                    </div>
                  </div>

                  {/* Vínculo de Local de Trabalho e Gestor */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Local de Trabalho Vinculado
                      </label>
                      <select
                        value={localTrabalhoIdFuncionario}
                        onChange={(e) => setLocalTrabalhoIdFuncionario(e.target.value)}
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                      >
                        <option value="">Nenhum local selecionado</option>
                        {locaisTrabalho.map((loc) => (
                          <option key={loc.id} value={loc.id}>
                            {loc.nome} ({loc.municipio || 'S/M'} - {loc.uf || 'S/UF'})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                        Gestor Direto (Opcional)
                      </label>
                      <select
                        value={gestorIdFuncionario}
                        onChange={(e) => setGestorIdFuncionario(e.target.value)}
                        className="w-full rounded-xl border border-slate-700/80 bg-slate-950/60 px-3.5 py-2.5 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                      >
                        <option value="">Sem gestor imediato</option>
                        {funcionariosEmpresa
                          .filter((f) => f.perfilAcesso === 'GESTOR')
                          .map((g) => (
                            <option key={g.id} value={g.id}>
                              {g.nomeCompleto} ({g.usuario})
                            </option>
                          ))}
                      </select>
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

          {/* Seção Completa: Colaboradores da Empresa Cadastrados no Banco */}
          <div className="mt-8 rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl backdrop-blur-xl">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                  <Briefcase size={20} />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white flex items-center gap-2">
                    Colaboradores Cadastrados na Empresa
                    <span className="text-xs font-mono px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
                      {funcionariosEmpresa.length}
                    </span>
                  </h3>
                  <p className="text-xs text-slate-400">
                    Gestão corporativa, desligamento e exclusão de colaboradores
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <div className="relative">
                  <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={termoBuscaFuncionario}
                    onChange={(e) => setTermoBuscaFuncionario(e.target.value)}
                    placeholder="Filtrar por nome, usuário, matrícula..."
                    className="rounded-xl border border-slate-700 bg-slate-950/80 pl-9 pr-3 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:border-indigo-500 focus:outline-none w-64"
                  />
                </div>
                <button
                  type="button"
                  onClick={() => carregarFuncionarios(empresaIdSelecionada || (empresas[0] && empresas[0].id))}
                  disabled={loadingFuncionarios}
                  className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors cursor-pointer"
                  title="Recarregar colaboradores"
                >
                  <RefreshCw size={14} className={loadingFuncionarios ? 'animate-spin' : ''} />
                </button>
              </div>
            </div>

            {loadingFuncionarios ? (
              <div className="flex items-center justify-center py-12 text-slate-400 text-xs gap-2">
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
                Carregando colaboradores da empresa...
              </div>
            ) : funcionariosEmpresa.length === 0 ? (
              <div className="text-center py-10 text-slate-500 text-xs">
                Nenhum colaborador encontrado para a empresa selecionada. Cadastre o primeiro acima!
              </div>
            ) : (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950/60 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800">
                    <tr>
                      <th className="py-3 px-3">Colaborador</th>
                      <th className="py-3 px-3">Perfil</th>
                      <th className="py-3 px-3">CPF / PIS-PASEP</th>
                      <th className="py-3 px-3">Matrícula</th>
                      <th className="py-3 px-3">Admissão</th>
                      <th className="py-3 px-3">Status</th>
                      <th className="py-3 px-3 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {funcionariosEmpresa
                      .filter((f) => {
                        if (!termoBuscaFuncionario.trim()) return true;
                        const t = termoBuscaFuncionario.toLowerCase();
                        return (
                          f.nomeCompleto?.toLowerCase().includes(t) ||
                          f.usuario?.toLowerCase().includes(t) ||
                          f.matricula?.toLowerCase().includes(t) ||
                          f.cpf?.includes(t) ||
                          f.pis_pasep?.includes(t)
                        );
                      })
                      .map((f) => (
                        <tr key={f.id} className="hover:bg-slate-800/30 transition-colors">
                          <td className="py-3 px-3">
                            <div className="font-semibold text-white">{f.nomeCompleto}</div>
                            <div className="text-[11px] text-slate-400 font-mono">@{f.usuario}</div>
                          </td>
                          <td className="py-3 px-3">
                            <span
                              className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                                f.perfilAcesso === 'COLABORADOR'
                                  ? 'bg-emerald-500/20 text-emerald-400'
                                  : f.perfilAcesso === 'GESTOR'
                                  ? 'bg-blue-500/20 text-blue-400'
                                  : 'bg-purple-500/20 text-purple-400'
                              }`}
                            >
                              {f.perfilAcesso}
                            </span>
                          </td>
                          <td className="py-3 px-3 font-mono text-[11px] text-slate-400">
                            <div>CPF: {formatarCpf(f.cpf) || f.cpf || '—'}</div>
                            <div>PIS: {formatarPis(f.pis_pasep) || f.pis_pasep || '—'}</div>
                          </td>
                          <td className="py-3 px-3 font-mono text-indigo-300 font-medium">
                            {f.matricula || '—'}
                          </td>
                          <td className="py-3 px-3 text-slate-400">
                            {f.dataAdmissao || '—'}
                          </td>
                          <td className="py-3 px-3">
                            {f.dataDesligamento ? (
                              <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-red-950/60 text-red-400 border border-red-800/40">
                                Desligado ({f.dataDesligamento})
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-950/60 text-emerald-400 border border-emerald-800/40">
                                Ativo
                              </span>
                            )}
                          </td>
                          <td className="py-3 px-3 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              {!f.dataDesligamento && (
                                <button
                                  type="button"
                                  onClick={() => handleAbrirDesligamento(f)}
                                  className="p-1.5 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 transition-colors cursor-pointer"
                                  title="Registrar desligamento do colaborador"
                                >
                                  <UserMinus size={13} />
                                </button>
                              )}
                              <button
                                type="button"
                                onClick={() => handleExcluirFuncionario(f)}
                                disabled={excluindoFuncionarioId === f.id}
                                className="p-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/30 transition-colors cursor-pointer disabled:opacity-50"
                                title="Excluir colaborador (sem marcações)"
                              >
                                {excluindoFuncionarioId === f.id ? (
                                  <div className="h-3 w-3 animate-spin rounded-full border border-red-400 border-t-transparent" />
                                ) : (
                                  <Trash2 size={13} />
                                )}
                              </button>
                              <button
                                type="button"
                                onClick={() => handleTestarLoginNovo(f)}
                                className="p-1.5 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 transition-colors cursor-pointer"
                                title="Testar login como este colaborador"
                              >
                                <KeyRound size={13} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Modal de Desligamento de Colaborador */}
          {modalDesligamentoAberto && funcionarioParaDesligar && (
            <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
              <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                  <div className="flex items-center gap-2">
                    <UserMinus size={20} className="text-amber-400" />
                    <h3 className="text-base font-bold text-white">Desligar Colaborador</h3>
                  </div>
                  <button
                    type="button"
                    onClick={() => setModalDesligamentoAberto(false)}
                    className="text-slate-400 hover:text-white"
                  >
                    <X size={18} />
                  </button>
                </div>

                <p className="text-xs text-slate-300">
                  Confirmar desligamento de <strong>{funcionarioParaDesligar.nomeCompleto}</strong> (login: <code className="text-indigo-300">{funcionarioParaDesligar.usuario}</code>). Após o desligamento, novas batidas de ponto posteriores a esta data serão impedidas.
                </p>

                <form onSubmit={handleConfirmarDesligamento} className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
                      Data de Desligamento *
                    </label>
                    <input
                      type="date"
                      required
                      value={dataDesligamentoInput}
                      onChange={(e) => setDataDesligamentoInput(e.target.value)}
                      className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2.5 text-sm text-slate-100 focus:border-indigo-500 focus:outline-none"
                    />
                  </div>

                  <div className="flex gap-2 justify-end pt-2">
                    <button
                      type="button"
                      onClick={() => setModalDesligamentoAberto(false)}
                      className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-300 cursor-pointer"
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      disabled={submittingDesligar}
                      className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-500 text-xs font-semibold text-white shadow-lg shadow-amber-600/30 cursor-pointer disabled:opacity-50"
                    >
                      {submittingDesligar ? 'Desligando...' : 'Confirmar Desligamento'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

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
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wider uppercase bg-amber-500/20 text-amber-300 border border-amber-500/30">
                      Tarefa T05
                    </span>
                  </div>
                  <p className="text-xs text-slate-400">
                    Cadastro e consulta de feriados (Nacional, Estadual, Municipal) e pontos facultativos com impacto no cálculo da jornada
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
                    <button
                      type="button"
                      onClick={() => {
                        setDescricaoFeriado('Dia da Proclamação da República');
                        setDataFeriado('2026-11-15');
                        setAlcanceFeriado('NACIONAL');
                        toast('Exemplo de Feriado preenchido!', { icon: '🇧🇷' });
                      }}
                      className="text-[10px] px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300"
                    >
                      Exemplo Nacional
                    </button>
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
                          value={alcanceFeriado}
                          onChange={(e) => setAlcanceFeriado(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                        >
                          <option value="NACIONAL">NACIONAL (Geral)</option>
                          <option value="ESTADUAL">ESTADUAL (Exige UF)</option>
                          <option value="MUNICIPAL">MUNICIPAL (UF + Município)</option>
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
                            placeholder="SP"
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
                              placeholder="São Paulo"
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
                    <button
                      type="button"
                      onClick={() => {
                        setDescricaoPF('Quarta-feira de Cinzas (até 14h)');
                        setDataPF('2026-02-18');
                        setAlcancePF('NACIONAL');
                        toast('Exemplo de Ponto Facultativo preenchido!', { icon: '✨' });
                      }}
                      className="text-[10px] px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300"
                    >
                      Exemplo
                    </button>
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
                          value={alcancePF}
                          onChange={(e) => setAlcancePF(e.target.value)}
                          className="w-full rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-slate-100 focus:border-indigo-500 focus:outline-none"
                        >
                          <option value="NACIONAL">NACIONAL</option>
                          <option value="ESTADUAL">ESTADUAL</option>
                          <option value="MUNICIPAL">MUNICIPAL</option>
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
                            placeholder="SP"
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
                              placeholder="São Paulo"
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
                    <label className="block text-[11px] font-semibold text-slate-400 mb-1">Ano</label>
                    <input
                      type="number"
                      min="2020"
                      max="2035"
                      value={anoCalendario}
                      onChange={(e) => setAnoCalendario(Number(e.target.value))}
                      className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-400 mb-1">UF (Filtro)</label>
                    <input
                      type="text"
                      maxLength={2}
                      value={ufFiltro}
                      onChange={(e) => setUfFiltro(e.target.value.toUpperCase())}
                      placeholder="SP"
                      className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs text-white font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-400 mb-1">Município (Filtro)</label>
                    <input
                      type="text"
                      value={municipioFiltro}
                      onChange={(e) => setMunicipioFiltro(e.target.value)}
                      placeholder="São Paulo"
                      className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs text-white"
                    />
                  </div>
                  <div className="flex items-end">
                    <button
                      type="button"
                      onClick={() => {
                        carregarCalendarioFeriados();
                        carregarCalendarioPF();
                      }}
                      className="w-full py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white"
                    >
                      Filtrar
                    </button>
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
                            <div className="font-semibold text-white flex items-center gap-2">
                              <span>{f.descricao}</span>
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-amber-300 font-mono">
                                {f.alcance}
                              </span>
                              {f.uf && <span className="text-[10px] text-slate-400 font-mono">{f.uf}</span>}
                              {f.municipio && <span className="text-[10px] text-slate-400 font-mono">- {f.municipio}</span>}
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono mt-0.5">Data: {f.data}</div>
                          </div>
                          <div className="flex items-center gap-2">
                            {f.ativo ? (
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
                            <div className="font-semibold text-white flex items-center gap-2">
                              <span>{pf.descricao}</span>
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-indigo-300 font-mono">
                                {pf.alcance}
                              </span>
                              {pf.uf && <span className="text-[10px] text-slate-400 font-mono">{pf.uf}</span>}
                              {pf.municipio && <span className="text-[10px] text-slate-400 font-mono">- {pf.municipio}</span>}
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono mt-0.5">Data: {pf.data}</div>
                          </div>
                          <div className="flex items-center gap-2">
                            {pf.ativo ? (
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
                      <h3 className="text-lg font-bold text-white">Simulador de Horário Previsto do Dia</h3>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wider uppercase bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                        Critério Principal T04
                      </span>
                    </div>
                    <p className="text-xs text-slate-400">
                      Testa a rota <span className="font-mono text-indigo-300">GET /api/v1/regimes-trabalho/horario-previsto?funcionarioId=...&data=...</span>
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
                      <FormControl fullWidth size="small">
                        <Select
                          value={funcionarioIdConsulta}
                          onChange={(e) => {
                            setFuncionarioIdConsulta(e.target.value);
                            setFuncionarioIdRegime(e.target.value);
                          }}
                          sx={{
                            color: '#f8fafc',
                            backgroundColor: 'rgba(2, 6, 23, 0.6)',
                            borderRadius: '0.75rem',
                            '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(51, 65, 85, 0.8)' },
                            '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#6366f1' },
                            '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#6366f1' },
                            '.MuiSvgIcon-root': { color: '#94a3b8' },
                          }}
                        >
                          {funcionariosEmpresa.map((f) => (
                            <MenuItem key={f.id} value={f.id}>
                              {f.nomeCompleto} ({f.usuario || f.matricula}) - {f.perfilAcesso}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
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
                        <FormControl fullWidth size="small">
                          <Select
                            value={funcionarioIdRegime}
                            onChange={(e) => setFuncionarioIdRegime(e.target.value)}
                            sx={{
                              color: '#f8fafc',
                              backgroundColor: 'rgba(2, 6, 23, 0.6)',
                              borderRadius: '0.75rem',
                              '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(51, 65, 85, 0.8)' },
                              '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#6366f1' },
                              '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#6366f1' },
                              '.MuiSvgIcon-root': { color: '#94a3b8' },
                            }}
                          >
                            {funcionariosEmpresa.map((f) => (
                              <MenuItem key={f.id} value={f.id}>
                                {f.nomeCompleto} ({f.usuario || f.matricula})
                              </MenuItem>
                            ))}
                          </Select>
                        </FormControl>
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
            onImportacaoConcluida={() => {
              carregarEquipamentos();
            }}
          />
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
                      Dispara <span className="font-mono text-purple-300">POST /api/v1/admin/empresas</span> com <span className="font-mono text-purple-300">X-Admin-Key</span>
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
