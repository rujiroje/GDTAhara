import React, { useState, useEffect, useRef } from 'react';
import { SimpleTreeView, TreeItem } from '@mui/x-tree-view';
import {
  Alert, Box, Button, Chip, CircularProgress,
  Dialog, DialogActions, DialogContent, DialogTitle,
  Stack, TextField, Typography,
} from '@mui/material';
import {
  getSubLotsForReport,
  getBarcodeBlob,
  getZpl,
  printLabel,
} from '../../api/phase1Api';

// ── helpers ──────────────────────────────────────────────────────────────────

const statusColor = (status) => {
  if (status === 'labeled') return 'success';
  return 'default';
};

// ── SubLotTree ────────────────────────────────────────────────────────────────

const SubLotTree = ({ productionReportId, parentLotNumber, refreshKey }) => {
  const [subLots, setSubLots]     = useState([]);
  const [loading, setLoading]     = useState(false);
  const [loadError, setLoadError] = useState(null);

  // Dialog
  const [open, setOpen]               = useState(false);
  const [selected, setSelected]       = useState(null);
  const [barcodeUrl, setBarcodeUrl]   = useState(null);
  const [zplText, setZplText]         = useState(null);
  const [showZpl, setShowZpl]         = useState(false);
  const [printerTarget, setPrinterTarget] = useState('');
  const [printing, setPrinting]       = useState(false);
  const [printMsg, setPrintMsg]       = useState(null);

  // Keep latest objectURL in a ref so cleanup always revokes the current value
  const barcodeRef = useRef(null);

  // ── Load sub-lots ─────────────────────────────────────────────────────────

  useEffect(() => {
    if (!productionReportId) return;
    setLoading(true);
    setLoadError(null);
    getSubLotsForReport(productionReportId)
      .then(data => setSubLots(Array.isArray(data) ? data : []))
      .catch(err => setLoadError(err?.response?.data?.message || 'โหลดข้อมูลล้มเหลว'))
      .finally(() => setLoading(false));
  }, [productionReportId, refreshKey]);

  // ── Cleanup objectURL on unmount ──────────────────────────────────────────

  useEffect(() => {
    return () => {
      if (barcodeRef.current) URL.revokeObjectURL(barcodeRef.current);
    };
  }, []);

  // ── Dialog handlers ───────────────────────────────────────────────────────

  const handleOpen = (subLot) => {
    setSelected(subLot);
    setOpen(true);
    setZplText(null);
    setShowZpl(false);
    setPrintMsg(null);
    getBarcodeBlob(subLot.id).then(url => {
      barcodeRef.current = url;
      setBarcodeUrl(url);
    }).catch(() => {
      setBarcodeUrl(null);
    });
  };

  const handleClose = () => {
    setOpen(false);
    setSelected(null);
    // Revoke via functional update so we always have the latest value
    setBarcodeUrl(prev => {
      if (prev) { URL.revokeObjectURL(prev); barcodeRef.current = null; }
      return null;
    });
  };

  const handleShowZpl = () => {
    if (!selected) return;
    getZpl(selected.id)
      .then(text => { setZplText(text); setShowZpl(true); })
      .catch(() => setZplText('— โหลด ZPL ล้มเหลว —'));
  };

  const handlePrint = () => {
    if (!selected || !printerTarget.trim()) return;
    setPrinting(true);
    setPrintMsg(null);
    printLabel(selected.id, printerTarget.trim())
      .then(() => setPrintMsg({ severity: 'success', text: 'พิมพ์สำเร็จ' }))
      .catch(() => setPrintMsg({ severity: 'error', text: 'พิมพ์ล้มเหลว — ตรวจสอบ host:port' }))
      .finally(() => setPrinting(false));
  };

  // ── Render ────────────────────────────────────────────────────────────────

  if (loading) return <CircularProgress size={28} sx={{ m: 2 }} />;
  if (loadError) return <Alert severity="error" sx={{ m: 1 }}>{loadError}</Alert>;

  const rootLabel = `${parentLotNumber ?? '(ไม่ระบุ Lot)'} — ${subLots.length} กล่อง`;

  return (
    <>
      <SimpleTreeView defaultExpandedItems={['root']}>
        <TreeItem itemId="root" label={rootLabel}>
          {subLots.length === 0 && (
            <TreeItem itemId="empty" label="ยังไม่มีกล่อง" />
          )}
          {subLots.map(sl => (
            <TreeItem
              key={sl.id}
              itemId={`sl-${sl.id}`}
              label={
                <Stack
                  direction="row"
                  spacing={1}
                  alignItems="center"
                  sx={{ cursor: 'pointer', py: 0.5 }}
                  onClick={(e) => { e.stopPropagation(); handleOpen(sl); }}
                >
                  <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                    {sl.subLotNumber}
                  </Typography>
                  <Chip
                    label={sl.status}
                    size="small"
                    color={statusColor(sl.status)}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {sl.boxQuantity} กล่อง{sl.weightKg ? ` · ${sl.weightKg} kg` : ''}
                  </Typography>
                </Stack>
              }
            />
          ))}
        </TreeItem>
      </SimpleTreeView>

      {/* ── Detail Dialog ──────────────────────────────────────────────── */}
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontFamily: 'monospace', fontSize: '1rem' }}>
          {selected?.subLotNumber}
        </DialogTitle>

        <DialogContent dividers>
          <Stack spacing={2}>

            {/* Barcode image */}
            <Box sx={{ textAlign: 'center', minHeight: 80 }}>
              {barcodeUrl
                ? <img src={barcodeUrl} alt="barcode" style={{ maxWidth: '100%' }} />
                : <CircularProgress size={24} />
              }
            </Box>

            {/* ZPL toggle */}
            {!showZpl && (
              <Button variant="outlined" size="small" onClick={handleShowZpl}>
                ดู ZPL
              </Button>
            )}
            {showZpl && (
              <Box
                component="pre"
                sx={{
                  fontSize: '0.65rem', overflowX: 'auto',
                  bgcolor: '#f4f4f4', p: 1, borderRadius: 1, maxHeight: 200,
                }}
              >
                {zplText}
              </Box>
            )}

            {/* Print row */}
            <Stack direction="row" spacing={1} alignItems="center">
              <TextField
                size="small"
                label="Printer (host:port)"
                placeholder="192.168.1.200:9100"
                value={printerTarget}
                onChange={e => setPrinterTarget(e.target.value)}
                sx={{ flex: 1 }}
              />
              <Button
                variant="contained"
                size="small"
                onClick={handlePrint}
                disabled={printing || !printerTarget.trim()}
                sx={{ minWidth: 80 }}
              >
                {printing ? <CircularProgress size={16} color="inherit" /> : 'Print'}
              </Button>
            </Stack>

            {printMsg && (
              <Alert severity={printMsg.severity}>{printMsg.text}</Alert>
            )}
          </Stack>
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose}>ปิด</Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default SubLotTree;
