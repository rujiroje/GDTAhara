import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axiosInstance from '../api/axios';
import { Typography, Paper, Button, Box, CircularProgress, Tabs, Tab, Grid, Table, TableContainer, TableHead, TableRow, TableCell, TableBody } from '@mui/material';
import ShiftDataDisplay from '../components/ShiftDataDisplay';

// --- Component ย่อยสำหรับแต่ละ Tab ---

const OverallSummaryView = ({ reportId }) => {
    const [summary, setSummary] = useState(null);
    useEffect(() => {
        axiosInstance.get(`/pc/reports/${reportId}/summary`).then(res => setSummary(res.data));
    }, [reportId]);

    if (!summary) return <CircularProgress sx={{ my: 4 }} />;

    return (
        <Box sx={{ mt: 3 }}>
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography>ยอดผลิตดี (ชิ้น)</Typography><Typography variant="h4" color="primary">{summary.goodQty.toLocaleString()}</Typography></Paper></Grid>
                <Grid item xs={12} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography>ยอดของเสีย (ชิ้น)</Typography><Typography variant="h4" color="error">{summary.totalNgQty.toLocaleString()}</Typography></Paper></Grid>
                <Grid item xs={12} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography>Yield</Typography><Typography variant="h4" color="green">{summary.yield}</Typography></Paper></Grid>
                <Grid item xs={12} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography>Downtime (นาที)</Typography><Typography variant="h4">{summary.downtimeEvents?.reduce((acc, curr) => acc + parseInt(curr.duration.split(' ')[0] || 0), 0)}</Typography></Paper></Grid>
            </Grid>
            
            <Grid container spacing={3}>
                <Grid item xs={12} md={7}>
                    <Typography variant="h6" gutterBottom>ประวัติ Downtime</Typography>
                    <TableContainer component={Paper} variant="outlined"><Table size="small"><TableHead><TableRow><TableCell>เวลาเริ่ม</TableCell><TableCell>ระยะเวลา</TableCell><TableCell>สาเหตุ</TableCell><TableCell>ผู้บันทึก</TableCell></TableRow></TableHead><TableBody>{summary.downtimeEvents?.length > 0 ? summary.downtimeEvents.map((evt, i) => (<TableRow key={i}><TableCell>{evt.startTime}</TableCell><TableCell>{evt.duration}</TableCell><TableCell>{evt.reason}</TableCell><TableCell>{evt.technicianName}</TableCell></TableRow>)) : <TableRow><TableCell colSpan={4} align="center">ไม่มีข้อมูล</TableCell></TableRow>}</TableBody></Table></TableContainer>
                </Grid>
                <Grid item xs={12} md={5}>
                    <Typography variant="h6" gutterBottom>สรุปยอดใช้วัตถุดิบ</Typography>
                    <TableContainer component={Paper} variant="outlined"><Table size="small"><TableHead><TableRow><TableCell>วัตถุดิบ</TableCell><TableCell align="right">จำนวนที่ใช้</TableCell><TableCell>หน่วย</TableCell></TableRow></TableHead><TableBody>{summary.materialUsage?.length > 0 ? summary.materialUsage.map(mat => (<TableRow key={mat.materialName}><TableCell>{mat.materialName}</TableCell><TableCell align="right">{mat.totalQuantity}</TableCell><TableCell>{mat.unit}</TableCell></TableRow>)) : <TableRow><TableCell colSpan={3} align="center">ไม่มีข้อมูล</TableCell></TableRow>}</TableBody></Table></TableContainer>
                </Grid>
            </Grid>
        </Box>
    );
};

const ShiftBasedView = ({ reportId }) => {
    const [data, setData] = useState(null);
    useEffect(() => {
        axiosInstance.get(`/shift-leader/dashboard/${reportId}`).then(res => setData(res.data));
    }, [reportId]);

    if (!data) return <CircularProgress sx={{ my: 4 }} />;

    return (
        <Box sx={{ mt: 2 }}>
            <ShiftDataDisplay title="กะกลางวัน (03:00 - 15:00)" data={data.dayShiftData} />
            <ShiftDataDisplay title="กะกลางคืน (15:00 - 03:00)" data={data.nightShiftData} />
        </Box>
    );
};

// --- Component หลัก ---

const ReportSummaryPage = () => {
    const { reportId } = useParams();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState(0);
    const [orderNumber, setOrderNumber] = useState('');

    // Fetch order number once to show in page header (ส่วนหัว)
    useEffect(() => {
        let isMounted = true;
        axiosInstance.get(`/pc/reports/${reportId}/summary`).then(res => {
            if (!isMounted) return;
            const data = res.data || {};
            const ord = data.orderNumber || data.orderNo || data.prodOrder || data.productionOrder || '';
            setOrderNumber(ord);
        }).catch(() => {
            if (isMounted) setOrderNumber('');
        });
        return () => { isMounted = false; };
    }, [reportId]);

    const handleTabChange = (event, newValue) => {
        setActiveTab(newValue);
    };

    return (
        <Paper sx={{ p: 2, m: 1 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Box>
                    <Typography variant="h5">สรุปผลการผลิต</Typography>
                    <Typography variant="subtitle1" color="text.secondary">
                        เลขที่คำสั่งผลิต: {orderNumber || '—'} | Report ID: {reportId}
                    </Typography>
                </Box>
                <Button variant="outlined" onClick={() => navigate('/reports')}>กลับไปหน้ารายงาน</Button>
            </Box>

            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs value={activeTab} onChange={handleTabChange}>
                    {/* [แก้ไข] เรียงลำดับ Tab ใหม่ */}
                    <Tab label="1. ภาพรวมคำสั่งผลิต" />
                    <Tab label="2. รายงานรายวัน" disabled />
                    <Tab label="3. รายงานแยกกะ" />
                </Tabs>
            </Box>

            {activeTab === 0 && <OverallSummaryView reportId={reportId} />}
            {activeTab === 2 && <ShiftBasedView reportId={reportId} />}
            
        </Paper>
    );
};

export default ReportSummaryPage;