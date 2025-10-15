// =================================================================
// File: src/auth/AuthContext.jsx
// =================================================================
import React, { useState, useEffect, createContext, useContext } from 'react';
import { jwtDecode } from 'jwt-decode';
import { api } from '../api/apiService';

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
                    setUser({ username: decodedUser.sub, role: decodedUser.role });
                } else {
                    localStorage.removeItem('token');
                }
            }
        } catch (error) {
            console.error("Failed to decode token:", error);
            localStorage.removeItem('token');
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        const response = await api.post('/auth/login', { username, password });
        const { token } = response.data;
        localStorage.setItem('token', token);
        const decodedUser = jwtDecode(token);
        setUser({ username: decodedUser.sub, role: decodedUser.role });
    };

    const logout = () => {
        localStorage.removeItem('token');
        setUser(null);
    };

    if (loading) return <div className="loading-container"><h2>กำลังโหลด...</h2></div>;

    return (
        <AuthContext.Provider value={{ user, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
