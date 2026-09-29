import { request } from './api';

export const authService = {
  async register(name, email, password) {
    const data = await request('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password })
    });

    if (data?.token) {
      localStorage.setItem('medsafe_token', data.token);
      localStorage.setItem('medsafe_user', JSON.stringify({
        userId: data.userId,
        name: data.name,
        email: data.email
      }));
    }
    return data;
  },

  async login(email, password) {
    const data = await request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    if (data?.token) {
      localStorage.setItem('medsafe_token', data.token);
      localStorage.setItem('medsafe_user', JSON.stringify({
        userId: data.userId,
        name: data.name,
        email: data.email
      }));
    }
    return data;
  },

  logout() {
    localStorage.removeItem('medsafe_token');
    localStorage.removeItem('medsafe_user');
  },

  getCurrentUser() {
    try {
      const userStr = localStorage.getItem('medsafe_user');
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },

  isAuthenticated() {
    return !!localStorage.getItem('medsafe_token');
  }
};

export default authService;
