import React, { useState, useEffect, useCallback } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  IconButton,
  Snackbar,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import BuildIcon from '@mui/icons-material/Build';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import RefreshIcon from '@mui/icons-material/Refresh';
import SkipNextIcon from '@mui/icons-material/SkipNext';
import { useAuth } from '../../App';
import {
  completeSetup,
  getPendingSetupJobs,
  skipSetup,
  startSetup,
} from '../../api/phase1Api';

const STATUS_COLOR = {
  PENDING: 'warning',
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  SKIPPED: 'default',
};

const BLANK_CHECKLIST = {
  moldChanged: false,
  moldCodeFrom: '',
  moldCodeTo: '',
  tempAdjusted: false,
  cycleAdjusted: false,
  blowPinAligned: false,
  fpiPassed: false,
  notes: '',
};

// ─────────────────────────────────────────────────────────────────────────────

const SetupJobPanel = () => {
  const { user } = useAuth();
  // Prefer userId from auth context; fall back to localStorage for sessions without new JWT
  const _rawId = user?.userId != null ? user.userId : Number(localStorage.getItem('userId') || '0');
  const userId = _rawId > 0 ? _rawId : null;

  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');

  // Complete dialog state
  const [completeJob, setCompleteJob] = useState(null);
  const [checklist, setChecklist] = useState(BLANK_CHECKLIST);
  const [completing, setCompleting] = useState(false);

  // Skip dialog state
  const [skipJob, setSkipJob] = useState(null);
  const [skipReason, setSkipReason] = useState('');
  const [skipping, setSkipping] = useState(false);

  // Snackbar
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'success' });
  const showSnack = (message, severity = 'success') =>
    setSnack({ open: true, message, severity });

  const loadJobs = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setLoadError('');
    try {
      const data = await getPendingSetupJobs(userId);
      setJobs(Array.isArray(data) ? data : []);
    } catch (err) {
      setLoadError(`โหลดงาน Setup ไม่สำเร็จ: ${err?.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => { loadJobs(); }, [loadJobs]);

  // ── Start → immediately open complete dialog ────────────────────────────────
  const handleStart = async (job) => {
    try {
      await startSetup(job.id);
      setChecklist(BLANK_CHECKLIST);
      setCompleteJob({ ...job, status: 'IN_PROGRESS' });
    } catch (err) {
      showSnack(`เริ่ม Setup ไม่สำเร็จ: ${err?.response?.data?.message || err.message}`, 'error');
    }
  };

  const openComplete = (job) => {
    setChecklist(BLANK_CHECKLIST);
    setCompleteJob(job);
  };

  const handleComplete = async () => {
    if (!completeJob) return;
    setCompleting(true);
    try {
      await completeSetup(completeJob.id, checklist);
      showSnack('Setup เสร็จสิ้น — ระบบบันทึก Downtime Event เพื่อคำนวณ OEE แล้ว');
      setCompleteJob(null);
      loadJobs();
    } catch (err) {
      showSnack(`บันทึกไม่สำเร็จ: ${err?.response?.data?.message || err.message}`, 'error');
    } finally {
      setCompleting(false);
    }
  };

  const handleSkip = async () => {
    if (!skipJob) return;
    if (!skipReason.trim()) {
      showSnack('กรุณาระบุเหตุผลที่ข้าม', 'warning');
      return;
    }
    setSkipping(true);
    try {
      await skipSetup(skipJob.id, skipReason.trim());
      showSnack('ข้าม Setup แล้ว');
      setSkipJob(null);
      setSkipReason('');
      loadJobs();
    } catch (err) {
      showSnack(`ข้ามไม่สำเร็จ: ${err?.response?.data?.message || err.message}`, 'error');
    } finally {
      setSkipping(false);
    }
  };

  const setCheck = (key) => (e) =>
    setChecklist((prev) => ({ ...prev, [key]: e.target.checked }));
  const setVal = (key) => (e) =>
    setChecklist((prev) => ({ ...prev, [key]: e.target.value }));

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <Box sx={{ mb: 3 }}>

      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
        <BuildIcon color="warning" />
        <Typography variant="h6" fontWeight={700}>งาน Setup เครื่องจักร</Typography>
        <Tooltip title="รีเฟรช">
          <span>
            <IconButton size="small" onClick={loadJobs} disabled={loading}>
              {loading ? <CircularProgress size={16} /> : <RefreshIcon />}
            </IconButton>
          </span>
        </Tooltip>
        {jobs.length > 0 && (
          <Chip label={`${jobs.length} งาน`} color="warning" size="small" />
        )}
      </Box>

      {loadError && <Alert severity="error" sx={{ mb: 1.5 }}>{loadError}</Alert>}

      {!loading && jobs.length === 0 && !loadError && (
        <Alert severity="info" sx={{ mb: 1 }}>ไม่มีงาน Setup ที่รอดำเนินการ</Alert>
      )}

      {/* Job cards */}
      {jobs.map((job) => (
        <Card
          key={job.id}
          variant="outlined"
          sx={{ mb: 1.5, borderLeft: '4px solid', borderColor: 'warning.main' }}
        >
          <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
              <Box>
                <Typography variant="subtitle1" fontWeight={700}>
                  {job.machineName || job.machineCode || `Machine ${job.machineId}`}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  วันที่: {job.planDate}
                  &nbsp;&nbsp;|&nbsp;&nbsp;
                  <strong>{job.fromProductCode || '—'}</strong>
                  {' → '}
                  <strong>{job.toProductCode || '—'}</strong>
                </Typography>
                {job.requiredBefore && (
                  <Typography variant="caption" color="error.main">
                    ต้องเสร็จก่อน {job.requiredBefore}
                  </Typography>
                )}
              </Box>
              <Chip
                label={job.status}
                color={STATUS_COLOR[job.status] || 'default'}
                size="small"
              />
            </Box>

            <Divider sx={{ mb: 1 }} />

            <Stack direction="row" spacing={1}>
              {job.status === 'PENDING' && (
                <Button
                  variant="contained"
                  size="small"
                  startIcon={<BuildIcon />}
                  onClick={() => handleStart(job)}
                >
                  เริ่ม Setup
                </Button>
              )}
              {job.status === 'IN_PROGRESS' && (
                <Button
                  variant="contained"
                  color="success"
                  size="small"
                  startIcon={<CheckCircleOutlineIcon />}
                  onClick={() => openComplete(job)}
                >
                  เสร็จสิ้น
                </Button>
              )}
              <Button
                variant="outlined"
                size="small"
                color="inherit"
                startIcon={<SkipNextIcon />}
                onClick={() => { setSkipJob(job); setSkipReason(''); }}
              >
                ข้าม
              </Button>
            </Stack>
          </CardContent>
        </Card>
      ))}

      {/* ── Complete / Checklist Dialog ─────────────────────────────────────── */}
      <Dialog
        open={Boolean(completeJob)}
        onClose={() => !completing && setCompleteJob(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ pb: 0.5 }}>
          ✅ บันทึกผล Setup
          <Typography variant="body2" color="text.secondary">
            {completeJob?.machineName} &nbsp;|&nbsp;
            {completeJob?.fromProductCode || '—'} → {completeJob?.toProductCode || '—'}
          </Typography>
        </DialogTitle>

        <DialogContent dividers>
          <Stack spacing={1.5}>

            {/* Mold */}
            <FormControlLabel
              control={
                <Checkbox checked={checklist.moldChanged} onChange={setCheck('moldChanged')} />
              }
              label="เปลี่ยน Mold"
            />
            {checklist.moldChanged && (
              <Stack direction="row" spacing={1}>
                <TextField
                  size="small"
                  label="Mold Code เดิม"
                  value={checklist.moldCodeFrom}
                  onChange={setVal('moldCodeFrom')}
                  sx={{ flex: 1 }}
                />
                <TextField
                  size="small"
                  label="Mold Code ใหม่"
                  value={checklist.moldCodeTo}
                  onChange={setVal('moldCodeTo')}
                  sx={{ flex: 1 }}
                />
              </Stack>
            )}

            {/* Adjustments */}
            <FormControlLabel
              control={
                <Checkbox checked={checklist.tempAdjusted} onChange={setCheck('tempAdjusted')} />
              }
              label="ปรับอุณหภูมิ (Temperature)"
            />
            <FormControlLabel
              control={
                <Checkbox checked={checklist.cycleAdjusted} onChange={setCheck('cycleAdjusted')} />
              }
              label="ปรับ Cycle Time"
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={checklist.blowPinAligned}
                  onChange={setCheck('blowPinAligned')}
                />
              }
              label="จัดตำแหน่ง Blow Pin"
            />
            <FormControlLabel
              control={
                <Checkbox checked={checklist.fpiPassed} onChange={setCheck('fpiPassed')} />
              }
              label="First Piece Inspection (FPI) ผ่าน ✓"
            />

            {/* Notes */}
            <TextField
              multiline
              rows={2}
              size="small"
              label="หมายเหตุ"
              fullWidth
              value={checklist.notes}
              onChange={setVal('notes')}
            />
          </Stack>
        </DialogContent>

        <DialogActions>
          <Button onClick={() => setCompleteJob(null)} disabled={completing}>
            ยกเลิก
          </Button>
          <Button
            variant="contained"
            color="success"
            onClick={handleComplete}
            disabled={completing}
            startIcon={completing ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {completing ? 'กำลังบันทึก…' : 'เสร็จสิ้น'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── Skip Dialog ─────────────────────────────────────────────────────── */}
      <Dialog
        open={Boolean(skipJob)}
        onClose={() => !skipping && setSkipJob(null)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>
          ข้ามงาน Setup
          <Typography variant="body2" color="text.secondary">
            {skipJob?.machineName}
          </Typography>
        </DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="เหตุผลที่ข้าม *"
            sx={{ mt: 1 }}
            value={skipReason}
            onChange={(e) => setSkipReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSkipJob(null)} disabled={skipping}>
            ยกเลิก
          </Button>
          <Button
            variant="contained"
            color="warning"
            onClick={handleSkip}
            disabled={skipping}
            startIcon={skipping ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {skipping ? 'กำลังบันทึก…' : 'ยืนยันข้าม'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── Snackbar ────────────────────────────────────────────────────────── */}
      <Snackbar
        open={snack.open}
        autoHideDuration={4000}
        onClose={() => setSnack((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          severity={snack.severity}
          onClose={() => setSnack((s) => ({ ...s, open: false }))}
          sx={{ width: '100%' }}
        >
          {snack.message}
        </Alert>
      </Snackbar>

    </Box>
  );
};

export default SetupJobPanel;
