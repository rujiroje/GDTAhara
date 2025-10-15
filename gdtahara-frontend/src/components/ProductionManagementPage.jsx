import React from 'react';
import { Typography, Paper, Button, Box } from '@mui/material';
import ProductionReportsTable from '../components/ProductionReportsTable';
import { useNavigate } from 'react-router-dom';

const ProductionManagementPage = () => {
    const navigate = useNavigate();

    return (
        <Paper sx={{ p: 2, m: 1 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">รายงานการผลิต</Typography>
                <Button variant="outlined" onClick={() => navigate('/dashboard')}>กลับไปหน้าภาพรวม</Button>
            </Box>
            
            {/* เราไม่จำเป็นต้องส่ง props อีกต่อไป เพราะ Table จะจัดการเอง */}
            <ProductionReportsTable />
        </Paper>
    );
};

export default ProductionManagementPage;
