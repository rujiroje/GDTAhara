import React, { useState } from 'react';
import { Box, Tabs, Tab } from '@mui/material';
import InventoryIcon from '@mui/icons-material/Inventory';
import ScienceIcon from '@mui/icons-material/Science';
import BarChartIcon from '@mui/icons-material/BarChart';

import CmOperatorDashboard from './CmOperatorDashboard';
import CmMeterDashboard from '../cm-meter/CmMeterDashboard';
import CmStockDashboard from '../cm-stock/CmStockDashboard';

const CmOperatorHub = () => {
  const [tab, setTab] = useState(0);

  return (
    <Box>
      <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          sx={{ px: 2 }}
        >
          <Tab
            icon={<InventoryIcon />}
            iconPosition="start"
            label="เบิก BOM"
            sx={{ minHeight: 52, minWidth: 140 }}
          />
          <Tab
            icon={<ScienceIcon />}
            iconPosition="start"
            label="บันทึกวัตถุดิบ"
            sx={{ minHeight: 52, minWidth: 160 }}
          />
          <Tab
            icon={<BarChartIcon />}
            iconPosition="start"
            label="สต็อกวัตถุดิบ"
            sx={{ minHeight: 52, minWidth: 160 }}
          />
        </Tabs>
      </Box>

      <Box hidden={tab !== 0}>
        {tab === 0 && <CmOperatorDashboard />}
      </Box>
      <Box hidden={tab !== 1}>
        {tab === 1 && <CmMeterDashboard />}
      </Box>
      <Box hidden={tab !== 2}>
        {tab === 2 && <CmStockDashboard />}
      </Box>
    </Box>
  );
};

export default CmOperatorHub;
