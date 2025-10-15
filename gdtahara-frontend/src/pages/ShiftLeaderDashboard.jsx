import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import { Typography, Paper, Button, Box, CircularProgress, Grid } from '@mui/material';
import ShiftLeaderProductionView from './ShiftLeaderProductionView';
import ShiftDataDisplay from '../components/ShiftDataDisplay';
import LabelStockManagement from './LabelStockManagement';
import MaterialStockManagement from './MaterialStockManagement';
import NotificationPanel from '../components/NotificationPanel'; // 1. Import
import ShiftLeaderNgLogPage from './ShiftLeaderNgLogPage'; // 2. Import
// เราจะสร้าง 2 Component นี้ในขั้นตอนต่อไป
// import LabelStockManagement from '../components/LabelStockManagement';
// import MaterialStockManagement from '../components/MaterialStockManagement';

const ShiftLeaderDashboard = () => {
    // State สำหรับควบคุมว่าจะแสดงหน้าจอไหน: 'main', 'labels', 'materials'
    const [view, setView] = useState('main'); 
    const [selectedReportId, setSelectedReportId] = useState(null);
    const [dashboardData, setDashboardData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [activeReports, setActiveReports] = useState([]);
    const [ngTypes, setNgTypes] = useState([]);
    
    // ทดสอบ backend connection ตอน component load
    useEffect(() => {
        console.log('ShiftLeaderDashboard mounted, testing connection...');
        testBackendConnection();
    }, []);
    
    const testBackendConnection = async () => {
        try {
            console.log('Testing simple backend connection...');
            // ทดสอบ simple endpoint ก่อน
            const simpleResponse = await axiosInstance.get('/shift-leader/simple-test');
            console.log('Simple backend test response:', simpleResponse.data);
            
            if (simpleResponse.data.status === 'success') {
                console.log('✅ Simple backend connection successful');
                
                // ถ้า simple test สำเร็จ ให้ลอง test dashboard
                try {
                    const response = await axiosInstance.get('/shift-leader/test-dashboard');
                    console.log('Dashboard test response:', response.data);
                    
                    if (response.data.status === 'success') {
                        setActiveReports(response.data.activeReports || []);
                        setNgTypes(response.data.ngTypes || []);
                        setError(null);
                    } else {
                        setError('Dashboard test failed');
                    }
                } catch (dashError) {
                    console.error('Dashboard test error:', dashError);
                    setError(`Dashboard test error: ${dashError.message}`);
                }
            } else {
                setError('Backend connection failed');
            }
        } catch (error) {
            console.error('Backend test error:', error);
            setError(`Backend connection error: ${error.message}`);
        } finally {
            setLoading(false);
        }
    };
    
    // --- โค้ดสำหรับแสดง Dashboard การผลิต (จะนำมาใส่ทีหลัง) ---
    if (loading) return <CircularProgress />;
    if (error) return <Typography color="error">{error}</Typography>;
    
    if (view === 'production') {
        return <ShiftLeaderProductionView onBack={() => setView('main')} />;
    }
    if (view === 'productionList') {
        // ... (โค้ดเหมือนเดิม)
    }
    
    if (view === 'dashboardDetail' && dashboardData) {
        // ... (โค้ดเหมือนเดิม)
    }
    if (view === 'materials') {
        return <MaterialStockManagement onBack={() => setView('main')} />;
    }
    // 2. เพิ่มเงื่อนไขให้แสดงหน้า Label Stock
    if (view === 'labels') {
        return <LabelStockManagement onBack={() => setView('main')} />;
    }
    if (view === 'ng') {
        return <ShiftLeaderNgLogPage onBack={() => setView('main')} />;
    }
    // if (view === 'labels') {
    //     return <LabelStockManagement onBack={() => setView('main')} />;
    // }
    // if (view === 'materials') {
    //     return <MaterialStockManagement onBack={() => setView('main')} />;
    // }
    const fetchDashboardDetails = async (reportId) => {
        setLoading(true);
        setError('');
        try {
            const response = await axiosInstance.get(`/shift-leader/dashboard/${reportId}`);
            setDashboardData(response.data);
            setSelectedReportId(reportId);
            setView('dashboardDetail');
        } catch (err) {
            setError('ไม่สามารถดึงข้อมูล Dashboard ได้');
        } finally {
            setLoading(false);
        }
    };
    
    if (loading) return <CircularProgress />;
    if (error) return <Typography color="error">{error}</Typography>;

    if (view === 'productionList') {
        return <ShiftLeaderProductionView 
                    onBack={() => setView('main')} 
                    onViewDetails={fetchDashboardDetails} 
                />;
    }
    
    if (view === 'dashboardDetail' && dashboardData) {
        return (
            <Paper sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Box>
                        <Typography variant="h5">รายละเอียดการผลิต: {dashboardData.machineName}</Typography>
                        <Typography variant="subtitle1" color="text.secondary">
                            เลขที่คำสั่งผลิต: {dashboardData.orderNumber || dashboardData.orderNo || dashboardData.prodOrder || dashboardData.productionOrder || '—'} | ผลิตภัณฑ์: {dashboardData.productName}
                        </Typography>
                    </Box>
                    <Button variant="outlined" onClick={() => setView('productionList')}>กลับไปหน้ารายการ</Button>
                </Box>
                <ShiftDataDisplay title="กะกลางวัน (03:00 - 15:00)" data={dashboardData.dayShiftData} />
                <ShiftDataDisplay title="กะกลางคืน (15:00 - 03:00)" data={dashboardData.nightShiftData} />
            </Paper>
        );
    }

    // หน้าจอหลัก (Main View)
    return (
        <Paper sx={{ p: 2 }}>
            <Typography variant="h5" gutterBottom>Shift Leader Dashboard</Typography>
            <Typography color="text.secondary" sx={{ mb: 3 }}>กรุณาเลือกการทำงานที่ต้องการ</Typography>
            
            {/* Debug Information */}
            <Paper variant="outlined" sx={{ p: 2, mb: 3, backgroundColor: '#f5f5f5' }}>
                <Typography variant="h6" color="primary">System Status</Typography>
                <Typography>Active Reports: {activeReports.length}</Typography>
                <Typography>NG Types Available: {ngTypes.length}</Typography>
                <Typography>Status: {error ? 'Error' : 'Connected'}</Typography>
                {error && <Typography color="error">Error: {error}</Typography>}
                {activeReports.length > 0 && (
                    <Typography>Latest Report: {activeReports[0].machineName} - {activeReports[0].productName}</Typography>
                )}
            </Paper>
            
            <Grid container spacing={3}>
                <Grid item xs={12} md={4}>
                    <Paper variant="outlined" sx={{ p: 3, textAlign: 'center' }}>
                        <Typography variant="h6">ภาพรวมการผลิต</Typography>
                        <Typography sx={{ my: 2 }}>ดูสรุปผลการผลิตของเครื่องจักรที่กำลังทำงาน</Typography>
                        <Button variant="contained" onClick={() => setView('productionList')}>ดู Dashboard</Button>
                    </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                    <Paper variant="outlined" sx={{ p: 3, textAlign: 'center' }}>
                        <Typography variant="h6">จัดการสต็อกป้าย</Typography>
                        <Typography sx={{ my: 2 }}>ดูและเพิ่มจำนวนสต็อกป้าย (Label) สำหรับแต่ละผลิตภัณฑ์</Typography>
                        {/* ปุ่มนี้จะเปลี่ยน view state เป็น 'labels' */}
                        <Button variant="contained" onClick={() => setView('labels')}>จัดการสต็อกป้าย</Button>
                    </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                    <Paper variant="outlined" sx={{ p: 3, textAlign: 'center' }}>
                        <Typography variant="h6">จัดการสต็อกวัตถุดิบ</Typography>
                        <Typography sx={{ my: 2 }}>ดู Stock Card และรับวัตถุดิบเข้าสู่ระบบ (Stock-In)</Typography>
                        {/* ปุ่มนี้จะเปลี่ยน view state เป็น 'materials' */}
                        <Button variant="contained" onClick={() => setView('materials')}>จัดการสต็อกวัตถุดิบ</Button>
                    </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                    <Paper variant="outlined" sx={{ p: 3, textAlign: 'center' }}>
                        <Typography variant="h6">บันทึกของเสีย</Typography>
                        <Typography sx={{ my: 2 }}>บันทึกของเสีย (NG) ที่พบในกระบวนการผลิต</Typography>
                        <Button variant="contained" color="warning" onClick={() => setView('ng')}>บันทึกของเสีย</Button>
                    </Paper>
                </Grid>
            </Grid>
        </Paper>
    );
};

export default ShiftLeaderDashboard;