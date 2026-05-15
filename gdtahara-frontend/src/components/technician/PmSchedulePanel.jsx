import React, { useState, useEffect } from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: 'http://localhost:8080/api' });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

const STATUS_BADGE = {
    UPCOMING: { label: 'กำหนดการปกติ', color: '#16a34a', bg: '#f0fdf4' },
    DUE:      { label: 'ใกล้ถึงกำหนด', color: '#d97706', bg: '#fffbeb' },
    OVERDUE:  { label: '⚠ เกินกำหนด',  color: '#dc2626', bg: '#fef2f2' },
    DONE:     { label: 'เสร็จแล้ว',     color: '#6b7280', bg: '#f9fafb' },
};

const badge = (status) => {
    const s = STATUS_BADGE[status] || STATUS_BADGE.UPCOMING;
    return { color: s.color, background: s.bg, border: `1px solid ${s.color}`,
             borderRadius: 5, padding: '2px 8px', fontSize: 12, fontWeight: 600, display: 'inline-block' };
};

const PmSchedulePanel = () => {
    const [schedules, setSchedules] = useState([]);
    const [machines, setMachines] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [confirmDone, setConfirmDone] = useState(null);
    const [form, setForm] = useState({ machineId: '', taskName: '', description: '', intervalDays: 30, nextDueDate: '' });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    const fetchData = async () => {
        setLoading(true);
        try {
            const [pmRes, machineRes] = await Promise.all([
                api.get('/pm-schedules'),
                api.get('/master-data/machines'),
            ]);
            setSchedules(pmRes.data);
            setMachines(machineRes.data);
        } catch (e) {
            setError('โหลดข้อมูลไม่สำเร็จ');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, []);

    const handleMarkDone = async (id) => {
        try {
            await api.post(`/pm-schedules/${id}/done`);
            setConfirmDone(null);
            fetchData();
        } catch { setError('บันทึกไม่สำเร็จ'); }
    };

    const handleCreate = async () => {
        if (!form.machineId || !form.taskName || !form.nextDueDate) {
            setError('กรุณากรอกข้อมูลให้ครบ'); return;
        }
        setSaving(true); setError('');
        try {
            await api.post('/pm-schedules', {
                machine: { id: parseInt(form.machineId) },
                taskName: form.taskName,
                description: form.description,
                intervalDays: parseInt(form.intervalDays),
                nextDueDate: form.nextDueDate,
            });
            setShowModal(false);
            setForm({ machineId: '', taskName: '', description: '', intervalDays: 30, nextDueDate: '' });
            fetchData();
        } catch { setError('บันทึกไม่สำเร็จ');
        } finally { setSaving(false); }
    };

    const urgent = schedules.filter(s => s.status === 'OVERDUE' || s.status === 'DUE');
    const normal = schedules.filter(s => s.status !== 'OVERDUE' && s.status !== 'DUE');

    return (
        <div style={{ marginBottom: 24 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <h3 style={{ margin: 0, color: '#1e40af' }}>
                    แผน PM {urgent.length > 0 && <span style={{ background: '#ef4444', color: '#fff',
                        borderRadius: 10, padding: '1px 8px', fontSize: 12, marginLeft: 8 }}>{urgent.length}</span>}
                </h3>
                <button onClick={() => setShowModal(true)}
                    style={{ padding: '7px 16px', background: '#1d4ed8', color: '#fff',
                        border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13 }}>
                    + เพิ่มแผน PM
                </button>
            </div>

            {error && <div style={{ color: '#ef4444', fontSize: 13, marginBottom: 8 }}>{error}</div>}
            {loading ? <div style={{ color: '#6b7280' }}>กำลังโหลด...</div> : (
                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                        <thead>
                            <tr style={{ background: '#f1f5f9' }}>
                                {['เครื่องจักร','งาน PM','ทุกกี่วัน','ทำล่าสุด','ครบกำหนด','สถานะ',''].map(h => (
                                    <th key={h} style={{ padding: '8px 10px', textAlign: 'left',
                                        borderBottom: '1px solid #e2e8f0' }}>{h}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {[...urgent, ...normal].map(pm => (
                                <tr key={pm.id} style={{ borderBottom: '1px solid #f1f5f9',
                                    background: pm.status === 'OVERDUE' ? '#fff5f5' : undefined }}>
                                    <td style={{ padding: '8px 10px', fontWeight: 500 }}>{pm.machine?.machineName}</td>
                                    <td style={{ padding: '8px 10px' }}>{pm.taskName}</td>
                                    <td style={{ padding: '8px 10px', textAlign: 'center' }}>{pm.intervalDays}</td>
                                    <td style={{ padding: '8px 10px', color: '#6b7280' }}>{pm.lastDoneDate || '-'}</td>
                                    <td style={{ padding: '8px 10px' }}>{pm.nextDueDate}</td>
                                    <td style={{ padding: '8px 10px' }}><span style={badge(pm.status)}>{STATUS_BADGE[pm.status]?.label}</span></td>
                                    <td style={{ padding: '8px 10px' }}>
                                        {pm.status !== 'DONE' && (
                                            <button onClick={() => setConfirmDone(pm)}
                                                style={{ padding: '4px 12px', background: '#16a34a', color: '#fff',
                                                    border: 'none', borderRadius: 5, cursor: 'pointer', fontSize: 12 }}>
                                                ✓ ทำเสร็จ
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {schedules.length === 0 && (
                                <tr><td colSpan={7} style={{ padding: 20, textAlign: 'center', color: '#9ca3af' }}>
                                    ยังไม่มีแผน PM — กด "+ เพิ่มแผน PM" เพื่อเริ่มต้น
                                </td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Modal: Confirm Done */}
            {confirmDone && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', borderRadius: 10, padding: 24, width: 340 }}>
                        <h4 style={{ margin: '0 0 12px' }}>ยืนยันการทำ PM</h4>
                        <p style={{ margin: '0 0 16px', fontSize: 14 }}>
                            บันทึกว่าทำ <b>{confirmDone.taskName}</b> บนเครื่อง <b>{confirmDone.machine?.machineName}</b> เสร็จแล้ว?<br/>
                            <span style={{ color: '#6b7280', fontSize: 12 }}>
                                ครั้งถัดไปจะตั้งเป็น {confirmDone.intervalDays} วันหลังจากนี้
                            </span>
                        </p>
                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                            <button onClick={() => setConfirmDone(null)}
                                style={{ padding: '7px 16px', border: '1px solid #d1d5db', borderRadius: 6, cursor: 'pointer', background: '#fff' }}>
                                ยกเลิก
                            </button>
                            <button onClick={() => handleMarkDone(confirmDone.id)}
                                style={{ padding: '7px 16px', background: '#16a34a', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
                                ยืนยัน
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal: Create PM */}
            {showModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', borderRadius: 10, padding: 24, width: 400, maxWidth: '92vw' }}>
                        <h4 style={{ margin: '0 0 16px' }}>เพิ่มแผน PM ใหม่</h4>
                        {[
                            { label: 'เครื่องจักร *', type: 'select', key: 'machineId' },
                            { label: 'ชื่องาน PM *', type: 'text', key: 'taskName', placeholder: 'เช่น เปลี่ยนน้ำมัน, ล้างแม่พิมพ์' },
                            { label: 'รายละเอียด', type: 'text', key: 'description', placeholder: 'ไม่บังคับ' },
                            { label: 'ทำซ้ำทุก (วัน) *', type: 'number', key: 'intervalDays' },
                            { label: 'ครั้งถัดไป (วันที่) *', type: 'date', key: 'nextDueDate' },
                        ].map(({ label, type, key, placeholder }) => (
                            <div key={key} style={{ marginBottom: 12 }}>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>{label}</label>
                                {type === 'select' ? (
                                    <select value={form[key]} onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))}
                                        style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db' }}>
                                        <option value="">-- เลือกเครื่อง --</option>
                                        {machines.map(m => <option key={m.id} value={m.id}>{m.machineName}</option>)}
                                    </select>
                                ) : (
                                    <input type={type} value={form[key]} placeholder={placeholder}
                                        onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))}
                                        style={{ width: '100%', padding: '8px 10px', borderRadius: 6,
                                            border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                                )}
                            </div>
                        ))}
                        {error && <div style={{ color: '#ef4444', fontSize: 12, marginBottom: 8 }}>{error}</div>}
                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                            <button onClick={() => { setShowModal(false); setError(''); }}
                                style={{ padding: '8px 16px', border: '1px solid #d1d5db', borderRadius: 6, cursor: 'pointer', background: '#fff' }}>
                                ยกเลิก
                            </button>
                            <button onClick={handleCreate} disabled={saving}
                                style={{ padding: '8px 16px', background: saving ? '#93c5fd' : '#1d4ed8',
                                    color: '#fff', border: 'none', borderRadius: 6, cursor: saving ? 'default' : 'pointer' }}>
                                {saving ? 'กำลังบันทึก...' : 'บันทึก'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PmSchedulePanel;
