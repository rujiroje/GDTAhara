import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Box, Button, TextField, Paper,
    Typography, CircularProgress, Snackbar, Alert,
    Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import PrintIcon from '@mui/icons-material/Print';
import PackagingLabelPrint from './PackagingLabelPrint';
import { printPackagingLabel } from '../utils/printPackagingLabel';

const PackagingRecording = ({ report, onBack }) => {
    const [lotNumber, setLotNumber]     = useState('');
    const [boxNo, setBoxNo]             = useState('');
    const [isLotLocked, setIsLotLocked] = useState(false);
    const [saving, setSaving]           = useState(false);
    const [labelData, setLabelData]     = useState(null);
    const [printOpen, setPrintOpen]     = useState(false);
    const [feedback, setFeedback]       = useState({ open: false, message: '', severity: 'success' });

    // Auto-fill lot number as yymmdd-{machineName} from the selected report
    useEffect(() => {
        if (!report || isLotLocked) return;
        const now = new Date();
        const yy = String(now.getFullYear()).slice(2);
        const mm = String(now.getMonth() + 1).padStart(2, '0');
        const dd = String(now.getDate()).padStart(2, '0');
        const machine = report.machineName ? `-${report.machineName}` : '';
        setLotNumber(`${yy}${mm}${dd}${machine}`);
        setBoxNo('');
    }, [report]);

    // Fetch estimated next box number for display only (not sent to server)
    useEffect(() => {
        if (!report || !lotNumber || isLotLocked) return;
        const timer = setTimeout(() => {
            axiosInstance.get(`/operator/reports/${report.id}/next-box-no?lotNumber=${lotNumber}`)
                .then(res => setBoxNo(String(res.data)))
                .catch(() => {});
        }, 400);
        return () => clearTimeout(timer);
    }, [lotNumber, report, isLotLocked]);

    const handleRecord = async () => {
        if (!lotNumber) {
            setFeedback({ open: true, message: 'กรุณากรอก Lot Number', severity: 'warning' });
            return;
        }
        setSaving(true);
        try {
            // ส่งเฉพาะ lotNumber — server generate boxNo atomically เอง
            const [saveRes, labelRes] = await Promise.all([
                axiosInstance.post(`/operator/reports/${report.id}/packaging-logs`, { lotNumber }),
                axiosInstance.get(`/operator/reports/${report.id}/label-data`),
            ]);

            // ใช้ boxNo จริงที่ server ยืนยัน (ไม่ใช่ estimate)
            const confirmedBoxNo = saveRes.data?.boxNo;

            const ld = labelRes.data ?? {};
            const built = {
                productName:     ld.productName     ?? report.productName ?? '',
                productCode:     ld.productCode     ?? '',
                customerCode:    ld.customerCode    ?? '',
                labelVariant:    ld.labelVariant    ?? '',
                qtyPerBox:       ld.qtyPerBox       ?? '',
                parentLotNumber: ld.parentLotNumber ?? report.parentLotNumber ?? '',
                lotNumber,
                boxNo:           confirmedBoxNo,
                machineName:     ld.machineName     ?? report.machineName ?? '',
                operatorName:    saveRes.data?.operator?.username ?? '',
                shift:           ld.shift           ?? report.shift ?? '',
                cavityNumbers:   ld.cavityNumbers   ?? '',
            };
            setLabelData(built);
            setIsLotLocked(true);
            setPrintOpen(true);

            // อัปเดต estimate สำหรับกล่องถัดไป
            setBoxNo(String(confirmedBoxNo + 1));
            setFeedback({ open: true, message: `บันทึกกล่องที่ ${confirmedBoxNo} สำเร็จ`, severity: 'success' });

        } catch (err) {
            setFeedback({
                open: true,
                message: err.response?.data || 'เกิดข้อผิดพลาดในการบันทึก',
                severity: 'error',
            });
        } finally {
            setSaving(false);
        }
    };

    const handlePrint = () => printPackagingLabel();

    return (
        <Box>
            <Button variant="outlined" onClick={onBack} sx={{ mb: 2 }}>
                &larr; กลับไปเลือกงาน
            </Button>

            {/* Lot number */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <TextField
                    label="Lot Number"
                    value={lotNumber}
                    onChange={e => { setLotNumber(e.target.value); setBoxNo(''); }}
                    disabled={isLotLocked}
                    fullWidth
                    size="small"
                />
                {isLotLocked && (
                    <Button variant="outlined" size="small" onClick={() => setIsLotLocked(false)}>
                        แก้ไข
                    </Button>
                )}
            </Box>

            {/* Box number + record button */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Paper variant="outlined" sx={{ p: 2, flexGrow: 1, textAlign: 'center' }}>
                    <Typography variant="caption" color="text.secondary">Box No.</Typography>
                    <Typography variant="h3" fontFamily="monospace">
                        {boxNo ? String(boxNo).padStart(3, '0') : '-'}
                    </Typography>
                </Paper>
                <Button
                    variant="contained"
                    sx={{ height: 90, width: 180, fontSize: '1rem', fontWeight: 700 }}
                    onClick={handleRecord}
                    disabled={saving || !lotNumber}
                >
                    {saving ? <CircularProgress size={24} color="inherit" /> : 'บันทึก 1 กล่อง'}
                </Button>
            </Box>

            {/* Print preview dialog */}
            <Dialog open={printOpen} onClose={() => setPrintOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>
                    Preview Label — กล่องที่ {String(labelData?.boxNo ?? 0).padStart(3, '0')}
                </DialogTitle>
                <DialogContent sx={{ overflow: 'hidden' }}>
                    {labelData && (
                        <div className="pkg-label-preview-wrap">
                            <PackagingLabelPrint labelData={labelData} />
                        </div>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setPrintOpen(false)}>ปิด</Button>
                    <Button variant="contained" startIcon={<PrintIcon />} onClick={handlePrint}>
                        พิมพ์ / Save as PDF
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar
                open={feedback.open}
                autoHideDuration={4000}
                onClose={() => setFeedback(f => ({ ...f, open: false }))}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert severity={feedback.severity} sx={{ width: '100%' }}>
                    {feedback.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default PackagingRecording;
