// =================================================================
// File: src/components/doc-mgmt/DocMgmtDashboard.jsx (Read-only version of PC Dashboard)
// =================================================================
import React, { useState, useEffect } from 'react';
import Chart from 'react-apexcharts';
import axios from 'axios';

// --- API Service ---
const API_URL = 'http://localhost:8080/api';
const api = axios.create({ baseURL: API_URL });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) { config.headers.Authorization = `Bearer ${token}`; }
    return config;
}, error => Promise.reject(error));

// --- Shared Components (Copied from PC Dashboard) ---
const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return ( <div className="modal-overlay"> <div className="modal-content"> <div className="modal-header"> <h3>{title}</h3> <button onClick={onClose} className="modal-close-button">&times;</button> </div> <div className="modal-body">{children}</div> </div> </div> );
};

const ShiftDataDisplay = ({ title, data }) => {
    const fmt = (val) => Number(val ?? 0).toLocaleString();
    return (
        <div className="shift-data-container" style={{background: '#fff', padding: '1.5rem', borderRadius: '0.75rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', marginBottom: '1.5rem'}}>
            <h3>{title}</h3>
            <div className="kpi-grid">
                <div className="kpi-card"><span className="kpi-label">ยอดผลิตดี (ชิ้น)</span><span className="kpi-value">{fmt(data?.goodProductionPieces)}</span></div>
                <div className="kpi-card"><span className="kpi-label">ยอดของเสีย (ชิ้น)</span><span className="kpi-value ng-value">{fmt(data?.ngProductionPieces)}</span></div>
                <div className="kpi-card"><span className="kpi-label">ยอดผลิตรวม (ชิ้น)</span><span className="kpi-value">{fmt(data?.totalProductionPieces)}</span></div>
                <div className="kpi-card"><span className="kpi-label">Yield</span><span className="kpi-value yield-value">{data?.yieldPercentage ?? '0.00%'}</span></div>
            </div>
        </div>
    );
};

const GaugeCard = ({ machineName, productName, target, current, ng }) => {
    const percent = target > 0 ? (current / target) * 100 : 0;
    const formatNumber = (num) => new Intl.NumberFormat('en-US').format(num || 0);
    const chartOptions = { chart: { type: 'radialBar', sparkline: { enabled: true } }, plotOptions: { radialBar: { startAngle: -90, endAngle: 90, hollow: { size: '75%' }, track: { background: "#e7e7e7", strokeWidth: '97%' }, dataLabels: { name: { show: false }, value: { offsetY: -2, fontSize: '22px', formatter: (val) => val.toFixed(1) + "%" } } } }, grid: { padding: { top: -10 } }, colors: ["#3b82f6"], labels: ['Progress'], };
    const chartSeries = [percent];
    return ( <div className="gauge-card"> <div className="gauge-chart-container"><Chart options={chartOptions} series={chartSeries} type="radialBar" height="140" /></div> <div className="gauge-info"> <h3 className="gauge-machine-name">{machineName}</h3><p className="gauge-product-name">{productName}</p> <div className="gauge-details"> <div className="gauge-detail-item"><span>ยอดผลิตดี</span><strong>{formatNumber(current)}</strong></div> <div className="gauge-detail-item"><span>เป้าหมาย</span><strong>{formatNumber(target)}</strong></div> <div className="gauge-detail-item ng"><span>ของเสีย</span><strong>{formatNumber(ng)}</strong></div> </div> </div> </div> );
};

const ReportDetailView = ({ reportId, onBack }) => {
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    useEffect(() => {
        const fetchSummary = async () => { if (!reportId) return; setLoading(true); try { const response = await api.get(`/pc/reports/${reportId}/summary`); setSummary(response.data); } catch (err) { setError('ไม่สามารถดึงข้อมูลสรุปได้'); } finally { setLoading(false); } };
        fetchSummary();
    }, [reportId]);

    if (loading) return <div className="loading-container"><h2>กำลังโหลดรายละเอียด...</h2></div>;
    if (error) return <div className="dashboard-card"><p className="error-message">{error}</p><button onClick={onBack} className="back-button">&larr; กลับ</button></div>;
    if (!summary) return null;
    
    const ngGrouped = summary.ngLogs?.reduce((acc, log) => { const key = log.ngDescription; if (!acc[key]) { acc[key] = 0; } acc[key] += log.quantity; return acc; }, {});
    const ngChartOptions = { chart: { type: 'pie' }, labels: ngGrouped ? Object.keys(ngGrouped) : [], responsive: [{ breakpoint: 480, options: { chart: { width: 200 }, legend: { position: 'bottom' } } }] };
    const ngChartSeries = ngGrouped ? Object.values(ngGrouped) : [];

    const fmt = (val) => Number(val ?? 0).toLocaleString();
    const fmtDate = (ts) => ts ? new Date(ts).toLocaleString() : '-';
    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; กลับไปหน้าก่อนหน้า</button>
            <div className="sl-header">
                <div>
                    <h2 className="sl-title">สรุปผลการผลิต: {summary.machineName}</h2>
                    <p className="sl-subtitle">ผลิตภัณฑ์: {summary.productName} | เป้าหมาย: {summary.targetQty?.toLocaleString() || 'N/A'} ชิ้น</p>
                </div>
            </div>
            <div className="kpi-grid pc-kpi">
                <div className="kpi-card"><span className="kpi-label">ยอดผลิตดี (ชิ้น)</span><span className="kpi-value">{fmt(summary?.goodQty)}</span></div>
                <div className="kpi-card"><span className="kpi-label">ยอดของเสีย (ชิ้น)</span><span className="kpi-value ng-value">{fmt(summary?.totalNgQty)}</span></div>
                <div className="kpi-card"><span className="kpi-label">Yield</span><span className="kpi-value yield-value">{summary?.yield ?? '-'}</span></div>
            </div>
            <div className="summary-section-grid">
                <div className="summary-panel">
                    <h3 className="summary-title">สรุปประเภทของเสีย</h3>
                    {summary.ngLogs?.length > 0 ? (<Chart options={ngChartOptions} series={ngChartSeries} type="pie" width="380" />) : <p>ไม่มีข้อมูลของเสีย</p>}
                </div>
                <div className="summary-panel">
                    <h3 className="summary-title">ประวัติ Downtime</h3>
                    <div className="data-table-container">
                        <table className="data-table">
                            <thead><tr><th>เวลาเริ่ม</th><th>เวลาสิ้นสุด</th><th>ระยะเวลา</th><th>สาเหตุ</th><th>ผู้บันทึก</th></tr></thead>
                            <tbody>{summary.downtimeEvents?.length > 0 ? summary.downtimeEvents.map((evt, i) => (<tr key={i}><td>{evt.startTime}</td><td>{evt.endTime}</td><td>{evt.duration}</td><td>{evt.reason}</td><td>{evt.technicianName}</td></tr>)) : (<tr><td colSpan="5">ไม่มีข้อมูล Downtime</td></tr>)}</tbody>
                        </table>
                    </div>
                </div>
            </div>
            <div className="summary-panel" style={{ gridColumn: '1 / -1', marginTop: '1.5rem' }}>
                <h3 className="summary-title">ประวัติการใช้วัตถุดิบ</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>เวลา</th><th>รหัสวัตถุดิบ</th><th>Lot Number</th><th>จำนวน (Kg.)</th><th>ผู้บันทึก</th></tr></thead>
                        <tbody>{summary.materialUsageLogs?.length > 0 ? summary.materialUsageLogs.map((log, i) => (<tr key={i}><td>{fmtDate(log.timestamp)}</td><td>{log.materialCode}</td><td>{log.lotNumber}</td><td>{log.quantityKg}</td><td>{log.technicianName}</td></tr>)) : (<tr><td colSpan="5">ไม่มีข้อมูลการใช้วัตถุดิบ</td></tr>)}</tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

const DailyReportView = ({ date, onBack }) => {
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchDailySummary = async () => {
            if (!date) return;
            setLoading(true);
            try {
                const response = await api.get(`/pc/reports/summary/daily?date=${date}`);
                setSummary(response.data);
            } catch (err) {
                setError('ไม่สามารถดึงข้อมูลสรุปรายวันได้');
            } finally {
                setLoading(false);
            }
        };
        fetchDailySummary();
    }, [date]);

    if (loading) return <div className="loading-container"><h2>กำลังโหลดสรุปรายวัน...</h2></div>;
    if (error) return <div className="dashboard-card"><p className="error-message">{error}</p><button onClick={onBack} className="back-button">&larr; กลับ</button></div>;
    if (!summary) return <div className="dashboard-card"><p>ไม่มีข้อมูลสำหรับวันที่ {date}</p><button onClick={onBack} className="back-button">&larr; กลับ</button></div>;

    const fmt = (val) => Number(val ?? 0).toLocaleString();
    const fmtDate = (ts) => ts ? new Date(ts).toLocaleString() : '-';
    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; กลับไปหน้ารายงานย้อนหลัง</button>
            <h2 className="dashboard-title">สรุปรายงานประจำวันที่: {date}</h2>
            <div className="kpi-grid pc-kpi">
                <div className="kpi-card"><span className="kpi-label">ยอดผลิตดีทั้งหมด</span><span className="kpi-value">{fmt(summary?.totalGoodQty)}</span></div>
                <div className="kpi-card"><span className="kpi-label">ยอดของเสียทั้งหมด</span><span className="kpi-value ng-value">{fmt(summary?.totalNgQty)}</span></div>
            </div>
            <div className="summary-panel" style={{ gridColumn: '1 / -1', marginTop: '1.5rem' }}>
                <h3 className="summary-title">ประวัติการใช้วัตถุดิบ</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>เวลา</th><th>รหัสวัตถุดิบ</th><th>Lot Number</th><th>จำนวน (Kg.)</th><th>ผู้บันทึก</th></tr></thead>
                        <tbody>{summary.materialUsageLogs?.length > 0 ? summary.materialUsageLogs.map((log, i) => (<tr key={i}><td>{fmtDate(log.timestamp)}</td><td>{log.materialCode}</td><td>{log.lotNumber}</td><td>{log.quantityKg}</td><td>{log.technicianName}</td></tr>)) : (<tr><td colSpan="5">ไม่มีข้อมูล</td></tr>)}</tbody>
                    </table>
                </div>
            </div>
            <div className="summary-panel" style={{ gridColumn: '1 / -1', marginTop: '1.5rem' }}>
                <h3 className="summary-title">ประวัติ Downtime</h3>
                <div className="data-table-container">
                    <table className="data-table">
                        <thead><tr><th>เวลาเริ่ม</th><th>เวลาสิ้นสุด</th><th>สาเหตุ</th><th>ผู้บันทึก</th></tr></thead>
                        <tbody>{summary.downtimeEvents?.length > 0 ? summary.downtimeEvents.map((evt, i) => (<tr key={i}><td>{evt.startTime}</td><td>{evt.endTime}</td><td>{evt.reason}</td><td>{evt.technicianName}</td></tr>)) : (<tr><td colSpan="4">ไม่มีข้อมูล</td></tr>)}</tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

const DailyShiftReportView = ({ date, onBack }) => {
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchDailyShiftSummary = async () => {
            if (!date) return;
            setLoading(true);
            setError('');
            try {
                const response = await api.get(`/pc/reports/summary/daily-by-shift?date=${date}`);
                setSummary(response.data);
            } catch (err) {
                setError('ไม่สามารถดึงข้อมูลสรุปรายวันแบบแยกกะได้ หรือไม่มีข้อมูลสำหรับวันที่เลือก');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchDailyShiftSummary();
    }, [date]);

    if (loading) return <div className="loading-container"><h2>กำลังโหลดสรุปรายวัน (แยกกะ)...</h2></div>;
    if (error) return <div className="dashboard-card"><p className="error-message">{error}</p><button onClick={onBack} className="back-button">&larr; กลับ</button></div>;
    if (!summary) return <div className="dashboard-card"><p>ไม่มีข้อมูลสำหรับวันที่ {date}</p><button onClick={onBack} className="back-button">&larr; กลับ</button></div>;

    return (
        <div className="dashboard-card-sl">
            <div className="sl-header">
                <div>
                    <h2 className="sl-title">รายละเอียดสรุปรายวัน (แยกกะ)</h2>
                    <p className="sl-subtitle">วันที่ผลิต: {summary.productionDate}</p>
                </div>
                <div>
                    <button onClick={onBack} className="back-button-sl">กลับไปหน้ารายงาน</button>
                </div>
            </div>
            <ShiftDataDisplay title="กะกลางวัน (03:00 - 15:00)" data={summary.dayShiftData} />
            <ShiftDataDisplay title="กะกลางคืน (15:00 - 03:00)" data={summary.nightShiftData} />
        </div>
    );
};

const HistoricalReports = ({ onBack, onViewDetail, onViewDailyReport, onViewDailyShiftReport, machines, products }) => {
    const [filters, setFilters] = useState({ startDate: new Date().toISOString().split('T')[0], endDate: new Date().toISOString().split('T')[0], machineId: 'all', productId: 'all' });
    const [results, setResults] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [isDateModalOpen, setIsDateModalOpen] = useState(false);
    const [modalData, setModalData] = useState({ viewType: '', availableDates: [] });
    const [selectedDate, setSelectedDate] = useState('');

    const handleFilterChange = (e) => { const { name, value } = e.target; setFilters(prev => ({ ...prev, [name]: value })); };

    const handleSearch = async () => {
        setLoading(true); setError(''); setResults(null);
        try {
            const params = new URLSearchParams({ startDate: filters.startDate, endDate: filters.endDate });
            if (filters.machineId !== 'all') { params.append('machineId', filters.machineId); }
            if (filters.productId !== 'all') { params.append('productId', filters.productId); }
            const response = await api.get(`/pc/reports/historical?${params.toString()}`);
            setResults(response.data);
        } catch (err) { 
            setError('ไม่สามารถดึงข้อมูลรายงานได้ หรือไม่มีข้อมูลในช่วงที่เลือก'); 
        } finally { 
            setLoading(false); 
        }
    };

    const getDatesInRange = (startDate, endDate) => {
        const dates = [];
        let currentDate = new Date(startDate + 'T00:00:00');
        const end = new Date(endDate + 'T00:00:00');
        while (currentDate <= end) {
            dates.push(currentDate.toISOString().split('T')[0]);
            currentDate.setDate(currentDate.getDate() + 1);
        }
        return dates;
    };

    const handleOpenDateModal = (report, viewType) => {
        const availableDates = getDatesInRange(report.startDate, report.endDate);
        setModalData({
            viewType: viewType,
            availableDates: availableDates
        });
        setSelectedDate(availableDates[0] || '');
        setIsDateModalOpen(true);
    };

    const handleConfirmDateSelection = () => {
        if (!selectedDate) {
            alert("กรุณาเลือกวันที่");
            return;
        }
        if (modalData.viewType === 'dailyDetail') {
            onViewDailyReport(selectedDate);
        } else if (modalData.viewType === 'dailyShiftDetail') {
            onViewDailyShiftReport(selectedDate);
        }
        setIsDateModalOpen(false);
    };

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; กลับไปหน้าภาพรวม</button>
            <h2 className="dashboard-title">รายงานย้อนหลัง</h2>

            <fieldset className="form-fieldset">
                <legend>ค้นหาคำสั่งผลิตย้อนหลัง</legend>
                <div className="filter-panel" style={{display: 'flex', gap: '1rem', alignItems: 'flex-end', marginBottom: '1.5rem'}}>
                    <div className="form-group"><label className="form-label">วันที่เริ่มต้น</label><input type="date" name="startDate" value={filters.startDate} onChange={handleFilterChange} className="form-input" /></div>
                    <div className="form-group"><label className="form-label">วันที่สิ้นสุด</label><input type="date" name="endDate" value={filters.endDate} onChange={handleFilterChange} className="form-input" /></div>
                    <div className="form-group"><label className="form-label">เครื่องจักร</label><select name="machineId" value={filters.machineId} onChange={handleFilterChange} className="form-input"><option value="all">ทุกเครื่องจักร</option>{machines.map(m => <option key={m.id} value={m.id}>{m.machineName}</option>)}</select></div>
                    <div className="form-group"><label className="form-label">ผลิตภัณฑ์</label><select name="productId" value={filters.productId} onChange={handleFilterChange} className="form-input"><option value="all">ทุกผลิตภัณฑ์</option>{products.map(p => <option key={p.id} value={p.id}>{p.productName}</option>)}</select></div>
                    <button onClick={handleSearch} disabled={loading} className="submit-button" style={{height: '42px'}}>{loading ? 'กำลังค้นหา...' : 'ค้นหา'}</button>
                </div>
            </fieldset>

            {error && <p className="error-message">{error}</p>}
            
            {results && (
                <div className="results-section">
                    <div className="kpi-grid pc-kpi">
                        <div className="kpi-card"><span className="kpi-label">ยอดผลิตดีทั้งหมด</span><span className="kpi-value">{Number(results?.totalGoodQty ?? 0).toLocaleString()}</span></div>
                        <div className="kpi-card"><span className="kpi-label">ยอดของเสียทั้งหมด</span><span className="kpi-value ng-value">{Number(results?.totalNgQty ?? 0).toLocaleString()}</span></div>
                        <div className="kpi-card"><span className="kpi-label">Yield เฉลี่ย</span><span className="kpi-value yield-value">{results?.averageYield ?? '-'}</span></div>
                    </div>
                    <div className="data-table-container" style={{marginTop: '2rem'}}>
                         <table className="data-table">
                            <thead><tr><th>Order No.</th><th>วันที่</th><th>เครื่องจักร</th><th>ผลิตภัณฑ์</th><th>สถานะ</th><th>Actions</th></tr></thead>
                            <tbody>{results.reports.map(report => (
                                <tr key={report.id}>
                                    <td>{report.orderNumber}</td>
                                    <td>{report.startDate} - {report.endDate}</td>
                                    <td>{report.machineName}</td>
                                    <td>{report.productName}</td>
                                    <td><span className={`status-${report.status.toLowerCase().replace(' ', '-')}`}>{report.status}</span></td>
                                    <td className="actions-cell" style={{flexDirection: 'column', alignItems: 'stretch', gap: '0.25rem'}}>
                                        <button className="add-button" onClick={() => onViewDetail(report.id)}>ภาพรวมคำสั่งผลิต</button>
                                        <button className="edit-button" onClick={() => handleOpenDateModal(report, 'dailyDetail')}>สรุปรายวัน</button>
                                        <button className="finalize-button" onClick={() => handleOpenDateModal(report, 'dailyShiftDetail')}>สรุปรายวัน (แยกกะ)</button>
                                    </td>
                                </tr>
                            ))}</tbody>
                        </table>
                    </div>
                </div>
            )}

            <Modal isOpen={isDateModalOpen} onClose={() => setIsDateModalOpen(false)} title="เลือกวันที่สำหรับรายงาน">
                <div className="form-group">
                    <label htmlFor="date-select" className="form-label">วันที่</label>
                    <select 
                        id="date-select"
                        className="form-input" 
                        value={selectedDate} 
                        onChange={(e) => setSelectedDate(e.target.value)}
                    >
                        {modalData.availableDates.map(date => (
                            <option key={date} value={date}>{date}</option>
                        ))}
                    </select>
                </div>
                <div className="form-actions">
                    <button type="button" onClick={() => setIsDateModalOpen(false)} className="cancel-button">ยกเลิก</button>
                    <button type="button" onClick={handleConfirmDateSelection} className="save-button">ดูรายงาน</button>
                </div>
            </Modal>
        </div>
    );
};

// --- Main Read-Only Dashboard Component ---
const DocMgmtDashboard = () => {
    const [view, setView] = useState('dashboard');
    const [previousView, setPreviousView] = useState('dashboard');
    const [selectedReportId, setSelectedReportId] = useState(null);
    const [selectedDate, setSelectedDate] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [machines, setMachines] = useState([]);
    const [products, setProducts] = useState([]);
    const [dashboardData, setDashboardData] = useState([]);

    const fetchData = async () => {
        if (!loading) setLoading(true);
        try {
            const [machinesRes, productsRes, dashboardRes] = await Promise.all([
                api.get('/pc/machines'), 
                api.get('/pc/products'), 
                api.get('/pc/dashboard-summary')
            ]);
            setMachines(machinesRes.data);
            setProducts(productsRes.data);
            setDashboardData(dashboardRes.data);
        } catch (err) {
            setError('เกิดข้อผิดพลาดในการดึงข้อมูล'); 
            console.error(err);
        } finally {
            setLoading(false);
        }
    };
    
    useEffect(() => {
        let intervalId = null;
        if (view === 'dashboard') {
            fetchData();
            intervalId = setInterval(() => { 
                api.get('/pc/dashboard-summary').then(res => setDashboardData(res.data)).catch(console.error); 
            }, 30000);
        } else {
            fetchData(); // Fetch data once for other views
        }
        return () => { if (intervalId) { clearInterval(intervalId); } };
    }, [view]);

    const changeView = (newView, data = null) => {
        setPreviousView(view);
        if (newView === 'detail') {
            setSelectedReportId(data);
        }
        if (newView === 'dailyDetail' || newView === 'dailyShiftDetail') {
            setSelectedDate(data);
        }
        setView(newView);
    };
    
    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูล...</h2></div>;

    // --- View Router ---
    if (view === 'detail') {
        return <ReportDetailView reportId={selectedReportId} onBack={() => changeView(previousView)} />;
    }
    if (view === 'history') {
        return <HistoricalReports 
                    onBack={() => changeView('dashboard')} 
                    onViewDetail={(id) => changeView('detail', id)} 
                    onViewDailyReport={(date) => changeView('dailyDetail', date)} 
                    onViewDailyShiftReport={(date) => changeView('dailyShiftDetail', date)}
                    machines={machines} 
                    products={products} 
                />;
    }
    if (view === 'dailyDetail') {
        return <DailyReportView date={selectedDate} onBack={() => changeView('history')} />;
    }
    if (view === 'dailyShiftDetail') {
        return <DailyShiftReportView date={selectedDate} onBack={() => changeView('history')} />;
    }

    // Default view: 'dashboard'
    return (
        <div className="pc-dashboard-container">
            <div className="pc-dashboard-header">
                <h2 className="pc-dashboard-title">ภาพรวมการผลิต (Production Overview)</h2>
                <div>
                    <button className="manage-reports-button" onClick={() => changeView('history')}>ดูรายงานย้อนหลัง</button>
                    {/* "Manage Production Orders" button is removed for this role */}
                </div>
            </div>
            {error && <p className="error-message">{error}</p>}
            {!error && dashboardData.length === 0 &&
                <div className="no-data-card">
                    <h3>ไม่มีเครื่องจักรที่กำลังทำงานอยู่</h3>
                </div>
            }
            <div className="gauge-grid">
                {dashboardData.map(data => (
                    <div key={data.reportId} onClick={() => changeView('detail', data.reportId)} style={{cursor: 'pointer'}}>
                        <GaugeCard machineName={data.machineName} productName={data.productName} target={data.targetQty} current={data.currentGoodQty} ng={data.currentNgQty}/>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default DocMgmtDashboard;