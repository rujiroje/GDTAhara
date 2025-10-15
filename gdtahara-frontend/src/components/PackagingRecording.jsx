import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import { Typography, Box, Button, TextField, Paper } from '@mui/material';

const PackagingRecording = ({ report, onBack }) => {
    const [packagingInfo, setPackagingInfo] = useState({ lotNumber: '', boxNo: '' });
    const [isLotLocked, setIsLotLocked] = useState(false);

    // Fetch next box number when lot number changes
    useEffect(() => {
        const fetchNextBoxNo = async () => {
            if (report && packagingInfo.lotNumber) {
                try {
                    const response = await axiosInstance.get(`/operator/reports/${report.id}/next-box-no?lotNumber=${packagingInfo.lotNumber}`);
                    setPackagingInfo(prev => ({ ...prev, boxNo: response.data }));
                } catch (err) {
                    console.error("Could not fetch next box number", err);
                }
            }
        };
        
        const timerId = setTimeout(() => {
            if (!isLotLocked) {
                fetchNextBoxNo();
            }
        }, 500); // Debounce to avoid too many requests

        return () => clearTimeout(timerId);
    }, [packagingInfo.lotNumber, report, isLotLocked]);

    const handleRecordPackaging = async () => {
        if (!packagingInfo.lotNumber || !packagingInfo.boxNo) {
            alert('กรุณากรอก Lot Number');
            return;
        }
        try {
            await axiosInstance.post(`/operator/reports/${report.id}/packaging-logs`, {
                lotNumber: packagingInfo.lotNumber,
                boxNo: parseInt(packagingInfo.boxNo, 10)
            });
            setIsLotLocked(true); // Lock lot number after first successful packaging
            // Fetch the next box number immediately
            const response = await axiosInstance.get(`/operator/reports/${report.id}/next-box-no?lotNumber=${packagingInfo.lotNumber}`);
            setPackagingInfo(prev => ({ ...prev, boxNo: response.data }));
        } catch (err) {
            alert(err.response?.data || 'เกิดข้อผิดพลาดในการบันทึกการแพ็ค');
        }
    };

    return (
        <Box>
            <Button variant="outlined" onClick={onBack} sx={{ mb: 2 }}>
                &larr; กลับไปเลือกงาน
            </Button>
            <Typography variant="h6" gutterBottom>บันทึกการบรรจุ (Packaging)</Typography>
            
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <TextField
                    label="Lot Number"
                    value={packagingInfo.lotNumber}
                    onChange={(e) => setPackagingInfo({ lotNumber: e.target.value, boxNo: '' })}
                    disabled={isLotLocked}
                    fullWidth
                />
                {isLotLocked && (
                    <Button onClick={() => setIsLotLocked(false)}>แก้ไข</Button>
                )}
            </Box>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Paper variant="outlined" sx={{ p: 2, flexGrow: 1, textAlign: 'center' }}>
                    <Typography>Box No.</Typography>
                    <Typography variant="h4">{packagingInfo.boxNo || '-'}</Typography>
                </Paper>
                <Button 
                    variant="contained" 
                    sx={{ height: '80px', width: '200px' }}
                    onClick={handleRecordPackaging}
                >
                    บันทึก 1 กล่อง
                </Button>
            </Box>
        </Box>
    );
};

export default PackagingRecording;
