// =================================================================
// File: src/components/common/GaugeCard.js
// =================================================================
import React from 'react';
import Chart from 'react-apexcharts';

const GaugeCard = ({ machineName, productName, target, current, ng }) => {
    const percent = target > 0 ? (current / target) * 100 : 0;
    const formatNumber = (num) => new Intl.NumberFormat('en-US').format(num);

    const chartOptions = {
        chart: { type: 'radialBar', sparkline: { enabled: true } },
        plotOptions: {
            radialBar: {
                startAngle: -90, endAngle: 90, hollow: { size: '75%' },
                track: { background: "#e7e7e7", strokeWidth: '97%' },
                dataLabels: {
                    name: { show: false },
                    value: { offsetY: -2, fontSize: '22px', formatter: (val) => val.toFixed(1) + "%" }
                }
            }
        },
        grid: { padding: { top: -10 } },
        colors: ["#3b82f6"],
        labels: ['Progress'],
    };

    const chartSeries = [percent];

    return (
        <div className="gauge-card">
            <div className="gauge-chart-container" style={{width: '120px', height: '120px', position: 'relative', flexShrink: 0}}>
                 <Chart options={chartOptions} series={chartSeries} type="radialBar" height="140" />
            </div>
            <div className="gauge-info" style={{flexGrow: 1}}>
                <h3 className="gauge-machine-name" style={{margin: '0 0 0.25rem 0', fontSize: '1.25rem', color: '#111827'}}>{machineName}</h3>
                <p className="gauge-product-name" style={{margin: '0 0 1rem 0', color: '#6b7280', fontSize: '0.875rem', borderBottom: '1px solid #f3f4f6', paddingBottom: '1rem'}}>{productName}</p>
                <div className="gauge-details" style={{display: 'flex', justifyContent: 'space-between', textAlign: 'center'}}>
                    <div className="gauge-detail-item" style={{display: 'flex', flexDirection: 'column'}}>
                        <span style={{fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.25rem'}}>ยอดผลิตดี</span>
                        <strong style={{fontSize: '1.25rem', color: '#1d4ed8'}}>{formatNumber(current)}</strong>
                    </div>
                    <div className="gauge-detail-item" style={{display: 'flex', flexDirection: 'column'}}>
                        <span style={{fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.25rem'}}>เป้าหมาย</span>
                        <strong style={{fontSize: '1.25rem', color: '#374151'}}>{formatNumber(target)}</strong>
                    </div>
                    <div className="gauge-detail-item ng" style={{display: 'flex', flexDirection: 'column'}}>
                        <span style={{fontSize: '0.875rem', color: '#6b7280', marginBottom: '0.25rem'}}>ของเสีย</span>
                        <strong style={{fontSize: '1.25rem', color: '#be185d'}}>{formatNumber(ng)}</strong>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default GaugeCard;
