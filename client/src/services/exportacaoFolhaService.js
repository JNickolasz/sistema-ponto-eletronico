import { API_BASE } from './api';

const BACKEND_ROOT = API_BASE.replace(/\/api\/v1\/?$/, '');

async function fetchAuth(path) {
  const token = localStorage.getItem('token');
  const res = await fetch(`${BACKEND_ROOT}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    const errText = await res.text().catch(() => '');
    let msg = errText;
    try {
      const parsed = JSON.parse(errText);
      msg = parsed?.message || parsed?.erro || errText;
    } catch {}
    throw new Error(msg || 'Erro na operação de exportação de folha.');
  }
  return res;
}

export const exportacaoFolhaService = {
  async consultarPrevia(competencia) {
    const res = await fetchAuth(`/api/folha/previa?competencia=${encodeURIComponent(competencia)}`);
    return await res.json();
  },

  async exportarCsv(competencia) {
    const res = await fetchAuth(`/api/folha/exportar?competencia=${encodeURIComponent(competencia)}`);
    const sha256 = res.headers.get('X-File-SHA256');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `folha-${competencia}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    return { sha256 };
  },

  async listarHistorico(competencia = '') {
    const q = competencia ? `?competencia=${encodeURIComponent(competencia)}` : '';
    const res = await fetchAuth(`/api/folha/historico${q}`);
    return await res.json();
  },
};

export default exportacaoFolhaService;
