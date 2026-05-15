import React, { useState, useEffect } from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: 'http://localhost:8080/api' });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

const EMPTY_FORM = {
    recipeCode: '', recipeName: '', version: '1.0',
    productId: '', machineId: '',
    targetCycleTimeSec: '', targetTempZone1: '', targetTempZone2: '',
    targetTempHead: '', targetBlowPressure: '', notes: '',
};

const RecipePanel = () => {
    const [recipes, setRecipes] = useState([]);
    const [machines, setMachines] = useState([]);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showModal, setShowModal] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form, setForm] = useState(EMPTY_FORM);
    const [saving, setSaving] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState(null);

    const fetchAll = async () => {
        setLoading(true);
        try {
            const [rRes, mRes, pRes] = await Promise.all([
                api.get('/recipes'),
                api.get('/master-data/machines'),
                api.get('/master-data/products'),
            ]);
            setRecipes(rRes.data);
            setMachines(mRes.data);
            setProducts(pRes.data);
            setError('');
        } catch (e) {
            setError('โหลดข้อมูลไม่สำเร็จ');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchAll(); }, []);

    const openCreate = () => {
        setEditingId(null);
        setForm(EMPTY_FORM);
        setError('');
        setShowModal(true);
    };

    const openEdit = (r) => {
        setEditingId(r.id);
        setForm({
            recipeCode: r.recipeCode || '',
            recipeName: r.recipeName || '',
            version: r.version || '1.0',
            productId: r.productId || '',
            machineId: r.machineId || '',
            targetCycleTimeSec: r.targetCycleTimeSec ?? '',
            targetTempZone1: r.targetTempZone1 ?? '',
            targetTempZone2: r.targetTempZone2 ?? '',
            targetTempHead: r.targetTempHead ?? '',
            targetBlowPressure: r.targetBlowPressure ?? '',
            notes: r.notes || '',
        });
        setError('');
        setShowModal(true);
    };

    const handleSave = async () => {
        if (!form.productId || !form.machineId || !form.targetCycleTimeSec) {
            setError('กรุณาเลือกสินค้า เครื่องจักร และกรอก Cycle Time'); return;
        }
        setSaving(true); setError('');
        const payload = {
            ...form,
            productId: parseInt(form.productId),
            machineId: parseInt(form.machineId),
            targetCycleTimeSec: parseFloat(form.targetCycleTimeSec),
            targetTempZone1: form.targetTempZone1 !== '' ? parseFloat(form.targetTempZone1) : null,
            targetTempZone2: form.targetTempZone2 !== '' ? parseFloat(form.targetTempZone2) : null,
            targetTempHead: form.targetTempHead !== '' ? parseFloat(form.targetTempHead) : null,
            targetBlowPressure: form.targetBlowPressure !== '' ? parseFloat(form.targetBlowPressure) : null,
            isActive: true,
        };
        try {
            if (editingId) {
                await api.put(`/recipes/${editingId}`, payload);
            } else {
                await api.post('/recipes', payload);
            }
            setShowModal(false);
            fetchAll();
        } catch (e) {
            setError('บันทึกไม่สำเร็จ: ' + (e.response?.data || e.message));
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id) => {
        try {
            await api.delete(`/recipes/${id}`);
            setConfirmDelete(null);
            fetchAll();
        } catch { setError('ลบไม่สำเร็จ'); }
    };

    const f = (v) => v != null && v !== '' ? v : '-';

    return (
        <div style={{ marginBottom: 24 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <h3 style={{ margin: 0, color: '#1e40af' }}>Recipe / พารามิเตอร์การผลิต</h3>
                <button onClick={openCreate}
                    style={{ padding: '7px 16px', background: '#1d4ed8', color: '#fff',
                        border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13 }}>
                    + เพิ่ม Recipe
                </button>
            </div>

            {error && <div style={{ color: '#ef4444', fontSize: 13, marginBottom: 8 }}>{error}</div>}
            {loading ? <div style={{ color: '#6b7280' }}>กำลังโหลด...</div> : (
                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                        <thead>
                            <tr style={{ background: '#f1f5f9' }}>
                                {['เครื่อง','สินค้า','Recipe Code','Version',
                                  'Cycle Time (s)','Temp Z1 (°C)','Temp Z2 (°C)','Temp Head (°C)','Blow Pressure','หมายเหตุ',''].map(h => (
                                    <th key={h} style={{ padding: '8px 10px', textAlign: 'left',
                                        borderBottom: '1px solid #e2e8f0', whiteSpace: 'nowrap' }}>{h}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {recipes.map(r => (
                                <tr key={r.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                                    <td style={{ padding: '8px 10px', fontWeight: 500 }}>{f(r.machineName)}</td>
                                    <td style={{ padding: '8px 10px' }}>{f(r.productName)}</td>
                                    <td style={{ padding: '8px 10px', fontFamily: 'monospace' }}>{f(r.recipeCode)}</td>
                                    <td style={{ padding: '8px 10px', textAlign: 'center' }}>{f(r.version)}</td>
                                    <td style={{ padding: '8px 10px', textAlign: 'center', fontWeight: 600, color: '#1d4ed8' }}>{f(r.targetCycleTimeSec)}</td>
                                    <td style={{ padding: '8px 10px', textAlign: 'center' }}>{f(r.targetTempZone1)}</td>
                                    <td style={{ padding: '8px 10px', textAlign: 'center' }}>{f(r.targetTempZone2)}</td>
                                    <td style={{ padding: '8px 10px', textAlign: 'center' }}>{f(r.targetTempHead)}</td>
                                    <td style={{ padding: '8px 10px', textAlign: 'center' }}>{f(r.targetBlowPressure)}</td>
                                    <td style={{ padding: '8px 10px', color: '#6b7280', maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis' }}>{r.notes || '-'}</td>
                                    <td style={{ padding: '8px 10px', whiteSpace: 'nowrap' }}>
                                        <button onClick={() => openEdit(r)}
                                            style={{ padding: '3px 10px', background: '#f59e0b', color: '#fff',
                                                border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 12, marginRight: 4 }}>
                                            แก้ไข
                                        </button>
                                        <button onClick={() => setConfirmDelete(r)}
                                            style={{ padding: '3px 10px', background: '#ef4444', color: '#fff',
                                                border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 12 }}>
                                            ลบ
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {recipes.length === 0 && (
                                <tr><td colSpan={11} style={{ padding: 20, textAlign: 'center', color: '#9ca3af' }}>
                                    ยังไม่มี Recipe — กด "+ เพิ่ม Recipe" เพื่อเริ่มต้น
                                </td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Modal: Create/Edit */}
            {showModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', borderRadius: 10, padding: 24, width: 520,
                        maxWidth: '95vw', maxHeight: '90vh', overflowY: 'auto' }}>
                        <h4 style={{ margin: '0 0 16px' }}>{editingId ? 'แก้ไข Recipe' : 'เพิ่ม Recipe ใหม่'}</h4>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                            {/* Machine */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>เครื่องจักร *</label>
                                <select value={form.machineId} onChange={e => setForm(f => ({ ...f, machineId: e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db' }}>
                                    <option value="">-- เลือก --</option>
                                    {machines.map(m => <option key={m.id} value={m.id}>{m.machineName}</option>)}
                                </select>
                            </div>
                            {/* Product */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>สินค้า *</label>
                                <select value={form.productId} onChange={e => setForm(f => ({ ...f, productId: e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db' }}>
                                    <option value="">-- เลือก --</option>
                                    {products.map(p => <option key={p.id} value={p.id}>{p.productName}</option>)}
                                </select>
                            </div>
                            {/* Recipe Code */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>Recipe Code</label>
                                <input value={form.recipeCode} onChange={e => setForm(f => ({ ...f, recipeCode: e.target.value }))}
                                    placeholder="เช่น RC-RBL101-AJI130"
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                            {/* Recipe Name */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>ชื่อ Recipe</label>
                                <input value={form.recipeName} onChange={e => setForm(f => ({ ...f, recipeName: e.target.value }))}
                                    placeholder="เช่น Aji130 Standard"
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                            {/* Cycle Time */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>Cycle Time (วินาที) *</label>
                                <input type="number" step="0.1" min="0" value={form.targetCycleTimeSec}
                                    onChange={e => setForm(f => ({ ...f, targetCycleTimeSec: e.target.value }))}
                                    placeholder="เช่น 6.5"
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                            {/* Version */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>Version</label>
                                <input value={form.version} onChange={e => setForm(f => ({ ...f, version: e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                            {/* Temp Zone 1 */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>อุณหภูมิ Zone 1 (°C)</label>
                                <input type="number" step="0.1" value={form.targetTempZone1}
                                    onChange={e => setForm(f => ({ ...f, targetTempZone1: e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                            {/* Temp Zone 2 */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>อุณหภูมิ Zone 2 (°C)</label>
                                <input type="number" step="0.1" value={form.targetTempZone2}
                                    onChange={e => setForm(f => ({ ...f, targetTempZone2: e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                            {/* Temp Head */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>อุณหภูมิ Head (°C)</label>
                                <input type="number" step="0.1" value={form.targetTempHead}
                                    onChange={e => setForm(f => ({ ...f, targetTempHead: e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                            {/* Blow Pressure */}
                            <div>
                                <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>Blow Pressure (bar)</label>
                                <input type="number" step="0.01" value={form.targetBlowPressure}
                                    onChange={e => setForm(f => ({ ...f, targetBlowPressure: e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', boxSizing: 'border-box' }} />
                            </div>
                        </div>

                        {/* Notes */}
                        <div style={{ marginTop: 12 }}>
                            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>หมายเหตุ</label>
                            <textarea value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))}
                                rows={2} style={{ width: '100%', padding: '8px 10px', borderRadius: 6,
                                    border: '1px solid #d1d5db', boxSizing: 'border-box', resize: 'vertical' }} />
                        </div>

                        {error && <div style={{ color: '#ef4444', fontSize: 12, marginTop: 8 }}>{error}</div>}

                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
                            <button onClick={() => { setShowModal(false); setError(''); }}
                                style={{ padding: '8px 18px', border: '1px solid #d1d5db', borderRadius: 6,
                                    cursor: 'pointer', background: '#fff' }}>ยกเลิก</button>
                            <button onClick={handleSave} disabled={saving}
                                style={{ padding: '8px 18px', background: saving ? '#93c5fd' : '#1d4ed8',
                                    color: '#fff', border: 'none', borderRadius: 6,
                                    cursor: saving ? 'default' : 'pointer' }}>
                                {saving ? 'กำลังบันทึก...' : 'บันทึก'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal: Confirm Delete */}
            {confirmDelete && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', borderRadius: 10, padding: 24, width: 360 }}>
                        <h4 style={{ margin: '0 0 12px' }}>ยืนยันการลบ</h4>
                        <p style={{ margin: '0 0 16px', fontSize: 14 }}>
                            ลบ Recipe <b>{confirmDelete.recipeCode || confirmDelete.recipeName}</b><br/>
                            <span style={{ color: '#6b7280', fontSize: 12 }}>
                                {confirmDelete.machineName} / {confirmDelete.productName}
                            </span>
                        </p>
                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                            <button onClick={() => setConfirmDelete(null)}
                                style={{ padding: '7px 16px', border: '1px solid #d1d5db', borderRadius: 6,
                                    cursor: 'pointer', background: '#fff' }}>ยกเลิก</button>
                            <button onClick={() => handleDelete(confirmDelete.id)}
                                style={{ padding: '7px 16px', background: '#ef4444', color: '#fff',
                                    border: 'none', borderRadius: 6, cursor: 'pointer' }}>ลบ</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default RecipePanel;
