import React, { useState, useEffect } from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: 'http://localhost:8080/api' });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

const STATUS_OPTIONS = [
    { value: 'RUNNING',        label: 'กำลังผลิต',      color: '#22c55e', bg: '#f0fdf4' },
    { value: 'IDLE',           label: 'รอผลิต',          color: '#f59e0b', bg: '#fffbeb' },
    { value: 'SETUP',          label: 'ตั้งค่า/เปลี่ยน',  color: '#3b82f6', bg: '#eff6ff' },
    { value: 'PLANNED_STOP',   label: 'หยุดแผน (PM)',    color: '#8b5cf6', bg: '#f5f3ff' },
    { value: 'UNPLANNED_STOP', label: 'หยุดฉุกเฉิน',     color: '#ef4444', bg: '#fef2f2' },
];

const statusStyle = (value) => {
    const s = STATUS_OPTIONS.find(o => o.value === value);
    return s ? { color: s.color, backgroundColor: s.bg, border: `1px solid ${s.color}`,
                 borderRadius: 6, padding: '2px 10px', fontWeight: 600, display: 'inline-block' } : {};
};

const formatDuration = (minutes) => {
    if (!minutes) return '-';
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h}ชม. ${m}น.` : `${m}น.`;
};

const MachineStatusPanel = ({ machines = [] }) => {
    const [currentStatuses, setCurrentStatuses] = useState({});
    const [showModal, setShowModal] = useState(false);
    const [selectedMachine, setSelectedMachine] = useState(null);
    const [newStatus, setNewStatus] = useState('RUNNING');
    const [reason, setReason] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    const fetchAllStatuses = async () => {
        const results = {};
        await Promise.all(machines.map(async (m) => {
            try {
                const res = await api.get(`/machine-status/${m.id}/current`);
                results[m.id] = res.data;
            } catch {
                results[m.id] = null;
            }
        }));
        setCurrentStatuses(results);
    };

    useEffect(() => {
        if (machines.length > 0) {
            fetchAllStatuses();
            const interval = setInterval(fetchAllStatuses, 60000);
            return () => clearInterval(interval);
        }
    }, [machines]);

    const openModal = (machine) => {
        setSelectedMachine(machine);
        const cur = currentStatuses[machine.id];
        setNewStatus(cur?.status || 'RUNNING');
        setReason('');
        setError('');
        setShowModal(true);
    };

    const handleSave = async () => {
        if (!selectedMachine) return;
        setSaving(true);
        setError('');
        try {
            await api.post('/machine-status', {
                machineId: selectedMachine.id,
                status: newStatus,
                reason: reason || null,
            });
            setShowModal(false);
            await fetchAllStatuses();
        } catch (err) {
            setError('บันทึกไม่สำเร็จ: ' + (err.response?.data || err.message));
        } finally {
            setSaving(false);
        }
    };

    if (machines.length === 0) return null;

    return (
        <div style={{ marginBottom: 24 }}>
            <h3 style={{ marginBottom: 12, color: '#1e40af' }}>สถานะเครื่องจักร</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 }}>
                {machines.map(machine => {
                    const cur = currentStatuses[machine.id];
                    const statusOpt = STATUS_OPTIONS.find(o => o.value === cur?.status);
                    return (
                        <div key={machine.id} style={{ border: '1px solid #e5e7eb', borderRadius: 8,
                            padding: 14, background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.07)' }}>
                            <div style={{ fontWeight: 600, marginBottom: 6 }}>{machine.machineName}</div>
                            {cur ? (
                                <>
                                    <div style={statusStyle(cur.status)}>{statusOpt?.label || cur.status}</div>
                                    <div style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>
                                        {formatDuration(cur.durationMinutes)} · {cur.source === 'IOT' ? '🔗 Auto' : '👤 Manual'}
                                    </div>
                                    {cur.reason && <div style={{ fontSize: 12, color: '#374151', marginTop: 2 }}>เหตุ: {cur.reason}</div>}
                                </>
                            ) : (
                                <div style={{ color: '#9ca3af', fontSize: 13 }}>ยังไม่มีข้อมูล</div>
                            )}
                            <button onClick={() => openModal(machine)}
                                style={{ marginTop: 10, width: '100%', padding: '6px 0',
                                    background: '#1d4ed8', color: '#fff', border: 'none',
                                    borderRadius: 6, cursor: 'pointer', fontSize: 13 }}>
                                เปลี่ยนสถานะ
                            </button>
                        </div>
                    );
                })}
            </div>

            {showModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', borderRadius: 10, padding: 24, width: 360, maxWidth: '90vw' }}>
                        <h4 style={{ margin: '0 0 16px' }}>เปลี่ยนสถานะ: {selectedMachine?.machineName}</h4>
                        <div style={{ marginBottom: 12 }}>
                            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>สถานะใหม่</label>
                            <select value={newStatus} onChange={e => setNewStatus(e.target.value)}
                                style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db' }}>
                                {STATUS_OPTIONS.map(o => (
                                    <option key={o.value} value={o.value}>{o.label}</option>
                                ))}
                            </select>
                        </div>
                        <div style={{ marginBottom: 16 }}>
                            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>เหตุผล (ไม่บังคับ)</label>
                            <input value={reason} onChange={e => setReason(e.target.value)}
                                placeholder="เช่น เปลี่ยน mold, รอวัตถุดิบ..."
                                style={{ width: '100%', padding: '8px 10px', borderRadius: 6,
                                    border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                        </div>
                        {error && <div style={{ color: '#ef4444', fontSize: 12, marginBottom: 8 }}>{error}</div>}
                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                            <button onClick={() => setShowModal(false)}
                                style={{ padding: '8px 18px', borderRadius: 6, border: '1px solid #d1d5db',
                                    cursor: 'pointer', background: '#fff' }}>
                                ยกเลิก
                            </button>
                            <button onClick={handleSave} disabled={saving}
                                style={{ padding: '8px 18px', borderRadius: 6, border: 'none',
                                    background: saving ? '#93c5fd' : '#1d4ed8', color: '#fff',
                                    cursor: saving ? 'default' : 'pointer' }}>
                                {saving ? 'กำลังบันทึก...' : 'บันทึก'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MachineStatusPanel;
