import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Typography, Box, Button, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions,
    TextField, Snackbar, Alert, Chip,
} from '@mui/material';

// 19 pastel pairs — soft bg + coordinated dark fg for readability
const NG_COLORS = [
    { bg: '#FFCDD2', fg: '#C62828' }, // Pastel Red
    { bg: '#FFCCBC', fg: '#BF360C' }, // Pastel Deep Orange
    { bg: '#FFE0B2', fg: '#E65100' }, // Pastel Orange
    { bg: '#FFF9C4', fg: '#F57F17' }, // Pastel Amber
    { bg: '#F0F4C3', fg: '#827717' }, // Pastel Lime
    { bg: '#DCEDC8', fg: '#33691E' }, // Pastel Light Green
    { bg: '#C8E6C9', fg: '#1B5E20' }, // Pastel Green
    { bg: '#B2DFDB', fg: '#004D40' }, // Pastel Teal
    { bg: '#B2EBF2', fg: '#006064' }, // Pastel Cyan
    { bg: '#B3E5FC', fg: '#01579B' }, // Pastel Light Blue
    { bg: '#BBDEFB', fg: '#0D47A1' }, // Pastel Blue
    { bg: '#C5CAE9', fg: '#1A237E' }, // Pastel Indigo
    { bg: '#D1C4E9', fg: '#4527A0' }, // Pastel Deep Purple
    { bg: '#E1BEE7', fg: '#6A1B9A' }, // Pastel Purple
    { bg: '#F8BBD0', fg: '#880E4F' }, // Pastel Pink
    { bg: '#FCE4EC', fg: '#AD1457' }, // Pastel Light Pink
    { bg: '#E8EAF6', fg: '#283593' }, // Pastel Blue-Grey
    { bg: '#E0F2F1', fg: '#00695C' }, // Pastel Teal Light
    { bg: '#F3E5F5', fg: '#6A1B9A' }, // Pastel Lavender
];

const isOtherType = (ng) => ng.ngDescriptionTh?.trim() === 'ปัญหาอื่นๆ';

// Split items into rows of sizes [7, 6, 6, 6, ...]
const buildRows = (items) => {
    if (!items.length) return [];
    const rows = [items.slice(0, 7)];
    for (let i = 7; i < items.length; i += 6) {
        rows.push(items.slice(i, i + 6));
    }
    return rows;
};

const NgRecording = ({ report, onBack }) => {
    const [ngTypes, setNgTypes]               = useState([]);
    const [loading, setLoading]               = useState(true);
    const [hourlyNgCount, setHourlyNgCount]   = useState({});
    const [nextResetTime, setNextResetTime]   = useState('');
    const [isAlertModalOpen, setIsAlertModalOpen] = useState(false);
    const [alertReason, setAlertReason]       = useState('');
    const [feedback, setFeedback]             = useState({ open: false, message: '', severity: 'success' });

    // "ปัญหาอื่นๆ" dialog state
    const [otherNg, setOtherNg]       = useState(null);
    const [otherDetail, setOtherDetail] = useState('');
    const [otherQty, setOtherQty]     = useState(1);
    const [otherSaving, setOtherSaving] = useState(false);

    // Fetch NG types — filter by machine type if known, otherwise fall back to all Operator types
    useEffect(() => {
        const machineType = report?.machineType;
        const params = machineType ? { machineType } : {};
        axiosInstance.get('/master-data/ng-types', { params })
            .then(res => {
                const data = res.data;
                const filtered = machineType ? data : data.filter(ng => ng.ngType === 'Operator');
                // Sort: "อื่นๆ" always last
                const sorted = [
                    ...filtered.filter(ng => !isOtherType(ng)),
                    ...filtered.filter(ng => isOtherType(ng)),
                ];
                setNgTypes(sorted);
            })
            .catch(err => console.error('Could not fetch NG types', err))
            .finally(() => setLoading(false));
    }, [report?.machineType]);

    // Fetch current-hour summary + set up hourly reset timer
    useEffect(() => {
        if (!report) return;

        // Load initial count for this hour from backend
        axiosInstance.get(`/operator/reports/${report.id}/hourly-ng-summary`)
            .then(res => setHourlyNgCount(res.data || {}))
            .catch(() => {});

        // Calculate ms until the start of the next hour
        const now           = new Date();
        const msToNextHour  = ((60 - now.getMinutes()) * 60 - now.getSeconds()) * 1000;

        // Display next reset clock
        const nextHour = new Date(now.getTime() + msToNextHour);
        setNextResetTime(`${String(nextHour.getHours()).padStart(2, '0')}:00`);

        let intervalId;
        const timeoutId = setTimeout(() => {
            setHourlyNgCount({});
            const nextNextHour = new Date();
            nextNextHour.setHours(nextNextHour.getHours() + 1, 0, 0, 0);
            setNextResetTime(`${String(nextNextHour.getHours()).padStart(2, '0')}:00`);
            intervalId = setInterval(() => {
                setHourlyNgCount({});
                const h = new Date(); h.setHours(h.getHours() + 1, 0, 0, 0);
                setNextResetTime(`${String(h.getHours()).padStart(2, '0')}:00`);
            }, 3_600_000);
        }, msToNextHour);

        return () => { clearTimeout(timeoutId); clearInterval(intervalId); };
    }, [report]);

    const handleRecordNg = async (ngTypeId, ngDescriptionTh, quantity = 1, note = undefined) => {
        try {
            await axiosInstance.post(`/operator/reports/${report.id}/ng-logs`, {
                ngTypeId,
                quantity,
                source: 'Operator_Run',
                ...(note ? { note } : {}),
            });
            // Update local hourly count immediately
            setHourlyNgCount(prev => ({
                ...prev,
                [ngDescriptionTh]: (prev[ngDescriptionTh] || 0) + quantity,
            }));
            setFeedback({ open: true, message: `บันทึก "${ngDescriptionTh}" ${quantity} ชิ้น สำเร็จ`, severity: 'success' });
        } catch {
            setFeedback({ open: true, message: 'เกิดข้อผิดพลาดในการบันทึก NG', severity: 'error' });
        }
    };

    const handleNgButtonClick = (ng) => {
        if (isOtherType(ng)) {
            setOtherNg(ng);
            setOtherDetail('');
            setOtherQty(1);
        } else {
            handleRecordNg(ng.id, ng.ngDescriptionTh);
        }
    };

    const handleOtherSubmit = async () => {
        if (!otherNg || !otherDetail.trim() || otherQty < 1) return;
        setOtherSaving(true);
        try {
            await handleRecordNg(otherNg.id, otherNg.ngDescriptionTh, otherQty, otherDetail.trim());
            setOtherNg(null);
        } finally {
            setOtherSaving(false);
        }
    };

    const handleAlertSubmit = async () => {
        try {
            await axiosInstance.post(`/operator/reports/${report.id}/alert`, { message: alertReason });
            setFeedback({ open: true, message: 'แจ้งปัญหาสำเร็จ', severity: 'info' });
            setIsAlertModalOpen(false);
            setAlertReason('');
        } catch {
            setFeedback({ open: true, message: 'เกิดข้อผิดพลาดในการแจ้งปัญหา', severity: 'error' });
        }
    };

    if (loading) return <CircularProgress />;

    const rows = buildRows(ngTypes);
    const rowOffsets = rows.reduce((acc, row, i) => {
        acc.push(i === 0 ? 0 : acc[i - 1] + rows[i - 1].length);
        return acc;
    }, []);

    const totalNg = Object.values(hourlyNgCount).reduce((s, v) => s + v, 0);
    const summaryEntries = Object.entries(hourlyNgCount).filter(([, v]) => v > 0);

    return (
        <Box>
            <Button variant="outlined" onClick={onBack} sx={{ mb: 2 }}>
                &larr; กลับไปเลือกงาน
            </Button>

            {/* NG button grid — row 1: 7 buttons, subsequent rows: 6 buttons */}
            <Box>
                {rows.map((row, rowIdx) => (
                    <Box key={rowIdx} sx={{ display: 'flex', gap: 1.5, mb: 1.5 }}>
                        {row.map((ng, colIdx) => {
                            const colorIdx = rowOffsets[rowIdx] + colIdx;
                            const { bg, fg } = NG_COLORS[colorIdx % NG_COLORS.length];
                            const count = hourlyNgCount[ng.ngDescriptionTh] || 0;
                            const isOther = isOtherType(ng);
                            return (
                                <Box key={ng.id} sx={{ flex: 1, position: 'relative', minWidth: 0 }}>
                                    <Button
                                        fullWidth
                                        onClick={() => handleNgButtonClick(ng)}
                                        sx={{
                                            height: 90,
                                            fontSize: '0.82rem',
                                            fontWeight: 700,
                                            textTransform: 'none',
                                            whiteSpace: 'normal',
                                            lineHeight: 1.35,
                                            textAlign: 'center',
                                            borderRadius: 2,
                                            color: fg,
                                            backgroundColor: bg,
                                            border: isOther ? `2px dashed ${fg}88` : `1.5px solid ${fg}33`,
                                            boxShadow: '0 1px 4px rgba(0,0,0,0.10)',
                                            '&:hover': {
                                                backgroundColor: bg,
                                                filter: 'brightness(0.93)',
                                                transform: 'translateY(-2px)',
                                                boxShadow: `0 5px 14px ${fg}40`,
                                            },
                                            transition: 'transform 0.13s ease, box-shadow 0.13s ease, filter 0.13s ease',
                                        }}
                                    >
                                        {ng.ngDescriptionTh}
                                    </Button>
                                    {/* Count badge — top-right corner */}
                                    {count > 0 && (
                                        <Chip
                                            label={count}
                                            size="small"
                                            sx={{
                                                position: 'absolute',
                                                top: -8,
                                                right: -8,
                                                minWidth: 24,
                                                height: 24,
                                                fontSize: '0.75rem',
                                                fontWeight: 800,
                                                backgroundColor: fg,
                                                color: '#fff',
                                                pointerEvents: 'none',
                                                zIndex: 1,
                                                '& .MuiChip-label': { px: '6px' },
                                            }}
                                        />
                                    )}
                                </Box>
                            );
                        })}
                    </Box>
                ))}
            </Box>

            {/* Hourly summary table */}
            <Box sx={{ mt: 3, p: 2, borderRadius: 2, backgroundColor: '#f8f9fa', border: '1px solid #e0e0e0' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                    <Typography variant="subtitle1" fontWeight={700}>
                        สรุปยอด NG ในชั่วโมงนี้
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        รีเซ็ตอัตโนมัติเวลา {nextResetTime} น.
                    </Typography>
                </Box>

                {summaryEntries.length > 0 ? (
                    <Box component="table" sx={{ width: '100%', borderCollapse: 'collapse' }}>
                        <Box component="tbody">
                            {summaryEntries
                                .sort(([, a], [, b]) => b - a)
                                .map(([desc, count]) => (
                                    <Box component="tr" key={desc}
                                        sx={{ '&:not(:last-child) td': { borderBottom: '1px solid #e0e0e0' } }}>
                                        <Box component="td" sx={{ py: 0.6, pr: 2, fontSize: '0.88rem', color: '#333' }}>
                                            {desc}
                                        </Box>
                                        <Box component="td" sx={{ py: 0.6, textAlign: 'right', fontWeight: 700,
                                            fontSize: '0.95rem', color: '#C62828', whiteSpace: 'nowrap' }}>
                                            {count} ชิ้น
                                        </Box>
                                    </Box>
                                ))}
                        </Box>
                        <Box component="tfoot">
                            <Box component="tr">
                                <Box component="td" sx={{ pt: 1, fontWeight: 700, fontSize: '0.9rem', borderTop: '2px solid #bbb' }}>
                                    รวมทั้งหมด
                                </Box>
                                <Box component="td" sx={{ pt: 1, textAlign: 'right', fontWeight: 800,
                                    fontSize: '1rem', color: '#B71C1C', borderTop: '2px solid #bbb' }}>
                                    {totalNg} ชิ้น
                                </Box>
                            </Box>
                        </Box>
                    </Box>
                ) : (
                    <Typography variant="body2" color="text.secondary">
                        ยังไม่มีการบันทึกของเสียในชั่วโมงนี้
                    </Typography>
                )}
            </Box>

            {/* Machine stop alert button */}
            <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid #e0e0e0' }}>
                <Button
                    fullWidth
                    variant="contained"
                    onClick={() => setIsAlertModalOpen(true)}
                    sx={{
                        height: 56,
                        fontSize: '1rem',
                        fontWeight: 700,
                        backgroundColor: '#B71C1C',
                        letterSpacing: 0.5,
                        '&:hover': { backgroundColor: '#C62828' },
                    }}
                >
                    🚨 แจ้งเครื่องจักรหยุด
                </Button>
            </Box>

            {/* "ปัญหาอื่นๆ" detail dialog */}
            <Dialog open={!!otherNg} onClose={() => setOtherNg(null)} fullWidth maxWidth="xs">
                <DialogTitle>บันทึกของเสีย — {otherNg?.ngDescriptionTh}</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        fullWidth
                        multiline
                        rows={3}
                        label="รายละเอียดปัญหา *"
                        placeholder="ระบุรายละเอียดของปัญหาที่พบ"
                        value={otherDetail}
                        onChange={e => setOtherDetail(e.target.value)}
                        sx={{ mt: 1, mb: 2 }}
                    />
                    <TextField
                        fullWidth
                        type="number"
                        label="จำนวนของเสีย (ชิ้น) *"
                        inputProps={{ min: 1 }}
                        value={otherQty}
                        onChange={e => setOtherQty(Math.max(1, Number(e.target.value)))}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOtherNg(null)} disabled={otherSaving}>ยกเลิก</Button>
                    <Button
                        variant="contained"
                        disabled={!otherDetail.trim() || otherQty < 1 || otherSaving}
                        onClick={handleOtherSubmit}
                    >
                        {otherSaving ? 'กำลังบันทึก...' : 'บันทึก'}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Alert dialog */}
            <Dialog open={isAlertModalOpen} onClose={() => setIsAlertModalOpen(false)} fullWidth maxWidth="sm">
                <DialogTitle>แจ้งเครื่องจักรหยุด</DialogTitle>
                <DialogContent>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        ระบุสาเหตุที่เครื่องจักรหยุดทำงาน
                    </Typography>
                    <TextField
                        autoFocus
                        fullWidth
                        multiline
                        rows={3}
                        label="สาเหตุ"
                        value={alertReason}
                        onChange={e => setAlertReason(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setIsAlertModalOpen(false)}>ยกเลิก</Button>
                    <Button
                        variant="contained"
                        color="error"
                        disabled={!alertReason.trim()}
                        onClick={handleAlertSubmit}
                    >
                        แจ้งปัญหา
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar
                open={feedback.open}
                autoHideDuration={3000}
                onClose={() => setFeedback(f => ({ ...f, open: false }))}
            >
                <Alert severity={feedback.severity} sx={{ width: '100%' }}>
                    {feedback.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default NgRecording;
