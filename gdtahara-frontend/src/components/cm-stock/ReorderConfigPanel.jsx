import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, TextField, Button, CircularProgress, Alert,
  IconButton, Chip, Tooltip, Switch, Collapse
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import CloseIcon from '@mui/icons-material/Close';
import RefreshIcon from '@mui/icons-material/Refresh';
import AddIcon from '@mui/icons-material/Add';
import { getReorderConfig, updateReorderConfig } from '../../api/cmStockApi';

const fmtKg = (v) =>
  v == null ? '—' : Number(v).toLocaleString('th-TH', { minimumFractionDigits: 0 });

const RowEdit = ({ row, onSave, onCancel }) => {
  const [form, setForm] = useState({
    reorderPointKg: row.reorderPointKg ?? '',
    safetyStockKg: row.safetyStockKg ?? '',
    reorderQtyKg: row.reorderQtyKg ?? '',
    leadTimeDays: row.leadTimeDays ?? '',
    isActive: row.isActive ?? true,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      const patch = {};
      if (form.reorderPointKg !== '') patch.reorderPointKg = String(form.reorderPointKg);
      if (form.safetyStockKg !== '') patch.safetyStockKg = String(form.safetyStockKg);
      if (form.reorderQtyKg !== '') patch.reorderQtyKg = String(form.reorderQtyKg);
      if (form.leadTimeDays !== '') patch.leadTimeDays = String(form.leadTimeDays);
      patch.isActive = String(form.isActive);
      await onSave(patch);
    } catch (err) {
      setError(err.response?.data?.message || 'บันทึกไม่สำเร็จ');
      setSaving(false);
    }
  };

  const numInput = (field, label) => (
    <TextField
      label={label}
      type="number"
      value={form[field]}
      onChange={e => setForm(p => ({ ...p, [field]: e.target.value }))}
      size="small"
      sx={{ width: 90 }}
      inputProps={{ min: 0, step: 0.001 }}
    />
  );

  return (
    <TableRow sx={{ bgcolor: 'action.selected' }}>
      <TableCell>
        <Typography variant="body2" fontWeight={600}>{row.materialCode}</Typography>
      </TableCell>
      <TableCell>{numInput('reorderPointKg', 'จุดสั่งซื้อ')}</TableCell>
      <TableCell>{numInput('safetyStockKg', 'Safety Stock')}</TableCell>
      <TableCell>{numInput('reorderQtyKg', 'ปริมาณสั่ง')}</TableCell>
      <TableCell>
        <TextField
          label="Lead time"
          type="number"
          value={form.leadTimeDays}
          onChange={e => setForm(p => ({ ...p, leadTimeDays: e.target.value }))}
          size="small"
          sx={{ width: 70 }}
          inputProps={{ min: 0 }}
        />
      </TableCell>
      <TableCell>
        <Switch
          checked={!!form.isActive}
          onChange={e => setForm(p => ({ ...p, isActive: e.target.checked }))}
          size="small"
        />
      </TableCell>
      <TableCell>
        {error && <Typography variant="caption" color="error">{error}</Typography>}
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Tooltip title="บันทึก">
            <IconButton size="small" color="primary" onClick={handleSave} disabled={saving}>
              {saving ? <CircularProgress size={16} /> : <SaveIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
          <Tooltip title="ยกเลิก">
            <IconButton size="small" onClick={onCancel} disabled={saving}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </TableCell>
    </TableRow>
  );
};

const AddNewRow = ({ onSave, onCancel }) => {
  const [form, setForm] = useState({
    materialCode: '',
    reorderPointKg: '',
    safetyStockKg: '',
    reorderQtyKg: '',
    leadTimeDays: '',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    if (!form.materialCode.trim()) { setError('กรุณาระบุรหัสวัตถุดิบ'); return; }
    setSaving(true);
    setError('');
    try {
      const patch = { isActive: 'true' };
      if (form.reorderPointKg) patch.reorderPointKg = form.reorderPointKg;
      if (form.safetyStockKg) patch.safetyStockKg = form.safetyStockKg;
      if (form.reorderQtyKg) patch.reorderQtyKg = form.reorderQtyKg;
      if (form.leadTimeDays) patch.leadTimeDays = form.leadTimeDays;
      await onSave(form.materialCode.trim().toUpperCase(), patch);
    } catch (err) {
      setError(err.response?.data?.message || 'บันทึกไม่สำเร็จ');
      setSaving(false);
    }
  };

  const numInput = (field, label, width = 90) => (
    <TextField label={label} type="number" value={form[field]}
      onChange={e => setForm(p => ({ ...p, [field]: e.target.value }))}
      size="small" sx={{ width }} inputProps={{ min: 0, step: 0.001 }} />
  );

  return (
    <TableRow sx={{ bgcolor: 'primary.50' }}>
      <TableCell>
        <TextField label="รหัสวัตถุดิบ *" value={form.materialCode}
          onChange={e => setForm(p => ({ ...p, materialCode: e.target.value }))}
          size="small" sx={{ width: 110 }} placeholder="เช่น PE-HD" />
      </TableCell>
      <TableCell>{numInput('reorderPointKg', 'จุดสั่งซื้อ')}</TableCell>
      <TableCell>{numInput('safetyStockKg', 'Safety Stock')}</TableCell>
      <TableCell>{numInput('reorderQtyKg', 'ปริมาณสั่ง')}</TableCell>
      <TableCell>{numInput('leadTimeDays', 'Lead time', 70)}</TableCell>
      <TableCell />
      <TableCell>
        {error && <Typography variant="caption" color="error" display="block">{error}</Typography>}
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Tooltip title="บันทึก">
            <IconButton size="small" color="primary" onClick={handleSave} disabled={saving}>
              {saving ? <CircularProgress size={16} /> : <SaveIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
          <Tooltip title="ยกเลิก">
            <IconButton size="small" onClick={onCancel} disabled={saving}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </TableCell>
    </TableRow>
  );
};

const ReorderConfigPanel = () => {
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [adding, setAdding] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    getReorderConfig()
      .then(setConfigs)
      .catch(() => setError('โหลดการตั้งค่าไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleSave = async (materialCode, patch) => {
    await updateReorderConfig(materialCode, patch);
    setEditing(null);
    setAdding(false);
    load();
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="subtitle1" fontWeight={700}>ตั้งค่า Reorder Point</Typography>
          <Typography variant="caption" color="text.secondary">
            Scheduler ตรวจสอบทุก 15 นาที — แจ้งเตือนเมื่อ balance &lt; reorderPointKg
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            size="small"
            startIcon={<AddIcon />}
            variant="outlined"
            onClick={() => { setAdding(true); setEditing(null); }}
            disabled={adding}
          >
            เพิ่ม material
          </Button>
          <IconButton onClick={load} size="small" disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {loading && <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>}

      {!loading && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.50' } }}>
                <TableCell>รหัสวัตถุดิบ</TableCell>
                <TableCell align="right">จุดสั่งซื้อ (KG)</TableCell>
                <TableCell align="right">Safety Stock (KG)</TableCell>
                <TableCell align="right">ปริมาณสั่ง (KG)</TableCell>
                <TableCell align="right">Lead Time (วัน)</TableCell>
                <TableCell align="center">ใช้งาน</TableCell>
                <TableCell align="center">แก้ไข</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {adding && (
                <AddNewRow
                  onSave={(mc, p) => handleSave(mc, p)}
                  onCancel={() => setAdding(false)}
                />
              )}
              {configs.length === 0 && !adding && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                    ยังไม่มีการตั้งค่า — กดปุ่ม "+ เพิ่ม material" เพื่อเริ่มต้น
                  </TableCell>
                </TableRow>
              )}
              {configs.map(cfg =>
                editing === cfg.materialCode ? (
                  <RowEdit
                    key={cfg.materialCode}
                    row={cfg}
                    onSave={(patch) => handleSave(cfg.materialCode, patch)}
                    onCancel={() => setEditing(null)}
                  />
                ) : (
                  <TableRow key={cfg.materialCode} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>{cfg.materialCode}</Typography>
                    </TableCell>
                    <TableCell align="right">{fmtKg(cfg.reorderPointKg)}</TableCell>
                    <TableCell align="right">{fmtKg(cfg.safetyStockKg)}</TableCell>
                    <TableCell align="right">{fmtKg(cfg.reorderQtyKg)}</TableCell>
                    <TableCell align="right">{cfg.leadTimeDays ?? '—'}</TableCell>
                    <TableCell align="center">
                      <Chip
                        label={cfg.isActive ? 'เปิด' : 'ปิด'}
                        color={cfg.isActive ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="แก้ไข">
                        <IconButton size="small" onClick={() => setEditing(cfg.materialCode)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                )
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {!loading && configs.length > 0 && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          หมายเหตุ: material ที่ไม่มีในรายการนี้ — เพิ่มโดยกดแก้ไขหลังจาก overview โหลด material code นั้นขึ้นมา
        </Typography>
      )}
    </Box>
  );
};

export default ReorderConfigPanel;
