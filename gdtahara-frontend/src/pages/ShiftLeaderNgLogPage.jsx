import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import { 
    Typography, Paper, Button, Box, CircularProgress, Select, 
    MenuItem, FormControl, InputLabel, TextField, Stack
} from '@mui/material';

const ShiftLeaderNgLogPage = ({ onBack }) => {
    const [reports, setReports] = useState([]);
    const [ngTypes, setNgTypes] = useState([]);
    const [selectedReportId, setSelectedReportId] = useState('');
    const [ngData, setNgData] = useState({ ngTypeId: '', quantity: 1 });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [reportsRes, ngTypesRes] = await Promise.all([
                    axiosInstance.get('/pc/reports/active'),
                    axiosInstance.get('/master-data/ng-types')
                ]);
                setReports(reportsRes.data);
                setNgTypes(ngTypesRes.data.filter(ng => ng.ngType === 'Shift Leader'));
            } catch (error) { console.error(error); }
            finally { setLoading(false); }
        };
        fetchData();
    }, []);

    const handleSave = async () => {
        if (!selectedReportId || !ngData.ngTypeId || ngData.quantity < 1) {
            alert('กรุณากรอกข้อมูลให้ครบ');
            return;
        }
        try {
            await axiosInstance.post(`/shift-leader/reports/${selectedReportId}/ng-logs`, ngData);
            alert('บันทึกของเสียสำเร็จ');
            setNgData({ ngTypeId: '', quantity: 1 });
            setSelectedReportId('');
        } catch (err) { alert('เกิดข้อผิดพลาด'); }
    };

    if (loading) return <CircularProgress />;

    return (
        <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">บันทึกของเสีย (Shift Leader)</Typography>
                <Button variant="outlined" onClick={onBack}>กลับไปเมนูหลัก</Button>
            </Box>
            <Stack spacing={3} sx={{ maxWidth: 500 }}>
                <FormControl fullWidth>
                    <InputLabel>เลือกคำสั่งผลิต</InputLabel>
                    <Select value={selectedReportId} label="เลือกคำสั่งผลิต" onChange={e => setSelectedReportId(e.target.value)}>
                        {reports.map(r => (
                            <MenuItem key={r.id} value={r.id}>
                                {`Order: ${r.orderNumber || 'N/A'} (เครื่อง: ${r.machineName})`}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <FormControl fullWidth>
                    <InputLabel>ประเภทของเสีย</InputLabel>
                    <Select name="ngTypeId" value={ngData.ngTypeId} label="ประเภทของเสีย" onChange={e => setNgData(p => ({...p, ngTypeId: e.target.value}))}>
                        {ngTypes.map(ng => <MenuItem key={ng.id} value={ng.id}>{ng.ngDescriptionTh}</MenuItem>)}
                    </Select>
                </FormControl>
                <TextField 
                    name="quantity" 
                    label="จำนวน" 
                    type="number" 
                    value={ngData.quantity} 
                    onChange={e => setNgData(p => ({...p, quantity: e.target.value}))} 
                    InputProps={{ inputProps: { min: 1 } }}
                />
                <Button variant="contained" onClick={handleSave} disabled={!selectedReportId || !ngData.ngTypeId}>
                    บันทึก
                </Button>
            </Stack>
        </Paper>
    );
};

export default ShiftLeaderNgLogPage;