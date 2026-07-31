const PKG_CSS = `
@page { size: 4in 4in; margin: 3mm; }
* { box-sizing: border-box; }
body { margin:0; padding:0; font-family:'Angsana New','TH SarabunPSK',Arial,sans-serif; }

.pkg-label { font-size:8pt; width:90mm; padding:2mm; color:#000; line-height:1.2; }

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
`;

/**
 * Print the packaging label by opening a clean popup window.
 * The popup contains only the label HTML + inline CSS — no dialog, no portal conflicts.
 */
export function printPackagingLabel() {
    const labelEl = document.querySelector('.pkg-label');
    if (!labelEl) return;

    const pw = window.open('', '_blank', 'width=460,height=480');
    if (!pw) {
        alert('กรุณาอนุญาต popup window สำหรับการพิมพ์ Label');
        return;
    }

    pw.document.head.innerHTML = `<meta charset="utf-8"><style>${PKG_CSS}</style>`;
    pw.document.body.innerHTML = labelEl.outerHTML;

    // Wait briefly for images (tst-logo.png, MOG.png) to load before printing
    setTimeout(() => {
        pw.focus();
        pw.print();
        setTimeout(() => pw.close(), 300);
    }, 700);
}
