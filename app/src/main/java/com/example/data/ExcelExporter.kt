package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {
    private fun escapeXml(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val code = ch.code
            if (code in 0x00..0x08 || code == 0x0B || code == 0x0C || code in 0x0E..0x1F) continue
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun sanitizeUserText(raw: String): String {
        val cleaned = raw.replace("\r", " ").replace("\n", " ").trim()
        if (cleaned.isEmpty()) return "-"
        return if (cleaned != "-" && cleaned[0] in listOf('=', '+', '-', '@')) "'$cleaned" else cleaned
    }

    private fun inlineStrCell(ref: String, styleIdx: Int, value: String): String =
        """<c r="$ref" s="$styleIdx" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>"""

    private fun numberCell(ref: String, styleIdx: Int, value: Int): String =
        """<c r="$ref" s="$styleIdx" t="n"><v>$value</v></c>"""

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><color rgb="FF111827"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF006100"/><name val="Calibri"/></font>
    <font><sz val="11"/><color rgb="FF595959"/><name val="Calibri"/></font>
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
      <left/><right/>
      <top style="thin"><color rgb="FF1F4E78"/></top>
      <bottom style="medium"><color rgb="FF1F4E78"/></bottom>
      <diagonal/>
    </border>
    <border>
      <left/><right/>
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
</styleSheet>"""

    private fun buildWorksheetXml(entries: List<LeaderboardEntry>): String {
        val headers = listOf(
            "الترتيب",
            "الاسم بالكامل",
            "رقم الهاتف",
            "النقاط المعتمدة",
            "نقاط البونص اليدوية",
            "نقاط قيد المراجعة",
            "عدد المهام المعتمدة",
            "الأهلية للجائزة"
        )
        val cols = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        val lastRow = maxOf(entries.size + 1, 2)

        val rowsSb = StringBuilder()
        rowsSb.append("""<row r="1" ht="34" customHeight="1">""")
        headers.forEachIndexed { idx, title ->
            rowsSb.append(inlineStrCell("${cols[idx]}1", 1, title))
        }
        rowsSb.append("</row>\n")

        entries.forEachIndexed { idx, item ->
            val rowNum = idx + 2
            val isOddDataRow = idx % 2 == 0
            val textStyle = if (isOddDataRow) 2 else 4
            val numStyle = if (isOddDataRow) 3 else 5
            val isEligible = item.rank in 1..FastCalendar.TOP_PRIZE_RANKS && item.totalApprovedPoints > 0
            val prizeLabel = if (isEligible) "مؤهل للجائزة" else "مشارك"
            val prizeStyle = if (isEligible) 6 else 7

            rowsSb.append("""<row r="$rowNum" ht="24" customHeight="1">""")
            rowsSb.append(numberCell("A$rowNum", numStyle, item.rank))
            rowsSb.append(inlineStrCell("B$rowNum", textStyle, sanitizeUserText(item.user.fullName)))
            rowsSb.append(inlineStrCell("C$rowNum", textStyle, sanitizeUserText(item.user.phone)))
            rowsSb.append(numberCell("D$rowNum", numStyle, item.totalApprovedPoints))
            rowsSb.append(numberCell("E$rowNum", numStyle, item.user.bonusPoints))
            rowsSb.append(numberCell("F$rowNum", numStyle, item.pendingPoints))
            rowsSb.append(numberCell("G$rowNum", numStyle, item.approvedTasksCount))
            rowsSb.append(inlineStrCell("H$rowNum", prizeStyle, prizeLabel))
            rowsSb.append("</row>\n")
        }

        val dataBarRange = "D2:D$lastRow"
        val cfXml = if (entries.isNotEmpty()) """
  <conditionalFormatting sqref="$dataBarRange">
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
  </conditionalFormatting>""" else ""

        val extLstXml = if (entries.isNotEmpty()) """
  <extLst>
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
          <xm:sqref>$dataBarRange</xm:sqref>
        </x14:conditionalFormatting>
      </x14:conditionalFormattings>
    </ext>
  </extLst>""" else ""

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" mc:Ignorable="x14ac" xmlns:x14ac="http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac">
  <dimension ref="A1:H$lastRow"/>
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
    $rowsSb
  </sheetData>
  <autoFilter ref="A1:H$lastRow"/>$cfXml
  <pageMargins left="0.5" right="0.5" top="0.75" bottom="0.75" header="0.3" footer="0.3"/>$extLstXml
</worksheet>"""
    }

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="لوحة الأبطال" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    fun buildLeaderboardXlsxBytes(entries: List<LeaderboardEntry>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val files = listOf(
                "[Content_Types].xml" to CONTENT_TYPES_XML,
                "_rels/.rels" to ROOT_RELS_XML,
                "xl/workbook.xml" to WORKBOOK_XML,
                "xl/_rels/workbook.xml.rels" to WORKBOOK_RELS_XML,
                "xl/styles.xml" to buildStylesXml(),
                "xl/worksheets/sheet1.xml" to buildWorksheetXml(entries)
            )
            for ((path, content) in files) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    fun exportAndShareExcel(context: Context, entries: List<LeaderboardEntry>): Result<String> {
        return runCatching {
            val bytes = buildLeaderboardXlsxBytes(entries)
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "tariq_almazwad_report.xlsx")
            file.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير نقاط تحدي طريق المزود - صوم الميلاد المجيد")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(intent, "تصدير تقرير النقاط المنسّق (Excel .xlsx)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            "تم إنشاء تقرير Excel المنسّق (tariq_almazwad_report.xlsx) بنجاح"
        }
    }
}
