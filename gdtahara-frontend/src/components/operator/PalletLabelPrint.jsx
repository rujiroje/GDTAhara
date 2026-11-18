import React from 'react';
import { Box, Button, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PrintIcon from '@mui/icons-material/Print';

/**
 * Pallet label — QP-PD-002-F046 Rev.02
 * Paper: A5 landscape (210 × 148 mm), 5 mm margins
 *
 * Layout matches the physical Pallet Tag form:
 *   Header : [Company name] | [CODE / PALLET / LOT DATE: BOX NO.]
 *   Matrix : LOT NO. column + 8 BOX columns (3-digit box numbers)
 *   Footer : QA | SHIFT PD (DAY / NIGHT) | จำนวน + DATE
 *   Bottom : form code right-aligned
 */

const COLS = 8;

const toDisplayDate = (iso) => {
  if (!iso) return '——';
  const [y, m, d] = String(iso).split('-');
  return `${d}/${m}/${y.slice(2)}`;
};

const PalletLabelPrint = ({ labelData, onBack }) => {
  if (!labelData) return null;

  const {
    palletNumber = '',
    productCode = '',
    productName = '',
    boxCount = 0,
    closeDate,
    lotBoxMatrix = {},
  } = labelData;

  // Build table rows: each LOT date can overflow into multiple rows of COLS boxes
  const tableRows = [];
  for (const [lotDate, boxes] of Object.entries(lotBoxMatrix)) {
    const chunks = [];
    for (let i = 0; i < boxes.length; i += COLS) chunks.push(boxes.slice(i, i + COLS));
    if (chunks.length === 0) chunks.push([]);
    chunks.forEach((chunk, ci) => {
      tableRows.push({ lotDate, chunk, isFirst: ci === 0, rowSpan: chunks.length });
    });
  }

  return (
    <>
      {/* ── Screen controls ── */}
      <Stack direction="row" spacing={2} mb={2} className="no-print" alignItems="center">
        <Button startIcon={<ArrowBackIcon />} onClick={onBack} variant="outlined" size="small">
          กลับ
        </Button>
        <Button startIcon={<PrintIcon />} variant="contained" size="small" onClick={() => window.print()}>
          พิมพ์ Label
        </Button>
        <Typography variant="caption" color="text.secondary">
          กระดาษ A5 แนวนอน (Landscape) — ขอบ 5 mm
        </Typography>
      </Stack>

      <style>{`
        @media print {
          html, body { margin: 0; padding: 0; }
          body * { visibility: hidden !important; }
          #pallet-label-root, #pallet-label-root * { visibility: visible !important; }
          #pallet-label-root { position: fixed; inset: 0; }
          .no-print { display: none !important; }
          @page { size: A5 landscape; margin: 5mm; }
        }
      `}</style>

      {/* ════════════════════ LABEL ════════════════════ */}
      <Box id="pallet-label-root" sx={{
        width: '200mm',
        minHeight: '138mm',
        border: '2px solid #000',
        fontFamily: '"Arial", "Helvetica", sans-serif',
        fontSize: '9pt',
        bgcolor: '#fff',
        color: '#000',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
      }}>

        {/* ══ HEADER ══ */}
        <Box sx={{ display: 'flex', borderBottom: '2px solid #000' }}>

          {/* Left: company name */}
          <Box sx={{
            flex: 1,
            borderRight: '1.5px solid #000',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            p: '2mm 3mm',
            gap: '1mm',
          }}>
            <Typography sx={{ fontSize: '10.5pt', fontWeight: 900, letterSpacing: '0.3px', lineHeight: 1.2, whiteSpace: 'nowrap' }}>
              Toyo Seikan (Thailand) Co.,Ltd.
            </Typography>
            {productName && (
              <Typography sx={{ fontSize: '7.5pt', color: '#555', mt: '1mm' }}>
                {productName}
              </Typography>
            )}
          </Box>

          {/* Right: CODE / PALLET / column labels */}
          <Box sx={{ width: '68mm', display: 'flex', flexDirection: 'column' }}>

            {/* CODE row */}
            <Box sx={{
              borderBottom: '1px solid #000',
              px: '2mm', py: '1mm',
              display: 'flex', alignItems: 'center', gap: '2mm',
            }}>
              <Typography sx={{ fontSize: '7.5pt', fontWeight: 700, whiteSpace: 'nowrap' }}>
                CODE :
              </Typography>
              <Typography sx={{ fontSize: '10pt', fontWeight: 800 }}>
                {productCode}
              </Typography>
            </Box>

            {/* PALLET number row */}
            <Box sx={{
              borderBottom: '1px solid #000',
              px: '2mm', py: '0.5mm',
              display: 'flex', alignItems: 'center', gap: '2mm',
            }}>
              <Typography sx={{ fontSize: '7.5pt', fontWeight: 700, whiteSpace: 'nowrap' }}>
                PALLET :
              </Typography>
              <Typography sx={{ fontSize: '22pt', fontWeight: 900, lineHeight: 1.1 }}>
                {palletNumber}
              </Typography>
            </Box>

            {/* LOT DATE / BOX NO. column label sub-row */}
            <Box sx={{ display: 'flex', flex: 1, alignItems: 'center' }}>
              <Box sx={{
                flex: 1, px: '2mm', py: '1mm',
                borderRight: '1px dashed #999',
              }}>
                <Typography sx={{ fontSize: '7pt', fontWeight: 700 }}>LOT DATE :</Typography>
              </Box>
              <Box sx={{ flex: 1.5, px: '2mm', py: '1mm' }}>
                <Typography sx={{ fontSize: '7pt', fontWeight: 700 }}>BOX NO.</Typography>
              </Box>
            </Box>
          </Box>
        </Box>

        {/* ══ MATRIX TABLE ══ */}
        <Box sx={{ flex: 1, borderBottom: '1.5px solid #000', overflowX: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
            <colgroup>
              <col style={{ width: '18mm' }} />
              {Array.from({ length: COLS }, (_, i) => (
                <col key={i} style={{ width: `calc((100% - 18mm) / ${COLS})` }} />
              ))}
            </colgroup>
            <thead>
              <tr>
                <th style={TH}>LOT NO.</th>
                {Array.from({ length: COLS }, (_, i) => (
                  <th key={i} style={TH}>{i + 1}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {tableRows.length === 0 ? (
                <tr>
                  <td colSpan={COLS + 1} style={{ ...TD, textAlign: 'center', color: '#aaa', height: '12mm' }}>
                    — ไม่มีข้อมูลกล่อง —
                  </td>
                </tr>
              ) : (
                tableRows.map((row, idx) => (
                  <tr key={idx}>
                    {row.isFirst && (
                      <td rowSpan={row.rowSpan} style={{
                        ...TD,
                        textAlign: 'center',
                        fontWeight: 800,
                        fontSize: '8.5pt',
                        verticalAlign: 'middle',
                        borderRight: '2px solid #000',
                        backgroundColor: '#f4f4f4',
                      }}>
                        {row.lotDate}
                      </td>
                    )}
                    {Array.from({ length: COLS }, (_, ci) => (
                      <td key={ci} style={{ ...TD, textAlign: 'center', fontSize: '9pt', fontWeight: row.chunk[ci] ? 700 : 400 }}>
                        {row.chunk[ci] || ''}
                      </td>
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </Box>

        {/* ══ FOOTER ══ */}
        <Box sx={{ display: 'flex', minHeight: '22mm' }}>

          {/* QA */}
          <Box sx={{
            flex: 1,
            borderRight: '1.5px solid #000',
            p: '1mm 2mm',
          }}>
            <Typography sx={{ fontSize: '7.5pt', fontWeight: 700 }}>QA</Typography>
          </Box>

          {/* SHIFT PD — DAY / NIGHT */}
          <Box sx={{
            flex: 2,
            borderRight: '1.5px solid #000',
            display: 'flex',
            flexDirection: 'column',
          }}>
            <Box sx={{ borderBottom: '1px solid #000', px: '2mm', py: '0.5mm' }}>
              <Typography sx={{ fontSize: '7.5pt', fontWeight: 700, textAlign: 'center' }}>
                SHIFT PD
              </Typography>
            </Box>
            <Box sx={{ flex: 1, display: 'flex' }}>
              <Box sx={{
                flex: 1,
                borderRight: '1px solid #000',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                pt: '1mm',
              }}>
                <Typography sx={{ fontSize: '7pt', fontWeight: 600 }}>DAY</Typography>
              </Box>
              <Box sx={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                pt: '1mm',
              }}>
                <Typography sx={{ fontSize: '7pt', fontWeight: 600 }}>NIGHT</Typography>
              </Box>
            </Box>
          </Box>

          {/* จำนวน + DATE */}
          <Box sx={{
            flex: 1.5,
            p: '1.5mm 2mm',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}>
            <Box>
              <Typography sx={{ fontSize: '7pt', color: '#666' }}>จำนวน (กล่อง)</Typography>
              <Typography sx={{ fontSize: '16pt', fontWeight: 900, lineHeight: 1.1 }}>
                {boxCount}
              </Typography>
            </Box>
            <Box>
              <Typography sx={{ fontSize: '7pt', color: '#666' }}>DATE / วันที่</Typography>
              <Typography sx={{ fontSize: '9pt', fontWeight: 700 }}>
                {toDisplayDate(closeDate)}
              </Typography>
            </Box>
          </Box>
        </Box>

        {/* ══ FORM CODE (bottom strip) ══ */}
        <Box sx={{
          px: '2mm', py: '0.3mm',
          borderTop: '1px solid #ccc',
          textAlign: 'right',
        }}>
          <Typography sx={{ fontSize: '6pt', color: '#888' }}>
            &lt; QP-PD-002-F046 &gt; Rev.02
          </Typography>
        </Box>

      </Box>
    </>
  );
};

// ── style constants ───────────────────────────────────────────────

const TH = {
  border: '1.5px solid #000',
  padding: '1mm 1mm',
  backgroundColor: '#e0e0e0',
  textAlign: 'center',
  fontWeight: 700,
  fontSize: '7.5pt',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
};

const TD = {
  border: '1px solid #bbb',
  padding: '0.5mm 0.5mm',
  minHeight: '7mm',
  fontSize: '8.5pt',
  overflow: 'hidden',
  whiteSpace: 'nowrap',
};

export default PalletLabelPrint;
