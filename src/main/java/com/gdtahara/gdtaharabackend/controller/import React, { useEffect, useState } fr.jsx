import React, { useEffect, useState } from 'react';
import axios from 'axios';
import ProductionControlDashboard from './components/pc/ProductionControlDashboard';

function App() {
    const [dashboardData, setDashboardData] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        axios.get('http://localhost:8080/api/pc/dashboard-summary')
            .then(response => {
                console.log('Data received:', response.data); // เพิ่ม log เพื่อ debug
                setDashboardData(response.data.dashboardData || []); // ตรวจสอบว่า dashboardData เป็น array
            })
            .catch(err => {
                console.error('Error fetching data:', err); // เพิ่ม log เพื่อ debug
                setError(err.message);
            });
    }, []);

    if (error) {
        return <div>Error: {error}</div>;
    }

    if (!dashboardData) {
        return <div>Loading...</div>;
    }

    return <ProductionControlDashboard dashboardData={dashboardData} />;
}

export default App;
