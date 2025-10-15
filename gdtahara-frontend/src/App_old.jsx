import React, { useEffect, useState } from 'react';
import axios from 'axios';

function App() {
    const [data, setData] = useState(null);

    useEffect(() => {
        axios.get('http://localhost:8080/api/pc/dashboard-summary')
            .then(response => {
                console.log('Data received:', response.data); // Debugging log
                setData(response.data);
            })
            .catch(error => {
                console.error('Error fetching data:', error); // Debugging log
            });
    }, []);

    if (!data) {
        return <div>Loading...</div>;
    }

    return (
        <div>
            <h1>Dashboard Summary</h1>
            <pre>{JSON.stringify(data, null, 2)}</pre>
        </div>
    );
}

export default App;
