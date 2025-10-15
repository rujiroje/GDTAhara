import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import { useNavigate } from 'react-router-dom'; // 1. Import useNavigate
import {
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper,
    Box, Typography, Button, Chip
} from '@mui/material';

const ProductionReportsTable = () => { 
    const navigate = useNavigate(); // 2. สร้าง instance ของ navigate
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchReports = async () => {
        try {
            setLoading(true);
            const response = await axiosInstance.get('/pc/reports');
            setReports(response.data.sort((a, b) => b.id - a.id));
        } catch (err) {
            setError('ไม่สามารถดึงข้อมูลใบสั่งผลิตได้');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchReports();
    }, []);
    
    const handleFinalize = async (reportId) => {
        if (window.confirm('คุณต้องการปิดงานใบสั่งผลิตนี้ใช่หรือไม่?')) {
            try {
                await axiosInstance.post(`/pc/reports/${reportId}/finalize`);
                fetchReports(); 
            } catch (err) {
                alert('เกิดข้อผิดพลาดในการปิดงาน');
            }
        }
    };

    if (loading) return <Typography>Loading reports...</Typography>;
    if (error) return <Typography color="error">{error}</Typography>;

    return (
        <TableContainer component={Paper}>
            <Table>
                <TableHead>
                    <TableRow>
                        <TableCell sx={{ fontWeight: 'bold' }}>Order Number</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Start Date</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>End Date</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Machine</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Product</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
                        <TableCell align="center" sx={{ fontWeight: 'bold' }}>Actions</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {reports.map((report) => (
                        <TableRow key={report.id} hover>
                            <TableCell>{report.orderNumber}</TableCell>
                            <TableCell>{report.startDate}</TableCell>
                            <TableCell>{report.endDate}</TableCell>
                            <TableCell>{report.machineName}</TableCell>
                            <TableCell>{report.productName}</TableCell>
                            <TableCell>
                                <Chip
                                    label={report.status}
                                    color={report.status === 'In Progress' ? 'primary' : 'success'}
                                    size="small"
                                />
                            </TableCell>
                            <TableCell align="center">
                                <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                                    {/* 3. เปลี่ยน onClick ให้ใช้ navigate */}
                                    <Button size="small" variant="outlined" onClick={() => navigate(`/reports/summary/${report.id}`)}>View Summary</Button>
                                    <Button size="small" variant="outlined" color="warning" disabled={!report.editable} onClick={() => navigate(`/reports/edit/${report.id}`)}>Edit</Button>
                                    <Button size="small" variant="contained" color="secondary" disabled={!report.finalizable} onClick={() => handleFinalize(report.id)}>Finalize</Button>
                                </Box>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
};

export default ProductionReportsTable;
