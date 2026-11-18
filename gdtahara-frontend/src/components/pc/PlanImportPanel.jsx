import React, { useState, useEffect, useRef } from 'react';
import {
  Alert, Box, Button, Card, CardContent, Chip,
  CircularProgress, IconButton, Paper, Stack,
  Table, TableBody, TableCell, TableHead, TableRow,
  TextField, Tooltip, Typography,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { importPlan, getImportLogs } from '../../api/phase1Api';

// ── PlanImportPanel ───────────────────────────────────────────────────────────

const PlanImportPanel = ({ onBack }) => {
  const fileRef = useRef(null);

  const [file, setFile]               = useState(null);
  const [factoryCode, setFactoryCode] = useState('');
  const [importing, setImporting]     = useState(false);
  const [result, setResult]           = useState(null);
  const [importError, setImportError] = useState(null);
  const [logs, setLogs]               = useState([]);
  const [loadingLogs, setLoadingLogs] = useState(false);

  // ── helpers ───────────────────────────────────────────────────────────────

  const loadLogs = async () => {
    setLoadingLogs(true);
    try {
      const data = await getImportLogs(factoryCode || null);
      setLogs(Array.isArray(data) ? data : []);
    } catch {
      setLogs([]); // endpoint may not exist yet — fail silently
    } finally {
      setLoadingLogs(false);
    }
  };

  useEffect(() => { loadLogs(); }, []); // load on mount

  // ── import ────────────────────────────────────────────────────────────────

  const handleImport = async () => {
    if (!file) return;
    setImporting(true);
    setResult(null);
    setImportError(null);
    try {
      const data = await importPlan(file, factoryCode || null);
      setResult(data);
      loadLogs();
    } catch (err) {
      const status = err?.response?.status;
      const msg    = err?.response?.data?.message ?? err?.message ?? 'นำเข้าล้มเหลว';
      setImportError(
        status === 422
          ? `ตรวจจับโครงสร้างไฟล์ไม่ได้ — กรุณาใส่ Factory Code หรือตรวจสอบไฟล์ (${msg})`
          : msg
      );
    } finally {
      setImporting(false);
    }
  };

  const handleFileChange = (e) => {
    setFile(e.target.files[0] || null);
    setResult(null);
    setImportError(null);
  };

  // ── render ────────────────────────────────────────────────────────────────

  return (
    <Box sx={{ p: 2 }}>

      {/* Header */}
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        {onBack && (
          <Button variant="outlined" size="small" onClick={onBack}>
            ← กลับ
          </Button>
        )}
        <Typography variant="h5" fontWeight={600}>
          นำเข้าแผนผลิต (Excel Import)
        </Typography>
      </Stack>

      {/* Upload card */}
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="body2" color="text.secondary">
              รองรับไฟล์ .xlsx จาก PC monthly plan matrix · Factory Code (ถ้าระบุ) จะใช้ก่อน auto-detect
            </Typography>

            {/* Controls row */}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
              {/* Hidden file input */}
              <input
                ref={fileRef}
                type="file"
                accept=".xlsx"
                style={{ display: 'none' }}
                onChange={handleFileChange}
              />
              <Button variant="outlined" onClick={() => fileRef.current.click()} sx={{ minWidth: 140 }}>
                เลือกไฟล์ .xlsx
              </Button>

              {file && (
                <Chip
                  label={file.name}
                  color="primary"
                  variant="outlined"
                  onDelete={() => { setFile(null); setResult(null); setImportError(null); }}
                />
              )}

              <TextField
                size="small"
                label="Factory Code (ไม่บังคับ)"
                placeholder="เช่น TAHARA"
                value={factoryCode}
                onChange={e => setFactoryCode(e.target.value.toUpperCase())}
                inputProps={{ maxLength: 20 }}
                sx={{ minWidth: 180 }}
              />

              <Button
                variant="contained"
                onClick={handleImport}
                disabled={!file || importing}
                sx={{ minWidth: 100 }}
              >
                {importing
                  ? <CircularProgress size={18} color="inherit" />
                  : 'Import'}
              </Button>
            </Stack>

            {/* Error */}
            {importError && (
              <Alert severity="error" onClose={() => setImportError(null)}>
                {importError}
              </Alert>
            )}

            {/* Result summary */}
            {result && (
              <Box>
                <Alert severity="success" sx={{ mb: 1 }}>
                  นำเข้าสำเร็จ — <strong>{result.factoryCode}</strong> / {result.sheetName}
                  &nbsp;·&nbsp;{result.message}
                </Alert>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  <Chip size="small" label={`+${result.rowsAdded} แถวใหม่`}      color="success" />
                  <Chip size="small" label={`~${result.rowsUpdated} อัปเดต`}     color="info" />
                  <Chip size="small" label={`${result.rowsSkippedPast} ข้ามอดีต`}    color="default" />
                  <Chip size="small" label={`${result.rowsSkippedStarted} ข้ามเริ่มแล้ว`} color="warning" />
                </Stack>
                {result.warnings?.length > 0 && (
                  <Alert severity="warning" sx={{ mt: 1 }}>
                    {result.warnings.map((w, i) => <div key={i}>{w}</div>)}
                  </Alert>
                )}
              </Box>
            )}
          </Stack>
        </CardContent>
      </Card>

      {/* Import history table */}
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
        <Typography variant="h6">ประวัติการนำเข้าล่าสุด</Typography>
        <Tooltip title="รีเฟรชประวัติ">
          <span>
            <IconButton size="small" onClick={loadLogs} disabled={loadingLogs}>
              <RefreshIcon fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
        {loadingLogs && <CircularProgress size={16} />}
      </Stack>

      <Paper variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
              <TableCell>ไฟล์</TableCell>
              <TableCell>Factory</TableCell>
              <TableCell align="right">ใหม่</TableCell>
              <TableCell align="right">อัปเดต</TableCell>
              <TableCell align="right">ข้ามอดีต</TableCell>
              <TableCell align="right">ข้ามเริ่ม</TableCell>
              <TableCell>ผู้นำเข้า</TableCell>
              <TableCell>เวลา</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {logs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center" sx={{ py: 3 }}>
                  <Typography variant="body2" color="text.secondary">
                    ยังไม่มีประวัติการนำเข้า
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              logs.map(log => (
                <TableRow key={log.id} hover>
                  <TableCell
                    sx={{ maxWidth: 200, overflow: 'hidden',
                          textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={log.filename}
                  >
                    {log.filename}
                  </TableCell>
                  <TableCell>{log.factoryCode ?? '—'}</TableCell>
                  <TableCell align="right">{log.rowsAdded}</TableCell>
                  <TableCell align="right">{log.rowsUpdated}</TableCell>
                  <TableCell align="right">{log.rowsSkippedPast}</TableCell>
                  <TableCell align="right">{log.rowsSkippedStarted}</TableCell>
                  <TableCell>{log.importedBy?.username ?? '—'}</TableCell>
                  <TableCell>
                    {log.importedAt
                      ? new Date(log.importedAt).toLocaleString('th-TH')
                      : '—'}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </Paper>

    </Box>
  );
};

export default PlanImportPanel;
