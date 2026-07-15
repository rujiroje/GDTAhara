import React, { useState } from 'react';
import axiosInstance from '../../api/axios';

const errMsg = (err, fallback) => {
    const d = err?.response?.data;
    if (typeof d === 'string') return d;
    if (d?.message) return d.message;
    return fallback;
};

const BomViewerPage = ({ onBack }) => {
    const [fgCode, setFgCode] = useState('');
    const [loading, setLoading] = useState(false);
    const [bom, setBom] = useState(null);
    const [error, setError] = useState('');
    const [showHistory, setShowHistory] = useState(false);
    const [history, setHistory] = useState([]);

    const search = async () => {
        const code = fgCode.trim().toUpperCase();
        if (!code) return;
        setLoading(true);
        setError('');
        setBom(null);
        setHistory([]);
        setShowHistory(false);
        try {
            const res = await axiosInstance.get(`/bom/${code}`);
            setBom(res.data);
        } catch (err) {
            if (err.response?.status === 404)
                setError(`ไม่พบ BOM สำหรับรหัส "${code}" (อาจยังไม่ได้นำเข้า หรือหมดอายุ)`);
            else
                setError(errMsg(err, 'เกิดข้อผิดพลาด'));
        } finally {
            setLoading(false);
        }
    };

    const loadHistory = async () => {
        const code = fgCode.trim().toUpperCase();
        if (!code) return;
        setLoading(true);
        setError('');
        try {
            const res = await axiosInstance.get(`/bom/${code}/history`);
            setHistory(res.data || []);
            setShowHistory(true);
        } catch (err) {
            setError(errMsg(err, 'ไม่สามารถโหลดประวัติได้'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ padding: '1.5rem', maxWidth: 1100, margin: '0 auto' }}>
            <button onClick={onBack} style={backBtnStyle}>&larr; กลับ</button>
            <h2 style={{ marginBottom: '1.5rem' }}>🔍 ดู BOM สูตรผลิต</h2>

            <div style={cardStyle}>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                    <div>
                        <label style={labelStyle}>รหัสสินค้า FG (Material Code)</label>
                        <input
                            value={fgCode}
                            onChange={e => setFgCode(e.target.value.toUpperCase())}
                            onKeyDown={e => e.key === 'Enter' && search()}
                            placeholder="เช่น 1001234"
                            style={inputStyle}
                        />
                    </div>
                    <button onClick={search} disabled={loading || !fgCode.trim()} style={primaryBtnStyle(loading || !fgCode.trim())}>
                        {loading ? '⏳ กำลังค้นหา...' : '🔍 ค้นหา'}
                    </button>
                    {bom && !showHistory && (
                        <button onClick={loadHistory} style={secondaryBtnStyle}>📜 ดูประวัติ</button>
                    )}
                    {showHistory && (
                        <button onClick={() => setShowHistory(false)} style={secondaryBtnStyle}>← กลับ Active BOM</button>
                    )}
                </div>
                {error && <div style={errorStyle}>{error}</div>}
            </div>

            {/* Active BOM view */}
            {bom && !showHistory && (
                <>
                    <div style={cardStyle}>
                        <h3 style={{ marginTop: 0 }}>BOM Header — {bom.fgCode}</h3>
                        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', fontSize: '0.9rem' }}>
                            <InfoCell label="สถานะ" value={<span style={statusBadgeStyle(bom.status)}>{bom.status}</span>} />
                            <InfoCell label="Alt No." value={bom.alternativeNumber} />
                            <InfoCell label="Plant" value={bom.plantCode} />
                            <InfoCell label="Material Group" value={bom.materialGroup || '-'} />
                            <InfoCell label="SAP Type" value={bom.sapMaterialType || '-'} />
                            <InfoCell label="Base Qty" value={`${bom.baseQuantity} ${bom.unit}`} />
                            <InfoCell label="มีผลตั้งแต่" value={bom.effectiveFrom || '-'} />
                            <InfoCell label="หมดอายุ" value={bom.effectiveTo || <span style={{ color: '#16a34a' }}>ยังมีผล</span>} />
                            <InfoCell label="นำเข้าจาก" value={bom.importedFromFile || '-'} />
                            <InfoCell label="โดย" value={bom.importedBy || '-'} />
                        </div>
                    </div>

                    <div style={cardStyle}>
                        <h3 style={{ marginTop: 0 }}>รายการวัตถุดิบ ({bom.items?.length ?? 0} รายการ)</h3>
                        {bom.items?.some(i => i.isScrap) && (
                            <p style={{ fontSize: '0.85rem', color: '#dc2626', marginTop: 0 }}>
                                ⚠️ แถวสีชมพู = รายการ Scrap (qty ติดลบ หรือประเภท SCAP)
                            </p>
                        )}
                        <div style={{ overflowX: 'auto' }}>
                            <table style={tableStyle}>
                                <thead>
                                    <tr style={{ background: '#f3f4f6' }}>
                                        <th style={thStyle}>#</th>
                                        <th style={thStyle}>รหัส RM</th>
                                        <th style={thStyle}>ชื่อวัตถุดิบ</th>
                                        <th style={thStyle}>ประเภท</th>
                                        <th style={{ ...thStyle, textAlign: 'right' }}>qty/FG</th>
                                        <th style={thStyle}>หน่วย</th>
                                        <th style={{ ...thStyle, textAlign: 'right' }}>Loss%</th>
                                        <th style={thStyle}>สถานะ</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {(bom.items || []).map((item, i) => (
                                        <tr
                                            key={item.id || i}
                                            style={{
                                                borderBottom: '1px solid #e5e7eb',
                                                background: item.isScrap ? '#fff1f2' : 'transparent',
                                            }}
                                        >
                                            <td style={tdStyle}>{item.itemNumber}</td>
                                            <td style={{ ...tdStyle, fontWeight: 600, fontFamily: 'monospace' }}>{item.rmCode}</td>
                                            <td style={tdStyle}>{item.rmName}</td>
                                            <td style={tdStyle}><span style={matTypeBadgeStyle}>{item.materialType}</span></td>
                                            <td style={{ ...tdStyle, textAlign: 'right' }}>{parseFloat(item.quantityPer || 0).toFixed(4)}</td>
                                            <td style={tdStyle}>{item.unit}</td>
                                            <td style={{ ...tdStyle, textAlign: 'right' }}>
                                                {parseFloat(item.lossPercent || 0) > 0 ? `${item.lossPercent}%` : '-'}
                                            </td>
                                            <td style={tdStyle}>
                                                {item.isScrap
                                                    ? <span style={{ color: '#dc2626', fontWeight: 600 }}>⚠️ SCRAP</span>
                                                    : <span style={{ color: '#16a34a' }}>✓ ปกติ</span>}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </>
            )}

            {/* History view */}
            {showHistory && history.length > 0 && (
                <div style={cardStyle}>
                    <h3 style={{ marginTop: 0 }}>ประวัติ BOM — {fgCode} ({history.length} เวอร์ชัน)</h3>
                    {history.map((h, i) => (
                        <div
                            key={i}
                            style={{
                                borderLeft: `4px solid ${h.status === 'ACTIVE' ? '#16a34a' : '#9ca3af'}`,
                                paddingLeft: '1rem', marginBottom: '0.75rem',
                                padding: '0.75rem 1rem',
                                background: h.status === 'ACTIVE' ? '#f0fdf4' : '#f9fafb',
                                borderRadius: '0 6px 6px 0',
                            }}
                        >
                            <span style={{ ...statusBadgeStyle(h.status), marginRight: '0.75rem' }}>{h.status}</span>
                            <strong>Alt {h.alternativeNumber}</strong>
                            {' | '}มีผล: <strong>{h.effectiveFrom}</strong>
                            {' → '}
                            <strong>{h.effectiveTo || 'ยังมีผล'}</strong>
                            {' | '}{h.items?.length ?? 0} รายการ
                            {' | '}Plant: {h.plantCode}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

const InfoCell = ({ label, value }) => (
    <div>
        <div style={{ color: '#6b7280', fontSize: '0.78rem', marginBottom: 2 }}>{label}</div>
        <div style={{ fontWeight: 600 }}>{value}</div>
    </div>
);

const statusBadgeStyle = (s) => ({
    padding: '2px 10px', borderRadius: 12, fontSize: '0.8rem', fontWeight: 600,
    background: s === 'ACTIVE' ? '#d1fae5' : '#f3f4f6',
    color: s === 'ACTIVE' ? '#065f46' : '#374151',
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
const inputStyle = { padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: '0.9rem', width: 220 };
const primaryBtnStyle = (disabled) => ({
    background: disabled ? '#9ca3af' : '#2563eb', color: '#fff', border: 'none',
    borderRadius: 6, padding: '8px 20px', cursor: disabled ? 'not-allowed' : 'pointer', fontWeight: 600,
});
const secondaryBtnStyle = {
    background: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: 6, padding: '8px 16px', cursor: 'pointer',
};
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

export default BomViewerPage;
