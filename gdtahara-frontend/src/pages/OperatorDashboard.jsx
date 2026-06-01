import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Typography, Paper, Button, Box, CircularProgress, Grid
} from '@mui/material';
import RunCardV2 from '../components/operator/RunCardV2';
import TrackOutPanel from '../components/operator/TrackOutPanel';

// เราจะสร้าง Component สำหรับการบันทึกข้อมูลในขั้นตอนต่อไป
// import NgRecording from '../components/NgRecording';
// import PackagingRecording from '../components/PackagingRecording';

const OperatorDashboard = () => {
    const [view, setView] = useState('select_report'); // 'select_report', 'select_task', 'ng_task', 'packaging_task'
    const [activeReports, setActiveReports] = useState([]);
    const [selectedReport, setSelectedReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [connectionStatus, setConnectionStatus] = useState('testing');

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
                <Button variant="outlined" onClick={() => setView('select_task')}>
                    &larr; กลับ
                </Button>
                <Box sx={{ textAlign: 'center', my: 3 }}>
                    <Typography variant="h5">บันทึกของเสีย (NG)</Typography>
                    <Typography color="text.secondary">เครื่อง: {selectedReport?.machineName}</Typography>
                    <Typography color="text.secondary">ผลิตภัณฑ์: {selectedReport?.productName}</Typography>
                </Box>
                <Typography variant="h6" sx={{ mt: 2 }}>
                    🚧 NG Recording Interface - Coming Soon
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    ระบบบันทึกของเสียจะพัฒนาในขั้นตอนต่อไป
                </Typography>
            </Paper>
        );
    }

    // View: Packaging Recording Task
    if (view === 'packaging_task') {
        return (
            <Paper sx={{ p: 2 }}>
                <Button variant="outlined" onClick={() => setView('select_task')}>
                    &larr; กลับ
                </Button>
                <Box sx={{ textAlign: 'center', my: 3 }}>
                    <Typography variant="h5">บันทึกการบรรจุ (Packaging)</Typography>
                    <Typography color="text.secondary">เครื่อง: {selectedReport?.machineName}</Typography>
                    <Typography color="text.secondary">ผลิตภัณฑ์: {selectedReport?.productName}</Typography>
                </Box>
                <Typography variant="h6" sx={{ mt: 2 }}>
                    📦 Packaging Recording Interface - Coming Soon
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    ระบบบันทึกการบรรจุจะพัฒนาในขั้นตอนต่อไป
                </Typography>
            </Paper>
        );
    }

    // View: เลือกงาน (บันทึก NG หรือ Packaging)
    if (view === 'select_task') {
        return (
            <Paper sx={{ p: 2 }}>
                <Button variant="outlined" onClick={() => setView('select_report')}>
                    &larr; กลับไปเลือกใบสั่งผลิต
                </Button>
                <Box sx={{ textAlign: 'center', my: 3 }}>
                    <Typography variant="h5">เครื่อง: {selectedReport.machineName}</Typography>
                    <Typography color="text.secondary">ผลิตภัณฑ์: {selectedReport.productName}</Typography>
                </Box>
                <Grid container spacing={3} justifyContent="center">
                    <Grid item xs={12} md={5}>
                        <Paper 
                            variant="outlined" 
                            sx={{ p: 4, textAlign: 'center', cursor: 'pointer', '&:hover': { borderColor: 'primary.main', backgroundColor: '#f5f5f5' } }}
                            onClick={() => setView('ng_task')}
                        >
                            <Typography variant="h6">บันทึกของเสีย (NG)</Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                บันทึกจำนวนและประเภทของเสียที่เกิดขึ้น
                            </Typography>
                        </Paper>
                    </Grid>
                    <Grid item xs={12} md={5}>
                        <Paper
                            variant="outlined"
                            sx={{ p: 4, textAlign: 'center', cursor: 'pointer', '&:hover': { borderColor: 'primary.main', backgroundColor: '#f5f5f5' } }}
                            onClick={() => setView('packaging_task')}
                        >
                            <Typography variant="h6">บันทึกการบรรจุ (Packaging)</Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                บันทึกหมายเลขกล่องและ Lot Number
                            </Typography>
                        </Paper>
                    </Grid>
                    <Grid item xs={12} md={5}>
                        <Paper
                            variant="outlined"
                            sx={{ p: 4, textAlign: 'center', cursor: 'pointer', '&:hover': { borderColor: 'success.main', backgroundColor: '#f0f9f0' } }}
                            onClick={() => setView('run_card')}
                        >
                            <Typography variant="h6" color="success.main">Run Card v2</Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                ยืนยันกล่อง · ดู Barcode · พิมพ์ Label
                            </Typography>
                        </Paper>
                    </Grid>
                    <Grid item xs={12} md={5}>
                        <Paper
                            variant="outlined"
                            sx={{ p: 4, textAlign: 'center', cursor: 'pointer', '&:hover': { borderColor: 'warning.main', backgroundColor: '#fffbf0' } }}
                            onClick={() => setView('track_out')}
                        >
                            <Typography variant="h6" color="warning.dark">Track-Out</Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                สแกน Barcode · ตรวจสอบกล่อง · พิมพ์ / Mark Labeled
                            </Typography>
                        </Paper>
                    </Grid>
                </Grid>
            </Paper>
        );
    }

    // View เริ่มต้น: เลือกใบสั่งผลิต
    return (
        <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">เลือกใบสั่งผลิตเพื่อเริ่มทำงาน</Typography>
                <Button variant="outlined" color="warning" onClick={() => setView('track_out')}>
                    📦 Track-Out / สแกน Barcode
                </Button>
            </Box>
            
            {/* Debug Information Panel */}
            <Paper variant="outlined" sx={{ p: 2, mb: 3, backgroundColor: '#f5f5f5' }}>
                <Typography variant="h6" color="primary">Operator System Status</Typography>
                <Typography>Backend Connection: {connectionStatus}</Typography>
                <Typography>Active Reports: {activeReports.length}</Typography>
                <Typography>Status: {error ? 'Error' : 'OK'}</Typography>
                {error && <Typography color="error">Error: {error}</Typography>}
                {activeReports.length > 0 && (
                    <Typography>Latest Report: {activeReports[0].machineName} - {activeReports[0].productName}</Typography>
                )}
            </Paper>
            
            {activeReports.length === 0 && !loading && (
                <Typography sx={{mt: 2}}>
                    {error ? error : 'ไม่มีใบสั่งผลิตที่กำลังทำงานอยู่'}
                </Typography>
            )}
            <Grid container spacing={2} sx={{ mt: 1 }}>
                {activeReports.map(report => (
                    <Grid item xs={12} sm={6} md={4} key={report.id}>
                        <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: '100%' }}>
                            <Typography variant="h6">{report.machineName}</Typography>
                            <Typography color="text.secondary">{report.productName}</Typography>
                            <Typography variant="body2" sx={{mt: 1}}>Order: {report.orderNumber || 'N/A'}</Typography>
                            <Box sx={{ flexGrow: 1 }} />
                            <Button variant="contained" sx={{ mt: 2 }} onClick={() => handleSelectReport(report)}>
                                เริ่มทำงาน
                            </Button>
                        </Paper>
                    </Grid>
                ))}
            </Grid>
        </Paper>
    );
};

export default OperatorDashboard;
