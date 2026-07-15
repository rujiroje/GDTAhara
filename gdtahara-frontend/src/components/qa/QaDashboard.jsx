// =================================================================
// File: src/components/qa/QaDashboard.jsx (ไฟล์ใหม่-ฉบับเต็ม)
// =================================================================
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import MachineSelectGrid from '../common/MachineSelectGrid';

// --- API Service (จำลองการตั้งค่า) ---
const API_URL = 'http://localhost:8080/api';
const api = axios.create({ baseURL: API_URL });

api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, error => Promise.reject(error));


// --- Shared Components ---
const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <div className="modal-header">
                    <h3>{title}</h3>
                    <button onClick={onClose} className="modal-close-button">&times;</button>
                </div>
                <div className="modal-body">{children}</div>
            </div>
        </div>
    );
};


// --- Main QA Component ---
const QaDashboard = () => {
    const [activeReports, setActiveReports] = useState([]);
    const [selectedReport, setSelectedReport] = useState(null);
    const [error, setError] = useState('');
    const [ngTypes, setNgTypes] = useState([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedNg, setSelectedNg] = useState(null);
    const [quantity, setQuantity] = useState(1);
    const [qaHistory, setQaHistory] = useState([]);
    const [showHistory, setShowHistory] = useState(false);
    const [editingLog, setEditingLog] = useState(null);
    const [editQuantity, setEditQuantity] = useState(0);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [currentUser, setCurrentUser] = useState(''); // เก็บ username ปัจจุบัน

    useEffect(() => {
        const fetchActiveReports = async () => {
            try {
                // **[แก้ไข]** ใช้ QA endpoint แทน PC endpoint
                const response = await api.get('/qa/reports/active');
                console.log('QA active reports:', response.data);
                setActiveReports(response.data);
            } catch (err) {
                console.error('Error fetching QA reports:', err);
                setError('ไม่สามารถดึงรายการใบสั่งผลิตได้');
            }
        };
        fetchActiveReports();
    }, []);
    
    useEffect(() => {
        // ดึง current user จาก localStorage หรือ context
        const token = localStorage.getItem('token');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                setCurrentUser(payload.sub || ''); // username จาก JWT
            } catch (e) {
                console.error('Error parsing token:', e);
            }
        }
    }, []);

    useEffect(() => {
        if (selectedReport) {
            const fetchNgTypes = async () => {
                try {
                    // **[แก้ไข]** ใช้ QA endpoint สำหรับ NG Types
                    const response = await api.get('/qa/ng-types');
                    console.log('QA NG Types:', response.data);
                    setNgTypes(response.data);
                } catch (err) {
                    console.error("Could not fetch QA NG types", err);
                    setError('ไม่สามารถดึงรายการ NG Types ได้');
                }
            };
            
            const fetchQaHistory = async () => {
                try {
                    const response = await api.get(`/qa/reports/${selectedReport.id}/qa-history`);
                    console.log('QA History:', response.data);
                    setQaHistory(response.data);
                } catch (err) {
                    console.error("Could not fetch QA history", err);
                    // ไม่ต้อง set error เพราะไม่ใช่ข้อมูลหลัก
                }
            };
            
            fetchNgTypes();
            fetchQaHistory();
        }
    }, [selectedReport]);

    const handleOpenModal = (ng) => {
        setSelectedNg(ng);
        setQuantity(1);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedNg(null);
    };

    const handleRecordQaNg = async (e) => {
        e.preventDefault();
        if(!quantity || parseInt(quantity, 10) <= 0) {
            alert('กรุณากรอกจำนวนที่ถูกต้อง');
            return;
        }
        try {
            await api.post(`/qa/reports/${selectedReport.id}/ng-logs`, {
                ngTypeId: selectedNg.id,
                quantity: parseInt(quantity, 10),
            });
            alert('บันทึกของเสียสำเร็จ');
            handleCloseModal();
            
            // **[ใหม่]** Refresh QA history หลังบันทึกสำเร็จ
            const historyResponse = await api.get(`/qa/reports/${selectedReport.id}/qa-history`);
            setQaHistory(historyResponse.data);
        } catch (err) {
            alert(err.response?.data || 'เกิดข้อผิดพลาดในการบันทึก');
        }
    };

    const handleEditLog = (logItem) => {
        setEditingLog(logItem);
        setEditQuantity(logItem.quantity);
        setIsEditModalOpen(true);
    };

    const handleCloseEditModal = () => {
        setIsEditModalOpen(false);
        setEditingLog(null);
        setEditQuantity(0);
    };

    const handleUpdateLog = async (e) => {
        e.preventDefault();
        if (editQuantity < 0) {
            alert('จำนวนต้องไม่ติดลบ (ใส่ 0 หากต้องการลบ)');
            return;
        }
        
        try {
            await api.put(`/qa/ng-logs/${editingLog.id}`, {
                quantity: parseInt(editQuantity, 10)
            });
            
            alert(editQuantity === 0 ? 'ลบรายการสำเร็จ' : 'แก้ไขจำนวนสำเร็จ');
            handleCloseEditModal();
            
            // Refresh QA history
            const historyResponse = await api.get(`/qa/reports/${selectedReport.id}/qa-history`);
            setQaHistory(historyResponse.data);
        } catch (err) {
            alert(err.response?.data || 'เกิดข้อผิดพลาดในการแก้ไข');
        }
    };

    if (selectedReport) {
        return (
            <div className="dashboard-card">
                <button onClick={() => setSelectedReport(null)} className="back-button"> &larr; กลับไปหน้ารายการ</button>
                <h2 className="dashboard-title">บันทึกผลการตรวจสอบคุณภาพ</h2>
                <div className="selected-report-info">
                    <span><strong>เครื่องจักร:</strong> {selectedReport.machineName}</span>
                    <span><strong>ผลิตภัณฑ์:</strong> {selectedReport.productName}</span>
                </div>
                <h3 className="dashboard-subtitle">บันทึกของเสีย (NG) จากการสุ่มตรวจ</h3>
                <div className="ng-buttons-container">
                    {ngTypes.map((ng, index) => (
                        <button 
                            key={ng.id} 
                            className={`ng-button ng-color-${index % 10}`}
                            onClick={() => handleOpenModal(ng)}
                        >
                            {ng.ngDescriptionTh}
                        </button>
                    ))}
                </div>
                
                {/* **[ใหม่]** ส่วนแสดงประวัติการตรวจสอบคุณภาพ */}
                <div className="qa-history-section">
                    <div className="history-header">
                        <h3 className="dashboard-subtitle">ประวัติการตรวจสอบคุณภาพ</h3>
                        <button 
                            className="toggle-history-button"
                            onClick={() => setShowHistory(!showHistory)}
                        >
                            {showHistory ? 'ซ่อนประวัติ' : `แสดงประวัติ (${qaHistory.length} รายการ)`}
                        </button>
                    </div>
                    
                    {showHistory && (
                        <div className="history-content">
                            {qaHistory.length === 0 ? (
                                <p className="no-history">ยังไม่มีประวัติการตรวจสอบคุณภาพในใบสั่งผลิตนี้</p>
                            ) : (
                                <div className="history-table">
                                    <table>
                                        <thead>
                                            <tr>
                                                <th>วันที่/เวลา</th>
                                                <th>ผู้ตรวจ</th>
                                                <th>ประเภทของเสีย</th>
                                                <th>จำนวน</th>
                                                <th>จัดการ</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {qaHistory.map((item, index) => (
                                                <tr key={item.id || index}>
                                                    <td>{item.timestamp}</td>
                                                    <td>{item.qaUser}</td>
                                                    <td>{item.ngType}</td>
                                                    <td>{item.quantity} ชิ้น</td>
                                                    <td>
                                                        {item.qaUser === currentUser ? (
                                                            <button 
                                                                className="edit-log-button"
                                                                onClick={() => handleEditLog(item)}
                                                                title="แก้ไขจำนวน (ใส่ 0 เพื่อลบ)"
                                                            >
                                                                ✏️ แก้ไข
                                                            </button>
                                                        ) : (
                                                            <span className="no-edit">-</span>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </div>
                
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={`บันทึก: ${selectedNg?.ngDescriptionTh}`}>
                    <form onSubmit={handleRecordQaNg}>
                        <div className="form-group">
                            <label className="form-label">จำนวนที่พบ</label>
                            <input 
                                type="number" 
                                value={quantity} 
                                onChange={(e) => setQuantity(e.target.value)} 
                                className="form-input" 
                                required 
                                min="1"
                            />
                        </div>
                        <div className="form-actions">
                            <button type="button" onClick={handleCloseModal} className="cancel-button">ยกเลิก</button>
                            <button type="submit" className="save-button">บันทึก</button>
                        </div>
                    </form>
                </Modal>
                
                {/* **[ใหม่]** Modal สำหรับแก้ไข NG Log */}
                <Modal isOpen={isEditModalOpen} onClose={handleCloseEditModal} title={`แก้ไข: ${editingLog?.ngType}`}>
                    <form onSubmit={handleUpdateLog}>
                        <div className="form-group">
                            <label className="form-label">จำนวนใหม่ (ใส่ 0 เพื่อลบรายการ)</label>
                            <input 
                                type="number" 
                                value={editQuantity} 
                                onChange={(e) => setEditQuantity(e.target.value)} 
                                className="form-input" 
                                required 
                                min="0"
                            />
                            <small className="form-hint">
                                จำนวนเดิม: {editingLog?.quantity} ชิ้น
                            </small>
                        </div>
                        <div className="form-actions">
                            <button type="button" onClick={handleCloseEditModal} className="cancel-button">ยกเลิก</button>
                            <button type="submit" className="save-button">
                                {editQuantity === '0' ? 'ลบรายการ' : 'บันทึกการแก้ไข'}
                            </button>
                        </div>
                    </form>
                </Modal>
            </div>
        );
    }

    return (
        <div className="dashboard-card">
            {error && <p className="error-message">{error}</p>}
            <MachineSelectGrid
                reports={activeReports}
                onSelect={report => setSelectedReport(report)}
                title="เลือกใบสั่งผลิตเพื่อตรวจสอบคุณภาพ"
            />
        </div>
    );
};

export default QaDashboard;

// **[ใหม่]** CSS สำหรับ QA History ให้เพิ่มใน global CSS หรือ component CSS
/*
.qa-history-section {
    margin-top: 2rem;
    border-top: 1px solid #e0e0e0;
    padding-top: 1rem;
}

.history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.toggle-history-button {
    padding: 0.5rem 1rem;
    background-color: #f5f5f5;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.9rem;
}

.toggle-history-button:hover {
    background-color: #e0e0e0;
}

.history-content {
    background-color: #f9f9f9;
    padding: 1rem;
    border-radius: 8px;
}

.no-history {
    text-align: center;
    color: #666;
    font-style: italic;
}

.history-table table {
    width: 100%;
    border-collapse: collapse;
    background-color: white;
    border-radius: 4px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.history-table th,
.history-table td {
    padding: 0.75rem;
    text-align: left;
    border-bottom: 1px solid #e0e0e0;
}

.history-table th {
    background-color: #f0f0f0;
    font-weight: 600;
    color: #333;
}

.history-table tr:last-child td {
    border-bottom: none;
}

.history-table tr:hover {
    background-color: #f5f5f5;
}

.edit-log-button {
    padding: 0.25rem 0.5rem;
    background-color: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.8rem;
}

.edit-log-button:hover {
    background-color: #0056b3;
}

.no-edit {
    color: #999;
    font-style: italic;
}

.form-hint {
    display: block;
    margin-top: 0.25rem;
    font-size: 0.85rem;
    color: #666;
}
*/