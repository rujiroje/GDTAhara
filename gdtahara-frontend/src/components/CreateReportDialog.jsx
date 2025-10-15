import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField,
    Select, MenuItem, FormControl, InputLabel, Grid, Box
} from '@mui/material';

// เพิ่ม prop initialData เพื่อรับข้อมูลเดิมมาแก้ไข
const CreateReportDialog = ({ open, onClose, onSaveSuccess, onError, activeReports, initialData }) => {
    const [machines, setMachines] = useState([]);
    const [products, setProducts] = useState([]);
    const [formData, setFormData] = useState({});
    
    // ตรวจสอบว่าเป็นโหมด "แก้ไข" หรือไม่
    const isEditMode = Boolean(initialData);

    useEffect(() => {
        // ถ้าเป็นโหมดแก้ไข ให้ใช้ข้อมูลเดิม, ถ้าสร้างใหม่ ให้ใช้ค่าว่าง
        const defaultData = {
            orderNumber: '',
            startDate: new Date().toISOString().slice(0, 10),
            endDate: new Date().toISOString().slice(0, 10),
            machineId: '',
            productId: '',
            targetQty: ''
        };
        setFormData(isEditMode ? initialData : defaultData);

        if (open) {
            const fetchDropdownData = async () => {
                try {
                    const [machinesRes, productsRes] = await Promise.all([
                        axiosInstance.get('/pc/machines'),
                        axiosInstance.get('/pc/products')
                    ]);
                    setMachines(machinesRes.data);
                    setProducts(productsRes.data);
                } catch (error) {
                    console.error("Failed to fetch dropdown data", error);
                }
            };
            fetchDropdownData();
        }
    }, [open, initialData, isEditMode]); // ให้ Effect ทำงานใหม่เมื่อ initialData เปลี่ยน

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSave = async () => {
    //           VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
    const payload = {
        ...formData,
        targetQty: parseInt(formData.targetQty, 10) || 0,
        machineId: formData.machineId, // ต้องเป็น ID อยู่แล้ว
        productId: formData.productId  // ต้องเป็น ID อยู่แล้ว
    };
    // ...
};
    
    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            {/* เปลี่ยน Title ตามโหมด */}
            <DialogTitle>{isEditMode ? 'Edit Production Report' : 'สร้างคำสั่งผลิต'}</DialogTitle>
            <DialogContent>
                <Box component="form" noValidate autoComplete="off" sx={{ pt: 1 }}>
                    <Grid container spacing={2}>
                        {/* ... โค้ด Grid item ทั้งหมดเหมือนเดิม ... */}
                        <Grid item xs={12}><TextField name="orderNumber" label="Order Number" value={formData.orderNumber || ''} onChange={handleChange} fullWidth/></Grid>
                        <Grid item xs={12}><TextField name="startDate" label="Start Date" type="date" value={formData.startDate || ''} onChange={handleChange} fullWidth InputLabelProps={{ shrink: true }}/></Grid>
                        <Grid item xs={12}><TextField name="endDate" label="End Date" type="date" value={formData.endDate || ''} onChange={handleChange} fullWidth InputLabelProps={{ shrink: true }}/></Grid>
                        <Grid item xs={12}><FormControl fullWidth><InputLabel>Machine</InputLabel><Select name="machineId" label="Machine" value={formData.machineId || ''} onChange={handleChange}>{machines.map(m => <MenuItem key={m.id} value={m.id}>{m.machineName}</MenuItem>)}</Select></FormControl></Grid>
                        <Grid item xs={12}><FormControl fullWidth><InputLabel>Product</InputLabel><Select name="productId" label="Product" value={formData.productId || ''} onChange={handleChange}>{products.map(p => <MenuItem key={p.id} value={p.id}>{p.productName}</MenuItem>)}</Select></FormControl></Grid>
                        <Grid item xs={12}><TextField name="targetQty" label="Target Quantity" type="number" value={formData.targetQty || ''} onChange={handleChange} fullWidth/></Grid>
                    </Grid>
                </Box>
            </DialogContent>
            <DialogActions sx={{ p: '16px 24px' }}>
                <Button onClick={onClose}>Cancel</Button>
                <Button onClick={handleSave} variant="contained">Save</Button>
            </DialogActions>
        </Dialog>
    );
};

export default CreateReportDialog;