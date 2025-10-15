import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import {
    Typography, Paper, Button, Box, CircularProgress, Table, TableBody,
    TableCell, TableContainer, TableHead, TableRow, Dialog, DialogTitle,
    DialogContent, TextField, DialogActions, Select, MenuItem, FormControl, 
    InputLabel, Stack // <--- เพิ่ม Stack เข้ามาใน import นี้
} from '@mui/material';

// Component ย่อยสำหรับแสดง Stock Card
const StockCardView = ({ material, onBack }) => {
    const [stockCard, setStockCard] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchCard = async () => {
            try {
                const res = await axiosInstance.get(`/shift-leader/stock-card/${material.id}`);
                setStockCard(res.data);
            } catch (error) {
                console.error("Failed to fetch stock card", error);
            } finally {
                setLoading(false);
            }
        };
        fetchCard();
    }, [material.id]);

    if (loading) return <CircularProgress />;

    return (
        <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Stock Card: {stockCard?.materialName || material.materialName}</Typography>
                <Button variant="outlined" size="small" onClick={onBack}>กลับ</Button>
            </Box>
            <TableContainer>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Timestamp</TableCell>
                            <TableCell>Type</TableCell>
                            <TableCell>Lot</TableCell>
                            <TableCell>Report</TableCell>
                            <TableCell align="right">Qty</TableCell>
                            <TableCell>User</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {stockCard?.transactions.map((tx, i) => (
                            <TableRow key={i} sx={{ backgroundColor: tx.transactionType === 'IN' ? '#e8f5e9' : '#fffbe6' }}>
                                <TableCell>{new Date(tx.timestamp).toLocaleString()}</TableCell>
                                <TableCell>{tx.transactionType}</TableCell>
                                <TableCell>{tx.lotNumber}</TableCell>
                                <TableCell>{tx.reportIdentifier}</TableCell>
                                <TableCell align="right">{tx.quantity.toLocaleString()}</TableCell>
                                <TableCell>{tx.userName}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </>
    );
};

// Component หลัก
const MaterialStockManagement = ({ onBack }) => {
    const [materials, setMaterials] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [stockInData, setStockInData] = useState({ materialId: '', quantity: '', lotNumber: '' });
    const [view, setView] = useState('list'); // 'list' or 'stock_card'
    const [selectedMaterial, setSelectedMaterial] = useState(null);

    const fetchMaterials = async () => {
        try {
            setLoading(true);
            const response = await axiosInstance.get('/shift-leader/materials-with-stock');
            setMaterials(response.data);
        } catch (error) { console.error("Failed to fetch materials", error); }
        finally { setLoading(false); }
    };

    useEffect(() => {
        if (view === 'list') {
            fetchMaterials();
        }
    }, [view]);

    const handleOpenModal = () => setIsModalOpen(true);
    const handleCloseModal = () => {
        setIsModalOpen(false);
        setStockInData({ materialId: '', quantity: '', lotNumber: '' });
    };

    const handleStockIn = async () => {
        if (!stockInData.materialId || !stockInData.quantity || !stockInData.lotNumber) {
            alert('กรุณากรอกข้อมูลให้ครบถ้วน');
            return;
        }
        try {
            await axiosInstance.post('/shift-leader/stock-in', stockInData);
            await fetchMaterials();
            handleCloseModal();
        } catch (err) { alert('เกิดข้อผิดพลาดในการรับเข้าสต็อก'); }
    };
    
    const viewStockCard = (material) => {
        setSelectedMaterial(material);
        setView('stock_card');
    };

    if (loading) return <CircularProgress />;

    if (view === 'stock_card') {
        return <StockCardView material={selectedMaterial} onBack={() => setView('list')} />;
    }

    return (
        <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">จัดการสต็อกวัตถุดิบ</Typography>
                <div>
                    <Button variant="contained" onClick={handleOpenModal} sx={{ mr: 1 }}>รับเข้าสต็อก (Stock-In)</Button>
                    <Button variant="outlined" onClick={onBack}>กลับไปเมนูหลัก</Button>
                </div>
            </Box>

            <TableContainer>
                <Table><TableHead><TableRow><TableCell>Code</TableCell><TableCell>Name</TableCell><TableCell align="right">Current Stock</TableCell><TableCell>Unit</TableCell><TableCell align="center">Actions</TableCell></TableRow></TableHead>
                    <TableBody>
                        {materials.map((mat) => (
                            <TableRow key={mat.id} hover>
                                <TableCell>{mat.materialCode}</TableCell><TableCell>{mat.materialName}</TableCell><TableCell align="right">{mat.currentStock.toLocaleString()}</TableCell><TableCell>{mat.unit}</TableCell>
                                <TableCell align="center"><Button size="small" onClick={() => viewStockCard(mat)}>ดู Stock Card</Button></TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog open={isModalOpen} onClose={handleCloseModal}>
                <DialogTitle>รับวัตถุดิบเข้าสต็อก (Stock-In)</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ pt: 1, minWidth: 400 }}>
                        <FormControl fullWidth>
                            <InputLabel>วัตถุดิบ</InputLabel>
                            <Select name="materialId" value={stockInData.materialId} label="วัตถุดิบ" onChange={(e) => setStockInData(p => ({ ...p, materialId: e.target.value }))}>
                                {materials.map(mat => <MenuItem key={mat.id} value={mat.id}>{mat.materialName}</MenuItem>)}
                            </Select>
                        </FormControl>
                        <TextField label="Lot Number" name="lotNumber" value={stockInData.lotNumber} onChange={(e) => setStockInData(p => ({ ...p, lotNumber: e.target.value }))} />
                        <TextField label="จำนวน" type="number" name="quantity" value={stockInData.quantity} onChange={(e) => setStockInData(p => ({ ...p, quantity: e.target.value }))} />
                    </Stack>
                </DialogContent>
                <DialogActions><Button onClick={handleCloseModal}>ยกเลิก</Button><Button onClick={handleStockIn} variant="contained">ยืนยัน</Button></DialogActions>
            </Dialog>
        </Paper>
    );
};

export default MaterialStockManagement;