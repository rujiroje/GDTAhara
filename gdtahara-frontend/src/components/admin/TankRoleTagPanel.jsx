import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';

const API_URL = `${import.meta.env.BASE_URL}api`;
const api = axios.create({ baseURL: API_URL });
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

const ROLES = [
  { value: 'RESIN_100', label: 'RESIN_100 — ถัง A (Virgin 100%)',  color: '#1565c0', bg: '#e3f2fd' },
  { value: 'EVOH',      label: 'EVOH — ถัง B',                     color: '#2e7d32', bg: '#e8f5e9' },
  { value: 'ADMER',     label: 'ADMER — ถัง C (Adhesive)',          color: '#6a1b9a', bg: '#f3e5f5' },
  { value: 'BLEND',     label: 'BLEND — ถัง D (Regrind/บดคืน)',    color: '#e65100', bg: '#fff3e0' },
];

const roleBadge = (role) => {
  const r = ROLES.find(x => x.value === role);
  if (!r) return <span style={{ color: '#999', fontSize: 12 }}>—</span>;
  return (
    <span style={{
      background: r.bg, color: r.color, border: `1px solid ${r.color}44`,
      borderRadius: 12, padding: '1px 10px', fontWeight: 700, fontSize: 12, whiteSpace: 'nowrap'
    }}>{r.value}</span>
  );
};

const FILTERS = [
  { key: 'all',      label: 'ทั้งหมด' },
  { key: 'untagged', label: 'ยังไม่แท็ก' },
  ...ROLES.map(r => ({ key: r.value, label: r.value })),
];

const TankRoleTagPanel = () => {
  const [materials, setMaterials] = useState([]);
  const [filter, setFilter]       = useState('untagged');
  const [search, setSearch]       = useState('');
  const [editing, setEditing]     = useState({});   // { [id]: newRole | null | '' }
  const [saving, setSaving]       = useState({});   // { [id]: bool }
  const [saved, setSaved]         = useState({});   // { [id]: bool } flash
  const [error, setError]         = useState('');
  const [loading, setLoading]     = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    api.get('/admin/materials')
      .then(r => setMaterials(r.data))
      .catch(() => setError('โหลดข้อมูล materials ไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = materials.filter(m => {
    const matchFilter =
      filter === 'all'      ? true :
      filter === 'untagged' ? !m.tankRole :
      m.tankRole === filter;
    const q = search.toLowerCase();
    const matchSearch = !q ||
      m.materialCode?.toLowerCase().includes(q) ||
      m.materialName?.toLowerCase().includes(q);
    return matchFilter && matchSearch;
  });

  const counts = {
    all:      materials.length,
    untagged: materials.filter(m => !m.tankRole).length,
    RESIN_100: materials.filter(m => m.tankRole === 'RESIN_100').length,
    EVOH:      materials.filter(m => m.tankRole === 'EVOH').length,
    ADMER:     materials.filter(m => m.tankRole === 'ADMER').length,
    BLEND:     materials.filter(m => m.tankRole === 'BLEND').length,
  };

  const handleChange = (id, val) => {
    setEditing(prev => ({ ...prev, [id]: val }));
  };

  const handleSave = async (mat) => {
    const newRole = editing[mat.id] !== undefined ? editing[mat.id] : mat.tankRole;
    setSaving(prev => ({ ...prev, [mat.id]: true }));
    try {
      await api.patch(`/admin/materials/${mat.id}/tank-role`, { tankRole: newRole || null });
      setMaterials(prev => prev.map(m => m.id === mat.id ? { ...m, tankRole: newRole || null } : m));
      setEditing(prev => { const n = { ...prev }; delete n[mat.id]; return n; });
      setSaved(prev => ({ ...prev, [mat.id]: true }));
      setTimeout(() => setSaved(prev => { const n = { ...prev }; delete n[mat.id]; return n; }), 1500);
    } catch {
      alert('บันทึกไม่สำเร็จ');
    }
    setSaving(prev => ({ ...prev, [mat.id]: false }));
  };

  const isDirty = (mat) => editing[mat.id] !== undefined && editing[mat.id] !== (mat.tankRole || '');

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h3 style={{ margin: 0 }}>แท็ก Tank Role วัตถุดิบ (C-04)</h3>
        <button className="add-button" onClick={load} disabled={loading}>
          {loading ? 'กำลังโหลด...' : 'รีเฟรช'}
        </button>
      </div>

      {error && <p className="error-message">{error}</p>}

      {/* Filter tabs */}
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 10 }}>
        {FILTERS.map(f => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            style={{
              padding: '4px 12px', borderRadius: 16, fontSize: 12, cursor: 'pointer',
              border: filter === f.key ? '2px solid #1976d2' : '1px solid #ccc',
              background: filter === f.key ? '#e3f2fd' : '#fff',
              fontWeight: filter === f.key ? 700 : 400,
              color: filter === f.key ? '#1565c0' : '#333'
            }}
          >
            {f.label} ({counts[f.key] ?? 0})
          </button>
        ))}
      </div>

      {/* Search */}
      <input
        type="text"
        placeholder="ค้นหารหัส / ชื่อวัตถุดิบ..."
        value={search}
        onChange={e => setSearch(e.target.value)}
        className="form-input"
        style={{ marginBottom: 10, maxWidth: 320 }}
      />

      <p style={{ fontSize: 12, color: '#666', marginBottom: 8 }}>
        แสดง {filtered.length} รายการ
      </p>

      <div className="data-table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 120 }}>รหัส</th>
              <th>ชื่อวัตถุดิบ</th>
              <th style={{ width: 80 }}>Type</th>
              <th style={{ width: 130 }}>Tank Role ปัจจุบัน</th>
              <th style={{ width: 220 }}>แท็กใหม่</th>
              <th style={{ width: 80 }}>บันทึก</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={6} style={{ textAlign: 'center', color: '#999', padding: 24 }}>
                {loading ? 'กำลังโหลด...' : 'ไม่มีข้อมูล'}
              </td></tr>
            ) : filtered.map(mat => {
              const currentEdit = editing[mat.id];
              const selectVal = currentEdit !== undefined ? currentEdit : (mat.tankRole || '');
              return (
                <tr key={mat.id} style={{ background: saved[mat.id] ? '#f1f8e9' : undefined }}>
                  <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{mat.materialCode}</td>
                  <td style={{ fontSize: 13 }}>{mat.materialName}</td>
                  <td style={{ fontSize: 12, color: '#666' }}>{mat.materialType}</td>
                  <td>{roleBadge(mat.tankRole)}</td>
                  <td>
                    <select
                      value={selectVal}
                      onChange={e => handleChange(mat.id, e.target.value)}
                      className="form-input"
                      style={{ padding: '3px 6px', fontSize: 12, margin: 0 }}
                    >
                      <option value="">— ล้างแท็ก —</option>
                      {ROLES.map(r => (
                        <option key={r.value} value={r.value}>{r.label}</option>
                      ))}
                    </select>
                  </td>
                  <td>
                    {saved[mat.id] ? (
                      <span style={{ color: '#2e7d32', fontSize: 12, fontWeight: 700 }}>✓ บันทึก</span>
                    ) : (
                      <button
                        className="save-button"
                        style={{ padding: '3px 10px', fontSize: 12 }}
                        disabled={!isDirty(mat) || saving[mat.id]}
                        onClick={() => handleSave(mat)}
                      >
                        {saving[mat.id] ? '...' : 'บันทึก'}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default TankRoleTagPanel;
