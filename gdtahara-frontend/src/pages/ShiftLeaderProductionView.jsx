import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Typography, Paper, Button, Box, CircularProgress,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip
} from '@mui/material';

const ShiftLeaderProductionView = ({ onBack, onViewDetails }) => {
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchAllActiveReports = async () => {
            try {
                const response = await axiosInstance.get('/pc/reports/active');
                setReports(response.data);
            } catch (error) {
                console.error("Failed to fetch active reports", error);
            } finally {
                setLoading(false);
            }
        };
        fetchAllActiveReports();
    }, []);

    if (loading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}><CircularProgress /></Box>;
    }

    return (
        <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">ภาพรวมการผลิตทั้งหมด (ที่กำลังดำเนินการ)</Typography>
                <Button variant="outlined" onClick={onBack}>กลับไปเมนูหลัก</Button>
            </Box>

            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow>
                            {/* [แก้ไข] เพิ่มคอลัมน์ */}
                            <TableCell>Order Number</TableCell>
                            <TableCell>Start Date</TableCell>
                            <TableCell>End Date</TableCell>
                            <TableCell>Machine</TableCell>
                            <TableCell>Product</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell align="center">Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {reports.length > 0 ? (
                            reports.map((report) => (
                                <TableRow key={report.id} hover>
                                    {/* [แก้ไข] เพิ่มข้อมูล */}
                                    <TableCell>{report.orderNumber}</TableCell>
                                    <TableCell>{report.startDate}</TableCell>
                                    <TableCell>{report.endDate}</TableCell>
                                    <TableCell>{report.machineName}</TableCell>
                                    <TableCell>{report.productName}</TableCell>
                                    <TableCell>
                                        <Chip label={report.status} color="primary" size="small" />
                                    </TableCell>
                                    <TableCell align="center">
                                        <Button 
                                            variant="contained" 
                                            size="small"
                                            onClick={() => onViewDetails(report.id)}
                                        >
                                            ดูรายละเอียด
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={7} align="center">
                                    ไม่มีคำสั่งผลิตที่กำลังดำเนินการอยู่
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
        </Paper>
    );
};

export default ShiftLeaderProductionView;