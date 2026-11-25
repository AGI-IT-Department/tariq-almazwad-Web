import type { LeaderboardEntry } from '../types';
import { FastCalendar } from './fastCalendar';

interface ZipEntry {
  name: string;
  data: Uint8Array;
}

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) {
      c = (c & 1) !== 0 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[i] = c >>> 0;
  }
  return table;
})();

function crc32(bytes: Uint8Array): number {
  let crc = 0xffffffff;
  for (let i = 0; i < bytes.length; i++) {
    crc = CRC_TABLE[(crc ^ bytes[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function buildZipBytes(entries: ZipEntry[]): Uint8Array {
  const encoder = new TextEncoder();
  const localParts: Uint8Array[] = [];
  const centralParts: Uint8Array[] = [];
  let offset = 0;

  for (const entry of entries) {
    const nameBytes = encoder.encode(entry.name);
    const data = entry.data;
    const checksum = crc32(data);
    const size = data.length;

    // Local File Header (30 bytes + filename)
    const localHeader = new Uint8Array(30 + nameBytes.length);
    const lv = new DataView(localHeader.buffer);
    lv.setUint32(0, 0x04034b50, true); // signature
    lv.setUint16(4, 20, true);         // version needed (2.0)
    lv.setUint16(6, 0x0800, true);     // UTF-8 flag
    lv.setUint16(8, 0, true);          // method 0 (STORE)
    lv.setUint16(10, 0, true);         // mod time
    lv.setUint16(12, 0x5821, true);    // mod date
    lv.setUint32(14, checksum, true);  // crc32
    lv.setUint32(18, size, true);      // compressed size
    lv.setUint32(22, size, true);      // uncompressed size
    lv.setUint16(26, nameBytes.length, true);
    lv.setUint16(28, 0, true);         // extra length
    localHeader.set(nameBytes, 30);

    localParts.push(localHeader, data);

    // Central Directory Header (46 bytes + filename)
    const centralHeader = new Uint8Array(46 + nameBytes.length);
    const cv = new DataView(centralHeader.buffer);
    cv.setUint32(0, 0x02014b50, true); // signature
    cv.setUint16(4, 20, true);         // version made by
    cv.setUint16(6, 20, true);         // version needed
    cv.setUint16(8, 0x0800, true);     // UTF-8 flag
    cv.setUint16(10, 0, true);         // method 0 (STORE)
    cv.setUint16(12, 0, true);         // mod time
    cv.setUint16(14, 0x5821, true);    // mod date
    cv.setUint32(16, checksum, true);
    cv.setUint32(20, size, true);
    cv.setUint32(24, size, true);
    cv.setUint16(28, nameBytes.length, true);
    cv.setUint16(30, 0, true);         // extra length
    cv.setUint16(32, 0, true);         // comment length
    cv.setUint16(34, 0, true);         // disk number
    cv.setUint16(36, 0, true);         // internal attrs
    cv.setUint32(38, 0, true);         // external attrs
    cv.setUint32(42, offset, true);    // relative offset of local header
    centralHeader.set(nameBytes, 46);

    centralParts.push(centralHeader);
    offset += localHeader.length + data.length;
  }

  const centralSize = centralParts.reduce((sum, p) => sum + p.length, 0);
  const eocd = new Uint8Array(22);
  const ev = new DataView(eocd.buffer);
  ev.setUint32(0, 0x06054b50, true);
  ev.setUint16(4, 0, true);
  ev.setUint16(6, 0, true);
  ev.setUint16(8, entries.length, true);
  ev.setUint16(10, entries.length, true);
  ev.setUint32(12, centralSize, true);
  ev.setUint32(16, offset, true);
  ev.setUint16(20, 0, true);

  const totalLength = offset + centralSize + eocd.length;
  const output = new Uint8Array(totalLength);
  let pos = 0;
  for (const part of localParts) {
    output.set(part, pos);
    pos += part.length;
  }
  for (const part of centralParts) {
    output.set(part, pos);
    pos += part.length;
  }
  output.set(eocd, pos);
  return output;
}

function escapeXml(raw: string): string {
  // Strip illegal XML 1.0 control chars and escape XML entities
  return raw
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function sanitizeUserText(raw: string): string {
  const cleaned = (raw || '').replace(/[\r\n]+/g, ' ').trim();
  if (!cleaned) return '-';
  if (['=', '+', '-', '@'].includes(cleaned[0]) && cleaned !== '-') {
    return `'${cleaned}`;
  }
  return cleaned;
}

function inlineStrCell(ref: string, styleIdx: number, value: string): string {
  return `<c r="${ref}" s="${styleIdx}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>`;
}

function numberCell(ref: string, styleIdx: number, value: number): string {
  const safeVal = Number.isFinite(value) ? value : 0;
  return `<c r="${ref}" s="${styleIdx}" t="n"><v>${safeVal}</v></c>`;
}

/**
 * Style indices in xl/styles.xml:
 * 0: Default
 * 1: Header row (Navy-Teal #1F4E78 fill, White bold 12pt, centered)
 * 2: Odd row text (Soft Ice-Blue #DDEBF7 fill, dark ink 11pt, blue horizontal borders)
 * 3: Odd row integer / formatted number (#,##0, Soft Ice-Blue #DDEBF7 fill)
 * 4: Even row text (White #FFFFFF fill, dark ink 11pt, blue horizontal borders)
 * 5: Even row integer / formatted number (#,##0, White #FFFFFF fill)
 * 6: Prize Eligible badge (Soft Green #C6EFCE fill, Bold Dark Green #006100)
 * 7: Regular Participant badge (Soft Gray #EDEDED fill, Muted Gray #595959)
 */
function buildStylesXml(): string {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font>
      <sz val="11"/>
      <color rgb="FF111827"/>
      <name val="Calibri"/>
    </font>
    <font>
      <b/>
      <sz val="12"/>
      <color rgb="FFFFFFFF"/>
      <name val="Calibri"/>
    </font>
    <font>
      <b/>
      <sz val="11"/>
      <color rgb="FF006100"/>
      <name val="Calibri"/>
    </font>
    <font>
      <sz val="11"/>
      <color rgb="FF595959"/>
      <name val="Calibri"/>
    </font>
  </fonts>
  <fills count="7">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1F4E78"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFDDEBF7"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFFFFF"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFC6EFCE"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFEDEDED"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="3">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left/>
      <right/>
      <top style="thin"><color rgb="FF1F4E78"/></top>
      <bottom style="medium"><color rgb="FF1F4E78"/></bottom>
      <diagonal/>
    </border>
    <border>
      <left/>
      <right/>
      <top style="thin"><color rgb="FF8EA9DB"/></top>
      <bottom style="thin"><color rgb="FF8EA9DB"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="8">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="3" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="3" fontId="0" fillId="3" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="4" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="3" fontId="0" fillId="4" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="2" fillId="5" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="3" fillId="6" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
  </cellXfs>
  <cellStyles count="1">
    <cellStyle name="Normal" xfId="0" builtinId="0"/>
  </cellStyles>
</styleSheet>`;
}

function buildWorksheetXml(entries: LeaderboardEntry[]): string {
  const headers = [
    'الترتيب',
    'الاسم بالكامل',
    'رقم الهاتف',
    'النقاط المعتمدة',
    'نقاط البونص اليدوية',
    'نقاط قيد المراجعة',
    'عدد المهام المعتمدة',
    'الأهلية للجائزة',
  ];
  const cols = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
  const lastRow = Math.max(entries.length + 1, 2);

  const rowsXml: string[] = [];

  // Row 1: Styled Header
  const headerCells = headers
    .map((title, idx) => inlineStrCell(`${cols[idx]}1`, 1, title))
    .join('');
  rowsXml.push(`<row r="1" ht="34" customHeight="1">${headerCells}</row>`);

  // Rows 2..N: Zebra-striped Participant Rows
  entries.forEach((item, idx) => {
    const rowNum = idx + 2;
    const isOddDataRow = idx % 2 === 0;
    const textStyle = isOddDataRow ? 2 : 4;
    const numStyle = isOddDataRow ? 3 : 5;
    const isEligible =
      item.rank >= 1 &&
      item.rank <= FastCalendar.TOP_PRIZE_RANKS &&
      item.totalApprovedPoints > 0;
    const prizeLabel = isEligible ? 'مؤهل للجائزة' : 'مشارك';
    const prizeStyle = isEligible ? 6 : 7;

    const cells = [
      numberCell(`A${rowNum}`, numStyle, item.rank),
      inlineStrCell(`B${rowNum}`, textStyle, sanitizeUserText(item.user.fullName)),
      inlineStrCell(`C${rowNum}`, textStyle, sanitizeUserText(item.user.phone)),
      numberCell(`D${rowNum}`, numStyle, item.totalApprovedPoints),
      numberCell(`E${rowNum}`, numStyle, item.user.bonusPoints),
      numberCell(`F${rowNum}`, numStyle, item.pendingPoints),
      numberCell(`G${rowNum}`, numStyle, item.approvedTasksCount),
      inlineStrCell(`H${rowNum}`, prizeStyle, prizeLabel),
    ].join('');

    rowsXml.push(`<row r="${rowNum}" ht="24" customHeight="1">${cells}</row>`);
  });

  const dataBarRange = `D2:D${lastRow}`;
  const cfXml =
    entries.length > 0
      ? `<conditionalFormatting sqref="${dataBarRange}">
    <cfRule type="dataBar" priority="1">
      <dataBar>
        <cfvo type="min"/>
        <cfvo type="max"/>
        <color rgb="FF638EC6"/>
      </dataBar>
      <extLst>
        <ext uri="{B025F937-C7B1-47D3-B67F-A62EFF666E3E}" xmlns:x14="http://schemas.microsoft.com/office/spreadsheetml/2009/9/main">
          <x14:id>{A1B2C3D4-5678-4A9B-8C7D-202611300001}</x14:id>
        </ext>
      </extLst>
    </cfRule>
  </conditionalFormatting>`
      : '';

  const extLstXml =
    entries.length > 0
      ? `<extLst>
    <ext uri="{78C0D931-6437-407d-A8EE-F0AAD7539E65}" xmlns:x14="http://schemas.microsoft.com/office/spreadsheetml/2009/9/main">
      <x14:conditionalFormattings>
        <x14:conditionalFormatting xmlns:xm="http://schemas.microsoft.com/office/excel/2006/main">
          <x14:cfRule type="dataBar" id="{A1B2C3D4-5678-4A9B-8C7D-202611300001}">
            <x14:dataBar minLength="0" maxLength="100" gradient="1">
              <x14:cfvo type="autoMin"/>
              <x14:cfvo type="autoMax"/>
              <x14:fillColor rgb="FF638EC6"/>
            </x14:dataBar>
          </x14:cfRule>
          <xm:sqref>${dataBarRange}</xm:sqref>
        </x14:conditionalFormatting>
      </x14:conditionalFormattings>
    </ext>
  </extLst>`
      : '';

  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" mc:Ignorable="x14ac" xmlns:x14ac="http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac">
  <dimension ref="A1:H${lastRow}"/>
  <sheetViews>
    <sheetView tabSelected="1" rightToLeft="1" workbookViewId="0">
      <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="24"/>
  <cols>
    <col min="1" max="1" width="12" customWidth="1"/>
    <col min="2" max="2" width="28" customWidth="1"/>
    <col min="3" max="3" width="20" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="22" customWidth="1"/>
    <col min="6" max="6" width="20" customWidth="1"/>
    <col min="7" max="7" width="22" customWidth="1"/>
    <col min="8" max="8" width="20" customWidth="1"/>
  </cols>
  <sheetData>
    ${rowsXml.join('\n    ')}
  </sheetData>
  <autoFilter ref="A1:H${lastRow}"/>
  ${cfXml}
  <pageMargins left="0.5" right="0.5" top="0.75" bottom="0.75" header="0.3" footer="0.3"/>
  ${extLstXml}
</worksheet>`;
}

const CONTENT_TYPES_XML = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>`;

const ROOT_RELS_XML = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>`;

const WORKBOOK_XML = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="لوحة الأبطال" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>`;

const WORKBOOK_RELS_XML = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>`;

export const ExcelExporter = {
  buildLeaderboardXlsxBytes(entries: LeaderboardEntry[]): Uint8Array {
    const encoder = new TextEncoder();
    return buildZipBytes([
      { name: '[Content_Types].xml', data: encoder.encode(CONTENT_TYPES_XML) },
      { name: '_rels/.rels', data: encoder.encode(ROOT_RELS_XML) },
      { name: 'xl/workbook.xml', data: encoder.encode(WORKBOOK_XML) },
      { name: 'xl/_rels/workbook.xml.rels', data: encoder.encode(WORKBOOK_RELS_XML) },
      { name: 'xl/styles.xml', data: encoder.encode(buildStylesXml()) },
      { name: 'xl/worksheets/sheet1.xml', data: encoder.encode(buildWorksheetXml(entries)) },
    ]);
  },

  downloadLeaderboardExcel(entries: LeaderboardEntry[]): string {
    const bytes = this.buildLeaderboardXlsxBytes(entries);
    const copy = new Uint8Array(bytes.byteLength);
    copy.set(bytes);
    const blob = new Blob([copy.buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'tariq_almazwad_report.xlsx';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return 'تم تنزيل تقرير Excel المنسّق (tariq_almazwad_report.xlsx) بنجاح';
  },
};
