const PKG_CSS_BODY = `
* { box-sizing: border-box; }
body { margin:0; padding:0; font-family:'Angsana New','TH SarabunPSK',Arial,sans-serif; }

/* ══════════════════════════════════════════════════════
   EXPORT LABEL — 4" × 8" (original)
══════════════════════════════════════════════════════ */
.pkg-label { font-size:8pt; width:4in; padding:2mm; color:#000; line-height:1.2; }

.pkg-logo-wrap {
    width:100%; height:9mm; overflow:hidden;
    display:flex; align-items:center; justify-content:center;
    margin-bottom:1mm;
}
.pkg-logo { max-width:100%; max-height:100%; width:auto; height:auto; object-fit:contain; }

.pkg-hr { border:none; border-top:1px solid #555; margin:1.5px 0; }

.pkg-body { display:flex; gap:2mm; align-items:stretch; margin-top:1.5mm; }

.pkg-col-makok { flex:0 0 20mm; display:flex; flex-direction:column; }
.pkg-makok-icon-solo { display:block; width:100%; height:auto; object-fit:contain; flex-shrink:0; }
.pkg-makok-empty-box { flex:1; min-height:0; border:1px solid #555; box-sizing:border-box; }

.pkg-col-data { flex:1 1 0; min-width:0; display:flex; flex-direction:column; }

.pkg-table { width:100%; border-collapse:collapse; border:1px solid #888; }
.pkg-table tr:not(:last-child) td { border-bottom:0.5px solid #bbb; }

.pkg-fl {
    font-size:6pt; color:#444; white-space:nowrap;
    width:40%; border-right:0.5px solid #bbb;
    padding:0.6mm 1mm; vertical-align:middle;
}
.pkg-fl-big { font-size:8pt; font-weight:700; color:#000; }
.pkg-fl-sm {
    font-size:6pt; color:#444; white-space:nowrap;
    border-left:0.5px solid #bbb; border-right:0.5px solid #bbb;
    padding:0.6mm 0.8mm; vertical-align:middle;
}
.pkg-fv {
    font-size:7.5pt; padding:0.6mm 1mm;
    vertical-align:middle; overflow:hidden;
    text-overflow:ellipsis; white-space:nowrap;
}
.pkg-fv-bold  { font-weight:700; }
.pkg-fv-mono  { font-family:monospace; font-size:7pt; }
.pkg-fv-small { font-size:6pt; white-space:normal; word-break:break-all; line-height:1.15; }
.pkg-fv-boxno { font-size:13pt; font-weight:900; font-family:monospace; line-height:1; }

.pkg-barcode-wrap {
    width:100%; display:flex; justify-content:center; align-items:center;
    padding:0.5mm 0;
}
.pkg-barcode-wrap svg { width:100%; height:14mm; display:block; }

.pkg-export-section {
    border: 1px solid #555;
    margin-top: 2mm;
    padding: 1.5mm 2mm;
    font-family: Arial, sans-serif;
    font-size: 8pt;
}
.pkg-export-title {
    text-align: center;
    font-weight: 900;
    font-size: 12pt;
    letter-spacing: 3px;
    margin-bottom: 0.5mm;
}
.pkg-export-subtitle {
    text-align: center;
    font-size: 7.5pt;
    margin-bottom: 2mm;
}
.pkg-export-table { width:100%; border-collapse:collapse; }
.pkg-exp-no    { width:5mm; font-weight:700; vertical-align:top; padding:0.5mm; white-space:nowrap; }
.pkg-exp-label { width:27mm; vertical-align:top; padding:0.5mm; white-space:nowrap; }
.pkg-exp-sep   { width:3mm; vertical-align:top; padding:0.5mm 0; }
.pkg-exp-val   { vertical-align:top; padding:0.5mm; font-weight:700; }

/* ══════════════════════════════════════════════════════
   LOCAL LABEL — 80 mm × 85 mm  (QP-PD-002-F063)
══════════════════════════════════════════════════════ */
.pkg-label-local {
    width: 80mm;
    padding: 0;
    color: #000;
    font-family: 'Angsana New','TH SarabunPSK',Arial,sans-serif;
    font-size: 9pt;
    line-height: 1.2;
}

/* Doc ref lines */
.pkg-qp-docref-top {
    text-align: right;
    font-size: 6.5pt;
    font-weight: bold;
    font-family: Arial, sans-serif;
    margin-bottom: 0.5mm;
}
.pkg-qp-docref-bot {
    text-align: right;
    font-size: 6.5pt;
    font-family: Arial, sans-serif;
    margin-top: 0.5mm;
}

/* Outer table */
.pkg-qp-table {
    width: 100%;
    border-collapse: collapse;
    border: 1.5px solid #000;
    table-layout: fixed;
}

/* Header cell */
.pkg-qp-header-cell {
    padding: 1.2mm 2mm;
    border-bottom: 1px solid #000;
    text-align: center;
}
.pkg-qp-logo {
    max-width: 100%;
    max-height: 11mm;
    width: auto;
    height: auto;
    object-fit: contain;
    display: block;
    margin: 0 auto;
}

/* Field columns */
.pkg-qp-fl {
    width: 29mm;
    padding: 0.9mm 1.5mm;
    border-right: 0.8px solid #555;
    border-bottom: 0.5px solid #bbb;
    vertical-align: middle;
    font-size: 8.5pt;
    white-space: nowrap;
    font-weight: 600;
}
.pkg-qp-sep {
    width: 3mm;
    padding: 0.9mm 0.4mm;
    border-right: 0.5px solid #bbb;
    border-bottom: 0.5px solid #bbb;
    vertical-align: middle;
    text-align: center;
}
.pkg-qp-fv {
    padding: 0.9mm 1.5mm;
    border-bottom: 0.5px solid #bbb;
    vertical-align: middle;
    font-size: 8.5pt;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.pkg-qp-fv-large {
    font-size: 12pt;
    font-weight: 900;
    text-align: center;
}
.pkg-qp-fv-boxno {
    font-size: 14pt;
    font-weight: 900;
    text-align: center;
    font-family: monospace;
}
.pkg-qp-fv-bold { font-weight: 700; }
.pkg-qp-fv-mono { font-family: monospace; font-size: 8pt; }
.pkg-qp-fv-small { font-size: 7pt; white-space: normal; word-break: break-all; line-height: 1.1; }

/* Date + Shift row */
.pkg-qp-shift {
    border-bottom: 0.5px solid #bbb;
    padding: 0.9mm 1mm;
    vertical-align: middle;
    white-space: nowrap;
}
.pkg-qp-shift-label {
    font-size: 7pt;
    color: #444;
    border: 0.5px solid #999;
    padding: 0 1.5mm;
    margin-right: 1.5mm;
}
.pkg-qp-shift-val {
    font-size: 9pt;
    font-weight: 700;
}

/* Bottom row */
.pkg-qp-bot-qr {
    width: 22mm;
    padding: 1.5mm;
    vertical-align: middle;
    text-align: center;
    border-right: 0.5px solid #999;
    border-top: 0.8px solid #000;
}
.pkg-qp-qr-img {
    width: 19mm;
    height: 19mm;
    display: block;
    margin: 0 auto;
}
.pkg-qp-qr-placeholder {
    width: 19mm;
    height: 19mm;
    border: 1px dashed #aaa;
}

.pkg-qp-bot-mog {
    width: 35mm;
    padding: 1.2mm 1.5mm;
    vertical-align: top;
    border-right: 0.5px solid #999;
    border-top: 0.8px solid #000;
}
.pkg-mog-icons {
    display: flex;
    align-items: center;
    gap: 1mm;
    margin-bottom: 0.8mm;
}
.pkg-mog-icon-sm {
    height: 6mm;
    width: auto;
    object-fit: contain;
}
.pkg-mog-text {
    font-size: 5.2pt;
    line-height: 1.3;
    color: #111;
}
.pkg-mog-nomicro {
    font-size: 5pt;
    font-weight: 700;
    display: block;
    margin-top: 0.5mm;
}

.pkg-qp-bot-empty {
    vertical-align: top;
    border-top: 0.8px solid #000;
}
`;

/**
 * Print the packaging label by opening a clean popup window.
 * - LOCAL labels (pkg-label-local): 80mm × 85mm
 * - EXPORT labels (pkg-export-section present): 4" × 8"
 */
export function printPackagingLabel() {
    const labelEl = document.querySelector('.pkg-label, .pkg-label-local');
    if (!labelEl) return;

    const isLocal  = labelEl.classList.contains('pkg-label-local');
    const isExport = !isLocal && !!labelEl.querySelector('.pkg-export-section');

    let pageSize, pageW, pageH, popupW, popupH;
    if (isLocal) {
        pageSize = '80mm 85mm';
        pageW    = '80mm';
        pageH    = '85mm';
        popupW   = 340;
        popupH   = 360;
    } else if (isExport) {
        pageSize = '4in 8in';
        pageW    = '4in';
        pageH    = '8in';
        popupW   = 460;
        popupH   = 880;
    } else {
        pageSize = '4in 4in';
        pageW    = '4in';
        pageH    = '4in';
        popupW   = 460;
        popupH   = 480;
    }

    const pw = window.open('', '_blank', `width=${popupW},height=${popupH}`);
    if (!pw) {
        alert('กรุณาอนุญาต popup window สำหรับการพิมพ์ Label');
        return;
    }

    const pageCSS = `
        @page { size: ${pageSize}; margin: 0; }
        html, body { width: ${pageW}; height: ${pageH}; margin: 0; padding: 0; overflow: hidden; }
    `;
    pw.document.head.innerHTML = `<meta charset="utf-8"><style>${pageCSS}${PKG_CSS_BODY}</style>`;

    // Absolutify all src attributes so popup can load images
    const html = labelEl.outerHTML.replace(/\ssrc="([^"]*)"/g, (match, src) => {
        try { return ` src="${new URL(src, window.location.href).href}"`; }
        catch { return match; }
    });
    pw.document.body.innerHTML = html;

    setTimeout(() => {
        pw.focus();
        pw.print();
        setTimeout(() => pw.close(), 300);
    }, 700);
}
