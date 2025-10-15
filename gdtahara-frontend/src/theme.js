// =================================================================
// File: src/theme.js (ไฟล์ใหม่)
// =================================================================
import { createTheme } from '@mui/material/styles';

// สร้าง Theme กลางสำหรับ Material-UI (สามารถปรับแต่งสี ฟอนต์ ฯลฯ ได้ที่นี่)
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
  },
});

export default theme;
