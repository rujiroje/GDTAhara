import React, { useEffect, useRef, useState } from 'react';
import JsBarcode from 'jsbarcode';
import QRCode from 'qrcode';

const PackagingLabelPrint = ({ labelData }) => {
    const barcodeRef = useRef(null);
    const [qrDataUrl, setQrDataUrl] = useState('');

    const {
        productName     = '',
        productCode     = '',
        customerCode    = '',
        labelVariant    = '',
        qtyPerBox       = '',
        parentLotNumber = '',
        lotNumber       = '',
        boxNo           = 0,
        machineName     = '',
        operatorName    = '',
        shift           = '',
        cavityNumbers   = '',
    } = labelData ?? {};

    const boxNoStr   = String(boxNo).padStart(3, '0');
    const barcodeVal = `${productCode}-${lotNumber}-${boxNoStr}`;
    const custCode   = customerCode || productCode;

    // Derive display date from lotNumber (yymmdd → dd/mm/Buddhist year)
    const prodDate = (() => {
        if (lotNumber && lotNumber.length >= 6) {
            const yy = lotNumber.slice(0, 2);
            const mm = lotNumber.slice(2, 4);
            const dd = lotNumber.slice(4, 6);
            return `${dd}/${mm}/${parseInt(yy, 10) + 43}`;
        }
        return '';
    })();

    const isExport = labelVariant === 'EXPORT';

    // ── EXPORT: CODE128 barcode ────────────────────────────────────────────
    useEffect(() => {
        if (!isExport) return;
        const svg = barcodeRef.current;
        if (!svg || barcodeVal.length <= 2) return;
        try {
            JsBarcode(svg, barcodeVal, {
                format: 'CODE128', width: 1.3, height: 32,
                displayValue: true, fontSize: 8, margin: 2,
            });
            const w = svg.getAttribute('width');
            const h = svg.getAttribute('height');
            if (w && h) {
                svg.setAttribute('viewBox', `0 0 ${w} ${h}`);
                svg.setAttribute('preserveAspectRatio', 'none');
                svg.removeAttribute('width');
                svg.removeAttribute('height');
                const cx = parseFloat(w) / 2;
                svg.querySelectorAll('text').forEach(t => {
                    t.setAttribute('x', String(cx));
                    t.setAttribute('text-anchor', 'middle');
                });
            }
        } catch { /* invalid barcode value */ }
    }, [barcodeVal, isExport]);

    // ── LOCAL: QR code data URL ────────────────────────────────────────────
    useEffect(() => {
        if (isExport || barcodeVal.length <= 2) return;
        QRCode.toDataURL(barcodeVal, {
            errorCorrectionLevel: 'M',
            width: 180,
            margin: 1,
            color: { dark: '#000000', light: '#ffffff' },
        })
            .then(url => setQrDataUrl(url))
            .catch(() => {});
    }, [barcodeVal, isExport]);

    // ══════════════════════════════════════════════════════════════════
    // LOCAL LABEL — QP-PD-002-F063  80 mm × 85 mm
    // ══════════════════════════════════════════════════════════════════
    if (!isExport) {
        return (
            <div className="pkg-label pkg-label-local">

                {/* Doc ref — top right */}
                <div className="pkg-qp-docref-top">QP-PD-002-F063&nbsp;Rev.00</div>

                {/* Outer table */}
                <table className="pkg-qp-table">
                    <tbody>

                        {/* ── Header: logo + company name ─────────────────── */}
                        <tr>
                            <td colSpan={4} className="pkg-qp-header-cell">
                                <img
                                    src={`${import.meta.env.BASE_URL}tst-logo.png`}
                                    alt="Toyo Seikan (Thailand) Co.,Ltd."
                                    className="pkg-qp-logo"
                                />
                            </td>
                        </tr>

                        {/* ── ชื่อสินค้า ───────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">ชื่อสินค้า</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv" colSpan={2}>{productName}</td>
                        </tr>

                        {/* ── รหัสลูกค้า ──────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">รหัสลูกค้า</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv pkg-qp-fv-large" colSpan={2}>{custCode}</td>
                        </tr>

                        {/* ── รหัส TST ─────────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">รหัส TST</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv pkg-qp-fv-mono" colSpan={2}>{productCode}</td>
                        </tr>

                        {/* ── จำนวนบรรจุ ───────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">จำนวนบรรจุ (ชิ้น)</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv pkg-qp-fv-large" colSpan={2}>
                                {qtyPerBox}&nbsp;(Pcs.)&nbsp;/&nbsp;Box&nbsp;<span className="pkg-qp-fv-boxno">{boxNoStr}</span>
                            </td>
                        </tr>

                        {/* ── Checked By ───────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">Checked By</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv" colSpan={2}>&nbsp;</td>
                        </tr>

                        {/* ── WO ───────────────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">WO</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv pkg-qp-fv-small" colSpan={2}>{parentLotNumber}</td>
                        </tr>

                        {/* ── Cavity No. ────────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">Cavity No.</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv pkg-qp-fv-large" colSpan={2}>{cavityNumbers || '-'}</td>
                        </tr>

                        {/* ── วันที่ผลิต + Shift ─────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">วันที่ผลิต</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv pkg-qp-fv-bold">{prodDate}</td>
                            <td className="pkg-qp-fv pkg-qp-shift">
                                <span className="pkg-qp-shift-label">Shift</span>
                                <span className="pkg-qp-shift-val">{shift || '-'}</span>
                            </td>
                        </tr>

                        {/* ── ผู้บรรจุ ──────────────────────────────────────── */}
                        <tr>
                            <td className="pkg-qp-fl">ผู้บรรจุ</td>
                            <td className="pkg-qp-sep">:</td>
                            <td className="pkg-qp-fv" colSpan={2}>{operatorName}</td>
                        </tr>

                        {/* ── Bottom: QR | มอก info | empty box ────────────── */}
                        <tr>
                            {/* QR code */}
                            <td className="pkg-qp-bot-qr" colSpan={1}>
                                {qrDataUrl
                                    ? <img src={qrDataUrl} alt="QR" className="pkg-qp-qr-img" />
                                    : <div className="pkg-qp-qr-placeholder" />
                                }
                            </td>

                            {/* มอก. info */}
                            <td className="pkg-qp-bot-mog" colSpan={2}>
                                <div className="pkg-mog-icons">
                                    <img src={`${import.meta.env.BASE_URL}MOG.png`} alt="มอก." className="pkg-mog-icon-sm" />
                                </div>
                                <div className="pkg-mog-text">
                                    <b>มอก. 655 เล่ม 1-2553</b><br />
                                    ชื่อผู้รับอนุญาต บริษัท โตโอไซกัน<br />
                                    (ประเทศไทย) จำกัด<br />
                                    ภาชนะพลาสติกสำหรับอาหาร<br />
                                    ประเภททนความร้อน<br />
                                    ประเภทธรรมดา<br />
                                    ประเภททนความเย็น<br />
                                    ชนิดพอลิโพรพิลีน (PP)<br />
                                    แบบใช้ครั้งเดียว<br />
                                    อุณหภูมิการใช้งาน -12°C - 100°C<br />
                                    ห้ามวางใกล้เปลวไฟ<br />
                                    <span className="pkg-mog-nomicro">🚫 ห้ามใช้กับเตาไมโครเวฟ</span>
                                </div>
                            </td>

                            {/* Empty box */}
                            <td className="pkg-qp-bot-empty" />
                        </tr>

                    </tbody>
                </table>

                {/* Doc ref — bottom right */}
                <div className="pkg-qp-docref-bot">&lt;QP - PD - 002 - F063&nbsp;Rev.00&gt;</div>

            </div>
        );
    }

    // ══════════════════════════════════════════════════════════════════
    // EXPORT LABEL — 4" × 8" (original layout kept)
    // ══════════════════════════════════════════════════════════════════
    return (
        <div className="pkg-label">

            {/* ── Logo ──────────────────────────────────────────────── */}
            <div className="pkg-logo-wrap">
                <img src={`${import.meta.env.BASE_URL}tst-logo.png`} alt="Toyo Seikan (Thailand) Co.,Ltd." className="pkg-logo" />
            </div>
            <hr className="pkg-hr" />

            {/* ── Barcode ───────────────────────────────────────────── */}
            <div className="pkg-barcode-wrap">
                <svg ref={barcodeRef} />
            </div>
            <hr className="pkg-hr" />

            {/* ── Body: data table | มอก.+box ──────────────────────── */}
            <div className="pkg-body">
                <div className="pkg-col-data">
                    <table className="pkg-table">
                        <tbody>
                            <tr>
                                <td className="pkg-fl pkg-fl-big">ชื่อสินค้า</td>
                                <td className="pkg-fv pkg-fv-bold" colSpan={3}>{productName}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl pkg-fl-big">รหัสลูกค้า</td>
                                <td className="pkg-fv pkg-fv-bold pkg-fv-mono" colSpan={3}>{custCode}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl">รหัส TST</td>
                                <td className="pkg-fv pkg-fv-bold pkg-fv-mono" colSpan={3}>{productCode}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl pkg-fl-big">จำนวนบรรจุ (ชิ้น)</td>
                                <td className="pkg-fv pkg-fv-bold" colSpan={3}>{qtyPerBox}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl pkg-fl-big">Production Lot</td>
                                <td className="pkg-fv pkg-fv-bold pkg-fv-mono" colSpan={3}>{lotNumber}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl">WO</td>
                                <td className="pkg-fv pkg-fv-small" colSpan={3}>{parentLotNumber}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl">เครื่องจักร</td>
                                <td className="pkg-fv pkg-fv-bold pkg-fv-mono" colSpan={3}>{machineName}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl pkg-fl-big">กล่องที่</td>
                                <td className="pkg-fv pkg-fv-boxno" colSpan={3}>{boxNoStr}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl">วันที่ผลิต</td>
                                <td className="pkg-fv pkg-fv-bold">{prodDate}</td>
                                <td className="pkg-fl-sm">Shift</td>
                                <td className="pkg-fv pkg-fv-bold">{shift || '-'}</td>
                            </tr>
                            <tr>
                                <td className="pkg-fl pkg-fl-big">ผู้บรรจุ</td>
                                <td className="pkg-fv pkg-fv-bold" colSpan={3}>{operatorName}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div className="pkg-col-makok">
                    <img src={`${import.meta.env.BASE_URL}MOG.png`} alt="มอก." className="pkg-makok-icon-solo" />
                    <div className="pkg-makok-empty-box" />
                </div>
            </div>

            {/* ── GOODS section ─────────────────────────────────────── */}
            <div className="pkg-export-section">
                <div className="pkg-export-title">GOODS</div>
                <div className="pkg-export-subtitle">{productName}</div>
                <table className="pkg-export-table">
                    <tbody>
                        <tr>
                            <td className="pkg-exp-no">1.</td>
                            <td className="pkg-exp-label">Supplier name</td>
                            <td className="pkg-exp-sep">:</td>
                            <td className="pkg-exp-val">TOYO SEIKAN (THAILAND) CO.,LTD</td>
                        </tr>
                        <tr>
                            <td className="pkg-exp-no">2.</td>
                            <td className="pkg-exp-label">Quantity</td>
                            <td className="pkg-exp-sep">:</td>
                            <td className="pkg-exp-val">{qtyPerBox} pcs/case</td>
                        </tr>
                        <tr>
                            <td className="pkg-exp-no">3.</td>
                            <td className="pkg-exp-label">Loading date</td>
                            <td className="pkg-exp-sep">:</td>
                            <td className="pkg-exp-val">.........</td>
                        </tr>
                        <tr>
                            <td className="pkg-exp-no">4.</td>
                            <td className="pkg-exp-label">Origin</td>
                            <td className="pkg-exp-sep">:</td>
                            <td className="pkg-exp-val">Thailand</td>
                        </tr>
                    </tbody>
                </table>
            </div>

        </div>
    );
};

export default PackagingLabelPrint;
