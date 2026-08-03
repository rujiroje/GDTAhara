import React, { useState, useEffect, useCallback } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import DeleteIcon from '@mui/icons-material/Delete';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import TimelineIcon from '@mui/icons-material/Timeline';
import {
  getSetupTimeLogs,
  getSetupActivityCodes,
  startTimeLog,
  endTimeLog,
  deleteTimeLog,
  endAllTimeLogs,
  completeSetup,
} from '../../api/phase1Api';

// ── Gantt chart (SVG) ─────────────────────────────────────────────────────────

const GanttChart = ({ timeLogs }) => {
  const ended = timeLogs.filter(l => l.endTime);
  if (!ended.length) return null;

  const toMs = (iso) => new Date(iso).getTime();
  const nowMs = Date.now();

  const allMs = timeLogs.flatMap(l => [
    toMs(l.startTime),
    l.endTime ? toMs(l.endTime) : nowMs,
  ]);
  const minMs = Math.min(...allMs);
  const maxMs = Math.max(...allMs);
  const spanMs = maxMs - minMs || 1;

  const ROW = 26;
  const LABEL = 56;
  const W = 480;
  const H = timeLogs.length * ROW + 28;

  const pct = (ms) => ((ms - minMs) / spanMs) * W;

  const fmt = (iso) =>
    new Date(iso).toLocaleTimeString('th-TH', { hour: '2-digit', minute: '2-digit' });

  const tickCount = Math.min(6, Math.ceil(spanMs / 60000));
  const ticks = Array.from({ length: tickCount + 1 }, (_, i) =>
    minMs + (spanMs * i) / tickCount
  );

  return (
    <Box sx={{ overflowX: 'auto', mt: 1 }}>
      <svg width={LABEL + W + 4} height={H} style={{ display: 'block' }}>
        {/* Tick marks */}
        {ticks.map((t, i) => (
          <g key={i}>
            <line
              x1={LABEL + pct(t)} y1={0}
              x2={LABEL + pct(t)} y2={H - 16}
              stroke="#e0e0e0" strokeWidth={1}
            />
            <text
              x={LABEL + pct(t)} y={H - 4}
              fontSize={9} textAnchor="middle" fill="#999"
            >
              {fmt(t)}
            </text>
          </g>
        ))}

        {/* Log bars */}
        {timeLogs.map((log, i) => {
          const x1 = pct(toMs(log.startTime));
          const x2 = pct(log.endTime ? toMs(log.endTime) : nowMs);
          const barW = Math.max(x2 - x1, 6);
          const y = i * ROW + 4;
          const isRunning = !log.endTime;

          return (
            <g key={log.id}>
              <text x={LABEL - 4} y={y + 16} fontSize={11} textAnchor="end"
                fontWeight={700} fill={log.colorHex || '#607D8B'}>
                {log.activityCode}
              </text>
              <rect
                x={LABEL + x1} y={y}
                width={barW} height={ROW - 6}
                fill={log.colorHex || '#607D8B'}
                opacity={isRunning ? 0.6 : 0.85}
                rx={3}
              />
              {barW > 28 && (
                <text
                  x={LABEL + x1 + barW / 2} y={y + 13}
                  fontSize={9} textAnchor="middle" fill="#fff"
                >
                  {isRunning ? '▶' : `${log.durationMin}m`}
                </text>
              )}
            </g>
          );
        })}
      </svg>
    </Box>
  );
};

// ── Main dialog ───────────────────────────────────────────────────────────────

const SetupTimeLogDialog = ({ job, open, onClose, onCompleted }) => {
  const [timeLogs, setTimeLogs] = useState([]);
  const [activityCodes, setActivityCodes] = useState([]);
  const [selectedCodeId, setSelectedCodeId] = useState('');
  const [description, setDescription] = useState('');
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [adding, setAdding] = useState(false);
  const [completing, setCompleting] = useState(false);
  const [endingAll, setEndingAll] = useState(false);
  const [error, setError] = useState('');
  const [notes, setNotes] = useState('');

  const running = timeLogs.filter(l => !l.endTime);
  const finished = timeLogs.filter(l => l.endTime);
  const totalMin = finished.reduce((s, l) => s + (l.durationMin || 0), 0);

  const load = useCallback(async () => {
    if (!job) return;
    setLoadingLogs(true);
    try {
      const [logs, codes] = await Promise.all([
        getSetupTimeLogs(job.id),
        getSetupActivityCodes(),
      ]);
      setTimeLogs(logs);
      setActivityCodes(codes);
      if (!selectedCodeId && codes.length) setSelectedCodeId(codes[0].id);
    } catch (err) {
      setError(`โหลดข้อมูลไม่สำเร็จ: ${err?.response?.data?.message || err.message}`);
    } finally {
      setLoadingLogs(false);
    }
  }, [job, selectedCodeId]);

  useEffect(() => {
    if (open) { setError(''); setNotes(job?.notes || ''); load(); }
  }, [open, job]);

  const handleAdd = async () => {
    if (!selectedCodeId) { setError('กรุณาเลือก Activity Code'); return; }
    setAdding(true); setError('');
    try {
      await startTimeLog(job.id, selectedCodeId, description);
      setDescription('');
      await load();
    } catch (err) {
      setError(`เริ่ม Activity ไม่สำเร็จ: ${err?.response?.data?.message || err.message}`);
    } finally {
      setAdding(false);
    }
  };

  const handleEnd = async (logId) => {
    setError('');
    try {
      await endTimeLog(job.id, logId);
      await load();
    } catch (err) {
      setError(`จบ Activity ไม่สำเร็จ: ${err?.response?.data?.message || err.message}`);
    }
  };

  const handleDelete = async (logId) => {
    setError('');
    try {
      await deleteTimeLog(job.id, logId);
      await load();
    } catch (err) {
      setError(`ลบไม่สำเร็จ: ${err?.response?.data?.message || err.message}`);
    }
  };

  const handleEndAll = async () => {
    setEndingAll(true); setError('');
    try {
      const logs = await endAllTimeLogs(job.id);
      setTimeLogs(logs);
    } catch (err) {
      setError(`จบทั้งหมดไม่สำเร็จ: ${err?.response?.data?.message || err.message}`);
    } finally {
      setEndingAll(false);
    }
  };

  const handleComplete = async () => {
    if (running.length > 0) {
      setError('ยังมี Activity ที่กำลังทำอยู่ — กรุณากด "จบ" หรือ "จบทั้งหมด" ก่อน');
      return;
    }
    const hasQ = finished.some(l => l.activityCode === 'Q');
    if (!hasQ) {
      setError('กรุณาบันทึก Activity Q (ตรวจคุณภาพ / FPI) ให้เสร็จก่อน จึงจะกด Setup เสร็จสิ้นได้');
      return;
    }
    setCompleting(true); setError('');
    try {
      await completeSetup(job.id, {
        moldChanged: finished.some(l => l.activityCode === 'M'),
        moldCodeFrom: '',
        moldCodeTo: '',
        tempAdjusted: finished.some(l => l.activityCode === 'A'),
        cycleAdjusted: finished.some(l => l.activityCode === 'C'),
        blowPinAligned: false,
        fpiPassed: true,   // Q activity = FPI ผ่าน
        notes,
      });
      onCompleted?.();
      onClose();
    } catch (err) {
      setError(`บันทึกเสร็จสิ้นไม่สำเร็จ: ${err?.response?.data?.message || err.message}`);
    } finally {
      setCompleting(false);
    }
  };

  const fmtTime = (iso) =>
    iso ? new Date(iso).toLocaleTimeString('th-TH', { hour: '2-digit', minute: '2-digit' }) : '—';

  const codeChip = (log) => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
      <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: log.colorHex || '#ccc', flexShrink: 0 }} />
      <Typography variant="body2" fontWeight={700}>{log.activityCode}</Typography>
    </Box>
  );

  return (
    <Dialog open={open} onClose={() => !completing && onClose()} fullWidth maxWidth="md">
      <DialogTitle sx={{ pb: 0.5 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <TimelineIcon color="primary" />
          <Box>
            <Typography variant="h6" component="span">
              {job?.machineName || `Machine ${job?.machineId}`}
              {job?.machineType && (
                <Chip label={job.machineType} size="small" sx={{ ml: 1, fontSize: '0.7rem' }} />
              )}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {job?.fromProductCode || '—'} → {job?.toProductCode || '—'}
              {totalMin > 0 && (
                <Typography component="span" sx={{ ml: 2 }} color="success.main">
                  รวม {totalMin} นาที
                </Typography>
              )}
            </Typography>
          </Box>
        </Box>
      </DialogTitle>

      <DialogContent dividers sx={{ p: { xs: 1.5, sm: 2 } }}>
        {error && <Alert severity="error" sx={{ mb: 1.5 }} onClose={() => setError('')}>{error}</Alert>}

        {/* ── Quick-add form ──────────────────────────────────────────────── */}
        <Box sx={{
          p: 1.5, mb: 2, borderRadius: 1,
          bgcolor: 'action.hover', border: '1px solid', borderColor: 'divider',
        }}>
          <Typography variant="subtitle2" gutterBottom>เพิ่ม Activity</Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="flex-start">
            <FormControl size="small" sx={{ minWidth: 200, flexShrink: 0 }}>
              <InputLabel>Activity Code *</InputLabel>
              <Select
                value={selectedCodeId}
                label="Activity Code *"
                onChange={(e) => setSelectedCodeId(e.target.value)}
              >
                {activityCodes.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: c.colorHex, flexShrink: 0 }} />
                      <strong>{c.code}</strong>&nbsp;—&nbsp;{c.descriptionTh}
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              size="small"
              placeholder="หมายเหตุ (ถ้ามี)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              sx={{ flex: 1 }}
            />
            <Button
              variant="contained"
              size="small"
              startIcon={adding ? <CircularProgress size={14} color="inherit" /> : <PlayArrowIcon />}
              onClick={handleAdd}
              disabled={adding || !selectedCodeId}
              sx={{ flexShrink: 0 }}
            >
              เริ่มจับเวลา
            </Button>
          </Stack>
        </Box>

        {loadingLogs ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            {/* ── Running entries ──────────────────────────────────────── */}
            {running.length > 0 && (
              <Box sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Chip
                    icon={<AccessTimeIcon />}
                    label={`กำลังทำ ${running.length} Activity`}
                    color="warning" size="small"
                  />
                  <Button
                    size="small" variant="outlined" color="warning"
                    startIcon={endingAll ? <CircularProgress size={12} /> : <StopIcon />}
                    onClick={handleEndAll} disabled={endingAll}
                  >
                    จบทั้งหมด
                  </Button>
                </Box>
                {running.map((log) => (
                  <Box key={log.id} sx={{
                    display: 'flex', alignItems: 'center', gap: 1, mb: 0.5,
                    p: 1, borderRadius: 1,
                    border: '1px solid', borderColor: 'warning.light',
                    bgcolor: 'rgba(237, 108, 2, 0.08)',
                  }}>
                    {codeChip(log)}
                    <Typography variant="body2" color="text.secondary" sx={{ flex: 1 }}>
                      {log.activityDescTh}
                      {log.description && ` — ${log.description}`}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      เริ่ม {fmtTime(log.startTime)}
                    </Typography>
                    <Tooltip title="จบ Activity นี้">
                      <IconButton size="small" color="success" onClick={() => handleEnd(log.id)}>
                        <StopIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="ลบ">
                      <IconButton size="small" color="error" onClick={() => handleDelete(log.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Box>
                ))}
              </Box>
            )}

            {/* ── Completed entries table ──────────────────────────────── */}
            {finished.length > 0 && (
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" gutterBottom>Activity ที่เสร็จแล้ว</Typography>
                <Box sx={{ overflowX: 'auto' }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ width: 36 }}>#</TableCell>
                        <TableCell>Code</TableCell>
                        <TableCell>รายละเอียด</TableCell>
                        <TableCell align="center">เริ่ม</TableCell>
                        <TableCell align="center">จบ</TableCell>
                        <TableCell align="right">นาที</TableCell>
                        <TableCell />
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {finished.map((log) => (
                        <TableRow key={log.id} hover>
                          <TableCell sx={{ color: 'text.secondary', fontSize: '0.75rem' }}>
                            {log.sequenceNo}
                          </TableCell>
                          <TableCell>{codeChip(log)}</TableCell>
                          <TableCell>
                            <Typography variant="body2">{log.activityDescTh}</Typography>
                            {log.description && (
                              <Typography variant="caption" color="text.secondary">
                                {log.description}
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell align="center">
                            <Typography variant="caption">{fmtTime(log.startTime)}</Typography>
                          </TableCell>
                          <TableCell align="center">
                            <Typography variant="caption">{fmtTime(log.endTime)}</Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="body2" fontWeight={700}>
                              {log.durationMin}
                            </Typography>
                          </TableCell>
                          <TableCell sx={{ width: 40 }}>
                            <Tooltip title="ลบ">
                              <IconButton size="small" color="error"
                                onClick={() => handleDelete(log.id)}>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </TableCell>
                        </TableRow>
                      ))}
                      <TableRow>
                        <TableCell colSpan={5} align="right">
                          <Typography variant="body2" fontWeight={700}>รวม</Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography variant="body1" fontWeight={900} color="success.main">
                            {totalMin}
                          </Typography>
                        </TableCell>
                        <TableCell />
                      </TableRow>
                    </TableBody>
                  </Table>
                </Box>
              </Box>
            )}

            {/* ── Gantt chart ──────────────────────────────────────────── */}
            {timeLogs.length > 0 && (
              <Box sx={{ mb: 2 }}>
                <Divider sx={{ mb: 1 }} />
                <Typography variant="subtitle2" gutterBottom>
                  แผนภูมิเวลา (Gantt)
                </Typography>
                <GanttChart timeLogs={timeLogs} />
              </Box>
            )}

            {timeLogs.length === 0 && (
              <Alert severity="info" sx={{ mb: 1.5 }}>
                ยังไม่มี Activity — กดปุ่ม "เริ่มจับเวลา" เพื่อเพิ่ม Activity แรก
              </Alert>
            )}

            {/* ── Notes ────────────────────────────────────────────────── */}
            <Divider sx={{ mb: 1.5 }} />
            <TextField
              size="small" fullWidth multiline rows={2}
              label="หมายเหตุ Setup (รวม)"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 2, py: 1.5 }}>
        <Button onClick={onClose} disabled={completing}>ปิด</Button>
        {running.length > 0 && (
          <Button
            variant="outlined" color="warning"
            startIcon={endingAll ? <CircularProgress size={14} /> : <StopIcon />}
            onClick={handleEndAll} disabled={endingAll || completing}
          >
            จบทั้งหมด
          </Button>
        )}
        <Tooltip title={
          running.length > 0
            ? 'จบ Activity ที่กำลังทำก่อน'
            : !finished.some(l => l.activityCode === 'Q')
              ? 'ต้องมี Activity Q (ตรวจคุณภาพ / FPI) ก่อน'
              : ''
        }>
          <span>
            <Button
              variant="contained" color="success"
              startIcon={completing
                ? <CircularProgress size={14} color="inherit" />
                : <CheckCircleOutlineIcon />}
              onClick={handleComplete}
              disabled={completing || running.length > 0}
            >
              {completing ? 'กำลังบันทึก…' : 'Setup เสร็จสิ้น'}
            </Button>
          </span>
        </Tooltip>
      </DialogActions>
    </Dialog>
  );
};

export default SetupTimeLogDialog;
