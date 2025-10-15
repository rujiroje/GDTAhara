import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import { useNavigate } from 'react-router-dom';
import { Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Box, CircularProgress } from '@mui/material';

const PcDashboard = () => {
    const navigate = useNavigate();
    const [summary, setSummary] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let isMounted = true; // Track if component is still mounted
        
        const fetchDashboardData = async () => {
            console.log('🔄 Starting dashboard data fetch...');
            setLoading(true);
            setError(null);
            
            try {
                // Try multiple endpoints to ensure compatibility
                let response;
                try {
                    // Try the new ProductionController endpoint first
                    response = await axiosInstance.get('/production/dashboard-summary');
                    console.log('✅ Using /production/dashboard-summary endpoint');
                } catch (newEndpointError) {
                    console.log('⚠️ New endpoint failed, trying old endpoint...');
                    // Fallback to old ProductionControlController endpoint
                    response = await axiosInstance.get('/production/dashboard-summary');  // Fixed: removed /pc prefix
                    console.log('✅ Using /production/dashboard-summary endpoint');
                }
                
                console.log('✅ API Response received:', response.data);
                console.log('📊 Response data type:', typeof response.data, 'Length:', response.data?.length);
                
                if (isMounted) {
                    // Handle both direct array response and wrapped response
                    let dashboardData = response.data;
                    
                    // If response is wrapped in a "data" property
                    if (response.data && response.data.data && Array.isArray(response.data.data)) {
                        dashboardData = response.data.data;
                        console.log('📦 Using wrapped data property');
                    } else if (Array.isArray(response.data)) {
                        dashboardData = response.data;
                        console.log('� Using direct array response');
                    } else {
                        console.warn('⚠️ Response data format not recognized:', response.data);
                        dashboardData = [];
                    }
                    
                    // เรียงลำดับข้อมูลตาม machine name แล้วตาม product name
                    const sortedData = dashboardData.sort((a, b) => {
                        const machineCompare = (a.machineName || '').localeCompare(b.machineName || '');
                        if (machineCompare !== 0) {
                            return machineCompare;
                        }
                        return (a.productName || '').localeCompare(b.productName || '');
                    });
                    
                    setSummary(sortedData);
                    console.log('✅ Summary state updated with', sortedData.length, 'items (sorted by machine name)');
                    console.log('📋 Items:', sortedData);
                }
            } catch (error) {
                console.error("❌ Failed to fetch dashboard summary", error);
                if (isMounted) {
                    setError(error.message);
                    setSummary([]);
                }
            } finally {
                if (isMounted) {
                    setLoading(false);
                    console.log('🏁 Loading state set to false');
                }
            }
        };
        
        // Add slight delay to ensure component is fully mounted and authenticated
        const timer = setTimeout(fetchDashboardData, 100);
        
        // Cleanup function
        return () => {
            isMounted = false;
            clearTimeout(timer);
        };
    }, []);

    console.log('🖼️ Render - Loading:', loading, 'Summary length:', summary.length, 'Error:', error);

    if (loading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}><CircularProgress /></Box>;
    }

    if (error) {
        return (
            <Paper sx={{ p: 4, textAlign: 'center' }}>
                <Typography variant="h6" color="error">เกิดข้อผิดพลาด</Typography>
                <Typography color="text.secondary">{error}</Typography>
            </Paper>
        );
    }

    return (
        <div>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h4" gutterBottom>ภาพรวมการผลิตวันนี้</Typography>
                <Button variant="contained" onClick={() => navigate('/reports/new')}>
                    สร้างคำสั่งผลิต
                </Button>
                {/* Debug info */}
                <Typography variant="caption" sx={{ color: 'gray' }}>
                    Items: {summary.length}
                </Typography>
            </Box>

            {summary && summary.length > 0 ? (
                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Machine</TableCell>
                                <TableCell>Product</TableCell>
                                <TableCell align="right">Target Qty</TableCell>
                                <TableCell align="right">Good Qty</TableCell>
                                <TableCell align="right" sx={{ color: 'red' }}>NG Qty</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {summary.map((report, index) => {
                                console.log(`🔢 Rendering row ${index}:`, report);
                                return (
                                    <TableRow key={report.reportId || report.id || index} hover>
                                        <TableCell>{report.machineName || 'N/A'}</TableCell>
                                        <TableCell>{report.productName || 'N/A'}</TableCell>
                                        <TableCell align="right">{report.targetQty?.toLocaleString() || '0'}</TableCell>
                                        <TableCell align="right">{(report.currentGoodQty || report.goodQty)?.toLocaleString() || '0'}</TableCell>
                                        <TableCell align="right" sx={{ color: 'red' }}>{(report.currentNgQty || report.ngQty)?.toLocaleString() || '0'}</TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            ) : (
                <Paper sx={{ p: 4, textAlign: 'center' }}>
                    <Typography variant="h6">ไม่มีคำสั่งผลิตที่กำลังทำงานอยู่</Typography>
                    <Typography color="text.secondary">คุณสามารถสร้างคำสั่งผลิตใหม่ได้โดยกดปุ่ม "สร้างคำสั่งผลิต"</Typography>
                </Paper>
            )}
        </div>
    );
};

export default PcDashboard;