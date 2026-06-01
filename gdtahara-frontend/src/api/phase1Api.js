import axiosInstance from './axios';

// ── Production Plan ────────────────────────────────────────────────

export const getPlansForDate = async (date) => {
  const response = await axiosInstance.get(`/production-plans/date/${date}`);
  return response.data;
};

export const getPlansForMachine = async (machineId, from, to) => {
  const response = await axiosInstance.get(`/production-plans/machine/${machineId}`, {
    params: { from, to },
  });
  return response.data;
};

export const getPlansForMonth = async (year, month) => {
  const response = await axiosInstance.get(`/production-plans/month/${year}/${month}`);
  return response.data;
};

export const getShiftSplit = async (planId) => {
  const response = await axiosInstance.get(`/production-plans/${planId}/shift-split`);
  return response.data;
};

export const createPlan = async (payload) => {
  const response = await axiosInstance.post('/production-plans/', payload);
  return response.data;
};

export const updatePlan = async (id, payload) => {
  const response = await axiosInstance.put(`/production-plans/${id}`, payload);
  return response.data;
};

// ── Sub-Lot ────────────────────────────────────────────────────────

export const getSubLotsForReport = async (reportId) => {
  const response = await axiosInstance.get(`/sub-lots/report/${reportId}`);
  return response.data;
};

export const countBoxes = async (reportId) => {
  const response = await axiosInstance.get(`/sub-lots/report/${reportId}/count`);
  return response.data;
};

export const confirmBox = async (payload) => {
  const response = await axiosInstance.post('/sub-lots/confirm-box', payload);
  return response.data;
};

export const markLabeled = async (id, zplRef) => {
  const response = await axiosInstance.put(`/sub-lots/${id}/labeled`, { zplRef });
  return response.data;
};

/**
 * Fetches the barcode image as a Blob and returns a temporary object URL.
 * Call URL.revokeObjectURL(url) when the component unmounts to free memory.
 */
export const getBarcodeBlob = async (id) => {
  const response = await axiosInstance.get(`/sub-lots/${id}/barcode`, { responseType: 'blob' });
  return URL.createObjectURL(response.data);
};

// ── Excel Plan Import ─────────────────────────────────────────────

/**
 * Upload an Excel plan file. Returns ImportResult from the backend.
 * Sends multipart/form-data; factoryCode is optional.
 */
export const importPlan = async (file, factoryCode) => {
  const form = new FormData();
  form.append('file', file);
  if (factoryCode) form.append('factoryCode', factoryCode);
  const response = await axiosInstance.post('/production-plans/import', form);
  return response.data;
};

/** Latest import log entries, optionally filtered by factoryCode. */
export const getImportLogs = async (factoryCode) => {
  const response = await axiosInstance.get('/import-logs',
    { params: factoryCode ? { factoryCode } : undefined });
  return response.data;
};

// ── Label Print ────────────────────────────────────────────────────

export const getZpl = async (id) => {
  const response = await axiosInstance.get(`/labels/sub-lots/${id}/zpl`, { responseType: 'text' });
  return response.data;
};

export const printLabel = async (id, printerTarget) => {
  const response = await axiosInstance.post(`/labels/sub-lots/${id}/print`, { printerTarget });
  return response.data;
};
