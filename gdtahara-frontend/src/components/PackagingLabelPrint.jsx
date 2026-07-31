import React, { useEffect, useRef } from 'react';
import JsBarcode from 'jsbarcode';

const PackagingLabelPrint = ({ labelData }) => {
    const barcodeRef = useRef(null);

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

    useEffect(() => {
        const svg = barcodeRef.current;
        if (!svg || barcodeVal.length <= 2) return;
        try {
            JsBarcode(svg, barcodeVal, {
                format:       'CODE128',
                width:        1.3,
                height:       32,
                displayValue: true,
                fontSize:     8,
                margin:       2,
            });
            // Make SVG stretch to full container width at fixed height (no auto-height gap)
            const w = svg.getAttribute('width');
            const h = svg.getAttribute('height');
            if (w && h) {
                svg.setAttribute('viewBox', `0 0 ${w} ${h}`);
                svg.setAttribute('preserveAspectRatio', 'none');
                svg.removeAttribute('width');
                svg.removeAttribute('height');
            }
        } catch { /* invalid barcode value */ }
    }, [barcodeVal]);

    return (
        <div className="pkg-label">

            {/* ── Logo (full width) ─────────────────────────────────── */}
            <div className="pkg-logo-wrap">
                <img src="/tst-logo.png" alt="Toyo Seikan (Thailand) Co.,Ltd." className="pkg-logo" />
            </div>
            <hr className="pkg-hr" />

            {/* ── Barcode (full width — same as logo) ───────────────── */}
            <div className="pkg-barcode-wrap">
                <svg ref={barcodeRef} />
            </div>
            <hr className="pkg-hr" />

            {/* ── Body: data (left) | มอก.+box (right) ─────────────── */}
            <div className="pkg-body">

                {/* LEFT: data table */}
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
                                <td className="pkg-fl pkg-fl-big">กล่องที่</td>
                                <td className="pkg-fv pkg-fv-boxno" colSpan={3}>{boxNoStr}</td>
                            </tr>
                            {/* Date + Shift: 4-cell row */}
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

                {/* RIGHT: มอก. image (top) + empty bordered rectangle (bottom) */}
                <div className="pkg-col-makok">
                    <img src="/MOG.png" alt="มอก." className="pkg-makok-icon-solo" />
                    <div className="pkg-makok-empty-box" />
                </div>

            </div>
        </div>
    );
};

export default PackagingLabelPrint;
