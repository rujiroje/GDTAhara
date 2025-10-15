// Import เพิ่มเติม (ถ้าจำเป็น)
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { format } from 'date-fns';

// Component
function HistoricalReportsSection() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [results, setResults] = useState([]);
  const [filters, setFilters] = useState({
    startDate: format(new Date(), 'yyyy-MM-dd'),
    endDate: format(new Date(), 'yyyy-MM-dd'),
    machineId: '',
    productId: ''
  });
  const [debugInfo, setDebugInfo] = useState({
    apiCalled: false,
    requestData: null,
    responseData: null,
    error: null
  });

  // ฟังก์ชันสำหรับดึงข้อมูลรายงาน
  const fetchHistoricalReports = async () => {
    setLoading(true);
    setError('');
    
    setDebugInfo(prev => ({
      ...prev,
      apiCalled: true,
      requestData: filters
    }));
    
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('กรุณาเข้าสู่ระบบก่อนใช้งาน');
        setLoading(false);
        return;
      }

      // สร้าง URL parameters
      const params = new URLSearchParams();
      
      if (!filters.startDate || !filters.endDate) {
        setError('กรุณาระบุวันที่เริ่มต้นและวันที่สิ้นสุด');
        setLoading(false);
        return;
      }
      
      params.append('startDate', filters.startDate);
      params.append('endDate', filters.endDate);
      
      if (filters.machineId && filters.machineId !== 'all') {
        params.append('machineId', filters.machineId);
      }
      
      if (filters.productId && filters.productId !== 'all') {
        params.append('productId', filters.productId);
      }
      
      console.log('Request params:', params.toString());
      
      const config = {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      };
      
      const url = `http://localhost:8080/api/production/reports/production-historical?${params}`;  // Fixed: full URL to avoid baseURL conflict
      console.log('Calling API:', url);
      
      const response = await axios.get(url, config);
      console.log('API Response:', response);
      
      setDebugInfo(prev => ({
        ...prev,
        responseData: response.data,
        error: null
      }));
      
      if (Array.isArray(response.data)) {
        setResults(response.data);
        if (response.data.length === 0) {
          setError('ไม่พบข้อมูลตามเงื่อนไขที่ระบุ');
        }
      } else {
        console.error('Invalid response format:', response.data);
        setError('ข้อมูลที่ได้รับมีรูปแบบไม่ถูกต้อง');
        setResults([]);
      }
    } catch (err) {
      console.error('Error fetching historical reports:', err);
      
      setDebugInfo(prev => ({
        ...prev,
        error: {
          message: err.message,
          response: err.response?.data,
          status: err.response?.status
        }
      }));
      
      if (err.response) {
        setError(`เกิดข้อผิดพลาด: ${err.response.status} ${err.response.statusText || ''}`);
      } else if (err.request) {
        setError('ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้');
      } else {
        setError(`เกิดข้อผิดพลาด: ${err.message}`);
      }
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  // แสดงผลลัพธ์ในรูปแบบตาราง
  const renderResults = () => {
    if (loading) {
      return <div className="loading">กำลังโหลดข้อมูล...</div>;
    }
    
    if (error) {
      return <div className="error">{error}</div>;
    }
    
    if (!results || results.length === 0) {
      return <div className="no-data">ไม่มีข้อมูลแสดงผล กรุณาเปลี่ยนเงื่อนไขการค้นหา</div>;
    }
    
    return (
      <div className="results-table">
        <table>
          <thead>
            <tr>
              <th>Order #</th>
              <th>วันที่เริ่ม</th>
              <th>วันที่สิ้นสุด</th>
              <th>เครื่องจักร</th>
              <th>ผลิตภัณฑ์</th>
              <th>Good Qty</th>
              <th>NG Qty</th>
              <th>Yield</th>
              <th>จำนวนกล่อง</th>
              <th>น้ำหนักเศษ (kg)</th>
            </tr>
          </thead>
          <tbody>
            {results.map(item => (
              <tr key={item.id}>
                <td>{item.orderNumber}</td>
                <td>{item.startDate}</td>
                <td>{item.endDate}</td>
                <td>{item.machineName}</td>
                <td>{item.productName}</td>
                <td>{item.goodQty}</td>
                <td>{item.ngQty}</td>
                <td>{item.yield}</td>
                <td>{item.totalBoxes}</td>
                <td>{item.totalScrapWeight}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  // เพิ่มส่วนแสดง Debug Info ในโหมด development
  const renderDebugInfo = () => {
    if (process.env.NODE_ENV !== 'development') return null;
    
    return (
      <div className="debug-info" style={{margin: '20px', padding: '10px', border: '1px solid #ccc', backgroundColor: '#f8f8f8'}}>
        <h3>Debug Info</h3>
        <div>
          <strong>API Called:</strong> {debugInfo.apiCalled ? 'Yes' : 'No'}
        </div>
        {debugInfo.requestData && (
          <div>
            <strong>Request Data:</strong>
            <pre>{JSON.stringify(debugInfo.requestData, null, 2)}</pre>
          </div>
        )}
        {debugInfo.responseData && (
          <div>
            <strong>Response Data:</strong>
            <pre>{JSON.stringify(debugInfo.responseData, null, 2)}</pre>
          </div>
        )}
        {debugInfo.error && (
          <div>
            <strong>Error:</strong>
            <pre>{JSON.stringify(debugInfo.error, null, 2)}</pre>
          </div>
        )}
      </div>
    );
  };
  
  // ส่วน render หลักของ component
  return (
    <div className="historical-reports-section">
      <h2>รายงานการผลิตย้อนหลัง</h2>
      
      <div className="filters">
        {/* ... ส่วนแสดงฟิลเตอร์ ... */}
        <div className="form-group">
          <label>วันที่เริ่มต้น</label>
          <input 
            type="date" 
            value={filters.startDate}
            onChange={e => setFilters({...filters, startDate: e.target.value})}
          />
        </div>
        
        <div className="form-group">
          <label>วันที่สิ้นสุด</label>
          <input 
            type="date" 
            value={filters.endDate}
            onChange={e => setFilters({...filters, endDate: e.target.value})}
          />
        </div>
        
        {/* ... ส่วน filter อื่นๆ ... */}
        
        <button onClick={fetchHistoricalReports} disabled={loading}>
          {loading ? 'กำลังค้นหา...' : 'ค้นหา'}
        </button>
      </div>
      
      {renderResults()}
      {renderDebugInfo()}
    </div>
  );
}

export default HistoricalReportsSection;
