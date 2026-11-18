import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Tabs, Tab, Table, TableHead, TableBody, TableRow, TableCell,
  TableContainer, Paper, Chip, IconButton, CircularProgress, Alert,
  Card, CardContent, Grid, Tooltip, Button
} from '@mui/material';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingFlatRounded';
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat';
import InventoryIcon from '@mui/icons-material/Inventory';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';

import {
  getStockOverview, getMultiMaterialSummary, getLowStockAlerts, getMaterialDetail
} from '../../api/cmStockApi';
import StockCardTable from './StockCardTable';
import ReorderConfigPanel from './ReorderConfigPanel';
import { useAuth } from '../../App';
import SettingsIcon from '@mui/icons-material/Settings';

const fmtKg = (v) =>
  v == null ? '-' : Number(v).toLocaleString('th-TH', { minimumFractionDigits: 2 });

const fmtDt = (v) => {
  if (!v) return '-';
  return new Date(v).toLocaleString('th-TH', { dateStyle: 'short', timeStyle: 'short' });
};

const statusChip = (status) => {
  const map = {
    NORMAL:   { label: 'ปกติ',    color: 'success' },
    LOW:      { label: 'ใกล้หมด', color: 'warning' },
    CRITICAL: { label: 'วิกฤต',  color: 'error' },
    UNKNOWN:  { label: '-',       color: 'default' },
  };
  const cfg = map[status] || map.UNKNOWN;
  return <Chip label={cfg.label} color={cfg.color} size="small" />;
};

const TrendIcon = ({ trend }) => {
  if (trend === 'UP')   return <TrendingUpIcon fontSize="small" color="success" />;
  if (trend === 'DOWN') return <TrendingDownIcon fontSize="small" color="error" />;
  return <TrendingFlatIcon fontSize="small" color="disabled" />;
};

// ── Overview Tab ──────────────────────────────────────────────────────────────
const OverviewTab = ({ overview, onSelectMaterial, onShowStockCard }) => {
  if (!overview) return null;
  const { materials = [], totalBalanceKg, totalActiveLots, alertCount } = overview;

  return (
    <Box>
      {/* Summary cards */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={6} sm={4}>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">ยอดรวมทั้งหมด</Typography>
              <Typography variant="h6">{fmtKg(totalBalanceKg)} KG</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={6} sm={4}>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">Lots ที่ active</Typography>
              <Typography variant="h6">{totalActiveLots ?? '-'}</Typography>
            </CardContent>
          </Card>
        </Grid>
        {alertCount > 0 && (
          <Grid item xs={6} sm={4}>
            <Card variant="outlined" sx={{ borderColor: 'warning.main' }}>
              <CardContent sx={{ py: 1.5 }}>
                <Typography variant="caption" color="warning.main">แจ้งเตือนสต็อกต่ำ</Typography>
                <Typography variant="h6" color="warning.main">{alertCount} รายการ</Typography>
              </CardContent>
            </Card>
          </Grid>
        )}
      </Grid>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>รหัสวัตถุดิบ</TableCell>
              <TableCell>ชื่อ</TableCell>
              <TableCell align="right">คงเหลือ (KG)</TableCell>
              <TableCell align="right">จุดสั่งซื้อ</TableCell>
              <TableCell align="right">Lots</TableCell>
              <TableCell align="right">เคลื่อนไหวล่าสุด</TableCell>
              <TableCell align="center">สถานะ</TableCell>
              <TableCell align="center">Stock Card</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {materials.map((m) => (
              <TableRow
                key={m.materialCode}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => onSelectMaterial(m)}
              >
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    {m.hasActiveAlert && (
                      <Tooltip title="มีการแจ้งเตือนสต็อกต่ำ">
                        <WarningAmberIcon fontSize="small" color="warning" />
                      </Tooltip>
                    )}
                    <Typography variant="body2" fontWeight="medium">{m.materialCode}</Typography>
                  </Box>
                </TableCell>
                <TableCell>
                  <Typography variant="body2">{m.materialName || '-'}</Typography>
                </TableCell>
                <TableCell align="right">{fmtKg(m.balanceKg)}</TableCell>
                <TableCell align="right">{m.reorderPointKg ? fmtKg(m.reorderPointKg) : '-'}</TableCell>
                <TableCell align="right">{m.lotCount ?? 0}</TableCell>
                <TableCell align="right">
                  <Typography variant="caption">{fmtDt(m.lastMovementAt)}</Typography>
                </TableCell>
                <TableCell align="center">{statusChip(m.stockStatus)}</TableCell>
                <TableCell align="center" onClick={e => { e.stopPropagation(); onShowStockCard(m); }}>
                  <Button size="small" variant="outlined">ดู</Button>
                </TableCell>
              </TableRow>
            ))}
            {materials.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} align="center">ไม่มีข้อมูล</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};

// ── Multi-Material Tab ────────────────────────────────────────────────────────
const MultiMaterialTab = ({ summary }) => (
  <TableContainer component={Paper}>
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>วัตถุดิบ</TableCell>
          <TableCell align="right">คงเหลือ (KG)</TableCell>
          <TableCell align="right">ใช้ 7 วัน (KG)</TableCell>
          <TableCell align="right">รับ 7 วัน (KG)</TableCell>
          <TableCell align="right">เปลี่ยนแปลง</TableCell>
          <TableCell align="center">แนวโน้ม</TableCell>
          <TableCell align="center">สถานะ</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {(summary || []).map((m) => (
          <TableRow key={m.materialCode} hover>
            <TableCell>
              <Typography variant="body2" fontWeight="medium">{m.materialCode}</Typography>
              {m.materialName && (
                <Typography variant="caption" color="text.secondary">{m.materialName}</Typography>
              )}
            </TableCell>
            <TableCell align="right">{fmtKg(m.balanceKg)}</TableCell>
            <TableCell align="right">{fmtKg(m.usedKg)}</TableCell>
            <TableCell align="right">{fmtKg(m.receivedKg)}</TableCell>
            <TableCell align="right">
              <Typography
                variant="body2"
                color={m.changeKg >= 0 ? 'success.main' : 'error.main'}
              >
                {m.changeKg >= 0 ? '+' : ''}{fmtKg(m.changeKg)}
              </Typography>
            </TableCell>
            <TableCell align="center"><TrendIcon trend={m.trend} /></TableCell>
            <TableCell align="center">{statusChip(m.stockStatus)}</TableCell>
          </TableRow>
        ))}
        {(!summary || summary.length === 0) && (
          <TableRow>
            <TableCell colSpan={7} align="center">ไม่มีข้อมูล</TableCell>
          </TableRow>
        )}
      </TableBody>
    </Table>
  </TableContainer>
);

// ── Alerts Tab ────────────────────────────────────────────────────────────────
const AlertsTab = ({ alerts }) => (
  <Box>
    {(!alerts || alerts.length === 0) && (
      <Alert severity="success">ไม่มีการแจ้งเตือนสต็อกต่ำในขณะนี้</Alert>
    )}
    {(alerts || []).map((a) => (
      <Card key={a.id} variant="outlined" sx={{ mb: 1, borderColor: 'warning.main' }}>
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <Box>
              <Typography variant="subtitle2" color="warning.dark">
                ⚠️ {a.materialCode}
              </Typography>
              <Typography variant="body2">
                ยอดคงเหลือ: <strong>{fmtKg(a.balanceAtAlert)} KG</strong>
                {' '}/ จุดสั่งซื้อ: {fmtKg(a.reorderPoint)} KG
              </Typography>
              {a.estDaysRemaining != null && (
                <Typography variant="body2" color="error">
                  คาดว่าจะหมดใน {a.estDaysRemaining} วัน
                </Typography>
              )}
            </Box>
            <Typography variant="caption" color="text.secondary">
              {a.alertDate}
            </Typography>
          </Box>
        </CardContent>
      </Card>
    ))}
  </Box>
);

// ── Material Detail View ──────────────────────────────────────────────────────
const MaterialDetailView = ({ materialCode, materialName, onBack, onShowStockCard }) => {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    getMaterialDetail(materialCode)
      .then(setDetail)
      .catch(() => setError('โหลดข้อมูลไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, [materialCode]);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={onBack} size="small">กลับ</Button>
        <Typography variant="h6">{materialName || materialCode}</Typography>
        {detail && statusChip(detail.stockStatus)}
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {loading && <CircularProgress size={24} />}

      {detail && !loading && (
        <>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid item xs={6} sm={3}>
              <Card variant="outlined">
                <CardContent sx={{ py: 1 }}>
                  <Typography variant="caption" color="text.secondary">คงเหลือ</Typography>
                  <Typography variant="h6">{fmtKg(detail.balanceKg)} KG</Typography>
                </CardContent>
              </Card>
            </Grid>
            {detail.reorderPointKg && (
              <Grid item xs={6} sm={3}>
                <Card variant="outlined">
                  <CardContent sx={{ py: 1 }}>
                    <Typography variant="caption" color="text.secondary">จุดสั่งซื้อ</Typography>
                    <Typography variant="h6">{fmtKg(detail.reorderPointKg)} KG</Typography>
                  </CardContent>
                </Card>
              </Grid>
            )}
            {detail.dailyAvgUsageKg != null && (
              <Grid item xs={6} sm={3}>
                <Card variant="outlined">
                  <CardContent sx={{ py: 1 }}>
                    <Typography variant="caption" color="text.secondary">ใช้เฉลี่ย/วัน</Typography>
                    <Typography variant="h6">{fmtKg(detail.dailyAvgUsageKg)} KG</Typography>
                  </CardContent>
                </Card>
              </Grid>
            )}
            {detail.estDaysRemaining != null && (
              <Grid item xs={6} sm={3}>
                <Card variant="outlined">
                  <CardContent sx={{ py: 1 }}>
                    <Typography variant="caption" color="text.secondary">คาดหมดใน</Typography>
                    <Typography variant="h6" color={detail.estDaysRemaining < 3 ? 'error.main' : 'inherit'}>
                      {detail.estDaysRemaining} วัน
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            )}
          </Grid>

          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography variant="subtitle1">Lots ที่ active ({detail.lots?.length ?? 0})</Typography>
            <Button variant="outlined" size="small" onClick={() => onShowStockCard({ materialCode, materialName })}>
              ดู Stock Card
            </Button>
          </Box>

          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Lot</TableCell>
                  <TableCell align="right">คงเหลือ (KG)</TableCell>
                  <TableCell>รับเข้าเมื่อ</TableCell>
                  <TableCell>เคลื่อนไหวล่าสุด</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(detail.lots || []).map((lot) => (
                  <TableRow key={lot.lotRef} hover>
                    <TableCell>{lot.lotRef || '-'}</TableCell>
                    <TableCell align="right">{fmtKg(lot.balanceKg)}</TableCell>
                    <TableCell><Typography variant="caption">{fmtDt(lot.receivedAt)}</Typography></TableCell>
                    <TableCell><Typography variant="caption">{fmtDt(lot.lastActivityAt)}</Typography></TableCell>
                  </TableRow>
                ))}
                {(!detail.lots || detail.lots.length === 0) && (
                  <TableRow>
                    <TableCell colSpan={4} align="center">ไม่มี lot ที่ active</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Box>
  );
};

// ── Main Dashboard ────────────────────────────────────────────────────────────
const CmStockDashboard = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'DataAdmin' || user?.role === 'Admin' || user?.role === 'ADMIN';
  const [tab, setTab] = useState(0);
  const [overview, setOverview] = useState(null);
  const [summary, setSummary] = useState(null);
  const [alerts, setAlerts] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [view, setView] = useState(null); // null | { type: 'detail'|'stockcard', mc, name }

  const loadAll = useCallback(() => {
    setLoading(true);
    setError('');
    Promise.all([
      getStockOverview(),
      getMultiMaterialSummary(7),
      getLowStockAlerts(),
    ])
      .then(([ov, sm, al]) => {
        setOverview(ov);
        setSummary(sm);
        setAlerts(al);
      })
      .catch(() => setError('โหลดข้อมูลไม่สำเร็จ'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadAll(); }, [loadAll]);

  if (view?.type === 'stockcard') {
    return (
      <Box sx={{ p: 2 }}>
        <StockCardTable
          materialCode={view.mc}
          materialName={view.name}
          onBack={() => setView(null)}
        />
      </Box>
    );
  }

  if (view?.type === 'detail') {
    return (
      <Box sx={{ p: 2 }}>
        <MaterialDetailView
          materialCode={view.mc}
          materialName={view.name}
          onBack={() => setView(null)}
          onShowStockCard={(m) => setView({ type: 'stockcard', mc: m.materialCode, name: m.materialName })}
        />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2, pt: 1 }}>
        <Typography variant="h6">สต็อกวัตถุดิบ CM</Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {alerts && alerts.length > 0 && (
            <Chip
              icon={<NotificationsActiveIcon />}
              label={`แจ้งเตือน ${alerts.length}`}
              color="warning"
              size="small"
              onClick={() => setTab(2)}
            />
          )}
          <IconButton onClick={loadAll} size="small" disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Box>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper', px: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab icon={<InventoryIcon />} iconPosition="start" label="ภาพรวม" sx={{ minHeight: 48 }} />
          <Tab label="เปรียบเทียบ 7 วัน" sx={{ minHeight: 48 }} />
          <Tab
            icon={alerts?.length > 0 ? <NotificationsActiveIcon color="warning" /> : undefined}
            iconPosition="start"
            label={`แจ้งเตือน${alerts?.length > 0 ? ` (${alerts.length})` : ''}`}
            sx={{ minHeight: 48 }}
          />
          {isAdmin && (
            <Tab icon={<SettingsIcon />} iconPosition="start" label="ตั้งค่า Reorder" sx={{ minHeight: 48 }} />
          )}
        </Tabs>
      </Box>

      <Box sx={{ p: 2 }}>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {loading && tab < 3 && <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>}

        {(!loading || tab === 3) && (
          <>
            {tab === 0 && (
              <OverviewTab
                overview={overview}
                onSelectMaterial={(m) => setView({ type: 'detail', mc: m.materialCode, name: m.materialName })}
                onShowStockCard={(m) => setView({ type: 'stockcard', mc: m.materialCode, name: m.materialName })}
              />
            )}
            {tab === 1 && <MultiMaterialTab summary={summary} />}
            {tab === 2 && <AlertsTab alerts={alerts} />}
            {tab === 3 && isAdmin && <ReorderConfigPanel />}
          </>
        )}
      </Box>
    </Box>
  );
};

export default CmStockDashboard;
