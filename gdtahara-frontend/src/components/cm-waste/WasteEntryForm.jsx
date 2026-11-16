import React, { useState, useEffect } from 'react';
import {
  Box, Paper, Typography, TextField, MenuItem, Button,
  CircularProgress, Alert, InputAdornment
} from '@mui/material';
import { getWasteTypes, createWasteRecord } from '../../api/cmWasteApi';

export default function WasteEntryForm({ onSaved }) {
  const [wasteTypes, setWasteTypes] = useState([]);
  const [form, setForm] = useState({
    machineId: '', woNumber: '', wasteTypeCode: '', weightKg: '', note: ''
  });
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    setLoading(true);
    getWasteTypes()
      .then(setWasteTypes)
      .catch(() => setError('โหลดประเภทของเสียไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, []);

  const handleChange = (field) => (e) => setForm(prev => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.machineId || !form.wasteTypeCode || !form.weightKg) {
      setError('กรุณากรอกข้อมูลที่จำเป็น: เครื่อง, ประเภทของเสีย, น้ำหนัก');
      return;
    }
    setSubmitting(true);
    setError('');
    setSuccess('');
    try {
      await createWasteRecord({
        machineId: form.machineId,
        woNumber: form.woNumber || undefined,
        wasteTypeCode: form.wasteTypeCode,
        weightKg: parseFloat(form.weightKg),
        note: form.note || undefined,
      });
      setSuccess('บันทึกของเสียสำเร็จ');
      setForm({ machineId: '', woNumber: '', wasteTypeCode: '', weightKg: '', note: '' });
      onSaved?.();
    } catch (err) {
      setError(err.response?.data?.message || 'บันทึกไม่สำเร็จ');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>;

  return (
    <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
      <Typography variant="h6" fontWeight={700} sx={{ mb: 2 }}>บันทึกของเสีย</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
      <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField
          label="รหัสเครื่อง *"
          value={form.machineId}
          onChange={handleChange('machineId')}
          placeholder="เช่น RBL201"
          size="small"
          fullWidth
        />
        <TextField
          label="เลข WO"
          value={form.woNumber}
          onChange={handleChange('woNumber')}
          size="small"
          fullWidth
        />
        <TextField
          select
          label="ประเภทของเสีย *"
          value={form.wasteTypeCode}
          onChange={handleChange('wasteTypeCode')}
          size="small"
          fullWidth
        >
          <MenuItem value="">-- เลือก --</MenuItem>
          {wasteTypes.map(t => (
            <MenuItem key={t.code} value={t.code}>{t.code} — {t.nameTh}</MenuItem>
          ))}
        </TextField>
        <TextField
          label="น้ำหนัก (kg) *"
          type="number"
          inputProps={{ min: 0.001, step: 0.001 }}
          value={form.weightKg}
          onChange={handleChange('weightKg')}
          InputProps={{ endAdornment: <InputAdornment position="end">kg</InputAdornment> }}
          size="small"
          fullWidth
        />
        <TextField
          label="หมายเหตุ"
          value={form.note}
          onChange={handleChange('note')}
          multiline
          rows={2}
          size="small"
          fullWidth
        />
        <Button type="submit" variant="contained" disabled={submitting}>
          {submitting ? <CircularProgress size={20} /> : 'บันทึก'}
        </Button>
      </Box>
    </Paper>
  );
}
