import React from 'react';

const LINE_COLORS = [
    { bg: '#FFCDD2', fg: '#C62828' },
    { bg: '#FFCCBC', fg: '#BF360C' },
    { bg: '#FFF9C4', fg: '#F57F17' },
    { bg: '#DCEDC8', fg: '#33691E' },
    { bg: '#C8E6C9', fg: '#1B5E20' },
    { bg: '#B2EBF2', fg: '#006064' },
    { bg: '#B3E5FC', fg: '#01579B' },
    { bg: '#BBDEFB', fg: '#0D47A1' },
    { bg: '#C5CAE9', fg: '#1A237E' },
    { bg: '#D1C4E9', fg: '#4527A0' },
    { bg: '#E1BEE7', fg: '#6A1B9A' },
    { bg: '#F8BBD0', fg: '#880E4F' },
    { bg: '#FCE4EC', fg: '#AD1457' },
    { bg: '#E0F2F1', fg: '#00695C' },
    { bg: '#F3E5F5', fg: '#6A1B9A' },
];

/**
 * Reusable machine/line selection grid — Color Block style.
 * props:
 *   reports   : array of { id, machineName, productName, orderNumber, startDate?, endDate? }
 *   onSelect  : (report) => void
 *   title     : string (optional, default "เลือก Line การผลิต")
 *   emptyText : string (optional)
 */
const MachineSelectGrid = ({ reports = [], onSelect, title = 'เลือก Line การผลิต', emptyText }) => {
    const sorted = [...reports].sort((a, b) =>
        (a.machineName ?? '').localeCompare(b.machineName ?? '', undefined, { numeric: true })
    );

    return (
        <div style={{ padding: '8px 0' }}>
            {title && (
                <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 12px 0' }}>
                    {title}
                </h2>
            )}

            {sorted.length === 0 && (
                <p style={{ color: '#666' }}>
                    {emptyText ?? 'ไม่มีใบสั่งผลิตที่กำลังทำงานอยู่'}
                </p>
            )}

            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(4, 1fr)',
                gap: '12px',
            }}>
                {sorted.map((report, idx) => {
                    const { bg, fg } = LINE_COLORS[idx % LINE_COLORS.length];
                    return (
                        <div
                            key={report.id}
                            onClick={() => onSelect(report)}
                            style={{
                                backgroundColor: bg,
                                border: `2px solid ${fg}33`,
                                borderRadius: '8px',
                                padding: '14px 16px',
                                minHeight: '90px',
                                cursor: 'pointer',
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center',
                                boxShadow: '0 1px 4px rgba(0,0,0,0.10)',
                                transition: 'transform 0.12s ease, box-shadow 0.12s ease',
                                userSelect: 'none',
                            }}
                            onMouseEnter={e => {
                                e.currentTarget.style.transform = 'translateY(-3px)';
                                e.currentTarget.style.filter = 'brightness(0.95)';
                                e.currentTarget.style.boxShadow = `0 6px 16px ${fg}40`;
                            }}
                            onMouseLeave={e => {
                                e.currentTarget.style.transform = '';
                                e.currentTarget.style.filter = '';
                                e.currentTarget.style.boxShadow = '0 1px 4px rgba(0,0,0,0.10)';
                            }}
                        >
                            <div style={{ fontWeight: 800, fontSize: '1.15rem', color: fg, lineHeight: 1.2 }}>
                                {report.machineName}
                            </div>
                            <div style={{ fontSize: '0.82rem', color: fg, opacity: 0.85, marginTop: '4px',
                                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                {report.productName}
                            </div>
                            {report.orderNumber && (
                                <div style={{ fontSize: '0.72rem', color: fg, opacity: 0.6, marginTop: '2px' }}>
                                    {report.orderNumber}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default MachineSelectGrid;
