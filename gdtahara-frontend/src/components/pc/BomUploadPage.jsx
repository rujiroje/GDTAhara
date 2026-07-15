import React, { useState, useEffect, useRef } from 'react';
import axiosInstance from '../../api/axios';

const errMsg = (err, fallback) => {
    const d = err?.response?.data;
    if (typeof d === 'string') return d;
    if (d?.message) return d.message;
    return fallback;
};

const BomUploadPage = ({ onBack }) => {
    const fileRef = useRef(null);
    const [file, setFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState('');
    const [logs, setLogs] = useState([]);
    const [loadingLogs, setLoadingLogs] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [syncResult, setSyncResult] = useState(null);
    const [syncError, setSyncError] = useState('');

    const loadLogs = async () => {
        setLoadingLogs(true);
        try {
            const res = await axiosInstance.get('/bom/import-log');
            setLogs(Array.isArray(res.data) ? res.data : []);
        } catch {
            setLogs([]);
        } finally {
            setLoadingLogs(false);
        }
    };

    useEffect(() => { loadLogs(); }, []);

    const handleSync = async () => {
        setSyncing(true);
        setSyncResult(null);
        setSyncError('');
        try {
            const res = await axiosInstance.post('/bom/sync-products');
            setSyncResult(res.data);
        } catch (err) {
            setSyncError(errMsg(err, 'เกิดข้อผิดพลาดในการ Sync'));
        } finally {
            setSyncing(false);
        }
    };

    const handleUpload = async () => {
        if (!file) { setError('กรุณาเลือกไฟล์'); return; }
        setError('');
        setResult(null);
        setUploading(true);
        try {
            const formData = new FormData();
            formData.append('file', file);
            const res = await axiosInstance.post('/bom/import', formData, {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
            setResult(res.data);
            loadLogs();
        } catch (err) {
            setError(errMsg(err, 'เกิดข้อผิดพลาดในการนำเข้า'));
        } finally {
            setUploading(false);
        }
    };

    return (
        <div style={{ padding: '1.5rem', maxWidth: 900, margin: '0 auto' }}>
            <button onClick={onBack} style={backBtnStyle}>&larr; กลับ</button>
            <h2 style={{ marginBottom: '1.5rem' }}>📤 นำเข้า BOM จาก Excel</h2>

            {/* Upload card */}
            <div style={cardStyle}>
                <h3 style={{ marginTop: 0 }}>เลือกไฟล์ BOM (.xlsx)</h3>
                <p style={{ color: '#6b7280', fontSize: '0.875rem', marginTop: 0 }}>
                    รองรับไฟล์ FG New และ SEMI New จาก SAP (sheet: "fert new" หรือ "bomfile-5100-HALB")
                </p>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
                    <input
                        ref={fileRef}
                        type="file"
                        accept=".xlsx,.xls"
                        onChange={e => { setFile(e.target.files[0]); setResult(null); setError(''); }}
                        style={{ flex: 1, minWidth: 200 }}
                    />
                    <button
                        onClick={handleUpload}
                        disabled={uploading || !file}
                        style={uploadBtnStyle(uploading || !file)}
                    >
                        {uploading ? '⏳ กำลังนำเข้า...' : '📥 นำเข้า'}
                    </button>
                </div>
                {file && (
                    <p style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#374151' }}>
                        ไฟล์ที่เลือก: <strong>{file.name}</strong> ({(file.size / 1024).toFixed(1)} KB)
                    </p>
                )}
                {error && <div style={errorStyle}>{error}</div>}
            </div>

            {/* Result */}
            {result && (
                <div style={{ ...cardStyle, borderLeft: '4px solid #16a34a' }}>
                    <h3 style={{ marginTop: 0, color: '#16a34a' }}>✅ นำเข้าสำเร็จ — {result.fileType}</h3>
                    <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
                        <Stat label="เพิ่มใหม่" value={result.rowsAdded} color="#16a34a" />
                        <Stat label="อัปเดต" value={result.rowsUpdated} color="#2563eb" />
                        <Stat label="ข้าม" value={result.rowsSkipped} color="#6b7280" />
                        <Stat label="ข้อผิดพลาด" value={result.rowsError} color="#dc2626" />
                    </div>
                    {result.errors && result.errors.length > 0 && (
                        <details>
                            <summary style={{ cursor: 'pointer', color: '#dc2626', fontWeight: 600 }}>
                                ⚠️ ข้อผิดพลาด ({result.errors.length} รายการ)
                            </summary>
                            <ul style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#dc2626' }}>
                                {result.errors.map((e, i) => <li key={i}>{e}</li>)}
                            </ul>
                        </details>
                    )}
                </div>
            )}

            {/* Sync Products card */}
            <div style={cardStyle}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.75rem' }}>
                    <div>
                        <h3 style={{ margin: 0 }}>🔄 Sync Products จาก BOM</h3>
                        <p style={{ color: '#6b7280', fontSize: '0.875rem', marginTop: '0.25rem', marginBottom: 0 }}>
                            สร้าง Product record สำหรับ FG Code ทุกรายการใน BOM ที่ยังไม่มีในตาราง Products
                        </p>
                    </div>
                    <button onClick={handleSync} disabled={syncing} style={syncBtnStyle(syncing)}>
                        {syncing ? '⏳ กำลัง Sync...' : '🔄 Sync Products'}
                    </button>
                </div>
                {syncResult && (
                    <div style={{ marginTop: '1rem', display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
                        <Stat label="สร้างใหม่" value={syncResult.created} color="#16a34a" />
                        <Stat label="มีอยู่แล้ว (ข้าม)" value={syncResult.skipped} color="#6b7280" />
                        <Stat label="FG Code ทั้งหมด" value={syncResult.total} color="#2563eb" />
                    </div>
                )}
                {syncError && <div style={{ ...errorStyle, marginTop: '1rem' }}>{syncError}</div>}
            </div>

            {/* Import log table */}
            <div style={cardStyle}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <h3 style={{ margin: 0 }}>📋 ประวัติการนำเข้า</h3>
                    <button onClick={loadLogs} style={refreshBtnStyle} disabled={loadingLogs}>🔄 รีเฟรช</button>
                </div>
                {loadingLogs ? (
                    <p>กำลังโหลด...</p>
                ) : logs.length === 0 ? (
                    <p style={{ color: '#6b7280' }}>ยังไม่มีประวัติการนำเข้า</p>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={tableStyle}>
                            <thead>
                                <tr style={{ background: '#f3f4f6' }}>
                                    <th style={thStyle}>วันที่นำเข้า</th>
                                    <th style={thStyle}>ชื่อไฟล์</th>
                                    <th style={thStyle}>ประเภท</th>
                                    <th style={thStyle}>โดย</th>
                                    <th style={thStyle}>เพิ่ม</th>
                                    <th style={thStyle}>อัปเดต</th>
                                    <th style={thStyle}>ข้าม</th>
                                    <th style={thStyle}>Error</th>
                                    <th style={thStyle}>เวลา (ms)</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logs.map((log, i) => (
                                    <tr key={log.id || i} style={{ borderBottom: '1px solid #e5e7eb' }}>
                                        <td style={tdStyle}>
                                            {log.importedAt
                                                ? new Date(log.importedAt).toLocaleString('th-TH')
                                                : '-'}
                                        </td>
                                        <td style={tdStyle}>{log.filename || '-'}</td>
                                        <td style={tdStyle}>
                                            <span style={typeBadgeStyle(log.fileType)}>{log.fileType || '-'}</span>
                                        </td>
                                        <td style={tdStyle}>
                                            {log.importedBy?.username || log.importedBy || '-'}
                                        </td>
                                        <td style={{ ...tdStyle, color: '#16a34a', fontWeight: 600 }}>{log.rowsAdded ?? 0}</td>
                                        <td style={{ ...tdStyle, color: '#2563eb', fontWeight: 600 }}>{log.rowsUpdated ?? 0}</td>
                                        <td style={{ ...tdStyle, color: '#6b7280' }}>{log.rowsSkipped ?? 0}</td>
                                        <td style={{ ...tdStyle, color: log.rowsError > 0 ? '#dc2626' : '#6b7280', fontWeight: log.rowsError > 0 ? 600 : 400 }}>
                                            {log.rowsError ?? 0}
                                        </td>
                                        <td style={tdStyle}>{log.durationMs ?? '-'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

const Stat = ({ label, value, color }) => (
    <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: '2rem', fontWeight: 700, color }}>{value ?? 0}</div>
        <div style={{ fontSize: '0.85rem', color: '#6b7280' }}>{label}</div>
    </div>
);

const typeBadgeStyle = (type) => ({
    padding: '2px 8px',
    borderRadius: 12,
    fontSize: '0.78rem',
    fontWeight: 600,
    background: type === 'FG' ? '#dbeafe' : type === 'SEMI' ? '#d1fae5' : '#f3f4f6',
    color: type === 'FG' ? '#1d4ed8' : type === 'SEMI' ? '#065f46' : '#374151',
});

const backBtnStyle = {
    background: 'none', border: '1px solid #d1d5db', borderRadius: 6,
    padding: '6px 14px', cursor: 'pointer', marginBottom: '1rem',
};
const cardStyle = {
    background: '#fff', borderRadius: 8,
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)', padding: '1.25rem', marginBottom: '1.25rem',
};
const uploadBtnStyle = (disabled) => ({
    background: disabled ? '#9ca3af' : '#2563eb', color: '#fff', border: 'none',
    borderRadius: 6, padding: '8px 20px', cursor: disabled ? 'not-allowed' : 'pointer', fontWeight: 600,
});
const syncBtnStyle = (disabled) => ({
    background: disabled ? '#9ca3af' : '#7c3aed', color: '#fff', border: 'none',
    borderRadius: 6, padding: '8px 20px', cursor: disabled ? 'not-allowed' : 'pointer', fontWeight: 600,
    whiteSpace: 'nowrap',
});
const refreshBtnStyle = {
    background: 'none', border: '1px solid #d1d5db', borderRadius: 6,
    padding: '4px 12px', cursor: 'pointer', fontSize: '0.85rem',
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

export default BomUploadPage;
