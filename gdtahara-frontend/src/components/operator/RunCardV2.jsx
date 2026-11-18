import React, { useState, useEffect, useMemo, useRef } from 'react';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { format } from 'date-fns';
import {
  Alert, Box, Button, Card, CardContent,
  CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, InputLabel, LinearProgress, MenuItem, Select,
  Snackbar, Stack, TextField, ToggleButton, ToggleButtonGroup, Typography,
} from '@mui/material';
import SubLotTree from './SubLotTree';
import {
  getPlansForDate,
  getShiftSplit,
  confirmBox as apiConfirmBox,
} from '../../api/phase1Api';
import axiosInstance from '../../api/axios';

// ── field helpers (handles both flat DTO and nested entity shapes) ────────────

const getMachineId   = (r) => r?.machineId   ?? r?.machine?.id;
const getMachineName = (r) => r?.machineName  ?? r?.machine?.machineName ?? '—';
const getPlanMachId  = (p) => p?.machineId    ?? p?.machine?.id;

// ── RunCardV2 ─────────────────────────────────────────────────────────────────

const RunCardV2 = ({ activeReports = [], initialReport = null }) => {

  // ── selectors ──────────────────────────────────────────────────────────────

  const machines = useMemo(() => {
    const seen = new Set();
    return activeReports.filter(r => {
      const id = getMachineId(r);
      if (!id || seen.has(id)) return false;
      seen.add(id);
      return true;
    });
  }, [activeReports]);

  const [selectedMachineId, setSelectedMachineId] = useState('');
  const [selectedDate,      setSelectedDate]      = useState(new Date());
  const [selectedShift,     setSelectedShift]     = useState('D');

  // One-time initialization from initialReport or first available machine
  const initDone = useRef(false);
  useEffect(() => {
    if (initDone.current) return;
    const source = initialReport ?? machines[0];
    if (!source) return;
    initDone.current = true;
    setSelectedMachineId(String(getMachineId(source) ?? ''));
    if (initialReport?.shift)     setSelectedShift(initialReport.shift);
    if (initialReport?.startDate) {
      const d = new Date(initialReport.startDate);
      if (!isNaN(d)) setSelectedDate(d);
    }
  }, [initialReport, machines]);

  // ── server state ───────────────────────────────────────────────────────────

  const [plans,       setPlans]       = useState([]);
  const [shiftSplit,  setShiftSplit]  = useState(null);
  const [boxData,     setBoxData]     = useState(null);
  const [refreshKey,  setRefreshKey]  = useState(0);
  const [loadingPlan, setLoadingPlan] = useState(false);
  const [planError,   setPlanError]   = useState(null);

  // ── dialog / snackbar ──────────────────────────────────────────────────────

  const [confirmOpen, setConfirmOpen]   = useState(false);
  const [confirmForm, setConfirmForm]   = useState({ boxQuantity: '', weightKg: '', palletNumber: '' });
  const [confirming,  setConfirming]    = useState(false);
  const [snack,       setSnack]         = useState({ open: false, msg: '', severity: 'success' });

  // ── derived values ─────────────────────────────────────────────────────────

  const currentReport = useMemo(() =>
    activeReports.find(r =>
      String(getMachineId(r)) === selectedMachineId &&
      (!r.shift || r.shift === selectedShift)
    ), [activeReports, selectedMachineId, selectedShift]);

  const currentPlan = useMemo(() =>
    plans.find(p => String(getPlanMachId(p)) === selectedMachineId)
  , [plans, selectedMachineId]);

  const productionReportId = currentReport?.id ?? null;
  const parentLotNumber    = currentReport?.parentLotNumber ?? null;

  const shiftTarget =
    selectedShift === 'D'
      ? (shiftSplit?.dayTarget ?? shiftSplit?.day_target)
      : (shiftSplit?.nightTarget ?? shiftSplit?.night_target);

  const actualBoxes  = boxData?.count ?? 0;
  const pctAchieve   = shiftTarget ? Math.round((actualBoxes / shiftTarget) * 100) : null;

  // ── effects ────────────────────────────────────────────────────────────────

  // Load plans when date changes
  useEffect(() => {
    if (!selectedDate) return;
    setLoadingPlan(true);
    setPlanError(null);
    getPlansForDate(format(selectedDate, 'yyyy-MM-dd'))
      .then(data => { setPlans(Array.isArray(data) ? data : []); setShiftSplit(null); })
      .catch(err  => setPlanError(err?.response?.data?.message ?? 'โหลดแผนผลิตล้มเหลว'))
      .finally(()  => setLoadingPlan(false));
  }, [selectedDate]);

  // Load shift split when plan is resolved
  useEffect(() => {
    if (!currentPlan?.id) { setShiftSplit(null); return; }
    getShiftSplit(currentPlan.id)
      .then(data => setShiftSplit(data))
      .catch(()  => setShiftSplit(null));
  }, [currentPlan?.id]);

  // Load box count from packaging_logs (actual boxes packed by operator)
  useEffect(() => {
    if (!productionReportId) { setBoxData(null); return; }
    axiosInstance.get(`/operator/reports/${productionReportId}/packaging-count`)
      .then(res => setBoxData(res.data))
      .catch(()  => setBoxData(null));
  }, [productionReportId, refreshKey]);

  // ── confirm box ────────────────────────────────────────────────────────────

  const handleConfirm = async () => {
    if (!productionReportId || !confirmForm.boxQuantity) return;
    setConfirming(true);
    try {
      await apiConfirmBox({
        productionReportId,
        boxQuantity:  parseInt(confirmForm.boxQuantity, 10),
        weightKg:     confirmForm.weightKg   ? parseFloat(confirmForm.weightKg)  : null,
        palletNumber: confirmForm.palletNumber || null,
      });
      setConfirmOpen(false);
      setConfirmForm({ boxQuantity: '', weightKg: '', palletNumber: '' });
      setRefreshKey(k => k + 1);
      setSnack({ open: true, msg: 'ยืนยันกล่องสำเร็จ', severity: 'success' });
    } catch (err) {
      const msg = err?.response?.status === 409
        ? 'ใบสั่งผลิตถูก Finalize แล้ว — ไม่สามารถเพิ่มกล่องได้'
        : (err?.response?.data?.message ?? 'เกิดข้อผิดพลาด ไม่สามารถยืนยันกล่องได้');
      setSnack({ open: true, msg, severity: 'error' });
    } finally {
      setConfirming(false);
    }
  };

  const closeSnack = () => setSnack(s => ({ ...s, open: false }));

  // ── render ─────────────────────────────────────────────────────────────────

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box sx={{ p: 1 }}>

        {/* ── Selector bar ────────────────────────────────────────────── */}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>เครื่องจักร</InputLabel>
            <Select value={selectedMachineId} label="เครื่องจักร"
              onChange={e => setSelectedMachineId(e.target.value)}>
              {machines.length === 0 && (
                <MenuItem value="" disabled>ไม่มีเครื่องที่ทำงานอยู่</MenuItem>
              )}
              {machines.map(r => (
                <MenuItem key={getMachineId(r)} value={String(getMachineId(r))}>
                  {getMachineName(r)}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <DatePicker
            label="วันที่"
            value={selectedDate}
            onChange={val => { if (val) setSelectedDate(val); }}
            slotProps={{ textField: { size: 'small' } }}
          />

          <ToggleButtonGroup value={selectedShift} exclusive size="small"
            onChange={(_, v) => { if (v) setSelectedShift(v); }}>
            <ToggleButton value="D">กะ D</ToggleButton>
            <ToggleButton value="N">กะ N</ToggleButton>
          </ToggleButtonGroup>
        </Stack>

        {loadingPlan && <LinearProgress sx={{ mb: 2 }} />}
        {planError   && <Alert severity="warning" sx={{ mb: 2 }}>{planError}</Alert>}

        {/*
          TODO: productionReportId resolution
          currentReport is derived from activeReports by machineId + shift.
          If /operator/reports/active does not return machineId/shift fields,
          add a dedicated backend endpoint and wire here instead.
        */}
        {!productionReportId && selectedMachineId && !loadingPlan && (
          <Alert severity="info" sx={{ mb: 2 }}>
            ไม่พบใบสั่งผลิตที่ตรงกับเครื่อง/กะที่เลือก
          </Alert>
        )}

        {/* ── Summary card ─────────────────────────────────────────────── */}
        {(currentPlan || productionReportId) && (
          <Card variant="outlined" sx={{ mb: 2 }}>
            <CardContent sx={{ pb: '12px !important' }}>
              <Stack direction="row" spacing={3} flexWrap="wrap" useFlexGap>
                <Box>
                  <Typography variant="caption" color="text.secondary">Parent Lot</Typography>
                  <Typography variant="body1" fontFamily="monospace" fontSize="0.85rem">
                    {parentLotNumber ?? '—'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    เป้ากะ {selectedShift}
                  </Typography>
                  <Typography variant="h6">
                    {shiftTarget != null ? shiftTarget : (currentPlan?.targetQty ?? '—')}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">กล่องจริง</Typography>
                  <Typography variant="h6">{actualBoxes}</Typography>
                </Box>
                {pctAchieve !== null && (
                  <Box>
                    <Typography variant="caption" color="text.secondary">%บรรลุ</Typography>
                    <Typography variant="h6"
                      color={pctAchieve >= 100 ? 'success.main'
                           : pctAchieve >= 80  ? 'warning.main'
                           : 'error.main'}>
                      {pctAchieve}%
                    </Typography>
                  </Box>
                )}
              </Stack>
            </CardContent>
          </Card>
        )}

        {/* ── Confirm box button ────────────────────────────────────────── */}
        {productionReportId && (
          <Button variant="contained" sx={{ mb: 2 }} onClick={() => setConfirmOpen(true)}>
            ยืนยันกล่อง (Confirm Box)
          </Button>
        )}

        {/* ── SubLotTree ───────────────────────────────────────────────── */}
        {productionReportId && (
          <SubLotTree
            productionReportId={productionReportId}
            parentLotNumber={parentLotNumber}
            refreshKey={refreshKey}
          />
        )}

        {/* ── Confirm dialog ───────────────────────────────────────────── */}
        <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)} maxWidth="xs" fullWidth>
          <DialogTitle>ยืนยันกล่อง</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField label="จำนวนชิ้น (boxQuantity)" type="number" size="small" required
                value={confirmForm.boxQuantity}
                onChange={e => setConfirmForm(f => ({ ...f, boxQuantity: e.target.value }))}
                inputProps={{ min: 1 }} />
              <TextField label="น้ำหนัก kg (ไม่บังคับ)" type="number" size="small"
                value={confirmForm.weightKg}
                onChange={e => setConfirmForm(f => ({ ...f, weightKg: e.target.value }))}
                inputProps={{ step: 0.001 }} />
              <TextField label="หมายเลข Pallet (ไม่บังคับ)" size="small"
                value={confirmForm.palletNumber}
                onChange={e => setConfirmForm(f => ({ ...f, palletNumber: e.target.value }))} />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setConfirmOpen(false)}>ยกเลิก</Button>
            <Button variant="contained" onClick={handleConfirm}
              disabled={!confirmForm.boxQuantity || confirming}>
              {confirming ? <CircularProgress size={18} color="inherit" /> : 'ยืนยัน'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* ── Snackbar ─────────────────────────────────────────────────── */}
        <Snackbar open={snack.open} autoHideDuration={4000} onClose={closeSnack}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert severity={snack.severity} onClose={closeSnack}>{snack.msg}</Alert>
        </Snackbar>

      </Box>
    </LocalizationProvider>
  );
};

export default RunCardV2;
