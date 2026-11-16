import React, { useState, useEffect, useCallback } from 'react';
import { Box, Typography, Chip, Alert } from '@mui/material';
import WasteEntryForm from './WasteEntryForm';
import WasteHistoryTable from './WasteHistoryTable';
import { getMyWasteRecords } from '../../api/cmWasteApi';
import { getCurrentShift } from '../../api/cmMeterApi';
import { useAuth } from '../../App';

export default function CmWasteHub() {
  const { user } = useAuth();
  const [records, setRecords] = useState([]);
  const [shift, setShift] = useState(null);
  const [error, setError] = useState('');

  const loadRecords = useCallback(() => {
    getMyWasteRecords()
      .then(setRecords)
      .catch(() => setError('โหลดประวัติไม่สำเร็จ'));
  }, []);

  useEffect(() => { loadRecords(); }, [loadRecords]);
  useEffect(() => {
    getCurrentShift().then(setShift).catch(() => {});
  }, []);

  return (
    <Box sx={{ p: 2, maxWidth: 960, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3, gap: 1.5, flexWrap: 'wrap' }}>
        <Typography variant="h6" fontWeight={700}>บันทึกของเสีย</Typography>
        <Chip label="C-02" size="small" color="secondary" />
        {shift && (
          <Chip
            label={`กะ ${shift.shiftCode} · ${shift.shiftDate} · ${shift.startTime}–${shift.endTime}`}
            size="small"
            variant="outlined"
            color="info"
          />
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap', alignItems: 'flex-start' }}>
        <WasteEntryForm onSaved={loadRecords} />
        <Box sx={{ flex: 1, minWidth: 320 }}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
            รายการของฉัน — กะปัจจุบัน
          </Typography>
          <WasteHistoryTable records={records} onRefresh={loadRecords} currentUsername={user?.username} />
        </Box>
      </Box>
    </Box>
  );
}
