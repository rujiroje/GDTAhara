import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Grid,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Tabs,
  Tab,
  Divider,
  Alert,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio
} from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { th } from 'date-fns/locale';
import SaveIcon from '@mui/icons-material/Save';
import axios from 'axios';

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`parameter-tabpanel-${index}`}
      aria-labelledby={`parameter-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const ParameterChecklistForm = ({ reportId, onSubmit, onCancel }) => {
  const [tabValue, setTabValue] = useState(0);
  const [formData, setFormData] = useState({
    reportId: reportId,
    technicianId: null,
    recordTime: new Date(),
    
    // ส่วนที่ 1: Extruder Screw
    // EVOH fields
    extruderEvohScrewRpm: '',
    extruderEvohLoLimit: '',
    extruderEvohResinPress: '',
    extruderEvohResinTemp: '',
    extruderEvohMotorCurrent: '',
    
    // Virgin fields
    extruderVirginScrewRpm: '',
    extruderVirginLoLimit: '',
    extruderVirginResinPress: '',
    extruderVirginResinTemp: '',
    extruderVirginMotorCurrent: '',
    
    // ส่วนที่ 2: Temperature
    // EVOH Temperature fields
    tempEvohFb: '',
    tempEvohC1: '',
    tempEvohC2: '',
    tempEvohC3: '',
    tempEvohA1: '',
    tempEvohA2: '',
    tempEvohH1: '',
    tempEvohH2: '',
    tempEvohH3: '',
    
    // Virgin Temperature fields
    tempVirginFb: '',
    tempVirginC1: '',
    tempVirginC2: '',
    tempVirginC3: '',
    tempVirginA1: '',
    tempVirginA2: '',
    tempVirginH1: '',
    tempVirginH2: '',
    tempVirginH3: '',
    
    // Head Temperature fields
    tempHeadD1_1: '',
    tempHeadD2_1: '',
    tempHeadD3_1: '',
    tempHeadD4_1: '',
    tempHeadD1_2: '',
    tempHeadD2_2: '',
    tempHeadD3_2: '',
    tempHeadD4_2: '',
    tempHeadD1_3: '',
    tempHeadD2_3: '',
    tempHeadD3_3: '',
    tempHeadD4_3: '',
    tempHeadD1_4: '',
    tempHeadD2_4: '',
    tempHeadD3_4: '',
    tempHeadD4_4: '',
    tempHeadL1: '',
    tempHeadL2: '',
    tempHeadL3: '',
    tempHeadL4: '',
    
    // ส่วนที่ 3: ค่าการทำงานอื่นๆ
    cycleTimeSec: '',
    moldTemp: '',
    highBlowMpa: '',
    lowPressureMpa: '',
    blowRatio: '',
    blowAirCondition1: '',
    blowAirCondition2: '',
    parisonAir1: '',
    parisonAir2: '',
    zeroValue: '',
    weightValue: '',
    spanValue: '',
    
    // ส่วนที่ 4: การตรวจสอบ
    productQualityCheck: '',
    machineOperationCheck: '',
    safetyProcedureCheck: '',
    additionalNotes: ''
  });
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    // โหลดข้อมูล technician จาก localStorage หรือ context
    const userId = localStorage.getItem('userId');
    if (userId) {
      setFormData(prev => ({ ...prev, technicianId: parseInt(userId) }));
    }
  }, []);

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  const handleInputChange = (field) => (event) => {
    const value = event.target.value;
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleDateTimeChange = (newValue) => {
    setFormData(prev => ({ ...prev, recordTime: newValue }));
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const submitData = {
        ...formData,
        recordTime: formData.recordTime.toISOString()
      };
      
      const response = await axios.post(
        `http://localhost:8080/api/technician/parameter-records`,
        submitData
      );
      
      setSuccess(true);
      setTimeout(() => {
        if (onSubmit) onSubmit(response.data);
      }, 1500);
      
    } catch (err) {
      console.error('Error saving parameter record:', err);
      setError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการบันทึกข้อมูล');
    } finally {
      setLoading(false);
    }
  };

  const renderExtruderSection = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom color="primary">
          ส่วนที่ 1: Extruder Screw Parameters
        </Typography>
      </Grid>
      
      {/* EVOH Section */}
      <Grid item xs={12}>
        <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 'bold', color: 'secondary.main' }}>
          EVOH Parameters
        </Typography>
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder EVOH Screw RPM"
          type="number"
          value={formData.extruderEvohScrewRpm}
          onChange={handleInputChange('extruderEvohScrewRpm')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder EVOH LO Limit"
          type="number"
          value={formData.extruderEvohLoLimit}
          onChange={handleInputChange('extruderEvohLoLimit')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder EVOH Resin Press"
          type="number"
          value={formData.extruderEvohResinPress}
          onChange={handleInputChange('extruderEvohResinPress')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder EVOH Resin Temp"
          type="number"
          value={formData.extruderEvohResinTemp}
          onChange={handleInputChange('extruderEvohResinTemp')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder EVOH Motor Current"
          type="number"
          value={formData.extruderEvohMotorCurrent}
          onChange={handleInputChange('extruderEvohMotorCurrent')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>

      <Grid item xs={12}>
        <Divider sx={{ my: 2 }} />
        <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 'bold', color: 'secondary.main' }}>
          Virgin Parameters
        </Typography>
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder Virgin Screw RPM"
          type="number"
          value={formData.extruderVirginScrewRpm}
          onChange={handleInputChange('extruderVirginScrewRpm')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder Virgin LO Limit"
          type="number"
          value={formData.extruderVirginLoLimit}
          onChange={handleInputChange('extruderVirginLoLimit')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder Virgin Resin Press"
          type="number"
          value={formData.extruderVirginResinPress}
          onChange={handleInputChange('extruderVirginResinPress')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder Virgin Resin Temp"
          type="number"
          value={formData.extruderVirginResinTemp}
          onChange={handleInputChange('extruderVirginResinTemp')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Extruder Virgin Motor Current"
          type="number"
          value={formData.extruderVirginMotorCurrent}
          onChange={handleInputChange('extruderVirginMotorCurrent')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
    </Grid>
  );

  const renderTemperatureSection = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom color="primary">
          ส่วนที่ 2: Temperature Parameters
        </Typography>
      </Grid>
      
      {/* EVOH Temperature */}
      <Grid item xs={12}>
        <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 'bold', color: 'secondary.main' }}>
          EVOH Temperature
        </Typography>
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp EVOH FB"
          type="number"
          value={formData.tempEvohFb}
          onChange={handleInputChange('tempEvohFb')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp EVOH C1"
          type="number"
          value={formData.tempEvohC1}
          onChange={handleInputChange('tempEvohC1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp EVOH C2"
          type="number"
          value={formData.tempEvohC2}
          onChange={handleInputChange('tempEvohC2')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp EVOH C3"
          type="number"
          value={formData.tempEvohC3}
          onChange={handleInputChange('tempEvohC3')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp EVOH A1"
          type="number"
          value={formData.tempEvohA1}
          onChange={handleInputChange('tempEvohA1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp EVOH A2"
          type="number"
          value={formData.tempEvohA2}
          onChange={handleInputChange('tempEvohA2')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>

      <Grid item xs={12}>
        <Divider sx={{ my: 2 }} />
        <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 'bold', color: 'secondary.main' }}>
          Virgin Temperature
        </Typography>
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp Virgin FB"
          type="number"
          value={formData.tempVirginFb}
          onChange={handleInputChange('tempVirginFb')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp Virgin C1"
          type="number"
          value={formData.tempVirginC1}
          onChange={handleInputChange('tempVirginC1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Temp Virgin C2"
          type="number"
          value={formData.tempVirginC2}
          onChange={handleInputChange('tempVirginC2')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>

      <Grid item xs={12}>
        <Divider sx={{ my: 2 }} />
        <Typography variant="subtitle1" gutterBottom sx={{ fontWeight: 'bold', color: 'secondary.main' }}>
          Head Temperature D Series
        </Typography>
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head D1-1"
          type="number"
          value={formData.tempHeadD1_1}
          onChange={handleInputChange('tempHeadD1_1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head D2-1"
          type="number"
          value={formData.tempHeadD2_1}
          onChange={handleInputChange('tempHeadD2_1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head D3-1"
          type="number"
          value={formData.tempHeadD3_1}
          onChange={handleInputChange('tempHeadD3_1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head D4-1"
          type="number"
          value={formData.tempHeadD4_1}
          onChange={handleInputChange('tempHeadD4_1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>

      <Grid item xs={12}>
        <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 'bold', color: 'text.secondary' }}>
          Head Temperature L Series
        </Typography>
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head L1"
          type="number"
          value={formData.tempHeadL1}
          onChange={handleInputChange('tempHeadL1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head L2"
          type="number"
          value={formData.tempHeadL2}
          onChange={handleInputChange('tempHeadL2')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head L3"
          type="number"
          value={formData.tempHeadL3}
          onChange={handleInputChange('tempHeadL3')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <TextField
          fullWidth
          label="Temp Head L4"
          type="number"
          value={formData.tempHeadL4}
          onChange={handleInputChange('tempHeadL4')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
    </Grid>
  );

  const renderOtherValuesSection = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom color="primary">
          ส่วนที่ 3: ค่าการทำงานอื่นๆ
        </Typography>
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Cycle Time (sec)"
          type="number"
          value={formData.cycleTimeSec}
          onChange={handleInputChange('cycleTimeSec')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Mold Temperature"
          type="number"
          value={formData.moldTemp}
          onChange={handleInputChange('moldTemp')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="High Blow (MPa)"
          type="number"
          value={formData.highBlowMpa}
          onChange={handleInputChange('highBlowMpa')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Low Pressure (MPa)"
          type="number"
          value={formData.lowPressureMpa}
          onChange={handleInputChange('lowPressureMpa')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Blow Ratio"
          type="number"
          value={formData.blowRatio}
          onChange={handleInputChange('blowRatio')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Blow Air Condition 1"
          type="number"
          value={formData.blowAirCondition1}
          onChange={handleInputChange('blowAirCondition1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Blow Air Condition 2"
          type="number"
          value={formData.blowAirCondition2}
          onChange={handleInputChange('blowAirCondition2')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Parison Air 1"
          type="number"
          value={formData.parisonAir1}
          onChange={handleInputChange('parisonAir1')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Parison Air 2"
          type="number"
          value={formData.parisonAir2}
          onChange={handleInputChange('parisonAir2')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Zero Value"
          type="number"
          value={formData.zeroValue}
          onChange={handleInputChange('zeroValue')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Weight Value"
          type="number"
          value={formData.weightValue}
          onChange={handleInputChange('weightValue')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <TextField
          fullWidth
          label="Span Value"
          type="number"
          value={formData.spanValue}
          onChange={handleInputChange('spanValue')}
          inputProps={{ step: "0.1" }}
        />
      </Grid>
    </Grid>
  );

  const renderChecksSection = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom color="primary">
          ส่วนที่ 4: การตรวจสอบ
        </Typography>
      </Grid>
      
      <Grid item xs={12}>
        <FormControl component="fieldset">
          <FormLabel component="legend">การตรวจสอบคุณภาพผลิตภัณฑ์</FormLabel>
          <RadioGroup
            value={formData.productQualityCheck}
            onChange={(e) => setFormData(prev => ({ ...prev, productQualityCheck: e.target.value }))}
            row
          >
            <FormControlLabel value="ปกติ" control={<Radio />} label="ปกติ" />
            <FormControlLabel value="ผิดปกติ" control={<Radio />} label="ผิดปกติ" />
            <FormControlLabel value="ต้องปรับปรุง" control={<Radio />} label="ต้องปรับปรุง" />
          </RadioGroup>
        </FormControl>
      </Grid>
      
      <Grid item xs={12}>
        <FormControl component="fieldset">
          <FormLabel component="legend">การตรวจสอบการทำงานของเครื่องจักร</FormLabel>
          <RadioGroup
            value={formData.machineOperationCheck}
            onChange={(e) => setFormData(prev => ({ ...prev, machineOperationCheck: e.target.value }))}
            row
          >
            <FormControlLabel value="ปกติ" control={<Radio />} label="ปกติ" />
            <FormControlLabel value="ผิดปกติ" control={<Radio />} label="ผิดปกติ" />
            <FormControlLabel value="ต้องบำรุงรักษา" control={<Radio />} label="ต้องบำรุงรักษา" />
          </RadioGroup>
        </FormControl>
      </Grid>
      
      <Grid item xs={12}>
        <FormControl component="fieldset">
          <FormLabel component="legend">การตรวจสอบความปลอดภัย</FormLabel>
          <RadioGroup
            value={formData.safetyProcedureCheck}
            onChange={(e) => setFormData(prev => ({ ...prev, safetyProcedureCheck: e.target.value }))}
            row
          >
            <FormControlLabel value="ปลอดภัย" control={<Radio />} label="ปลอดภัย" />
            <FormControlLabel value="มีความเสี่ยง" control={<Radio />} label="มีความเสี่ยง" />
            <FormControlLabel value="อันตราย" control={<Radio />} label="อันตราย" />
          </RadioGroup>
        </FormControl>
      </Grid>
      
      <Grid item xs={12}>
        <TextField
          fullWidth
          label="หมายเหตุเพิ่มเติม"
          multiline
          rows={4}
          value={formData.additionalNotes}
          onChange={handleInputChange('additionalNotes')}
          placeholder="กรุณาระบุรายละเอียดเพิ่มเติม หรือข้อสังเกต..."
        />
      </Grid>
    </Grid>
  );

  return (
    <Box sx={{ width: '100%' }}>
      <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h5" gutterBottom color="primary" sx={{ mb: 3 }}>
          Parameter Checklist - บันทึกพารามิเตอร์
        </Typography>

        {/* เวลาบันทึก */}
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} md={6}>
            <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={th}>
              <DateTimePicker
                label="เวลาที่บันทึก"
                value={formData.recordTime}
                onChange={handleDateTimeChange}
                renderInput={(params) => <TextField {...params} fullWidth />}
                ampm={false}
                format="dd/MM/yyyy HH:mm"
              />
            </LocalizationProvider>
          </Grid>
        </Grid>

        {/* Alerts */}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        
        {success && (
          <Alert severity="success" sx={{ mb: 2 }}>
            บันทึกข้อมูลพารามิเตอร์เรียบร้อยแล้ว
          </Alert>
        )}

        {/* Tabs */}
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="parameter sections">
            <Tab label="Extruder Screw" />
            <Tab label="Temperature" />
            <Tab label="Other Values" />
            <Tab label="Checks" />
          </Tabs>
        </Box>

        {/* Tab Panels */}
        <TabPanel value={tabValue} index={0}>
          {renderExtruderSection()}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          {renderTemperatureSection()}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          {renderOtherValuesSection()}
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          {renderChecksSection()}
        </TabPanel>

        {/* Action Buttons */}
        <Box sx={{ mt: 4, display: 'flex', justifyContent: 'space-between' }}>
          <Button
            variant="outlined"
            onClick={onCancel}
            disabled={loading}
          >
            ยกเลิก
          </Button>

          <Button
            variant="contained"
            startIcon={<SaveIcon />}
            onClick={handleSubmit}
            disabled={loading}
            sx={{ minWidth: 120 }}
          >
            {loading ? 'กำลังบันทึก...' : 'บันทึกข้อมูล'}
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default ParameterChecklistForm;