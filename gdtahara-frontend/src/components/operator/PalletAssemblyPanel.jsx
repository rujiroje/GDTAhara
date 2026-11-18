import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Paper,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
  LinearProgress,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import QrCodeScannerIcon from '@mui/icons-material/QrCodeScanner';
import PrintIcon from '@mui/icons-material/Print';
import DeleteIcon from '@mui/icons-material/Delete';
import {
  createPallet,
  scanBoxIntoPallet,
  removeBoxFromPallet,
  closePallet,
  markPalletPrinted,
  listPalletsByDate,
  getPallet,
  getPalletLabel,
  rearrangePallet,
} from '../../api/phase1Api';
import PalletLabelPrint from './PalletLabelPrint';

// sub-view: 'home' → 'create' → 'scanning' → 'preview' → 'print'

const PalletAssemblyPanel = ({ report, onBack }) => {
  const [subView, setSubView] = useState('home');
  const [todayPallets, setTodayPallets] = useState([]);
  const [loadingOpen, setLoadingOpen] = useState(false);

  // create form
  const [palletNumber, setPalletNumber] = useState('');
  const [creating, setCreating] = useState(false);

  // active pallet
  const [pallet, setPallet] = useState(null);
  const [scanInput, setScanInput] = useState('');
  const [scanning, setScanning] = useState(false);
  const [removingId, setRemovingId] = useState(null);
  const [closing, setClosing] = useState(false);
  const [labelData, setLabelData] = useState(null);
  const [printing, setPrinting] = useState(false);

  // rearrange form
  const [rearrangeMode, setRearrangeMode] = useState(false);
  const [newPalletNum, setNewPalletNum] = useState('');
  const [rearrangeReason, setRearrangeReason] = useState('');
  const [rearranging, setRearranging] = useState(false);

  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });
  const scanRef = useRef(null);

  // productId and productName come from the selected report
  const reportProductId = report?.productId;
  const reportProductName = report?.productName || '';
  const machineName = report?.machineName || '';

  const showSnack = (msg, sev = 'info') => setSnack({ open: true, message: msg, severity: sev });

  // load today's pallets for this product (OPEN + CLOSED + PRINTED)
  const refreshOpen = useCallback(() => {
    setLoadingOpen(true);
    const today = new Date().toISOString().slice(0, 10);
    listPalletsByDate(today, reportProductId || undefined)
      .then(data => setTodayPallets(data || []))
      .catch(() => showSnack('โหลด Pallet ของวันนี้ไม่ได้', 'error'))
      .finally(() => setLoadingOpen(false));
  }, [reportProductId]);

  useEffect(() => {
    if (subView === 'home') {
      refreshOpen();
      setRearrangeMode(false);
      setNewPalletNum('');
      setRearrangeReason('');
    }
  }, [subView, refreshOpen]);

  useEffect(() => {
    if (subView === 'scanning') setTimeout(() => scanRef.current?.focus(), 80);
  }, [subView, pallet]);

  // CREATE
  const handleCreate = async () => {
    if (!reportProductId) return showSnack('ไม่พบข้อมูลสินค้าจาก Report', 'error');
    if (!palletNumber.trim()) return showSnack('กรุณาระบุ Pallet Number', 'warning');
    setCreating(true);
    try {
      const data = await createPallet({ productId: Number(reportProductId), palletNumber: palletNumber.trim() });
      setPallet(data);
      setSubView('scanning');
    } catch (err) {
      showSnack(err.response?.data?.message || err.response?.data || 'สร้าง Pallet ไม่สำเร็จ', 'error');
    } finally {
      setCreating(false);
    }
  };

  // SCAN
  const handleScan = async (e) => {
    if (e.key !== 'Enter') return;
    const num = scanInput.trim();
    if (!num) return;
    setScanInput('');
    setScanning(true);
    try {
      const updated = await scanBoxIntoPallet(pallet.id, num);
      setPallet(updated);
      showSnack(`เพิ่มกล่อง ${num} ✓`, 'success');
    } catch (err) {
      showSnack(err.response?.data?.message || err.response?.data || `ไม่สามารถเพิ่มกล่อง ${num}`, 'error');
    } finally {
      setScanning(false);
      setTimeout(() => scanRef.current?.focus(), 80);
    }
  };

  // REMOVE BOX
  const handleRemoveBox = async (subLotId, subLotNumber) => {
    if (!window.confirm(`ลบกล่อง ${subLotNumber} ออก?`)) return;
    setRemovingId(subLotId);
    try {
      const updated = await removeBoxFromPallet(pallet.id, subLotId);
      setPallet(updated);
      showSnack(`ลบกล่อง ${subLotNumber} แล้ว`, 'info');
    } catch (err) {
      showSnack(err.response?.data?.message || 'ลบกล่องไม่สำเร็จ', 'error');
    } finally {
      setRemovingId(null);
    }
  };

  // CLOSE
  const handleClose = async () => {
    if (!window.confirm(`ปิด Pallet #${pallet.palletNumber}?`)) return;
    setClosing(true);
    try {
      const updated = await closePallet(pallet.id);
      setPallet(updated);
      setSubView('preview');
    } catch (err) {
      showSnack(err.response?.data?.message || err.response?.data || 'ปิด Pallet ไม่สำเร็จ', 'error');
    } finally {
      setClosing(false);
    }
  };

  // PRINT
  const handlePrint = async () => {
    setPrinting(true);
    try {
      const label = await getPalletLabel(pallet.id);
      setLabelData(label);
      setSubView('print');
    } catch (err) {
      showSnack('โหลดข้อมูล Label ไม่สำเร็จ', 'error');
    } finally {
      setPrinting(false);
    }
  };

  const handleAfterPrint = async () => {
    try { await markPalletPrinted(pallet.id); } catch { /* non-critical */ }
    setPallet(null);
    setSubView('home');
  };

  // REARRANGE
  const handleRearrange = async () => {
    if (!newPalletNum.trim()) return showSnack('กรุณาระบุเลข Pallet ใหม่', 'warning');
    setRearranging(true);
    try {
      const newPallet = await rearrangePallet(pallet.id, newPalletNum.trim(), rearrangeReason.trim());
      setPallet(newPallet);
      setRearrangeMode(false);
      setNewPalletNum('');
      setRearrangeReason('');
      setSubView('scanning');
      showSnack(`ประกอบ Pallet ใหม่ #${newPallet.palletNumber} สำเร็จ`, 'success');
    } catch (err) {
      showSnack(err.response?.data?.message || err.response?.data || 'ประกอบ Pallet ใหม่ไม่สำเร็จ', 'error');
    } finally {
      setRearranging(false);
    }
  };

  const progress = pallet?.targetQty
    ? Math.min(100, Math.round(((pallet.actualQty || 0) / pallet.targetQty) * 100))
    : null;

  const statusColor = (s) => s === 'OPEN' ? 'success' : s === 'CLOSED' ? 'warning' : s === 'REARRANGED' ? 'error' : 'default';

  // ── Context header shared across views ──────────────────────────
  const ContextBadge = () => (
    <Paper variant="outlined" sx={{ px: 2, py: 0.8, mb: 2, bgcolor: '#f5f5f5' }}>
      <Stack direction="row" spacing={3}>
        <Typography variant="body2"><strong>เครื่อง:</strong> {machineName}</Typography>
        <Typography variant="body2"><strong>สินค้า:</strong> {reportProductName}</Typography>
      </Stack>
    </Paper>
  );

  // ══ PRINT ═══════════════════════════════════════════════════════
  if (subView === 'print' && labelData) {
    return <PalletLabelPrint labelData={labelData} onBack={handleAfterPrint} />;
  }

  // ══ PREVIEW ══════════════════════════════════════════════════════
  if (subView === 'preview' && pallet) {
    const isRearranged = pallet.status === 'REARRANGED';
    return (
      <Box>
        <Stack direction="row" alignItems="center" spacing={1} mb={1}>
          <Button startIcon={<ArrowBackIcon />} size="small" variant="outlined"
            onClick={() => { setPallet(null); setRearrangeMode(false); setSubView('home'); }}>กลับ</Button>
          <Typography variant="h6" fontWeight={700}>Pallet #{pallet.palletNumber}</Typography>
          <Chip label={pallet.status} color={statusColor(pallet.status)} size="small" />
        </Stack>
        <ContextBadge />

        {isRearranged && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Pallet นี้ถูกประกอบใหม่แล้ว — ไม่สามารถพิมพ์ label ได้อีก
          </Alert>
        )}
        {pallet.parentPalletNumber && (
          <Alert severity="info" sx={{ mb: 2 }}>
            ประกอบใหม่จาก Pallet #{pallet.parentPalletNumber}
            {pallet.rearrangeReason ? ` (${pallet.rearrangeReason})` : ''}
          </Alert>
        )}

        <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
          <Stack spacing={0.5}>
            <Typography><strong>จำนวนกล่อง:</strong> {pallet.boxCount} กล่อง</Typography>
            <Typography><strong>จำนวนขวด:</strong> {(pallet.actualQty || 0).toLocaleString()} ขวด {pallet.targetQty ? `(เป้า ${pallet.targetQty.toLocaleString()})` : ''}</Typography>
            {pallet.lotDateMin && (
              <Typography><strong>LOT Date:</strong> {pallet.lotDateMin}{pallet.lotDateMax && pallet.lotDateMax !== pallet.lotDateMin ? ` – ${pallet.lotDateMax}` : ''}</Typography>
            )}
          </Stack>
        </Paper>

        <BoxTable boxes={pallet.boxes || []} readOnly />

        {!isRearranged && (
          <Stack direction="row" spacing={1} mt={2} justifyContent="flex-end" flexWrap="wrap">
            <Button variant="outlined" color="warning" size="large"
              onClick={() => { setRearrangeMode(m => !m); setNewPalletNum(''); setRearrangeReason(''); }}>
              ประกอบ Pallet ใหม่
            </Button>
            <Button variant="contained" color="primary" size="large"
              startIcon={printing ? <CircularProgress size={16} /> : <PrintIcon />}
              disabled={printing} onClick={handlePrint}>
              พิมพ์ Label
            </Button>
          </Stack>
        )}

        {rearrangeMode && !isRearranged && (
          <Paper variant="outlined" sx={{ p: 2, mt: 2, borderColor: 'warning.main' }}>
            <Typography variant="subtitle2" fontWeight={700} mb={1.5}>
              ประกอบ Pallet ใหม่ — กล่องทั้งหมดจะถูกย้ายไปยัง Pallet ใหม่
            </Typography>
            <Stack spacing={1.5}>
              <TextField label="เลข Pallet ใหม่" value={newPalletNum}
                onChange={e => setNewPalletNum(e.target.value)}
                placeholder="เช่น P002" size="small" fullWidth autoFocus />
              <TextField label="เหตุผล (ไม่บังคับ)" value={rearrangeReason}
                onChange={e => setRearrangeReason(e.target.value)}
                size="small" fullWidth />
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button variant="outlined" size="small"
                  onClick={() => setRearrangeMode(false)}>ยกเลิก</Button>
                <Button variant="contained" color="warning" size="small"
                  disabled={rearranging || !newPalletNum.trim()} onClick={handleRearrange}>
                  {rearranging ? <CircularProgress size={18} /> : 'ยืนยัน'}
                </Button>
              </Stack>
            </Stack>
          </Paper>
        )}

        <SnackBar snack={snack} setSnack={setSnack} />
      </Box>
    );
  }

  // ══ SCANNING ════════════════════════════════════════════════════
  if (subView === 'scanning' && pallet) {
    return (
      <Box>
        <Stack direction="row" alignItems="center" spacing={1} mb={1} flexWrap="wrap" gap={1}>
          <Button startIcon={<ArrowBackIcon />} size="small" variant="outlined"
            onClick={() => { setPallet(null); setSubView('home'); }}>กลับ</Button>
          <Typography variant="h6" fontWeight={700} flex={1}>Pallet #{pallet.palletNumber}</Typography>
          <Chip label={pallet.status} color={statusColor(pallet.status)} size="small" />
        </Stack>
        <ContextBadge />

        <Paper variant="outlined" sx={{ p: 1.5, mb: 2 }}>
          <Stack direction="row" spacing={3} flexWrap="wrap">
            <Typography variant="body2"><strong>กล่อง:</strong> {pallet.boxCount}</Typography>
            <Typography variant="body2"><strong>ขวด:</strong> {(pallet.actualQty || 0).toLocaleString()}{pallet.targetQty ? ` / ${pallet.targetQty.toLocaleString()}` : ''}</Typography>
          </Stack>
          {progress !== null && (
            <Box mt={1}>
              <LinearProgress variant="determinate" value={progress}
                color={progress >= 100 ? 'success' : progress >= 80 ? 'warning' : 'primary'}
                sx={{ height: 10, borderRadius: 5 }} />
              <Typography variant="caption" color="text.secondary">{progress}%</Typography>
            </Box>
          )}
        </Paper>

        <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: 'action.hover' }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <QrCodeScannerIcon color="primary" />
            <TextField inputRef={scanRef} label="สแกน Barcode กล่อง (กด Enter)"
              value={scanInput} onChange={e => setScanInput(e.target.value)}
              onKeyDown={handleScan} disabled={scanning} size="small" fullWidth
              autoComplete="off" inputProps={{ style: { fontSize: '1.1rem', letterSpacing: 2 } }} />
            {scanning && <CircularProgress size={24} />}
          </Stack>
        </Paper>

        <BoxTable boxes={pallet.boxes || []} onRemove={handleRemoveBox} removingId={removingId} />

        <Box mt={2} display="flex" justifyContent="flex-end">
          <Button variant="contained" color="warning" size="large"
            disabled={closing || (pallet.boxes?.length || 0) === 0} onClick={handleClose}>
            {closing ? <CircularProgress size={20} /> : 'ปิด Pallet'}
          </Button>
        </Box>
        <SnackBar snack={snack} setSnack={setSnack} />
      </Box>
    );
  }

  // ══ CREATE ══════════════════════════════════════════════════════
  if (subView === 'create') {
    return (
      <Box maxWidth={480}>
        <Stack direction="row" alignItems="center" spacing={1} mb={2}>
          <Button startIcon={<ArrowBackIcon />} size="small" variant="outlined"
            onClick={() => setSubView('home')}>กลับ</Button>
          <Typography variant="h6" fontWeight={700}>สร้าง Pallet ใหม่</Typography>
        </Stack>
        <ContextBadge />

        <Stack spacing={2}>
          {/* product is locked from the selected report */}
          <Paper variant="outlined" sx={{ p: 1.5 }}>
            <Typography variant="caption" color="text.secondary">สินค้า (จากเครื่องที่เลือก)</Typography>
            <Typography fontWeight={700}>{reportProductName}</Typography>
          </Paper>

          <TextField label="Pallet Number" value={palletNumber}
            onChange={e => setPalletNumber(e.target.value)}
            placeholder="เช่น P001" fullWidth autoFocus />

          <Button variant="contained" size="large"
            disabled={creating || !palletNumber.trim()} onClick={handleCreate}>
            {creating ? <CircularProgress size={22} /> : 'สร้าง Pallet'}
          </Button>
        </Stack>
        <SnackBar snack={snack} setSnack={setSnack} />
      </Box>
    );
  }

  // ══ HOME ════════════════════════════════════════════════════════
  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} mb={1}>
        <Button startIcon={<ArrowBackIcon />} size="small" variant="outlined" onClick={onBack}>
          กลับ
        </Button>
        <Typography variant="h5" fontWeight={700}>🎁 ประกอบ Pallet</Typography>
      </Stack>
      <ContextBadge />

      <Button variant="contained" size="large" fullWidth
        sx={{ mb: 3, py: 1.8, fontSize: '1.05rem' }}
        onClick={() => { setPalletNumber(''); setSubView('create'); }}>
        + สร้าง Pallet ใหม่
      </Button>

      <Divider sx={{ mb: 2 }}>
        <Typography variant="caption" color="text.secondary">
          Pallet วันนี้ ({reportProductName})
        </Typography>
      </Divider>

      {loadingOpen && <CircularProgress size={24} />}
      {!loadingOpen && todayPallets.length === 0 && (
        <Typography color="text.secondary" textAlign="center" mt={2}>ยังไม่มี Pallet วันนี้</Typography>
      )}

      <Stack spacing={1.5}>
        {todayPallets.map(p => {
          const isOpen = p.status === 'OPEN';
          const handleClick = async () => {
            if (isOpen) {
              setPallet(p);
              setSubView('scanning');
            } else {
              // fetch full pallet (with boxes) for preview/reprint
              try {
                const full = await getPallet(p.id);
                setPallet(full);
                setSubView('preview');
              } catch {
                showSnack('โหลดข้อมูล Pallet ไม่สำเร็จ', 'error');
              }
            }
          };
          return (
            <Paper key={p.id} variant="outlined" sx={{
              p: 1.5, cursor: 'pointer',
              borderColor: isOpen ? 'success.main' : 'warning.main',
              borderWidth: 1.5,
              '&:hover': { bgcolor: 'action.hover' },
            }} onClick={handleClick}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography fontWeight={700}>#{p.palletNumber}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {p.boxCount} กล่อง / {(p.actualQty || 0).toLocaleString()} ขวด
                  </Typography>
                  <Typography variant="caption" color="text.secondary">สร้างโดย: {p.createdByName}</Typography>
                </Box>
                <Stack direction="row" spacing={1} alignItems="center">
                  {!isOpen && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: { xs: 'none', sm: 'block' } }}>
                      📄 คลิกเพื่อพิมพ์ใหม่
                    </Typography>
                  )}
                  <Chip label={p.status} color={statusColor(p.status)} size="small" />
                </Stack>
              </Stack>
            </Paper>
          );
        })}
      </Stack>
      <SnackBar snack={snack} setSnack={setSnack} />
    </Box>
  );
};

// ── BoxTable ─────────────────────────────────────────────────────
const BoxTable = ({ boxes, onRemove, removingId, readOnly }) => {
  if (!boxes || boxes.length === 0)
    return <Typography color="text.secondary" textAlign="center" mt={2}>ยังไม่มีกล่อง</Typography>;
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>#</TableCell>
            <TableCell>Sub-Lot Number</TableCell>
            <TableCell>LOT Date</TableCell>
            <TableCell align="right">จำนวน (ขวด)</TableCell>
            {!readOnly && <TableCell />}
          </TableRow>
        </TableHead>
        <TableBody>
          {boxes.map((box, idx) => (
            <TableRow key={box.subLotId} hover>
              <TableCell>{idx + 1}</TableCell>
              <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{box.subLotNumber}</TableCell>
              <TableCell><Chip label={box.displayLotDate} size="small" variant="outlined" /></TableCell>
              <TableCell align="right">{(box.boxQuantity || 0).toLocaleString()}</TableCell>
              {!readOnly && (
                <TableCell>
                  <Button size="small" color="error" disabled={!!removingId}
                    startIcon={removingId === box.subLotId ? <CircularProgress size={14} /> : <DeleteIcon />}
                    onClick={() => onRemove(box.subLotId, box.subLotNumber)}>ลบ</Button>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
};

// ── Shared Snackbar ───────────────────────────────────────────────
const SnackBar = ({ snack, setSnack }) => (
  <Snackbar open={snack.open} autoHideDuration={3500}
    onClose={() => setSnack(s => ({ ...s, open: false }))}
    anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
    <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>
      {snack.message}
    </Alert>
  </Snackbar>
);

export default PalletAssemblyPanel;
