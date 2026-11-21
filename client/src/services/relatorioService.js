import { API_BASE } from './api';

const BACKEND_ROOT = API_BASE.replace(/\/api\/v1\/?$/, '');

async function fetchAuth(path) {
  const token = localStorage.getItem('token');
  const res = await fetch(`${BACKEND_ROOT}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || 'Falha ao consultar relatório.');
  }
  return res;
}

function buildQuery(params = {}) {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && String(v).trim() !== '') {
      qs.set(k, String(v).trim());
    }
  });
  const s = qs.toString();
  return s ? `?${s}` : '';
}

export const relatorioService = {
  async consultar(tipo, params = {}) {
    const res = await fetchAuth(`/api/relatorios/${tipo}${buildQuery(params)}`);
    return await res.json();
  },

  async baixarCsv(tipo, params = {}) {
    const res = await fetchAuth(`/api/relatorios/${tipo}/csv${buildQuery(params)}`);
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `relatorio-${tipo}-${params.competencia || 'periodo'}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};

export default relatorioService;
