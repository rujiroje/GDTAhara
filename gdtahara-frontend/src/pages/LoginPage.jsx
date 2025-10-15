// =================================================================
// File: src/pages/Login.js (ไฟล์ใหม่)
// Component สำหรับหน้า Login
// =================================================================
import React, { useState } from 'react';
import { useAuth } from '../auth/AuthContext';

const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login } = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            await login(username, password);
        } catch (err) {
            setError('ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง หรือเกิดข้อผิดพลาด');
            console.error(err);
        }
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
                    <div className="form-group">
                        <label htmlFor="username" className="form-label">ชื่อผู้ใช้</label>
                        <input id="username" type="text" value={username} onChange={(e) => setUsername(e.target.value)} className="form-input" required />
                    </div>
                    <div className="form-group" style={{ marginBottom: '1.5rem' }}>
                        <label htmlFor="password" className="form-label">รหัสผ่าน</label>
                        <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="form-input" required />
                    </div>
                    <button type="submit" className="submit-button">เข้าสู่ระบบ</button>
                </form>
            </div>
        </div>
    );
};

export default Login;
