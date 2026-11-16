import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, CircularProgress, Alert, Typography, InputAdornment
} from '@mui/material';
import { createWasteEditRequest } from '../../api/cmWasteApi';

export default function EditRequestModal({ record, onClose, onSaved }) {
  const [form, setForm] = useState({
    weightKg: record?.weightKg?.toString() || '',
    wasteTypeCode: record?.wasteTypeCode || '',
    note: record?.note || '',
    reason: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    if (!form.reason.trim()) {
      setError('กรุณาระบุเหตุผลในการขอแก้ไข');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const payload = { reason: form.reason };
      if (form.weightKg) payload.requestedWeightKg = parseFloat(form.weightKg);
      if (form.wasteTypeCode) payload.requestedWasteTypeCode = form.wasteTypeCode;
      if (form.note) payload.requestedNote = form.note;
      await createWasteEditRequest(record.id, payload);
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || 'ส่งคำขอไม่สำเร็จ');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>ส่งคำขอแก้ไข</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
        <Typography variant="caption" color="text.secondary">
          รายการ #{record?.id} — {record?.machineId} — {record?.wasteTypeCode}
        </Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField
          label="ประเภทของเสีย (ใหม่)"
          value={form.wasteTypeCode}
          onChange={e => setForm(p => ({ ...p, wasteTypeCode: e.target.value }))}
          size="small" fullWidth
        />
        <TextField
          label="น้ำหนัก (kg) ใหม่"
          type="number"
          inputProps={{ min: 0.001, step: 0.001 }}
          value={form.weightKg}
          onChange={e => setForm(p => ({ ...p, weightKg: e.target.value }))}
          InputProps={{ endAdornment: <InputAdornment position="end">kg</InputAdornment> }}
          size="small" fullWidth
        />
        <TextField
          label="หมายเหตุใหม่"
          value={form.note}
          onChange={e => setForm(p => ({ ...p, note: e.target.value }))}
          multiline rows={2}
          size="small" fullWidth
        />
        <TextField
          label="เหตุผลในการขอแก้ไข *"
          value={form.reason}
          onChange={e => setForm(p => ({ ...p, reason: e.target.value }))}
          multiline rows={2}
          size="small" fullWidth
          required
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={submitting}>ยกเลิก</Button>
        <Button onClick={handleSubmit} variant="contained" color="warning" disabled={submitting}>
          {submitting ? <CircularProgress size={18} /> : 'ส่งคำขอ'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
