// =================================================================
// File: src/components/pc/ProductionControlDashboard.jsx (ฉบับสมบูรณ์ Final - เปิดใช้งานปุ่มสรุปรายวัน)
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

// --- Shared Components ---
const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return ( <div className="modal-overlay"> <div className="modal-content"> <div className="modal-header"> <h3>{title}</h3> <button onClick={onClose} className="modal-close-button">&times;</button> </div> <div className="modal-body">{children}</div> </div> </div> );
};

const ShiftDataDisplay = ({ title, data }) => ( <div className="shift-data-container" style={{background: '#fff', padding: '1.5rem', borderRadius: '0.75rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', marginBottom: '1.5rem'}}> <h3>{title}</h3> <div className="kpi-grid"><div className="kpi-card"><span className="kpi-label">ยอดผลิตดี (กล่อง)</span><span className="kpi-value">{data.goodProductionBoxes}</span></div><div className="kpi-card"><span className="kpi-label">ยอดของเสีย (ชิ้น)</span><span className="kpi-value ng-value">{data.ngProductionPieces}</span></div><div className="kpi-card"><span className="kpi-label">ยอดผลิตรวม (ชิ้น)</span><span className="kpi-value">{data.totalProductionPieces}</span></div><div className="kpi-card"><span className="kpi-label">Yield</span><span className="kpi-value yield-value">{data.yieldPercentage}</span></div></div> <div className="summary-section-grid"><div className="summary-panel"><h3 className="summary-title">สรุปยอดของเสีย</h3><ul className="ng-summary-list">{data.ngSummary.length > 0 ? data.ngSummary.map(ng => (<li key={ng.ngDescription}><span>{ng.ngDescription}</span><span>{ng.count} ชิ้น</span></li>)) : <p>ไม่มีข้อมูล</p>}</ul></div><div className="summary-panel"><h3 className="summary-title">ประวัติ Downtime</h3><div className="data-table-container"><table className="data-table"><thead><tr><th>เวลาเริ่ม</th><th>เวลาสิ้นสุด</th><th>ระยะเวลา</th><th>สาเหตุ</th><th>ผู้บันทึก</th></tr></thead><tbody>{data.downtimeHistory.length > 0 ? data.downtimeHistory.map((event, index) => (<tr key={index}><td>{event.startTime}</td><td>{event.endTime}</td><td>{event.duration}</td><td>{event.reason}</td><td>{event.technicianName}</td></tr>)) : <tr><td colSpan="5">ไม่มีข้อมูล</td></tr>}</tbody></table></div></div></div> </div> );

// --- Sub-Components (Pages) ---

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

    return ( <div className="dashboard-card"> <button onClick={onBack} className="back-button">&larr; กลับไปหน้าก่อนหน้า</button> <div className="sl-header"> <div> <h2 className="sl-title">สรุปผลการผลิต: {summary.machineName}</h2> <p className="sl-subtitle">ผลิตภัณฑ์: {summary.productName} | เป้าหมาย: {summary.targetQty?.toLocaleString() || 'N/A'} ชิ้น</p> </div> </div> <div className="kpi-grid pc-kpi"> <div className="kpi-card"><span className="kpi-label">ยอดผลิตดี (ชิ้น)</span><span className="kpi-value">{summary.goodQty.toLocaleString()}</span></div> <div className="kpi-card"><span className="kpi-label">ยอดของเสีย (ชิ้น)</span><span className="kpi-value ng-value">{summary.totalNgQty.toLocaleString()}</span></div> <div className="kpi-card"><span className="kpi-label">Yield</span><span className="kpi-value yield-value">{summary.yield}</span></div> </div> <div className="summary-section-grid"> <div className="summary-panel"> <h3 className="summary-title">สรุปประเภทของเสีย</h3> {summary.ngLogs?.length > 0 ? (<Chart options={ngChartOptions} series={ngChartSeries} type="pie" width="380" />) : <p>ไม่มีข้อมูลของเสีย</p>} </div> <div className="summary-panel"> <h3 className="summary-title">ประวัติ Downtime</h3> <div className="data-table-container"> <table className="data-table"> <thead><tr><th>เวลาเริ่ม</th><th>เวลาสิ้นสุด</th><th>ระยะเวลา</th><th>สาเหตุ</th><th>ผู้บันทึก</th></tr></thead> <tbody> {summary.downtimeEvents?.length > 0 ? summary.downtimeEvents.map((evt, i) => ( <tr key={i}><td>{evt.startTime}</td><td>{evt.endTime}</td><td>{evt.duration}</td><td>{evt.reason}</td><td>{evt.technicianName}</td></tr> )) : <tr><td colSpan="5">ไม่มีข้อมูล Downtime</td></tr>} </tbody> </table> </div> </div> </div> <div className="summary-panel" style={{ gridColumn: '1 / -1', marginTop: '1.5rem' }}> <h3 className="summary-title">ประวัติการใช้วัตถุดิบ</h3> <div className="data-table-container"> <table className="data-table"> <thead> <tr> <th>เวลา</th> <th>รหัสวัตถุดิบ</th> <th>Lot Number</th> <th>จำนวน (Kg.)</th> <th>ผู้บันทึก</th> </tr> </thead> <tbody> {summary.materialUsageLogs?.length > 0 ? summary.materialUsageLogs.map((log, i) => ( <tr key={i}> <td>{new Date(log.timestamp).toLocaleString()}</td> <td>{log.materialCode}</td> <td>{log.lotNumber}</td> <td>{log.quantityKg}</td> <td>{log.technicianName}</td> </tr> )) : <tr><td colSpan="5">ไม่มีข้อมูลการใช้วัตถุดิบ</td></tr>} </tbody> </table> </div> </div> </div> );
};

// **[ใหม่]** สร้าง Component สำหรับแสดงหน้ารายงานสรุปรายวัน
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
    if (!summary) return null;

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; กลับไปหน้ารายงานย้อนหลัง</button>
            <h2 className="dashboard-title">สรุปรายงานประจำวันที่: {date}</h2>
            <div className="kpi-grid pc-kpi">
                <div className="kpi-card"><span className="kpi-label">ยอดผลิตดีทั้งหมด</span><span className="kpi-value">{summary.totalGoodQty.toLocaleString()}</span></div>
                <div className="kpi-card"><span className="kpi-label">ยอดของเสียทั้งหมด</span><span className="kpi-value ng-value">{summary.totalNgQty.toLocaleString()}</span></div>
            </div>
            <div className="summary-panel" style={{ gridColumn: '1 / -1', marginTop: '1.5rem' }}> <h3 className="summary-title">ประวัติการใช้วัตถุดิบ</h3> <div className="data-table-container"> <table className="data-table"> <thead> <tr> <th>เวลา</th> <th>รหัสวัตถุดิบ</th> <th>Lot Number</th> <th>จำนวน (Kg.)</th> <th>ผู้บันทึก</th> </tr> </thead> <tbody> {summary.materialUsageLogs?.length > 0 ? summary.materialUsageLogs.map((log, i) => ( <tr key={i}> <td>{new Date(log.timestamp).toLocaleString()}</td> <td>{log.materialCode}</td> <td>{log.lotNumber}</td> <td>{log.quantityKg}</td> <td>{log.technicianName}</td> </tr> )) : <tr><td colSpan="5">ไม่มีข้อมูล</td></tr>} </tbody> </table> </div> </div>
            <div className="summary-panel" style={{ gridColumn: '1 / -1', marginTop: '1.5rem' }}> <h3 className="summary-title">ประวัติ Downtime</h3> <div className="data-table-container"> <table className="data-table"> <thead><tr><th>เวลาเริ่ม</th><th>เวลาสิ้นสุด</th><th>สาเหตุ</th><th>ผู้บันทึก</th></tr></thead> <tbody> {summary.downtimeEvents?.length > 0 ? summary.downtimeEvents.map((evt, i) => ( <tr key={i}><td>{evt.startTime}</td><td>{evt.endTime}</td><td>{evt.reason}</td><td>{evt.technicianName}</td></tr> )) : <tr><td colSpan="4">ไม่มีข้อมูล</td></tr>} </tbody> </table> </div> </div>
        </div>
    );
};


const HistoricalReports = ({ onBack, onViewDetail, onViewHistoryDetail, onViewDailyReport, machines, products }) => {
    const [filters, setFilters] = useState({ startDate: new Date().toISOString().split('T')[0], endDate: new Date().toISOString().split('T')[0], machineId: 'all', productId: 'all' });
    const [results, setResults] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleFilterChange = (e) => { const { name, value } = e.target; setFilters(prev => ({ ...prev, [name]: value })); };
    const handleSearch = async () => {
        setLoading(true); setError(''); setResults(null);
        try {
            const params = new URLSearchParams({ startDate: filters.startDate, endDate: filters.endDate });
            if (filters.machineId !== 'all') { params.append('machineId', filters.machineId); }
            if (filters.productId !== 'all') { params.append('productId', filters.productId); }
            const response = await api.get(`/pc/reports/historical?${params.toString()}`);
            setResults(response.data);
        } catch (err) { setError('ไม่สามารถดึงข้อมูลรายงานได้ หรือไม่มีข้อมูลในช่วงที่เลือก'); } 
        finally { setLoading(false); }
    };

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; กลับไปหน้าภาพรวม</button>
            <h2 className="dashboard-title">รายงานย้อนหลัง</h2>
            <div className="filter-panel" style={{display: 'flex', gap: '1rem', alignItems: 'flex-end', marginBottom: '1.5rem'}}>
                <div className="form-group"><label className="form-label">วันที่เริ่มต้น</label><input type="date" name="startDate" value={filters.startDate} onChange={handleFilterChange} className="form-input" /></div>
                <div className="form-group"><label className="form-label">วันที่สิ้นสุด</label><input type="date" name="endDate" value={filters.endDate} onChange={handleFilterChange} className="form-input" /></div>
                <div className="form-group"><label className="form-label">เครื่องจักร</label><select name="machineId" value={filters.machineId} onChange={handleFilterChange} className="form-input"><option value="all">ทุกเครื่องจักร</option>{machines.map(m => <option key={m.id} value={m.id}>{m.machineName}</option>)}</select></div>
                <div className="form-group"><label className="form-label">ผลิตภัณฑ์</label><select name="productId" value={filters.productId} onChange={handleFilterChange} className="form-input"><option value="all">ทุกผลิตภัณฑ์</option>{products.map(p => <option key={p.id} value={p.id}>{p.productName}</option>)}</select></div>
                <button onClick={handleSearch} disabled={loading} className="submit-button" style={{height: '42px'}}>{loading ? 'กำลังค้นหา...' : 'ค้นหา'}</button>
            </div>
            {error && <p className="error-message">{error}</p>}
            {results && (
                <div className="results-section">
                    <div className="kpi-grid pc-kpi">
                        <div className="kpi-card"><span className="kpi-label">ยอดผลิตดีทั้งหมด</span><span className="kpi-value">{results.totalGoodQty.toLocaleString()}</span></div>
                        <div className="kpi-card"><span className="kpi-label">ยอดของเสียทั้งหมด</span><span className="kpi-value ng-value">{results.totalNgQty.toLocaleString()}</span></div>
                        <div className="kpi-card"><span className="kpi-label">Yield เฉลี่ย</span><span className="kpi-value yield-value">{results.averageYield}</span></div>
                    </div>
                    <div className="data-table-container" style={{marginTop: '2rem'}}>
                         <table className="data-table">
                            <thead><tr><th>Order No.</th><th>วันที่</th><th>เครื่องจักร</th><th>ผลิตภัณฑ์</th><th>สถานะ</th><th>Actions</th></tr></thead>
                            <tbody>
                                {results.reports.map(report => (<tr key={report.id}><td>{report.orderNumber}</td><td>{report.startDate} - {report.endDate}</td><td>{report.machineName}</td><td>{report.productName}</td><td><span className={`status-${report.status.toLowerCase().replace(' ', '-')}`}>{report.status}</span></td><td className="actions-cell" style={{flexDirection: 'column', alignItems: 'stretch', gap: '0.25rem'}}>
                                    <button className="add-button" onClick={() => onViewDetail(report.id)}>1. ภาพรวมคำสั่งผลิต</button>
                                    <button className="edit-button" onClick={() => onViewDailyReport(report.startDate)}>2. สรุปรายวัน</button>
                                    <button className="finalize-button" onClick={() => onViewHistoryDetail(report.id)}>3. สรุปรายวัน (แยกกะ)</button>
                                </td></tr>))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
};

// ... (โค้ด ProductionOrderManagement, ShiftBasedReportDetailView เหมือนเดิม) ...
const ShiftBasedReportDetailView = ({ reportId, onBack }) => {
    const [dashboardData, setDashboardData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    useEffect(() => {
        const fetchDashboardData = async () => { setLoading(true); try { const response = await api.get(`/shift-leader/dashboard/${reportId}`); setDashboardData(response.data); } catch (err) { setError('ไม่สามารถดึงข้อมูล Dashboard ได้'); setDashboardData(null); } finally { setLoading(false); } };
        fetchDashboardData();
    }, [reportId]);

    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูล...</h2></div>;
    if (error) return <div className="dashboard-card"><p className="error-message">{error}</p><button onClick={onBack} className="back-button">&larr; กลับ</button></div>;
    if (!dashboardData) return null;

    return ( <div className="dashboard-card-sl"> <div className="sl-header"><div><h2 className="sl-title">รายละเอียด: {dashboardData.machineName}</h2><p className="sl-subtitle">ผลิตภัณฑ์: {dashboardData.productName} | วันที่ผลิต: {dashboardData.productionDate}</p></div><div><button onClick={onBack} className="back-button-sl">กลับไปหน้ารายงาน</button></div></div> <ShiftDataDisplay title="กะกลางวัน (03:00 - 15:00)" data={dashboardData.dayShiftData} /> <ShiftDataDisplay title="กะกลางคืน (15:00 - 03:00)" data={dashboardData.nightShiftData} /> </div> );
};

const ProductionOrderManagement = ({ onBack, onViewDetail, machines, products, allReports, fetchData }) => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingReport, setEditingReport] = useState(null);
    const [filteredReports, setFilteredReports] = useState([]);
    const [statusFilter, setStatusFilter] = useState('all');
    const initialFormData = {
        orderNumber: '', startDate: new Date().toISOString().split('T')[0], endDate: new Date().toISOString().split('T')[0],
        machineId: '', productId: '', targetQty: ''
    };
    const [formData, setFormData] = useState(initialFormData);

    useEffect(() => {
        let reports = [...allReports];
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

    const handleOpenCreateModal = () => { setEditingReport(null); setFormData(initialFormData); setIsModalOpen(true); };
    const handleOpenEditModal = (report) => {
        setEditingReport(report);
        const machine = machines.find(m => m.machineName === report.machineName);
        const product = products.find(p => p.productName === report.productName);
        setFormData({ orderNumber: report.orderNumber || '', startDate: report.startDate, endDate: report.endDate, machineId: machine ? machine.id : '', productId: product ? product.id : '', targetQty: report.targetQty || '' });
        setIsModalOpen(true);
    };
    const handleCloseModal = () => { setIsModalOpen(false); setEditingReport(null); };
    const handleFinalize = async (reportId) => { if (window.confirm('คุณต้องการปิดงานใบสั่งผลิตนี้ใช่หรือไม่?')) { try { await api.post(`/pc/reports/${reportId}/finalize`); alert('ปิดงานสำเร็จ!'); fetchData(); } catch (err) { alert('เกิดข้อผิดพลาดในการปิดงาน'); } } };
    const handleDelete = async (reportId) => { if (window.confirm('คุณแน่ใจหรือไม่ว่าต้องการลบใบสั่งผลิตนี้?')) { try { await api.delete(`/pc/reports/${reportId}`); alert('ลบใบสั่งผลิตสำเร็จ!'); fetchData(); } catch (err) { alert(err.response?.data || 'เกิดข้อผิดพลาดในการลบข้อมูล'); } } };

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; กลับไปหน้าภาพรวม</button>
            <h2 className="dashboard-title">จัดการคำสั่งผลิต</h2>
            <div className="table-header">
                <h3>รายการใบสั่งผลิตทั้งหมด</h3>
                <div> <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="form-input" style={{marginRight: '1rem', display: 'inline-block', width: 'auto'}}> <option value="all">แสดงทุกสถานะ</option> <option value="In Progress">กำลังดำเนินการ</option> <option value="Finalized">ปิดงานแล้ว</option> </select> <button className="add-button" onClick={handleOpenCreateModal}>สร้างใบสั่งผลิตใหม่</button> </div>
            </div>
            <div className="data-table-container">
                <table className="data-table">
                   <thead><tr><th>Order No.</th><th>วันที่</th><th>เครื่องจักร</th><th>ผลิตภัณฑ์</th><th>สถานะ</th><th>Actions</th></tr></thead>
                   <tbody>
                        {filteredReports.map(report => (
                           <tr key={report.id}>
                               <td>{report.orderNumber}</td><td>{report.startDate} - {report.endDate}</td><td>{report.machineName}</td>
                               <td>{report.productName}</td>
                               <td><span className={`status-${report.status.toLowerCase().replace(' ', '-')}`}>{report.status}</span></td>
                               <td className="actions-cell">
                                    <button className="add-button" onClick={() => onViewDetail(report.id)}>ดูสรุป</button>
                                    <button className="edit-button" disabled={!report.editable} onClick={() => handleOpenEditModal(report)}>แก้ไข</button>
                                    <button className="finalize-button" disabled={!report.finalizable} onClick={() => handleFinalize(report.id)}>ปิดงาน</button>
                                    <button className="delete-button" disabled={!report.deletable} onClick={() => handleDelete(report.id)}>ลบ</button>
                               </td>
                           </tr>
                       ))}
                   </tbody>
                </table>
            </div>
            <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={editingReport ? 'แก้ไขใบสั่งผลิต' : 'สร้างใบสั่งผลิตใหม่'}>
                <form onSubmit={handleFormSubmit} className="production-form">
                    <div className="form-group"><label className="form-label">หมายเลขคำสั่งผลิต (Order No.)</label><input type="text" name="orderNumber" value={formData.orderNumber} onChange={handleFormChange} className="form-input" /></div>
                    <div className="form-group"><label className="form-label">วันที่เริ่มต้น</label><input type="date" name="startDate" value={formData.startDate} onChange={handleFormChange} className="form-input" required /></div>
                    <div className="form-group"><label className="form-label">วันที่สิ้นสุด</label><input type="date" name="endDate" value={formData.endDate} onChange={handleFormChange} className="form-input" required /></div>
                    <div className="form-group"><label className="form-label">เครื่องจักร</label><select name="machineId" value={formData.machineId} onChange={handleFormChange} className="form-input" required><option value="" disabled>-- เลือกเครื่องจักร --</option>{machines.map(m => <option key={m.id} value={m.id}>{m.machineName}</option>)}</select></div>
                    <div className="form-group"><label className="form-label">ผลิตภัณฑ์</label><select name="productId" value={formData.productId} onChange={handleFormChange} className="form-input" required><option value="" disabled>-- เลือกผลิตภัณฑ์ --</option>{products.map(p => <option key={p.id} value={p.id}>{p.productName}</option>)}</select></div>
                    <div className="form-group"><label className="form-label">เป้าหมาย (Target)</label><input type="number" name="targetQty" value={formData.targetQty} onChange={handleFormChange} className="form-input" /></div>
                    <div className="form-actions"><button type="button" onClick={handleCloseModal} className="cancel-button">ยกเลิก</button><button type="submit" className="save-button">บันทึก</button></div>
                </form>
            </Modal>
        </div>
    );
};

// --- Main PC Dashboard Component ---
const ProductionControlDashboard = () => {
    const [view, setView] = useState('dashboard');
    const [previousView, setPreviousView] = useState('dashboard');
    const [selectedReportId, setSelectedReportId] = useState(null);
    const [selectedDate, setSelectedDate] = useState(null); // **[ใหม่]** State สำหรับเก็บวันที่
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [machines, setMachines] = useState([]);
    const [products, setProducts] = useState([]);
    const [allReports, setAllReports] = useState([]);
    const [dashboardData, setDashboardData] = useState([]);

    const fetchData = async () => {
        if (!loading) setLoading(true);
        try {
            const [machinesRes, productsRes, reportsRes, dashboardRes] = await Promise.all([
                api.get('/pc/machines'), api.get('/pc/products'), api.get('/pc/reports'), api.get('/pc/dashboard-summary')
            ]);
            setMachines(machinesRes.data);
            setProducts(productsRes.data);
            setAllReports(reportsRes.data);
            setDashboardData(dashboardRes.data);
        } catch (err) {
            setError('เกิดข้อผิดพลาดในการดึงข้อมูล'); console.error(err);
        } finally {
            setLoading(false);
        }
    };
    
    useEffect(() => {
        let intervalId = null;
        if (view === 'dashboard') {
            fetchData();
            intervalId = setInterval(() => { api.get('/pc/dashboard-summary').then(res => setDashboardData(res.data)).catch(console.error); }, 30000);
        } else {
            fetchData(); // Fetch data once for other views
        }
        return () => { if (intervalId) { clearInterval(intervalId); } };
    }, [view]);

    const changeView = (newView, data = null) => {
        setPreviousView(view);
        if (newView === 'detail' || newView === 'historyDetail') {
            setSelectedReportId(data);
        }
        if (newView === 'dailyDetail') {
            setSelectedDate(data);
        }
        setView(newView);
    };
    
    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูล...</h2></div>;

    // --- View Router ---
    if (view === 'manage') {
        return <ProductionOrderManagement onBack={() => changeView('dashboard')} onViewDetail={(id) => changeView('detail', id)} machines={machines} products={products} allReports={allReports} fetchData={fetchData}/>;
    }
    if (view === 'detail') {
        return <ReportDetailView reportId={selectedReportId} onBack={() => changeView(previousView)} />;
    }
    if (view === 'history') {
        return <HistoricalReports onBack={() => changeView('dashboard')} onViewDetail={(id) => changeView('detail', id)} onViewHistoryDetail={(id) => changeView('historyDetail', id)} onViewDailyReport={(date) => changeView('dailyDetail', date)} machines={machines} products={products} />;
    }
    if (view === 'historyDetail') {
        return <ShiftBasedReportDetailView reportId={selectedReportId} onBack={() => changeView('history')} />;
    }
    if (view === 'dailyDetail') {
        return <DailyReportView date={selectedDate} onBack={() => changeView('history')} />;
    }


    // Default view: 'dashboard'
    return (
        <div className="pc-dashboard-container">
            <div className="pc-dashboard-header">
                <h2 className="pc-dashboard-title">ภาพรวมการผลิต (Production Overview)</h2>
                <div>
                    <button className="manage-reports-button" onClick={() => changeView('history')} style={{ marginRight: '1rem' }}>ดูรายงานย้อนหลัง</button>
                    <button className="manage-reports-button" onClick={() => changeView('manage')}>จัดการใบสั่งผลิต</button>
                </div>
            </div>
            {error && <p className="error-message">{error}</p>}
            {!error && dashboardData.length === 0 &&
                <div className="no-data-card">
                    <h3>ไม่มีเครื่องจักรที่กำลังทำงานอยู่</h3>
                    <p>สามารถเริ่มได้โดยการไปที่ "จัดการใบสั่งผลิต" เพื่อสร้างใบสั่งผลิตใหม่</p>
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

export default ProductionControlDashboard;