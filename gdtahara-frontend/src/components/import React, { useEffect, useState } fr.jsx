import React, { useEffect, useState } from 'react';
import axios from 'axios';
import ProductionControlDashboard from './components/pc/ProductionControlDashboard';

function App() {
    const [dashboardData, setDashboardData] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        axios.get('http://localhost:8080/api/pc/dashboard-summary')
            .then(response => {
                console.log('Data received:', response.data); // Debugging log
                setDashboardData(response.data.dashboardData || []); // Ensure dashboardData is an array
            })
            .catch(err => {
                console.error('Error fetching data:', err); // Debugging log
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
