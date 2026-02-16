import axios from 'axios';

// Create axios instance with base URL from environment
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  responseType: 'json',
  // Let axios handle JSON parsing with proper UTF-8 decoding
});

export default api;
