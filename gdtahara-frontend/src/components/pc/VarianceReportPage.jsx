import React, { useState } from 'react';
import axiosInstance from '../../api/axios';

const errMsg = (err, fallback) => {
    const d = err?.response?.data;
    if (typeof d === 'string') return d;
    if (d?.message) return d.message;
    return fallback;
};

const todayStr = () => new Date().toISOString().split('T')[0];
const minus7Str = () => new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0];

const statusColors = {
    GREEN:  { background: '#d1fae5', color: '#065f46' },
    YELLOW: { background: '#fef3c7', color: '#92400e' },
    RED:    { background: '#fee2e2', color: '#991b1b' },
};

const VarianceReportPage = ({ onBack }) => {
    const [mode, setMode] = useState('range'); // 'report' | 'range'
    const [reportId, setReportId] = useState('');
    const [from, setFrom] = useState(minus7Str());
    const [to, setTo] = useState(todayStr());
    const [loading, setLoading] = useState(false);
    const [rows, setRows] = useState([]);
    const [error, setError] = useState('');
    const [filterStatus, setFilterStatus] = useState('ALL');

    const analyze = async () => {
        setLoading(true);
        setError('');
        setRows([]);
        setFilterStatus('ALL');
        try {
            let res;
            if (mode === 'report') {
                if (!reportId) { setError('กรุณากรอก Report ID'); setLoading(false); return; }
                res = await axiosInstance.get(`/bom/report/${reportId}/variance`);
            } else {
                res = await axiosInstance.get('/bom/report/variance/range', { params: { from, to } });
            }
            const data = Array.isArray(res.data) ? res.data : [];
            setRows(data);
            if (data.length === 0)
                setError('ไม่พบข้อมูล Variance (อาจไม่มีแผนผลิตที่มี BOM หรือไม่มีรายการนำออก RM)');
        } catch (err) {
            if (err.response?.status === 404) setError('ไม่พบข้อมูล Report ID นี้');
            else setError(errMsg(err, 'เกิดข้อผิดพลาด'));
        } finally {
            setLoading(false);
        }
    };

    const displayed = filterStatus === 'ALL' ? rows : rows.filter(r => r.status === filterStatus);
    const greenCount  = rows.filter(r => r.status === 'GREEN').length;
    const yellowCount = rows.filter(r => r.status === 'YELLOW').length;
    const redCount    = rows.filter(r => r.status === 'RED').length;

    return (
        <div style={{ padding: '1.5rem', maxWidth: 1200, margin: '0 auto' }}>
            <button onClick={onBack} style={backBtnStyle}>&larr; กลับ</button>
            <h2 style={{ marginBottom: '1.5rem' }}>📊 รายงาน Variance วัตถุดิบ</h2>

            {/* Filter card */}
            <div style={cardStyle}>
                <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem' }}>
                    <button onClick={() => { setMode('report'); setRows([]); setError(''); }} style={tabBtnStyle(mode === 'report')}>
                        ตาม Report ID
                    </button>
                    <button onClick={() => { setMode('range'); setRows([]); setError(''); }} style={tabBtnStyle(mode === 'range')}>
                        ตามช่วงวันที่
                    </button>
                </div>

                {mode === 'report' ? (
                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                        <div>
                            <label style={labelStyle}>Production Report ID</label>
                            <input
                                type="number"
                                value={reportId}
                                onChange={e => setReportId(e.target.value)}
                                onKeyDown={e => e.key === 'Enter' && analyze()}
                                placeholder="เช่น 42"
                                style={{ ...inputStyle, width: 160 }}
                            />
                        </div>
                    </div>
                ) : (
                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                        <div>
                            <label style={labelStyle}>จากวันที่</label>
                            <input type="date" value={from} onChange={e => setFrom(e.target.value)} style={inputStyle} />
                        </div>
                        <div>
                            <label style={labelStyle}>ถึงวันที่</label>
                            <input type="date" value={to} onChange={e => setTo(e.target.value)} style={inputStyle} />
                        </div>
                    </div>
                )}

                <button onClick={analyze} disabled={loading} style={{ ...primaryBtnStyle(loading), marginTop: '1rem' }}>
                    {loading ? '⏳ กำลังวิเคราะห์...' : '📊 วิเคราะห์ Variance'}
                </button>
                {error && rows.length === 0 && <div style={errorStyle}>{error}</div>}
            </div>

            {/* Summary badges + filter */}
            {rows.length > 0 && (
                <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.25rem', flexWrap: 'wrap', alignItems: 'center' }}>
                    <SummaryBadge
                        label="ทั้งหมด" count={rows.length}
                        active={filterStatus === 'ALL'}
                        onClick={() => setFilterStatus('ALL')}
                        bg="#f3f4f6" color="#374151"
                    />
                    <SummaryBadge
                        label="🟢 OK (≤5%)" count={greenCount}
                        active={filterStatus === 'GREEN'}
                        onClick={() => setFilterStatus('GREEN')}
                        bg="#d1fae5" color="#065f46"
                    />
                    <SummaryBadge
                        label="🟡 Warning (5-10%)" count={yellowCount}
                        active={filterStatus === 'YELLOW'}
                        onClick={() => setFilterStatus('YELLOW')}
                        bg="#fef3c7" color="#92400e"
                    />
                    <SummaryBadge
                        label="🔴 Alert (>10%)" count={redCount}
                        active={filterStatus === 'RED'}
                        onClick={() => setFilterStatus('RED')}
                        bg="#fee2e2" color="#991b1b"
                    />
                </div>
            )}

            {/* Variance table */}
            {displayed.length > 0 && (
                <div style={cardStyle}>
                    <h3 style={{ marginTop: 0 }}>
                        ตาราง Variance
                        {filterStatus !== 'ALL' && ` (กรอง: ${filterStatus})`}
                        {' '}({displayed.length} รายการ)
                    </h3>
                    <div style={{ overflowX: 'auto' }}>
                        <table style={tableStyle}>
                            <thead>
                                <tr style={{ background: '#f3f4f6' }}>
                                    <th style={thStyle}>FG Code</th>
                                    <th style={thStyle}>รหัส RM</th>
                                    <th style={thStyle}>ชื่อวัตถุดิบ</th>
                                    <th style={thStyle}>ประเภท</th>
                                    <th style={thStyle}>หน่วย</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>แผน</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>จริง</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>Variance</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>%</th>
                                    <th style={{ ...thStyle, textAlign: 'center' }}>สถานะ</th>
                                </tr>
                            </thead>
                            <tbody>
                                {displayed.map((r, i) => {
                                    const varNum = parseFloat(r.variance || 0);
                                    const varColor = varNum > 0 ? '#dc2626' : varNum < 0 ? '#16a34a' : '#6b7280';
                                    const sc = statusColors[r.status] || statusColors.GREEN;
                                    return (
                                        <tr
                                            key={i}
                                            style={{
                                                borderBottom: '1px solid #e5e7eb',
                                                background: r.isScrap ? '#fff8f1' : 'transparent',
                                            }}
                                        >
                                            <td style={{ ...tdStyle, fontFamily: 'monospace' }}>{r.fgCode}</td>
                                            <td style={{ ...tdStyle, fontWeight: 600, fontFamily: 'monospace' }}>{r.rmCode}</td>
                                            <td style={tdStyle}>{r.rmName}</td>
                                            <td style={tdStyle}>{r.materialType}</td>
                                            <td style={tdStyle}>{r.unit}</td>
                                            <td style={{ ...tdStyle, textAlign: 'right' }}>
                                                {parseFloat(r.plannedQty || 0).toFixed(3)}
                                            </td>
                                            <td style={{ ...tdStyle, textAlign: 'right' }}>
                                                {parseFloat(r.actualQty || 0).toFixed(3)}
                                            </td>
                                            <td style={{ ...tdStyle, textAlign: 'right', color: varColor, fontWeight: 600 }}>
                                                {varNum >= 0 ? '+' : ''}{varNum.toFixed(3)}
                                            </td>
                                            <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 600 }}>
                                                {parseFloat(r.variancePercent || 0).toFixed(2)}%
                                            </td>
                                            <td style={{ ...tdStyle, textAlign: 'center' }}>
                                                <span style={{
                                                    ...sc,
                                                    padding: '3px 12px', borderRadius: 12,
                                                    fontSize: '0.8rem', fontWeight: 700,
                                                }}>
                                                    {r.status}
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

            {!loading && rows.length === 0 && !error && (
                <p style={{ color: '#6b7280', textAlign: 'center', marginTop: '2rem' }}>
                    กดวิเคราะห์เพื่อดูรายงาน Variance
                </p>
            )}
        </div>
    );
};

const SummaryBadge = ({ label, count, bg, color, active, onClick }) => (
    <div
        onClick={onClick}
        style={{
            background: bg, color,
            padding: '10px 18px', borderRadius: 8,
            fontWeight: 600, cursor: 'pointer',
            border: active ? `2px solid ${color}` : '2px solid transparent',
            transition: 'border 0.15s',
        }}
    >
        <span style={{ fontSize: '1.4rem', marginRight: 8 }}>{count}</span>
        <span style={{ fontSize: '0.85rem' }}>{label}</span>
    </div>
);

const backBtnStyle = {
    background: 'none', border: '1px solid #d1d5db', borderRadius: 6,
    padding: '6px 14px', cursor: 'pointer', marginBottom: '1rem',
};
const cardStyle = {
    background: '#fff', borderRadius: 8,
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)', padding: '1.25rem', marginBottom: '1.25rem',
};
const labelStyle = { display: 'block', marginBottom: 4, fontSize: '0.85rem', fontWeight: 600, color: '#374151' };
const inputStyle = { padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: '0.9rem', minWidth: 160 };
const primaryBtnStyle = (disabled) => ({
    background: disabled ? '#9ca3af' : '#2563eb', color: '#fff', border: 'none',
    borderRadius: 6, padding: '8px 20px', cursor: disabled ? 'not-allowed' : 'pointer', fontWeight: 600,
});
const tabBtnStyle = (active) => ({
    padding: '6px 16px', borderRadius: 6,
    border: `1px solid ${active ? '#2563eb' : '#d1d5db'}`,
    background: active ? '#dbeafe' : '#f9fafb',
    color: active ? '#1d4ed8' : '#374151',
    cursor: 'pointer', fontWeight: active ? 600 : 400,
});
const errorStyle = {
    marginTop: '0.75rem', background: '#fee2e2', color: '#dc2626',
    padding: '0.75rem', borderRadius: 6, fontSize: '0.875rem',
};
const tableStyle = { width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' };
const thStyle = {
    padding: '8px 12px', textAlign: 'left', fontWeight: 600,
    fontSize: '0.825rem', color: '#374151', borderBottom: '2px solid #e5e7eb',
};
const tdStyle = { padding: '8px 12px', color: '#374151' };

export default VarianceReportPage;
