import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Stepper, Step, StepLabel, Button,
  Card, CardActionArea, CardContent, Grid, TextField,
  MenuItem, Select, FormControl, InputLabel, CircularProgress,
  Alert, Divider, Chip
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

import { getCmMaterials, postRefill } from '../../api/cmMeterApi';

const STEPS = ['เลือกถัง', 'เลือกวัตถุดิบ', 'ระบุปริมาณ', 'ยืนยัน', 'สำเร็จ'];

const getShift = () => {
  const h = new Date().getHours();
  if (h >= 7 && h < 15) return 'A';
  if (h >= 15 && h < 23) return 'B';
  return 'C';
};

const RefillEntry = ({ machine, tanks, onBack }) => {
  const [step, setStep] = useState(0);
  const [form, setForm] = useState({
    tank: null,
    material: null,
    lotNumber: '',
    quantity: '',
    operatorNote: '',
  });
  const [materials, setMaterials] = useState([]);
  const [matLoading, setMatLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (step === 1 && form.tank) {
      setMatLoading(true);
      getCmMaterials(form.tank.tankRole)
        .then(setMaterials)
        .catch(() => setError('โหลดวัตถุดิบไม่สำเร็จ'))
        .finally(() => setMatLoading(false));
    }
  }, [step, form.tank]);

  const next = () => { setError(''); setStep(s => s + 1); };
  const back = () => { setError(''); setStep(s => s - 1); };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      await postRefill({
        machineId: machine.machineId,
        tankId: form.tank.tankId,
        materialId: form.material.materialId,
        lotNumber: form.lotNumber,
        quantity: parseFloat(form.quantity),
        unit: form.tank.unit || 'kg',
        shiftCode: getShift(),
        operatorNote: form.operatorNote,
      });
      setStep(4);
    } catch (e) {
      setError(e.response?.data?.message || 'บันทึกไม่สำเร็จ กรุณาลองใหม่');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{ p: 2, maxWidth: 600, mx: 'auto' }}>
      {/* Top bar */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 1 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={onBack}
          sx={{ minHeight: 44 }}
        >
          กลับ
        </Button>
        <Typography variant="h6" fontWeight={700}>เติมวัตถุดิบ — {machine?.machineName}</Typography>
      </Box>

      <Stepper activeStep={step} alternativeLabel sx={{ mb: 3 }}>
        {STEPS.map(label => (
          <Step key={label}>
            <StepLabel sx={{ '& .MuiStepLabel-label': { fontSize: '0.72rem' } }}>
              {label}
            </StepLabel>
          </Step>
        ))}
      </Stepper>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* Step 0: เลือกถัง */}
      {step === 0 && (
        <Box>
          <Typography variant="subtitle1" sx={{ mb: 1.5 }}>เลือกถังที่ต้องการเติม</Typography>
          <Grid container spacing={1.5}>
            {tanks.map(tank => (
              <Grid item xs={6} sm={4} key={tank.tankId}>
                <Card
                  variant="outlined"
                  sx={{
                    borderColor: form.tank?.tankId === tank.tankId ? 'primary.main' : 'divider',
                    bgcolor: form.tank?.tankId === tank.tankId ? 'primary.50' : 'background.paper',
                  }}
                >
                  <CardActionArea
                    onClick={() => setForm(f => ({ ...f, tank }))}
                    sx={{ p: 1.5, minHeight: 80, textAlign: 'center' }}
                  >
                    <Typography fontWeight={700}>{tank.tankName}</Typography>
                    <Chip label={tank.material} size="small" variant="outlined" sx={{ mt: 0.5 }} />
                    <Typography variant="caption" display="block" color="text.secondary" sx={{ mt: 0.5 }}>
                      {tank.currentLevel} {tank.unit} / {tank.capacity} {tank.unit}
                    </Typography>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>
          <Button
            variant="contained"
            fullWidth
            sx={{ mt: 3, minHeight: 48 }}
            disabled={!form.tank}
            onClick={next}
          >
            ถัดไป
          </Button>
        </Box>
      )}

      {/* Step 1: เลือกวัตถุดิบ */}
      {step === 1 && (
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
            <Typography variant="subtitle1">เลือกวัตถุดิบ</Typography>
            {form.tank?.tankRole && (
              <Chip label={`ถัง: ${form.tank.tankRole}`} size="small" color="primary" variant="outlined" />
            )}
          </Box>
          {matLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Grid container spacing={1.5}>
              {materials.map(mat => (
                <Grid item xs={6} sm={4} key={mat.materialId}>
                  <Card
                    variant="outlined"
                    sx={{
                      borderColor: form.material?.materialId === mat.materialId ? 'primary.main' : 'divider',
                      bgcolor: form.material?.materialId === mat.materialId ? 'primary.50' : 'background.paper',
                    }}
                  >
                    <CardActionArea
                      onClick={() => setForm(f => ({ ...f, material: mat, lotNumber: mat.lots?.[0] ?? '' }))}
                      sx={{ p: 1.5, minHeight: 80, textAlign: 'center' }}
                    >
                      <Typography fontWeight={700} sx={{ fontSize: '0.85rem' }}>{mat.rmCode}</Typography>
                      <Typography variant="caption" color="text.secondary">{mat.materialName}</Typography>
                    </CardActionArea>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}

          {form.material && form.material.lots?.length > 0 && (
            <FormControl fullWidth sx={{ mt: 2 }}>
              <InputLabel>Lot Number</InputLabel>
              <Select
                value={form.lotNumber}
                label="Lot Number"
                onChange={e => setForm(f => ({ ...f, lotNumber: e.target.value }))}
                sx={{ minHeight: 44 }}
              >
                {form.material.lots.map(lot => (
                  <MenuItem key={lot} value={lot}>{lot}</MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          {form.material && (!form.material.lots || form.material.lots.length === 0) && (
            <TextField
              fullWidth
              label="Lot Number"
              value={form.lotNumber}
              onChange={e => setForm(f => ({ ...f, lotNumber: e.target.value }))}
              sx={{ mt: 2 }}
              inputProps={{ style: { minHeight: 44 } }}
            />
          )}

          <Box sx={{ display: 'flex', gap: 1.5, mt: 3 }}>
            <Button variant="outlined" sx={{ minHeight: 48, flex: 1 }} onClick={back}>ย้อนกลับ</Button>
            <Button
              variant="contained"
              sx={{ minHeight: 48, flex: 1 }}
              disabled={!form.material}
              onClick={next}
            >
              ถัดไป
            </Button>
          </Box>
        </Box>
      )}

      {/* Step 2: ระบุปริมาณ */}
      {step === 2 && (
        <Box>
          <Typography variant="subtitle1" sx={{ mb: 2 }}>ระบุปริมาณที่เติม</Typography>
          <TextField
            label={`ปริมาณ (${form.tank?.unit || 'kg'})`}
            type="number"
            fullWidth
            value={form.quantity}
            onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))}
            inputProps={{ min: 0, style: { fontSize: '1.5rem', textAlign: 'center', minHeight: 56 } }}
            InputLabelProps={{ shrink: true }}
            sx={{ mb: 2 }}
          />
          <TextField
            label="หมายเหตุ (ถ้ามี)"
            fullWidth
            value={form.operatorNote}
            onChange={e => setForm(f => ({ ...f, operatorNote: e.target.value }))}
            multiline
            rows={2}
          />
          <Box sx={{ display: 'flex', gap: 1.5, mt: 3 }}>
            <Button variant="outlined" sx={{ minHeight: 48, flex: 1 }} onClick={back}>ย้อนกลับ</Button>
            <Button
              variant="contained"
              sx={{ minHeight: 48, flex: 1 }}
              disabled={!form.quantity || parseFloat(form.quantity) <= 0}
              onClick={next}
            >
              ถัดไป
            </Button>
          </Box>
        </Box>
      )}

      {/* Step 3: ยืนยัน */}
      {step === 3 && (
        <Box>
          <Typography variant="subtitle1" sx={{ mb: 2 }}>ยืนยันการเติมวัตถุดิบ</Typography>
          <Card variant="outlined" sx={{ mb: 2 }}>
            <CardContent>
              {[
                ['เครื่อง', machine?.machineName],
                ['ถัง', form.tank?.tankName],
                ['วัตถุดิบ', `${form.material?.rmCode} — ${form.material?.materialName}`],
                ['Lot', form.lotNumber || '—'],
                ['ปริมาณ', `${form.quantity} ${form.tank?.unit || 'kg'}`],
                ['กะ', getShift()],
                ...(form.operatorNote ? [['หมายเหตุ', form.operatorNote]] : []),
              ].map(([label, value]) => (
                <Box key={label} sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.75 }}>
                  <Typography variant="body2" color="text.secondary">{label}</Typography>
                  <Typography variant="body2" fontWeight={600}>{value}</Typography>
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
              color="success"
              sx={{ minHeight: 48, flex: 1 }}
              onClick={handleSubmit}
              disabled={submitting}
            >
              {submitting ? <CircularProgress size={20} color="inherit" /> : 'ยืนยันบันทึก'}
            </Button>
          </Box>
        </Box>
      )}

      {/* Step 4: สำเร็จ */}
      {step === 4 && (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <CheckCircleIcon sx={{ fontSize: 72, color: 'success.main', mb: 2 }} />
          <Typography variant="h6" fontWeight={700} gutterBottom>บันทึกสำเร็จ</Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            เติม {form.material?.rmCode} {form.quantity} {form.tank?.unit || 'kg'} ลงถัง {form.tank?.tankName} เรียบร้อยแล้ว
          </Typography>
          <Button
            variant="contained"
            size="large"
            sx={{ minHeight: 48 }}
            onClick={onBack}
          >
            กลับหน้าหลัก
          </Button>
        </Box>
      )}
    </Box>
  );
};

export default RefillEntry;
