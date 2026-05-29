import React, { useState, useEffect } from 'react';
import Chart from 'react-apexcharts';
import axios from 'axios';
import { t, getLang } from '../../i18n/t';
import { useAuth } from '../../App';

// --- API Service ---
const API_URL = 'http://localhost:8080/api';
const api = axios.create({
    baseURL: API_URL,
    withCredentials: true, // ส่ง Credentials (เช่น Cookies) ไปกับทุก Request
});

api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    // Attach language info consistently so backend localizes NG names correctly
    try {
        const lang = (localStorage.getItem('lang') || '').toLowerCase().startsWith('en') ? 'en' : 'th';
        if (!config.params) config.params = {};
        config.params.lang = lang;
        config.headers['Accept-Language'] = lang;
    } catch {}
    return config;
}, error => Promise.reject(error));

api.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error("API Error:", error.response || error.message);
        return Promise.reject(error);
    }
);

// --- Shared Components ---
const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <div className="modal-header">
                    <h3>{title}</h3>
                    <button onClick={onClose} className="modal-close-button">
                        &times;
                    </button>
                </div>
                <div className="modal-body">
                    {children}
                </div>
            </div>
        </div>
    );
};

const ShiftDataDisplay = ({ title, data }) => {
    // Add null check to prevent errors
    if (!data) {
        return (
            <div className="shift-data-container" style={{background: '#fff', padding: '1.5rem', borderRadius: '0.75rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', marginBottom: '1.5rem'}}>
                <h3>{title}</h3>
                <div className="no-data-message" style={{textAlign: 'center', padding: '2rem', color: '#666'}}>
                    <p>📋 {t('noData')}</p>
                    <small>{t('selectOtherDate')}</small>
                </div>
            </div>
        );
    }
    
    return (
        <div className="shift-data-container" style={{background: '#fff', padding: '1.5rem', borderRadius: '0.75rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', marginBottom: '1.5rem'}}>
            <h3>{title}</h3>
            <div className="kpi-grid" style={{gridTemplateColumns: 'repeat(3, 1fr)'}}>
                <div className="kpi-card">
                    <span className="kpi-label">{t('goodPiecesLabel')}</span>
                    <span className="kpi-value">{data.goodProductionPieces?.toLocaleString() || 0}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label">{t('ngPiecesLabel')}</span>
                    <span className="kpi-value ng-value">{data.ngProductionPieces?.toLocaleString() || 0}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label">{t('totalPiecesLabel')}</span>
                    <span className="kpi-value">{data.totalProductionPieces?.toLocaleString() || 0}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label">{t('goodBoxesLabel')}</span>
                    <span className="kpi-value">{data.goodProductionBoxes?.toLocaleString() || 0}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label">{t('yieldLabel')}</span>
                    <span className="kpi-value yield-value">{data.yieldPercentage || '0.00%'}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label">{t('scrapWeightLabel')}</span>
                    <span className="kpi-value ng-value">{data.totalScrapWeight?.toFixed(2) || 0}</span>
                </div>
            </div>
        </div>
    );
};

const DataSummaryPanel = ({ title, data }) => {
    // Handle both array (original usage) and object (shift data) formats
    const isShiftData = data && !Array.isArray(data) && typeof data === 'object';
    const isArrayData = Array.isArray(data);
    
    if (!data || (isArrayData && data.length === 0) || (isShiftData && !data.ngSummary && !data.downtimeHistory && !data.materialUsageLogs)) {
        return (
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h4 className="summary-title">{title}</h4>
                <div className="no-data-message" style={{textAlign: 'center', padding: '2rem', color: '#666'}}>
                    <p>ไม่มีข้อมูล</p>
                </div>
            </div>
        );
    }
    
    // For shift data object format
    if (isShiftData) {
        // Helper to format NG time from backend-provided fields
        const formatNgTime = (entry) => {
            if (!entry) return 'N/A';
            const raw = entry.timeDisplay || entry.time || entry.timestamp;
            if (!raw) return 'N/A';
            // If backend already formatted (e.g., dd/MM/yyyy HH:mm:ss), just show it
            if (typeof raw === 'string' && /\d{1,2}\/\d{1,2}\/\d{2,4}/.test(raw)) {
                return raw;
            }
            // Otherwise try to parse as ISO/date string
            const d = new Date(raw);
            if (!isNaN(d.getTime())) {
                const locale = getLang() === 'th' ? 'th-TH' : 'en-US';
                return d.toLocaleString(locale);
            }
            // Fallback to raw string
            return String(raw);
        };
        // Debug: แสดงข้อมูลที่ได้รับ
        console.log('🔍 DataSummaryPanel received shift data:', data);
        console.log('📊 NG Summary:', data.ngSummary);
        console.log('📏 NG Summary length:', data.ngSummary?.length);
        if (data.ngSummary && data.ngSummary.length > 0) {
            console.log('🎯 NG Summary sample item:', data.ngSummary[0]);
            console.log('🔑 NG Summary fields:', Object.keys(data.ngSummary[0]));
        }
        console.log('⏰ Downtime History:', data.downtimeHistory);
        console.log('📏 Downtime History length:', data.downtimeHistory?.length);
        if (data.downtimeHistory && data.downtimeHistory.length > 0) {
            console.log('🎯 Downtime sample item:', data.downtimeHistory[0]);
            console.log('🔑 Downtime fields:', Object.keys(data.downtimeHistory[0]));
        }
        console.log('🏭 Material Usage Logs:', data.materialUsageLogs);
        console.log('📏 Material Usage Logs length:', data.materialUsageLogs?.length);
        if (data.materialUsageLogs && data.materialUsageLogs.length > 0) {
            console.log('🎯 Material sample item:', data.materialUsageLogs[0]);
            console.log('🔑 Material fields:', Object.keys(data.materialUsageLogs[0]));
        }
        
        return (
            <>
                <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                    <h4 className="summary-title">{title} - {t('ngSummaryTitle')}</h4>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>{t('timeLabel')}</th>
                                    <th>{t('ngTypeLabel')}</th>
                                    <th>{t('countLabel')}</th>
                                    <th>{t('percentageLabel')}</th>
                                    <th>{t('recorderLabel')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {Array.isArray(data.ngSummary) && data.ngSummary.length > 0 ? data.ngSummary.map((log, i) => {
                                    console.log(`🎯 Rendering NG row ${i}:`, log);
                                    return (
                                        <tr key={i}>
                                            <td>{formatNgTime(log)}</td>
                                            <td>{getNgDescription(log)}</td>
                                            <td>{log.count || log.totalQuantity || 0}</td>
                                            <td>{
                                                typeof log.percentage === 'number'
                                                    ? `${log.percentage.toFixed(2)}%`
                                                    : (log.percentage ? `${log.percentage}%` : 'N/A')
                                            }</td>
                                            <td>System</td>
                                        </tr>
                                    );
                                }) : (
                                    <tr>
                                        <td colSpan="5" style={{ textAlign: 'center', color: '#666' }}>
                                            {t('noData')}
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                    <h4 className="summary-title">{title} - {t('downtimeSummaryTitle')}</h4>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>{t('startTime')}</th>
                                    <th>{t('endTime')}</th>
                                    <th>{t('duration')}</th>
                                    <th>{t('reason')}</th>
                                    <th>{t('recorderLabel')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {Array.isArray(data.downtimeHistory) && data.downtimeHistory.length > 0 ? data.downtimeHistory.map((evt, i) => {
                                    console.log(`🎯 Rendering Downtime row ${i}:`, evt);
                                    return (
                                        <tr key={i}>
                                            <td>{evt.startTime || 'N/A'}</td>
                                            <td>{evt.endTime || 'กำลังดำเนินการ'}</td>
                                            <td>{evt.duration || 'N/A'}</td>
                                            <td>{evt.reason || evt.description || 'ไม่ระบุสาเหตุ'}</td>
                                            <td>{evt.technicianName || evt.userName || 'ไม่ระบุ'}</td>
                                        </tr>
                                    );
                                }) : (
                                    <tr>
                                        <td colSpan="5" style={{ textAlign: 'center', color: '#666' }}>
                                            {t('noData')}
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                    <h4 className="summary-title">{title} - {t('materialUsageSummaryTitle')}</h4>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>{t('timeLabel')}</th>
                                    <th>{t('materialCode')}</th>
                                    <th>{t('lotNumber')}</th>
                                    <th>{t('quantityKg')}</th>
                                    <th>{t('recorderLabel')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {Array.isArray(data.materialUsageLogs) && data.materialUsageLogs.length > 0 ? data.materialUsageLogs.map((log, i) => {
                                    console.log(`🎯 Rendering Material row ${i}:`, log);
                                    return (
                                        <tr key={i}>
                                            <td>{log.timestamp ? new Date(log.timestamp).toLocaleString(getLang() === 'th' ? 'th-TH' : 'en-US') : 'N/A'}</td>
                                            <td>{log.materialCode || log.materialName || 'ไม่ระบุ'}</td>
                                            <td>{log.lotNumber || 'ไม่ระบุ'}</td>
                                            <td>{log.quantityKg || log.quantity || 0}</td>
                                            <td>{log.technicianName || log.userName || 'ไม่ระบุ'}</td>
                                        </tr>
                                    );
                                }) : (
                                    <tr>
                                        <td colSpan="5" style={{ textAlign: 'center', color: '#666' }}>
                                            {t('noData')}
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* Packaging logs for the shift */}
                <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                    <h4 className="summary-title">{title} - {t('packagingLogsTitle')}</h4>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>{t('timeLabel')}</th>
                                    <th>{t('lotNumber')}</th>
                                    <th>{t('boxNo')}</th>
                                    <th>{t('operator')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {Array.isArray(data.packagingLogs) && data.packagingLogs.length > 0 ? data.packagingLogs.map((log, i) => (
                                    <tr key={i}>
                                        <td>{log.timestamp ? new Date(log.timestamp).toLocaleString(getLang() === 'th' ? 'th-TH' : 'en-US') : 'N/A'}</td>
                                        <td>{log.lotNumber || '-'}</td>
                                        <td>{log.boxNo || '-'}</td>
                                        <td>{log.operatorName || log.userName || 'ไม่ระบุ'}</td>
                                    </tr>
                                )) : (
                                    <tr>
                                        <td colSpan="4" style={{ textAlign: 'center', color: '#666' }}>
                                            {t('noData')}
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </>
        );
    }
    
    // For original array format (backward compatibility)
    return (
        <>
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h4 className="summary-title">{title}</h4>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>{t('timeLabel')}</th>
                                <th>{t('ngTypeLabel')}</th>
                                <th>{t('countLabel')}</th>
                                <th>Source</th>
                                <th>{t('recorderLabel')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.map((log, i) => (
                                <tr key={i}>
                                    <td>{log.timestamp}</td>
                                    <td>{getNgDescription(log)}</td>
                                    <td>{log.quantity}</td>
                                    <td>{log.source}</td>
                                    <td>{log.userName}</td>
                                </tr>
                            ))}
                            {data.length === 0 && (
                                <tr>
                                    <td colSpan="5" style={{ textAlign: 'center', color: '#666' }}>
                                        {t('noData')}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h4 className="summary-title">{t('downtimeSummaryTitle')}</h4>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>{t('startTime')}</th>
                                <th>{t('endTime')}</th>
                                <th>{t('duration')}</th>
                                <th>{t('reason')}</th>
                                <th>{t('recorderLabel')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.downtimeHistory?.length > 0 ? data.downtimeHistory.map((evt, i) => (
                                <tr key={i}>
                                    <td>{evt.startTime}</td>
                                    <td>{evt.endTime}</td>
                                    <td>{evt.duration}</td>
                                    <td>{evt.reason}</td>
                                    <td>{evt.technicianName}</td>
                                </tr>
                            )) : (
                                <tr>
                                    <td colSpan="5" style={{ textAlign: 'center', color: '#666' }}>
                                        {t('noData')}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h4 className="summary-title">{t('materialUsageSummaryTitle')}</h4>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>{t('timeLabel')}</th>
                                <th>{t('materialCode')}</th>
                                <th>{t('lotNumber')}</th>
                                <th>{t('quantityKg')}</th>
                                <th>{t('recorderLabel')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.materialUsageLogs?.length > 0 ? data.materialUsageLogs.map((log, i) => (
                                <tr key={i}>
                                    <td>{new Date(log.timestamp).toLocaleString()}</td>
                                    <td>{log.materialCode}</td>
                                    <td>{log.lotNumber}</td>
                                    <td>{log.quantityKg}</td>
                                    <td>{log.technicianName}</td>
                                </tr>
                            )) : (
                                <tr>
                                    <td colSpan="5" style={{ textAlign: 'center', color: '#666' }}>
                                        {t('noData')}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </>
    );
};

// 🔧 Helper: แปลงข้อความรายชื่อบุคลากรแบบ "Role (n): name1, name2 | Role2 (m): ..." เป็นอ็อบเจ็กต์ที่แยกตามบทบาท
const parseWorkersByRole = (text) => {
    const result = {};
    if (!text || typeof text !== 'string') return result;
    // แยกด้วย |
    const parts = text.split('|').map(s => s.trim()).filter(Boolean);
    for (const p of parts) {
        // รูปแบบ Role (n): a, b
        const m = p.match(/^(.+?)\s*\((\d+)\)\s*:\s*(.*)$/);
        if (m) {
            const role = m[1].trim();
            const names = m[3].split(',').map(s => s.trim()).filter(Boolean);
            result[role] = names;
        } else {
            // fallback: Role: a, b
            const idx = p.indexOf(':');
            if (idx > 0) {
                const role = p.slice(0, idx).trim();
                const names = p.slice(idx + 1).split(',').map(s => s.trim()).filter(Boolean);
                result[role] = names;
            }
        }
    }
    return result;
};

// 🔧 Helper: เลือกชื่อประเภทของเสียตามภาษา (EN/TH) จากหลายชื่อฟิลด์ทั่วไป
const getNgDescription = (row) => {
    if (!row || typeof row !== 'object') return getLang() === 'th' ? 'ไม่ระบุ' : 'N/A';
    const lang = getLang();
    if (lang === 'en') {
        return (
            row.ngDescriptionEn || row.descriptionEn || row.ngTypeNameEn ||
            row.englishDescription || row.englishName || row.ngEnglish ||
            row.description || row.ngDescription || row.ngTypeName || 'N/A'
        );
    }
    // Thai/default
    return (
        row.ngDescription || row.description || row.ngTypeName ||
        row.ngDescriptionTh || row.descriptionTh || 'ไม่ระบุ'
    );
};

// 🔧 Helper: สถานะ (display label + css class)
const statusDisplay = (status) => {
    if (!status) return t('notSpecified');
    const key = String(status).toUpperCase();
    switch (key) {
        case 'IN_PROGRESS': return t('inProgress');
        case 'ACTIVE': return t('active');
        case 'INACTIVE': return t('inactive');
        case 'COMPLETED': return t('completed');
        default: return status;
    }
};

const statusClass = (status) => {
    const key = String(status || '').toUpperCase();
    switch (key) {
        case 'IN_PROGRESS': return 'in-progress';
        case 'ACTIVE': return 'active';
        case 'INACTIVE': return 'inactive';
        case 'COMPLETED': return 'completed';
        default: return 'unknown';
    }
};

const GaugeCard = ({ machineName, productName, target, current, ng, status, statusDisplayName }) => {
    const percent = target > 0 ? (current / target) * 100 : 0;
    const formatNumber = (num) => new Intl.NumberFormat('en-US').format(num || 0);
    
    const chartOptions = {
        chart: {
            type: 'radialBar',
            sparkline: { enabled: true }
        },
        plotOptions: {
            radialBar: {
                startAngle: -90,
                endAngle: 90,
                hollow: { size: '75%' },
                track: {
                    background: "#e7e7e7",
                    strokeWidth: '97%'
                },
                dataLabels: {
                    name: { show: false },
                    value: {
                        offsetY: -2,
                        fontSize: '22px',
                        formatter: (val) => val.toFixed(1) + "%"
                    }
                }
            }
        },
        grid: { padding: { top: -10 } },
        colors: ["#3b82f6"],
        labels: ['Progress']
    };
    
    const chartSeries = [percent];
    
    return (
        <div className="gauge-card">
            {/* สถานะงาน */}
            <div className="gauge-status" style={{ display: 'flex', justifyContent: 'flex-end' }}>
                {(() => {
                    const label = statusDisplayName || statusDisplay(status);
                    const cls = statusClass(status);
                    const colorMap = {
                        'in-progress': '#2563eb',
                        'active': '#059669',
                        'inactive': '#9ca3af',
                        'completed': '#7c3aed',
                        'unknown': '#6b7280'
                    };
                    const bg = colorMap[cls] || '#6b7280';
                    return (
                        <span className={`status-badge ${cls}`}
                              style={{
                                  padding: '2px 8px',
                                  borderRadius: '12px',
                                  fontSize: '12px',
                                  color: 'white',
                                  backgroundColor: bg,
                                  marginBottom: '6px'
                              }}>
                            {label}
                        </span>
                    );
                })()}
            </div>
            <div className="gauge-chart-container">
                <Chart options={chartOptions} series={chartSeries} type="radialBar" height="140" />
            </div>
            <div className="gauge-info">
                <h3 className="gauge-machine-name">{machineName}</h3>
                <p className="gauge-product-name">{productName}</p>
                <div className="gauge-details">
                    <div className="gauge-detail-item">
                        <span>{t('good')}</span>
                        <strong>{formatNumber(current)}</strong>
                    </div>
                    <div className="gauge-detail-item">
                        <span>{t('target')}</span>
                        <strong>{formatNumber(target)}</strong>
                    </div>
                    <div className="gauge-detail-item ng">
                        <span>{t('ng')}</span>
                        <strong>{formatNumber(ng)}</strong>
                    </div>
                </div>
            </div>
        </div>
    );
};

const ReportDetailView = ({ reportId, onBack }) => {
    const [summary, setSummary] = useState(null);
    const [reportSummary, setReportSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    useEffect(() => {
        const fetchData = async () => {
            if (!reportId) return;
            console.log('🔍 ReportDetailView: Fetching data for reportId:', reportId);
            setLoading(true);
            try {
                // Fetch both summary and enhanced report summary
                console.log('📡 Making API calls...');
                
                // Use report-summary endpoint instead of summary (which doesn't exist)
                const summaryPromise = api.get(`/production/reports/${reportId}/report-summary`)
                    .then(response => {
                        console.log('✅ Primary Summary API success:', response.data);
                        return response;
                    })
                    .catch(error => {
                        console.error('❌ Primary Summary API error:', error.response?.status, error.response?.data || error.message);
                        return null;
                    });
                
                const reportSummaryPromise = api.get(`/production/reports/${reportId}/report-summary`)
                    .then(response => {
                        console.log('✅ Report Summary API success:', response.data);
                        return response;
                    })
                    .catch(error => {
                        console.error('❌ Report Summary API error:', error.response?.status, error.response?.data || error.message);
                        return null;
                    });
                
                const [summaryResponse, reportSummaryResponse] = await Promise.all([
                    summaryPromise,
                    reportSummaryPromise
                ]);
                
                if (summaryResponse?.data) {
                    console.log('📊 Setting primary summary data:', summaryResponse.data);
                    setSummary(summaryResponse.data);
                }
                
                if (reportSummaryResponse?.data) {
                    console.log('📊 Setting enhanced report summary data:', reportSummaryResponse.data);
                    setReportSummary(reportSummaryResponse.data);
                }
                
                if (!summaryResponse?.data && !reportSummaryResponse?.data) {
                    console.log('⚠️ No data from both APIs');
                    setError(t('cannotFetchSummaryNoApiData'));
                }
                
            } catch (err) {
                console.error('🚨 Error fetching report details:', err);
                setError(`${t('cannotFetchSummary')}: ${err.message}`);
            } finally {
                setLoading(false);
                console.log('🏁 ReportDetailView: Data fetching completed');
            }
        };
        fetchData();
    }, [reportId]);

    if (loading) return <div className="loading-container"><h2>{t('loadingDetails')}</h2></div>;
    if (error) return <div className="dashboard-card"><p className="error-message">{error}</p><button onClick={onBack} className="back-button">&larr; {t('back')}</button></div>;
    if (!summary) {
        console.log('⚠️ No summary data, rendering fallback message');
        console.log('📊 Current state - summary:', summary, 'reportSummary:', reportSummary);
        return (
            <div className="dashboard-card">
                <button onClick={onBack} className="back-button">&larr; {t('backToPrevious')}</button>
                <div className="error-message" style={{textAlign: 'center', padding: '2rem'}}>
                    <h3>{t('noSummaryData')}</h3>
                    <p>{t('reportIdLabel')}: {reportId}</p>
                    <p>{t('pleaseCheckConsole')}</p>
                </div>
            </div>
        );
    }

    console.log('✅ Rendering ReportDetailView with summary:', summary);

    // Derived totals for overview KPIs
    const totalScrapWeightKg = (() => {
        // Prefer enhanced summary field if present
        if (reportSummary && (typeof reportSummary.totalScrapWeightKg !== 'undefined')) {
            const v = reportSummary.totalScrapWeightKg;
            return typeof v === 'number' ? v : parseFloat(v || 0);
        }
        if (typeof summary.totalScrapWeight !== 'undefined') {
            const v = summary.totalScrapWeight;
            return typeof v === 'number' ? v : parseFloat(v || 0);
        }
        return 0;
    })();

    const totalMaterialUsedKg = Array.isArray(summary.materialUsageLogs)
        ? summary.materialUsageLogs.reduce((acc, m) => acc + (parseFloat(m.quantityKg) || 0), 0)
        : 0;

    // Scrap logs (if available) and Technician-specific totals
    const scrapLogs = (reportSummary && Array.isArray(reportSummary.scrapWeightLogs) ? reportSummary.scrapWeightLogs
                    : (Array.isArray(summary.scrapWeightLogs) ? summary.scrapWeightLogs : []));

    const isTechnicianLog = (log) => {
        const probe = (v) => (typeof v === 'string') && /tech/i.test(v);
        return !!(
            probe(log?.recordedByRole) || probe(log?.role) || probe(log?.userRole) ||
            probe(log?.createdByRole) || probe(log?.createdBy) || probe(log?.recordedBy) ||
            (typeof log?.isTechnician === 'boolean' && log.isTechnician === true)
        );
    };

    const technicianScrapWeightKg = (() => {
        if (!Array.isArray(scrapLogs) || scrapLogs.length === 0) {
            // Fallback to overall if detailed logs are unavailable
            return totalScrapWeightKg || 0;
        }
        const techLogs = scrapLogs.filter(isTechnicianLog);
        if (techLogs.length === 0) {
            // If there is no clear technician marker, don't misreport; show 0 or fallback to total if only technician logs exist in the system
            return 0;
        }
        return techLogs.reduce((acc, l) => acc + (parseFloat(l?.weightKg) || 0), 0);
    })();

    // Aggregate Technician scrap by description/reason for summary table
    const technicianScrapAgg = (() => {
        const map = new Map();
        if (Array.isArray(scrapLogs) && scrapLogs.length > 0) {
            for (const l of scrapLogs) {
                if (!isTechnicianLog(l)) continue;
                const key = l?.reason || l?.description || t('unspecifiedReason');
                const w = parseFloat(l?.weightKg) || 0;
                const prev = map.get(key) || { reason: key, count: 0, totalKg: 0 };
                prev.count += 1;
                prev.totalKg += w;
                map.set(key, prev);
            }
        }
        return Array.from(map.values()).sort((a, b) => b.totalKg - a.totalKg);
    })();

    // Aggregate material usage by material code for a compact summary table
    const materialAgg = (() => {
        const map = new Map();
        if (Array.isArray(summary.materialUsageLogs)) {
            for (const m of summary.materialUsageLogs) {
                const code = m.materialCode || t('notSpecified');
                const qty = parseFloat(m.quantityKg) || 0;
                const prev = map.get(code) || { materialCode: code, totalKg: 0, count: 0 };
                prev.totalKg += qty;
                prev.count += 1;
                map.set(code, prev);
            }
        }
        return Array.from(map.values()).sort((a, b) => b.totalKg - a.totalKg);
    })();

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('backToPrevious')}</button>
            <h2 className="dashboard-title">
                {t('productionSummaryTitle')}: {reportSummary?.orderNumber || summary?.orderNumber || summary?.orderNo || summary?.prodOrder || summary?.productionOrder || '—'}
            </h2>
            <p>
                {t('machineLabel')}: {summary.machineName} | {t('productLabel')}: {summary.productName} | {t('targetLabel')}: {summary.targetQty?.toLocaleString() || "N/A"}
            </p>
            
            {/* Basic KPIs */}
            <div className="kpi-grid" style={{gridTemplateColumns: 'repeat(6, 1fr)'}}>
                <div className="kpi-card"><span className="kpi-label">{t('good')}</span><span className="kpi-value">{summary.goodQty?.toLocaleString() || "0"}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('ng')}</span><span className="kpi-value ng-value">{summary.totalNgQty?.toLocaleString() || "0"}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('yieldLabel')}</span><span className="kpi-value yield-value">{summary.yield || "0.00%"}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('scrapWeightLabel')}</span><span className="kpi-value ng-value">{totalScrapWeightKg.toFixed(2)}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('materialUsageSummaryTitle')} (Kg.)</span><span className="kpi-value">{totalMaterialUsedKg.toFixed(2)}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('technicianScrapSummaryTitle')} (Kg.)</span><span className="kpi-value ng-value">{technicianScrapWeightKg.toFixed(2)}</span></div>
            </div>
            
            {/* Enhanced OEE Metrics */}
            {reportSummary && (
                <div className="oee-metrics-panel" style={{ marginTop: '1.5rem', background: '#f8f9fa', padding: '1.5rem', borderRadius: '0.75rem' }}>
                    <h3 style={{ color: '#2c3e50', marginBottom: '1rem' }}>📊 {t('oeeTitle')}</h3>
                    <div className="kpi-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
                        <div className="kpi-card oee-card">
                            <span className="kpi-label">{t('oeeOverall')}</span>
                            <span className="kpi-value oee-value" style={{ fontSize: '2rem', color: '#e74c3c' }}>{reportSummary.oee || '0.00%'}</span>
                        </div>
                        <div className="kpi-card">
                            <span className="kpi-label">{t('availability')}</span>
                            <span className="kpi-value" style={{ fontSize: '1.5rem', color: '#3498db' }}>{reportSummary.availability || '0.00%'}</span>
                        </div>
                        <div className="kpi-card">
                            <span className="kpi-label">{t('performance')}</span>
                            <span className="kpi-value" style={{ fontSize: '1.5rem', color: '#f39c12' }}>{reportSummary.performance || '0.00%'}</span>
                        </div>
                        <div className="kpi-card">
                            <span className="kpi-label">{t('quality')}</span>
                            <span className="kpi-value" style={{ fontSize: '1.5rem', color: '#27ae60' }}>{reportSummary.quality || '0.00%'}</span>
                        </div>
                    </div>
                </div>
            )}
            
            {/* NG Type Summary */}
            {reportSummary?.ngTypeSummary?.length > 0 && (
                <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                    <h3 style={{ color: '#e74c3c' }}>🔴 {t('ngSummaryTitle')}</h3>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>{t('ngTypeLabel')}</th>
                                    <th>{t('pieces')}</th>
                                    <th>{t('percent')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {reportSummary.ngTypeSummary.map((ng, i) => (
                                    <tr key={i}>
                                        <td>{getNgDescription(ng)}</td>
                                        <td style={{ textAlign: 'right' }}>{ng.count?.toLocaleString() || 0}</td>
                                        <td style={{ textAlign: 'right', color: '#e74c3c' }}>{ng.percentage ? ng.percentage.toFixed(2) + '%' : '0.00%'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Material Usage Summary (aggregated) */}
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 style={{ color: '#2c3e50' }}>🏭 {t('materialUsageSummaryTitle')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>{t('materialCode')}</th>
                                <th style={{ textAlign: 'right' }}>{t('count')}</th>
                                <th style={{ textAlign: 'right' }}>{t('quantityKg')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {materialAgg.length > 0 ? materialAgg.map((row) => (
                                <tr key={row.materialCode}>
                                    <td>{row.materialCode}</td>
                                    <td style={{ textAlign: 'right' }}>{row.count.toLocaleString()}</td>
                                    <td style={{ textAlign: 'right' }}>{row.totalKg.toFixed(3)}</td>
                                </tr>
                            )) : (
                                <tr>
                                    <td colSpan={3} style={{ textAlign: 'center', color: '#666' }}>{t('noMaterialUsageData')}</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Technician Scrap Summary (if available) */}
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 style={{ color: '#8e44ad' }}>🧰 {t('technicianScrapSummaryTitle')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>{t('reason')}</th>
                                <th style={{ textAlign: 'right' }}>{t('count')}</th>
                                <th style={{ textAlign: 'right' }}>{t('quantityKg')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {technicianScrapAgg.length > 0 ? technicianScrapAgg.map((row) => (
                                <tr key={row.reason}>
                                    <td>{row.reason}</td>
                                    <td style={{ textAlign: 'right' }}>{row.count.toLocaleString()}</td>
                                    <td style={{ textAlign: 'right' }}>{row.totalKg.toFixed(3)}</td>
                                </tr>
                            )) : (
                                <tr>
                                    <td colSpan={3} style={{ textAlign: 'center', color: '#666' }}>{t('noTechnicianScrapData')}</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
            
            {/* Downtime Reason Summary */}
            {reportSummary?.downtimeReasonSummary?.length > 0 && (
                <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                    <h3 style={{ color: '#f39c12' }}>⏰ {t('downtimeSummaryTitle')}</h3>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>{t('reason')}</th>
                                    <th>{t('duration')}</th>
                                    <th>{t('count')}</th>
                                    <th>{t('percent')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {reportSummary.downtimeReasonSummary.map((dt, i) => (
                                    <tr key={i}>
                                        <td>{dt.reason || t('unspecifiedReason')}</td>
                                        <td style={{ textAlign: 'right' }}>{dt.formattedDuration || `0 ${t('minutes')}`}</td>
                                        <td style={{ textAlign: 'center' }}>{dt.count || 0}</td>
                                        <td style={{ textAlign: 'right', color: '#f39c12' }}>{dt.percentage ? dt.percentage.toFixed(2) + '%' : '0.00%'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
            <div className="summary-panel">
                <h3>{t('downtimeHistory')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>{t('startTime')}</th>
                                <th>{t('endTime')}</th>
                                <th>{t('duration')}</th>
                                <th>{t('reason')}</th>
                                <th>{t('recorderLabel')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {summary.downtimeEvents?.length > 0 ? summary.downtimeEvents.map((evt, i) => (
                                <tr key={i}>
                                    <td>{evt.startTime}</td>
                                    <td>{evt.endTime}</td>
                                    <td>{evt.duration}</td>
                                    <td>{evt.reason}</td>
                                    <td>{evt.technicianName}</td>
                                </tr>
                            )) : (
                                <tr>
                                    <td colSpan="5">{t('noDowntimeData')}</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

// Component for Daily Report Date Selection
const DailyReportSelector = ({ onBack, onDateSelected, defaultDate, machineId, productId, orderNumber }) => {
    const [selectedDate, setSelectedDate] = useState(defaultDate || new Date().toISOString().split('T')[0]);
    const [availableDates, setAvailableDates] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchAvailableDates = async () => {
            try {
                // ดึงรายการวันที่ 30 วันย้อนหลัง และกรองด้วย machineId/productId/orderNumber ถ้ามี
                const endDate = new Date().toISOString().split('T')[0];
                const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
                const params = new URLSearchParams({ startDate, endDate });
                if (machineId) params.append('machineId', machineId);
                if (productId) params.append('productId', productId);
                if (orderNumber) params.append('orderNumber', orderNumber);
                const response = await api.get(`/production/reports/available-dates?${params.toString()}`);
                setAvailableDates(response.data || []);
            } catch (err) {
                console.error('Error fetching available dates:', err);
                setAvailableDates([]);
            } finally {
                setLoading(false);
            }
        };
        fetchAvailableDates();
    }, [machineId, productId, orderNumber]);

    const handleDateChange = (e) => {
        setSelectedDate(e.target.value);
    };

    const handleViewReport = () => {
        console.log('🔍 DailyReportSelector: handleViewReport called with date:', selectedDate);
        if (selectedDate) {
            console.log('📅 DailyReportSelector: calling onDateSelected with:', selectedDate);
            onDateSelected(selectedDate);
        } else {
            console.warn('⚠️ DailyReportSelector: No date selected');
        }
    };

    if (loading) {
        return (
            <div className="dashboard-card">
                <button onClick={onBack} className="back-button">&larr; {t('backToHistory')}</button>
                <h2>{t('loadingData')}</h2>
            </div>
        );
    }

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('backToHistory')}</button>
            <h2 className="dashboard-title">{t('selectDateForDailySummary')}{orderNumber ? ` • ${t('productionSummaryTitle')}: ${orderNumber}` : ''}</h2>
            
            <div className="filters-section" style={{ marginTop: '2rem' }}>
                <div className="filter-group">
                    <label htmlFor="dailyDate">{t('chooseDate')}:</label>
                    <input
                        type="date"
                        id="dailyDate"
                        value={selectedDate}
                        onChange={handleDateChange}
                        className="date-input"
                    />
                </div>
                
                {availableDates.length > 0 && (
                    <div className="filter-group">
                        <label htmlFor="availableDates">{t('chooseFromAvailableDates')}:</label>
                        <select
                            id="availableDates"
                            value={selectedDate}
                            onChange={(e) => setSelectedDate(e.target.value)}
                            className="select-input"
                        >
                            <option value="">{t('chooseDate')}...</option>
                            {availableDates.map(date => (
                                <option key={date} value={date}>
                                    {new Date(date).toLocaleDateString(getLang() === 'th' ? 'th-TH' : 'en-US', {
                                        year: 'numeric',
                                        month: 'long',
                                        day: 'numeric',
                                        weekday: 'long'
                                    })}
                                </option>
                            ))}
                        </select>
                    </div>
                )}
                
                <div className="buttons-section" style={{ marginTop: '1rem' }}>
                    <button 
                        onClick={handleViewReport}
                        className="search-button"
                        disabled={!selectedDate}
                    >
                        {t('viewDailySummary')}
                    </button>
                </div>
            </div>
            
            {availableDates.length === 0 && !loading && (
                <div className="info-message" style={{ marginTop: '1rem', padding: '1rem', backgroundColor: '#f0f8ff', borderRadius: '4px' }}>
                    <p>📅 {t('noAvailableDates')}</p>
                    <p style={{ fontSize: '0.9rem', color: '#666' }}>
                        {t('youCanPickDate')}
                    </p>
                </div>
            )}
        </div>
    );
};

const DailyReportView = ({ date, machineId, productId, orderNumber, machineName: ctxMachineName, productName: ctxProductName, onBack, onSelectNewDate }) => {
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [oeeData, setOeeData] = useState(null);

    useEffect(() => {
        const fetchDailySummary = async () => {
            if (!date) {
                console.warn('⚠️ DailyReportView: No date provided');
                return;
            }
            console.log('🔍 DailyReportView: Fetching daily summary for date:', date);
            setLoading(true);
            setError('');
            try {
                let url = `/production/reports/summary/daily?date=${date}`;
                // ฝั่ง backend ใช้ String machineId (เช่น machineName หรือรหัสเครื่อง)
                const machineParam = (ctxMachineName && ctxMachineName.trim()) ? ctxMachineName : (machineId || '').toString();
                if (machineParam) { url += `&machineId=${encodeURIComponent(machineParam)}`; }
                if (productId) { url += `&productId=${encodeURIComponent(productId)}`; }
                console.log('📡 DailyReportView: Making API call to:', url);
                const response = await api.get(url);  
                console.log('✅ DailyReportView: API response:', response.data);
                console.log('🔍 NG Summary data:', response.data?.ngSummary);
                console.log('📊 NG Summary length:', response.data?.ngSummary?.length);
                setSummary(response.data);
            } catch (err) {
                console.error('❌ DailyReportView: Daily summary error:', err);
                console.error('❌ Response status:', err.response?.status);
                console.error('❌ Response data:', err.response?.data);
                setError('ไม่สามารถดึงข้อมูลสรุปรายวันได้ หรือไม่มีข้อมูลสำหรับวันที่เลือก');
            } finally {
                setLoading(false);
            }
        };
        fetchDailySummary();
    }, [date]);

    useEffect(() => {
        const fetchOee = async () => {
            if (!machineId || !date) return;
            try {
                const res = await api.get(`/oee/${machineId}?date=${date}`);
                setOeeData(res.data);
            } catch {
                setOeeData(null);
            }
        };
        fetchOee();
    }, [machineId, date]);

    if (loading) return <div className="loading-container"><h2>{t('loadingDailySummary')}</h2></div>;
    
    if (error) return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('back')}</button>
            <p className="error-message">{error}</p>
            <div className="buttons-section" style={{ marginTop: '1rem' }}>
                <button onClick={onSelectNewDate} className="search-button">
                    {t('selectOtherDate')}
                </button>
            </div>
        </div>
    );
    
    if (!summary) return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('back')}</button>
            <p>{t('noData')} - {date}</p>
            <div className="buttons-section" style={{ marginTop: '1rem' }}>
                <button onClick={onSelectNewDate} className="search-button">
                    {t('selectOtherDate')}
                </button>
            </div>
        </div>
    );

    // Calculate additional metrics
    const calculateMetrics = () => {
        const totalTarget = summary.dailyReports?.reduce((sum, report) => sum + (report.targetQty || 0), 0) || 0;
        const totalGood = summary.totalGoodQty || 0;
        const totalNg = summary.totalNgQty || 0;
        const actualGood = Math.max(totalGood - totalNg, 0);
        const yieldPercent = totalTarget > 0 ? ((actualGood / totalTarget) * 100) : 0;
        return { totalTarget, yieldPercent: yieldPercent.toFixed(2) };
    };

    const metrics = calculateMetrics();
    const oeePercent = oeeData ? oeeData.oee.toFixed(1) : null;

    // เพิ่ม KPI ตามเทมเพลตภาพรวม: วัตถุดิบใช้รวม และ น้ำหนักของเสียจาก Technician
    const totalMaterialUsedKg = Array.isArray(summary.materialUsageLogs)
        ? summary.materialUsageLogs.reduce((acc, m) => acc + (parseFloat(m.quantityKg) || 0), 0)
        : 0;

    const technicianScrapTotalKg = (() => {
        const weightData = Array.isArray(summary.ngSummary) ? summary.ngSummary.filter(ng => (ng.weightKg || 0) > 0) : [];
        return weightData.reduce((sum, ng) => sum + (ng.weightKg || 0), 0);
    })();

    // ตารางสรุปวัตถุดิบ (รวมทั้งวัน)
    const materialAgg = (() => {
        const map = new Map();
        if (Array.isArray(summary.materialUsageLogs)) {
            for (const m of summary.materialUsageLogs) {
                const code = m.materialCode || 'ไม่ระบุ';
                const qty = parseFloat(m.quantityKg) || 0;
                const prev = map.get(code) || { materialCode: code, totalKg: 0, count: 0 };
                prev.totalKg += qty;
                prev.count += 1;
                map.set(code, prev);
            }
        }
        return Array.from(map.values()).sort((a, b) => b.totalKg - a.totalKg);
    })();

    // สรุปสาเหตุ Downtime (รวมทั้งวัน) จาก summary.downtimeEvents
    const downtimeReasonAgg = (() => {
        const map = new Map();
        const events = Array.isArray(summary.downtimeEvents) ? summary.downtimeEvents : [];
        for (const evt of events) {
            const reason = evt?.reason || evt?.description || 'ไม่ระบุสาเหตุ';
            const minutes = (() => {
                // duration เป็น string เช่น "65 นาที" หรือ "1 ชั่วโมง 5 นาที"
                const d = (evt?.duration || '').toString();
                const h = /([0-9]+)\s*ชั่วโมง/.exec(d);
                const m = /([0-9]+)\s*นาที/.exec(d);
                const hours = h ? parseInt(h[1], 10) : 0;
                const mins = m ? parseInt(m[1], 10) : 0;
                if (hours === 0 && mins === 0) {
                    // fallback: ดึงเลขทั้งหมดรวมๆ (เช่น "65 นาที")
                    const num = parseInt(d.replace(/[^0-9]/g, ''), 10);
                    return isNaN(num) ? 0 : num;
                }
                return (hours * 60) + mins;
            })();
            const prev = map.get(reason) || { reason, totalMinutes: 0, count: 0 };
            prev.totalMinutes += minutes;
            prev.count += 1;
            map.set(reason, prev);
        }
        const arr = Array.from(map.values());
        const total = arr.reduce((acc, r) => acc + r.totalMinutes, 0) || 0;
        return arr
            .map(r => ({
                ...r,
                percentage: total > 0 ? (r.totalMinutes * 100) / total : 0,
                formattedDuration: r.totalMinutes >= 60
                    ? `${Math.floor(r.totalMinutes / 60)} ชั่วโมง ${r.totalMinutes % 60} นาที`
                    : `${r.totalMinutes} นาที`
            }))
            .sort((a, b) => b.totalMinutes - a.totalMinutes);
    })();

    return (
        <div className="dashboard-card">
            <div className="dashboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <button onClick={onBack} className="back-button">&larr; {t('backToHistory')}</button>
                <button onClick={onSelectNewDate} className="edit-button">
                    📅 {t('selectOtherDate')}
                </button>
            </div>
            <h2 className="dashboard-title">{t('productionOverviewTitle')}: {new Date(date).toLocaleDateString(getLang() === 'th' ? 'th-TH' : 'en-US', {
                year: 'numeric',
                month: 'long', 
                day: 'numeric',
                weekday: 'long'
            })}{orderNumber ? ` • ${t('orderNumber')}: ${orderNumber}` : ''}</h2>

            {/* แสดงเครื่องจักร/ผลิตภัณฑ์/เป้าหมายตาม Template */}
            <p>
                {t('machineLabel')}: {summary?.machineName || ctxMachineName || t('notSpecified')} | {t('productLabel')}: {summary?.productName || ctxProductName || t('notSpecified')} | {t('targetLabel')}: {(summary?.dailyReports && summary.dailyReports.length > 0)
                    ? (summary.dailyReports.reduce((s, r) => s + (r.targetQty || 0), 0)).toLocaleString()
                    : 'N/A'}
            </p>

            {/* เครื่องจักรและผลิตภัณฑ์ที่ใช้ในวันนี้ */}
            {summary.dailyReports && summary.dailyReports.length > 0 && (
                <div className="summary-panel" style={{ marginTop: '1rem' }}>
                    <h3 className="summary-title">{t('machineProductToday')}</h3>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead><tr><th>{t('orderNumber')}</th><th>{t('machineLabel')}</th><th>{t('productLabel')}</th><th>{t('statusLabel')}</th></tr></thead>
                            <tbody>
                                {summary.dailyReports.map((report, i) => {
                                    return (
                                        <tr key={i}>
                                            <td><strong>{report.orderNumber || t('notSpecified')}</strong></td>
                                            <td>{report.machineName || t('notSpecified')}</td>
                                            <td>{report.productName || t('notSpecified')}</td>
                                            <td>
                                                <span className={`status-${statusClass(report.status)}`}>
                                                    {statusDisplay(report.status)}
                                                </span>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* KPI Grid หลัก */}
            <div className="kpi-grid pc-kpi" style={{gridTemplateColumns: 'repeat(6, 1fr)', marginTop: '1.5rem'}}>
                <div className="kpi-card"><span className="kpi-label">{t('totalTargetLabel')}</span><span className="kpi-value">{metrics.totalTarget.toLocaleString()}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('totalGoodAll')}</span><span className="kpi-value">{summary.totalGoodQty.toLocaleString()}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('totalNgAll')}</span><span className="kpi-value ng-value">{summary.totalNgQty.toLocaleString()}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('totalBoxes')}</span><span className="kpi-value">{summary.totalGoodBoxes?.toLocaleString() || 0}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('yieldLabel')}</span><span className="kpi-value yield-value">{metrics.yieldPercent}%</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('oeeLabel')}</span><span className="kpi-value oee-value">{oeePercent !== null ? `${oeePercent}%` : '-'}</span></div>
            </div>

            {/* OEE Breakdown Panel */}
            {oeeData && (
                <div className="summary-panel" style={{ marginTop: '1rem' }}>
                    <h3 className="summary-title">OEE Breakdown</h3>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                        {[
                            { label: 'Availability', value: oeeData.availability, detail: `${oeeData.runningMinutes} / ${oeeData.plannedProductionMinutes} นาที` },
                            { label: 'Performance', value: oeeData.performance, detail: `Ideal cycle: ${oeeData.idealCycleTimeSec}s` },
                            { label: 'Quality', value: oeeData.quality, detail: `Good: ${oeeData.goodQty} / Total: ${oeeData.totalQty}` },
                        ].map(({ label, value, detail }) => {
                            const pct = value.toFixed(1);
                            const color = value >= 85 ? '#16a34a' : value >= 60 ? '#d97706' : '#dc2626';
                            return (
                                <div key={label} style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 14, background: '#fff' }}>
                                    <div style={{ fontWeight: 600, marginBottom: 6, color: '#374151' }}>{label}</div>
                                    <div style={{ fontSize: 28, fontWeight: 700, color }}>{pct}%</div>
                                    <div style={{ marginTop: 6, height: 8, background: '#f3f4f6', borderRadius: 4, overflow: 'hidden' }}>
                                        <div style={{ height: '100%', width: `${Math.min(value, 100)}%`, background: color, borderRadius: 4, transition: 'width 0.5s' }} />
                                    </div>
                                    <div style={{ fontSize: 11, color: '#6b7280', marginTop: 4 }}>{detail}</div>
                                </div>
                            );
                        })}
                    </div>
                    <div style={{ marginTop: 12, padding: '10px 14px', background: '#f8fafc', borderRadius: 8, display: 'flex', gap: 24, fontSize: 13, color: '#374151' }}>
                        <span>Running: <strong>{oeeData.runningMinutes} น.</strong></span>
                        <span>Idle: <strong>{oeeData.idleMinutes} น.</strong></span>
                        <span>Unplanned Stop: <strong style={{ color: '#dc2626' }}>{oeeData.unplannedStopMinutes} น.</strong></span>
                        <span>Planned Stop: <strong>{oeeData.plannedStopMinutes} น.</strong></span>
                    </div>
                </div>
            )}
            {!oeeData && machineId && (
                <div style={{ marginTop: '1rem', padding: '10px 14px', background: '#fffbeb', borderRadius: 8, fontSize: 13, color: '#92400e' }}>
                    OEE: ยังไม่มีข้อมูล MachineStatusLog หรือ Recipe สำหรับวันนี้ — ต้องบันทึกสถานะเครื่องและตั้ง Recipe ก่อน
                </div>
            )}

            <div className="kpi-grid pc-kpi" style={{gridTemplateColumns: 'repeat(4, 1fr)', marginTop: '1rem'}}>
                <div className="kpi-card"><span className="kpi-label">{t('totalScrapWeightLabel')}</span><span className="kpi-value ng-value">{summary.totalScrapWeight?.toFixed(2) || 0}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('technicianScrapWeightLabel')}</span><span className="kpi-value ng-value">{technicianScrapTotalKg.toFixed(2)}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('totalMaterialUsedLabel')}</span><span className="kpi-value">{totalMaterialUsedKg.toFixed(2)}</span></div>
                <div className="kpi-card"><span className="kpi-label">{t('ngTypesCountLabel')}</span><span className="kpi-value">{summary.ngSummary?.length || 0}</span></div>
            </div>

            {/* สรุปยอดใช้วัตถุดิบ (รวมทั้งวัน) */}
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 className="summary-title">🏭 {t('materialUsageWholeDay')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>{t('materialCode')}</th><th style={{textAlign:'right'}}>{t('count')}</th><th style={{textAlign:'right'}}>{t('quantityKg')}</th></tr></thead>
                        <tbody>
                            {materialAgg.length > 0 ? materialAgg.map((row) => (
                                <tr key={row.materialCode}><td>{row.materialCode}</td><td style={{textAlign:'right'}}>{row.count.toLocaleString()}</td><td style={{textAlign:'right'}}>{row.totalKg.toFixed(3)}</td></tr>
                            )) : <tr><td colSpan={3} style={{textAlign:'center', color:'#666'}}>{t('noData')}</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* สรุปสาเหตุ Downtime (รวมทั้งวัน) */}
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 className="summary-title">⏰ {t('downtimeWholeDay')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>{t('reason')}</th><th style={{textAlign:'right'}}>{t('duration')}</th><th style={{textAlign:'right'}}>{t('count')}</th><th style={{textAlign:'right'}}>{t('percentageLabel')}</th></tr></thead>
                        <tbody>
                            {downtimeReasonAgg.length > 0 ? downtimeReasonAgg.map((r, i) => (
                                <tr key={i}><td>{r.reason}</td><td style={{textAlign:'right'}}>{r.formattedDuration}</td><td style={{textAlign:'right'}}>{r.count}</td><td style={{textAlign:'right'}}>{r.percentage.toFixed(2)}%</td></tr>
                            )) : <tr><td colSpan={4} style={{textAlign:'center', color:'#666'}}>{t('noData')}</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* ตารางน้ำหนักของเสีย PE/PP จาก Technician */}
            <div className="summary-panel" style={{ marginTop: '2rem' }}>
                <h3 className="summary-title">🏷️ {t('technicianWasteWeightTitle')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>{t('ngTypeLabel')}</th><th>{t('count')}</th><th>{t('scrapWeightLabel')}</th><th>{t('percentageLabel')}</th></tr></thead>
                        <tbody>
                            {summary.ngSummary?.length > 0 ? (() => {
                                const weightData = summary.ngSummary.filter(ng => (ng.weightKg || 0) > 0);
                                
                                if (weightData.length === 0) {
                                    return <tr><td colSpan="4">{t('noData')}</td></tr>;
                                }
                                
                                const totalWeight = weightData.reduce((sum, ng) => sum + (ng.weightKg || 0), 0);
                                
                                return weightData
                                    .sort((a, b) => (b.weightKg || 0) - (a.weightKg || 0))
                                    .map((ng, i) => {
                                        const weight = ng.weightKg || 0;
                                        const count = ng.count || 0;
                                        const weightDisplay = typeof weight === 'number' ? weight.toFixed(3) : parseFloat(weight).toFixed(3);
                                        const percentage = totalWeight > 0 ? ((weight / totalWeight) * 100).toFixed(2) : '0.00';
                                        
                                        return (
                                            <tr key={i} className="technician-weight-row">
                                                <td><strong>{getNgDescription(ng)}</strong></td>
                                                <td>{count.toLocaleString()}</td>
                                                <td className="ng-weight"><strong>{weightDisplay} {t('kgUnit')}</strong></td>
                                                <td className="ng-percentage"><strong>{percentage}%</strong></td>
                                            </tr>
                                        );
                                    });
                            })() : <tr><td colSpan="4">{t('noData')}</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* ตารางของเสียแบบชิ้น */}
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 className="summary-title">📊 {t('pieceWasteSummaryTitle')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>{t('ngTypeLabel')}</th><th>{t('ngPiecesLabel')}</th><th>{t('percentageLabel')}</th></tr></thead>
                        <tbody>
                            {summary.ngSummary?.length > 0 ? (() => {
                                const pieceData = summary.ngSummary.filter(ng => (ng.weightKg || 0) === 0);
                                
                                if (pieceData.length === 0) {
                                    return <tr><td colSpan="3">{t('noData')}</td></tr>;
                                }
                                
                                const totalPieces = pieceData.reduce((sum, ng) => sum + (ng.count || 0), 0);
                                
                                return pieceData
                                    .sort((a, b) => (b.count || 0) - (a.count || 0))
                                    .map((ng, i) => {
                                        const count = ng.count || 0;
                                        const percentage = totalPieces > 0 ? ((count / totalPieces) * 100).toFixed(2) : '0.00';
                                        
                                        return (
                                            <tr key={i} className="piece-count-row">
                                                <td><strong>{getNgDescription(ng)}</strong></td>
                                                <td>{count.toLocaleString()}</td>
                                                <td className="ng-percentage"><strong>{percentage}%</strong></td>
                                            </tr>
                                        );
                                    });
                            })() : <tr><td colSpan="3">{t('noData')}</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>

            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 className="summary-title">{t('materialUsageHistoryTitle')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>{t('timeLabel')}</th><th>{t('materialCode')}</th><th>{t('lotNumber')}</th><th>{t('quantityKg')}</th><th>{t('recorderLabel')}</th></tr></thead>
                        <tbody>
                            {summary.materialUsageLogs?.length > 0 ? summary.materialUsageLogs.map((log, i) => (
                                <tr key={i}><td>{new Date(log.timestamp).toLocaleString(getLang() === 'th' ? 'th-TH' : 'en-US')}</td><td>{log.materialCode}</td><td>{log.lotNumber}</td><td>{log.quantityKg}</td><td>{log.technicianName}</td></tr>
                            )) : <tr><td colSpan="5">{t('noData')}</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* รายการบันทึกการบรรจุ (ทั้งวัน 03:00–03:00) */}
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 className="summary-title">📦 {t('packagingLogsWholeDayTitle')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>{t('timeLabel')}</th>
                                <th>{t('lotNumber')}</th>
                                <th>{t('boxNo')}</th>
                                <th>{t('operator')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {Array.isArray(summary.packagingLogs) && summary.packagingLogs.length > 0 ? summary.packagingLogs.map((log, idx) => (
                                <tr key={idx}>
                                    <td>{log.timestamp ? new Date(log.timestamp).toLocaleString(getLang() === 'th' ? 'th-TH' : 'en-US') : 'N/A'}</td>
                                    <td>{log.lotNumber || '-'}</td>
                                    <td>{log.boxNo || '-'}</td>
                                    <td>{log.operatorName || log.userName || t('notSpecified')}</td>
                                </tr>
                            )) : (
                                <tr>
                                    <td colSpan="4" style={{ textAlign: 'center', color: '#666' }}>{t('noData')}</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 className="summary-title">{t('downtimeHistory')}</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>{t('startTime')}</th><th>{t('endTime')}</th><th>{t('duration')}</th><th>{t('reason')}</th><th>{t('recorderLabel')}</th></tr></thead>
                        <tbody>
                            {summary.downtimeEvents?.length > 0 ? summary.downtimeEvents.map((evt, i) => (
                                <tr key={i}><td>{evt.startTime}</td><td>{evt.endTime}</td><td>{evt.duration}</td><td>{evt.reason}</td><td>{evt.technicianName}</td></tr>
                            )) : <tr><td colSpan="5">{t('noData')}</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* สรุปกะทำงานและบุคลากร (นับตามจริงและแยกตามบทบาท) */}
            <div className="summary-panel" style={{ marginTop: '1.5rem' }}>
                <h3 className="summary-title">{t('totalWorkersSummaryTitle')}</h3>
                <div className="shift-summary" style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem', marginBottom: '1rem' }}>
                    <div className="shift-card" style={{ padding: '1rem', border: '1px solid #ddd', borderRadius: '8px', backgroundColor: '#f8f9fa' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#333' }}>{t('morningShift')}</h4>
                        <div className="shift-info">
                            <p><strong>{t('shiftLeader')}:</strong> {summary.morningShiftSupervisor || t('notSpecified')}</p>
                            <p><strong>{t('workers')}:</strong> {summary.morningShiftWorkers || t('notSpecified')}</p>
                            {(() => {
                                const map = parseWorkersByRole(summary.morningShiftWorkers);
                                const roles = Object.keys(map);
                                const counts = roles.reduce((s, r) => s + (map[r]?.length || 0), 0);
                                return (
                                    <>
                                        <p><strong>{t('headcount')}:</strong> {counts + (summary.morningShiftSupervisor && summary.morningShiftSupervisor !== 'ไม่มีข้อมูล' ? 1 : 0)} {t('peopleUnit')}</p>
                                        {roles.length > 0 && (
                                            <div style={{ fontSize: '0.9rem', color: '#555' }}>
                                                {roles.map(role => (
                                                    <div key={role}>{role}: {(map[role] || []).join(', ')}</div>
                                                ))}
                                            </div>
                                        )}
                                    </>
                                );
                            })()}
                        </div>
                    </div>
                    <div className="shift-card" style={{ padding: '1rem', border: '1px solid #ddd', borderRadius: '8px', backgroundColor: '#f8f9fa' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#333' }}>{t('nightShift')}</h4>
                        <div className="shift-info">
                            <p><strong>{t('shiftLeader')}:</strong> {summary.nightShiftSupervisor || t('notSpecified')}</p>
                            <p><strong>{t('workers')}:</strong> {summary.nightShiftWorkers || t('notSpecified')}</p>
                            {(() => {
                                const map = parseWorkersByRole(summary.nightShiftWorkers);
                                const roles = Object.keys(map);
                                const counts = roles.reduce((s, r) => s + (map[r]?.length || 0), 0);
                                return (
                                    <>
                                        <p><strong>{t('headcount')}:</strong> {counts + (summary.nightShiftSupervisor && summary.nightShiftSupervisor !== 'ไม่มีข้อมูล' ? 1 : 0)} {t('peopleUnit')}</p>
                                        {roles.length > 0 && (
                                            <div style={{ fontSize: '0.9rem', color: '#555' }}>
                                                {roles.map(role => (
                                                    <div key={role}>{role}: {(map[role] || []).join(', ')}</div>
                                                ))}
                                            </div>
                                        )}
                                    </>
                                );
                            })()}
                        </div>
                    </div>
                </div>
                
                {/* สรุปบุคลากรทั้งหมด */}
                <div className="total-workers" style={{ padding: '1rem', backgroundColor: '#e8f4fd', borderRadius: '8px', border: '1px solid #b3d9f2' }}>
                    <h4 style={{ margin: '0 0 0.5rem 0', color: '#0056b3' }}>{t('totalWorkersSummaryTitle')}</h4>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
                        {(() => {
                            const m = parseWorkersByRole(summary.morningShiftWorkers);
                            const n = parseWorkersByRole(summary.nightShiftWorkers);
                            const morningTotal = Object.values(m).reduce((s, arr) => s + arr.length, 0) + (summary.morningShiftSupervisor && summary.morningShiftSupervisor !== 'ไม่มีข้อมูล' ? 1 : 0);
                            const nightTotal = Object.values(n).reduce((s, arr) => s + arr.length, 0) + (summary.nightShiftSupervisor && summary.nightShiftSupervisor !== 'ไม่มีข้อมูล' ? 1 : 0);
                            const total = morningTotal + nightTotal;
                            const supervisors = (summary.morningShiftSupervisor && summary.morningShiftSupervisor !== 'ไม่มีข้อมูล' ? 1 : 0) + (summary.nightShiftSupervisor && summary.nightShiftSupervisor !== 'ไม่มีข้อมูล' ? 1 : 0);
                            const regulars = Math.max(total - supervisors, 0);
                            return (
                                <>
                                    <p><strong>{t('totalAll')}:</strong> {total} {t('peopleUnit')}</p>
                                    <p><strong>{t('supervisors')}:</strong> {supervisors} {t('peopleUnit')}</p>
                                    <p><strong>{t('regularWorkers')}:</strong> {regulars} {t('peopleUnit')}</p>
                                </>
                            );
                        })()}
                    </div>
                </div>
            </div>
        </div>
    );
};

const DailyShiftReportView = ({ date, machineId, productId, machineName: ctxMachineName, productName: ctxProductName, onBack, onSelectNewDate, machines }) => {
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showDayDetails, setShowDayDetails] = useState(false);
    const [showNightDetails, setShowNightDetails] = useState(false);
    // Machine selector removed per requirements

    useEffect(() => {
        const fetchDailyShiftSummary = async () => {
            if (!date) return;
            setLoading(true);
            setError('');
            try {
                let url = `/production/reports/summary/daily-shift?date=${date}`;
                // หากมี machineName จาก context ให้ใช้เป็น machineId (backend ใช้ String)
                const machineParam = (ctxMachineName && ctxMachineName.trim()) ? ctxMachineName
                    : (machineId ? machineId.toString() : '');
                if (machineParam) {
                    url += `&machineId=${encodeURIComponent(machineParam)}`;
                }
                if (productId) {
                    url += `&productId=${encodeURIComponent(productId)}`;
                }
                console.log('🔍 Daily Shift API call:', url);
                const response = await api.get(url);
                setSummary(response.data);
            } catch (err) {
                setError(t('cannotConnectServer'));
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchDailyShiftSummary();
    }, [date, machines, machineId, productId, ctxMachineName]);

    if (loading) return <div className="loading-container"><h2>{t('loadingDailyShiftSummary')}</h2></div>;
    
    if (error) return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('back')}</button>

            <p className="error-message">{error}</p>
            <div className="buttons-section" style={{ marginTop: '1rem' }}>
                <button onClick={onSelectNewDate} className="search-button">
                    {t('selectOtherDate')}
                </button>
            </div>
        </div>
    );
    
    if (!summary) return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('back')}</button>

            <p>{t('noData')}</p>
            <div className="buttons-section" style={{ marginTop: '1rem' }}>
                <button onClick={onSelectNewDate} className="search-button">
                    {t('selectOtherDate')}
                </button>
            </div>
        </div>
    );

    // Debug information - ลบออกเมื่อแน่ใจว่าทำงานถูกต้อง
    console.log('Daily Shift Summary Data:', summary);

    // Calculate shift metrics
    const calculateShiftMetrics = (shiftData) => {
        if (!shiftData) return { targetQty: 0, goodQty: 0, ngQty: 0, scrapWeight: 0, yieldPercent: '0.00' };

        // เป้าหมายต้องมาจาก targetQty เท่านั้น ห้ามใช้ยอดผลิตจริงเป็น fallback
        const targetRaw = (typeof shiftData.targetQty !== 'undefined') ? shiftData.targetQty : 0;
        const target = typeof targetRaw === 'number' ? targetRaw : parseInt(targetRaw, 10) || 0;
        const good = shiftData.goodQty || shiftData.goodProductionPieces || 0;
        const ng = shiftData.ngQty || shiftData.ngProductionPieces || 0;
        const weight = shiftData.scrapWeight || (shiftData.totalScrapWeight ? parseFloat(shiftData.totalScrapWeight) : 0);

        // ใช้ yieldPercentage จาก backend หากมี หรือคำนวณเองจากเป้าหมาย
        let yieldValue = '0.00';
        if (shiftData.yieldPercentage && shiftData.yieldPercentage !== '0%') {
            yieldValue = shiftData.yieldPercentage.replace('%', '');
        } else if (target > 0) {
            yieldValue = ((good / target) * 100).toFixed(2);
        }

        return {
            targetQty: target,
            goodQty: good,
            ngQty: ng,
            scrapWeight: weight,
            yieldPercent: yieldValue
        };
    };

    // น้ำหนักของเสียจาก Technician: รวมเฉพาะรายการที่เป็นน้ำหนัก (weightKg > 0)
    const getTechnicianScrapWeight = (shiftData) => {
        try {
            const list = Array.isArray(shiftData?.ngSummary) ? shiftData.ngSummary : [];
            return list.filter(x => (parseFloat(x?.weightKg) || 0) > 0)
                .reduce((sum, x) => sum + (parseFloat(x.weightKg) || 0), 0);
        } catch (e) {
            return 0;
        }
    };

    const dayMetrics = calculateShiftMetrics(summary.dayShiftData);
    const nightMetrics = calculateShiftMetrics(summary.nightShiftData);
    const dayTechScrap = getTechnicianScrapWeight(summary.dayShiftData);
    const nightTechScrap = getTechnicianScrapWeight(summary.nightShiftData);
    
    // Calculate combined totals
    const totalTarget = dayMetrics.targetQty + nightMetrics.targetQty;
    const totalGood = dayMetrics.goodQty + nightMetrics.goodQty;
    const totalNg = dayMetrics.ngQty + nightMetrics.ngQty;
    const totalScrap = dayMetrics.scrapWeight + nightMetrics.scrapWeight;
    const totalTechScrap = dayTechScrap + nightTechScrap;
    const overallYield = totalTarget > 0 ? ((totalGood / totalTarget) * 100).toFixed(2) : '0.00';

    return (
        <div className="dashboard-card">
            <div className="dashboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <button onClick={onBack} className="back-button">&larr; {t('backToHistory')}</button>
                <button onClick={onSelectNewDate} className="edit-button">
                    📅 {t('selectOtherDate')}
                </button>
            </div>
            
            <h2 className="dashboard-title">{t('dailyShiftDetailTitle')}: {new Date(date).toLocaleDateString(getLang() === 'th' ? 'th-TH' : 'en-US', {
                year: 'numeric',
                month: 'long', 
                day: 'numeric',
                weekday: 'long'
            })}</h2>

            {/* Machine selector removed as per requirement */}

            {/* ข้อมูลเบื้องต้น */}
            <div className="summary-panel" style={{ marginTop: '1rem', backgroundColor: '#e3f2fd', padding: '1rem', borderRadius: '8px' }}>
                <h3 className="summary-title">📋 {t('productionDetails')}</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' }}>
                    <div className="info-card" style={{ padding: '0.75rem', backgroundColor: 'white', borderRadius: '6px', border: '1px solid #e0e0e0' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#1976d2' }}>📄 {t('orderNumber')}</h4>
                        <p style={{ margin: 0, fontSize: '1.1rem', fontWeight: 'bold', color: summary.orderNumber && summary.orderNumber !== 'ไม่มีข้อมูล' ? '#333' : '#999' }}>
                            {summary.orderNumber || summary.dayShiftData?.orderNumber || summary.nightShiftData?.orderNumber || 'ไม่มีข้อมูล'}
                        </p>
                    </div>
                    <div className="info-card" style={{ padding: '0.75rem', backgroundColor: 'white', borderRadius: '6px', border: '1px solid #e0e0e0' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#1976d2' }}>🏭 {t('machineLabel')}</h4>
                        <p style={{ margin: 0, fontSize: '1.1rem', fontWeight: 'bold', color: (summary.machineName || ctxMachineName) && (summary.machineName || ctxMachineName) !== 'ไม่มีข้อมูล' ? '#333' : '#999' }}>
                            {summary.machineName || ctxMachineName || summary.dayShiftData?.machineName || summary.nightShiftData?.machineName || 'ไม่มีข้อมูล'}
                        </p>
                    </div>
                    <div className="info-card" style={{ padding: '0.75rem', backgroundColor: 'white', borderRadius: '6px', border: '1px solid #e0e0e0' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#1976d2' }}>📦 {t('productLabel')}</h4>
                        <p style={{ margin: 0, fontSize: '1.1rem', fontWeight: 'bold', color: (summary.productName || ctxProductName) && (summary.productName || ctxProductName) !== 'ไม่มีข้อมูล' ? '#333' : '#999' }}>
                            {summary.productName || ctxProductName || summary.dayShiftData?.productName || summary.nightShiftData?.productName || 'ไม่มีข้อมูล'}
                        </p>
                    </div>
                    <div className="info-card" style={{ padding: '0.75rem', backgroundColor: 'white', borderRadius: '6px', border: '1px solid #e0e0e0' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#1976d2' }}>🎯 {t('planTargetPieces')}</h4>
                        <p style={{ margin: 0, fontSize: '1.1rem', fontWeight: 'bold', color: '#333' }}>
                            {(((summary.dayShiftData?.targetQty || 0) + (summary.nightShiftData?.targetQty || 0)) || 0).toLocaleString()}
                        </p>
                    </div>
                </div>
            </div>

            {/* สรุปรวมทั้งวัน */}
            <div className="summary-panel" style={{ marginTop: '1rem', backgroundColor: '#f8f9fa', padding: '1rem', borderRadius: '8px' }}>
                <h3 className="summary-title">📊 {t('dailySummary')}</h3>
                <div className="kpi-grid pc-kpi" style={{gridTemplateColumns: 'repeat(5, 1fr)'}}>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('totalGoodAll')}</span>
                        <span className="kpi-value">{totalGood.toLocaleString()}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('totalNgAll')}</span>
                        <span className="kpi-value ng-value">{totalNg.toLocaleString()}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('totalScrapWeightLabel')}</span>
                        <span className="kpi-value ng-value">{totalScrap.toFixed(2)} {t('kgUnit')}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('technicianScrapWeightLabel')}</span>
                        <span className="kpi-value ng-value">{totalTechScrap.toFixed(2)} {t('kgUnit')}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('yieldLabel')}</span>
                        <span className="kpi-value yield-value">{overallYield}%</span>
                    </div>
                </div>
            </div>

            {/* ข้อมูลกะเช้า */}
            <div className="summary-panel" style={{ marginTop: '2rem' }}>
                <h3 className="summary-title" style={{ color: '#ff9800' }}>🌅 {t('morningShift')}</h3>
                
                {/* KPI กะเช้า */}
                <div className="kpi-grid pc-kpi" style={{gridTemplateColumns: 'repeat(5, 1fr)', marginBottom: '1.5rem'}}>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('good')}</span>
                        <span className="kpi-value">{dayMetrics.goodQty.toLocaleString()}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('ng')}</span>
                        <span className="kpi-value ng-value">{dayMetrics.ngQty.toLocaleString()}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('scrapWeightLabel')}</span>
                        <span className="kpi-value ng-value">{dayMetrics.scrapWeight.toFixed(2)} {t('kgUnit')}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('technicianScrapWeightLabel')}</span>
                        <span className="kpi-value ng-value">{dayTechScrap.toFixed(2)} {t('kgUnit')}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('yieldLabel')}</span>
                        <span className="kpi-value yield-value">{dayMetrics.yieldPercent}%</span>
                    </div>
                </div>

                {/* หัวหน้ากะและพนักงานกะเช้า */}
                <div className="shift-details" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                    <div className="shift-workers" style={{ padding: '1rem', backgroundColor: '#fff3e0', borderRadius: '8px', border: '1px solid #ffcc02' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#f57c00' }}>👥 {t('shiftPersonnelMorningTitle')}</h4>
                        <p><strong>{t('shiftLeader')}:</strong> {summary.dayShiftData?.shiftLeader || t('notSpecified')}</p>
                        <p><strong>{t('workers')}:</strong> {summary.dayShiftData?.workers || t('notSpecified')}</p>
                    </div>
                    <div className="shift-count" style={{ padding: '1rem', backgroundColor: '#e8f5e8', borderRadius: '8px', border: '1px solid #4caf50' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#2e7d32' }}>👨‍👩‍👧‍👦 {t('headcount')}</h4>
                        {(() => {
                            const map = parseWorkersByRole(summary.dayShiftData?.workers);
                            const roles = Object.keys(map);
                            const counts = roles.reduce((s, r) => s + (map[r]?.length || 0), 0);
                            const total = counts + (summary.dayShiftData?.shiftLeader ? 1 : 0);
                            return (<p style={{ fontSize: '2rem', margin: 0, fontWeight: 'bold', color: '#2e7d32' }}>{total} {t('peopleUnit')}</p>);
                        })()}
                    </div>
                </div>

                {/* รายละเอียดเพิ่มเติมกะเช้า */}
                <div className="details-section" style={{ marginTop: '1rem' }}>
                    <button 
                        onClick={() => setShowDayDetails(!showDayDetails)}
                        className="details-toggle-button"
                        style={{
                            backgroundColor: '#ff9800',
                            color: 'white',
                            border: 'none',
                            padding: '0.5rem 1rem',
                            borderRadius: '6px',
                            cursor: 'pointer',
                            fontSize: '0.9rem',
                            marginBottom: '1rem'
                        }}
                    >
                        {showDayDetails ? `🔼 ${t('hideDetails')}` : `🔽 ${t('showDetails')}`} {t('morningShift')}
                    </button>
                    
                    {showDayDetails && summary.dayShiftData && (
                        <DataSummaryPanel title={`${t('morningShift')} - ${t('productionDetails')}`} data={summary.dayShiftData} />
                    )}
                </div>
            </div>

            {/* ข้อมูลกะกลางคืน */}
            <div className="summary-panel" style={{ marginTop: '2rem' }}>
                <h3 className="summary-title" style={{ color: '#3f51b5' }}>🌙 {t('nightShift')}</h3>
                
                {/* KPI กะกลางคืน */}
                <div className="kpi-grid pc-kpi" style={{gridTemplateColumns: 'repeat(5, 1fr)', marginBottom: '1.5rem'}}>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('good')}</span>
                        <span className="kpi-value">{nightMetrics.goodQty.toLocaleString()}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('ng')}</span>
                        <span className="kpi-value ng-value">{nightMetrics.ngQty.toLocaleString()}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('scrapWeightLabel')}</span>
                        <span className="kpi-value ng-value">{nightMetrics.scrapWeight.toFixed(2)} {t('kgUnit')}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('technicianScrapWeightLabel')}</span>
                        <span className="kpi-value ng-value">{nightTechScrap.toFixed(2)} {t('kgUnit')}</span>
                    </div>
                    <div className="kpi-card">
                        <span className="kpi-label">{t('yieldLabel')}</span>
                        <span className="kpi-value yield-value">{nightMetrics.yieldPercent}%</span>
                    </div>
                </div>

                {/* หัวหน้ากะและพนักงานกะกลางคืน */}
                <div className="shift-details" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                    <div className="shift-workers" style={{ padding: '1rem', backgroundColor: '#e8eaf6', borderRadius: '8px', border: '1px solid #3f51b5' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#3f51b5' }}>👥 {t('shiftPersonnelNightTitle')}</h4>
                        <p><strong>{t('shiftLeader')}:</strong> {summary.nightShiftData?.shiftLeader || t('notSpecified')}</p>
                        <p><strong>{t('workers')}:</strong> {summary.nightShiftData?.workers || t('notSpecified')}</p>
                    </div>
                    <div className="shift-count" style={{ padding: '1rem', backgroundColor: '#e3f2fd', borderRadius: '8px', border: '1px solid #2196f3' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: '#1976d2' }}>👨‍👩‍👧‍👦 {t('headcount')}</h4>
                        {(() => {
                            const map = parseWorkersByRole(summary.nightShiftData?.workers);
                            const roles = Object.keys(map);
                            const counts = roles.reduce((s, r) => s + (map[r]?.length || 0), 0);
                            const total = counts + (summary.nightShiftData?.shiftLeader ? 1 : 0);
                            return (<p style={{ fontSize: '2rem', margin: 0, fontWeight: 'bold', color: '#1976d2' }}>{total} {t('peopleUnit')}</p>);
                        })()}
                    </div>
                </div>

                {/* รายละเอียดเพิ่มเติมกะกลางคืน */}
                <div className="details-section" style={{ marginTop: '1rem' }}>
                    <button 
                        onClick={() => setShowNightDetails(!showNightDetails)}
                        className="details-toggle-button"
                        style={{
                            backgroundColor: '#3f51b5',
                            color: 'white',
                            border: 'none',
                            padding: '0.5rem 1rem',
                            borderRadius: '6px',
                            cursor: 'pointer',
                            fontSize: '0.9rem',
                            marginBottom: '1rem'
                        }}
                    >
                        {showNightDetails ? `🔼 ${t('hideDetails')}` : `🔽 ${t('showDetails')}`} {t('nightShift')}
                    </button>
                    
                    {showNightDetails && summary.nightShiftData && (
                        <DataSummaryPanel title={`${t('nightShift')} - ${t('productionDetails')}`} data={summary.nightShiftData} />
                    )}
                </div>
            </div>

            {/* สรุปบุคลากรทั้งหมด */}
            <div className="summary-panel" style={{ marginTop: '2rem' }}>
                <h3 className="summary-title">👥 {t('totalWorkersSummaryTitle')}</h3>
                <div className="total-workers" style={{ padding: '1rem', backgroundColor: '#e8f4fd', borderRadius: '8px', border: '1px solid #b3d9f2' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' }}>
                        <div className="worker-stat">
                            <h4 style={{ margin: '0 0 0.5rem 0', color: '#0056b3' }}>{t('totalAll')}</h4>
                            <p style={{ fontSize: '1.5rem', margin: 0, fontWeight: 'bold' }}>
                                {(() => {
                                    const dm = parseWorkersByRole(summary.dayShiftData?.workers);
                                    const dn = parseWorkersByRole(summary.nightShiftData?.workers);
                                    const day = Object.values(dm).reduce((s, arr) => s + arr.length, 0) + (summary.dayShiftData?.shiftLeader ? 1 : 0);
                                    const night = Object.values(dn).reduce((s, arr) => s + arr.length, 0) + (summary.nightShiftData?.shiftLeader ? 1 : 0);
                                    return day + night;
                                })()} {t('peopleUnit')}
                            </p>
                        </div>
                        <div className="worker-stat">
                            <h4 style={{ margin: '0 0 0.5rem 0', color: '#0056b3' }}>{t('supervisors')}</h4>
                            <p style={{ fontSize: '1.5rem', margin: 0, fontWeight: 'bold' }}>2 {t('peopleUnit')}</p>
                        </div>
                        <div className="worker-stat">
                            <h4 style={{ margin: '0 0 0.5rem 0', color: '#0056b3' }}>{t('regularWorkers')}</h4>
                            <p style={{ fontSize: '1.5rem', margin: 0, fontWeight: 'bold' }}>
                                {(() => {
                                    const dm = parseWorkersByRole(summary.dayShiftData?.workers);
                                    const dn = parseWorkersByRole(summary.nightShiftData?.workers);
                                    const day = Object.values(dm).reduce((s, arr) => s + arr.length, 0);
                                    const night = Object.values(dn).reduce((s, arr) => s + arr.length, 0);
                                    return day + night;
                                })()} {t('peopleUnit')}
                            </p>
                        </div>
                        <div className="worker-stat">
                            <h4 style={{ margin: '0 0 0.5rem 0', color: '#0056b3' }}>{t('averagePerShift')}</h4>
                            <p style={{ fontSize: '1.5rem', margin: 0, fontWeight: 'bold' }}>
                                {(() => {
                                    const dm = parseWorkersByRole(summary.dayShiftData?.workers);
                                    const dn = parseWorkersByRole(summary.nightShiftData?.workers);
                                    const day = Object.values(dm).reduce((s, arr) => s + arr.length, 0) + (summary.dayShiftData?.shiftLeader ? 1 : 0);
                                    const night = Object.values(dn).reduce((s, arr) => s + arr.length, 0) + (summary.nightShiftData?.shiftLeader ? 1 : 0);
                                    return ((day + night) / 2).toFixed(1);
                                })()} {t('peopleUnit')}
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

const ProductionOrderManagement = ({ onBack, onViewDetail, machines, products, allReports, fetchData }) => {
    const { user } = useAuth();
    const role = (user?.role || '').toString();
    const canManage = role === 'Production Control' || role === 'DataAdmin';
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingReport, setEditingReport] = useState(null);
    const [filteredReports, setFilteredReports] = useState([]);
    const [statusFilter, setStatusFilter] = useState('IN_PROGRESS');
    const initialFormData = {
        orderNumber: '', startDate: new Date().toISOString().split('T')[0], endDate: new Date().toISOString().split('T')[0],
        machineId: '', productId: '', targetQty: ''
    };
    const [formData, setFormData] = useState(initialFormData);

    useEffect(() => {
        let reports = Array.isArray(allReports) ? [...allReports] : [];
        if (statusFilter !== 'all') { reports = reports.filter(r => r.status === statusFilter); }
        setFilteredReports(reports);
    }, [statusFilter, allReports]);

    const handleFormChange = (e) => { const { name, value } = e.target; setFormData(prev => ({ ...prev, [name]: value })); };
    const handleFormSubmit = async (e) => {
        e.preventDefault();
        const apiCall = editingReport ? api.put(`/pc/reports/${editingReport.id}`, formData) : api.post('/pc/reports', formData);
        try { await apiCall; alert(editingReport ? 'แก้ไขใบสั่งผลิตสำเร็จ!' : 'สร้างใบสั่งผลิตสำเร็จ!'); handleCloseModal(); fetchData(); } 
        catch (err) { alert(err.response?.data || 'เกิดข้อผิดพลาดในการบันทึกข้อมูล'); }
    };

    const handleOpenCreateModal = () => { if (!canManage) return; setEditingReport(null); setFormData(initialFormData); setIsModalOpen(true); };
    const handleOpenEditModal = (report) => {
        if (!canManage) return;
        setEditingReport(report);
        const machine = machines.find(m => m.machineName === report.machineName);
        const product = products.find(p => p.productName === report.productName);
        setFormData({ orderNumber: report.orderNumber || '', startDate: report.startDate, endDate: report.endDate, machineId: machine ? machine.id : '', productId: product ? product.id : '', targetQty: report.targetQty || '' });
        setIsModalOpen(true);
    };
    const handleCloseModal = () => { setIsModalOpen(false); setEditingReport(null); };
    const handleFinalize = async (reportId) => { if (!canManage) return; if (window.confirm('คุณต้องการปิดงานใบสั่งผลิตนี้ใช่หรือไม่?')) { try { await api.post(`/pc/reports/${reportId}/finalize`); alert('ปิดงานสำเร็จ!'); fetchData(); } catch (err) { alert('เกิดข้อผิดพลาดในการปิดงาน'); } } };
    const handleDelete = async (reportId) => { if (!canManage) return; if (window.confirm('คุณแน่ใจหรือไม่ว่าต้องการลบใบสั่งผลิตนี้?')) { try { await api.delete(`/pc/reports/${reportId}`); alert('ลบใบสั่งผลิตสำเร็จ!'); fetchData(); } catch (err) { alert(err.response?.data || 'เกิดข้อผิดพลาดในการลบข้อมูล'); } } };

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('backToOverview')}</button>
            <h2 className="dashboard-title">{t('manageProductionOrders')}</h2>
            <div className="table-header">
                <h3>{t('productionOverviewTitle')}</h3>
                <div>
                  <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="form-input" style={{marginRight: '1rem', display: 'inline-block', width: 'auto'}}>
                    <option value="all">{getLang() === 'en' ? 'All' : 'ทั้งหมด'}</option>
                    <option value="IN_PROGRESS">{getLang() === 'en' ? 'In Progress' : 'กำลังดำเนินการ'}</option>
                    <option value="PENDING">{getLang() === 'en' ? 'Pending (Not started)' : 'พร้อมทำงาน (รอเริ่ม)'}</option>
                    <option value="ACTIVE">{getLang() === 'en' ? 'Expired (Not closed)' : 'หมดเวลา (รอปิดงาน)'}</option>
                    <option value="INACTIVE">{getLang() === 'en' ? 'Closed' : 'ปิดงานแล้ว'}</option>
                  </select>
                  {canManage && <button className="add-button" onClick={handleOpenCreateModal}>{t('createNewOrder') || 'สร้างใบสั่งผลิตใหม่'}</button>}
                </div>
            </div>
            <div className="data-table-container">
                <table className="data-table">
                   <thead><tr><th>Order No.</th><th>{t('dateLabel')}</th><th>{t('machineLabel')}</th><th>{t('productLabel')}</th><th>{t('statusLabel')}</th><th>{t('actionsLabel')}</th></tr></thead>
                   <tbody>
                        {Array.isArray(filteredReports) && filteredReports.map(report => (
                           <tr key={report.id}>
                               <td>{report.orderNumber}</td><td>{report.startDate} - {report.endDate}</td><td>{report.machineName}</td>
                               <td>{report.productName}</td>
                               <td>
                                   <span className={`status-${statusClass(report.status)}`}>
                                       {statusDisplay(report.status)}
                                   </span>
                               </td>
                               <td className="actions-cell">
                                    <button className="add-button" onClick={() => onViewDetail(report.id)}>{t('orderOverview')}</button>
                                    {canManage && <button className="edit-button" disabled={!report.editable} onClick={() => handleOpenEditModal(report)}>{t('edit') || 'แก้ไข'}</button>}
                                    {canManage && <button className="finalize-button" disabled={!report.finalizable} onClick={() => handleFinalize(report.id)}>{t('finalize') || 'ปิดงาน'}</button>}
                                    {canManage && <button className="delete-button" disabled={!report.deletable} onClick={() => handleDelete(report.id)}>{t('delete') || 'ลบ'}</button>}
                               </td>
                           </tr>
                       ))}
                   </tbody>
                </table>
            </div>
            {canManage && (
            <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={editingReport ? (t('editOrder') || 'แก้ไขใบสั่งผลิต') : (t('createNewOrder') || 'สร้างใบสั่งผลิตใหม่')}>
                <form onSubmit={handleFormSubmit} className="production-form">
                    <div className="form-group"><label className="form-label">{t('orderNumber')}</label><input type="text" name="orderNumber" value={formData.orderNumber} onChange={handleFormChange} className="form-input" /></div>
                    <div className="form-group"><label className="form-label">{t('startDate')}</label><input type="date" name="startDate" value={formData.startDate} onChange={handleFormChange} className="form-input" required /></div>
                    <div className="form-group"><label className="form-label">{t('endDate')}</label><input type="date" name="endDate" value={formData.endDate} onChange={handleFormChange} className="form-input" required /></div>
                    <div className="form-group"><label className="form-label">{t('machineLabel')}</label><select name="machineId" value={formData.machineId} onChange={handleFormChange} className="form-input" required><option value="" disabled>--</option>{Array.isArray(machines) && machines.map(m => <option key={m.id} value={m.id}>{m.machineName}</option>)}</select></div>
                    <div className="form-group"><label className="form-label">{t('productLabel')}</label><select name="productId" value={formData.productId} onChange={handleFormChange} className="form-input" required><option value="" disabled>--</option>{Array.isArray(products) && products.map(p => <option key={p.id} value={p.id}>{p.productName}</option>)}</select></div>
                    <div className="form-group"><label className="form-label">{t('targetLabel')}</label><input type="number" name="targetQty" value={formData.targetQty} onChange={handleFormChange} className="form-input" /></div>
                    <div className="form-actions"><button type="button" onClick={handleCloseModal} className="cancel-button">{t('back')}</button><button type="submit" className="save-button">{t('save') || 'บันทึก'}</button></div>
                </form>
            </Modal>
            )}
        </div>
    );
};

const HistoricalReports = ({ onBack, onViewDetail, onViewDailyReport, onViewDailyShiftReport, machines, products }) => {
    const [filters, setFilters] = useState({ startDate: new Date().toISOString().split('T')[0], endDate: new Date().toISOString().split('T')[0], machineId: 'all', productId: 'all' });
    const [results, setResults] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleFilterChange = (e) => { const { name, value } = e.target; setFilters(prev => ({ ...prev, [name]: value })); };
    const handleSearch = async () => {
        setLoading(true); 
        setError(''); 
        setResults(null);
        
        console.log('🔍 Starting historical reports search...', filters);
        
        try {
            const params = new URLSearchParams({ 
                startDate: filters.startDate, 
                endDate: filters.endDate 
            });
            
            if (filters.machineId !== 'all') { 
                params.append('machineId', filters.machineId); 
            }
            if (filters.productId !== 'all') { 
                params.append('productId', filters.productId); 
            }
            
            const url = `/production/reports/production-historical?${params.toString()}`;  // Fixed: removed /api prefix (baseURL has it)
            console.log('📡 API URL:', url);
            
            const response = await api.get(url);
            console.log('📥 API Response:', response);
            
            // Handle different response structures
            let data = response.data;
            if (data && data.data) {
                // Backend sends {data: [...], status: "success"}
                data = data.data;
            }
            
            console.log('📊 Processed data:', data);
            
            if (Array.isArray(data)) {
                setResults(data);
                if (data.length === 0) {
                    setError('ไม่มีข้อมูลรายงานในช่วงวันที่ที่ระบุ');
                }
            } else {
                console.warn('⚠️ Data is not an array:', data);
                setError('รูปแบบข้อมูลไม่ถูกต้อง');
                setResults([]);
            }
            
        } catch (err) { 
            console.error('❌ Search error:', err);
            console.error('❌ Error response:', err.response?.data);
            console.error('❌ Error status:', err.response?.status);
            
            if (err.response?.status === 401) {
                setError('กรุณาเข้าสู่ระบบใหม่');
            } else if (err.response?.status === 500) {
                setError('เกิดข้อผิดพลาดที่เซิร์ฟเวอร์');
            } else if (err.response?.data?.error) {
                setError(err.response.data.error);
            } else {
                setError('ไม่สามารถดึงข้อมูลรายงานได้ หรือไม่มีข้อมูลในช่วงที่เลือก');
            }
        } finally { 
            setLoading(false); 
        }
    };

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; {t('backToOverview') || 'Back to overview'}</button>
            <h2 className="dashboard-title">{t('historicalReports')}</h2>
            <div className="filter-panel" style={{display: 'flex', gap: '1rem', alignItems: 'flex-end', marginBottom: '1.5rem'}}>
                <div className="form-group"><label className="form-label">{t('startDate')}</label><input type="date" name="startDate" value={filters.startDate} onChange={handleFilterChange} className="form-input" /></div>
                <div className="form-group"><label className="form-label">{t('endDate')}</label><input type="date" name="endDate" value={filters.endDate} onChange={handleFilterChange} className="form-input" /></div>
                <div className="form-group"><label className="form-label">{t('machineLabel')}</label><select name="machineId" value={filters.machineId} onChange={handleFilterChange} className="form-input"><option value="all">{t('all')}</option>{Array.isArray(machines) && machines.map(m => <option key={m.id} value={m.id}>{m.machineName}</option>)}</select></div>
                <div className="form-group"><label className="form-label">{t('productLabel')}</label><select name="productId" value={filters.productId} onChange={handleFilterChange} className="form-input"><option value="all">{t('all')}</option>{Array.isArray(products) && products.map(p => <option key={p.id} value={p.id}>{p.productName}</option>)}</select></div>
                <button onClick={handleSearch} disabled={loading} className="submit-button" style={{height: '42px'}}>{loading ? t('searching') : t('search')}</button>
            </div>
            
            {/* Loading state */}
            {loading && (
                <div className="loading-message" style={{textAlign: 'center', padding: '2rem'}}>
                    <p>🔍 {t('searching')}</p>
                </div>
            )}
            
            {/* Error message */}
            {error && !loading && (
                <div className="error-message" style={{color: 'red', padding: '1rem', backgroundColor: '#ffebee', borderRadius: '4px', margin: '1rem 0'}}>
                    <p>⚠️ {error}</p>
                </div>
            )}
            
            {/* Results table */}
            {results && Array.isArray(results) && results.length > 0 && !loading && (
                <div className="results-section">
                    <div style={{marginBottom: '1rem'}}>
                        <p><strong>✅ {results.length} {t('recordsLabel') || 'records found'}</strong></p>
                    </div>
                    <div className="data-table-container" style={{marginTop: '1rem'}}>
                         <table className="data-table">
                            <thead><tr><th>Order No.</th><th>{t('dateLabel')}</th><th>{t('machineLabel')}</th><th>{t('productLabel')}</th><th>{t('statusLabel')}</th><th>{t('actionsLabel')}</th></tr></thead>
                            <tbody>
                                {results.map(report => (<tr key={report.id}><td>{report.orderNumber}</td><td>{report.startDate} - {report.endDate}</td><td>{report.machineName}</td><td>{report.productName}</td><td><span className={`status-${statusClass(report.status)}`}>{statusDisplay(report.status)}</span></td><td className="actions-cell" style={{flexDirection: 'column', alignItems: 'stretch', gap: '0.25rem'}}>
                                    <button className="add-button" onClick={() => onViewDetail(report.id)}>1. {t('orderOverview')}</button>
                                    <button className="edit-button" onClick={() => onViewDailyReport(report)}>2. {t('dailySummary')}</button>
                                    <button className="finalize-button" onClick={() => onViewDailyShiftReport(report)}>3. {t('dailyShiftSummary')}</button>
                                </td></tr>))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
            
            {/* No data message */}
            {results && Array.isArray(results) && results.length === 0 && !loading && (
                <div className="no-data-message" style={{textAlign: 'center', padding: '2rem', backgroundColor: '#f5f5f5', borderRadius: '4px', margin: '1rem 0'}}>
                    <p>📊 {t('noResultsInRange')}</p>
                    <p style={{fontSize: '0.9rem', color: '#666'}}>{t('hintChangeFilters')}</p>
                </div>
            )}
        </div>
    );
};

const HistoricalReportsSection = () => {
    const [filters, setFilters] = useState({
        startDate: new Date().toISOString().split('T')[0],
        endDate: new Date().toISOString().split('T')[0],
        machineId: 'all',
        productId: 'all'
    });
    
    const [results, setResults] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [hasSearched, setHasSearched] = useState(false); // เพิ่มตัวแปรเพื่อติดตามว่ามีการค้นหาแล้วหรือยัง
    const [machines, setMachines] = useState([]);
    const [products, setProducts] = useState([]);
    const [debugInfo, setDebugInfo] = useState({
        apiCalled: false,
        requestData: null,
        responseData: null,
        error: null
    });

    // โหลดข้อมูลเครื่องจักรและผลิตภัณฑ์เมื่อ component โหลด
    useEffect(() => {
        const fetchDropdownData = async () => {
            try {
                const [machinesRes, productsRes] = await Promise.all([
                    api.get('/production/machines'),  // use shared client for lang/header
                    api.get('/production/products')
                ]);
                // Backend ส่งข้อมูลในรูปแบบ {data: [...], status: "success"}
                setMachines(machinesRes.data?.data || []);
                setProducts(productsRes.data?.data || []);
            } catch (error) {
                console.error("Error fetching dropdown data:", error);
                setError(t('failedToLoadDropdowns') || 'Failed to load machines and products');
            }
        };
        
        fetchDropdownData();
    }, []);

    // อัปเดตค่าใน filters เมื่อผู้ใช้เปลี่ยนค่าในฟอร์ม
    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(prevFilters => ({
            ...prevFilters,
            [name]: value
        }));
    };

    // ค้นหาข้อมูลเมื่อกดปุ่มค้นหา
    const handleSearch = async (e) => {
        if (e) e.preventDefault();
        setLoading(true);
        setError('');
        setHasSearched(true); // กำหนดว่ามีการค้นหาแล้ว
        
        // กำหนดว่า API ถูกเรียกแล้ว
        setDebugInfo(prev => ({
            ...prev,
            apiCalled: true,
            requestData: {
                startDate: filters.startDate,
                endDate: filters.endDate,
                machineId: filters.machineId,
                productId: filters.productId
            }
        }));
        
        try {
            // ตรวจสอบว่าเข้าสู่ระบบอยู่หรือไม่
            const token = localStorage.getItem('token');
            if (!token) {
                setError(t('pleaseLogin'));
                setLoading(false);
                return;
            }

            // แปลงวันที่ให้อยู่ในรูปแบบที่ถูกต้อง
            const formattedStartDate = filters.startDate ? new Date(filters.startDate).toISOString().split('T')[0] : '';
            const formattedEndDate = filters.endDate ? new Date(filters.endDate).toISOString().split('T')[0] : '';
            
            if (!formattedStartDate || !formattedEndDate) {
                setError(t('requireStartEndDate') || 'Please provide start and end dates');
                setLoading(false);
                return;
            }
            
            const params = new URLSearchParams();
            params.append('startDate', formattedStartDate);
            params.append('endDate', formattedEndDate);
            
            if (filters.machineId && filters.machineId !== 'all') {
              params.append('machineId', filters.machineId);
            }
            
            if (filters.productId && filters.productId !== 'all') {
              params.append('productId', filters.productId);
            }
            
            console.log('Sending request with params:', params.toString());
            
            const url = `/production/reports/production-historical?${params}`;  // Fixed: removed /api prefix (baseURL has it)
            console.log('Calling API:', url);
            
                        const response = await api.get(url);
            console.log('API Response:', response);
            
            // บันทึกข้อมูล response สำหรับ debug
            setDebugInfo(prev => ({
              ...prev,
              responseData: response.data,
              error: null
            }));
            
            if (Array.isArray(response.data)) {
                setResults(response.data);
                if (response.data.length === 0) {
                    setError(t('noResultsInRange'));
                }
            } else {
                console.error('Invalid response format:', response.data);
                setError(t('invalidDataFormat') || 'Invalid data format received');
                setResults([]);
            }
        } catch (err) {
            console.error('Error fetching historical reports:', err);
            
            // บันทึกข้อผิดพลาดสำหรับ debug
            setDebugInfo(prev => ({
              ...prev,
              error: {
                message: err.message,
                response: err.response?.data,
                status: err.response?.status
              }
            }));
            
                        if (err.response) {
                            setError(err.response.status === 401 ? t('pleaseLogin') : `Error: ${err.response.status} ${err.response.statusText || ''}`);
                        } else if (err.request) {
                            setError(t('cannotConnectServer') || 'Cannot connect to server');
                        } else {
                            setError(`Error: ${err.message}`);
            }
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="dashboard-card">
            <h2>{t('historicalReports')}</h2>
            
            {/* ส่วนฟอร์มค้นหา */}
            <form onSubmit={handleSearch} className="filters-section">
                <div className="filters-grid">
                    <div className="filter-group">
                        <label htmlFor="startDate">{t('startDate')}</label>
                        <input
                            type="date"
                            id="startDate"
                            name="startDate"
                            value={filters.startDate}
                            onChange={handleFilterChange}
                            required
                        />
                    </div>
                    
                    <div className="filter-group">
                        <label htmlFor="endDate">{t('endDate')}</label>
                        <input
                            type="date"
                            id="endDate"
                            name="endDate"
                            value={filters.endDate}
                            onChange={handleFilterChange}
                            required
                        />
                    </div>
                    
                    <div className="filter-group">
                        <label htmlFor="machineId">{t('machineLabel')}</label>
                        <select
                            id="machineId"
                            name="machineId"
                            value={filters.machineId}
                            onChange={handleFilterChange}
                        >
                            <option value="all">{t('all')}</option>
                            {Array.isArray(machines) && machines.map(machine => (
                                <option key={machine.id} value={machine.id}>
                                    {machine.machineName}
                                </option>
                            ))}
                        </select>
                    </div>
                    
                    <div className="filter-group">
                        <label htmlFor="productId">{t('productLabel')}</label>
                        <select
                            id="productId"
                            name="productId"
                            value={filters.productId}
                            onChange={handleFilterChange}
                        >
                            <option value="all">{t('all')}</option>
                            {Array.isArray(products) && products.map(product => (
                                <option key={product.id} value={product.id}>
                                    {product.productName}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>
                
                <div className="buttons-section">
                    <button type="submit" className="search-button" disabled={loading}>
                        {loading ? t('searching') : t('search')}
                    </button>
                </div>
            </form>
            
            {/* แสดงข้อความผิดพลาด ถ้ามี */}
            {error && <p className="error-message">{error}</p>}
            
            {/* แสดงผลลัพธ์ เฉพาะเมื่อมีการค้นหาแล้วและพบข้อมูล */}
            {hasSearched && (
                Array.isArray(results) && results.length > 0 ? (
                    <HistoricalReports reports={results} />
                ) : (
                    !loading && !error && (
                        <div className="no-data-message">
                            <p>{t('selectFiltersAndSearch')}</p>
                        </div>
                    )
                )
            )}
            
            {/* ถ้ายังไม่ได้ค้นหา และไม่มีข้อความผิดพลาด */}
            {!hasSearched && !error && (
                <div className="instruction-message">
                    <p>{t('selectFiltersAndSearch')}</p>
                </div>
            )}

            {/* ส่วนแสดง Debug Info ในหน้า UI (เฉพาะเวลา development) */}
            {process.env.NODE_ENV === 'development' && (
              <div className="debug-info" style={{margin: '20px', padding: '10px', border: '1px solid #ccc', backgroundColor: '#f8f8f8'}}>
                <h3>Debug Info</h3>
                <div>
                  <strong>API Called:</strong> {debugInfo.apiCalled ? 'Yes' : 'No'}
                </div>
                {debugInfo.requestData && (
                  <div>
                    <strong>Request Data:</strong>
                    <pre>{JSON.stringify(debugInfo.requestData, null, 2)}</pre>
                  </div>
                )}
                {debugInfo.responseData && (
                  <div>
                    <strong>Response Data:</strong>
                    <pre>{JSON.stringify(debugInfo.responseData, null, 2)}</pre>
                  </div>
                )}
                {debugInfo.error && (
                  <div>
                    <strong>Error:</strong>
                    <pre>{JSON.stringify(debugInfo.error, null, 2)}</pre>
                  </div>
                )}
              </div>
            )}
        </div>
    );
};

// --- Main PC Dashboard Component ---
const ProductionControlDashboard = () => {
    const { user } = useAuth();
    const canManage = user?.role === 'Production Control' || user?.role === 'DataAdmin';
    const [view, setView] = useState('dashboard');
    const [previousView, setPreviousView] = useState('dashboard');
    const [selectedReportId, setSelectedReportId] = useState(null);
    const [selectedDate, setSelectedDate] = useState(null);
    // เก็บบริบทของรายวัน เช่น เครื่อง/สินค้า/คำสั่ง เพื่อกรอง API ให้ตรงใบสั่งที่เลือก
    const [dailyContext, setDailyContext] = useState({ date: null, machineId: null, productId: null, orderNumber: null });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [machines, setMachines] = useState([]);
    const [products, setProducts] = useState([]);
    const [allReports, setAllReports] = useState([]);
    const [dashboardData, setDashboardData] = useState([]);

    const fetchData = async () => {
        console.log('🔄 fetchData called, loading state:', loading);
        if (!loading) setLoading(true);
        setError('');
        
        try {
            console.log('📡 Making API calls...');
            
            // Try multiple endpoint combinations to ensure compatibility
            let dashboardRes;
            try {
                // Try new ProductionController endpoint first
                dashboardRes = await api.get('/production/dashboard-summary');
                console.log('✅ Using /production/dashboard-summary endpoint');
            } catch (newEndpointError) {
                console.log('⚠️ New endpoint failed, trying old endpoint...');
                // Fallback to old ProductionControlController endpoint
                dashboardRes = await api.get('/pc/production/dashboard-summary');
                console.log('✅ Using /pc/production/dashboard-summary endpoint');
            }

            const [machinesRes, productsRes, reportsRes] = await Promise.all([
                api.get('/pc/production/machines'), 
                api.get('/pc/production/products'), 
                api.get('/pc/production/reports')
            ]);
            
            console.log('📊 Raw API responses:');
            console.log('- Machines:', machinesRes.data);
            console.log('- Products:', productsRes.data);
            console.log('- Reports:', reportsRes.data);
            console.log('- Dashboard:', dashboardRes.data);
            
            // Handle both direct array response and wrapped response for all endpoints
            const machinesData = machinesRes.data?.data || machinesRes.data || [];
            const productsData = productsRes.data?.data || productsRes.data || [];
            const reportsData = reportsRes.data?.data || reportsRes.data || [];
            let dashboardDataRaw = dashboardRes.data?.data || dashboardRes.data || [];
            
            // Ensure dashboard data is an array
            if (!Array.isArray(dashboardDataRaw)) {
                console.warn('⚠️ Dashboard data is not an array:', dashboardDataRaw);
                dashboardDataRaw = [];
            }
            
            console.log('📋 Processed data:');
            console.log('- Machines count:', machinesData.length);
            console.log('- Products count:', productsData.length);
            console.log('- Reports count:', reportsData.length);
            console.log('- Dashboard count:', dashboardDataRaw.length);
            console.log('- Dashboard items:', dashboardDataRaw);
            
            // เรียงลำดับข้อมูลตาม machine name แล้วตาม product name
            const sortedDashboardData = Array.isArray(dashboardDataRaw) ? 
                dashboardDataRaw.sort((a, b) => {
                    const machineCompare = (a.machineName || '').localeCompare(b.machineName || '');
                    if (machineCompare !== 0) {
                        return machineCompare;
                    }
                    return (a.productName || '').localeCompare(b.productName || '');
                }) : [];

            setMachines(Array.isArray(machinesData) ? machinesData : []);
            setProducts(Array.isArray(productsData) ? productsData : []);
            setAllReports(Array.isArray(reportsData) ? reportsData : []);
            setDashboardData(sortedDashboardData);
            
            console.log('✅ State updated successfully');
            
        } catch (err) {
            console.error("❌ Error fetching data:", err);
            console.error("❌ Error response:", err.response?.data);
            console.error("❌ Error status:", err.response?.status);
            
            if (err.response?.status === 401) {
                setError('กรุณาเข้าสู่ระบบใหม่');
            } else if (err.response?.status === 403) {
                setError('ไม่มีสิทธิ์เข้าถึงข้อมูล');
            } else {
                setError('เกิดข้อผิดพลาดในการดึงข้อมูล: ' + (err.message || 'Unknown error'));
            }
        } finally {
            setLoading(false);
            console.log('🏁 fetchData completed');
        }
    };

    useEffect(() => {
        console.log('🔄 useEffect triggered, view:', view);
        let intervalId = null;
        
        // Add slight delay to ensure component is properly mounted and authenticated
        const timer = setTimeout(() => {
            if (view === 'dashboard') {
                fetchData();
                
                // Set up periodic refresh for dashboard only
                intervalId = setInterval(async () => {
                    console.log('🔄 Periodic dashboard refresh...');
                    try {
                        // Try multiple endpoints for dashboard refresh
                        let dashboardRes;
                        try {
                            dashboardRes = await api.get('/production/dashboard-summary');
                        } catch (newEndpointError) {
                            dashboardRes = await api.get('/pc/production/dashboard-summary');
                        }
                        
                        const dashboardDataRaw = dashboardRes.data?.data || dashboardRes.data || [];
                        if (Array.isArray(dashboardDataRaw)) {
                            // เรียงลำดับข้อมูลตาม machine name แล้วตาม product name
                            const sortedData = dashboardDataRaw.sort((a, b) => {
                                const machineCompare = (a.machineName || '').localeCompare(b.machineName || '');
                                if (machineCompare !== 0) {
                                    return machineCompare;
                                }
                                return (a.productName || '').localeCompare(b.productName || '');
                            });
                            
                            setDashboardData(sortedData);
                            console.log('✅ Dashboard data refreshed:', sortedData.length, 'items (sorted by machine name)');
                        }
                    } catch (error) {
                        console.error('❌ Periodic refresh failed:', error);
                    }
                }, 30000);
            } else {
                fetchData();
            }
        }, 100);
        
        return () => { 
            clearTimeout(timer);
            if (intervalId) { 
                clearInterval(intervalId); 
                console.log('🛑 Cleared refresh interval');
            } 
        };
    }, [view]);

    const changeView = (newView, data = null) => {
        console.log('🔄 ProductionControlDashboard: changeView called with view:', newView, 'data:', data);
        console.log('🔄 Current view:', view, 'Selected date:', selectedDate);
        
        setPreviousView(view);
        if (newView === 'detail') {
            setSelectedReportId(data);
        } else if (newView === 'dailyDetail' || newView === 'dailyShiftDetail') {
            console.log('📅 Setting selectedDate to:', data);
            setSelectedDate(data);
        } else if (newView === 'dailySelector' || newView === 'dailyShiftSelector') {
            console.log('🗑️ Resetting selectedDate');
            setSelectedDate(null); // Reset when going to selector
        }
        console.log('🎯 Setting view to:', newView);
        setView(newView);
    };

    if (loading) return <div className="loading-container"><h2>{t('loadingData')}</h2></div>;
    if (error) return <div className="error-message">{error}</div>;

    // --- View Router ---
        if (view === 'manage') {
                if (!canManage) {
                        return (
                            <div className="pc-dashboard-container">
                                <div className="pc-dashboard-header">
                                    <h2 className="pc-dashboard-title">{t('manageProductionOrders')}</h2>
                                </div>
                                <div className="no-data-card">
                                    <h3>{t('pleaseLogin')}</h3>
                                    <button className="back-button" onClick={() => setView('dashboard')}>&larr; {t('backToOverview')}</button>
                                </div>
                            </div>
                        );
                }
                return <ProductionOrderManagement onBack={() => changeView('dashboard')} onViewDetail={(id) => changeView('detail', id)} machines={machines} products={products} allReports={allReports} fetchData={fetchData}/>;
    } else if (view === 'detail') {
        return <ReportDetailView reportId={selectedReportId} onBack={() => changeView(previousView)} />;
    } else if (view === 'history') {
        return (
            <HistoricalReports 
                onBack={() => changeView('dashboard')} 
                onViewDetail={(id) => changeView('detail', id)} 
                onViewDailyReport={(report) => {
                    // สร้าง context จาก report ที่เลือก เพื่อให้ Daily กรองถูกใบสั่ง
                    const machine = Array.isArray(machines) ? machines.find(m => (m.machineName || '').toString() === (report.machineName || '').toString()) : null;
                    const product = Array.isArray(products) ? products.find(p => (p.productName || '').toString() === (report.productName || '').toString()) : null;
                    const date = report?.startDate || report?.date || new Date().toISOString().split('T')[0];
                    setDailyContext({
                        date,
                        machineId: machine?.id || null,
                        machineName: report?.machineName || machine?.machineName || null,
                        productId: product?.id || null,
                        productName: report?.productName || product?.productName || null,
                        orderNumber: report?.orderNumber || null
                    });
                    // ไปหน้าเลือกวันที่ก่อน เพื่อให้ผู้ใช้เลือกวัน และแสดงรายการวันที่ที่กรองตามเครื่อง/สินค้า/ใบสั่ง
                    changeView('dailySelector');
                }} 
                onViewDailyShiftReport={(report) => {
                    const machine = Array.isArray(machines) ? machines.find(m => (m.machineName || '').toString() === (report.machineName || '').toString()) : null;
                    const product = Array.isArray(products) ? products.find(p => (p.productName || '').toString() === (report.productName || '').toString()) : null;
                    const date = report?.startDate || report?.date || new Date().toISOString().split('T')[0];
                    setDailyContext({
                        date,
                        machineId: machine?.id || null,
                        machineName: report?.machineName || machine?.machineName || null,
                        productId: product?.id || null,
                        productName: report?.productName || product?.productName || null,
                        orderNumber: report?.orderNumber || null
                    });
                    changeView('dailyShiftSelector');
                }} 
                machines={machines} 
                products={products} 
            />
        );
    } else if (view === 'dailyDetail') {
        if (selectedDate) {
            return <DailyReportView 
                date={selectedDate}
                machineId={dailyContext?.machineId}
                productId={dailyContext?.productId}
                orderNumber={dailyContext?.orderNumber}
                machineName={dailyContext?.machineName}
                productName={dailyContext?.productName}
                onBack={() => changeView('history')} 
                onSelectNewDate={() => changeView('dailySelector')}
            />;
        } else {
            return <DailyReportSelector 
                onBack={() => changeView('history')} 
                onDateSelected={(date) => changeView('dailyDetail', date)}
            />;
        }
    } else if (view === 'dailySelector') {
        return <DailyReportSelector 
            onBack={() => changeView('history')} 
            onDateSelected={(date) => changeView('dailyDetail', date)}
            machineId={dailyContext?.machineId || null}
            productId={dailyContext?.productId || null}
            orderNumber={dailyContext?.orderNumber || null}
        />;
    } else if (view === 'dailyShiftDetail') {
        if (selectedDate) {
            return <DailyShiftReportView 
                date={selectedDate}
                machineId={dailyContext?.machineId}
                productId={dailyContext?.productId}
                machineName={dailyContext?.machineName}
                productName={dailyContext?.productName}
                onBack={() => changeView('history')} 
                onSelectNewDate={() => changeView('dailyShiftSelector')}
                machines={machines}
            />;
        } else {
            return <DailyReportSelector 
                onBack={() => changeView('history')} 
                onDateSelected={(date) => changeView('dailyShiftDetail', date)}
                machineId={dailyContext?.machineId || null}
                productId={dailyContext?.productId || null}
                orderNumber={dailyContext?.orderNumber || null}
            />;
        }
    } else if (view === 'dailyShiftSelector') {
        return <DailyReportSelector 
            onBack={() => changeView('history')} 
            onDateSelected={(date) => changeView('dailyShiftDetail', date)}
            machineId={dailyContext?.machineId || null}
            productId={dailyContext?.productId || null}
            orderNumber={dailyContext?.orderNumber || null}
        />;
    } else if (view === 'machineSchedule') {
        return <MachineScheduleCalendar onBack={() => changeView('dashboard')} allReports={allReports} machines={machines} />;
    }

    // Default view: 'dashboard'
    return (
        <div className="pc-dashboard-container">
            <div className="pc-dashboard-header">
                <h2 className="pc-dashboard-title">{t('productionOverviewTitle')}</h2>
                <div>
                    <button className="manage-reports-button" onClick={() => changeView('history')} style={{ marginRight: '1rem' }}>{t('viewHistoricalReports')}</button>
                    <button className="manage-reports-button" onClick={() => changeView('machineSchedule')} style={{ marginRight: '1rem', backgroundColor: '#6f42c1' }}>
                      {getLang() === 'en' ? '📅 Machine Schedule' : '📅 ตารางแผนการผลิต'}
                    </button>
                                        {canManage && (
                      <button className="manage-reports-button" onClick={() => changeView('manage')}>{t('manageProductionOrders')}</button>
                    )}
                </div>
            </div>
            {error && <p className="error-message">{error}</p>}
            {!error && dashboardData.length === 0 &&
                <div className="no-data-card">
                    <h3>{t('noMachinesRunning')}</h3>
                    <p>{t('goToManageToCreate')}</p>
                </div>
            }
            <div className="gauge-grid">
                {Array.isArray(dashboardData) && dashboardData.map(data => (
                    <div key={data.reportId} onClick={() => changeView('detail', data.reportId)} style={{cursor: 'pointer'}}>
                        <GaugeCard
                            machineName={data.machineName}
                            productName={data.productName}
                            target={data.targetQty}
                            current={data.currentGoodQty}
                            ng={data.currentNgQty}
                            status={data.status}
                            statusDisplayName={data.statusDisplayName}
                        />
                    </div>
                ))}
            </div>
        </div>
    );
};

// ─────────────────────────────────────────────────────────────────────────────
// Machine Schedule Calendar — monthly grid: rows = machines, columns = days
// ─────────────────────────────────────────────────────────────────────────────
const MachineScheduleCalendar = ({ onBack, allReports, machines }) => {
    const today = new Date();
    const [year, setYear]   = React.useState(today.getFullYear());
    const [month, setMonth] = React.useState(today.getMonth()); // 0-indexed

    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const days = Array.from({ length: daysInMonth }, (_, i) => i + 1);

    const monthNames = ['มกราคม','กุมภาพันธ์','มีนาคม','เมษายน','พฤษภาคม','มิถุนายน',
                        'กรกฎาคม','สิงหาคม','กันยายน','ตุลาคม','พฤศจิกายน','ธันวาคม'];
    const monthNamesEn = ['January','February','March','April','May','June',
                          'July','August','September','October','November','December'];

    const prevMonth = () => { if (month === 0) { setMonth(11); setYear(y => y-1); } else setMonth(m => m-1); };
    const nextMonth = () => { if (month === 11) { setMonth(0);  setYear(y => y+1); } else setMonth(m => m+1); };

    // Build lookup: machineName → list of reports active in this month
    const reports = Array.isArray(allReports) ? allReports : [];

    // Get unique machine names (from reports + master list)
    const machineNames = React.useMemo(() => {
        const fromReports = reports.map(r => r.machineName).filter(Boolean);
        const fromMasters = Array.isArray(machines) ? machines.map(m => m.machineName || m.name).filter(Boolean) : [];
        return [...new Set([...fromMasters, ...fromReports])].sort();
    }, [reports, machines]);

    // Color per status
    const statusColor = (status) => {
        if (!status) return '#adb5bd';
        const s = status.toUpperCase();
        if (s === 'IN_PROGRESS') return '#28a745';
        if (s === 'PENDING')     return '#007bff';
        if (s === 'ACTIVE')      return '#fd7e14';
        if (s === 'INACTIVE')    return '#6c757d';
        return '#adb5bd';
    };
    const statusLabel = (status) => {
        if (!status) return '';
        const s = status.toUpperCase();
        if (s === 'IN_PROGRESS') return 'กำลังผลิต';
        if (s === 'PENDING')     return 'รอเริ่ม';
        if (s === 'ACTIVE')      return 'หมดเวลา';
        if (s === 'INACTIVE')    return 'ปิดแล้ว';
        return status;
    };

    // Check if a report covers a specific day in this month
    const reportsForCell = (machineName, day) => {
        const cellDate = new Date(year, month, day);
        return reports.filter(r => {
            if ((r.machineName || '') !== machineName) return false;
            if (!r.startDate || !r.endDate) return false;
            const start = new Date(r.startDate);
            const end   = new Date(r.endDate);
            return start <= cellDate && cellDate <= end;
        });
    };

    const isToday = (day) => {
        return today.getFullYear() === year && today.getMonth() === month && today.getDate() === day;
    };

    const lang = typeof getLang === 'function' ? getLang() : 'th';

    return (
        <div style={{ padding: '1.5rem' }}>
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '1.5rem', gap: '1rem', flexWrap: 'wrap' }}>
                <button onClick={onBack} className="back-button">&larr; {lang === 'en' ? 'Back' : 'กลับ'}</button>
                <h2 style={{ margin: 0, flexGrow: 1 }}>
                    📅 {lang === 'en' ? 'Machine Production Schedule' : 'ตารางแผนการผลิตตามเครื่องจักร'}
                </h2>
                {/* Month navigator */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <button onClick={prevMonth} style={{ padding: '0.3rem 0.8rem', cursor: 'pointer', borderRadius: '4px', border: '1px solid #ccc' }}>◀</button>
                    <span style={{ fontWeight: 'bold', minWidth: '160px', textAlign: 'center', fontSize: '1rem' }}>
                        {lang === 'en' ? monthNamesEn[month] : monthNames[month]} {year}
                    </span>
                    <button onClick={nextMonth} style={{ padding: '0.3rem 0.8rem', cursor: 'pointer', borderRadius: '4px', border: '1px solid #ccc' }}>▶</button>
                </div>
            </div>

            {/* Legend */}
            <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap', fontSize: '0.8rem' }}>
                {[['IN_PROGRESS','กำลังผลิต'],['PENDING','รอเริ่ม'],['ACTIVE','หมดเวลา'],['INACTIVE','ปิดแล้ว']].map(([s,label]) => (
                    <span key={s} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ width: 14, height: 14, borderRadius: 3, backgroundColor: statusColor(s), display: 'inline-block' }}/>
                        {label}
                    </span>
                ))}
            </div>

            {/* Calendar table */}
            <div style={{ overflowX: 'auto' }}>
                <table style={{ borderCollapse: 'collapse', minWidth: '900px', width: '100%', fontSize: '0.78rem' }}>
                    <thead>
                        <tr style={{ backgroundColor: '#343a40', color: '#fff' }}>
                            <th style={{ padding: '8px 12px', textAlign: 'left', minWidth: '110px', position: 'sticky', left: 0, backgroundColor: '#343a40', zIndex: 2 }}>
                                {lang === 'en' ? 'Machine' : 'เครื่องจักร'}
                            </th>
                            {days.map(d => (
                                <th key={d} style={{
                                    padding: '6px 2px', textAlign: 'center', minWidth: '28px',
                                    backgroundColor: isToday(d) ? '#ffc107' : '#343a40',
                                    color: isToday(d) ? '#000' : '#fff',
                                    fontWeight: isToday(d) ? 'bold' : 'normal'
                                }}>{d}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {machineNames.map((machineName, rowIdx) => (
                            <tr key={machineName} style={{ backgroundColor: rowIdx % 2 === 0 ? '#f8f9fa' : '#fff' }}>
                                <td style={{
                                    padding: '6px 10px', fontWeight: 600, fontSize: '0.8rem',
                                    position: 'sticky', left: 0, backgroundColor: rowIdx % 2 === 0 ? '#f8f9fa' : '#fff',
                                    borderRight: '2px solid #dee2e6', zIndex: 1
                                }}>{machineName}</td>
                                {days.map(d => {
                                    const cellReports = reportsForCell(machineName, d);
                                    return (
                                        <td key={d} style={{
                                            padding: '2px', textAlign: 'center', verticalAlign: 'middle',
                                            border: '1px solid #dee2e6',
                                            backgroundColor: isToday(d) ? '#fff9e6' : 'transparent'
                                        }}>
                                            {cellReports.map(r => (
                                                <div key={r.id} title={`${r.orderNumber}\n${r.startDate} – ${r.endDate}\n${statusLabel(r.status)}`}
                                                     style={{
                                                         backgroundColor: statusColor(r.status),
                                                         color: '#fff', borderRadius: '3px',
                                                         padding: '1px 3px', fontSize: '0.65rem',
                                                         marginBottom: '1px', cursor: 'default',
                                                         whiteSpace: 'nowrap', overflow: 'hidden',
                                                         maxWidth: '100%', textOverflow: 'ellipsis'
                                                     }}>
                                                    {r.orderNumber}
                                                </div>
                                            ))}
                                        </td>
                                    );
                                })}
                            </tr>
                        ))}
                        {machineNames.length === 0 && (
                            <tr><td colSpan={daysInMonth + 1} style={{ textAlign: 'center', padding: '2rem', color: '#6c757d' }}>
                                {lang === 'en' ? 'No data' : 'ไม่มีข้อมูล'}
                            </td></tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Summary list for this month */}
            <div style={{ marginTop: '2rem' }}>
                <h4 style={{ marginBottom: '0.75rem' }}>
                    {lang === 'en' ? 'Production Orders This Month' : `รายการใบสั่งผลิตในเดือน ${monthNames[month]} ${year}`}
                </h4>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                    <thead>
                        <tr style={{ backgroundColor: '#e9ecef' }}>
                            {['เลขที่คำสั่ง','เครื่องจักร','ผลิตภัณฑ์','วันเริ่ม','วันสิ้นสุด','สถานะ'].map(h => (
                                <th key={h} style={{ padding: '8px 10px', textAlign: 'left', border: '1px solid #dee2e6' }}>{h}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {reports.filter(r => {
                            if (!r.startDate || !r.endDate) return false;
                            const start = new Date(r.startDate);
                            const end   = new Date(r.endDate);
                            const mStart = new Date(year, month, 1);
                            const mEnd   = new Date(year, month + 1, 0);
                            return start <= mEnd && end >= mStart;
                        }).sort((a, b) => new Date(a.startDate) - new Date(b.startDate))
                        .map(r => (
                            <tr key={r.id} style={{ borderBottom: '1px solid #dee2e6' }}>
                                <td style={{ padding: '6px 10px', fontWeight: 600 }}>{r.orderNumber}</td>
                                <td style={{ padding: '6px 10px' }}>{r.machineName}</td>
                                <td style={{ padding: '6px 10px' }}>{r.productName}</td>
                                <td style={{ padding: '6px 10px' }}>{r.startDate}</td>
                                <td style={{ padding: '6px 10px' }}>{r.endDate}</td>
                                <td style={{ padding: '6px 10px' }}>
                                    <span style={{ backgroundColor: statusColor(r.status), color: '#fff', padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem' }}>
                                        {statusLabel(r.status)}
                                    </span>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ProductionControlDashboard;