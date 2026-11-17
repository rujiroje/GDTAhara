import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Card, CardActionArea, Grid,
  Table, TableHead, TableBody, TableRow, TableCell, TableContainer,
  Paper, Button, Chip, CircularProgress, Alert, Tooltip
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import AssessmentIcon from '@mui/icons-material/Assessment';
import EditIcon from '@mui/icons-material/Edit';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';

import { getCmMachines, getCmTanks, getRefillHistory } from '../../api/cmMeterApi';
import RefillEntry from './RefillEntry';
import EndOfShiftMeter from './EndOfShiftMeter';
import CorrectionEntry from './CorrectionEntry';

const getShift = () => {
  const h = new Date().getHours();
  if (h >= 7 && h < 15) return 'A';
  if (h >= 15 && h < 23) return 'B';
  return 'C';
};

const statusColor = {
  OK:       '#2e7d32',
  LOW:      '#ed6c02',
  CRITICAL: '#d32f2f',
  EMPTY:    '#616161',
};

const statusLabel = {
  OK:       'ปกติ',
  LOW:      'ใกล้หมด',
  CRITICAL: 'วิกฤต',
  EMPTY:    'หมด',
};

const calcStatus = (tank) => {
  const kg = tank.latestMeterKg ?? 0;
  const cap = tank.capacityKg ?? 500;
  const pct = cap > 0 ? (kg / cap) * 100 : 0;
  if (pct <= 0) return 'EMPTY';
  if (pct < 20) return 'CRITICAL';
  if (pct < 40) return 'LOW';
  return 'OK';
};

const calcLevel = (tank) => {
  const kg = tank.latestMeterKg ?? 0;
  const cap = tank.capacityKg ?? 500;
  return cap > 0 ? Math.round((kg / cap) * 100) : 0;
};

const LevelBar = ({ level, status }) => (
  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 100 }}>
    <Box sx={{
      flexGrow: 1, height: 10, borderRadius: 5,
      bgcolor: '#e0e0e0', overflow: 'hidden'
    }}>
      <Box sx={{
        width: `${Math.min(level, 100)}%`, height: '100%',
        bgcolor: statusColor[status] || '#757575',
        transition: 'width .3s'
      }} />
    </Box>
    <Typography variant="caption" sx={{ minWidth: 36, textAlign: 'right', color: statusColor[status] }}>
      {level}%
    </Typography>
  </Box>
);

const CmMeterDashboard = () => {
  const shift = getShift();

  const [machines, setMachines] = useState([]);
  const [selected, setSelected] = useState(null);
  const [tanks, setTanks] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeView, setActiveView] = useState(null);

  useEffect(() => {
    getCmMachines()
      .then(setMachines)
      .catch(() => setError('โหลดรายชื่อเครื่องไม่สำเร็จ'));
  }, []);

  const loadMachineData = useCallback((machine) => {
    setSelected(machine);
    setLoading(true);
    setError('');
    Promise.all([
      getCmTanks(machine.machineId),
      getRefillHistory(machine.machineId, 20),
    ])
      .then(([t, h]) => { setTanks(t); setHistory(h); })
      .catch(() => setError('โหลดข้อมูลถังไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, []);

  const handleBack = () => {
    setActiveView(null);
    if (selected) loadMachineData(selected);
  };

  // C-04: build map tankId → last-refilled materialCode (most recent non-correction)
  const lastRefillByTank = {};
  history
    .filter(r => r.entryType !== 'CORRECTION')
    .forEach(r => {
      if (!lastRefillByTank[r.tankId] && r.material) {
        lastRefillByTank[r.tankId] = r.material;
      }
    });

  if (activeView === 'refill')
    return <RefillEntry machine={selected} tanks={tanks} onBack={handleBack} />;
  if (activeView === 'eos')
    return <EndOfShiftMeter machine={selected} tanks={tanks} shift={shift} onBack={handleBack} />;
  if (activeView === 'correction')
    return <CorrectionEntry machine={selected} tanks={tanks} onBack={handleBack} />;

  return (
    <Box sx={{ p: 2, maxWidth: 960, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 1.5 }}>
        <Typography variant="h6" fontWeight={700}>บันทึกวัตถุดิบ CM</Typography>
        <Chip label={`กะ ${shift}`} size="small" color="primary" />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>เลือกเครื่อง</Typography>
      <Grid container spacing={1.5} sx={{ mb: 3 }}>
        {machines.map(m => (
          <Grid item xs={6} sm={4} md={3} key={m.machineId}>
            <Card
              variant="outlined"
              sx={{
                borderColor: selected?.machineId === m.machineId ? 'primary.main' : 'divider',
                bgcolor: selected?.machineId === m.machineId ? 'primary.50' : 'background.paper',
              }}
            >
              <CardActionArea
                onClick={() => loadMachineData(m)}
                sx={{ p: 1.5, minHeight: 56 }}
              >
                <Typography variant="body1" fontWeight={600} textAlign="center">
                  {m.machineName || m.machineId}
                </Typography>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>

      {selected && (
        <>
          <Box sx={{ display: 'flex', gap: 1.5, mb: 3, flexWrap: 'wrap' }}>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              size="large"
              sx={{ minHeight: 48, flex: 1, minWidth: 140 }}
              disabled={loading || tanks.length === 0}
              onClick={() => setActiveView('refill')}
            >
              เติมวัตถุดิบ
            </Button>
            <Button
              variant="outlined"
              startIcon={<AssessmentIcon />}
              size="large"
              sx={{ minHeight: 48, flex: 1, minWidth: 140 }}
              disabled={loading || tanks.length === 0}
              onClick={() => setActiveView('eos')}
            >
              บันทึกสิ้นกะ
            </Button>
            <Button
              variant="outlined"
              color="warning"
              startIcon={<EditIcon />}
              size="large"
              sx={{ minHeight: 48, flex: 1, minWidth: 140 }}
              disabled={loading || tanks.length === 0}
              onClick={() => setActiveView('correction')}
            >
              แก้ไขค่า
            </Button>
          </Box>

          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
            สถานะถัง — {selected.machineName || selected.machineId}
          </Typography>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer component={Paper} variant="outlined" sx={{ mb: 3 }}>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.50' } }}>
                    <TableCell>ถัง</TableCell>
                    <TableCell>BOM แนะนำ</TableCell>
                    <TableCell>ระดับ</TableCell>
                    <TableCell>ปริมาณ (kg)</TableCell>
                    <TableCell>สถานะ</TableCell>
                    <TableCell>อ่านล่าสุด</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tanks.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                        ไม่มีข้อมูลถัง
                      </TableCell>
                    </TableRow>
                  ) : tanks.map(tank => {
                    const status = calcStatus(tank);
                    const level = calcLevel(tank);
                    const bom = tank.bomSuggestedMaterial;
                    const lastRefill = lastRefillByTank[tank.tankId];
                    const hasMismatch = bom && lastRefill && lastRefill !== bom.materialCode;
                    const hasMatch   = bom && lastRefill && lastRefill === bom.materialCode;
                    return (
                      <TableRow key={tank.tankId} hover>
                        <TableCell sx={{ fontWeight: 600 }}>{tank.tankName}</TableCell>
                        <TableCell>
                          {bom ? (
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <Tooltip title={bom.materialName || bom.materialCode} arrow>
                                <Chip
                                  label={bom.materialCode}
                                  size="small"
                                  sx={{
                                    bgcolor: hasMismatch ? '#fff3e0' : hasMatch ? '#e8f5e9' : '#e3f2fd',
                                    color:   hasMismatch ? '#e65100'  : hasMatch ? '#2e7d32'  : '#1565c0',
                                    fontWeight: 600,
                                    border: `1px solid ${hasMismatch ? '#ffb74d' : hasMatch ? '#81c784' : '#90caf9'}`,
                                    maxWidth: 110,
                                    '& .MuiChip-label': { overflow: 'hidden', textOverflow: 'ellipsis' }
                                  }}
                                />
                              </Tooltip>
                              {hasMismatch && (
                                <Tooltip title={`เติมล่าสุด: ${lastRefill} ≠ BOM: ${bom.materialCode}`} arrow>
                                  <WarningAmberIcon sx={{ fontSize: 16, color: '#e65100', cursor: 'help' }} />
                                </Tooltip>
                              )}
                              {hasMatch && (
                                <CheckCircleOutlineIcon sx={{ fontSize: 16, color: '#2e7d32' }} />
                              )}
                            </Box>
                          ) : (
                            <Typography variant="caption" color="text.disabled">ไม่มีแผน</Typography>
                          )}
                        </TableCell>
                        <TableCell sx={{ minWidth: 130 }}>
                          <LevelBar level={level} status={status} />
                        </TableCell>
                        <TableCell>
                          {tank.latestMeterKg ?? 0} kg
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={statusLabel[status] || status}
                            size="small"
                            sx={{
                              bgcolor: (statusColor[status] || '#757575') + '18',
                              color: statusColor[status] || '#757575',
                              fontWeight: 700,
                              border: `1px solid ${(statusColor[status] || '#757575')}44`
                            }}
                          />
                        </TableCell>
                        <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                          {tank.latestReadingAt
                            ? new Date(tank.latestReadingAt).toLocaleString('th-TH', {
                                day: '2-digit', month: '2-digit',
                                hour: '2-digit', minute: '2-digit'
                              })
                            : '—'}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {history.length > 0 && (
            <>
              <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                ประวัติล่าสุด
              </Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.50' } }}>
                      <TableCell>เวลา</TableCell>
                      <TableCell>ถัง</TableCell>
                      <TableCell>วัตถุดิบ / Lot</TableCell>
                      <TableCell>ปริมาณ</TableCell>
                      <TableCell>กะ</TableCell>
                      <TableCell>ประเภท</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {history.map((row, i) => (
                      <TableRow key={i} hover>
                        <TableCell sx={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>
                          {new Date(row.recordedAt).toLocaleString('th-TH', {
                            day: '2-digit', month: '2-digit',
                            hour: '2-digit', minute: '2-digit'
                          })}
                        </TableCell>
                        <TableCell>{row.tankName}</TableCell>
                        <TableCell>{row.material || '—'}</TableCell>
                        <TableCell sx={{ color: row.quantity < 0 ? '#d32f2f' : 'inherit' }}>
                          {row.quantity > 0 ? '+' : ''}{row.quantity} {row.unit}
                        </TableCell>
                        <TableCell>{row.shiftCode}</TableCell>
                        <TableCell>
                          <Chip
                            label={row.entryType === 'CORRECTION' ? 'แก้ไข' : 'เติม'}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </>
      )}
    </Box>
  );
};

export default CmMeterDashboard;
