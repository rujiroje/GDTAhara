import axios from 'axios';

const axiosInstance = axios.create({
    baseURL: 'http://localhost:8080/api', // Backend API URL
    withCredentials: true, // ส่ง Credentials (เช่น Cookies) ไปกับทุก Request
    headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
    }
});

axiosInstance.interceptors.request.use(
    (config) => {
        // Allow caller to opt-out of auth handling per request
        const skipAuth = config.headers && (config.headers['X-Skip-Auth'] === true || config.headers['X-Skip-Auth'] === 'true');

        // Determine preferred language from localStorage or browser
        let lang = localStorage.getItem('lang');
        if (!lang) {
            const navLang = (navigator.language || navigator.userLanguage || 'th').toLowerCase();
            lang = navLang.startsWith('th') ? 'th' : 'en';
            localStorage.setItem('lang', lang);
        }
        // Always send both header and query param; backend prioritizes ?lang over header
        config.headers['Accept-Language'] = lang;
        // Ensure lang query param is present/overridden
        if (!config.params) config.params = {};
        config.params.lang = lang;

        // Identify public/auth endpoints to avoid noisy logs before login
        const url = (config.url || '').toString();
        const isAuthEndpoint = /\/auth\//.test(url) || /\/emergency\//.test(url);

        if (!skipAuth) {
            const token = localStorage.getItem('token');
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            } else {
                // Don't spam console for public endpoints (e.g., login) when token is not yet available
                if (!isAuthEndpoint) {
                    // Use a low-severity log for easier debugging without alarming errors
                    console.debug('JWT token is not set for this request.');
                }
            }
        }
        return config;
    },
    (error) => Promise.reject(error)
);

axiosInstance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            const url = (error.config && error.config.url) ? error.config.url.toString() : '';
            const isAuthEndpoint = /\/auth\//.test(url) || /\/emergency\//.test(url);
            if (!isAuthEndpoint) {
                console.warn('Unauthorized (401). Redirecting to login.');
                localStorage.removeItem('token');
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default axiosInstance;