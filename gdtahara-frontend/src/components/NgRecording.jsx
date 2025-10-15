import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import { Typography, Box, Button, Grid, CircularProgress, Dialog, DialogTitle, DialogContent, TextField, DialogActions, Snackbar, Alert } from '@mui/material';

const NgRecording = ({ report, onBack }) => {
    const [ngTypes, setNgTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isAlertModalOpen, setIsAlertModalOpen] = useState(false);
    const [alertReason, setAlertReason] = useState('');
    const [feedback, setFeedback] = useState({ open: false, message: '', severity: 'success' });

    useEffect(() => {
        const fetchNgTypes = async () => {
            try {
                const response = await axiosInstance.get('/master-data/ng-types');
                setNgTypes(response.data.filter(ng => ng.ngType === 'Operator'));
            } catch (err) {
                console.error("Could not fetch NG types", err);
            } finally {
                setLoading(false);
            }
        };
        fetchNgTypes();
    }, []);

    const handleRecordNg = async (ngTypeId) => {
        try {
            await axiosInstance.post(`/operator/reports/${report.id}/ng-logs`, {
                ngTypeId: ngTypeId,
                quantity: 1,
                source: 'Operator_Run'
            });
            setFeedback({ open: true, message: 'บันทึกของเสียสำเร็จ', severity: 'success' });
        } catch (err) {
            setFeedback({ open: true, message: 'เกิดข้อผิดพลาดในการบันทึก NG', severity: 'error' });
        }
    };

    const handleAlertSubmit = async (e) => {
        e.preventDefault();
        try {
            await axiosInstance.post(`/operator/reports/${report.id}/alert`, { message: alertReason });
            setFeedback({ open: true, message: 'แจ้งปัญหาสำเร็จ', severity: 'info' });
            setIsAlertModalOpen(false);
            setAlertReason('');
        } catch (err) {
            setFeedback({ open: true, message: 'เกิดข้อผิดพลาดในการแจ้งปัญหา', severity: 'error' });
        }
    };

    const handleCloseFeedback = () => {
        setFeedback({ ...feedback, open: false });
    };

    if (loading) return <CircularProgress />;

    // Array ของสีสำหรับปุ่ม
    const buttonColors = [
        'primary', 'secondary', 'success', 'info', 'warning', 'error'
    ];

    return (
        <Box>
            <Button variant="outlined" onClick={onBack} sx={{ mb: 2 }}>
                &larr; กลับไปเลือกงาน
            </Button>
            <Typography variant="h6" gutterBottom>บันทึกของเสีย (NG)</Typography>
            <Grid container spacing={2}>
                {ngTypes.map((ng, index) => (
                    <Grid item xs={6} sm={4} md={3} key={ng.id}>
                        <Button
                            variant="contained" // เปลี่ยนเป็น contained เพื่อให้เห็นสีชัดเจน
                            color={buttonColors[index % buttonColors.length]} // วน Loop สี
                            fullWidth
                            sx={{ 
                                height: '80px', 
                                textTransform: 'none', 
                                p: 1, 
                                whiteSpace: 'normal',
                                color: 'white' // ทำให้ตัวอักษรเป็นสีขาว
                            }}
                            onClick={() => handleRecordNg(ng.id)}
                        >
                            {ng.ngDescriptionTh}
                        </Button>
                    </Grid>
                ))}
            </Grid>
            <Box sx={{ mt: 4, borderTop: 1, borderColor: 'divider', pt: 2 }}>
                <Button fullWidth variant="contained" color="error" onClick={() => setIsAlertModalOpen(true)}>
                    แจ้งเครื่องจักรหยุด
                </Button>
            </Box>

            <Dialog open={isAlertModalOpen} onClose={() => setIsAlertModalOpen(false)}>
                {/* ... โค้ด Dialog เหมือนเดิม ... */}
            </Dialog>

            <Snackbar open={feedback.open} autoHideDuration={4000} onClose={handleCloseFeedback}>
                <Alert onClose={handleCloseFeedback} severity={feedback.severity} sx={{ width: '100%' }}>
                    {feedback.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default NgRecording;
