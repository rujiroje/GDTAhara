import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axiosInstance from '../api/axios';
import {
    Paper, Typography, Button, Box, TextField, Select,
    MenuItem, FormControl, InputLabel, CircularProgress, Stack // 1. Import Stack
} from '@mui/material';

const ReportFormPage = () => {
    const { reportId } = useParams();
    const navigate = useNavigate();
    const isEditMode = Boolean(reportId);

    const [machines, setMachines] = useState([]);
    const [products, setProducts] = useState([]);
    const [formData, setFormData] = useState({
        orderNumber: '',
        startDate: new Date().toISOString().slice(0, 10),
        endDate: new Date().toISOString().slice(0, 10),
        machineId: '',
        productId: '',
        targetQty: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchInitialData = async () => {
            setLoading(true);
            try {
                const [machinesRes, productsRes] = await Promise.all([
                    axiosInstance.get('/pc/machines'),
                    axiosInstance.get('/pc/products')
                ]);
                setMachines(machinesRes.data);
                setProducts(productsRes.data);

                if (isEditMode) {
                    const { data: reportData } = await axiosInstance.get(`/pc/reports/${reportId}`);
                    setFormData({
                        ...reportData,
                        machineId: reportData.machine.id,
                        productId: reportData.product.id
                    });
                }
            } catch (err) {
                setError('Failed to load data');
            } finally {
                setLoading(false);
            }
        };
        fetchInitialData();
    }, [reportId, isEditMode]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        const payload = {
            orderNumber: formData.orderNumber,
            startDate: formData.startDate,
            endDate: formData.endDate,
            machineId: formData.machineId,
            productId: formData.productId,
            targetQty: parseInt(formData.targetQty, 10) || 0
        };
        try {
            if (isEditMode) {
                await axiosInstance.put(`/pc/reports/${reportId}`, payload);
            } else {
                await axiosInstance.post('/pc/reports', payload);
            }
            navigate('/reports');
        } catch (err) {
            setError(err.response?.data || 'Error saving report');
            setLoading(false);
        }
    };

    if (loading && !formData.id) return <CircularProgress />;

    return (
        <Paper sx={{ p: 3, maxWidth: 700, margin: 'auto' }}>
            <Typography variant="h5" gutterBottom>
                {isEditMode ? 'แก้ไขคำสั่งผลิต' : 'สร้างคำสั่งผลิตใหม่'}
            </Typography>
            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
                {/* 2. ใช้ Stack ในการจัดเรียง Layout ในแนวตั้ง */}
                <Stack spacing={3}>
                    <TextField name="orderNumber" label="Order Number" value={formData.orderNumber || ''} onChange={handleChange} fullWidth />
                    
                    <FormControl fullWidth>
                        <InputLabel>Machine</InputLabel>
                        <Select name="machineId" label="Machine" value={formData.machineId || ''} onChange={handleChange}>
                            {machines.map(m => <MenuItem key={m.id} value={m.id}>{m.machineName}</MenuItem>)}
                        </Select>
                    </FormControl>

                    <FormControl fullWidth>
                        <InputLabel>Product</InputLabel>
                        <Select name="productId" label="Product" value={formData.productId || ''} onChange={handleChange}>
                            {products.map(p => <MenuItem key={p.id} value={p.id}>{p.productName}</MenuItem>)}
                        </Select>
                    </FormControl>

                    {/* ใช้ Box เพื่อจัดกลุ่มวันที่ให้อยู่แนวนอน */}
                    <Box sx={{ display: 'flex', gap: 2 }}>
                        <TextField name="startDate" label="Start Date" type="date" value={formData.startDate || ''} onChange={handleChange} fullWidth InputLabelProps={{ shrink: true }} />
                        <TextField name="endDate" label="End Date" type="date" value={formData.endDate || ''} onChange={handleChange} fullWidth InputLabelProps={{ shrink: true }} />
                    </Box>

                    <TextField name="targetQty" label="Target Quantity" type="number" value={formData.targetQty || ''} onChange={handleChange} fullWidth />
                    
                    {error && <Typography color="error" align="center">{error}</Typography>}

                    <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, borderTop: 1, borderColor: 'divider', pt: 2, mt: 1 }}>
                        <Button variant="outlined" onClick={() => navigate('/reports')}>ยกเลิก</Button>
                        <Button type="submit" variant="contained" disabled={loading}>
                            {loading ? 'กำลังบันทึก...' : 'บันทึกคำสั่งผลิต'}
                        </Button>
                    </Box>
                </Stack>
            </Box>
        </Paper>
    );
};

export default ReportFormPage;