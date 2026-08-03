// =================================================================
// File: src/components/technician/TechnicianDashboard.jsx (ฉบับปรับปรุงตาม Requirement ใหม่)
// =================================================================
import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import _ from 'lodash';
import { Badge, Box, Tab, Tabs } from '@mui/material';
import BuildIcon from '@mui/icons-material/Build';
import AssignmentIcon from '@mui/icons-material/Assignment';
import ParameterChecklistForm from './ParameterChecklistForm';
import PmSchedulePanel from './PmSchedulePanel';
import SetupJobPanel from './SetupJobPanel';
import MachineSelectGrid from '../common/MachineSelectGrid';

// --- API Service ---
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

const NotificationPanel = () => {
    const [alerts, setAlerts] = useState([]);

    const fetchAlerts = async () => {
        try {
            const response = await api.get('/notifications/alerts/active');
            setAlerts(response.data);
        } catch (error) { console.error("Failed to fetch alerts:", error); }
    };

    useEffect(() => {
        fetchAlerts();
        const interval = setInterval(fetchAlerts, 30000);
        return () => clearInterval(interval);
    }, []);

    const handleAcknowledge = async (id) => {
        try {
            await api.post(`/notifications/alerts/${id}/acknowledge`);
            fetchAlerts();
        } catch (error) { alert('เกิดข้อผิดพลาดในการรับทราบการแจ้งเตือน'); }
    };

    if (!alerts || !Array.isArray(alerts) || alerts.length === 0) return null;

    return (
        <div className="notification-panel">
            <h3 className="notification-title"><span role="img" aria-label="alert">🚨</span> การแจ้งเตือนเครื่องจักรหยุด</h3>
            <div className="notification-list">
                {alerts.map(alert => (
                    <div key={alert.id} className="notification-item">
                        <div className="notification-content">
                            <p><strong>เครื่อง:</strong> {alert.machineName} ({alert.productName})</p>
                            <p><strong>สาเหตุ:</strong> {alert.message}</p>
                            <p className="notification-meta">แจ้งโดย: {alert.operatorName} | เวลา: {new Date(alert.timestamp).toLocaleString()}</p>
                        </div>
                        <button onClick={() => handleAcknowledge(alert.id)} className="acknowledge-button">รับทราบ</button>
                    </div>
                ))}
            </div>
        </div>
    );
};

// --- Main Technician Component ---
const scrapOptions = [
    'PE ส่งบดใช้ ถุงสีขาว',
    'PE ส่งบดขาย ถุงสีแดง',
    'PE ส่งขาย ถุงสีฟ้า',
    'PE เพิร์ส ถุงสีฟ้า',
    'PP ส่งบดใช้ ถุงสีขาว',
    'PP ส่งบดขาย ถุงสีแดง',
    'PP ส่งขาย ถุงสีแดง',
    'PP เพิร์ส ถุงสีแดง',
];

const initialParameterFormData = {
    extruderScrew: {
        main: { screwRpm: '', loLimit: '', resinPress: '', motorCurrent: '', resinTemp: '' },
        admer: { screwRpm: '', loLimit: '', resinPress: '', motorCurrent: '', resinTemp: '' },
        evoh: { screwRpm: '', loLimit: '', resinPress: '', motorCurrent: '', resinTemp: '' },
        virgin: { screwRpm: '', loLimit: '', resinPress: '', motorCurrent: '', resinTemp: '' },
    },
    temperature: {
        main: { fb: '', c1: '', c2: '', c3: '', a1: '', a2: '', a3: '', a4: '' },
        admer: { fb: '', c1: '', c2: '', c3: '', a1: '', h1: '', h2: '', h3: '' },
        evoh: { fb: '', c1: '', c2: '', c3: '', a1: '', h1: '', h2: '', h3: '' },
        virgin: { fb: '', c1: '', c2: '', c3: '', a1: '', a2: '', a3: '', a4: '' },
        head: {
            d1_1: '', d2_1: '', d3_1: '', d4_1: '', d1_2: '', d2_2: '', d3_2: '', d4_2: '',
            d1_3: '', d2_3: '', d3_3: '', d4_3: '', d1_4: '', d2_4: '', d3_4: '', d4_4: '',
            l1: '', l2: '', l3: '', l4: ''
        }
    },
    otherValues: {
        cycleTime: '',
        moldTemp: '',
        highBlow: '',
        lowPressure: '',
        blowRate: '',
        blowPinPlatenLeft: { blowPin: '', frontA: '', frontB: '', frontC: '', frontD: '', frontE: '', frontF: '' },
        blowPinPlatenRight: { blowPin: '', frontA: '', frontB: '', frontC: '', frontD: '', frontE: '', frontF: '' },
        blowAirCondition1: '',
        blowAirCondition2: '',
        parisonAir1: '',
        parisonAir2: '',
        zero: '',
        learnWeight: '',
        span: '',
    },
    otherChecks: {
        emergencySw: '',
        sq: '',
        ss: '',
    }
};

const ParameterForm = ({ formData, setFormData, onSubmit, onCancel, timeRecord }) => {

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        const keys = name.split('.');

        const newState = JSON.parse(JSON.stringify(formData));
        let current = newState;
        for (let i = 0; i < keys.length - 1; i++) {
            current = current[keys[i]];
        }

        if (type === 'checkbox') {
            current[keys[keys.length - 1]] = checked ? value : '';
        } else {
            current[keys[keys.length - 1]] = value;
        }

        setFormData(newState);
    };

    const renderInput = (name, placeholder = '', type = 'number') => (
        <input
            type={type}
            step="any"
            name={name}
            value={_.get(formData, name, '')}
            onChange={handleInputChange}
            placeholder={placeholder}
            className="form-input parameter-input"
        />
    );

    const renderExtruderSection = () => (
        <fieldset className="form-fieldset">
            <legend>ส่วนที่ 1: Extruder Screw</legend>
            <div className="grid-4-col">
                {['MAIN (EL3101)', 'ADMER (LO-Left)', 'EVOH (LO-Right)', 'VIRGIN (UPL)'].map((label, i) => {
                    const key = ['main', 'admer', 'evoh', 'virgin'][i];
                    return (
                        <div key={key} className="form-group-vertical">
                            <strong>{label}</strong>
                            <label>Screw rpm</label> {renderInput(`extruderScrew.${key}.screwRpm`)}
                            <label>Lo Limit</label> {renderInput(`extruderScrew.${key}.loLimit`)}
                            <label>Resin Press</label> {renderInput(`extruderScrew.${key}.resinPress`)}
                            <label>Resin Temp</label> {renderInput(`extruderScrew.${key}.resinTemp`)}
                            <label>Motor Current</label> {renderInput(`extruderScrew.${key}.motorCurrent`)}
                        </div>
                    );
                })}
            </div>
        </fieldset>
    );

    const renderTemperatureSection = () => (
        <fieldset className="form-fieldset">
            <legend>ส่วนที่ 2: Temperature (อุณหภูมิ) °C</legend>
            <div className="temp-group">
                <strong>MAIN (FL):</strong>
                <div className="temp-inputs">
                    {['FB', 'C1', 'C2', 'C3', 'C4', 'C5', 'A1', 'A2', 'A3', 'A4'].map(p => <div key={p}><label>{p}</label>{renderInput(`temperature.main.${p.toLowerCase()}`)}</div>)}
                </div>
            </div>
            <div className="temp-group">
                <strong>ADMER (LO-Left):</strong>
                <div className="temp-inputs">
                    {['FB', 'C1', 'C2', 'C3', 'A1', 'A2', 'H1', 'H2', 'H3'].map(p => <div key={p}><label>{p}</label>{renderInput(`temperature.admer.${p.toLowerCase()}`)}</div>)}
                </div>
            </div>
            <div className="temp-group">
                <strong>EVOH (LO-Right):</strong>
                <div className="temp-inputs">
                    {['FB', 'C1', 'C2', 'C3', 'A1', 'A2', 'H1', 'H2', 'H3'].map(p => <div key={p}><label>{p}</label>{renderInput(`temperature.evoh.${p.toLowerCase()}`)}</div>)}
                </div>
            </div>
            <div className="temp-group">
                <strong>VIRGIN (UPL):</strong>
                <div className="temp-inputs">
                    {['FB', 'C1', 'C2', 'C3', 'A1', 'A2', 'A3', 'A4'].map(p => <div key={p}><label>{p}</label>{renderInput(`temperature.virgin.${p.toLowerCase()}`)}</div>)}
                </div>
            </div>
            <div className="temp-group">
                <strong>HEAD:</strong>
                <div className="temp-inputs-grid">
                    {['D1-1', 'D2-1', 'D3-1', 'D4-1', 'D1-2', 'D2-2', 'D3-2', 'D4-2', 'D1-3', 'D2-3', 'D3-3', 'D4-3', 'D1-4', 'D2-4', 'D3-4', 'D4-4', 'L1', 'L2', 'L3', 'L4'].map(p => {
                        const name = `temperature.head.${p.replace('-', '_').toLowerCase()}`;
                        return <div key={p}><label>{p}</label>{renderInput(name)}</div>
                    })}
                </div>
            </div>
        </fieldset>
    );

    const renderOtherValuesSection = () => (
        <fieldset className="form-fieldset">
            <legend>ส่วนที่ 3: ค่าการทำงานอื่นๆ</legend>
            <div className="grid-layout-5-col">
                <div className="form-group-vertical">
                    <label>Cycle time (sec)</label> {renderInput('otherValues.cycleTime')}
                    <label>Mold Temp</label> {renderInput('otherValues.moldTemp')}
                    <label>HIGH BLOW (Mpa)</label> {renderInput('otherValues.highBlow')}
                    <label>LOW PRESSURE (Mpa)</label> {renderInput('otherValues.lowPressure')}
                    <label>Blow ratio</label> {renderInput('otherValues.blowRate')}
                </div>
                <div className="form-group-vertical">
                    <label>Blow Air Condition1</label> {renderInput('otherValues.blowAirCondition1')}
                    <label>Blow Air Condition2</label> {renderInput('otherValues.blowAirCondition2')}
                    <label>Parison Air1</label> {renderInput('otherValues.parisonAir1')}
                    <label>Parison Air2</label> {renderInput('otherValues.parisonAir2')}
                </div>
                <div className="form-group-vertical">
                    <label>Zero</label> {renderInput('otherValues.zero')}
                    <label>Weight</label> {renderInput('otherValues.learnWeight')}
                    <label>Span</label> {renderInput('otherValues.span')}
                </div>
                <div className="form-group-vertical">
                    <strong>Blow Pin PLATEN Left</strong>
                    <label>Blow Pin</label> {renderInput('otherValues.blowPinPlatenLeft.blowPin')}
                    {['A', 'B', 'C', 'D', 'E', 'F'].map(p => <div key={`left-${p}`} className="form-group-horizontal"><label>FRONT({p})</label>{renderInput(`otherValues.blowPinPlatenLeft.front${p}`)}</div>)}
                </div>
                 <div className="form-group-vertical">
                    <strong>Blow Pin PLATEN Right</strong>
                    <label>Blow Pin</label> {renderInput('otherValues.blowPinPlatenRight.blowPin')}
                    {['A', 'B', 'C', 'D', 'E', 'F'].map(p => <div key={`right-${p}`} className="form-group-horizontal"><label>FRONT({p})</label>{renderInput(`otherValues.blowPinPlatenRight.front${p}`)}</div>)}
                </div>
            </div>
        </fieldset>
    );

    const renderOtherChecksSection = () => (
        <fieldset className="form-fieldset">
            <legend>ส่วนที่ 4: การตรวจสอบอื่นๆ</legend>
            <div className="form-group">
                <label>ตรวจสอบการทำงานของ Emergency SW:</label>
                <div className="checkbox-group">
                    <label><input type="checkbox" name="otherChecks.emergencySw" value="OK" checked={formData.otherChecks.emergencySw === 'OK'} onChange={handleInputChange} /> OK</label>
                    <label><input type="checkbox" name="otherChecks.emergencySw" value="NG" checked={formData.otherChecks.emergencySw === 'NG'} onChange={handleInputChange} /> NG</label>
                </div>
            </div>
            <div className="form-group-horizontal" style={{ gap: '20px' }}>
                <div className="form-group">
                    <label>บันทึกตรวจสอบ SQ (จำนวนชิ้น):</label>
                    <input type="number" name="otherChecks.sq" value={_.get(formData, 'otherChecks.sq', '')} onChange={handleInputChange} className="form-input" style={{width: '150px'}} />
                </div>
                <div className="form-group">
                    <label>บันทึกตรวจสอบ SS (จำนวนชิ้น):</label>
                    <input type="number" name="otherChecks.ss" value={_.get(formData, 'otherChecks.ss', '')} onChange={handleInputChange} className="form-input" style={{width: '150px'}} />
                </div>
            </div>
        </fieldset>
    );

    return (
        <form onSubmit={onSubmit} className="parameter-form">
            <h3 className="form-title">บันทึกค่า Parameter ({timeRecord})</h3>
            {renderExtruderSection()}
            {renderTemperatureSection()}
            {renderOtherValuesSection()}
            {renderOtherChecksSection()}
            <div className="form-actions">
                <button type="button" onClick={onCancel} className="cancel-button">ยกเลิก</button>
                <button type="submit" className="save-button">บันทึก</button>
            </div>
        </form>
    );
};

const TimeSelectionScreen = ({ onSelectTime, onBack }) => {
    const timeOptions = ['Standard', '10:00', '18:00', '02:00'];
    return (
        <div className="time-selection-container">
            <button onClick={onBack} className="back-button">&larr; กลับ</button>
            <h3 className="dashboard-title">เลือกรอบเวลาที่ต้องการบันทึก</h3>
            <div className="task-choice-container">
                {timeOptions.map(time => (
                    <button key={time} onClick={() => onSelectTime(time)} className="task-choice-button">{time}</button>
                ))}
            </div>
        </div>
    );
};

const ParameterRecordsView = ({ records, onEdit, onAddNew, onBack }) => (
    <div className="records-view-container">
        <button onClick={onBack} className="back-button">&larr; กลับ</button>
        <div className="records-header">
            <h3 className="dashboard-title">รายการที่บันทึกแล้ว</h3>
            <button onClick={onAddNew} className="add-new-button">+ บันทึกรอบเวลาใหม่</button>
        </div>
        {records.length > 0 ? (
            <div className="parameter-records-list">
                {records.map(record => (
                    <div key={record.id} className="record-card">
                        <div className="record-info">
                            <span className="record-time">{record.recordType}</span>
                            <span className="record-timestamp">บันทึกเมื่อ: {new Date(record.createdAt).toLocaleString()}</span>
                        </div>
                        <button onClick={() => onEdit(record)} className="edit-button">แก้ไข</button>
                    </div>
                ))}
            </div>
        ) : (
            <p>ยังไม่มีการบันทึกข้อมูลสำหรับ Report นี้</p>
        )}
    </div>
);

const TechnicianDashboard = () => {
    const [activeTab, setActiveTab] = useState(0);

    const [activeReports, setActiveReports] = useState([]);
    const [selectedReport, setSelectedReport] = useState(null);
    const [error, setError] = useState('');

    const [currentView, setCurrentView] = useState('reportList');
    const [parameterRecords, setParameterRecords] = useState([]);
    const [currentRecord, setCurrentRecord] = useState(null); 
    const [formData, setFormData] = useState(_.cloneDeep(initialParameterFormData));
    const [timeRecord, setTimeRecord] = useState('');

    const [isDowntimeModalOpen, setIsDowntimeModalOpen] = useState(false);
    const [isNgModalOpen, setIsNgModalOpen] = useState(false);
    const [isScrapModalOpen, setIsScrapModalOpen] = useState(false);
    
    const [downtimeData, setDowntimeData] = useState({ startTime: '', endTime: '', reason: '', solution: '' });
    const [ngTypes, setNgTypes] = useState([]);
    const [ngData, setNgData] = useState({ ngTypeId: '', quantity: 1 });
    const [scrapData, setScrapData] = useState({ scrapDescription: '', weightKg: '' });

    const fetchActiveReports = useCallback(async () => {
        try {
            const [reportsRes, ngTypesRes] = await Promise.all([
                api.get('/pc/reports/active?scope=today'),
                api.get('/master-data/ng-types')
            ]);
            
            // แก้ไข: ตรวจสอบโครงสร้าง response และดึงข้อมูลอย่างถูกต้อง
            console.log('Reports Response:', reportsRes.data);
            console.log('NgTypes Response:', ngTypesRes.data);
            
            const reportsData = reportsRes.data?.data || reportsRes.data || [];
            const ngTypesData = ngTypesRes.data?.data || ngTypesRes.data || [];
            
            setActiveReports(Array.isArray(reportsData) ? reportsData : []);
            setNgTypes(Array.isArray(ngTypesData) ? ngTypesData.filter(ng => ng.ngType === 'Technician') : []);
        } catch (err) {
            console.error('Error fetching data:', err);
            setError('ไม่สามารถดึงข้อมูลเริ่มต้นได้');
        }
    }, []);

    useEffect(() => {
        fetchActiveReports();
    }, [fetchActiveReports]);

    const createChangeHandler = (setter) => (e) => {
        const { name, value } = e.target;
        setter(prev => ({ ...prev, [name]: value }));
    };

    const _handleBackToReportSelection = () => {
        setSelectedReport(null);
        setCurrentView('reportList');
        setActiveTab(1);
        fetchActiveReports();
    };

    const handleDowntimeSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.post(`/technician/reports/${selectedReport.id}/downtime`, {
                ...downtimeData,
                startTime: downtimeData.startTime ? new Date(downtimeData.startTime).toISOString() : null,
                endTime: downtimeData.endTime ? new Date(downtimeData.endTime).toISOString() : null,
            });
            alert('บันทึก Downtime สำเร็จ');
            setIsDowntimeModalOpen(false);
            setDowntimeData({ startTime: '', endTime: '', reason: '', solution: '' });
        } catch (err) { alert(err.response?.data || 'เกิดข้อผิดพลาด'); }
    };
    
    const handleOpenParameterWorkflow = async () => {
        if (!selectedReport) return;
        try {
            console.log(`Fetching parameters for report ID: ${selectedReport.id}`);
            const response = await api.get(`/technician/reports/${selectedReport.id}/parameters`);
            console.log('Parameters response:', response.data);
            const list = Array.isArray(response.data) ? response.data : [];
            setParameterRecords(list);
            // ถ้ามีรายการเดิม ให้ไปหน้ารายการก่อน เพื่อแก้ไข/เพิ่มรอบใหม่
            // ถ้ายังไม่มีข้อมูล ให้พาไปเลือกเวลา (Standard/รอบเวลา) ทันที
            if (list.length > 0) {
                setCurrentView('recordsView');
            } else {
                setCurrentView('timeSelection');
            }
        } catch (err) {
            console.error('Error fetching parameters:', err);
            console.error('Error response:', err.response?.data);
            // ไม่ว่าผลจะเป็นอย่างไร ให้พาไปเลือกเวลา เพื่อให้ Technician บันทึกใหม่ได้ทันที
            setParameterRecords([]);
            setCurrentView('timeSelection');
        }
    };

    const handleSelectTime = (time) => {
        setTimeRecord(time);
        setFormData(_.cloneDeep(initialParameterFormData));
        setCurrentRecord(null); 
        setCurrentView('form');
    };

    const handleEditRecord = (record) => {
        setTimeRecord(record.recordType);
        setFormData(record.parameters); 
        setCurrentRecord(record); 
        setCurrentView('form');
    };

    const handleFormSubmit = async (e) => {
        e.preventDefault();
        const payload = { recordType: timeRecord || 'Standard', parameters: formData };
        try {
            if (currentRecord) {
                await api.put(`/technician/reports/parameters/${currentRecord.id}`, payload);
                alert('แก้ไขข้อมูลสำเร็จ');
            } else {
                await api.post(`/technician/reports/${selectedReport.id}/parameters`, payload);
                alert('บันทึกข้อมูลสำเร็จ');
            }
            // กลับไปหน้ารายการ และรีเฟรชรายการหลังบันทึกสำเร็จ
            await handleOpenParameterWorkflow();
        } catch (err) { alert(err.response?.data?.message || 'เกิดข้อผิดพลาดในการบันทึกข้อมูล'); }
    };

    const handleNgSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.post(`/technician/reports/${selectedReport.id}/ng-logs`, {
                ngTypeId: ngData.ngTypeId,
                quantity: parseInt(ngData.quantity, 10),
                source: 'Technician_Process',
            });
            alert('บันทึกของเสียสำเร็จ');
            setIsNgModalOpen(false);
            setNgData({ ngTypeId: '', quantity: 1 });
        } catch (err) { alert(err.response?.data || 'เกิดข้อผิดพลาด'); }
    };

    const handleScrapSubmit = async (e) => {
        e.preventDefault();
        const { scrapDescription, weightKg } = scrapData;
        
        const parts = scrapDescription.split(' ');
        const matType = parts[0];
        const scrapType = parts.slice(1).join(' ');

        try {
            await api.post(`/technician/reports/${selectedReport.id}/scrap-weight`, {
                matType: matType,
                scrapType: scrapType,
                weightKg: parseFloat(weightKg)
            });
            alert('บันทึกน้ำหนักของเสียสำเร็จ');
            setIsScrapModalOpen(false);
            setScrapData({ scrapDescription: '', weightKg: '' });
        } catch (err) { alert(err.response?.data || 'เกิดข้อผิดพลาด'); }
    };

    const renderCurrentView = () => {
        switch (currentView) {
            case 'timeSelection':
                return <TimeSelectionScreen onSelectTime={handleSelectTime} onBack={() => setCurrentView('reportList')} />;
            case 'form':
                return <ParameterForm formData={formData} setFormData={setFormData} onSubmit={handleFormSubmit} onCancel={() => setCurrentView('reportList')} timeRecord={timeRecord} />;
            case 'recordsView':
                return <ParameterRecordsView records={parameterRecords} onEdit={handleEditRecord} onAddNew={() => setCurrentView('timeSelection')} onBack={() => setCurrentView('reportList')} />;
            default:
                return renderMainTaskView();
        }
    };

    const renderMainTaskView = () => (
        <>
            <button onClick={_handleBackToReportSelection} className="back-button"> &larr; กลับไปหน้ารายการ</button>
            <h2 className="dashboard-title">บันทึกข้อมูลเทคนิค</h2>
            <div className="selected-report-info">
                <span><strong>เครื่องจักร:</strong> {selectedReport.machineName}</span>
                <span><strong>ผลิตภัณฑ์:</strong> {selectedReport.productName}</span>
            </div>
            <div className="task-choice-container technician-tasks" style={{gridTemplateColumns: 'repeat(4, 1fr)'}}>
                <button className="task-choice-button" onClick={() => setIsDowntimeModalOpen(true)}>บันทึกเครื่องหยุด (Downtime)</button>
                <button className="task-choice-button" onClick={handleOpenParameterWorkflow}>บันทึกค่า Parameter</button>
                <button className="task-choice-button" onClick={() => setIsNgModalOpen(true)}>บันทึกของเสีย (Process)</button>
                <button className="task-choice-button" onClick={() => setIsScrapModalOpen(true)}>ชั่งน้ำหนักของเสีย</button>
            </div>
        </>
    );

    if (selectedReport) {
        return (
            <div className="dashboard-card">
                <NotificationPanel />
                {renderCurrentView()}

                {/* Modals */}
                <Modal isOpen={isDowntimeModalOpen} onClose={() => setIsDowntimeModalOpen(false)} title="บันทึกเครื่องหยุด (Downtime)">
                    <form onSubmit={handleDowntimeSubmit} className="downtime-form">
                        <div className="form-group"><label className="form-label">เวลาที่เริ่มหยุด</label><input type="datetime-local" name="startTime" value={downtimeData.startTime} onChange={createChangeHandler(setDowntimeData)} className="form-input" required /></div>
                        <div className="form-group"><label className="form-label">เวลาที่เดินเครื่องต่อ</label><input type="datetime-local" name="endTime" value={downtimeData.endTime} onChange={createChangeHandler(setDowntimeData)} className="form-input" /></div>
                        <div className="form-group"><label className="form-label">สาเหตุ</label><textarea name="reason" value={downtimeData.reason} onChange={createChangeHandler(setDowntimeData)} className="form-input" rows="3" /></div>
                        <div className="form-group"><label className="form-label">การแก้ไข</label><textarea name="solution" value={downtimeData.solution} onChange={createChangeHandler(setDowntimeData)} className="form-input" rows="3" /></div>
                        <div className="form-actions"><button type="button" onClick={() => setIsDowntimeModalOpen(false)} className="cancel-button">ยกเลิก</button><button type="submit" className="save-button">บันทึก</button></div>
                    </form>
                </Modal>

                <Modal isOpen={isScrapModalOpen} onClose={() => setIsScrapModalOpen(false)} title="ชั่งน้ำหนักของเสีย">
                    <form onSubmit={handleScrapSubmit}>
                        <div className="form-group">
                            <label className="form-label">ประเภทของเสีย</label>
                            <select name="scrapDescription" value={scrapData.scrapDescription} onChange={createChangeHandler(setScrapData)} className="form-input" required>
                                <option value="" disabled>-- เลือกประเภท --</option>
                                {scrapOptions.map(option => <option key={option} value={option}>{option}</option>)}
                            </select>
                        </div>
                        <div className="form-group">
                            <label className="form-label">น้ำหนัก (Kg.)</label>
                            <input name="weightKg" type="number" step="0.01" value={scrapData.weightKg} onChange={createChangeHandler(setScrapData)} className="form-input" required />
                        </div>
                        <div className="form-actions">
                            <button type="button" onClick={() => setIsScrapModalOpen(false)} className="cancel-button">ยกเลิก</button>
                            <button type="submit" className="save-button">บันทึก</button>
                        </div>
                    </form>
                </Modal>

                <Modal isOpen={isNgModalOpen} onClose={() => setIsNgModalOpen(false)} title="บันทึกของเสีย (Process)">
                    <form onSubmit={handleNgSubmit}>
                        <div className="form-group">
                            <label className="form-label">ประเภทของเสีย</label>
                            <select name="ngTypeId" value={ngData.ngTypeId} onChange={createChangeHandler(setNgData)} className="form-input" required>
                                <option value="" disabled>-- เลือกประเภท --</option>
                                {ngTypes && Array.isArray(ngTypes) && ngTypes.map(type => <option key={type.id} value={type.id}>{type.ngDescriptionTh}</option>)}
                            </select>
                        </div>
                        <div className="form-group">
                            <label className="form-label">จำนวน</label>
                            <input name="quantity" type="number" value={ngData.quantity} onChange={createChangeHandler(setNgData)} className="form-input" required min="1" />
                        </div>
                        <div className="form-actions">
                            <button type="button" onClick={() => setIsNgModalOpen(false)} className="cancel-button">ยกเลิก</button>
                            <button type="submit" className="save-button">บันทึก</button>
                        </div>
                    </form>
                </Modal>
            </div>
        );
    }

    return (
        <div className="dashboard-card">
            <NotificationPanel />

            {/* ── Tab bar ─────────────────────────────────────────────── */}
            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
                <Tabs
                    value={activeTab}
                    onChange={(_, v) => setActiveTab(v)}
                    variant="fullWidth"
                >
                    <Tab
                        icon={<BuildIcon fontSize="small" />}
                        iconPosition="start"
                        label="งาน Setup"
                        sx={{ fontWeight: 700, minHeight: 48 }}
                    />
                    <Tab
                        icon={
                            <Badge badgeContent={activeReports.length || null} color="warning">
                                <AssignmentIcon fontSize="small" />
                            </Badge>
                        }
                        iconPosition="start"
                        label="บันทึกข้อมูลผลิต"
                        sx={{ fontWeight: 700, minHeight: 48 }}
                    />
                </Tabs>
            </Box>

            {/* ── Tab 0: Setup + PM ───────────────────────────────────── */}
            {activeTab === 0 && (
                <>
                    <SetupJobPanel />
                    <PmSchedulePanel />
                </>
            )}

            {/* ── Tab 1: Production recording ──────────────────────────── */}
            {activeTab === 1 && (
                <>
                    {error && <p className="error-message">{error}</p>}
                    <MachineSelectGrid
                        reports={activeReports ?? []}
                        onSelect={(report) => {
                            setSelectedReport(report);
                            setCurrentView('reportList');
                        }}
                        title="เลือกใบสั่งผลิตเพื่อบันทึกข้อมูล"
                    />
                </>
            )}
        </div>
    );
};

export default TechnicianDashboard;
