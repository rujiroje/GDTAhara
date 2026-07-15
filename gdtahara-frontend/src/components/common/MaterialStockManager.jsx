import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8080/api';
const api = axios.create({ baseURL: API_URL });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) { config.headers.Authorization = `Bearer ${token}`; }
    return config;
}, error => Promise.reject(error));

const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return ( <div className="modal-overlay"> <div className="modal-content"> <div className="modal-header"> <h3>{title}</h3> <button onClick={onClose} className="modal-close-button">&times;</button> </div> <div className="modal-body">{children}</div> </div> </div> );
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
            const unit = materials.find(m => m.id === selectedStockCard.materialId)?.unit || '';

            let balance = selectedStockCard.currentStock;
            const historyWithBalance = selectedStockCard.history.map(tx => {
                const runningBalance = balance;
                balance = tx.transactionType === 'IN' ? balance - tx.quantity : balance + tx.quantity;
                return { ...tx, runningBalance };
            });

            const fmtDate = (ts) => {
                if (!ts) return '-';
                const d = new Date(ts);
                if (isNaN(d.getTime())) return String(ts);
                return d.toLocaleString('th-TH', { dateStyle: 'short', timeStyle: 'short' });
            };

            return (
                <div>
                    <button onClick={() => setSelectedStockCard(null)} className="back-button-sl">&larr; กลับไปหน้ารวม</button>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', margin: '16px 0 20px' }}>
                        <div>
                            <h2 className="dashboard-title" style={{ marginBottom: 4 }}>
                                Stock Card: {selectedStockCard.materialName} ({selectedStockCard.materialCode})
                            </h2>
                            <span style={{ color: '#64748b', fontSize: '0.875rem' }}>
                                รายการเคลื่อนไหวสต็อกทั้งหมด {selectedStockCard.history.length} รายการ
                            </span>
                        </div>
                        <div style={{ background: 'linear-gradient(135deg,#1e40af,#1d4ed8)', color: '#fff', borderRadius: 12, padding: '14px 28px', textAlign: 'center', boxShadow: '0 4px 12px rgba(30,64,175,0.3)' }}>
                            <div style={{ fontSize: '0.75rem', opacity: 0.85, marginBottom: 2 }}>ยอดคงเหลือปัจจุบัน</div>
                            <div style={{ fontSize: '2rem', fontWeight: 700, lineHeight: 1 }}>{selectedStockCard.currentStock.toLocaleString()}</div>
                            <div style={{ fontSize: '0.85rem', marginTop: 2 }}>{unit}</div>
                        </div>
                    </div>

                    <div className="data-table-container">
                        <table className="data-table">
                            <thead>
                                <tr style={{ background: '#f8fafc' }}>
                                    <th>วันที่/เวลา</th>
                                    <th style={{ textAlign: 'center' }}>ประเภท</th>
                                    <th style={{ textAlign: 'right', color: '#16a34a' }}>รับเข้า</th>
                                    <th style={{ textAlign: 'right', color: '#dc2626' }}>จ่ายออก</th>
                                    <th style={{ textAlign: 'right', color: '#1e40af' }}>คงเหลือ</th>
                                    <th>Lot Number</th>
                                    <th>ข้อมูลการผลิต</th>
                                    <th>ผู้บันทึก</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {historyWithBalance.map(tx => {
                                    const isIn = tx.transactionType === 'IN';
                                    return (
                                        <tr key={tx.id} style={{ background: isIn ? '#f0fdf4' : tx.runningBalance < 0 ? '#fff1f2' : '#fffbeb' }}>
                                            <td style={{ whiteSpace: 'nowrap' }}>{fmtDate(tx.timestamp)}</td>
                                            <td style={{ textAlign: 'center' }}>
                                                <span style={{
                                                    background: isIn ? '#dcfce7' : '#fee2e2',
                                                    color: isIn ? '#15803d' : '#dc2626',
                                                    padding: '2px 12px', borderRadius: 12,
                                                    fontWeight: 700, fontSize: '0.78rem', letterSpacing: '0.05em'
                                                }}>
                                                    {isIn ? 'IN' : 'OUT'}
                                                </span>
                                            </td>
                                            <td style={{ textAlign: 'right', color: '#16a34a', fontWeight: isIn ? 700 : 400 }}>
                                                {isIn ? `+${tx.quantity.toLocaleString()}` : ''}
                                            </td>
                                            <td style={{ textAlign: 'right', color: '#dc2626', fontWeight: !isIn ? 700 : 400 }}>
                                                {!isIn ? `-${tx.quantity.toLocaleString()}` : ''}
                                            </td>
                                            <td style={{ textAlign: 'right', fontWeight: 700, color: tx.runningBalance < 0 ? '#dc2626' : '#1e40af' }}>
                                                {tx.runningBalance.toLocaleString()} {unit}
                                            </td>
                                            <td>{tx.lotNumber}</td>
                                            <td>{tx.productionInfo}</td>
                                            <td>{tx.userName}</td>
                                            <td className="actions-cell">
                                                {isIn && <button className="edit-button" onClick={() => handleOpenEditModal(tx)}>แก้ไข</button>}
                                            </td>
                                        </tr>
                                    );
                                })}
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

export default MaterialStockManager;
