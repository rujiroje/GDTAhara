import axiosInstance from './axios';

// GET /api/cm/waste/waste-types
export const getWasteTypes = async () => {
  const res = await axiosInstance.get('/cm/waste/waste-types');
  return res.data;
};

// POST /api/cm/waste — { machineId, woNumber?, wasteTypeCode, weightKg, note?, photoIds? }
export const createWasteRecord = async (dto) => {
  const res = await axiosInstance.post('/cm/waste', dto);
  return res.data;
};

// GET /api/cm/waste/current-shift — all CM records this shift
export const getCurrentShiftWaste = async () => {
  const res = await axiosInstance.get('/cm/waste/current-shift');
  return res.data;
};

// GET /api/cm/waste/my-records — my records this shift
export const getMyWasteRecords = async () => {
  const res = await axiosInstance.get('/cm/waste/my-records');
  return res.data;
};

// PATCH /api/cm/waste/{id}/self-edit — { wasteTypeCode?, weightKg?, note? }
export const selfEditWaste = async (id, dto) => {
  const res = await axiosInstance.patch(`/cm/waste/${id}/self-edit`, dto);
  return res.data;
};

// POST /api/cm/waste/{id}/edit-request — { requestedWasteTypeCode?, requestedWeightKg?, requestedNote?, reason }
export const createWasteEditRequest = async (id, dto) => {
  const res = await axiosInstance.post(`/cm/waste/${id}/edit-request`, dto);
  return res.data;
};

// GET /api/cm/waste/my-requests
export const getMyWasteEditRequests = async () => {
  const res = await axiosInstance.get('/cm/waste/my-requests');
  return res.data;
};

// GET /api/cm/waste/{id}/history
export const getWasteRecordHistory = async (id) => {
  const res = await axiosInstance.get(`/cm/waste/${id}/history`);
  return res.data;
};

// GET /api/doc/edit-requests (Doc role)
export const getDocPendingRequests = async () => {
  const res = await axiosInstance.get('/doc/edit-requests');
  return res.data;
};

// PATCH /api/doc/edit-requests/{requestId}/respond — { approved, note } (Doc role)
export const respondToEditRequest = async (requestId, dto) => {
  const res = await axiosInstance.patch(`/doc/edit-requests/${requestId}/respond`, dto);
  return res.data;
};
