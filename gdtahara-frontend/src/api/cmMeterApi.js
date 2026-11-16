import axiosInstance from './axios';

export const getCmMachines = async () => {
  const res = await axiosInstance.get('/cm/meter/machines');
  return res.data;
};

export const getCmTanks = async (machineId) => {
  const res = await axiosInstance.get('/cm/meter/tanks', { params: { machineId } });
  return res.data;
};

export const getCmMaterials = async (machineId) => {
  const res = await axiosInstance.get('/cm/meter/materials', { params: { machineId } });
  return res.data;
};

export const getRefillHistory = async (machineId, limit = 50) => {
  const res = await axiosInstance.get('/cm/meter/refill-history', { params: { machineId, limit } });
  return res.data;
};

// payload: { machineId, tankId, materialId, lotNumber, quantity, unit, shiftCode, operatorNote }
export const postRefill = async (payload) => {
  const res = await axiosInstance.post('/cm/meter/refill', payload);
  return res.data;
};

// payload: { machineId, shiftCode, readings: [{ tankId, meterValue, unit }] }
export const postEndOfShift = async (payload) => {
  const res = await axiosInstance.post('/cm/meter/end-of-shift', payload);
  return res.data;
};

// payload: { machineId, tankId, quantity (negative), reason, operatorNote }
export const postCorrection = async (payload) => {
  const res = await axiosInstance.post('/cm/meter/correction', payload);
  return res.data;
};
