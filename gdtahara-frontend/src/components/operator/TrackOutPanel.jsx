import React, { useState, useRef, useEffect } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Paper,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import LocalPrintshopIcon from '@mui/icons-material/LocalPrintshop';
import { getSubLotByNumber, markLabeled, printLabel } from '../../api/phase1Api';

const MAX_LOG = 30;

const subLotStatusColor = (status) => {
  const s = String(status || '').toUpperCase();
  if (s === 'LABELED') return 'success';
  if (s === 'CONFIRMED') return 'primary';
  if (s === 'PENDING') return 'warning';
  return 'default';
};

const TrackOutPanel = ({ onBack }) => {
  const [inputValue, setInputValue] = useState('');
  const [subLot, setSubLot] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [acting, setActing] = useState(false);
  const [scanLog, setScanLog] = useState([]);
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });
  const inputRef = useRef(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const refocus = () => setTimeout(() => inputRef.current?.focus(), 80);

  const showSnack = (message, severity) => setSnack({ open: true, message, severity });

  const pushLog = (number, result, ok) =>
    setScanLog(prev => [
      { number, result, ok, time: new Date().toLocaleTimeString('th-TH') },
      ...prev.slice(0, MAX_LOG - 1),
    ]);

  const clearInput = () => {
    setInputValue('');
    setSubLot(null);
  };

  const handleScan = async () => {
    const val = inputValue.trim();
    if (!val) return;
    setScanning(true);
    setSubLot(null);
    try {
      const data = await getSubLotByNumber(val);
      setSubLot(data);
      pushLog(val, `พบ: ${data.subLotNumber || val}`, true);
      setInputValue('');
    } catch (err) {
      if (err?.response?.status === 404) {
        showSnack(`ไม่พบกล่อง "${val}"`, 'error');
        pushLog(val, 'ไม่พบ', false);
      } else {
        showSnack(`เกิดข้อผิดพลาด: ${err?.response?.data?.message || err.message}`, 'error');
        pushLog(val, 'Error', false);
      }
      setInputValue('');
      refocus();
    } finally {
      setScanning(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') handleScan();
  };

  const handlePrint = async () => {
    if (!subLot) return;
    setActing(true);
    try {
      await printLabel(subLot.id, null);
      showSnack('ส่งงานพิมพ์สำเร็จ', 'success');
      pushLog(subLot.subLotNumber, 'Print OK', true);
    } catch (err) {
      showSnack(`พิมพ์ไม่สำเร็จ: ${err?.response?.data?.message || err.message}`, 'error');
    } finally {
      setActing(false);
      clearInput();
      refocus();
    }
  };

  const handleMarkLabeled = async () => {
    if (!subLot) return;
    setActing(true);
    try {
      await markLabeled(subLot.id, null);
      showSnack('บันทึก Labeled สำเร็จ', 'success');
      pushLog(subLot.subLotNumber, 'Labeled OK', true);
    } catch (err) {
      showSnack(`บันทึกไม่สำเร็จ: ${err?.response?.data?.message || err.message}`, 'error');
    } finally {
      setActing(false);
      clearInput();
      refocus();
    }
  };

  return (
    <Box sx={{ p: 2, maxWidth: 680, mx: 'auto' }}>

      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
        {onBack && (
          <IconButton onClick={onBack} size="small">
            <ArrowBackIcon />
          </IconButton>
        )}
        <Typography variant="h5" fontWeight={600}>
          Track-Out — สแกน Barcode
        </Typography>
      </Box>

      {/* Barcode input */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          สแกน Barcode หรือพิมพ์เลขกล่อง แล้วกด Enter (หรือปุ่มค้นหา)
        </Typography>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <TextField
            inputRef={inputRef}
            fullWidth
            label="เลขกล่อง / Sub-Lot Number"
            placeholder="สแกนหรือพิมพ์แล้วกด Enter…"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            autoFocus
            autoComplete="off"
            disabled={scanning || acting}
            size="medium"
          />
          <Button
            variant="contained"
            onClick={handleScan}
            disabled={!inputValue.trim() || scanning || acting}
            sx={{ minWidth: 100, height: 56 }}
          >
            {scanning ? <CircularProgress size={20} color="inherit" /> : 'ค้นหา'}
          </Button>
        </Stack>
      </Paper>

      {/* Sub-lot detail */}
      {subLot && (
        <Card sx={{ mb: 3, borderLeft: '4px solid', borderColor: 'primary.main' }}>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
              <Typography variant="h6" fontWeight={700}>
                {subLot.subLotNumber}
              </Typography>
              <Chip
                label={subLot.status || 'UNKNOWN'}
                color={subLotStatusColor(subLot.status)}
                size="small"
              />
            </Box>

            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
                gap: 1,
                mb: 2,
              }}
            >
              {[
                { label: 'จำนวน (ชิ้น)', value: subLot.boxQuantity ?? subLot.quantity ?? '-' },
                { label: 'น้ำหนัก', value: subLot.weightKg != null ? `${subLot.weightKg} kg` : '-' },
                { label: 'Pallet', value: subLot.palletNumber ?? subLot.pallet ?? '-' },
                { label: 'Lot', value: subLot.lotNumber ?? '-' },
              ].map(({ label, value }) => (
                <Box
                  key={label}
                  sx={{ p: 1, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}
                >
                  <Typography variant="caption" color="text.secondary" display="block">
                    {label}
                  </Typography>
                  <Typography variant="body1" fontWeight={600}>
                    {value}
                  </Typography>
                </Box>
              ))}
            </Box>

            <Divider sx={{ mb: 2 }} />

            <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
              <Button
                variant="contained"
                startIcon={acting ? <CircularProgress size={16} color="inherit" /> : <LocalPrintshopIcon />}
                onClick={handlePrint}
                disabled={acting}
              >
                Print Label
              </Button>
              <Button
                variant="outlined"
                color="success"
                startIcon={acting ? <CircularProgress size={16} color="inherit" /> : <CheckCircleOutlineIcon />}
                onClick={handleMarkLabeled}
                disabled={acting}
              >
                Mark Labeled
              </Button>
              <Box sx={{ flexGrow: 1 }} />
              <Button
                variant="text"
                size="small"
                onClick={() => { clearInput(); refocus(); }}
                disabled={acting}
              >
                ยกเลิก
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* Scan log */}
      {scanLog.length > 0 && (
        <Paper variant="outlined">
          <Box sx={{ px: 2, py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography variant="subtitle2" color="text.secondary">
              ประวัติการสแกน ({scanLog.length})
            </Typography>
          </Box>
          <List dense disablePadding>
            {scanLog.map((entry, i) => (
              <ListItem
                key={i}
                divider={i < scanLog.length - 1}
                sx={{ py: 0.5 }}
                secondaryAction={
                  <Typography variant="caption" color="text.secondary">
                    {entry.time}
                  </Typography>
                }
              >
                <ListItemText
                  primary={
                    <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="body2" fontWeight={500} component="span">
                        {entry.number}
                      </Typography>
                      <Chip
                        label={entry.result}
                        size="small"
                        color={entry.ok ? 'success' : 'error'}
                        variant="outlined"
                        sx={{ fontSize: '0.7rem', height: 20 }}
                      />
                    </Box>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      {/* Snackbar */}
      <Snackbar
        open={snack.open}
        autoHideDuration={3000}
        onClose={() => setSnack(s => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          severity={snack.severity}
          onClose={() => setSnack(s => ({ ...s, open: false }))}
          sx={{ width: '100%' }}
        >
          {snack.message}
        </Alert>
      </Snackbar>

    </Box>
  );
};

export default TrackOutPanel;
