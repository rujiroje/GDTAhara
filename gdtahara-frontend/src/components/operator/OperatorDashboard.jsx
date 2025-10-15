// =================================================================
// File: src/components/operator/OperatorDashboard.jsx (ฉบับแก้ไข แสดงยอด NG แยกประเภท)
// =================================================================
import React, { useState, useEffect } from 'react';
import axios from 'axios';

// --- API Service (ควรย้ายไปไฟล์กลาง) ---
const API_URL = 'http://localhost:8080/api';
const api = axios.create({ baseURL: API_URL });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) { config.headers.Authorization = `Bearer ${token}`; }
    return config;
}, error => Promise.reject(error));

// --- Shared Components (ควรย้ายไปไฟล์กลาง) ---
const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return ( <div className="modal-overlay"> <div className="modal-content"> <div className="modal-header"> <h3>{title}</h3> <button onClick={onClose} className="modal-close-button">&times;</button> </div> <div className="modal-body">{children}</div> </div> </div> );
};


// --- Main Operator Component ---
const OperatorDashboard = () => {
    const [activeReports, setActiveReports] = useState([]);
    const [selectedReport, setSelectedReport] = useState(null);
    const [error, setError] = useState('');
    const [ngTypes, setNgTypes] = useState([]);
    const [packagingInfo, setPackagingInfo] = useState({ lotNumber: '', boxNo: '' });
    const [isAlertModalOpen, setIsAlertModalOpen] = useState(false);
    const [alertReason, setAlertReason] = useState('');
    const [activeTask, setActiveTask] = useState(null);
    const [isLotLocked, setIsLotLocked] = useState(false);
    const [hourlyNgCount, setHourlyNgCount] = useState({});
    
    
    // **[แก้ไข]** Effect สำหรับดึงข้อมูล NG เริ่มต้น และตั้งเวลา Reset
    useEffect(() => {
        if (activeTask === 'ng' && selectedReport) {
            const fetchInitialNgCount = async () => {
                try {
                    const response = await api.get(`/operator/reports/${selectedReport.id}/hourly-ng-summary`);
                    setHourlyNgCount(response.data || {});
                } catch (err) {
                    console.error("Could not fetch initial NG count", err);
                }
            };
            fetchInitialNgCount();
        }
        
        const now = new Date();
        const minutesToNextHour = 60 - now.getMinutes();
        const secondsToNextHour = (minutesToNextHour * 60) - now.getSeconds();
        const timeoutId = setTimeout(() => {
            setHourlyNgCount({});
            const intervalId = setInterval(() => setHourlyNgCount({}), 3600000);
            return () => clearInterval(intervalId);
        }, secondsToNextHour * 1000);
        return () => clearTimeout(timeoutId);
    }, [activeTask, selectedReport]);

    useEffect(() => {
        const fetchActiveReports = async () => {
            try {
                console.log('Fetching active reports for operator...');
                // แก้ไขจาก /pc/ เป็น /operator/
                const response = await api.get('/operator/reports/active');
                console.log('Operator active reports response:', response.data);
                setActiveReports(response.data);
                setError(''); // Clear any previous errors
            } catch (err) {
                console.error('Error fetching active reports:', err);
                setError('ไม่สามารถดึงรายการใบสั่งผลิตได้: ' + (err.response?.data?.message || err.message));
            }
        };
        fetchActiveReports();
    }, []);

    useEffect(() => {
        if (selectedReport) {
            const fetchNgTypes = async () => {
                try {
                    // แก้ไขจาก /master-data/ เป็น /operator/
                    const response = await api.get('/operator/ng-types');
                    console.log('NG Types response:', response.data);
                    
                    // ข้อมูลจาก operator endpoint มา filtered แล้ว ไม่ต้อง filter เพิ่ม
                    let operatorNg = response.data || [];
                    
                    console.log('Operator NG Types from backend:', operatorNg);
                    console.log('Count from backend:', operatorNg.length);
                    
                    const sortedNg = operatorNg.sort((a, b) => {
                        if (a.ngDescriptionTh && a.ngDescriptionTh.includes('ปัญหาอื่นๆ')) return 1;
                        if (b.ngDescriptionTh && b.ngDescriptionTh.includes('ปัญหาอื่นๆ')) return -1;
                        return 0;
                    });
                    setNgTypes(sortedNg);
                } catch (err) {
                    console.error("Could not fetch NG types", err);
                    setError('ไม่สามารถดึงประเภท NG ได้: ' + (err.response?.data?.message || err.message));
                }
            };
            fetchNgTypes();
        }
    }, [selectedReport]);
    
    useEffect(() => {
        const fetchNextBoxNo = async () => {
            if (selectedReport && packagingInfo.lotNumber) {
                try {
                    const response = await api.get(`/operator/reports/${selectedReport.id}/next-box-no?lotNumber=${packagingInfo.lotNumber}`);
                    setPackagingInfo(prev => ({ ...prev, boxNo: response.data }));
                } catch (err) {
                    console.error("Could not fetch next box number", err);
                    setPackagingInfo(prev => ({ ...prev, boxNo: 'Error' }));
                }
            }
        };
        
        const timerId = setTimeout(() => { if(packagingInfo.lotNumber) fetchNextBoxNo(); }, 500);
        return () => clearTimeout(timerId);
    }, [packagingInfo.lotNumber, selectedReport]);


    const handleRecordNg = async (ngTypeId, ngDescription) => {
        try {
            await api.post(`/operator/reports/${selectedReport.id}/ng-logs`, { ngTypeId: ngTypeId, quantity: 1, source: 'Operator_Run' });
            // **[แก้ไข]** อัปเดต State ของ NG แต่ละชนิด
            setHourlyNgCount(prevCounts => ({
                ...prevCounts,
                [ngDescription]: (prevCounts[ngDescription] || 0) + 1
            }));
        } catch (err) {
            alert(err.response?.data || 'เกิดข้อผิดพลาดในการบันทึก NG');
        }
    };

    // ... (โค้ดส่วนอื่น ๆ เหมือนเดิม)
    const handleRecordPackaging = async () => {
        if (!packagingInfo.lotNumber || !packagingInfo.boxNo) { alert('กรุณากรอก Lot Number'); return; }
        try {
            await api.post(`/operator/reports/${selectedReport.id}/packaging-logs`, { lotNumber: packagingInfo.lotNumber, boxNo: parseInt(packagingInfo.boxNo, 10) });
            setIsLotLocked(true);
            const response = await api.get(`/operator/reports/${selectedReport.id}/next-box-no?lotNumber=${packagingInfo.lotNumber}`);
            setPackagingInfo(prev => ({ ...prev, boxNo: response.data }));
        } catch (err) {
            alert(err.response?.data || 'เกิดข้อผิดพลาดในการบันทึกการแพ็ค');
        }
    };
    
    const handleAlertSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.post(`/operator/reports/${selectedReport.id}/alert`, { message: alertReason });
            alert('แจ้งปัญหาสำเร็จ');
            setIsAlertModalOpen(false);
            setAlertReason('');
        } catch (err) {
            alert(err.response?.data || 'เกิดข้อผิดพลาดในการแจ้งปัญหา');
        }
    };
    
    const handleBackToSelection = () => { setSelectedReport(null); setActiveTask(null); setIsLotLocked(false); setPackagingInfo({ lotNumber: '', boxNo: '' }); };
    const handleBackToTaskChoice = () => { setActiveTask(null); };

    const renderTaskChoice = () => (
        <>
            <button onClick={handleBackToSelection} className="back-button"> &larr; กลับไปหน้ารายการ</button>
            <h2 className="dashboard-title">เลือกการทำงาน</h2>
             <div className="selected-report-info">
                <span><strong>เครื่องจักร:</strong> {selectedReport.machineName}</span>
                <span><strong>ผลิตภัณฑ์:</strong> {selectedReport.productName}</span>
            </div>
            <div className="task-choice-container">
                <button className="task-choice-button" onClick={() => setActiveTask('ng')}>รายงานของเสีย</button>
                <button className="task-choice-button" onClick={() => setActiveTask('packaging')}>บันทึกการบรรจุ</button>
            </div>
        </>
    );

    const renderNgRecording = () => (
        <>
            <button onClick={handleBackToTaskChoice} className="back-button"> &larr; กลับไปเลือกการทำงาน</button>
            <h3 className="dashboard-subtitle">บันทึกของเสีย (NG)</h3>
            <div className="ng-buttons-container">
                {ngTypes.map((ng, index) => (
                    <button 
                        key={ng.id} 
                        className={`ng-button ng-color-${index % 10}`} 
                        // **[แก้ไข]** ส่ง ng.ngDescriptionTh ไปด้วย
                        onClick={() => handleRecordNg(ng.id, ng.ngDescriptionTh)}
                    >
                        {ng.ngDescriptionTh}
                        {/* **[ใหม่]** แสดง Badge ตัวเลขของแต่ละชนิด */}
                        {hourlyNgCount[ng.ngDescriptionTh] > 0 && (
                            <span className="ng-count-badge">{hourlyNgCount[ng.ngDescriptionTh]}</span>
                        )}
                    </button>
                ))}
            </div>
             
            <div className="hourly-summary-container">
                {/* **[แก้ไข]** เปลี่ยนการแสดงผลเป็นตารางสรุป */}
                <h4>สรุปยอด NG ในชั่วโมงนี้</h4>
                {Object.keys(hourlyNgCount).length > 0 ? (
                    <table className="summary-table">
                        <tbody>
                            {Object.entries(hourlyNgCount).map(([key, value]) => (
                                <tr key={key}>
                                    <td>{key}</td>
                                    <td>{value} ชิ้น</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <p>ยังไม่มีการบันทึกของเสียในชั่วโมงนี้</p>
                )}
                <p>(ยอดรวมจะถูกรีเซ็ตทุกต้นชั่วโมง)</p>
            </div>
            <div className="downtime-alert-section">
                <button className="downtime-button" onClick={() => setIsAlertModalOpen(true)}>
                    แจ้งปัญหา
                </button>
            </div>
        </>
    );
    
    // ... (โค้ด renderPackagingRecording และส่วนอื่นๆ เหมือนเดิม)
    const renderPackagingRecording = () => (
        <>
            <button onClick={handleBackToTaskChoice} className="back-button"> &larr; กลับไปเลือกการทำงาน</button>
            <h3 className="dashboard-subtitle">บันทึกการบรรจุ (Packaging)</h3>
            <div className="form-group">
                <div className="lot-number-header">
                    <label className="form-label">Lot Number</label>
                    {isLotLocked && ( <button className="edit-lot-button" onClick={() => setIsLotLocked(false)}>แก้ไข</button> )}
                </div>
                <input name="lotNumber" value={packagingInfo.lotNumber} onChange={(e) => setPackagingInfo({ lotNumber: e.target.value, boxNo: '' })} className="form-input" disabled={isLotLocked}/>
            </div>
            <div className="packaging-grid">
                <div className="box-display">
                    <label className="form-label">Box No.</label>
                    <div className="box-number">{packagingInfo.boxNo || '-'}</div>
                </div>
                <button className="submit-button pack-button" onClick={handleRecordPackaging}>บันทึก 1 กล่อง</button>
            </div>
            <div className="downtime-alert-section">
                <button className="downtime-button" onClick={() => setIsAlertModalOpen(true)}>
                    แจ้งปัญหา
                </button>
            </div>
        </>
    );

    if (selectedReport) {
        return ( <div className="dashboard-card"> {activeTask === null && renderTaskChoice()} {activeTask === 'ng' && renderNgRecording()} {activeTask === 'packaging' && renderPackagingRecording()} <Modal isOpen={isAlertModalOpen} onClose={() => setIsAlertModalOpen(false)} title="แจ้งปัญหา"> <form onSubmit={handleAlertSubmit}> <div className="form-group"> <label className="form-label">กรุณาระบุเหตุผล</label> <textarea value={alertReason} onChange={(e) => setAlertReason(e.target.value)} className="form-input" rows="4" required /> </div> <div className="form-actions"> <button type="button" onClick={() => setIsAlertModalOpen(false)} className="cancel-button">ยกเลิก</button> <button type="submit" className="save-button">ยืนยันการแจ้ง</button> </div> </form> </Modal> </div> );
    }

    return ( <div className="dashboard-card"> 
        <h2 className="dashboard-title">เลือกใบสั่งผลิตเพื่อเริ่มทำงาน</h2> 
        
        {/* Debug Information */}
        <div style={{ padding: '10px', backgroundColor: '#f5f5f5', marginBottom: '20px', borderRadius: '5px' }}>
            <h4 style={{ color: '#1976d2' }}>Operator System Status</h4>
            <p>Active Reports: {activeReports.length}</p>
            <p>Status: {error ? 'Error' : 'Connected'}</p>
            {error && <p style={{ color: 'red' }}>Error: {error}</p>}
        </div>
        
        {error && <p className="error-message">{error}</p>} 
        {activeReports.length === 0 && !error && <p>ไม่มีใบสั่งผลิตที่กำลังทำงานอยู่</p>} 
        <div className="report-selection-container"> {activeReports.map(report => ( <div key={report.id} className="report-card"> <h3>{report.machineName}</h3> <p>{report.productName}</p> <p>วันที่: {report.productionDate || report.startDate}</p> <button className="select-button" onClick={() => setSelectedReport(report)}> เลือก </button> </div> ))} </div> </div> );
};

export default OperatorDashboard;