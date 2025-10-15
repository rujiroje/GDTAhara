// =================================================================
// File: src/components/shift-leader/ShiftLeaderDashboard.jsx (ฉบับสมบูรณ์ แก้ไขทั้งหมด)
// =================================================================
import React, { useState, useEffect } from 'react';
import axios from 'axios';

// --- API Service ---
const API_URL = 'http://localhost:8080/api';
const api = axios.create({ baseURL: API_URL });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) { config.headers.Authorization = `Bearer ${token}`; }
    return config;
}, error => Promise.reject(error));

// --- Shared Components ---
const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return ( <div className="modal-overlay"> <div className="modal-content"> <div className="modal-header"> <h3>{title}</h3> <button onClick={onClose} className="modal-close-button">&times;</button> </div> <div className="modal-body">{children}</div> </div> </div> );
};

const NotificationPanel = () => {
    const [alerts, setAlerts] = useState([]);
    const fetchAlerts = async () => { try { const response = await api.get('/notifications/alerts/active'); setAlerts(response.data); } catch (error) { console.error("Failed to fetch alerts:", error); } };
    useEffect(() => { fetchAlerts(); const interval = setInterval(fetchAlerts, 30000); return () => clearInterval(interval); }, []);
    const handleAcknowledge = async (id) => { try { await api.post(`/notifications/alerts/${id}/acknowledge`); fetchAlerts(); } catch (error) { alert('เกิดข้อผิดพลาดในการรับทราบการแจ้งเตือน'); } };
    if (alerts.length === 0) return null;
    return ( <div className="notification-panel"> <h3 className="notification-title"><span role="img" aria-label="alert">🚨</span> การแจ้งเตือนเครื่องจักรหยุด</h3> <div className="notification-list"> {alerts.map(alert => ( <div key={alert.id} className="notification-item"> <div className="notification-content"> <p><strong>เครื่อง:</strong> {alert.machineName} ({alert.productName})</p> <p><strong>สาเหตุ:</strong> {alert.message}</p> <p className="notification-meta">แจ้งโดย: {alert.operatorName} | เวลา: {alert.timestamp}</p> </div> <button onClick={() => handleAcknowledge(alert.id)} className="acknowledge-button">รับทราบ</button> </div> ))} </div> </div> );
};

const ShiftDataDisplay = ({ title, data }) => ( <div className="shift-data-container" style={{background: '#fff', padding: '1.5rem', borderRadius: '0.75rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', marginBottom: '1.5rem'}}> <h3>{title}</h3> <div className="kpi-grid"><div className="kpi-card"><span className="kpi-label">ยอดผลิตดี (กล่อง)</span><span className="kpi-value">{data.goodProductionBoxes}</span></div><div className="kpi-card"><span className="kpi-label">ยอดของเสีย (ชิ้น)</span><span className="kpi-value ng-value">{data.ngProductionPieces}</span></div><div className="kpi-card"><span className="kpi-label">ยอดผลิตรวม (ชิ้น)</span><span className="kpi-value">{data.totalProductionPieces}</span></div><div className="kpi-card"><span className="kpi-label">Yield</span><span className="kpi-value yield-value">{data.yieldPercentage}</span></div></div> <div className="summary-section-grid"><div className="summary-panel"><h3 className="summary-title">สรุปยอดของเสีย</h3><ul className="ng-summary-list">{data.ngSummary.length > 0 ? data.ngSummary.map(ng => (<li key={ng.ngDescription}><span>{ng.ngDescription}</span><span>{ng.count} ชิ้น</span></li>)) : <p>ไม่มีข้อมูล</p>}</ul></div><div className="summary-panel"><h3 className="summary-title">ประวัติ Downtime</h3><div className="data-table-container"><table className="data-table"><thead><tr><th>เวลาเริ่ม</th><th>เวลาสิ้นสุด</th><th>ระยะเวลา</th><th>สาเหตุ</th><th>ผู้บันทึก</th></tr></thead><tbody>{data.downtimeHistory.length > 0 ? data.downtimeHistory.map((event, index) => (<tr key={index}><td>{event.startTime}</td><td>{event.endTime}</td><td>{event.duration}</td><td>{event.reason}</td><td>{event.technicianName}</td></tr>)) : <tr><td colSpan="5">ไม่มีข้อมูล</td></tr>}</tbody></table></div></div></div> </div> );

// --- Sub-Component Pages ---

const LabelStockManager = ({ onBack }) => {
    const [stocks, setStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [quantityToAdd, setQuantityToAdd] = useState('');

    const fetchStocks = async () => { setLoading(true); try { const response = await api.get('/shift-leader/label-stocks'); setStocks(response.data); } catch (err) { console.error("Failed to fetch stocks", err); } finally { setLoading(false); } };
    useEffect(() => { fetchStocks(); }, []);

    const handleAddStockClick = (product) => { setSelectedProduct(product); setIsModalOpen(true); };
    const handleCloseStockModal = () => { setIsModalOpen(false); setSelectedProduct(null); setQuantityToAdd(''); };
    const handleAddStock = async (e) => {
        e.preventDefault();
        if (!quantityToAdd || parseInt(quantityToAdd, 10) <= 0) { alert('กรุณากรอกจำนวนที่ถูกต้อง'); return; }
        try {
            await api.post(`/shift-leader/label-stocks/${selectedProduct.productId}/add`, { quantityToAdd: parseInt(quantityToAdd, 10) });
            fetchStocks();
            handleCloseStockModal();
        } catch (err) { alert('เกิดข้อผิดพลาดในการเพิ่มสต็อก'); }
    };
    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูลสต็อกป้าย...</h2></div>;
    return ( <div className="dashboard-card"> <div className="sl-header"> <h2 className="dashboard-title">จัดการสต็อกป้าย (Label Stock)</h2> <button onClick={onBack} className="back-button-sl">กลับไปเมนูหลัก</button> </div> <div className="data-table-container"> <table className="data-table"> <thead><tr><th>รหัสผลิตภัณฑ์</th><th>ชื่อผลิตภัณฑ์</th><th>สต็อกปัจจุบัน</th><th>Actions</th></tr></thead> <tbody> {stocks.map(stock => ( <tr key={stock.productId}> <td>{stock.productCode}</td><td>{stock.productName}</td><td>{stock.currentStock?.toLocaleString() || 0}</td> <td className="actions-cell"> <button className="add-button" onClick={() => handleAddStockClick(stock)}>เพิ่มสต็อก</button> </td> </tr> ))} </tbody> </table> </div> <Modal isOpen={isModalOpen} onClose={handleCloseStockModal} title={`เพิ่มสต็อกสำหรับ: ${selectedProduct?.productName}`}> <form onSubmit={handleAddStock}><div className="form-group"><label className="form-label">จำนวนที่ต้องการเพิ่ม</label><input type="number" value={quantityToAdd} onChange={(e) => setQuantityToAdd(e.target.value)} className="form-input" required min="1"/></div><div className="form-actions"><button type="button" onClick={handleCloseStockModal} className="cancel-button">ยกเลิก</button><button type="submit" className="save-button">ยืนยัน</button></div></form> </Modal> </div> );
};

const MaterialStockManager = ({ onBack }) => {
    const [stockCards, setStockCards] = useState([]);
    const [materials, setMaterials] = useState([]);
    const [selectedStockCard, setSelectedStockCard] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingTransaction, setEditingTransaction] = useState(null);
    const initialFormData = { materialId: '', quantity: '', lotNumber: '' };
    const [formData, setFormData] = useState(initialFormData);
    const [loading, setLoading] = useState(true);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [materialsRes, stockCardsRes] = await Promise.all([
                api.get('/master-data/materials'),
                api.get('/shift-leader/material-stocks')
            ]);
            setMaterials(materialsRes.data);
            setStockCards(stockCardsRes.data);
        } catch (error) {
            console.error("Failed to fetch material data", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, []);

    const handleFormChange = (e) => { const { name, value } = e.target; setFormData(prev => ({ ...prev, [name]: value })); };
    
    const handleSubmit = async (e) => {
        e.preventDefault();
        const apiCall = editingTransaction
            ? api.put(`/shift-leader/stock-transactions/${editingTransaction.id}`, formData)
            : api.post('/shift-leader/stock-transactions', { ...formData, transactionType: 'IN' });
        try {
            await apiCall;
            alert(editingTransaction ? 'แก้ไขข้อมูลสำเร็จ!' : 'บันทึกรับของเข้าสำเร็จ!');
            handleCloseModal();
            fetchData();
        } catch (error) { alert('เกิดข้อผิดพลาดในการบันทึกข้อมูล'); }
    };

    const handleOpenCreateModal = () => {
        setEditingTransaction(null);
        setFormData(initialFormData);
        setIsModalOpen(true);
    };

    const handleOpenEditModal = (transaction) => {
        const material = materials.find(m => m.materialCode === selectedStockCard.materialCode);
        setEditingTransaction(transaction);
        setFormData({
            materialId: material ? material.id : '',
            quantity: transaction.quantity,
            lotNumber: transaction.lotNumber
        });
        setIsModalOpen(true);
    };
    
    const handleCloseModal = () => {
        setIsModalOpen(false);
        setEditingTransaction(null);
    };

    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูล...</h2></div>;
    
    const renderContent = () => {
        if (selectedStockCard) {
            return (
                <div>
                    <button onClick={() => setSelectedStockCard(null)} className="back-button-sl">&larr; กลับไปหน้ารวม</button>
                    <h2 className="dashboard-title">Stock Card: {selectedStockCard.materialName} ({selectedStockCard.materialCode})</h2>
                    <h3>ยอดคงเหลือปัจจุบัน: {selectedStockCard.currentStock} {materials.find(m => m.id === selectedStockCard.materialId)?.unit || ''}</h3>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead><tr><th>วันที่/เวลา</th><th>ประเภท</th><th>จำนวน</th><th>Lot Number</th><th>ข้อมูลการผลิต</th><th>ผู้บันทึก</th><th>Actions</th></tr></thead>
                            <tbody>
                                {selectedStockCard.history.map(tx => (
                                    <tr key={tx.id}>
                                        <td>{tx.timestamp}</td>
                                        <td><span className={tx.transactionType === 'IN' ? 'status-in-progress' : 'status-finalized'}>{tx.transactionType}</span></td>
                                        <td>{tx.quantity}</td>
                                        <td>{tx.lotNumber}</td>
                                        <td>{tx.productionInfo}</td>
                                        <td>{tx.userName}</td>
                                        <td className="actions-cell">
                                            {tx.transactionType === 'IN' && <button className="edit-button" onClick={() => handleOpenEditModal(tx)}>แก้ไข</button>}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            );
        }
        
        return (
            <div>
                <div className="table-header">
                    <h2 className="dashboard-title">จัดการสต็อกวัตถุดิบ</h2>
                    <button className="add-button" onClick={handleOpenCreateModal}>รับของเข้า (Stock-In)</button>
                </div>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>รหัสวัตถุดิบ</th><th>ชื่อวัตถุดิบ</th><th>ยอดคงเหลือ</th><th>Actions</th></tr></thead>
                        <tbody>
                            {stockCards.map(card => (
                                <tr key={card.materialId}>
                                    <td>{card.materialCode}</td>
                                    <td>{card.materialName}</td>
                                    <td>{card.currentStock}</td>
                                    <td className="actions-cell">
                                        <button className="add-button" onClick={() => setSelectedStockCard(card)}>ดูประวัติ</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    };

    return (
        <div className="dashboard-card">
            {selectedStockCard ? null : <button onClick={onBack} className="back-button-sl">&larr; กลับไปเมนูหลัก</button>}
            {renderContent()}
            <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={editingTransaction ? "แก้ไขรายการรับเข้า" : "บันทึกรับวัตถุดิบเข้าสต็อก"}>
                <form onSubmit={handleSubmit}>
                    <div className="form-group"> <label className="form-label">วัตถุดิบ</label> <select name="materialId" value={formData.materialId} onChange={handleFormChange} className="form-input" required> <option value="" disabled>-- เลือกวัตถุดิบ --</option> {materials.map(mat => ( <option key={mat.id} value={mat.id}>{mat.materialName} ({mat.materialCode})</option> ))} </select> </div>
                    <div className="form-group"> <label className="form-label">Lot Number</label> <input type="text" name="lotNumber" value={formData.lotNumber} onChange={handleFormChange} className="form-input" required /> </div>
                    <div className="form-group"> <label className="form-label">จำนวน (หน่วย: Kg.)</label> <input type="number" step="0.01" name="quantity" value={formData.quantity} onChange={handleFormChange} className="form-input" required /> </div>
                    <div className="form-actions"> <button type="button" onClick={handleCloseModal} className="cancel-button">ยกเลิก</button> <button type="submit" className="save-button">บันทึก</button> </div>
                </form>
            </Modal>
        </div>
    );
};


const NgRecording = ({ onBack, activeReports }) => {
    const [selectedReport, setSelectedReport] = useState(null);
    const [ngTypes, setNgTypes] = useState([]);
    const [ngLogs, setNgLogs] = useState([]);
    const [loadingLogs, setLoadingLogs] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingLog, setEditingLog] = useState(null);
    const [formData, setFormData] = useState({ ngTypeId: '', quantity: 1 });

    useEffect(() => {
        const fetchNgTypes = async () => {
            try {
                const response = await api.get('/master-data/ng-types');
                setNgTypes(response.data.filter(ng => ng.ngType === 'Shift Leader'));
            } catch (error) { console.error("Failed to fetch NG types", error); }
        };
        fetchNgTypes();
    }, []);

    const fetchNgLogsForReport = async (reportId) => {
        setLoadingLogs(true);
        try {
            const response = await api.get(`/shift-leader/reports/${reportId}/ng-logs`);
            setNgLogs(response.data);
        } catch (error) {
            console.error("Could not fetch existing NG logs", error);
            setNgLogs([]);
        } finally {
            setLoadingLogs(false);
        }
    };

    const handleSelectReport = (report) => {
        setSelectedReport(report);
        fetchNgLogsForReport(report.id);
    };

    const handleFormChange = (e) => { const { name, value } = e.target; setFormData(prev => ({ ...prev, [name]: value })); };
    
    const handleSubmit = async (e) => {
        e.preventDefault();
        const apiCall = editingLog 
            ? api.put(`/ng-logs/${editingLog.id}`, formData)
            : api.post(`/shift-leader/reports/${selectedReport.id}/ng-logs`, formData);
        try {
            await apiCall;
            setIsModalOpen(false);
            fetchNgLogsForReport(selectedReport.id); // Refresh logs after save/edit
            alert(editingLog ? 'แก้ไขข้อมูลสำเร็จ!' : 'บันทึกของเสียสำเร็จ!');
        } catch (error) {
            alert("เกิดข้อผิดพลาดในการบันทึก");
        }
    };
    
    const handleDelete = async (logId) => {
        if (window.confirm('คุณแน่ใจหรือไม่ว่าต้องการลบรายการนี้?')) {
            try {
                await api.delete(`/ng-logs/${logId}`);
                setNgLogs(ngLogs.filter(log => log.id !== logId));
                alert('ลบข้อมูลสำเร็จ!');
            } catch (error) {
                alert("เกิดข้อผิดพลาดในการลบ");
            }
        }
    };

    const openEditModal = (log) => {
        setEditingLog(log);
        const ngTypeId = log.ngType ? log.ngType.id : '';
        setFormData({ ngTypeId: ngTypeId, quantity: log.quantity });
        setIsModalOpen(true);
    };

    const openCreateModal = () => {
        setEditingLog(null);
        setFormData({ ngTypeId: '', quantity: 1 });
        setIsModalOpen(true);
    };

    if (!selectedReport) {
        return ( <div className="dashboard-card"> <button onClick={onBack} className="back-button-sl">&larr; กลับไปเมนูหลัก</button> <h2 className="dashboard-title">บันทึกของเสีย: เลือกใบสั่งผลิต</h2> <div className="report-selection-container"> {activeReports.map(report => ( <div key={report.id} className="report-card"> <h3>{report.machineName}</h3><p>{report.productName}</p><p>วันที่: {report.productionDate || report.startDate}</p><button className="select-button" onClick={() => handleSelectReport(report)}>เลือก</button> </div> ))} </div> </div> );
    }

    return (
        <div className="dashboard-card">
            <button onClick={() => setSelectedReport(null)} className="back-button-sl">&larr; กลับไปเลือกใบสั่งผลิต</button>
            <div className="table-header">
                <h2 className="dashboard-title">บันทึกของเสียสำหรับ: {selectedReport.machineName}</h2>
                <button className="add-button" onClick={openCreateModal}>เพิ่มรายการของเสีย</button>
            </div>
            <div className="data-table-container">
                {loadingLogs ? <p>กำลังโหลดประวัติ...</p> : 
                <table className="data-table">
                    <thead><tr><th>ประเภทของเสีย</th><th>จำนวน</th><th>Actions</th></tr></thead>
                    <tbody>
                        {ngLogs.map((log) => (
                            <tr key={log.id}>
                                <td>{log.ngType?.ngDescriptionTh || 'N/A'}</td>
                                <td>{log.quantity}</td>
                                <td className="actions-cell">
                                    <button className="edit-button" onClick={() => openEditModal(log)}>แก้ไข</button>
                                    <button className="delete-button" onClick={() => handleDelete(log.id)}>ลบ</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                }
            </div>
            <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingLog ? "แก้ไขรายการของเสีย" : "บันทึกรายการของเสีย"}>
                <form onSubmit={handleSubmit}>
                    <div className="form-group"> <label className="form-label">ประเภทของเสีย</label> <select name="ngTypeId" value={formData.ngTypeId} onChange={handleFormChange} className="form-input" required> <option value="" disabled>-- เลือกประเภท --</option> {ngTypes.map(ng => <option key={ng.id} value={ng.id}>{ng.ngDescriptionTh}</option>)} </select> </div>
                    <div className="form-group"> <label className="form-label">จำนวน</label> <input type="number" name="quantity" value={formData.quantity} onChange={handleFormChange} className="form-input" required min="1" /> </div>
                    <div className="form-actions"> <button type="button" onClick={() => setIsModalOpen(false)} className="cancel-button">ยกเลิก</button> <button type="submit" className="save-button">บันทึก</button> </div>
                </form>
            </Modal>
        </div>
    );
};


// --- Main ShiftLeaderDashboard Component ---
const ShiftLeaderDashboard = () => {
    const [view, setView] = useState('main'); 
    const [activeReports, setActiveReports] = useState([]);
    const [selectedReportId, setSelectedReportId] = useState(null);
    const [dashboardData, setDashboardData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const handleNavigate = (newView) => {
        setError('');
        setView(newView);
    };
    
    useEffect(() => {
        const fetchActiveReports = async () => {
            setLoading(true);
            try {
                const response = await api.get('/pc/reports/active?scope=today');
                setActiveReports(response.data);
            } catch (err) {
                setError('ไม่สามารถดึงข้อมูลใบสั่งผลิตได้');
            } finally {
                setLoading(false);
            }
        };
        
        if (view === 'dashboard' || view === 'ng') {
            fetchActiveReports();
        } else {
            setLoading(false); // Set loading false for other views
        }
    }, [view]);

    const handleSelectReport = async (reportId) => {
        setLoading(true);
        try {
            const response = await api.get(`/shift-leader/dashboard/${reportId}`);
            setDashboardData(response.data);
            setSelectedReportId(reportId);
            setView('detail');
        } catch (err) {
            setError('ไม่สามารถดึงข้อมูล Dashboard ได้');
            setView('dashboard');
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูล...</h2></div>;
    if (error) return <div className="dashboard-card"><p className="error-message">{error}</p><button onClick={() => handleNavigate('main')} className="back-button-sl">กลับไปเมนูหลัก</button></div>;

    // --- View Router ---
    if (view === 'detail') {
        return ( <div className="dashboard-card-sl"> <NotificationPanel /> <div className="sl-header"> <div> <h2 className="sl-title">รายละเอียด: {dashboardData.machineName}</h2> <p className="sl-subtitle">ผลิตภัณฑ์: {dashboardData.productName} | วันที่ผลิต: {dashboardData.productionDate}</p> </div> <div> <button onClick={() => handleNavigate('dashboard')} className="back-button-sl">กลับไปหน้ารวม</button> </div> </div> <ShiftDataDisplay title="กะกลางวัน (03:00 - 15:00)" data={dashboardData.dayShiftData} /> <ShiftDataDisplay title="กะกลางคืน (15:00 - 03:00)" data={dashboardData.nightShiftData} /> </div> );
    }
    if (view === 'dashboard') {
        return ( <div className="dashboard-card"> <NotificationPanel /> <div className="sl-header"> <h2 className="dashboard-title">เลือกเครื่องจักรเพื่อดู Dashboard</h2> <button onClick={() => handleNavigate('main')} className="back-button-sl">กลับไปเมนูหลัก</button> </div> {activeReports.length === 0 ? <p>ไม่มีใบสั่งผลิตที่กำลังทำงานในวันนี้</p> : <div className="report-selection-container"> {activeReports.map(report => ( <div key={report.id} className="report-card"> <h3>{report.machineName}</h3><p>{report.productName}</p><p>วันที่: {report.productionDate || report.startDate}</p><button className="select-button" onClick={() => handleSelectReport(report.id)}>ดู Dashboard</button> </div> ))} </div>} </div> );
    }
    if (view === 'stock') {
        return <LabelStockManager onBack={() => handleNavigate('main')} />;
    }
    if (view === 'material') {
        return <MaterialStockManager onBack={() => handleNavigate('main')} />;
    }
    if (view === 'ng') {
        return <NgRecording onBack={() => handleNavigate('main')} activeReports={activeReports} />;
    }

    // Default view: 'main'
    return (
        <div className="dashboard-card">
            <NotificationPanel />
            <div className="sl-header">
                <h2 className="dashboard-title">เลือกการทำงาน (Shift Leader)</h2>
            </div>
            <div className="task-choice-container">
                <button className="task-choice-button" onClick={() => handleNavigate('dashboard')}>ดู Dashboard การผลิต</button>
                <button className="task-choice-button" style={{backgroundColor: '#e0f2fe', borderColor: '#38bdf8', color: '#0369a1'}} onClick={() => handleNavigate('material')}>จัดการสต็อกวัตถุดิบ</button>
                <button className="task-choice-button" style={{backgroundColor: '#eef2ff', borderColor: '#818cf8', color: '#4338ca'}} onClick={() => handleNavigate('stock')}>จัดการสต็อกป้าย</button>
                <button className="task-choice-button" style={{backgroundColor: '#fef2f2', borderColor: '#f87171', color: '#991b1b'}} onClick={() => handleNavigate('ng')}>บันทึกของเสีย</button>
            </div>
        </div>
    );
};

export default ShiftLeaderDashboard;