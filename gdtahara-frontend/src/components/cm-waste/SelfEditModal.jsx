import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, CircularProgress, Alert, InputAdornment
} from '@mui/material';
import { selfEditWaste } from '../../api/cmWasteApi';

export default function SelfEditModal({ record, onClose, onSaved }) {
  const [form, setForm] = useState({
    weightKg: record?.weightKg?.toString() || '',
    wasteTypeCode: record?.wasteTypeCode || '',
    note: record?.note || '',
    reason: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    if (!form.reason.trim()) { setError('กรุณาระบุเหตุผลการแก้ไข'); return; }
    if (!form.wasteTypeCode.trim()) { setError('กรุณาระบุประเภทของเสีย'); return; }
    if (!form.weightKg) { setError('กรุณาระบุน้ำหนัก'); return; }
    setSubmitting(true);
    setError('');
    try {
      await selfEditWaste(record.id, {
        machineId: record.machineId,
        wasteTypeCode: form.wasteTypeCode,
        weightKg: parseFloat(form.weightKg),
        note: form.note || '',
        reason: form.reason,
      });
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || 'แก้ไขไม่สำเร็จ — ตรวจสอบสิทธิ์หรือเวลา');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>แก้ไขรายการของเสีย (Self Edit)</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          label="ประเภทของเสีย *"
          value={form.wasteTypeCode}
          onChange={e => setForm(p => ({ ...p, wasteTypeCode: e.target.value }))}
          size="small" fullWidth required
        />
        <TextField
          label="น้ำหนัก (kg) *"
          type="number"
          inputProps={{ min: 0.001, step: 0.001 }}
          value={form.weightKg}
          onChange={e => setForm(p => ({ ...p, weightKg: e.target.value }))}
          InputProps={{ endAdornment: <InputAdornment position="end">kg</InputAdornment> }}
          size="small" fullWidth required
        />
        <TextField
          label="หมายเหตุ"
          value={form.note}
          onChange={e => setForm(p => ({ ...p, note: e.target.value }))}
          multiline rows={2}
          size="small" fullWidth
        />
        <TextField
          label="เหตุผลการแก้ไข *"
          value={form.reason}
          onChange={e => setForm(p => ({ ...p, reason: e.target.value }))}
          multiline rows={2}
          size="small" fullWidth required
          helperText="จำเป็นต้องกรอก"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={submitting}>ยกเลิก</Button>
        <Button onClick={handleSubmit} variant="contained" disabled={submitting}>
          {submitting ? <CircularProgress size={18} /> : 'บันทึก'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
