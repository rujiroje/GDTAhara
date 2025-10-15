import React from 'react';

function ProductionControlDashboard({ dashboardData }) {
    if (!Array.isArray(dashboardData)) {
        console.error('dashboardData is not an array:', dashboardData); // Debugging log
        return <div>Error: Invalid data format</div>;
    }

    return (
        <div>
            <h1>Production Control Dashboard</h1>
            <ul>
                {dashboardData.map(data => (
                    <li key={data.reportId}>
                        {data.machineName} - {data.productName} (Good: {data.currentGoodQty}, NG: {data.currentNgQty})
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default ProductionControlDashboard;
