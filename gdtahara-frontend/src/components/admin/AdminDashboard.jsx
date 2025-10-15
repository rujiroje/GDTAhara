// =================================================================
// File: src/components/admin/AdminDashboard.jsx (ไฟล์ใหม่-ฉบับเต็ม)
// =================================================================
import React, { useState, useEffect } from 'react';
import axios from 'axios';

// --- API Service (จำลองการตั้งค่า) ---
const API_URL = 'http://localhost:8080/api';
const api = axios.create({ baseURL: API_URL });
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) { config.headers.Authorization = `Bearer ${token}`; }
    return config;
}, error => Promise.reject(error));


// --- Shared Components ---
const Modal = ({ isOpen, onClose, title, children }) => {
    if (!isOpen) return null;
    return ( <div className="modal-overlay"> <div className="modal-content"> <div className="modal-header"> <h3>{title}</h3> <button onClick={onClose} className="modal-close-button">&times;</button> </div> <div className="modal-body">{children}</div> </div> </div> );
};


// --- Sub-Components for AdminDashboard ---

const DataForm = ({ item, type, onSave, onCancel }) => {
    const [formData, setFormData] = useState(item || {});
    // Dynamic roles for user management (fallback included)
    const fallbackRoles = ['DataAdmin', 'Production Control', 'Shift Leader', 'Operator', 'Technician', 'QA', 'CM Operator', 'Management', 'Document'];
    const [roles, setRoles] = useState(fallbackRoles);

    useEffect(() => { setFormData(item || {}); }, [item]);

    // Fetch roles from backend when managing users to avoid hard-coded drift
    useEffect(() => {
        if (type === 'users') {
            api.get('/admin/roles')
                .then(res => {
                    if (Array.isArray(res.data) && res.data.length) {
                        // Deduplicate and keep stable order by fallback baseline
                        const apiRoles = Array.from(new Set(res.data.map(r => String(r))));
                        const merged = [...new Set([...apiRoles, ...fallbackRoles])];
                        setRoles(merged);
                    }
                })
                .catch(() => {
                    // keep fallback roles silently
                });
        }
    }, [type]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        onSave(formData);
    };

    const renderFields = () => {
        switch (type) {
            case 'products':
                return <>
                    <div className="form-grid">
                        <div className="form-group"><label className="form-label">Product Code</label><input name="productCode" value={formData.productCode || ''} onChange={handleChange} className="form-input" required /></div>
                        <div className="form-group"><label className="form-label">Product Name</label><input name="productName" value={formData.productName || ''} onChange={handleChange} className="form-input" required /></div>
                        <div className="form-group"><label className="form-label">Qty/Box</label><input name="qtyPerBox" type="number" value={formData.qtyPerBox || ''} onChange={handleChange} className="form-input" /></div>
                    </div>
                </>;
            case 'machines':
                return <>
                    <div className="form-group"><label className="form-label">Machine Code</label><input name="machineCode" value={formData.machineCode || ''} onChange={handleChange} className="form-input" required /></div>
                    <div className="form-group"><label className="form-label">Machine Name</label><input name="machineName" value={formData.machineName || ''} onChange={handleChange} className="form-input" /></div>
                </>;
            case 'ng-types':
                 const ngCategories = ['Operator', 'Technician', 'QA', 'Shift Leader'];
                 return <>
                    <div className="form-group"><label className="form-label">NG Code</label><input name="ngCode" value={formData.ngCode || ''} onChange={handleChange} className="form-input" required /></div>
                    <div className="form-group"><label className="form-label">Description</label><input name="ngDescriptionTh" value={formData.ngDescriptionTh || ''} onChange={handleChange} className="form-input" /></div>
                    <div className="form-group">
                        <label className="form-label">Category</label>
                        <select name="ngType" value={formData.ngType || ''} onChange={handleChange} className="form-input" required>
                            <option value="" disabled>-- กรุณาเลือก Category --</option>
                            {ngCategories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                        </select>
                    </div>
                </>;
             case 'parameter-checklists':
                return <>
                    <div className="form-grid">
                        <div className="form-group"><label className="form-label">Machine Type (e.g., RBL)</label><input name="machineType" value={formData.machineType || ''} onChange={handleChange} className="form-input" required /></div>
                        <div className="form-group"><label className="form-label">Item Name</label><input name="itemName" value={formData.itemName || ''} onChange={handleChange} className="form-input" required /></div>
                        <div className="form-group"><label className="form-label">Standard Value</label><input name="standardValue" value={formData.standardValue || ''} onChange={handleChange} className="form-input" /></div>
                        <div className="form-group"><label className="form-label">Unit</label><input name="unit" value={formData.unit || ''} onChange={handleChange} className="form-input" /></div>
                    </div>
                </>;
            default: // users
                const isNewUser = !item?.id;
                return <>
                    <div className="form-group"><label className="form-label">Username</label><input name="username" value={formData.username || ''} onChange={handleChange} className="form-input" required /></div>
                    {isNewUser && (
                         <div className="form-group"><label className="form-label">Password</label><input name="password" type="password" value={formData.password || ''} onChange={handleChange} className="form-input" required /></div>
                    )}
                    <div className="form-group">
                        <label className="form-label">Role</label>
                        <select name="role" value={formData.role || ''} onChange={handleChange} className="form-input" required>
                            <option value="" disabled>-- กรุณาเลือก Role --</option>
                            {roles.map(role => <option key={role} value={role}>{role}</option>)}
                        </select>
                    </div>
                </>;
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            {renderFields()}
            <div className="form-actions">
                <button type="button" onClick={onCancel} className="cancel-button">ยกเลิก</button>
                <button type="submit" className="save-button">บันทึก</button>
            </div>
        </form>
    );
};

const ResetPasswordForm = ({ userId, onSave, onCancel }) => {
    const [newPassword, setNewPassword] = useState('');
    const handleSubmit = (e) => { e.preventDefault(); onSave(userId, newPassword); };
    return (
        <form onSubmit={handleSubmit}>
            <div className="form-group">
                <label className="form-label">New Password</label>
                <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className="form-input" required minLength="6" />
            </div>
            <div className="form-actions">
                <button type="button" onClick={onCancel} className="cancel-button">ยกเลิก</button>
                <button type="submit" className="save-button">บันทึกรหัสผ่านใหม่</button>
            </div>
        </form>
    );
};

const CrudTable = ({ title, columns, endpoint }) => {
    const [data, setData] = useState([]);
    const [error, setError] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingItem, setEditingItem] = useState(null);
    const [isResetPasswordModalOpen, setResetPasswordModalOpen] = useState(false);
    const [currentItemId, setCurrentItemId] = useState(null);

    const fetchData = async () => {
        try {
            setError('');
            const response = await api.get(endpoint);
            setData(response.data);
        } catch (err) { setError(`ไม่สามารถดึงข้อมูล ${title} ได้`); }
    };

    useEffect(() => { fetchData(); }, [endpoint]);

    const handleOpenModal = (item = null) => { setEditingItem(item); setIsModalOpen(true); };
    const handleCloseModal = () => { setIsModalOpen(false); setEditingItem(null); };
    const handleOpenResetPasswordModal = (id) => { setCurrentItemId(id); setResetPasswordModalOpen(true); };
    const handleCloseResetPasswordModal = () => { setResetPasswordModalOpen(false); setCurrentItemId(null); };

    const handleSave = async (formData) => {
        try {
            if (formData.id) { await api.put(`${endpoint}/${formData.id}`, formData); }
            else { await api.post(endpoint, formData); }
            fetchData();
            handleCloseModal();
        } catch (err) { alert('เกิดข้อผิดพลาดในการบันทึกข้อมูล'); }
    };

    const handleDelete = async (id) => {
        if (window.confirm('คุณแน่ใจหรือไม่ว่าต้องการลบข้อมูลนี้?')) {
            try { await api.delete(`${endpoint}/${id}`); fetchData(); }
            catch (err) { alert('เกิดข้อผิดพลาดในการลบข้อมูล'); }
        }
    };
    
    const handleResetPassword = async (userId, newPassword) => {
        try {
            await api.post(`${endpoint}/${userId}/reset-password`, { newPassword });
            handleCloseResetPasswordModal();
            alert('รีเซ็ตรหัสผ่านสำเร็จ!');
        } catch (err) { alert('เกิดข้อผิดพลาดในการรีเซ็ตรหัสผ่าน'); }
    };

    return (
        <div>
            <div className="table-header">
                <h3>{title}</h3>
                <button className="add-button" onClick={() => handleOpenModal()}>เพิ่มข้อมูลใหม่</button>
            </div> 
            {error && <p className="error-message">{error}</p>}
            <div className="data-table-container">
                <table className="data-table">
                    <thead><tr>{columns.map(col => <th key={col.key}>{col.label}</th>)}<th>Actions</th></tr></thead>
                    <tbody>
                        {data.map(item => (
                            <tr key={item.id}>
                                {columns.map(col => <td key={col.key}>{item[col.key]}</td>)}
                                <td className="actions-cell">
                                    <button className="edit-button" onClick={() => handleOpenModal(item)}>แก้ไข</button>
                                    {endpoint === '/admin/users' && (<button className="reset-pw-button" onClick={() => handleOpenResetPasswordModal(item.id)}>รีเซ็ตรหัสผ่าน</button>)}
                                    <button className="delete-button" onClick={() => handleDelete(item.id)}>ลบ</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={editingItem ? `แก้ไข ${title}` : `เพิ่ม ${title}`}>
                <DataForm item={editingItem} type={endpoint.split('/').pop()} onSave={handleSave} onCancel={handleCloseModal} />
            </Modal>
            <Modal isOpen={isResetPasswordModalOpen} onClose={handleCloseResetPasswordModal} title="รีเซ็ตรหัสผ่าน">
                <ResetPasswordForm userId={currentItemId} onSave={handleResetPassword} onCancel={handleCloseResetPasswordModal} />
            </Modal>
        </div>
    );
};
const ImportData = () => {
    const [file, setFile] = useState(null);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [currentUpload, setCurrentUpload] = useState('');

    const handleFileChange = (e, type) => { setFile({ file: e.target.files[0], type }); };

    const handleUpload = async () => {
        if (!file) { setError('กรุณาเลือกไฟล์ก่อน'); return; }
        setLoading(true);
        setCurrentUpload(file.type);
        setError('');
        setMessage('');
        const formData = new FormData();
        formData.append('file', file.file);
        try {
            const response = await api.post(`/import/${file.type}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
            // Handle both string and object responses
            const message = typeof response.data === 'string' ? response.data : 
                           response.data?.message || 'นำเข้าข้อมูลสำเร็จ';
            setMessage(message);
        } catch (err) {
            // Handle error object from Backend
            const errorData = err.response?.data;
            let errorMessage;
            if (typeof errorData === 'string') {
                errorMessage = errorData;
            } else if (errorData?.error) {
                errorMessage = errorData.error;
            } else if (errorData?.message) {
                errorMessage = errorData.message;
            } else {
                errorMessage = 'เกิดข้อผิดพลาดในการนำเข้าข้อมูล';
            }
            setError(errorMessage);
        }
        setLoading(false);
        setCurrentUpload('');
        setFile(null);
    };

    const Uploader = ({ type, label }) => (
        <div className="uploader-section">
            <h4>{label}</h4>
            <input type="file" onChange={(e) => handleFileChange(e, type)} accept=".csv" />
            <button onClick={handleUpload} disabled={loading || !file || file.type !== type} className="import-button">
                {loading && currentUpload === type ? 'กำลังนำเข้า...' : `นำเข้า ${label}`}
            </button>
        </div>
    );

    return (
        <div>
            <h3 className="dashboard-title" style={{fontSize: '1.25rem'}}>นำเข้าข้อมูลหลัก (Master Data)</h3>
            <p>เลือกไฟล์ CSV ที่ต้องการนำเข้า (ระบบจะข้ามข้อมูลที่มีรหัสซ้ำกับของเดิม)</p>
            {message && <div className="success-message">{message}</div>}
            {error && <div className="error-message">{error}</div>}
            <div className="import-container">
                <Uploader type="machines" label="ข้อมูลเครื่องจักร (Machine.csv)" />
                <Uploader type="ng-types" label="ข้อมูลประเภทของเสีย (NG.csv)" />
                <Uploader type="products" label="ข้อมูลผลิตภัณฑ์ (Product.csv)" />
                <Uploader type="materials" label="ข้อมูลวัตถุดิบ (Material.csv)" />
            </div>
        </div>
    );
};

// --- Main Admin Component ---
const AdminDashboard = () => {
    const [activeTab, setActiveTab] = useState('users');
    const userColumns = [{ key: 'id', label: 'ID' }, { key: 'username', label: 'Username' }, { key: 'role', label: 'Role' }];
    const productColumns = [{ key: 'id', label: 'ID' }, { key: 'productCode', label: 'Product Code' }, { key: 'productName', label: 'Product Name' }];
    const machineColumns = [{ key: 'id', label: 'ID' }, { key: 'machineCode', label: 'Machine Code' }, { key: 'machineName', label: 'Machine Name' }];
    const ngTypeColumns = [{ key: 'id', label: 'ID' }, { key: 'ngCode', label: 'NG Code' }, { key: 'ngDescriptionTh', label: 'Description' }, { key: 'ngType', label: 'Category' }];
    const materialColumns = [ { key: 'id', label: 'ID' }, { key: 'materialCode', label: 'Material Code' }, { key: 'materialName', label: 'Material Name' }, { key: 'materialType', label: 'Type' }, { key: 'unit', label: 'Unit' } ];


    return (
        <div className="dashboard-card">
            <h2 className="dashboard-title">จัดการข้อมูลหลัก (Master Data)</h2>
            <div className="tabs">
                <button onClick={() => setActiveTab('users')} className={activeTab === 'users' ? 'active' : ''}>ผู้ใช้งาน</button>
                <button onClick={() => setActiveTab('products')} className={activeTab === 'products' ? 'active' : ''}>ผลิตภัณฑ์</button>
                <button onClick={() => setActiveTab('machines')} className={activeTab === 'machines' ? 'active' : ''}>เครื่องจักร</button>
                <button onClick={() => setActiveTab('materials')} className={activeTab === 'materials' ? 'active' : ''}>วัตถุดิบ</button>
                <button onClick={() => setActiveTab('ngTypes')} className={activeTab === 'ngTypes' ? 'active' : ''}>ประเภทของเสีย</button>
                <button onClick={() => setActiveTab('import')} className={activeTab === 'import' ? 'active' : ''}>นำเข้าข้อมูล</button>
            </div>
            <div className="tab-content">
                {activeTab === 'users' && <CrudTable title="ผู้ใช้งานระบบ" columns={userColumns} endpoint="/admin/users" />}
                {activeTab === 'products' && <CrudTable title="ผลิตภัณฑ์" columns={productColumns} endpoint="/admin/products" />}
                {activeTab === 'machines' && <CrudTable title="เครื่องจักร" columns={machineColumns} endpoint="/admin/machines" />}
                {activeTab === 'materials' && <CrudTable title="วัตถุดิบ" columns={materialColumns} endpoint="/admin/materials" />}
                {activeTab === 'ngTypes' && <CrudTable title="ประเภทของเสีย" columns={ngTypeColumns} endpoint="/admin/ng-types" />}
                {activeTab === 'import' && <ImportData />}
            </div>
        </div>
    );
};

export default AdminDashboard;