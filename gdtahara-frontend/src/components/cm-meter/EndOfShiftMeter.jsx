import React, { useState } from 'react';
import {
  Box, Typography, Button, Card, CardContent, Table, TableHead,
  TableBody, TableRow, TableCell, TableContainer, Paper, TextField,
  Chip, Alert, CircularProgress
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

import { postEndOfShift } from '../../api/cmMeterApi';

const EndOfShiftMeter = ({ machine, tanks, shift, onBack }) => {
  const [readings, setReadings] = useState(() =>
    tanks.map(t => ({ tankId: t.tankId, tankName: t.tankName, unit: t.unit || 'kg', value: '' }))
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const setVal = (tankId, val) =>
    setReadings(rs => rs.map(r => r.tankId === tankId ? { ...r, value: val } : r));

  const allFilled = readings.every(r => r.value !== '' && !isNaN(parseFloat(r.value)));

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      await postEndOfShift({
        machineId: machine.machineId,
        shiftCode: shift,
        readings: readings.map(r => ({
          tankId: r.tankId,
          meterValue: parseFloat(r.value),
          unit: r.unit,
        })),
      });
      setDone(true);
    } catch (e) {
      setError(e.response?.data?.message || 'บันทึกไม่สำเร็จ กรุณาลองใหม่');
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <Box sx={{ p: 2, maxWidth: 600, mx: 'auto', textAlign: 'center', py: 6 }}>
        <CheckCircleIcon sx={{ fontSize: 72, color: 'success.main', mb: 2 }} />
        <Typography variant="h6" fontWeight={700} gutterBottom>บันทึกสิ้นกะสำเร็จ</Typography>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          กะ {shift} — {machine?.machineName} — บันทึก {readings.length} ถัง
        </Typography>
        <Button variant="contained" size="large" sx={{ minHeight: 48 }} onClick={onBack}>
          กลับหน้าหลัก
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 2, maxWidth: 700, mx: 'auto' }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 1 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={onBack}
          sx={{ minHeight: 44 }}
        >
          กลับ
        </Button>
        <Typography variant="h6" fontWeight={700}>บันทึกสิ้นกะ — {machine?.machineName}</Typography>
        <Chip label={`กะ ${shift}`} size="small" color="primary" />
      </Box>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        กรอกค่ามิเตอร์/ปริมาณที่เหลือ ณ สิ้นกะสำหรับทุกถัง
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper} variant="outlined" sx={{ mb: 3 }}>
        <Table>
          <TableHead>
            <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.50' } }}>
              <TableCell>ถัง</TableCell>
              <TableCell>วัตถุดิบ</TableCell>
              <TableCell sx={{ minWidth: 160 }}>ค่ามิเตอร์ / ปริมาณคงเหลือ</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {tanks.map((tank, i) => (
              <TableRow key={tank.tankId}>
                <TableCell sx={{ fontWeight: 600 }}>{tank.tankName}</TableCell>
                <TableCell>
                  <Chip label={tank.material} size="small" variant="outlined" />
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <TextField
                      type="number"
                      value={readings[i]?.value ?? ''}
                      onChange={e => setVal(tank.tankId, e.target.value)}
                      size="small"
                      sx={{ width: 110 }}
                      inputProps={{ min: 0, style: { textAlign: 'right', minHeight: 36 } }}
                      placeholder="0"
                    />
                    <Typography variant="body2" color="text.secondary">
                      {tank.unit || 'kg'}
                    </Typography>
                  </Box>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Button
        variant="contained"
        color="success"
        fullWidth
        size="large"
        sx={{ minHeight: 52 }}
        disabled={!allFilled || submitting}
        onClick={handleSubmit}
      >
        {submitting ? <CircularProgress size={22} color="inherit" /> : `บันทึกสิ้นกะ ${readings.length} ถัง`}
      </Button>
    </Box>
  );
};

export default EndOfShiftMeter;
