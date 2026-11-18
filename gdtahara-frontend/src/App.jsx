// =================================================================
// File: src/App.jsx (ฉบับแก้ไข Final)
// =================================================================
import React, { useState, useEffect, createContext, useContext } from 'react';
import axiosInstance from './api/axios';
import { jwtDecode } from 'jwt-decode';
import Dashboard from './pages/Dashboard';
import './App.css';


// --- Authentication Context ---
const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        try {
            const token = localStorage.getItem('token');
            if (token) {
                const decodedUser = jwtDecode(token);
                if (decodedUser.exp * 1000 > Date.now()) {
                    const userId = decodedUser.userId ?? null;
                    setUser({ username: decodedUser.sub, role: decodedUser.role, userId });
                    if (userId != null) localStorage.setItem('userId', String(userId));
                } else { localStorage.removeItem('token'); }
            }
        } catch (error) {
            console.error("Failed to decode token:", error);
            localStorage.removeItem('token');
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
    const response = await axiosInstance.post('/auth/login', { username, password });
        const { token } = response.data;
        localStorage.setItem('token', token);
        const decodedUser = jwtDecode(token);
        const userId = decodedUser.userId ?? null;
        setUser({ username: decodedUser.sub, role: decodedUser.role, userId });
        if (userId != null) localStorage.setItem('userId', String(userId));
    };

    const logout = () => {
        localStorage.removeItem('token');
        setUser(null);
        window.location.reload();
    };

    if (loading) return <div className="loading-container"><h2>กำลังโหลด...</h2></div>;

    return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
};

// **[แก้ไข]** Export useAuth จากไฟล์นี้โดยตรง
export const useAuth = () => useContext(AuthContext);

// --- Login Component ---
const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login } = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try { await login(username, password); } 
        catch (err) { setError('ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง หรือเกิดข้อผิดพลาด'); console.error(err); }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <div className="login-header">
                    <h1 className="login-title">GDTahara System</h1>
                    <p className="login-subtitle">กรุณาเข้าสู่ระบบเพื่อใช้งาน</p>
                </div>
                {error && <div className="error-message">{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div className="form-group"><label htmlFor="username" className="form-label">ชื่อผู้ใช้</label><input id="username" type="text" value={username} onChange={(e) => setUsername(e.target.value)} className="form-input" required /></div>
                    <div className="form-group" style={{ marginBottom: '1.5rem' }}><label htmlFor="password" className="form-label">รหัสผ่าน</label><input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="form-input" required /></div>
                    <button type="submit" className="submit-button">เข้าสู่ระบบ</button>
                </form>
            </div>
        </div>
    );
};

// --- App Controller ---
function AppController() {
    const { user } = useAuth();
    return user ? <Dashboard /> : <Login />;
}

// --- Main App Component ---
export default function App() {
    return (
        <AuthProvider>
            <AppController />
        </AuthProvider>
    );
}