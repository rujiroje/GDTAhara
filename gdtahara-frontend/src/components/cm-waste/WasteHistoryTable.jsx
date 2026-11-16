import React, { useState } from 'react';
import {
  Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, Chip, IconButton, Tooltip
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import HistoryIcon from '@mui/icons-material/History';
import SelfEditModal from './SelfEditModal';
import EditRequestModal from './EditRequestModal';

export default function WasteHistoryTable({ records = [], onRefresh }) {
  const [selfEditRecord, setSelfEditRecord] = useState(null);
  const [editReqRecord, setEditReqRecord] = useState(null);

  const statusColor = {
    PENDING: '#ed6c02',
    APPROVED: '#2e7d32',
    REJECTED: '#d32f2f',
  };

  return (
    <>
      {selfEditRecord && (
        <SelfEditModal
          record={selfEditRecord}
          onClose={() => setSelfEditRecord(null)}
          onSaved={() => { setSelfEditRecord(null); onRefresh?.(); }}
        />
      )}
      {editReqRecord && (
        <EditRequestModal
          record={editReqRecord}
          onClose={() => setEditReqRecord(null)}
          onSaved={() => { setEditReqRecord(null); onRefresh?.(); }}
        />
      )}
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow sx={{ '& th': { fontWeight: 700, bgcolor: 'grey.50' } }}>
              <TableCell>เวลา</TableCell>
              <TableCell>เครื่อง</TableCell>
              <TableCell>ประเภท</TableCell>
              <TableCell align="right">น้ำหนัก (kg)</TableCell>
              <TableCell>WO</TableCell>
              <TableCell>สถานะคำขอ</TableCell>
              <TableCell align="center">แก้ไข</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {records.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                  ยังไม่มีรายการในกะนี้
                </TableCell>
              </TableRow>
            ) : records.map(rec => (
              <TableRow key={rec.id} hover>
                <TableCell sx={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>
                  {new Date(rec.recordedAt).toLocaleString('th-TH', {
                    day: '2-digit', month: '2-digit',
                    hour: '2-digit', minute: '2-digit'
                  })}
                </TableCell>
                <TableCell>{rec.machineId}</TableCell>
                <TableCell>
                  <Chip label={rec.wasteTypeCode} size="small" variant="outlined" />
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>
                  {parseFloat(rec.weightKg).toFixed(3)}
                </TableCell>
                <TableCell>{rec.woNumber || '—'}</TableCell>
                <TableCell>
                  {rec.pendingRequestStatus ? (
                    <Chip
                      label={rec.pendingRequestStatus}
                      size="small"
                      sx={{
                        bgcolor: (statusColor[rec.pendingRequestStatus] || '#757575') + '18',
                        color: statusColor[rec.pendingRequestStatus] || '#757575',
                        fontWeight: 700,
                        fontSize: '0.65rem'
                      }}
                    />
                  ) : '—'}
                </TableCell>
                <TableCell align="center">
                  {rec.pendingRequestStatus === 'PENDING' ? (
                    <Tooltip title="รอการอนุมัติอยู่">
                      <span>
                        <IconButton size="small" disabled><EditIcon fontSize="small" /></IconButton>
                      </span>
                    </Tooltip>
                  ) : (
                    <>
                      <Tooltip title="แก้ไขทันที (กะนี้)">
                        <IconButton size="small" onClick={() => setSelfEditRecord(rec)} color="primary">
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="ส่งคำขอแก้ไข">
                        <IconButton size="small" onClick={() => setEditReqRecord(rec)} color="warning">
                          <HistoryIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}
