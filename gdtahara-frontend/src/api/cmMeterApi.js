import axiosInstance from './axios';

// GET /api/cm/shift
export const getCurrentShift = async () => {
  const res = await axiosInstance.get('/cm/shift');
  return res.data;
};

// GET /api/cm/tanks/machine/{machineId}
export const getCmTanksByMachine = async (machineId) => {
  const res = await axiosInstance.get(`/cm/tanks/machine/${encodeURIComponent(machineId)}`);
  return res.data;
};

// GET /api/cm/tanks/{tankId}/status
export const getCmTankStatus = async (tankId) => {
  const res = await axiosInstance.get(`/cm/tanks/${encodeURIComponent(tankId)}/status`);
  return res.data;
};

// GET /api/cm/lots?materialCode=xxx
export const getAvailableLots = async (materialCode) => {
  const res = await axiosInstance.get('/cm/lots', { params: { materialCode } });
  return res.data;
};

// POST /api/cm/tanks/{tankId}/meter
// dto: { readingType, meterValueKg, note }
export const postMeterReading = async (tankId, dto) => {
  const res = await axiosInstance.post(`/cm/tanks/${encodeURIComponent(tankId)}/meter`, dto);
  return res.data;
};

// POST /api/cm/tanks/{tankId}/refill
// dto: { qtyKg, materialLotCode, note }
export const postRefill = async (tankId, dto) => {
  const res = await axiosInstance.post(`/cm/tanks/${encodeURIComponent(tankId)}/refill`, dto);
  return res.data;
};

// POST /api/cm/refills/{refillId}/correct
// dto: { qtyKg, materialLotCode, note }
export const postCorrection = async (refillId, dto) => {
  const res = await axiosInstance.post(`/cm/refills/${refillId}/correct`, dto);
  return res.data;
};

// GET /api/cm/tanks/{tankId}/report?shiftDate=YYYY-MM-DD&shiftCode=X
export const getShiftReport = async (tankId, shiftDate, shiftCode) => {
  const res = await axiosInstance.get(
    `/cm/tanks/${encodeURIComponent(tankId)}/report`,
    { params: { shiftDate, shiftCode } }
  );
  return res.data;
};

// GET /api/cm/machines — list of machine IDs
export const getCmMachines = async () => {
  const res = await axiosInstance.get('/cm/machines');
  return res.data;
};

// alias getCmTanksByMachine for backward compat
export const getCmTanks = async (machineId) => {
  return getCmTanksByMachine(machineId);
};

// GET /api/cm/machines/{machineId}/refill-history?limit=N
export const getRefillHistory = async (machineId, limit = 20) => {
  const res = await axiosInstance.get(
    `/cm/machines/${encodeURIComponent(machineId)}/refill-history`,
    { params: { limit } }
  );
  return res.data;
};
