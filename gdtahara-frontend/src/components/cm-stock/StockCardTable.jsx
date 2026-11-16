import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, TextField, Button, CircularProgress, Alert,
  Chip, Grid, Divider
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { getStockCard } from '../../api/cmStockApi';

const fmtKg = (v) =>
  v == null ? '' : Number(v).toLocaleString('th-TH', { minimumFractionDigits: 2 });

const fmtDt = (v) => {
  if (!v) return '';
  const d = new Date(v);
  return d.toLocaleString('th-TH', { dateStyle: 'short', timeStyle: 'short' });
};

const typeLabel = { IN: 'รับเข้า', OUT: 'เบิกออก', CORRECTION: 'แก้ไข' };
const typeColor = { IN: '#e8f5e9', OUT: 'inherit', CORRECTION: '#fffde7' };

const StockCardTable = ({ materialCode, materialName, onBack }) => {
  const today = new Date().toISOString().slice(0, 10);
  const monthAgo = new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10);

  const [from, setFrom] = useState(monthAgo);
  const [to, setTo] = useState(today);
  const [machineId, setMachineId] = useState('');
  const [lotRef, setLotRef] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    const params = { from, to };
    if (machineId.trim()) params.machineId = machineId.trim();
    if (lotRef.trim()) params.lotRef = lotRef.trim();
    getStockCard(materialCode, params)
      .then(setData)
      .catch(() => setError('โหลดข้อมูลไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, [materialCode, from, to, machineId, lotRef]);

  useEffect(() => { load(); }, [load]);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={onBack} size="small">กลับ</Button>
        <Typography variant="h6">
          Stock Card · {materialName || materialCode}
        </Typography>
      </Box>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={1} alignItems="center">
          <Grid item xs={6} sm={3} md={2}>
            <TextField label="จากวันที่" type="date" size="small" fullWidth
              value={from} onChange={e => setFrom(e.target.value)}
              InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6} sm={3} md={2}>
            <TextField label="ถึงวันที่" type="date" size="small" fullWidth
              value={to} onChange={e => setTo(e.target.value)}
              InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6} sm={3} md={2}>
            <TextField label="เครื่อง" size="small" fullWidth
              value={machineId} onChange={e => setMachineId(e.target.value)}
              placeholder="เช่น RBL201" />
          </Grid>
          <Grid item xs={6} sm={3} md={2}>
            <TextField label="Lot" size="small" fullWidth
              value={lotRef} onChange={e => setLotRef(e.target.value)} />
          </Grid>
          <Grid item xs={12} sm={2}>
            <Button variant="contained" onClick={load} fullWidth>ค้นหา</Button>
          </Grid>
        </Grid>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {loading && <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>}

      {data && !loading && (
        <>
          <TableContainer component={Paper}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>วันที่/เวลา</TableCell>
                  <TableCell>ประเภท</TableCell>
                  <TableCell>รายละเอียด</TableCell>
                  <TableCell align="right">รับเข้า (KG)</TableCell>
                  <TableCell align="right">เบิกออก (KG)</TableCell>
                  <TableCell align="right">คงเหลือ (KG)</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {/* Opening balance row */}
                <TableRow sx={{ bgcolor: '#f5f5f5' }}>
                  <TableCell colSpan={5}>
                    <Typography variant="caption">ยอดยกมา (ก่อน {from})</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="caption" fontWeight="bold">
                      {fmtKg(data.openingBalance)}
                    </Typography>
                  </TableCell>
                </TableRow>

                {data.movements && data.movements.map((m, i) => (
                  <TableRow key={i} sx={{ bgcolor: typeColor[m.movementType] || 'inherit' }}>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtDt(m.movementAt)}</TableCell>
                    <TableCell>
                      <Chip
                        label={typeLabel[m.movementType] || m.movementType}
                        size="small"
                        color={m.movementType === 'IN' ? 'success' : m.movementType === 'CORRECTION' ? 'warning' : 'default'}
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{m.lotRef || '-'}</Typography>
                      {m.machineId && (
                        <Typography variant="caption" color="text.secondary">
                          {m.machineId}{m.tankId ? ` · ${m.tankId}` : ''} · {m.actor}
                        </Typography>
                      )}
                      {!m.machineId && m.actor && (
                        <Typography variant="caption" color="text.secondary">{m.actor}</Typography>
                      )}
                      {m.note && (
                        <Typography variant="caption" display="block" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                          {m.note}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell align="right">
                      {m.movementType === 'IN' ? fmtKg(m.qtyKg) : ''}
                    </TableCell>
                    <TableCell align="right">
                      {m.movementType === 'OUT' ? fmtKg(m.qtyKg) : ''}
                      {m.movementType === 'CORRECTION' ? (
                        <Typography variant="body2" color="warning.main">
                          {fmtKg(m.negativeQty)}
                        </Typography>
                      ) : null}
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" fontWeight="medium">
                        {fmtKg(m.runningBalance)}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}

                {/* Summary row */}
                <TableRow sx={{ bgcolor: '#e3f2fd', fontWeight: 'bold' }}>
                  <TableCell colSpan={3}>
                    <Typography variant="caption" fontWeight="bold">รวมช่วงเวลา</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="caption" fontWeight="bold" color="success.main">
                      +{fmtKg(data.totalInKg)}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="caption" fontWeight="bold" color="error.main">
                      -{fmtKg(data.totalOutKg)}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="caption" fontWeight="bold">
                      {fmtKg(data.closingBalance)}
                    </Typography>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </TableContainer>

          {(!data.movements || data.movements.length === 0) && (
            <Typography sx={{ textAlign: 'center', py: 3, color: 'text.secondary' }}>
              ไม่พบรายการในช่วงเวลานี้
            </Typography>
          )}
        </>
      )}
    </Box>
  );
};

export default StockCardTable;
