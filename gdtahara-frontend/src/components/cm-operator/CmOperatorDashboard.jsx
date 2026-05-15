// =================================================================
// File: src/components/cm-operator/CmOperatorDashboard.jsx (ฉบับสมบูรณ์ แก้ไข handleChange)
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


const CmOperatorDashboard = () => {
    const [activeReports, setActiveReports] = useState([]);
    const [materials, setMaterials] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    const [selectedReport, setSelectedReport] = useState(null);
    const [selectedMaterialType, setSelectedMaterialType] = useState('');
    
    const initialFormData = { productionReportId: '', materialId: '', lotNumber: '', quantity: '' };
    const [formData, setFormData] = useState(initialFormData);
    const [lotNumbers, setLotNumbers] = useState([]);
    const [loadingLots, setLoadingLots] = useState(false);

    useEffect(() => {
        const fetchInitialData = async () => {
            setLoading(true);
            try {
                const [reportsRes, materialsRes] = await Promise.all([
                    api.get('/pc/reports/active'),
                    api.get('/master-data/materials')
                ]);
                
                // แก้ไข: ตรวจสอบโครงสร้าง response และดึงข้อมูลอย่างถูกต้อง
                console.log('Reports Response:', reportsRes.data);
                console.log('Materials Response:', materialsRes.data);
                
                const reportsData = reportsRes.data?.data || reportsRes.data || [];
                const materialsData = materialsRes.data?.data || materialsRes.data || [];
                
                console.log("Detailed Materials Data:", materialsData);
                console.log("Sample materials:", materialsData.slice(0, 3));
                if (materialsData.length > 0) {
                    console.log("First material structure:", JSON.stringify(materialsData[0], null, 2));
                    console.log("All material types found:", materialsData.map(m => m.materialType));
                }
                
                setActiveReports(Array.isArray(reportsData) ? reportsData : []);
                setMaterials(Array.isArray(materialsData) ? materialsData : []);
            } catch (err) {
                console.error('Error fetching initial data:', err);
                setError('ไม่สามารถดึงข้อมูลเริ่มต้นได้');
            } finally {
                setLoading(false);
            }
        };
        fetchInitialData();
    }, []);

    const fetchLotNumbers = async (materialId) => {
        if (!materialId) {
            setLotNumbers([]);
            return;
        };
        setLoadingLots(true);
        try {
            const response = await api.get(`/cm-operator/materials/${materialId}/lot-numbers`);
            setLotNumbers(response.data);
        } catch (error) {
            console.error("Failed to fetch lot numbers", error);
            setLotNumbers([]);
        } finally {
            setLoadingLots(false);
        }
    };
    
    const handleChange = (e) => {
        const { name, value } = e.target;
        
        // **[แก้ไข]** ใช้ if / else if / else เพื่อให้ Logic ถูกต้อง
        if (name === 'materialType') {
            setSelectedMaterialType(value);
            setFormData(prev => ({ ...prev, materialId: '', lotNumber: '' }));
            setLotNumbers([]); // ล้างค่า Lot Number เดิม
        } else if (name === 'materialId') {
            setFormData(prev => ({ ...prev, materialId: value, lotNumber: '' }));
            fetchLotNumbers(value); // ดึง Lot Number ใหม่
        } else {
            setFormData(prev => ({ ...prev, [name]: value }));
        }
    };
    
    const handleSelectReport = (report) => {
        setSelectedReport(report);
        setFormData(prev => ({ ...initialFormData, productionReportId: report.id }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.post('/cm-operator/stock-out', formData);
            alert('บันทึกการเบิกจ่ายสำเร็จ!');
            setFormData(prev => ({ ...initialFormData, productionReportId: prev.productionReportId }));
            setSelectedMaterialType('');
            setLotNumbers([]);
        } catch (err) {
            alert(err.response?.data || 'เกิดข้อผิดพลาดในการบันทึก');
        }
    };

    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูล...</h2></div>;

    // --- View 1: Select Production Report ---
    if (!selectedReport) {
        return (
            <div className="dashboard-card">
                <h2 className="dashboard-title">ขั้นตอนที่ 1: เลือกใบสั่งผลิตเพื่อบันทึกการใช้วัตถุดิบ</h2>
                {error && <p className="error-message">{error}</p>}
                {(!activeReports || activeReports.length === 0) && !error && <p>ไม่มีใบสั่งผลิตที่กำลังทำงานอยู่</p>}
                <div className="report-selection-container">
                    {activeReports && Array.isArray(activeReports) && activeReports.map(report => (
                        <div key={report.id} className="report-card">
                            <h3>{report.machineName}</h3>
                            <p>{report.productName}</p>
                            {report.orderNumber && <p>Order No.: {report.orderNumber}</p>}
                            <p>วันที่: {report.startDate} – {report.endDate}</p>
                            <button className="select-button" onClick={() => handleSelectReport(report)}>เลือก</button>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    // --- View 2: Material Usage Form ---
    // สร้าง materialTypes จากข้อมูลจริงในฐานข้อมูล
    const materialTypes = materials && Array.isArray(materials) 
        ? [...new Set(materials.map(m => m.materialType).filter(type => type && type.trim() !== ''))]
        : ["VIRGIN", "ADMER", "EVOH", "MIX"]; // fallback เดิม
        
    const filteredMaterials = (materials && Array.isArray(materials)) 
        ? materials.filter(m => m.materialType && m.materialType.trim() === selectedMaterialType)
        : [];
    
    // Debug logging สำหรับ materials - ปรับปรุงให้แสดงข้อมูลที่มีประโยชน์
    console.log('=== Materials Debug Info ===');
    console.log('Total Materials:', materials ? materials.length : 0);
    console.log('Available Material Types:', materialTypes);
    console.log('Selected Material Type:', selectedMaterialType);
    console.log('Filtered Materials Count:', filteredMaterials.length);
    
    if (materials && materials.length > 0) {
        console.log('First 3 materials sample:', materials.slice(0, 3));
        const uniqueTypes = [...new Set(materials.map(m => m.materialType))];
        console.log('All unique material types found:', uniqueTypes);
        
        // แสดงจำนวนวัตถุดิบในแต่ละประเภท
        uniqueTypes.forEach(type => {
            const count = materials.filter(m => m.materialType === type).length;
            console.log(`Type "${type}": ${count} materials`);
        });
    }
    
    if (selectedMaterialType) {
        console.log(`Looking for materials with type: "${selectedMaterialType}"`);
        console.log('Filtered materials found:', filteredMaterials.length);
        if (filteredMaterials.length > 0) {
            console.log('Sample filtered materials:', filteredMaterials.slice(0, 2));
        } else {
            console.log('No exact matches found. Checking for partial matches...');
            const partialMatches = materials.filter(m => 
                m.materialType && m.materialType.toLowerCase().includes(selectedMaterialType.toLowerCase())
            );
            console.log('Partial matches:', partialMatches.length);
        }
    }

    return (
        <div className="dashboard-card">
            <button onClick={() => setSelectedReport(null)} className="back-button">&larr; กลับไปเลือกใบสั่งผลิต</button>
            <h2 className="dashboard-title">ขั้นตอนที่ 2: บันทึกการเบิกจ่ายวัตถุดิบ (Stock-Out)</h2>
            <div className="selected-report-info">
                <span><strong>Order No.:</strong> {selectedReport.orderNumber}</span>
                <span><strong>เครื่องจักร:</strong> {selectedReport.machineName}</span>
                <span><strong>ผลิตภัณฑ์:</strong> {selectedReport.productName}</span>
            </div>

            <form onSubmit={handleSubmit} className="production-form" style={{marginTop: '2rem'}}>
                <div className="form-group">
                    <label className="form-label">เลือกชนิดวัตถุดิบ</label>
                    <select name="materialType" value={selectedMaterialType} onChange={handleChange} className="form-input" required>
                        <option value="" disabled>-- เลือกชนิด --</option>
                        {materialTypes.length > 0 ? (
                            materialTypes.map(type => <option key={type} value={type}>{type}</option>)
                        ) : (
                            <option value="" disabled>ไม่มีข้อมูลชนิดวัตถุดิบ</option>
                        )}
                    </select>
                </div>

                <div className="form-group">
                    <label className="form-label">เลือกวัตถุดิบ</label>
                    <select name="materialId" value={formData.materialId} onChange={handleChange} className="form-input" required disabled={!selectedMaterialType}>
                        <option value="" disabled>-- เลือกวัตถุดิบ --</option>
                        {filteredMaterials.length > 0 ? (
                            filteredMaterials.map(m => <option key={m.id} value={m.id}>{m.materialName} ({m.materialCode})</option>)
                        ) : selectedMaterialType ? (
                            <option value="" disabled>ไม่มีวัตถุดิบในประเภทนี้</option>
                        ) : null}
                    </select>
                    {selectedMaterialType && filteredMaterials.length === 0 && (
                        <small style={{color: 'orange', marginTop: '5px', display: 'block'}}>
                            ไม่พบวัตถุดิบในประเภท "{selectedMaterialType}" กรุณาติดต่อ Admin
                        </small>
                    )}
                </div>

                <div className="form-group">
                    <label className="form-label">เลือก Lot Number</label>
                    <select name="lotNumber" value={formData.lotNumber} onChange={handleChange} className="form-input" required disabled={!formData.materialId || loadingLots}>
                        <option value="" disabled>-- {loadingLots ? "กำลังโหลด..." : "เลือก Lot Number"} --</option>
                        {lotNumbers && Array.isArray(lotNumbers) && lotNumbers.map(lot => <option key={lot} value={lot}>{lot}</option>)}
                    </select>
                </div>
                
                <div className="form-group">
                    <label className="form-label">จำนวนที่ใช้ (Kg.)</label>
                    <input type="number" step="0.01" name="quantity" value={formData.quantity} onChange={handleChange} className="form-input" required />
                </div>

                <button type="submit" className="submit-button">บันทึกการเบิกจ่าย</button>
            </form>
        </div>
    );
};

export default CmOperatorDashboard;