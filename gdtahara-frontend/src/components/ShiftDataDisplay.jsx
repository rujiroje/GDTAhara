import React from 'react';
import { Typography, Paper, Box, Grid, List, ListItem, ListItemText } from '@mui/material';

const ShiftDataDisplay = ({ title, data }) => {
    if (!data) {
        return (
            <Paper sx={{ p: 2, mb: 3 }}>
                <Typography variant="h6" gutterBottom>{title}</Typography>
                <Typography>ไม่มีข้อมูลสำหรับกะนี้</Typography>
            </Paper>
        );
    }

    return (
        <Paper sx={{ p: 2, mb: 3, backgroundColor: '#f9fafb' }}>
            <Typography variant="h6" gutterBottom>{title}</Typography>
            <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={6} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography variant="subtitle2">ผลิตดี (กล่อง)</Typography><Typography variant="h5" color="primary">{data.goodProductionBoxes}</Typography></Paper></Grid>
                <Grid item xs={6} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography variant="subtitle2">ของเสีย (ชิ้น)</Typography><Typography variant="h5" color="error">{data.ngProductionPieces}</Typography></Paper></Grid>
                <Grid item xs={6} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography variant="subtitle2">ผลิตรวม (ชิ้น)</Typography><Typography variant="h5">{data.totalProductionPieces}</Typography></Paper></Grid>
                <Grid item xs={6} sm={3}><Paper sx={{ p: 2, textAlign: 'center' }}><Typography variant="subtitle2">Yield</Typography><Typography variant="h5" color="green">{data.yieldPercentage}</Typography></Paper></Grid>
            </Grid>
            
            {/* [แก้ไข] เพิ่มส่วนแสดงผล NG Summary */}
            <Box mt={2}>
                <Typography variant="subtitle1" fontWeight="bold">สรุปยอดของเสีย:</Typography>
                {data.ngSummary.length > 0 ? (
                    <Paper variant="outlined" sx={{ mt: 1 }}>
                        <List dense>
                            {data.ngSummary.map(ng => (
                                <ListItem key={ng.ngDescription} divider>
                                    <ListItemText primary={ng.ngDescription} />
                                    <Typography variant="body2">{ng.count} ชิ้น</Typography>
                                </ListItem>
                            ))}
                        </List>
                    </Paper>
                ) : (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        ไม่มีข้อมูลของเสียในกะนี้
                    </Typography>
                )}
            </Box>
        </Paper>
    );
};

export default ShiftDataDisplay;