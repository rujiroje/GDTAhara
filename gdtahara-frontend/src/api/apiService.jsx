// =================================================================
// File: src/api/apiService.js (ไฟล์ใหม่)
// ตั้งค่าการเชื่อมต่อ API กลางด้วย Axios
// =================================================================
import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

// สร้าง instance ของ axios
export const api = axios.create({
    baseURL: API_URL,
    withCredentials: true, // ส่ง Credentials (เช่น Cookies) ไปกับทุก Request
});

// สร้าง Interceptor เพื่อแนบ Token ไปกับทุก Request
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});
