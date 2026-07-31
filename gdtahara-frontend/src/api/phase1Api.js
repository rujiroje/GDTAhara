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

/** Look up a single sub-lot by its printed sub-lot number (barcode). */
export const getSubLotByNumber = async (subLotNumber) => {
  const response = await axiosInstance.get(`/sub-lots/by-number/${encodeURIComponent(subLotNumber)}`);
  return response.data;
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

// ── Machine Setup Job ─────────────────────────────────────────────

/** Pending setup jobs assigned (or unassigned) to a specific technician user. */
export const getPendingSetupJobs = async (userId) => {
  const response = await axiosInstance.get(`/setup-jobs/technician/${userId}/pending`);
  return response.data;
};

/** Trigger a scan for setup jobs in a date range (PC / Admin only). */
export const scanSetupJobs = async (from, to) => {
  const response = await axiosInstance.post('/setup-jobs/scan', null, { params: { from, to } });
  return response.data;
};

/** Assign a setup job to a technician. */
export const assignSetupJob = async (id, technicianUserId) => {
  const response = await axiosInstance.put(`/setup-jobs/${id}/assign`, { technicianUserId });
  return response.data;
};

/** Mark a setup job as started (records startedAt + duration clock). */
export const startSetup = async (id) => {
  const response = await axiosInstance.put(`/setup-jobs/${id}/start`);
  return response.data;
};

/**
 * Mark a setup job as completed with checklist data.
 * payload: { moldChanged, moldCodeFrom, moldCodeTo, tempAdjusted, cycleAdjusted, blowPinAligned, fpiPassed, notes }
 * Backend auto-creates a DowntimeEvent for OEE calculation.
 */
export const completeSetup = async (id, payload) => {
  const response = await axiosInstance.put(`/setup-jobs/${id}/complete`, payload);
  return response.data;
};

/** Skip a setup job with a reason (creates a SKIPPED record instead). */
export const skipSetup = async (id, skipReason) => {
  const response = await axiosInstance.put(`/setup-jobs/${id}/skip`, { skipReason });
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

// ── Setup Checklist ────────────────────────────────────────────────

export const getSetupTemplates = async (machineType) => {
  const params = machineType ? { machineType } : {};
  const response = await axiosInstance.get('/setup-checklist/templates', { params });
  return response.data;
};

export const getJobSteps = async (jobId) => {
  const response = await axiosInstance.get(`/setup-checklist/jobs/${jobId}/steps`);
  return response.data;
};

export const saveStepResults = async (jobId, steps) => {
  const response = await axiosInstance.post(`/setup-checklist/jobs/${jobId}/steps`, steps);
  return response.data;
};

export const uploadStepPhoto = async (jobId, templateId, file) => {
  const form = new FormData();
  form.append('file', file);
  const response = await axiosInstance.post(
    `/setup-checklist/jobs/${jobId}/steps/${templateId}/photo`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return response.data; // { filename }
};

export const getStepPhotoUrl = (filename) =>
  `${axiosInstance.defaults.baseURL}/setup-checklist/photos/${encodeURIComponent(filename)}`;

// ── Daily Blow Report ──────────────────────────────────────────────
export const fetchBlowDailyReport = async (reportId, date) => {
  const response = await axiosInstance.get(`/reports/blow-daily/${reportId}`, { params: { date } });
  return response.data;
};
