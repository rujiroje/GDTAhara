import React, { useState, useEffect } from 'react';
import * as XLSX from 'xlsx';
import axiosInstance from '../../api/axios';

const errMsg = (err, fallback) => {
    const d = err?.response?.data;
    if (typeof d === 'string') return d;
    if (d?.message) return d.message;
    return fallback;
};

const todayStr = () => new Date().toISOString().split('T')[0];
const plus7Str = () => new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0];

const fmt = (n, dp = 3) =>
    Number(n).toLocaleString('en-US', { minimumFractionDigits: dp, maximumFractionDigits: dp });

const MaterialRequirementDashboard = ({ onBack }) => {
    const [mode, setMode] = useState('range'); // 'range' | 'machine'
    const [from, setFrom] = useState(todayStr());
    const [to, setTo] = useState(plus7Str());
    const [machines, setMachines] = useState([]);
    const [machineId, setMachineId] = useState('');
    const [date, setDate] = useState(todayStr());
    const [loading, setLoading] = useState(false);
    const [rows, setRows] = useState([]);
    const [error, setError] = useState('');
    const [typeFilter, setTypeFilter] = useState('');

    useEffect(() => {
        axiosInstance.get('/pc/production/machines')
            .then(r => setMachines(r.data?.data || r.data || []))
            .catch(() => {});
    }, []);

    const calculate = async () => {
        setLoading(true);
        setError('');
        setRows([]);
        setTypeFilter('');
        try {
            let res;
            if (mode === 'machine' && machineId) {
                res = await axiosInstance.get(`/bom/plan/machine/${machineId}`, { params: { date } });
            } else {
                res = await axiosInstance.get('/bom/plan/date', { params: { from, to } });
            }
            const data = Array.isArray(res.data) ? res.data : [];
            setRows(data);
            if (data.length === 0) setError('ไม่พบข้อมูลในช่วงที่เลือก (อาจยังไม่มีแผนผลิต หรือยังไม่ได้นำเข้า BOM)');
        } catch (err) {
            setError(errMsg(err, 'ไม่สามารถคำนวณได้'));
        } finally {
            setLoading(false);
        }
    };

    const exportToExcel = () => {
        // --- Build heading text ---
        let periodText = '';
        if (mode === 'range') {
            periodText = `ตั้งแต่วันที่ ${from}  ถึง  ${to}`;
        } else {
            const mName = machines.find(m => String(m.id) === String(machineId))?.machineName || machineId;
            periodText = `เครื่องจักร: ${mName}  วันที่: ${date}`;
        }
        const exportedAt = new Date().toLocaleString('th-TH', { dateStyle: 'short', timeStyle: 'short' });

        // --- Column headers ---
        const SUMMARY_HDR  = ['รหัส RM','ชื่อวัตถุดิบ','ประเภท','รวมต้องการ','Stock คงเหลือ','ขาดอีก','หน่วย','สถานะ'];
        const DETAIL_HDR   = ['วันที่','เครื่องจักร','FG Code','รหัส RM','ชื่อวัตถุดิบ','ประเภท','เป้าผลิต (ชิ้น)','qty/FG','Loss%','รวมต้องการ','หน่วย'];

        const toSummaryArr = r => [
            r.rmCode, r.rmName, r.materialType || '',
            r.total,
            r.currentStock != null ? Number.parseFloat(r.currentStock) : '',
            r.shortfall != null ? r.shortfall : '',
            r.unit || '',
            r.currentStock == null ? 'ไม่ทราบ Stock' : r.shortfall > 0 ? 'ขาด' : 'เพียงพอ',
        ];
        const toDetailArr = r => [
            r.planDate || '', r.machineName || '', r.fgCode || '',
            r.rmCode || '', r.rmName || '', r.materialType || '',
            r.targetQty || 0,
            Number.parseFloat(r.quantityPer || 0),
            Number.parseFloat(r.lossPercent || 0),
            Number.parseFloat(r.totalRequired || 0),
            r.unit || '',
        ];

        // --- Helper: build a worksheet with heading + summary section + detail section ---
        const makeSheet = (typeLabel, sumRows, detRows) => {
            const title = typeLabel
                ? `รายงานความต้องการวัตถุดิบ  ประเภท: ${typeLabel}`
                : 'รายงานความต้องการวัตถุดิบ (ทุกประเภท)';
            const aoa = [
                [title],
                [periodText],
                [`Export เมื่อ: ${exportedAt}`],
                [],
                ['--- สรุปความต้องการรวม ---'],
                SUMMARY_HDR,
                ...sumRows.map(toSummaryArr),
                [],
                ['--- รายละเอียดตามแผนผลิต ---'],
                DETAIL_HDR,
                ...detRows.map(toDetailArr),
            ];
            const ws = XLSX.utils.aoa_to_sheet(aoa);
            // Column widths: accommodate the wider detail table (11 cols)
            ws['!cols'] = [
                { wch: 16 }, { wch: 36 }, { wch: 16 }, { wch: 16 },
                { wch: 36 }, { wch: 10 }, { wch: 14 },
                { wch: 10 }, { wch: 8 },  { wch: 14 }, { wch: 8 },
            ];
            return ws;
        };

        const wb = XLSX.utils.book_new();

        // Sheet "ทั้งหมด" — all types, use currently filtered data
        XLSX.utils.book_append_sheet(
            wb,
            makeSheet('', summaryRows, rows),
            'ทั้งหมด'
        );

        // One sheet per type (always from the full unfiltered summaryRows/rows)
        const allTypes = [...new Set(summaryRows.map(r => r.materialType).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b));
        for (const type of allTypes) {
            const typeSummary = summaryRows.filter(r => r.materialType === type);
            const typeDetail  = rows.filter(r => r.materialType === type);
            // Sheet names: max 31 chars, no special chars
            const sheetName = type.replace(/[\\/?*[\]]/g, '').substring(0, 31);
            XLSX.utils.book_append_sheet(wb, makeSheet(type, typeSummary, typeDetail), sheetName);
        }

        // Build filename
        let label = mode === 'range'
            ? `${from}_ถึง_${to}`
            : `${machines.find(m => String(m.id) === String(machineId))?.machineName || machineId}_${date}`;
        XLSX.writeFile(wb, `ความต้องการRM_${label}.xlsx`);
    };

    // Aggregate by rmCode|unit for summary table
    // currentStock comes from the first row with that rmCode (same value for all rows)
    const summaryMap = rows.reduce((acc, r) => {
        const key = `${r.rmCode}|${r.unit}`;
        if (!acc[key]) {
            acc[key] = {
                rmCode: r.rmCode, rmName: r.rmName, materialType: r.materialType,
                unit: r.unit, total: 0,
                currentStock: r.currentStock ?? null,
            };
        }
        acc[key].total += Number.parseFloat(r.totalRequired || 0);
        return acc;
    }, {});
    const summaryRows = Object.values(summaryMap).map(r => ({
        ...r,
        shortfall: r.currentStock != null
            ? Math.max(0, r.total - Number.parseFloat(r.currentStock))
            : null,
    })).sort((a, b) => a.rmCode.localeCompare(b.rmCode));

    // Unique types for filter pills
    const materialTypes = [...new Set(summaryRows.map(r => r.materialType).filter(Boolean))]
        .sort((a, b) => a.localeCompare(b));

    // Apply type filter
    const filteredSummary = typeFilter ? summaryRows.filter(r => r.materialType === typeFilter) : summaryRows;
    const filteredDetail  = typeFilter ? rows.filter(r => r.materialType === typeFilter)       : rows;

    return (
        <div style={{ padding: '1.5rem', maxWidth: 1100, margin: '0 auto' }}>
            <button onClick={onBack} style={backBtnStyle}>&larr; กลับ</button>
            <h2 style={{ marginBottom: '1.5rem' }}>📦 ความต้องการวัตถุดิบตามแผน</h2>

            {/* Filter card */}
            <div style={cardStyle}>
                <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem' }}>
                    <button onClick={() => { setMode('range'); setRows([]); setError(''); }} style={tabBtnStyle(mode === 'range')}>
                        ตามช่วงวันที่
                    </button>
                    <button onClick={() => { setMode('machine'); setRows([]); setError(''); }} style={tabBtnStyle(mode === 'machine')}>
                        ตามเครื่องจักร
                    </button>
                </div>

                {mode === 'range' && (
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

                {mode === 'machine' && (
                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                        <div>
                            <label style={labelStyle}>เครื่องจักร</label>
                            <select value={machineId} onChange={e => setMachineId(e.target.value)} style={inputStyle}>
                                <option value="">-- เลือกเครื่องจักร --</option>
                                {machines.map(m => (
                                    <option key={m.id} value={m.id}>{m.machineName}</option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label style={labelStyle}>วันที่</label>
                            <input type="date" value={date} onChange={e => setDate(e.target.value)} style={inputStyle} />
                        </div>
                    </div>
                )}

                <button
                    onClick={calculate}
                    disabled={loading || (mode === 'machine' && !machineId)}
                    style={{ ...primaryBtnStyle(loading || (mode === 'machine' && !machineId)), marginTop: '1rem' }}
                >
                    {loading ? '⏳ กำลังคำนวณ...' : '📊 คำนวณ'}
                </button>
                {error && rows.length === 0 && <div style={errorStyle}>{error}</div>}
            </div>

            {/* Type filter pills */}
            {summaryRows.length > 0 && materialTypes.length > 1 && (
                <div style={{ ...cardStyle, padding: '0.75rem 1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#374151', marginRight: '0.25rem' }}>ประเภท:</span>
                    <button onClick={() => setTypeFilter('')} style={typePillStyle(typeFilter === '')}>ทั้งหมด ({summaryRows.length})</button>
                    {materialTypes.map(t => (
                        <button key={t} onClick={() => setTypeFilter(t)} style={typePillStyle(typeFilter === t)}>
                            {t} ({summaryRows.filter(r => r.materialType === t).length})
                        </button>
                    ))}
                </div>
            )}

            {/* Summary table */}
            {filteredSummary.length > 0 && (
                <div style={cardStyle}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                        <h3 style={{ margin: 0 }}>
                            สรุปความต้องการรวม ({filteredSummary.length} รายการ RM
                            {typeFilter && <span style={{ color: '#2563eb' }}> — {typeFilter}</span>})
                        </h3>
                        <button onClick={exportToExcel} style={exportBtnStyle}>
                            📥 Export Excel
                        </button>
                    </div>
                    <div style={{ overflowX: 'auto' }}>
                        <table style={tableStyle}>
                            <thead>
                                <tr style={{ background: '#f3f4f6' }}>
                                    <th style={thStyle}>รหัส RM</th>
                                    <th style={thStyle}>ชื่อวัตถุดิบ</th>
                                    <th style={thStyle}>ประเภท</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>รวมต้องการ</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>Stock คงเหลือ</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>ขาดอีก</th>
                                    <th style={thStyle}>หน่วย</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredSummary.map((r, i) => {
                                    const hasShortfall = r.shortfall != null && r.shortfall > 0;
                                    const stockUnknown = r.currentStock == null;
                                    return (
                                        <tr key={i} style={{ borderBottom: '1px solid #e5e7eb', background: hasShortfall ? '#fff7ed' : 'transparent' }}>
                                            <td style={{ ...tdStyle, fontWeight: 600, fontFamily: 'monospace' }}>{r.rmCode}</td>
                                            <td style={tdStyle}>{r.rmName}</td>
                                            <td style={tdStyle}><span style={matTypeBadgeStyle}>{r.materialType}</span></td>
                                            <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 700 }}>
                                                {fmt(r.total)}
                                            </td>
                                            <td style={{ ...tdStyle, textAlign: 'right', color: stockUnknown ? '#9ca3af' : (hasShortfall ? '#dc2626' : '#16a34a'), fontWeight: 600 }}>
                                                {stockUnknown ? '—' : fmt(r.currentStock)}
                                            </td>
                                            <td style={{ ...tdStyle, textAlign: 'right' }}>
                                                {stockUnknown ? (
                                                    <span style={{ color: '#9ca3af', fontSize: '0.8rem' }}>ไม่ทราบ</span>
                                                ) : hasShortfall ? (
                                                    <span style={{ color: '#dc2626', fontWeight: 700 }}>
                                                        🔴 {fmt(r.shortfall)}
                                                    </span>
                                                ) : (
                                                    <span style={{ color: '#16a34a', fontWeight: 600 }}>✓ เพียงพอ</span>
                                                )}
                                            </td>
                                            <td style={tdStyle}>{r.unit}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Detail table */}
            {filteredDetail.length > 0 && (
                <div style={cardStyle}>
                    <h3 style={{ marginTop: 0 }}>รายละเอียดตามแผนผลิต ({filteredDetail.length} รายการ)</h3>
                    <div style={{ overflowX: 'auto' }}>
                        <table style={tableStyle}>
                            <thead>
                                <tr style={{ background: '#f3f4f6' }}>
                                    <th style={thStyle}>วันที่</th>
                                    <th style={thStyle}>เครื่อง</th>
                                    <th style={thStyle}>FG Code</th>
                                    <th style={thStyle}>รหัส RM</th>
                                    <th style={thStyle}>ชื่อวัตถุดิบ</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>เป้าผลิต</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>qty/FG</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>Loss%</th>
                                    <th style={{ ...thStyle, textAlign: 'right' }}>รวมต้องการ</th>
                                    <th style={thStyle}>หน่วย</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredDetail.map((r, i) => (
                                    <tr
                                        key={i}
                                        style={{
                                            borderBottom: '1px solid #e5e7eb',
                                            background: r.isScrap ? '#fff1f2' : 'transparent',
                                        }}
                                    >
                                        <td style={tdStyle}>{r.planDate || '-'}</td>
                                        <td style={tdStyle}>{r.machineName || '-'}</td>
                                        <td style={{ ...tdStyle, fontFamily: 'monospace' }}>{r.fgCode}</td>
                                        <td style={{ ...tdStyle, fontWeight: 600, fontFamily: 'monospace' }}>{r.rmCode}</td>
                                        <td style={tdStyle}>{r.rmName}</td>
                                        <td style={{ ...tdStyle, textAlign: 'right' }}>{Number(r.targetQty).toLocaleString('en-US')}</td>
                                        <td style={{ ...tdStyle, textAlign: 'right' }}>
                                            {fmt(r.quantityPer || 0, 4)}
                                        </td>
                                        <td style={{ ...tdStyle, textAlign: 'right' }}>
                                            {Number.parseFloat(r.lossPercent || 0) > 0 ? `${r.lossPercent}%` : '-'}
                                        </td>
                                        <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 600 }}>
                                            {fmt(r.totalRequired || 0, 4)}
                                        </td>
                                        <td style={tdStyle}>{r.unit}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {!loading && rows.length === 0 && !error && (
                <p style={{ color: '#6b7280', textAlign: 'center', marginTop: '2rem' }}>
                    กดคำนวณเพื่อดูความต้องการวัตถุดิบ
                </p>
            )}
        </div>
    );
};

const typePillStyle = (active) => ({
    padding: '4px 14px', borderRadius: 20, fontSize: '0.8rem', cursor: 'pointer',
    border: `1px solid ${active ? '#2563eb' : '#d1d5db'}`,
    background: active ? '#2563eb' : '#f9fafb',
    color: active ? '#fff' : '#374151',
    fontWeight: active ? 700 : 400,
    transition: 'all 0.15s',
});
const matTypeBadgeStyle = {
    padding: '1px 8px', borderRadius: 10, fontSize: '0.75rem',
    background: '#e5e7eb', color: '#374151',
};
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
const exportBtnStyle = {
    background: '#16a34a', color: '#fff', border: 'none',
    borderRadius: 6, padding: '7px 16px', cursor: 'pointer',
    fontWeight: 600, fontSize: '0.875rem', whiteSpace: 'nowrap',
};
const tableStyle = { width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' };
const thStyle = {
    padding: '8px 12px', textAlign: 'left', fontWeight: 600,
    fontSize: '0.825rem', color: '#374151', borderBottom: '2px solid #e5e7eb',
};
const tdStyle = { padding: '8px 12px', color: '#374151' };

export default MaterialRequirementDashboard;
