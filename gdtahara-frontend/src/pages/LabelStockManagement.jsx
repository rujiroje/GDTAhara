import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Typography, Paper, Button, Box, CircularProgress, Table, TableBody,
    TableCell, TableContainer, TableHead, TableRow, Dialog, DialogTitle,
    DialogContent, TextField, DialogActions
} from '@mui/material';

const LabelStockManagement = ({ onBack }) => {
    const [stocks, setStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [quantityToAdd, setQuantityToAdd] = useState('');

    const fetchStocks = async () => {
        try {
            setLoading(true);
            const response = await axiosInstance.get('/shift-leader/label-stocks');
            setStocks(response.data);
        } catch (error) {
            console.error("Failed to fetch label stocks", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStocks();
    }, []);

    const handleOpenModal = (product) => {
        setSelectedProduct(product);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setSelectedProduct(null);
        setQuantityToAdd('');
        setIsModalOpen(false);
    };

    const handleAddStock = async () => {
        if (!selectedProduct || !quantityToAdd || parseInt(quantityToAdd, 10) <= 0) {
            alert('กรุณาใส่จำนวนให้ถูกต้อง');
            return;
        }
        try {
            await axiosInstance.post(`/shift-leader/label-stocks/${selectedProduct.productId}/add`, {
                quantityToAdd: parseInt(quantityToAdd, 10)
            });
            await fetchStocks(); // ดึงข้อมูลใหม่หลังเพิ่มสำเร็จ
            handleCloseModal();
        } catch (err) {
            alert('เกิดข้อผิดพลาดในการเพิ่มสต็อก');
        }
    };

    if (loading) return <CircularProgress />;

    return (
        <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">จัดการสต็อกป้าย (Label Stock)</Typography>
                <Button variant="outlined" onClick={onBack}>กลับไปเมนูหลัก</Button>
            </Box>

            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Product Code</TableCell>
                            <TableCell>Product Name</TableCell>
                            <TableCell align="right">Current Stock</TableCell>
                            <TableCell align="center">Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {stocks.map((stock) => (
                            <TableRow key={stock.productId} hover>
                                <TableCell>{stock.productCode}</TableCell>
                                <TableCell>{stock.productName}</TableCell>
                                <TableCell align="right">{stock.currentStock.toLocaleString()}</TableCell>
                                <TableCell align="center">
                                    <Button variant="contained" size="small" onClick={() => handleOpenModal(stock)}>
                                        เพิ่มสต็อก
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Modal for adding stock */}
            <Dialog open={isModalOpen} onClose={handleCloseModal}>
                <DialogTitle>เพิ่มสต็อกสำหรับ: {selectedProduct?.productName}</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="จำนวนที่ต้องการเพิ่ม"
                        type="number"
                        fullWidth
                        variant="standard"
                        value={quantityToAdd}
                        onChange={(e) => setQuantityToAdd(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseModal}>ยกเลิก</Button>
                    <Button onClick={handleAddStock}>ยืนยัน</Button>
                </DialogActions>
            </Dialog>
        </Paper>
    );
};

export default LabelStockManagement;