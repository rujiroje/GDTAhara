// =================================================================
// File: src/components/common/NotificationPanel.js
// =================================================================
import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiService';

const NotificationPanel = () => {
    const [alerts, setAlerts] = useState([]);

    const fetchAlerts = async () => {
        try {
            const response = await api.get('/notifications/alerts/active');
            setAlerts(response.data);
        } catch (error) {
            console.error("Failed to fetch alerts:", error);
        }
    };

    useEffect(() => {
        fetchAlerts();
        const interval = setInterval(fetchAlerts, 30000); // Refresh every 30 seconds
        return () => clearInterval(interval);
    }, []);

    const handleAcknowledge = async (id) => {
        try {
            await api.post(`/notifications/alerts/${id}/acknowledge`);
            fetchAlerts(); // Refresh immediately after acknowledging
        } catch (error) {
            alert('เกิดข้อผิดพลาดในการรับทราบการแจ้งเตือน');
        }
    };

    if (alerts.length === 0) {
        return null;
    }

    return (
        <div className="notification-panel">
            <h3 className="notification-title">
                <span role="img" aria-label="alert">🚨</span> การแจ้งเตือนเครื่องจักรหยุด
            </h3>
            <div className="notification-list">
                {alerts.map(alert => (
                    <div key={alert.id} className="notification-item">
                        <div className="notification-content">
                            <p><strong>เครื่อง:</strong> {alert.machineName} ({alert.productName})</p>
                            <p><strong>สาเหตุ:</strong> {alert.message}</p>
                            <p className="notification-meta">แจ้งโดย: {alert.operatorName} | เวลา: {alert.timestamp}</p>
                        </div>
                        <button onClick={() => handleAcknowledge(alert.id)} className="acknowledge-button">รับทราบ</button>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default NotificationPanel;
