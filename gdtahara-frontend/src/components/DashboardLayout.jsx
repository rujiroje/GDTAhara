import React from 'react';
import { Box, Drawer, List, ListItem, ListItemButton, ListItemText, AppBar, Toolbar, Typography, CssBaseline, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const drawerWidth = 240;

const DashboardLayout = ({ children }) => {
    const navigate = useNavigate();
    const userRole = localStorage.getItem('userRole');

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('userRole');
        navigate('/login');
    };

    const menuItems = [
        { text: 'ภาพรวมการผลิต', path: '/dashboard', roles: ['Production Control', 'DataAdmin'] },
        { text: 'รายงานการผลิต', path: '/reports', roles: ['Production Control', 'DataAdmin'] },
    ];

    return (
        <Box sx={{ display: 'flex' }}>
            <CssBaseline />
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
                        GDTahara System
                    </Typography>
                    <Typography sx={{ mr: 2 }}>Role: {userRole}</Typography>
                    {/* [แก้ไข] เปลี่ยนปุ่ม Logout */}
                    <Button variant="contained" color="secondary" onClick={handleLogout}>
                        ออกจากระบบ
                    </Button>
                </Toolbar>
            </AppBar>
            <Drawer
                variant="permanent"
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
                }}
            >
                <Toolbar />
                <Box sx={{ overflow: 'auto' }}>
                    <List>
                        {menuItems.filter(item => item.roles.includes(userRole)).map((item) => (
                            <ListItem key={item.text} disablePadding>
                                <ListItemButton onClick={() => navigate(item.path)}>
                                    <ListItemText primary={item.text} />
                                </ListItemButton>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </Drawer>
            <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
                <Toolbar />
                {children}
            </Box>
        </Box>
    );
};

export default DashboardLayout;