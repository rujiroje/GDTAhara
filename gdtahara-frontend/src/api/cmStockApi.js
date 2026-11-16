import axiosInstance from './axios';

export const getStockOverview = async () => {
  const res = await axiosInstance.get('/cm/stock/overview');
  return res.data;
};

export const getMaterialDetail = async (materialCode) => {
  const res = await axiosInstance.get(`/cm/stock/${encodeURIComponent(materialCode)}`);
  return res.data;
};

export const getStockCard = async (materialCode, params = {}) => {
  const res = await axiosInstance.get(`/cm/stock/${encodeURIComponent(materialCode)}/stock-card`, { params });
  return res.data;
};

export const getMultiMaterialSummary = async (days = 7) => {
  const res = await axiosInstance.get('/cm/stock/multi-material-summary', { params: { days } });
  return res.data;
};

export const getLowStockAlerts = async () => {
  const res = await axiosInstance.get('/cm/stock/low-stock');
  return res.data;
};
