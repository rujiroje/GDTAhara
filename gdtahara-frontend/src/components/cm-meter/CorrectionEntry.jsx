import React, { useState } from 'react';
import {
  Box, Typography, Button, Card, CardActionArea, CardContent,
  Grid, TextField, MenuItem, Select, FormControl, InputLabel,
  Alert, CircularProgress, Chip, Divider
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

import { postCorrection } from '../../api/cmMeterApi';

const REASONS = [
  'บันทึกค่าผิดพลาด',
  'วัตถุดิบหก/สูญเสีย',
  'โอนย้ายระหว่างถัง',
  'ตรวจนับปรับยอด',
  'อื่นๆ',
];

const CorrectionEntry = ({ machine, tanks, onBack }) => {
  const [step, setStep] = useState(0); // 0=เลือกถัง, 1=ระบุ, 2=ยืนยัน, 3=สำเร็จ
  const [form, setForm] = useState({
    tank: null,
    quantity: '',
    reason: '',
    operatorNote: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const next = () => { setError(''); setStep(s => s + 1); };
  const back = () => { setError(''); setStep(s => s - 1); };

  const qty = parseFloat(form.quantity);
  const absQty = Math.abs(qty);

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      await postCorrection({
        machineId: machine.machineId,
        tankId: form.tank.tankId,
        quantity: -absQty,
        reason: form.reason,
        operatorNote: form.operatorNote,
      });
      setStep(3);
    } catch (e) {
      setError(e.response?.data?.message || 'บันทึกไม่สำเร็จ กรุณาลองใหม่');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{ p: 2, maxWidth: 600, mx: 'auto' }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 1 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={onBack} sx={{ minHeight: 44 }}>
          กลับ
        </Button>
        <Typography variant="h6" fontWeight={700}>แก้ไขค่า — {machine?.machineName}</Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* Step 0: เลือกถัง */}
      {step === 0 && (
        <Box>
          <Typography variant="subtitle1" sx={{ mb: 1.5 }}>เลือกถังที่ต้องการแก้ไข</Typography>
          <Grid container spacing={1.5}>
            {tanks.map(tank => (
              <Grid item xs={6} sm={4} key={tank.tankId}>
                <Card
                  variant="outlined"
                  sx={{
                    borderColor: form.tank?.tankId === tank.tankId ? 'warning.main' : 'divider',
                    bgcolor: form.tank?.tankId === tank.tankId ? 'warning.50' : 'background.paper',
                  }}
                >
                  <CardActionArea
                    onClick={() => setForm(f => ({ ...f, tank }))}
                    sx={{ p: 1.5, minHeight: 80, textAlign: 'center' }}
                  >
                    <Typography fontWeight={700}>{tank.tankName}</Typography>
                    <Chip label={tank.material} size="small" variant="outlined" sx={{ mt: 0.5 }} />
                    <Typography variant="caption" display="block" color="text.secondary" sx={{ mt: 0.5 }}>
                      {tank.currentLevel} {tank.unit}
                    </Typography>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>
          <Button
            variant="contained"
            color="warning"
            fullWidth
            sx={{ mt: 3, minHeight: 48 }}
            disabled={!form.tank}
            onClick={next}
          >
            ถัดไป
          </Button>
        </Box>
      )}

      {/* Step 1: ระบุปริมาณและเหตุผล */}
      {step === 1 && (
        <Box>
          <Typography variant="subtitle1" sx={{ mb: 2 }}>ระบุปริมาณที่ต้องการลดและเหตุผล</Typography>
          <Alert severity="warning" sx={{ mb: 2 }}>
            ระบบจะบันทึกเป็นค่าลบ (ลดปริมาณในถัง)
          </Alert>
          <TextField
            label={`ปริมาณที่ต้องการลด (${form.tank?.unit || 'kg'})`}
            type="number"
            fullWidth
            value={form.quantity}
            onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))}
            inputProps={{ min: 0, style: { fontSize: '1.5rem', textAlign: 'center', minHeight: 56 } }}
            InputLabelProps={{ shrink: true }}
            helperText="ใส่ตัวเลขบวก — ระบบจะแปลงเป็นลบให้อัตโนมัติ"
            sx={{ mb: 2 }}
          />
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>เหตุผล</InputLabel>
            <Select
              value={form.reason}
              label="เหตุผล"
              onChange={e => setForm(f => ({ ...f, reason: e.target.value }))}
              sx={{ minHeight: 44 }}
            >
              {REASONS.map(r => (
                <MenuItem key={r} value={r}>{r}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            label="หมายเหตุเพิ่มเติม"
            fullWidth
            multiline
            rows={2}
            value={form.operatorNote}
            onChange={e => setForm(f => ({ ...f, operatorNote: e.target.value }))}
          />
          <Box sx={{ display: 'flex', gap: 1.5, mt: 3 }}>
            <Button variant="outlined" sx={{ minHeight: 48, flex: 1 }} onClick={back}>ย้อนกลับ</Button>
            <Button
              variant="contained"
              color="warning"
              sx={{ minHeight: 48, flex: 1 }}
              disabled={!form.quantity || parseFloat(form.quantity) <= 0 || !form.reason}
              onClick={next}
            >
              ถัดไป
            </Button>
          </Box>
        </Box>
      )}

      {/* Step 2: ยืนยัน */}
      {step === 2 && (
        <Box>
          <Typography variant="subtitle1" sx={{ mb: 2 }}>ยืนยันการแก้ไข</Typography>
          <Card variant="outlined" sx={{ mb: 2, borderColor: 'warning.main' }}>
            <CardContent>
              {[
                ['เครื่อง', machine?.machineName],
                ['ถัง', form.tank?.tankName],
                ['ปริมาณที่ลด', `-${absQty} ${form.tank?.unit || 'kg'}`],
                ['เหตุผล', form.reason],
                ...(form.operatorNote ? [['หมายเหตุ', form.operatorNote]] : []),
              ].map(([label, value]) => (
                <Box key={label} sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.75 }}>
                  <Typography variant="body2" color="text.secondary">{label}</Typography>
                  <Typography
                    variant="body2"
                    fontWeight={600}
                    color={label === 'ปริมาณที่ลด' ? 'error.main' : 'inherit'}
                  >
                    {value}
                  </Typography>
                </Box>
              ))}
            </CardContent>
          </Card>
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <Button variant="outlined" sx={{ minHeight: 48, flex: 1 }} onClick={back} disabled={submitting}>
              ย้อนกลับ
            </Button>
            <Button
              variant="contained"
              color="warning"
              sx={{ minHeight: 48, flex: 1 }}
              onClick={handleSubmit}
              disabled={submitting}
            >
              {submitting ? <CircularProgress size={20} color="inherit" /> : 'ยืนยันแก้ไข'}
            </Button>
          </Box>
        </Box>
      )}

      {/* Step 3: สำเร็จ */}
      {step === 3 && (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <CheckCircleIcon sx={{ fontSize: 72, color: 'warning.main', mb: 2 }} />
          <Typography variant="h6" fontWeight={700} gutterBottom>บันทึกการแก้ไขสำเร็จ</Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            ลด {absQty} {form.tank?.unit || 'kg'} จากถัง {form.tank?.tankName} เรียบร้อยแล้ว
          </Typography>
          <Button variant="contained" size="large" sx={{ minHeight: 48 }} onClick={onBack}>
            กลับหน้าหลัก
          </Button>
        </Box>
      )}
    </Box>
  );
};

export default CorrectionEntry;
