import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import MachineSelectGrid from '../common/MachineSelectGrid';

const API_URL = 'http://localhost:8080/api';
const api = axios.create({ baseURL: API_URL });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
}, error => Promise.reject(error));

// ── Type badge ────────────────────────────────────────────────────────────────
const typeColor = { VIRGIN: '#1565c0', EVOH: '#6a0dad', ADMER: '#e65100', MIX: '#2e7d32' };
const TypeBadge = ({ type }) => {
    const c = typeColor[type] || '#546e7a';
    return (
        <span style={{
            background: c + '18', color: c, border: `1px solid ${c}44`,
            borderRadius: 10, padding: '2px 8px', fontWeight: 700, fontSize: '0.75rem', whiteSpace: 'nowrap'
        }}>{type}</span>
    );
};

// ── Scrap type options (same as Technician) ───────────────────────────────────
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

// ── Row state per BOM item ────────────────────────────────────────────────────
const mkRowState = (item) => {
    const isAuto = item.autoDeduct === true;
    return {
        rmCode:     item.rmCode,
        materialId: item.materialId,
        lotNumber:  item.availableLots?.[0] ?? '',
        // HIBE/VERP: pre-fill with BOM suggested qty
        quantity:   isAuto && item.suggestedQty > 0 ? String(item.suggestedQty) : '',
        enabled:    isAuto ? true : item.materialFound,
        autoDeduct: isAuto,
    };
};

// ── BOM Stock-Out Table (View 2) ──────────────────────────────────────────────
const BomStockOutView = ({ report, onBack }) => {
    const [bomItems, setBomItems]   = useState([]);
    const [rows, setRows]           = useState([]);
    const [loading, setLoading]     = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError]         = useState('');
    const [successMsg, setSuccessMsg] = useState('');

    const [isScrapModalOpen, setIsScrapModalOpen] = useState(false);
    const [scrapData, setScrapData] = useState({ scrapDescription: '', weightKg: '' });
    const [scrapSubmitting, setScrapSubmitting] = useState(false);
    const [scrapMsg, setScrapMsg] = useState('');

    const loadBom = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const res = await api.get(`/cm-operator/bom-items/${report.id}`);
            const items = res.data || [];
            setBomItems(items);
            setRows(items.map(mkRowState));
        } catch (e) {
            setError('ไม่สามารถโหลด BOM ได้: ' + (e?.response?.data || e.message));
        } finally {
            setLoading(false);
        }
    }, [report.id]);

    useEffect(() => { loadBom(); }, [loadBom]);

    const setRow = (idx, field, value) =>
        setRows(prev => prev.map((r, i) => i === idx ? { ...r, [field]: value } : r));

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSuccessMsg('');
        setError('');

        const payload = rows
            .filter(r => r.enabled && r.quantity && parseFloat(r.quantity) > 0 && r.materialId)
            .map(r => ({
                productionReportId: report.id,
                materialId:         r.materialId,
                lotNumber:          r.autoDeduct ? null : (r.lotNumber || null),
                quantity:           parseFloat(r.quantity),
                autoDeduct:         r.autoDeduct || false,
            }));

        if (payload.length === 0) {
            setError('กรุณากรอกปริมาณอย่างน้อย 1 รายการ');
            return;
        }

        setSubmitting(true);
        try {
            await api.post('/cm-operator/stock-out/batch', payload);
            setSuccessMsg(`บันทึกสำเร็จ ${payload.length} รายการ`);
            // Reset quantity fields after submit, keep lot selection
            setRows(prev => prev.map(r => ({ ...r, quantity: '' })));
        } catch (e) {
            setError('เกิดข้อผิดพลาด: ' + (e?.response?.data || e.message));
        } finally {
            setSubmitting(false);
        }
    };

    const handleScrapSubmit = async (e) => {
        e.preventDefault();
        setScrapMsg('');
        if (!scrapData.scrapDescription || !scrapData.weightKg) return;
        const parts = scrapData.scrapDescription.split(' ');
        const matType = parts[0];
        const scrapType = parts.slice(1).join(' ');
        setScrapSubmitting(true);
        try {
            await api.post(`/technician/reports/${report.id}/scrap-weight`, {
                matType, scrapType, weightKg: parseFloat(scrapData.weightKg),
            });
            setScrapMsg(`✅ บันทึกสำเร็จ: ${scrapData.scrapDescription} ${scrapData.weightKg} Kg`);
            setScrapData({ scrapDescription: '', weightKg: '' });
        } catch (e) {
            setScrapMsg('❌ เกิดข้อผิดพลาด: ' + (e?.response?.data || e.message));
        } finally {
            setScrapSubmitting(false);
        }
    };

    const activeCount = rows.filter(r => r.enabled && r.quantity && parseFloat(r.quantity) > 0).length;

    return (
        <div className="dashboard-card">
            <button onClick={onBack} className="back-button">&larr; กลับไปเลือกใบสั่งผลิต</button>
            <h2 className="dashboard-title">ขั้นตอนที่ 2: บันทึกการเบิกจ่ายวัตถุดิบ (Stock-Out)</h2>

            <div className="selected-report-info" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
                    <span><strong>Order No.:</strong> {report.orderNumber}</span>
                    <span><strong>เครื่องจักร:</strong> {report.machineName}</span>
                    <span><strong>ผลิตภัณฑ์:</strong> {report.productName}</span>
                </div>
                <button
                    type="button"
                    onClick={() => { setIsScrapModalOpen(true); setScrapMsg(''); }}
                    style={{ background: '#7b3f00', color: '#fff', border: 'none', borderRadius: 6, padding: '7px 16px', cursor: 'pointer', fontWeight: 600, fontSize: '0.85rem', whiteSpace: 'nowrap' }}
                >
                    ⚖️ ชั่งน้ำหนักของเสีย
                </button>
            </div>

            {/* Scrap weight modal */}
            {isScrapModalOpen && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <div style={{ background: '#fff', borderRadius: 10, padding: '1.75rem', width: 420, maxWidth: '95vw', boxShadow: '0 8px 32px rgba(0,0,0,0.2)' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                            <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>ชั่งน้ำหนักของเสีย</h3>
                            <button onClick={() => setIsScrapModalOpen(false)} style={{ background: 'none', border: 'none', fontSize: '1.4rem', cursor: 'pointer', color: '#666' }}>&times;</button>
                        </div>
                        <form onSubmit={handleScrapSubmit}>
                            <div style={{ marginBottom: '1rem' }}>
                                <label style={{ display: 'block', fontWeight: 600, marginBottom: 4, fontSize: '0.85rem' }}>ประเภทของเสีย</label>
                                <select
                                    value={scrapData.scrapDescription}
                                    onChange={e => setScrapData(p => ({ ...p, scrapDescription: e.target.value }))}
                                    required
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #cdd2d8', fontSize: '0.85rem' }}
                                >
                                    <option value="">-- เลือกประเภท --</option>
                                    {scrapOptions.map(o => <option key={o} value={o}>{o}</option>)}
                                </select>
                            </div>
                            <div style={{ marginBottom: '1.25rem' }}>
                                <label style={{ display: 'block', fontWeight: 600, marginBottom: 4, fontSize: '0.85rem' }}>น้ำหนัก (Kg.)</label>
                                <input
                                    type="number" step="0.01" min="0" required
                                    value={scrapData.weightKg}
                                    onChange={e => setScrapData(p => ({ ...p, weightKg: e.target.value }))}
                                    placeholder="0.00"
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #cdd2d8', fontSize: '0.85rem', boxSizing: 'border-box' }}
                                />
                            </div>
                            {scrapMsg && (
                                <div style={{ marginBottom: '1rem', padding: '8px 12px', borderRadius: 6, background: scrapMsg.startsWith('✅') ? '#d4edda' : '#fff3cd', fontSize: '0.83rem' }}>
                                    {scrapMsg}
                                </div>
                            )}
                            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                                <button type="button" onClick={() => setIsScrapModalOpen(false)}
                                    style={{ padding: '7px 18px', borderRadius: 6, border: '1px solid #cdd2d8', background: '#f8f9fa', cursor: 'pointer', fontWeight: 600 }}>
                                    ปิด
                                </button>
                                <button type="submit" disabled={scrapSubmitting}
                                    style={{ padding: '7px 18px', borderRadius: 6, border: 'none', background: '#7b3f00', color: '#fff', cursor: 'pointer', fontWeight: 600 }}>
                                    {scrapSubmitting ? 'กำลังบันทึก...' : 'บันทึก'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {error && (
                <div style={{ margin: '1rem 0', padding: '0.75rem 1rem', background: '#fff3cd', border: '1px solid #ffc107', borderRadius: 6, fontSize: '0.87rem' }}>
                    ⚠️ {error}
                </div>
            )}
            {successMsg && (
                <div style={{ margin: '1rem 0', padding: '0.75rem 1rem', background: '#d4edda', border: '1px solid #28a745', borderRadius: 6, fontSize: '0.87rem' }}>
                    ✅ {successMsg}
                </div>
            )}

            {loading ? (
                <div style={{ padding: '2rem', textAlign: 'center', color: '#666' }}>⏳ กำลังโหลด BOM...</div>
            ) : bomItems.length === 0 ? (
                <div style={{ padding: '2rem', textAlign: 'center', background: '#f8f9fa', borderRadius: 8, color: '#666' }}>
                    <p style={{ fontWeight: 600 }}>ไม่พบ BOM สำหรับสินค้านี้</p>
                    <p style={{ fontSize: '0.85rem' }}>กรุณาติดต่อ Admin เพื่ออัปโหลด BOM ก่อนบันทึกการใช้วัตถุดิบ</p>
                </div>
            ) : (
                <form onSubmit={handleSubmit} style={{ marginTop: '1.5rem' }}>
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.87rem' }}>
                            <thead>
                                <tr style={{ background: '#1a3a5c', color: '#fff' }}>
                                    <th style={th()}>ใช้</th>
                                    <th style={th()}>รหัส</th>
                                    <th style={th()}>ชื่อวัตถุดิบ</th>
                                    <th style={th()}>ประเภท</th>
                                    <th style={th('right')}>BOM (Kg)</th>
                                    <th style={th()}>Lot Number</th>
                                    <th style={th('right')}>ปริมาณที่ใช้จริง (Kg)</th>
                                </tr>
                            </thead>
                            <tbody>
                                {bomItems.map((item, idx) => {
                                    const row = rows[idx];
                                    if (!row) return null;
                                    const isAuto  = item.autoDeduct === true;
                                    const noMat   = !item.materialFound;
                                    const noLots  = !isAuto && item.availableLots?.length === 0;
                                    const rowBg   = isAuto
                                        ? (idx % 2 === 0 ? '#eaf4fb' : '#daeef8')
                                        : (noMat ? '#fff8f0' : (idx % 2 === 0 ? '#fff' : '#f8f9fa'));

                                    return (
                                        <tr key={item.rmCode} style={{ background: rowBg, opacity: noMat && !isAuto ? 0.7 : 1 }}>
                                            {/* Checkbox / Auto badge */}
                                            <td style={td('center')}>
                                                {isAuto ? (
                                                    <span style={{
                                                        background: '#0277bd22', color: '#0277bd',
                                                        border: '1px solid #0277bd55',
                                                        borderRadius: 10, padding: '2px 7px',
                                                        fontSize: '0.68rem', fontWeight: 700, whiteSpace: 'nowrap'
                                                    }}>AUTO</span>
                                                ) : (
                                                    <input
                                                        type="checkbox"
                                                        checked={row.enabled && !noMat}
                                                        disabled={noMat}
                                                        onChange={e => setRow(idx, 'enabled', e.target.checked)}
                                                        style={{ width: 16, height: 16, cursor: noMat ? 'not-allowed' : 'pointer' }}
                                                    />
                                                )}
                                            </td>
                                            {/* Code */}
                                            <td style={{ ...td(), fontFamily: 'monospace', fontWeight: 600 }}>
                                                {item.rmCode}
                                                {noMat && !isAuto && <div style={{ fontSize: '0.7rem', color: '#e65100' }}>ไม่พบใน Master</div>}
                                            </td>
                                            {/* Name */}
                                            <td style={td()}>{item.materialName || '-'}</td>
                                            {/* Type */}
                                            <td style={td('center')}><TypeBadge type={item.materialType} /></td>
                                            {/* BOM suggested qty */}
                                            <td style={{ ...td('right'), fontFamily: 'monospace' }}>
                                                {item.suggestedQty > 0
                                                    ? Number(item.suggestedQty).toFixed(3)
                                                    : <span style={{ color: '#aaa' }}>-</span>}
                                            </td>
                                            {/* Lot Number */}
                                            <td style={td()}>
                                                {isAuto ? (
                                                    <span style={{ color: '#0277bd', fontSize: '0.78rem', fontStyle: 'italic' }}>
                                                        ตัดอัตโนมัติ
                                                    </span>
                                                ) : noMat ? (
                                                    <span style={{ color: '#aaa', fontSize: '0.78rem' }}>—</span>
                                                ) : noLots ? (
                                                    <span style={{ color: '#e65100', fontSize: '0.78rem' }}>ไม่มี Stock</span>
                                                ) : (
                                                    <select
                                                        value={row.lotNumber}
                                                        onChange={e => setRow(idx, 'lotNumber', e.target.value)}
                                                        disabled={!row.enabled}
                                                        style={inputStyle(!row.enabled)}
                                                    >
                                                        {item.availableLots.map(l => <option key={l} value={l}>{l}</option>)}
                                                    </select>
                                                )}
                                            </td>
                                            {/* Actual qty */}
                                            <td style={td('right')}>
                                                {noMat && !isAuto ? (
                                                    <span style={{ color: '#aaa', fontSize: '0.78rem' }}>—</span>
                                                ) : (
                                                    <input
                                                        type="number"
                                                        step="0.001"
                                                        min="0"
                                                        value={row.quantity}
                                                        onChange={e => setRow(idx, 'quantity', e.target.value)}
                                                        disabled={!row.enabled}
                                                        placeholder={item.suggestedQty > 0 ? Number(item.suggestedQty).toFixed(3) : '0.000'}
                                                        style={{ ...inputStyle(!row.enabled), textAlign: 'right', width: '110px',
                                                            ...(isAuto ? { background: '#eaf4fb', borderColor: '#0277bd55' } : {}) }}
                                                    />
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>

                    <div style={{ marginTop: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem', justifyContent: 'flex-end' }}>
                        <span style={{ fontSize: '0.85rem', color: '#5a6a7a' }}>
                            {activeCount} รายการ พร้อมบันทึก
                        </span>
                        <button
                            type="submit"
                            disabled={submitting || activeCount === 0}
                            className="submit-button"
                            style={{ minWidth: 160, opacity: activeCount === 0 ? 0.5 : 1 }}
                        >
                            {submitting ? '⏳ กำลังบันทึก...' : `บันทึกการเบิกจ่าย (${activeCount})`}
                        </button>
                    </div>
                </form>
            )}
        </div>
    );
};

// ── Style helpers ─────────────────────────────────────────────────────────────
const th = (align) => ({
    padding: '8px 12px', textAlign: align || 'left', fontWeight: 600,
    borderBottom: '2px solid #0d2a45', whiteSpace: 'nowrap'
});
const td = (align) => ({
    padding: '7px 12px', textAlign: align || 'left',
    borderBottom: '1px solid #e9ecef', verticalAlign: 'middle'
});
const inputStyle = (disabled) => ({
    padding: '5px 8px', borderRadius: 4,
    border: `1px solid ${disabled ? '#dee2e6' : '#adb5bd'}`,
    background: disabled ? '#f8f9fa' : '#fff',
    fontSize: '0.85rem', color: disabled ? '#aaa' : '#000',
    cursor: disabled ? 'not-allowed' : 'auto'
});

// ── Main Dashboard ────────────────────────────────────────────────────────────
const CmOperatorDashboard = () => {
    const [activeReports, setActiveReports] = useState([]);
    const [loading, setLoading]             = useState(true);
    const [error, setError]                 = useState('');
    const [selectedReport, setSelectedReport] = useState(null);

    useEffect(() => {
        (async () => {
            try {
                const res = await api.get('/pc/reports/active');
                const data = res.data?.data || res.data || [];
                setActiveReports(Array.isArray(data) ? data : []);
            } catch {
                setError('ไม่สามารถดึงข้อมูลใบสั่งผลิตได้');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) return <div className="loading-container"><h2>กำลังโหลดข้อมูล...</h2></div>;

    if (!selectedReport) {
        return (
            <div className="dashboard-card">
                {error && <p className="error-message">{error}</p>}
                <MachineSelectGrid
                    reports={activeReports}
                    onSelect={setSelectedReport}
                    title="เลือกใบสั่งผลิตเพื่อบันทึกการใช้วัตถุดิบ"
                />
            </div>
        );
    }

    return (
        <BomStockOutView
            report={selectedReport}
            onBack={() => setSelectedReport(null)}
        />
    );
};

export default CmOperatorDashboard;
