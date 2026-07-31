import React, { useState, useEffect, useCallback } from 'react';
import * as XLSX from 'xlsx';
import { fetchBlowDailyReport } from '../../api/phase1Api';

const BlowDailyReportPage = ({ reportId, initialDate, onBack }) => {
    const [date, setDate]     = useState(initialDate || new Date().toISOString().split('T')[0]);
    const [data, setData]     = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError]   = useState('');

    const load = useCallback(async () => {
        if (!reportId || !date) return;
        setLoading(true);
        setError('');
        try {
            const result = await fetchBlowDailyReport(reportId, date);
            setData(result);
        } catch (e) {
            setError('ไม่สามารถโหลดข้อมูลได้: ' + (e?.response?.data?.message || e.message));
        } finally {
            setLoading(false);
        }
    }, [reportId, date]);

    useEffect(() => { load(); }, [load]);

    const exportExcel = () => {
        if (!data) return;

        const wb = XLSX.utils.book_new();

        // ── Sheet: Report ────────────────────────────────────────────────────
        const rows = [];

        // Header block
        rows.push(['ใบรายงานประจำวัน BLOW (RBL MACHINE)', '', '', '', '', '']);
        rows.push([]);
        rows.push(['วันที่', data.reportDate, '', 'เครื่อง', data.machineName, '']);
        rows.push(['รหัสสินค้า', data.productCode, '', 'ชื่อสินค้า', data.productName, '']);
        rows.push(['เลข WO', data.orderNumber, '', 'Lot Number', data.parentLotNumber, '']);
        rows.push(['Shift', data.shift || '-', '', 'เป้าหมาย', data.targetQty ?? '-', '']);
        rows.push([]);

        // Section 1
        rows.push(['ส่วนที่ 1: ผลผลิต', '', '', '', '', '']);
        rows.push(['จำนวนกล่องบรรจุ', data.totalBoxesPacked, '', 'จำนวน NG', data.totalNgQty, '']);
        rows.push([]);

        // Section 2
        rows.push(['ส่วนที่ 2: วัสดุบรรจุภัณฑ์ (UNBW)', '', '', '', '', '']);
        rows.push(['รหัส', 'ชื่อ', 'หน่วย', 'ต่อกล่อง', 'กล่องบรรจุ', 'รวมใช้']);
        (data.packingMaterials || []).forEach(r =>
            rows.push([r.rmCode, r.rmName, r.unit, r.qtyPerBox, r.boxesPacked, r.totalQty])
        );
        rows.push([]);

        // Section 3
        rows.push(['ส่วนที่ 3: วัตถุดิบที่ใช้', '', '', '', '', '']);
        rows.push(['รหัส', 'ชื่อ', 'ประเภท', 'หน่วย', 'ปริมาณ', 'Lot']);
        (data.rawMaterials || []).forEach(r =>
            rows.push([r.materialCode, r.materialName, r.materialType, r.unit, r.totalUsed, r.lotNumber])
        );
        rows.push([]);

        // Section 4
        rows.push(['ส่วนที่ 4: ผู้ปฏิบัติงาน', '', '', '', '', '']);
        (data.activeUsers || []).forEach((u, i) => rows.push([i + 1, u, '', '', '', '']));
        rows.push([]);

        // Section 5 — NG (summarised)
        const ngSummaryXls = Object.values(
            (data.ngDetails || []).reduce((acc, r) => {
                const key = `${r.ngType}||${r.ngDescription}||${r.source}`;
                if (!acc[key]) acc[key] = { ngType: r.ngType, ngDescription: r.ngDescription, source: r.source, quantity: 0 };
                acc[key].quantity += Number(r.quantity) || 0;
                return acc;
            }, {})
        ).sort((a, b) => b.quantity - a.quantity);
        rows.push(['ส่วนที่ 5: สรุปรายละเอียด NG', '', '', '', '', '']);
        rows.push(['ประเภท NG', 'คำอธิบาย', 'แหล่งที่มา', 'จำนวนรวม (ชิ้น)', '', '']);
        ngSummaryXls.forEach(r => rows.push([r.ngType, r.ngDescription, r.source, r.quantity, '', '']));
        rows.push([]);

        // Section 5 — Scrap
        rows.push(['ส่วนที่ 5: น้ำหนักเศษพลาสติก', '', '', '', '', '']);
        rows.push(['ประเภทเศษ', 'ประเภทวัสดุ', 'น้ำหนัก (kg)', 'ผู้บันทึก', 'เวลา', '']);
        (data.scrapDetails || []).forEach(r =>
            rows.push([r.scrapType, r.matType, r.weightKg, r.technicianName, r.timestamp, ''])
        );
        rows.push([]);

        // Section 5c — Scrap Comparison
        rows.push(['ส่วนที่ 5: เปรียบเทียบเศษพลาสติก (BOM vs จริง)', '', '', '', '', '']);
        rows.push(['ประเภทวัสดุ', 'ชื่อ', 'คาดหวัง BOM (kg)', 'จริง (kg)', 'ส่วนต่าง (kg)', '% ส่วนต่าง']);
        (data.scrapComparison || []).forEach(r => {
            const variance = Number(r.varianceKg);
            const expected = Number(r.expectedKg);
            const varPct = expected > 0
                ? `${variance >= 0 ? '+' : ''}${(variance / expected * 100).toFixed(1)}%` : '-';
            rows.push([r.materialType, r.rmName,
                Number(r.expectedKg).toFixed(3), Number(r.actualKg).toFixed(3),
                `${variance >= 0 ? '+' : ''}${variance.toFixed(3)}`, varPct]);
        });
        rows.push([]);

        // Section 6
        rows.push(['ส่วนที่ 6: Downtime', '', '', '', '', '']);
        rows.push(['ประเภท', 'หมวด', 'สาเหตุ', 'แก้ไข', 'เริ่ม', 'สิ้นสุด', 'นาที']);
        (data.downtimeEvents || []).forEach(r =>
            rows.push([r.category, r.downtimeType, r.reason, r.solution, r.startTime, r.endTime, r.durationMinutes])
        );
        rows.push(['', '', '', '', '', 'รวม Downtime', data.totalDowntimeMinutes + ' นาที']);

        const ws = XLSX.utils.aoa_to_sheet(rows);

        // Column widths
        ws['!cols'] = [
            { wch: 20 }, { wch: 30 }, { wch: 12 }, { wch: 12 }, { wch: 15 }, { wch: 20 }, { wch: 10 }
        ];

        XLSX.utils.book_append_sheet(wb, ws, 'DailyBlowReport');
        XLSX.writeFile(wb, `DailyBlowReport_${data.orderNumber || reportId}_${date}.xlsx`);
    };

    // ── Render helpers ──────────────────────────────────────────────────────

    const SectionTitle = ({ children }) => (
        <div style={{
            background: '#1a3a5c', color: '#fff', padding: '6px 12px',
            fontWeight: 700, fontSize: '0.85rem', borderRadius: '4px 4px 0 0', marginBottom: 0
        }}>
            {children}
        </div>
    );

    const TableWrap = ({ children }) => (
        <div style={{ overflowX: 'auto', borderRadius: '0 0 4px 4px', border: '1px solid #cdd2d8', borderTop: 'none', marginBottom: '1.5rem' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem' }}>
                {children}
            </table>
        </div>
    );

    const Th = ({ children, align }) => (
        <th style={{
            background: '#e8edf3', padding: '6px 10px', textAlign: align || 'left',
            borderBottom: '1px solid #cdd2d8', borderRight: '1px solid #cdd2d8',
            fontWeight: 600, whiteSpace: 'nowrap'
        }}>{children}</th>
    );

    const Td = ({ children, align, mono }) => (
        <td style={{
            padding: '5px 10px', textAlign: align || 'left',
            borderBottom: '1px solid #e9ecef', borderRight: '1px solid #e9ecef',
            fontFamily: mono ? 'monospace' : undefined
        }}>{children ?? '-'}</td>
    );

    if (loading) return (
        <div style={{ padding: '2rem', textAlign: 'center' }}>
            <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>⏳</div>
            <p>กำลังโหลดข้อมูล...</p>
        </div>
    );

    return (
        <div style={{ padding: '1.5rem', maxWidth: '1100px', margin: '0 auto' }}>

            {/* ── Top bar ──────────────────────────────────────────────────── */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
                <button onClick={onBack} style={btnStyle('#6c757d')}>← กลับ</button>
                <h2 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700 }}>
                    ใบรายงานประจำวัน BLOW (RBL MACHINE)
                </h2>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginLeft: 'auto' }}>
                    <label style={{ fontSize: '0.85rem', fontWeight: 600 }}>วันที่:</label>
                    <input
                        type="date"
                        value={date}
                        onChange={e => setDate(e.target.value)}
                        style={{ padding: '5px 8px', borderRadius: 4, border: '1px solid #cdd2d8', fontSize: '0.85rem' }}
                    />
                    <button onClick={exportExcel} disabled={!data} style={btnStyle('#217346')}>
                        ⬇ Export Excel
                    </button>
                </div>
            </div>

            {error && (
                <div style={{ padding: '0.75rem 1rem', background: '#fff3cd', border: '1px solid #ffc107', borderRadius: 4, marginBottom: '1rem', fontSize: '0.85rem' }}>
                    ⚠️ {error}
                </div>
            )}

            {data && (
                <>
                    {/* ── Header Card ──────────────────────────────────────── */}
                    <div style={{
                        display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem 2rem',
                        background: '#f8f9fa', border: '1px solid #cdd2d8', borderRadius: 6,
                        padding: '1rem 1.5rem', marginBottom: '1.5rem', fontSize: '0.85rem'
                    }}>
                        <Row label="วันที่"       value={data.reportDate} />
                        <Row label="เครื่อง"      value={data.machineName} />
                        <Row label="รหัสสินค้า"   value={data.productCode} mono />
                        <Row label="ชื่อสินค้า"   value={data.productName} />
                        <Row label="เลข WO"       value={data.orderNumber} mono />
                        <Row label="Lot Number"   value={data.parentLotNumber} mono />
                        <Row label="Shift"        value={data.shift || '-'} />
                        <Row label="เป้าหมาย"    value={data.targetQty != null ? data.targetQty.toLocaleString() + ' ชิ้น' : '-'} />
                    </div>

                    {/* ── Section 1: Productivity ──────────────────────────── */}
                    <SectionTitle>ส่วนที่ 1 — ผลผลิต (Productivity)</SectionTitle>
                    <div style={{
                        display: 'flex', gap: '1.5rem', padding: '0.75rem 1rem',
                        border: '1px solid #cdd2d8', borderTop: 'none', borderRadius: '0 0 4px 4px',
                        marginBottom: '1.5rem', flexWrap: 'wrap'
                    }}>
                        <KpiBox label="กล่องบรรจุ (กล่อง)" value={data.totalBoxesPacked.toLocaleString()} color="#1a3a5c" />
                        <KpiBox label="จำนวน NG (ชิ้น)"    value={data.totalNgQty.toLocaleString()}    color="#c0392b" />
                    </div>

                    {/* ── Section 2: Packing Materials ────────────────────── */}
                    <SectionTitle>ส่วนที่ 2 — วัสดุบรรจุภัณฑ์ (Packaging Materials — UNBW)</SectionTitle>
                    <TableWrap>
                        <thead>
                            <tr>
                                <Th>รหัสวัสดุ</Th>
                                <Th>ชื่อวัสดุ</Th>
                                <Th align="center">หน่วย</Th>
                                <Th align="right">จำนวนต่อกล่อง</Th>
                                <Th align="right">กล่องบรรจุ</Th>
                                <Th align="right">รวมใช้</Th>
                            </tr>
                        </thead>
                        <tbody>
                            {(data.packingMaterials || []).length === 0
                                ? <tr><td colSpan={6} style={{ padding: '1rem', textAlign: 'center', color: '#888' }}>ไม่มีข้อมูลวัสดุบรรจุภัณฑ์ (ตรวจสอบ BOM ประเภท UNBW)</td></tr>
                                : (data.packingMaterials).map((r, i) => (
                                    <tr key={i}>
                                        <Td mono>{r.rmCode}</Td>
                                        <Td>{r.rmName}</Td>
                                        <Td align="center">{r.unit}</Td>
                                        <Td align="right">{Number(r.qtyPerBox).toFixed(4)}</Td>
                                        <Td align="right">{r.boxesPacked.toLocaleString()}</Td>
                                        <Td align="right"><strong>{Number(r.totalQty).toFixed(2)}</strong></Td>
                                    </tr>
                                ))}
                        </tbody>
                    </TableWrap>

                    {/* ── Section 3: Raw Materials ─────────────────────────── */}
                    <SectionTitle>ส่วนที่ 3 — วัตถุดิบที่ใช้ (Raw Material Usage)</SectionTitle>
                    <TableWrap>
                        <thead>
                            <tr>
                                <Th>รหัสวัตถุดิบ</Th>
                                <Th>ชื่อวัตถุดิบ</Th>
                                <Th align="center">ประเภท</Th>
                                <Th align="center">หน่วย</Th>
                                <Th align="right">ปริมาณ</Th>
                                <Th>Lot วัตถุดิบ</Th>
                            </tr>
                        </thead>
                        <tbody>
                            {(data.rawMaterials || []).length === 0
                                ? <tr><td colSpan={6} style={{ padding: '1rem', textAlign: 'center', color: '#888' }}>ไม่มีบันทึกการใช้วัตถุดิบในวันนี้</td></tr>
                                : (data.rawMaterials).map((r, i) => (
                                    <tr key={i}>
                                        <Td mono>{r.materialCode}</Td>
                                        <Td>{r.materialName}</Td>
                                        <Td align="center"><TypeBadge type={r.materialType} /></Td>
                                        <Td align="center">{r.unit}</Td>
                                        <Td align="right"><strong>{Number(r.totalUsed).toFixed(3)}</strong></Td>
                                        <Td mono>{r.lotNumber || '-'}</Td>
                                    </tr>
                                ))}
                        </tbody>
                    </TableWrap>

                    {/* ── Section 4: Labor ─────────────────────────────────── */}
                    <SectionTitle>ส่วนที่ 4 — ผู้ปฏิบัติงาน (Labor)</SectionTitle>
                    <div style={{
                        border: '1px solid #cdd2d8', borderTop: 'none', borderRadius: '0 0 4px 4px',
                        padding: '0.75rem 1rem', marginBottom: '1.5rem', display: 'flex', flexWrap: 'wrap', gap: '0.5rem'
                    }}>
                        {(data.activeUsers || []).length === 0
                            ? <span style={{ color: '#888', fontSize: '0.85rem' }}>ไม่มีบันทึกในวันนี้</span>
                            : (data.activeUsers).map((u, i) => (
                                <span key={i} style={{
                                    background: '#e8edf3', border: '1px solid #cdd2d8',
                                    borderRadius: 12, padding: '3px 12px', fontSize: '0.82rem', fontWeight: 600
                                }}>{u}</span>
                            ))}
                    </div>

                    {/* ── Section 5: NG Summary ─────────────────────────────── */}
                    <SectionTitle>ส่วนที่ 5 — สรุปรายละเอียด NG</SectionTitle>
                    {(() => {
                        const ngSummary = Object.values(
                            (data.ngDetails || []).reduce((acc, r) => {
                                const key = `${r.ngType}||${r.ngDescription}||${r.source}`;
                                if (!acc[key]) acc[key] = { ngType: r.ngType, ngDescription: r.ngDescription, source: r.source, quantity: 0 };
                                acc[key].quantity += Number(r.quantity) || 0;
                                return acc;
                            }, {})
                        ).sort((a, b) => b.quantity - a.quantity);
                        const total = ngSummary.reduce((s, r) => s + r.quantity, 0);
                        return (
                            <TableWrap>
                                <thead>
                                    <tr>
                                        <Th>ประเภท NG</Th>
                                        <Th>คำอธิบาย</Th>
                                        <Th align="center">แหล่งที่มา</Th>
                                        <Th align="right">จำนวนรวม (ชิ้น)</Th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {ngSummary.length === 0
                                        ? <tr><td colSpan={4} style={{ padding: '1rem', textAlign: 'center', color: '#888' }}>ไม่มีบันทึก NG</td></tr>
                                        : ngSummary.map((r, i) => (
                                            <tr key={i}>
                                                <Td mono>{r.ngType}</Td>
                                                <Td>{r.ngDescription}</Td>
                                                <Td align="center">{r.source || '-'}</Td>
                                                <Td align="right" mono><strong>{r.quantity.toLocaleString()}</strong></Td>
                                            </tr>
                                        ))}
                                    {ngSummary.length > 0 && (
                                        <tr style={{ background: '#e8edf3', fontWeight: 700 }}>
                                            <td colSpan={3} style={{ padding: '5px 10px', textAlign: 'right', fontSize: '0.82rem' }}>รวม NG ทั้งหมด:</td>
                                            <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'monospace', fontSize: '0.82rem', color: '#c62828' }}>
                                                {total.toLocaleString()} ชิ้น
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </TableWrap>
                        );
                    })()}

                    {/* ── Section 5b: Scrap Weight ──────────────────────────── */}
                    <SectionTitle>ส่วนที่ 5 (ต่อ) — น้ำหนักเศษพลาสติก (Scrap Weight)</SectionTitle>
                    <TableWrap>
                        <thead>
                            <tr>
                                <Th>ประเภทเศษ</Th>
                                <Th align="center">ประเภทวัสดุ</Th>
                                <Th align="right">น้ำหนัก (kg)</Th>
                                <Th>ผู้บันทึก</Th>
                                <Th align="center">เวลา</Th>
                            </tr>
                        </thead>
                        <tbody>
                            {(data.scrapDetails || []).length === 0
                                ? <tr><td colSpan={5} style={{ padding: '1rem', textAlign: 'center', color: '#888' }}>ไม่มีบันทึกเศษพลาสติก</td></tr>
                                : (data.scrapDetails).map((r, i) => (
                                    <tr key={i}>
                                        <Td>{r.scrapType}</Td>
                                        <Td align="center"><TypeBadge type={r.matType} /></Td>
                                        <Td align="right" mono>{Number(r.weightKg).toFixed(2)}</Td>
                                        <Td>{r.technicianName}</Td>
                                        <Td align="center">{r.timestamp}</Td>
                                    </tr>
                                ))}
                        </tbody>
                    </TableWrap>

                    {/* ── Section 5c: Scrap Comparison (BOM vs Actual) ─────── */}
                    <SectionTitle>ส่วนที่ 5 — เปรียบเทียบเศษพลาสติก (BOM vs จริง)</SectionTitle>
                    <TableWrap>
                        <thead>
                            <tr>
                                <Th align="center">ประเภทวัสดุ</Th>
                                <Th>ชื่อ</Th>
                                <Th align="right">คาดหวัง BOM (kg)</Th>
                                <Th align="right">จริง (kg)</Th>
                                <Th align="right">ส่วนต่าง (kg)</Th>
                                <Th align="right">% ส่วนต่าง</Th>
                            </tr>
                        </thead>
                        <tbody>
                            {(data.scrapComparison || []).length === 0
                                ? <tr><td colSpan={6} style={{ padding: '1rem', textAlign: 'center', color: '#888' }}>ไม่มีข้อมูล (ตรวจสอบ BOM Scrap Items)</td></tr>
                                : (data.scrapComparison).map((r, i) => {
                                    const variance = Number(r.varianceKg);
                                    const expected = Number(r.expectedKg);
                                    const varPct = expected > 0
                                        ? `${variance >= 0 ? '+' : ''}${(variance / expected * 100).toFixed(1)}%`
                                        : null;
                                    const varColor = variance > 0.001 ? '#c62828' : variance < -0.001 ? '#2e7d32' : '#546e7a';
                                    return (
                                        <tr key={i}>
                                            <Td align="center"><TypeBadge type={r.materialType} /></Td>
                                            <Td>{r.rmName}</Td>
                                            <Td align="right" mono>{expected > 0 ? expected.toFixed(3) : '—'}</Td>
                                            <Td align="right" mono>{Number(r.actualKg).toFixed(3)}</Td>
                                            <Td align="right">
                                                <span style={{ fontFamily: 'monospace', color: varColor, fontWeight: 700 }}>
                                                    {variance >= 0 ? '+' : ''}{variance.toFixed(3)}
                                                </span>
                                            </Td>
                                            <Td align="right">
                                                {varPct
                                                    ? <span style={{ color: varColor, fontWeight: 600 }}>{varPct}</span>
                                                    : <span style={{ color: '#aaa' }}>—</span>}
                                            </Td>
                                        </tr>
                                    );
                                })}
                            {(data.scrapComparison || []).length > 0 && (() => {
                                const items = data.scrapComparison;
                                const totExp = items.reduce((s, r) => s + Number(r.expectedKg), 0);
                                const totAct = items.reduce((s, r) => s + Number(r.actualKg), 0);
                                const totVar = totAct - totExp;
                                const varColor = totVar > 0.001 ? '#c62828' : totVar < -0.001 ? '#2e7d32' : '#546e7a';
                                return (
                                    <tr style={{ background: '#e8edf3', fontWeight: 700 }}>
                                        <td colSpan={2} style={{ padding: '5px 10px', textAlign: 'right', fontSize: '0.82rem' }}>รวม:</td>
                                        <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'monospace', fontSize: '0.82rem' }}>{totExp.toFixed(3)}</td>
                                        <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'monospace', fontSize: '0.82rem' }}>{totAct.toFixed(3)}</td>
                                        <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'monospace', fontSize: '0.82rem', color: varColor }}>
                                            {totVar >= 0 ? '+' : ''}{totVar.toFixed(3)}
                                        </td>
                                        <td style={{ padding: '5px 10px', textAlign: 'right', fontSize: '0.82rem', color: varColor }}>
                                            {totExp > 0 ? `${totVar >= 0 ? '+' : ''}${(totVar / totExp * 100).toFixed(1)}%` : '—'}
                                        </td>
                                    </tr>
                                );
                            })()}
                        </tbody>
                    </TableWrap>

                    {/* ── Section 6: Downtime ───────────────────────────────── */}
                    <SectionTitle>ส่วนที่ 6 — Downtime</SectionTitle>
                    <TableWrap>
                        <thead>
                            <tr>
                                <Th>หมวด</Th>
                                <Th>ประเภท</Th>
                                <Th>สาเหตุ</Th>
                                <Th>การแก้ไข</Th>
                                <Th align="center">เริ่ม</Th>
                                <Th align="center">สิ้นสุด</Th>
                                <Th align="right">นาที</Th>
                            </tr>
                        </thead>
                        <tbody>
                            {(data.downtimeEvents || []).length === 0
                                ? <tr><td colSpan={7} style={{ padding: '1rem', textAlign: 'center', color: '#888' }}>ไม่มีบันทึก Downtime</td></tr>
                                : (data.downtimeEvents).map((r, i) => (
                                    <tr key={i}>
                                        <Td><CategoryBadge cat={r.category} /></Td>
                                        <Td>{r.downtimeType}</Td>
                                        <Td>{r.reason || '-'}</Td>
                                        <Td>{r.solution || '-'}</Td>
                                        <Td align="center">{r.startTime}</Td>
                                        <Td align="center">{r.endTime || '—'}</Td>
                                        <Td align="right" mono>{r.durationMinutes}</Td>
                                    </tr>
                                ))}
                            <tr style={{ background: '#e8edf3', fontWeight: 700 }}>
                                <td colSpan={6} style={{ padding: '5px 10px', textAlign: 'right', fontSize: '0.82rem' }}>รวม Downtime:</td>
                                <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'monospace', fontSize: '0.82rem' }}>
                                    {data.totalDowntimeMinutes} นาที
                                </td>
                            </tr>
                        </tbody>
                    </TableWrap>
                </>
            )}
        </div>
    );
};

// ── Sub-components ──────────────────────────────────────────────────────────

const Row = ({ label, value, mono }) => (
    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'baseline' }}>
        <span style={{ color: '#5a6a7a', minWidth: 100, fontSize: '0.82rem' }}>{label}:</span>
        <span style={{ fontWeight: 600, fontFamily: mono ? 'monospace' : undefined }}>{value || '-'}</span>
    </div>
);

const KpiBox = ({ label, value, color }) => (
    <div style={{
        background: '#fff', border: `2px solid ${color}`, borderRadius: 8,
        padding: '0.75rem 1.5rem', textAlign: 'center', minWidth: 140
    }}>
        <div style={{ fontSize: '0.75rem', color: '#5a6a7a', marginBottom: 4 }}>{label}</div>
        <div style={{ fontSize: '1.6rem', fontWeight: 800, color, fontFamily: 'monospace' }}>{value}</div>
    </div>
);

const typeColor = {
    VIRGIN: '#1565c0', EVOH: '#6a0dad', ADMER: '#e65100', MIX: '#2e7d32',
    UNBW: '#00838f', default: '#546e7a'
};
const TypeBadge = ({ type }) => (
    <span style={{
        background: (typeColor[type] || typeColor.default) + '18',
        color: typeColor[type] || typeColor.default,
        border: `1px solid ${typeColor[type] || typeColor.default}44`,
        borderRadius: 10, padding: '2px 8px', fontWeight: 600, fontSize: '0.75rem'
    }}>{type || '-'}</span>
);

const catColor = { PLANNED: '#1565c0', UNPLANNED: '#c62828', EXTERNAL: '#e65100', default: '#546e7a' };
const CategoryBadge = ({ cat }) => (
    <span style={{
        background: (catColor[cat] || catColor.default) + '18',
        color: catColor[cat] || catColor.default,
        border: `1px solid ${catColor[cat] || catColor.default}44`,
        borderRadius: 10, padding: '2px 8px', fontWeight: 600, fontSize: '0.75rem'
    }}>{cat || '-'}</span>
);

const btnStyle = (bg) => ({
    background: bg, color: '#fff', border: 'none', borderRadius: 5,
    padding: '6px 14px', cursor: 'pointer', fontSize: '0.82rem', fontWeight: 600
});

export default BlowDailyReportPage;
