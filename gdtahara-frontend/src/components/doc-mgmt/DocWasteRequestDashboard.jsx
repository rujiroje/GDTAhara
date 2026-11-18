import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, Button, Chip, CircularProgress, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import { getDocPendingRequests, respondToEditRequest } from '../../api/cmWasteApi';

function RespondDialog({ request, onClose, onDone }) {
  const [approved, setApproved] = useState(null);
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    if (approved === null) { setError('กรุณาเลือก อนุมัติ หรือ ปฏิเสธ'); return; }
    setSubmitting(true);
    setError('');
    try {
      await respondToEditRequest(request.id, { decision: approved ? 'APPROVED' : 'REJECTED', docNote: note });
      onDone();
    } catch (err) {
      setError(err.response?.data?.message || 'ตอบสนองไม่สำเร็จ');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>ตอบสนองคำขอแก้ไข #{request.id}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
        <Typography variant="body2" color="text.secondary">
          ผู้ขอ: {request.requesterId} — เหตุผล: {request.reason}
        </Typography>
        <Typography variant="body2">
          ของเสียใหม่: {request.requestedWasteTypeCode || '—'} — {request.requestedWeightKg ? `${request.requestedWeightKg} kg` : '—'}
        </Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant={approved === true ? 'contained' : 'outlined'}
            color="success"
            startIcon={<CheckIcon />}
            onClick={() => setApproved(true)}
            fullWidth
          >
            อนุมัติ
          </Button>
          <Button
            variant={approved === false ? 'contained' : 'outlined'}
            color="error"
            startIcon={<CloseIcon />}
            onClick={() => setApproved(false)}
            fullWidth
          >
            ปฏิเสธ
          </Button>
        </Box>
        <TextField
          label="หมายเหตุ Doc"
          value={note}
          onChange={e => setNote(e.target.value)}
          multiline rows={2}
          size="small" fullWidth
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={submitting}>ยกเลิก</Button>
        <Button onClick={handleSubmit} variant="contained" disabled={submitting}>
          {submitting ? <CircularProgress size={18} /> : 'ยืนยัน'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function DocWasteRequestDashboard() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [responding, setResponding] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    getDocPendingRequests()
      .then(setRequests)
      .catch(() => setError('โหลดคำขอไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const statusColor = {
    PENDING: '#ed6c02',
    APPROVED: '#2e7d32',
    REJECTED: '#d32f2f',
  };

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>;

  return (
    <Box sx={{ p: 2, maxWidth: 960, mx: 'auto' }}>
      <Typography variant="h6" fontWeight={700} sx={{ mb: 2 }}>คำขอแก้ไขของเสีย</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {responding && (
        <RespondDialog
          request={responding}
          onClose={() => setResponding(null)}
          onDone={() => { setResponding(null); load(); }}
        />
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.50' } }}>
              <TableCell>#</TableCell>
              <TableCell>เวลาขอ</TableCell>
              <TableCell>ผู้ขอ</TableCell>
              <TableCell>เหตุผล</TableCell>
              <TableCell>ของเสียใหม่</TableCell>
              <TableCell>น้ำหนักใหม่</TableCell>
              <TableCell>สถานะ</TableCell>
              <TableCell align="center">ดำเนินการ</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {requests.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                  ไม่มีคำขอที่รอดำเนินการ
                </TableCell>
              </TableRow>
            ) : requests.map(req => (
              <TableRow key={req.id} hover>
                <TableCell>{req.id}</TableCell>
                <TableCell sx={{ fontSize: '0.75rem' }}>
                  {req.requestedAt
                    ? new Date(req.requestedAt).toLocaleString('th-TH', {
                        day: '2-digit', month: '2-digit',
                        hour: '2-digit', minute: '2-digit'
                      })
                    : '—'}
                </TableCell>
                <TableCell>{req.requesterId}</TableCell>
                <TableCell sx={{ maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {req.reason}
                </TableCell>
                <TableCell>{req.requestedWasteTypeCode || '—'}</TableCell>
                <TableCell>{req.requestedWeightKg ? `${req.requestedWeightKg} kg` : '—'}</TableCell>
                <TableCell>
                  <Chip
                    label={req.status}
                    size="small"
                    sx={{
                      bgcolor: (statusColor[req.status] || '#757575') + '18',
                      color: statusColor[req.status] || '#757575',
                      fontWeight: 700,
                      fontSize: '0.65rem'
                    }}
                  />
                </TableCell>
                <TableCell align="center">
                  {req.status === 'PENDING' && (
                    <Button size="small" variant="outlined" onClick={() => setResponding(req)}>
                      ตอบสนอง
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
