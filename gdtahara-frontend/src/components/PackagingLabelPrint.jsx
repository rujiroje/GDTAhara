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
    } = labelData ?? {};

    const boxNoStr   = String(boxNo).padStart(3, '0');
    const barcodeVal = `${productCode}-${lotNumber}-${boxNoStr}`;
    const custCode   = customerCode || productCode;

    // Derive date from lotNumber (yymmdd → dd/mm/25yy Buddhist)
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
        if (barcodeRef.current && barcodeVal.length > 2) {
            try {
                JsBarcode(barcodeRef.current, barcodeVal, {
                    format:       'CODE128',
                    width:        1.3,
                    height:       32,
                    displayValue: true,
                    fontSize:     8,
                    margin:       2,
                });
            } catch { /* invalid value */ }
        }
    }, [barcodeVal]);

    const rows = [
        { label: 'ชื่อสินค้า',         value: productName,     cls: 'pkg-fv-bold' },
        { label: 'รหัสลูกค้า',         value: custCode,        cls: 'pkg-fv-bold pkg-fv-mono' },
        { label: 'รหัส TST',           value: productCode,     cls: 'pkg-fv-bold pkg-fv-mono' },
        { label: 'จำนวนบรรจุ (ชิ้น)', value: qtyPerBox,       cls: 'pkg-fv-bold' },
        { label: 'Production Lot',     value: lotNumber,       cls: 'pkg-fv-bold pkg-fv-mono' },
        { label: 'WO',                 value: parentLotNumber, cls: 'pkg-fv-small' },
        { label: 'กล่องที่',            value: boxNoStr,        cls: 'pkg-fv-boxno' },
        { label: 'วันที่ผลิต',          value: prodDate,        cls: 'pkg-fv-bold' },
        { label: 'ผู้บรรจุ',            value: operatorName,    cls: 'pkg-fv-bold' },
    ];

    return (
        <div className="pkg-label">

            {/* ── Logo ──────────────────────────────────── */}
            <div className="pkg-logo-wrap">
                <img src="/tst-logo.png" alt="Toyo Seikan (Thailand) Co.,Ltd." className="pkg-logo" />
            </div>
            <hr className="pkg-hr" />

            {/* ── Barcode full-width under logo ─────────── */}
            <div className="pkg-barcode-wrap">
                <svg ref={barcodeRef} />
            </div>
            <hr className="pkg-hr" />

            {/* ── Body: มอก. left | data right ─────────── */}
            <div className="pkg-body">

                {/* Left: มอก. image */}
                <div className="pkg-col-makok">
                    <img src="/MOG.png" alt="มอก." className="pkg-makok-full" />
                </div>

                {/* Right: data table */}
                <div className="pkg-col-data">
                    <table className="pkg-table">
                        <tbody>
                            {rows.map((r, i) => (
                                <tr key={i}>
                                    <td className="pkg-fl">{r.label}</td>
                                    <td className={`pkg-fv ${r.cls}`}>{r.value}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

            </div>
        </div>
    );
};

export default PackagingLabelPrint;
