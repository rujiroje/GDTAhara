import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Typography, Paper, Button, Box, CircularProgress, Grid
} from '@mui/material';

const LINE_COLORS = [
    { bg: '#FFCDD2', fg: '#C62828' },
    { bg: '#FFCCBC', fg: '#BF360C' },
    { bg: '#FFF9C4', fg: '#F57F17' },
    { bg: '#DCEDC8', fg: '#33691E' },
    { bg: '#C8E6C9', fg: '#1B5E20' },
    { bg: '#B2EBF2', fg: '#006064' },
    { bg: '#B3E5FC', fg: '#01579B' },
    { bg: '#BBDEFB', fg: '#0D47A1' },
    { bg: '#C5CAE9', fg: '#1A237E' },
    { bg: '#D1C4E9', fg: '#4527A0' },
    { bg: '#E1BEE7', fg: '#6A1B9A' },
    { bg: '#F8BBD0', fg: '#880E4F' },
    { bg: '#FCE4EC', fg: '#AD1457' },
    { bg: '#E0F2F1', fg: '#00695C' },
    { bg: '#F3E5F5', fg: '#6A1B9A' },
];
import RunCardV2 from '../components/operator/RunCardV2';
import TrackOutPanel from '../components/operator/TrackOutPanel';
import NgRecording from '../components/NgRecording';
import PackagingRecording from '../components/PackagingRecording';

const OperatorDashboard = () => {
    const [view, setView] = useState('select_report'); // 'select_report', 'select_task', 'ng_task', 'packaging_task'
    const [activeReports, setActiveReports] = useState([]);
    const [selectedReport, setSelectedReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [ setConnectionStatus] = useState('testing');

    // Test backend connection when component mounts
    useEffect(() => {
        const testConnection = async () => {
            try {
                console.log('Testing operator backend connection...');
                const response = await axiosInstance.get('/operator/test');
                console.log('Operator backend test response:', response.data);
                if (response.data.status === 'success') {
                    setConnectionStatus('connected');
                } else {
                    setConnectionStatus('failed');
                }
            } catch (error) {
                console.error('Operator backend test failed:', error);
                setConnectionStatus('failed');
            }
        };
        testConnection();
    }, []);

    useEffect(() => {
        // ดึงข้อมูลใบสั่งผลิตที่ Active เฉพาะเมื่ออยู่หน้าเลือก
        if (view === 'select_report') {
            const fetchActiveReports = async () => {
                setLoading(true);
                try {
                    console.log('Fetching active reports for operator...');
                    // แก้ไขจาก /pc/ เป็น /operator/
                    const response = await axiosInstance.get('/operator/reports/active');
                    console.log('Operator active reports response:', response.data);
                    setActiveReports(response.data);
                    setError(''); // Clear any previous errors
                } catch (err) {
                    console.error('Error fetching active reports:', err);
                    setError('ไม่สามารถดึงรายการใบสั่งผลิตได้: ' + (err.response?.data?.message || err.message));
                } finally {
                    setLoading(false);
                }
            };
            fetchActiveReports();
        }
    }, [view]);

    const handleSelectReport = (report) => {
        setSelectedReport(report);
        setView('select_task'); // ไปยังหน้าเลือกงาน
    };

    if (loading) return <CircularProgress />;
    if (error && view === 'select_report') return <Typography color="error">{error}</Typography>;

    // View: Track-Out (barcode scan)
    if (view === 'track_out') {
        return <TrackOutPanel onBack={() => setView('select_report')} />;
    }

    // View: Run Card v2
    if (view === 'run_card') {
        return (
            <Paper sx={{ p: 2 }}>
                <Button variant="outlined" onClick={() => setView('select_task')} sx={{ mb: 1 }}>
                    &larr; กลับ
                </Button>
                <Typography variant="h6" gutterBottom>
                    Run Card v2 — {selectedReport?.machineName}
                </Typography>
                <RunCardV2
                    activeReports={activeReports}
                    initialReport={selectedReport}
                />
            </Paper>
        );
    }

    // View: NG Recording Task
    if (view === 'ng_task') {
        return (
            <Paper sx={{ p: 2 }}>
                <Box sx={{ textAlign: 'center', mb: 2 }}>
                    <Typography variant="h5">บันทึกของเสีย (NG)</Typography>
                    <Typography color="text.secondary">เครื่อง: {selectedReport?.machineName}</Typography>
                    <Typography color="text.secondary">ผลิตภัณฑ์: {selectedReport?.productName}</Typography>
                </Box>
                <NgRecording
                    report={selectedReport}
                    onBack={() => setView('select_task')}
                />
            </Paper>
        );
    }

    // View: Packaging Recording Task
    if (view === 'packaging_task') {
        return (
            <Paper sx={{ p: 2 }}>
                <Box sx={{ textAlign: 'center', mb: 2 }}>
                    <Typography variant="h5">บันทึกการบรรจุ (Packaging)</Typography>
                    <Typography color="text.secondary">เครื่อง: {selectedReport?.machineName}</Typography>
                    <Typography color="text.secondary">ผลิตภัณฑ์: {selectedReport?.productName}</Typography>
                </Box>
                <PackagingRecording
                    report={selectedReport}
                    onBack={() => setView('select_task')}
                />
            </Paper>
        );
    }

    // View: เลือกงาน (บันทึก NG หรือ Packaging)
    if (view === 'select_task') {
        const TASKS = [
            { view: 'ng_task',       label: 'บันทึกของเสีย',    sub: 'NG Recording',     color: LINE_COLORS[0] },
            { view: 'packaging_task',label: 'บันทึกการบรรจุ',   sub: 'Packaging',        color: LINE_COLORS[2] },
            { view: 'run_card',      label: 'Run Card v2',      sub: 'ยืนยันกล่อง · Label', color: LINE_COLORS[6] },
            { view: 'track_out',     label: 'Track-Out',        sub: 'สแกน Barcode',     color: LINE_COLORS[9] },
        ];
        return (
            <Box sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                    <Button variant="outlined" size="small" onClick={() => setView('select_report')}>
                        &larr; กลับ
                    </Button>
                    <Box>
                        <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
                            {selectedReport.machineName}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {selectedReport.productName}
                        </Typography>
                    </Box>
                </Box>

                <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 1.5 }}>
                    {TASKS.map(task => {
                        const { bg, fg } = task.color;
                        return (
                            <Box
                                key={task.view}
                                onClick={() => setView(task.view)}
                                sx={{
                                    minHeight: 110,
                                    backgroundColor: bg,
                                    border: `2px solid ${fg}33`,
                                    borderRadius: 2,
                                    p: 2.5,
                                    cursor: 'pointer',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    justifyContent: 'center',
                                    alignItems: 'center',
                                    textAlign: 'center',
                                    boxShadow: '0 1px 4px rgba(0,0,0,0.10)',
                                    transition: 'transform 0.12s ease, box-shadow 0.12s ease',
                                    '&:hover': {
                                        transform: 'translateY(-3px)',
                                        boxShadow: `0 6px 16px ${fg}40`,
                                        filter: 'brightness(0.95)',
                                    },
                                    '&:active': { transform: 'scale(0.97)' },
                                }}
                            >
                                <Typography fontWeight={800} fontSize="1.05rem" color={fg} lineHeight={1.3}>
                                    {task.label}
                                </Typography>
                                <Typography fontSize="0.78rem" color={fg} sx={{ mt: 0.5, opacity: 0.75 }}>
                                    {task.sub}
                                </Typography>
                            </Box>
                        );
                    })}
                </Box>
            </Box>
        );

    }

    // View เริ่มต้น: เลือกใบสั่งผลิต (Color Block)
    return (
        <Box sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5" fontWeight={700}>เลือก Line การผลิต</Typography>
                <Button variant="outlined" color="warning" size="small"
                    onClick={() => setView('track_out')}>
                    📦 Track-Out
                </Button>
            </Box>

            {error && (
                <Typography color="error" sx={{ mb: 1 }}>{error}</Typography>
            )}

            {activeReports.length === 0 && !loading && (
                <Typography color="text.secondary" sx={{ mt: 2, textAlign: 'center' }}>
                    ไม่มีใบสั่งผลิตที่กำลังทำงานอยู่
                </Typography>
            )}

            {/* Color Block grid — 4 per row, sorted by machine name */}
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 1.5 }}>
                {[...activeReports]
                    .sort((a, b) => (a.machineName ?? '').localeCompare(b.machineName ?? '', undefined, { numeric: true }))
                    .map((report, idx) => {
                    const { bg, fg } = LINE_COLORS[idx % LINE_COLORS.length];
                    return (
                        <Box
                            key={report.id}
                            onClick={() => handleSelectReport(report)}
                            sx={{
                                minHeight: 90,
                                backgroundColor: bg,
                                border: `2px solid ${fg}33`,
                                borderRadius: 2,
                                p: 2,
                                cursor: 'pointer',
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center',
                                boxShadow: '0 1px 4px rgba(0,0,0,0.10)',
                                transition: 'transform 0.12s ease, box-shadow 0.12s ease',
                                '&:hover': {
                                    transform: 'translateY(-3px)',
                                    boxShadow: `0 6px 16px ${fg}40`,
                                    filter: 'brightness(0.95)',
                                },
                                '&:active': {
                                    transform: 'scale(0.97)',
                                },
                            }}
                        >
                            <Typography fontWeight={800} fontSize="1.25rem" color={fg} lineHeight={1.2}>
                                {report.machineName}
                            </Typography>
                            <Typography fontSize="0.82rem" color={fg} sx={{ mt: 0.5, opacity: 0.85 }}
                                noWrap>
                                {report.productName}
                            </Typography>
                            {report.orderNumber && (
                                <Typography fontSize="0.75rem" color={fg} sx={{ mt: 0.3, opacity: 0.6 }}>
                                    {report.orderNumber}
                                </Typography>
                            )}
                        </Box>
                    );
                })}
            </Box>
        </Box>
    );
};

export default OperatorDashboard;
