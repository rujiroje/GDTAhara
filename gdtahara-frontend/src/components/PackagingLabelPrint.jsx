import React, { useEffect, useRef } from 'react';
import JsBarcode from 'jsbarcode';

/**
 * Renders a single packaging box label as HTML.
 * Call window.print() after mounting to open the browser print dialog.
 *
 * labelData shape:
 *   productName, productCode, customerCode, labelVariant,
 *   qtyPerBox, parentLotNumber, lotNumber, boxNo (number),
 *   machineName, operatorName
 */
const PackagingLabelPrint = ({ labelData }) => {
    const barcodeRef = useRef(null);

    const {
        productName    = '',
        productCode    = '',
        customerCode   = '',
        labelVariant   = '',
        qtyPerBox      = '',
        parentLotNumber = '',
        lotNumber      = '',
        boxNo          = 0,
        machineName    = '',
        operatorName   = '',
    } = labelData ?? {};

    const boxNoStr    = String(boxNo).padStart(3, '0');
    const barcodeVal  = `${productCode}-${lotNumber}-${boxNoStr}`;
    const displayCust = customerCode || productCode;
    const displayVariant = [labelVariant, productCode].filter(Boolean).join('  |  ');

    useEffect(() => {
        if (barcodeRef.current) {
            try {
                JsBarcode(barcodeRef.current, barcodeVal, {
                    format: 'CODE128',
                    width: 2,
                    height: 60,
                    displayValue: true,
                    fontSize: 11,
                    margin: 4,
                });
            } catch { /* invalid barcode value — skip */ }
        }
    }, [barcodeVal]);

    return (
        <div className="pkg-label">
            {/* ── Header ─────────────────────────────────── */}
            <div className="pkg-header">
                <div className="pkg-company">Toyo Seikan (Thailand) Co.,Ltd.</div>
            </div>
            <hr className="pkg-hr" />

            {/* ── Product info ──────────────────────────── */}
            <div className="pkg-row"><span className="pkg-label-text">ชื่อสินค้า</span><span className="pkg-value">{productName}</span></div>
            <div className="pkg-row"><span className="pkg-label-text">รุ่น</span><span className="pkg-value">{displayVariant}</span></div>
            <div className="pkg-row"><span className="pkg-label-text">รหัสลูกค้า</span><span className="pkg-value">{displayCust}</span></div>
            <div className="pkg-row"><span className="pkg-label-text">จำนวนบรรจุ</span><span className="pkg-value">{qtyPerBox} ถุง / กล่อง</span></div>

            <hr className="pkg-hr" />

            {/* ── Box number ────────────────────────────── */}
            <div className="pkg-box-row">
                <span className="pkg-label-text pkg-box-label">กล่องที่</span>
                <span className="pkg-box-no">{boxNoStr}</span>
            </div>

            {/* ── Lots ─────────────────────────────────── */}
            <div className="pkg-lots">
                <div><span className="pkg-label-text">Work Order</span><span className="pkg-lot">{parentLotNumber}</span></div>
                <div><span className="pkg-label-text">Production Lot</span><span className="pkg-lot">{lotNumber}</span></div>
            </div>

            <hr className="pkg-hr" />

            {/* ── Machine / Operator / Code ─────────────── */}
            <div className="pkg-row"><span className="pkg-label-text">เครื่องที่</span><span className="pkg-value">{machineName}</span></div>
            <div className="pkg-row"><span className="pkg-label-text">ผู้บรรจุ</span><span className="pkg-value">{operatorName}</span></div>
            <div className="pkg-row"><span className="pkg-label-text">Code (TST)</span><span className="pkg-value pkg-code">{productCode}</span></div>

            <hr className="pkg-hr" />

            {/* ── Barcode ───────────────────────────────── */}
            <div className="pkg-barcode-wrap">
                <svg ref={barcodeRef} />
            </div>
        </div>
    );
};

export default PackagingLabelPrint;
