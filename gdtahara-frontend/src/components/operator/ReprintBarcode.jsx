import React, { useState, useEffect } from 'react';
import axiosInstance from '../../api/axios';
import {
    Box, Button, TextField, Typography, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions,
    Table, TableBody, TableCell, TableHead, TableRow, Paper,
    InputAdornment, IconButton,
} from '@mui/material';
import PrintIcon from '@mui/icons-material/Print';
import SearchIcon from '@mui/icons-material/Search';
import PackagingLabelPrint from '../PackagingLabelPrint';
import { printPackagingLabel } from '../../utils/printPackagingLabel';

const todayLot = () => {
    const now = new Date();
    const yy = String(now.getFullYear()).slice(2);
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    return yy + mm + dd;
};

const ReprintBarcode = ({ report, onBack }) => {
    const [logs, setLogs]           = useState([]);
    const [loading, setLoading]     = useState(false);
    const [lotFilter, setLotFilter] = useState(todayLot);
    const [labelData, setLabelData] = useState(null);
    const [printOpen, setPrintOpen] = useState(false);
    const [baseData, setBaseData]   = useState(null);

    // Fetch base label data (product/machine info) once on mount
    useEffect(() => {
        axiosInstance.get(`/operator/reports/${report.id}/label-data`)
            .then(res => setBaseData(res.data))
            .catch(() => {});
    }, [report.id]);

    const fetchLogs = async () => {
        setLoading(true);
        try {
            const res = await axiosInstance.get(`/operator/reports/${report.id}/packaging-logs`);
            setLogs(Array.isArray(res.data) ? res.data : []);
        } catch {
            setLogs([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchLogs(); }, [report.id]);

    const filtered = logs.filter(l =>
        !lotFilter || l.lotNumber?.includes(lotFilter)
    );

    const handleSelectLog = (log) => {
        const built = {
            ...(baseData ?? {}),
            lotNumber:    log.lotNumber,
            boxNo:        log.boxNo,
            operatorName: log.operatorName ?? '',
        };
        setLabelData(built);
        setPrintOpen(true);
    };

    const handlePrint = () => printPackagingLabel();

    return (
        <Box>
            <Button variant="outlined" onClick={onBack} sx={{ mb: 2 }}>
                &larr; กลับไปเลือกงาน
            </Button>
            <Typography variant="h6" fontWeight={700} sx={{ mb: 2 }}>
                🖨️ พิมพ์ซ้ำ Label — {report.machineName}
            </Typography>

            {/* Lot number filter */}
            <Box sx={{ display: 'flex', gap: 1.5, mb: 2, alignItems: 'center' }}>
                <TextField
                    size="small"
                    label="กรองตาม Lot Number"
                    value={lotFilter}
                    onChange={e => setLotFilter(e.target.value)}
                    InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                                <IconButton size="small" onClick={() => setLotFilter('')}>
                                    ✕
                                </IconButton>
                            </InputAdornment>
                        ),
                    }}
                    sx={{ width: 220 }}
                />
                <Button variant="outlined" startIcon={<SearchIcon />} onClick={fetchLogs} disabled={loading}>
                    รีเฟรช
                </Button>
                <Typography variant="caption" color="text.secondary">
                    {filtered.length} กล่อง
                </Typography>
            </Box>

            {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                    <CircularProgress />
                </Box>
            ) : filtered.length === 0 ? (
                <Typography color="text.secondary" sx={{ py: 2 }}>
                    ไม่พบข้อมูลการบรรจุ
                </Typography>
            ) : (
                <Paper variant="outlined">
                    <Table size="small">
                        <TableHead>
                            <TableRow sx={{ backgroundColor: '#f3f4f6' }}>
                                <TableCell sx={{ fontWeight: 700 }}>Lot Number</TableCell>
                                <TableCell sx={{ fontWeight: 700 }} align="center">กล่องที่</TableCell>
                                <TableCell sx={{ fontWeight: 700 }}>เวลาบรรจุ</TableCell>
                                <TableCell sx={{ fontWeight: 700 }}>ผู้บรรจุ</TableCell>
                                <TableCell />
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {filtered.map((log, i) => (
                                <TableRow
                                    key={i}
                                    hover
                                    sx={{ cursor: 'pointer' }}
                                    onClick={() => handleSelectLog(log)}
                                >
                                    <TableCell sx={{ fontFamily: 'monospace' }}>{log.lotNumber}</TableCell>
                                    <TableCell align="center" sx={{ fontWeight: 700 }}>
                                        {String(log.boxNo).padStart(3, '0')}
                                    </TableCell>
                                    <TableCell sx={{ fontSize: '0.82rem' }}>
                                        {log.timestamp
                                            ? new Date(log.timestamp).toLocaleString('th-TH', { dateStyle: 'short', timeStyle: 'short' })
                                            : '-'}
                                    </TableCell>
                                    <TableCell sx={{ fontSize: '0.82rem' }}>{log.operatorName || '-'}</TableCell>
                                    <TableCell align="right">
                                        <Button
                                            size="small"
                                            variant="outlined"
                                            startIcon={<PrintIcon />}
                                            onClick={e => { e.stopPropagation(); handleSelectLog(log); }}
                                        >
                                            พิมพ์
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </Paper>
            )}

            {/* Print preview dialog */}
            <Dialog open={printOpen} onClose={() => setPrintOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>ตัวอย่าง Label</DialogTitle>
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
                        พิมพ์
                    </Button>
                </DialogActions>
            </Dialog>

        </Box>
    );
};

export default ReprintBarcode;
