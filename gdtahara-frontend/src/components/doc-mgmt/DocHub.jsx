import React, { useState } from 'react';
import { Box, Tabs, Tab } from '@mui/material';
import AssessmentIcon from '@mui/icons-material/Assessment';
import EditNoteIcon from '@mui/icons-material/EditNote';
import DocMgmtDashboard from './DocMgmtDashboard';
import DocWasteRequestDashboard from './DocWasteRequestDashboard';

const DocHub = () => {
  const [tab, setTab] = useState(0);

  return (
    <Box>
      <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ px: 2 }}>
          <Tab
            icon={<AssessmentIcon />}
            iconPosition="start"
            label="ภาพรวมการผลิต"
            sx={{ minHeight: 52, minWidth: 180 }}
          />
          <Tab
            icon={<EditNoteIcon />}
            iconPosition="start"
            label="คำขอแก้ไขของเสีย"
            sx={{ minHeight: 52, minWidth: 180 }}
          />
        </Tabs>
      </Box>

      <Box hidden={tab !== 0}>
        {tab === 0 && <DocMgmtDashboard />}
      </Box>
      <Box hidden={tab !== 1}>
        {tab === 1 && <DocWasteRequestDashboard />}
      </Box>
    </Box>
  );
};

export default DocHub;
