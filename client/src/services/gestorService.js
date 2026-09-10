import api from './api';

export const gestorService = {
  /**
   * Consulta os dados do painel do Gestor
   */
  async getPainel() {
    return await api.get('/gestor/painel');
  },
};

export default gestorService;
