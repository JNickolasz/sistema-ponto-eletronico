import api, { API_BASE } from './api';

const BACKEND_ROOT = API_BASE.replace(/\/api\/v1\/?$/, '');

async function fetchAuth(path) {
  const token = localStorage.getItem('token');
  const res = await fetch(`${BACKEND_ROOT}${path}`, {
    headers: token && token !== 'admin-session-token' ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    const contentType = res.headers.get('content-type') || '';
    let errorMessage = '';
    let data = null;
    if (contentType.includes('application/json')) {
      data = await res.json().catch(() => null);
      errorMessage = data?.message || data?.mensagem || data?.error || '';
    } else {
      errorMessage = (await res.text().catch(() => '')).trim();
    }

    if (
      res.status === 401 &&
      token &&
      token !== 'admin-session-token' &&
      typeof window !== 'undefined'
    ) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(
        new CustomEvent('auth:session-revoked', {
          detail: { message: errorMessage || 'Sua sessão expirou. Faça login novamente.' },
        })
      );
    }

    throw new Error(errorMessage || 'Falha ao consultar relatório.');
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
