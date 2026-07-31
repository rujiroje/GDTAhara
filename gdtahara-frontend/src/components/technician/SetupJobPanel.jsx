import React, { useState, useEffect, useCallback, useRef } from 'react';
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
import CameraAltIcon from '@mui/icons-material/CameraAlt';
import DeleteIcon from '@mui/icons-material/Delete';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import { useAuth } from '../../App';
import {
  completeSetup,
  getPendingSetupJobs,
  skipSetup,
  startSetup,
  getSetupTemplates,
  getJobSteps,
  saveStepResults,
  uploadStepPhoto,
  getStepPhotoUrl,
} from '../../api/phase1Api';

const STATUS_COLOR = {
  PENDING: 'warning',
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  SKIPPED: 'default',
};

// Map template label → legacy boolean key on the job record
const toLegacyKey = (label) => {
  const l = (label || '').toLowerCase();
  if (l.includes('mold')) return 'moldChanged';
  if (l.includes('อุณหภูม') || l.includes('temp')) return 'tempAdjusted';
  if (l.includes('cycle')) return 'cycleAdjusted';
  if (l.includes('blow pin') || l.includes('blowpin')) return 'blowPinAligned';
  if (l.includes('fpi')) return 'fpiPassed';
  return null;
};

const isMoldStep = (label) => (label || '').toLowerCase().includes('mold');

// ─────────────────────────────────────────────────────────────────────────────

const SetupJobPanel = () => {
  const { user } = useAuth();
  const _rawId = user?.userId != null ? user.userId : Number(localStorage.getItem('userId') || '0');
  const userId = _rawId > 0 ? _rawId : null;

  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');

  // Expand/collapse for zones
  const [showAllToday, setShowAllToday] = useState(false);
  const [showDone, setShowDone] = useState(false);

  // Complete dialog
  const [completeJob, setCompleteJob] = useState(null);
  const [templates, setTemplates] = useState([]);
  const [stepResults, setStepResults] = useState({});
  const [moldCodes, setMoldCodes] = useState({ from: '', to: '' });
  const [jobNotes, setJobNotes] = useState('');
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [completing, setCompleting] = useState(false);

  // Skip dialog
  const [skipJob, setSkipJob] = useState(null);
  const [skipReason, setSkipReason] = useState('');
  const [skipping, setSkipping] = useState(false);

  const [snack, setSnack] = useState({ open: false, message: '', severity: 'success' });
  const showSnack = (message, severity = 'success') =>
    setSnack({ open: true, message, severity });

  const fileInputRefs = useRef({});

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

  // ── Open complete dialog ───────────────────────────────────────────────────
  const openComplete = useCallback(async (job) => {
    setCompleteJob(job);
    setMoldCodes({ from: job.moldCodeFrom || '', to: job.moldCodeTo || '' });
    setJobNotes(job.notes || '');
    setStepResults({});
    setTemplates([]);
    setLoadingTemplates(true);
    try {
      const [tpls, existingSteps] = await Promise.all([
        getSetupTemplates(job.machineType || ''),
        getJobSteps(job.id),
      ]);
      setTemplates(tpls);

      const init = {};
      tpls.forEach((t) => {
        const existing = existingSteps.find((s) => s.template?.id === t.id);
        init[t.id] = {
          done: existing?.done ?? false,
          notes: existing?.notes ?? '',
          photoFile: null,
          photoPreview: null,
          existingPhoto: existing?.photoFilename ?? null,
        };
      });
      setStepResults(init);
    } catch (err) {
      showSnack('โหลด Checklist ไม่สำเร็จ', 'error');
    } finally {
      setLoadingTemplates(false);
    }
  }, []);

  const handleStart = async (job) => {
    try {
      await startSetup(job.id);
      await openComplete({ ...job, status: 'IN_PROGRESS' });
    } catch (err) {
      showSnack(`เริ่ม Setup ไม่สำเร็จ: ${err?.response?.data?.message || err.message}`, 'error');
    }
  };

  // ── Step helpers ──────────────────────────────────────────────────────────
  const setStepDone = (templateId, checked) =>
    setStepResults((prev) => ({ ...prev, [templateId]: { ...prev[templateId], done: checked } }));

  const setStepNotes = (templateId, val) =>
    setStepResults((prev) => ({ ...prev, [templateId]: { ...prev[templateId], notes: val } }));

  const handlePhotoSelect = (templateId, file) => {
    if (!file) return;
    const preview = URL.createObjectURL(file);
    setStepResults((prev) => ({
      ...prev,
      [templateId]: { ...prev[templateId], photoFile: file, photoPreview: preview, existingPhoto: null },
    }));
  };

  const clearPhoto = (templateId) => {
    const sr = stepResults[templateId];
    if (sr?.photoPreview) URL.revokeObjectURL(sr.photoPreview);
    setStepResults((prev) => ({
      ...prev,
      [templateId]: { ...prev[templateId], photoFile: null, photoPreview: null, existingPhoto: null },
    }));
  };

  // ── Submit complete ───────────────────────────────────────────────────────
  const handleComplete = async () => {
    if (!completeJob) return;
    setCompleting(true);
    try {
      for (const tpl of templates) {
        const sr = stepResults[tpl.id];
        if (sr?.photoFile) await uploadStepPhoto(completeJob.id, tpl.id, sr.photoFile);
      }

      const stepPayload = templates.map((tpl) => ({
        templateId: tpl.id,
        done: stepResults[tpl.id]?.done ?? false,
        notes: stepResults[tpl.id]?.notes ?? '',
      }));
      await saveStepResults(completeJob.id, stepPayload);

      const legacy = {
        moldChanged: false,
        moldCodeFrom: moldCodes.from,
        moldCodeTo: moldCodes.to,
        tempAdjusted: false,
        cycleAdjusted: false,
        blowPinAligned: false,
        fpiPassed: false,
        notes: jobNotes,
      };
      templates.forEach((tpl) => {
        const key = toLegacyKey(tpl.label);
        if (key) legacy[key] = stepResults[tpl.id]?.done ?? false;
      });

      await completeSetup(completeJob.id, legacy);
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
    if (!skipReason.trim()) { showSnack('กรุณาระบุเหตุผลที่ข้าม', 'warning'); return; }
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

  // ── Partition jobs into priority zones ────────────────────────────────────
  const todayStr = new Date().toLocaleDateString('en-CA'); // YYYY-MM-DD in local tz

  const byRequiredBefore = (a, b) =>
    (a.requiredBefore || '99:99').localeCompare(b.requiredBefore || '99:99');

  const inProgress  = jobs.filter(j => j.status === 'IN_PROGRESS');
  const overdue     = jobs.filter(j => j.status === 'PENDING' && j.planDate < todayStr).sort(byRequiredBefore);
  const todayJobs   = jobs.filter(j => j.status === 'PENDING' && j.planDate >= todayStr).sort(byRequiredBefore);
  const done        = jobs.filter(j => j.status === 'COMPLETED' || j.status === 'SKIPPED');

  const TODAY_LIMIT   = 5;
  const visibleToday  = showAllToday ? todayJobs : todayJobs.slice(0, TODAY_LIMIT);
  const activeCount   = inProgress.length + overdue.length + todayJobs.length;

  // ── Job card renderer ─────────────────────────────────────────────────────
  const renderJobCard = (job, borderColor, isOverdue = false) => (
    <Card
      key={job.id}
      variant="outlined"
      sx={{ mb: 1.5, borderLeft: '4px solid', borderColor }}
    >
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
          <Box>
            <Typography variant="subtitle1" fontWeight={700}>
              {job.machineName || job.machineCode || `Machine ${job.machineId}`}
              {job.machineType && (
                <Chip label={job.machineType} size="small" sx={{ ml: 1, fontSize: '0.7rem' }} />
              )}
              {isOverdue && (
                <Chip label="เลยกำหนด" color="error" size="small" sx={{ ml: 1, fontSize: '0.7rem' }} />
              )}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              วันที่: {job.planDate}
              &nbsp;&nbsp;|&nbsp;&nbsp;
              <strong>{job.fromProductCode || '—'}</strong>
              {' → '}
              <strong>{job.toProductCode || '—'}</strong>
            </Typography>
            {job.requiredBefore && (
              <Typography variant="caption" color={isOverdue ? 'error.main' : 'text.secondary'}>
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
          {(job.status === 'PENDING' || job.status === 'IN_PROGRESS') && (
            <Button
              variant="outlined"
              size="small"
              color="inherit"
              startIcon={<SkipNextIcon />}
              onClick={() => { setSkipJob(job); setSkipReason(''); }}
            >
              ข้าม
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  );

  const ZoneHeader = ({ label, count, color, sx }) => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1, ...sx }}>
      <Chip label={label} color={color} size="small" />
      <Typography variant="caption" color="text.secondary">{count} งาน</Typography>
    </Box>
  );

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <Box sx={{ mb: 3 }}>

      {/* ── Header ─────────────────────────────────────────────────────────── */}
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
        {activeCount > 0 && (
          <Chip label={`${activeCount} งานที่รอ`} color="warning" size="small" />
        )}
      </Box>

      {loadError && <Alert severity="error" sx={{ mb: 1.5 }}>{loadError}</Alert>}

      {!loading && jobs.length === 0 && !loadError && (
        <Alert severity="info" sx={{ mb: 1 }}>ไม่มีงาน Setup ที่รอดำเนินการ</Alert>
      )}

      {/* ── Zone 1: IN_PROGRESS ────────────────────────────────────────────── */}
      {inProgress.length > 0 && (
        <>
          <ZoneHeader label="ทำอยู่" count={inProgress.length} color="primary" />
          {inProgress.map(job => renderJobCard(job, 'primary.main'))}
        </>
      )}

      {/* ── Zone 2: Overdue (เลยกำหนด) ─────────────────────────────────────── */}
      {overdue.length > 0 && (
        <>
          <ZoneHeader
            label="⚠️ เลยกำหนด"
            count={overdue.length}
            color="error"
            sx={{ mt: inProgress.length > 0 ? 2 : 0 }}
          />
          {overdue.map(job => renderJobCard(job, 'error.main', true))}
        </>
      )}

      {/* ── Zone 3: Due Today (วันนี้) ──────────────────────────────────────── */}
      {todayJobs.length > 0 && (
        <>
          <ZoneHeader
            label="วันนี้"
            count={todayJobs.length}
            color="warning"
            sx={{ mt: (inProgress.length + overdue.length) > 0 ? 2 : 0 }}
          />
          {visibleToday.map(job => renderJobCard(job, 'warning.main'))}
          {todayJobs.length > TODAY_LIMIT && (
            <Button
              size="small"
              variant="text"
              endIcon={showAllToday ? <ExpandLessIcon /> : <ExpandMoreIcon />}
              onClick={() => setShowAllToday(p => !p)}
              sx={{ mt: 0.5, mb: 0.5 }}
            >
              {showAllToday
                ? 'แสดงน้อยลง'
                : `แสดงเพิ่มอีก ${todayJobs.length - TODAY_LIMIT} รายการ`}
            </Button>
          )}
        </>
      )}

      {/* ── Zone 4: Done / Skipped ──────────────────────────────────────────── */}
      {done.length > 0 && (
        <>
          <Box
            sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 2, mb: 1, cursor: 'pointer' }}
            onClick={() => setShowDone(p => !p)}
          >
            <Chip
              label={showDone ? '▲ เสร็จ/ข้าม' : '▼ เสร็จ/ข้าม'}
              color="default"
              size="small"
              clickable
            />
            <Typography variant="caption" color="text.secondary">{done.length} งาน</Typography>
          </Box>
          {showDone && done.map(job => renderJobCard(job, 'grey.400'))}
        </>
      )}

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
            {completeJob?.machineName}
            {completeJob?.machineType && ` (${completeJob.machineType})`}
            &nbsp;|&nbsp;
            {completeJob?.fromProductCode || '—'} → {completeJob?.toProductCode || '—'}
          </Typography>
        </DialogTitle>

        <DialogContent dividers>
          {loadingTemplates ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Stack spacing={2}>
              {templates.map((tpl) => {
                const sr = stepResults[tpl.id] || {};
                const photoSrc = sr.photoPreview || (sr.existingPhoto ? getStepPhotoUrl(sr.existingPhoto) : null);

                return (
                  <Box key={tpl.id} sx={{ borderBottom: '1px solid', borderColor: 'divider', pb: 1.5 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            checked={sr.done ?? false}
                            onChange={(e) => setStepDone(tpl.id, e.target.checked)}
                          />
                        }
                        label={
                          <Typography variant="body2">
                            {tpl.label}
                            {tpl.required && (
                              <Typography component="span" color="error" sx={{ ml: 0.5 }}>*</Typography>
                            )}
                          </Typography>
                        }
                      />
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        {photoSrc && (
                          <IconButton size="small" color="error" onClick={() => clearPhoto(tpl.id)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        )}
                        <Tooltip title="แนบรูปถ่าย">
                          <IconButton
                            size="small"
                            color={photoSrc ? 'success' : 'default'}
                            onClick={() => fileInputRefs.current[tpl.id]?.click()}
                          >
                            <CameraAltIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <input
                          type="file"
                          accept="image/*"
                          capture="environment"
                          style={{ display: 'none' }}
                          ref={(el) => { fileInputRefs.current[tpl.id] = el; }}
                          onChange={(e) => handlePhotoSelect(tpl.id, e.target.files?.[0])}
                        />
                      </Box>
                    </Box>

                    {photoSrc && (
                      <Box sx={{ mt: 0.5, ml: 4 }}>
                        <img
                          src={photoSrc}
                          alt="step photo"
                          style={{ maxHeight: 120, maxWidth: '100%', borderRadius: 4, objectFit: 'cover' }}
                        />
                      </Box>
                    )}

                    {isMoldStep(tpl.label) && sr.done && (
                      <Stack direction="row" spacing={1} sx={{ mt: 1, ml: 4 }}>
                        <TextField
                          size="small"
                          label="Mold Code เดิม"
                          value={moldCodes.from}
                          onChange={(e) => setMoldCodes((m) => ({ ...m, from: e.target.value }))}
                          sx={{ flex: 1 }}
                        />
                        <TextField
                          size="small"
                          label="Mold Code ใหม่"
                          value={moldCodes.to}
                          onChange={(e) => setMoldCodes((m) => ({ ...m, to: e.target.value }))}
                          sx={{ flex: 1 }}
                        />
                      </Stack>
                    )}

                    <TextField
                      size="small"
                      placeholder="หมายเหตุขั้นตอนนี้ (ถ้ามี)"
                      fullWidth
                      value={sr.notes ?? ''}
                      onChange={(e) => setStepNotes(tpl.id, e.target.value)}
                      sx={{ mt: 1 }}
                    />
                  </Box>
                );
              })}

              {templates.length === 0 && (
                <Alert severity="warning">ยังไม่มีขั้นตอน Setup — กรุณาให้ Admin เพิ่มขั้นตอนก่อน</Alert>
              )}

              <TextField
                multiline
                rows={2}
                size="small"
                label="หมายเหตุรวม"
                fullWidth
                value={jobNotes}
                onChange={(e) => setJobNotes(e.target.value)}
              />
            </Stack>
          )}
        </DialogContent>

        <DialogActions>
          <Button onClick={() => setCompleteJob(null)} disabled={completing}>ยกเลิก</Button>
          <Button
            variant="contained"
            color="success"
            onClick={handleComplete}
            disabled={completing || loadingTemplates}
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
          <Typography variant="body2" color="text.secondary">{skipJob?.machineName}</Typography>
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
          <Button onClick={() => setSkipJob(null)} disabled={skipping}>ยกเลิก</Button>
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
