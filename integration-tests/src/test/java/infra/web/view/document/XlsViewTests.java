/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.view.document;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.view.View;

/**
 * Tests for AbstractXlsView and its subclasses.
 *
 * @author Juergen Hoeller
 */
public class XlsViewTests {

  private final StaticWebApplicationContext wac = new StaticWebApplicationContext();
  private final MockContextImpl sc = new MockContextImpl();
  private final HttpMockRequestImpl request = new HttpMockRequestImpl(this.sc);
  private final MockHttpResponseImpl response = new MockHttpResponseImpl();
  RequestContext requestContext = new MockRequestContext(wac, request, response);

  @Test
  @SuppressWarnings("resource")
  public void testXls() throws Exception {
    View excelView = new AbstractXlsView() {
      @Override
      protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, RequestContext request) throws Exception {
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Test Value");
      }
    };

    excelView.render(new HashMap<>(), requestContext);

    Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
    Assertions.assertThat(wb.getSheetName(0)).isEqualTo("Test Sheet");
    Sheet sheet = wb.getSheet("Test Sheet");
    Row row = sheet.getRow(0);
    Cell cell = row.getCell(0);
    Assertions.assertThat(cell.getStringCellValue()).isEqualTo("Test Value");
  }

  @Test
  @SuppressWarnings("resource")
  public void testXlsxView() throws Exception {
    View excelView = new AbstractXlsxView() {
      @Override
      protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
              RequestContext request) throws Exception {
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Test Value");
      }
    };

    excelView.render(new HashMap<>(), requestContext);

    Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
    Assertions.assertThat(wb.getSheetName(0)).isEqualTo("Test Sheet");
    Sheet sheet = wb.getSheet("Test Sheet");
    Row row = sheet.getRow(0);
    Cell cell = row.getCell(0);
    Assertions.assertThat(cell.getStringCellValue()).isEqualTo("Test Value");
  }

  @Test
  @SuppressWarnings("resource")
  public void testXlsxStreamingView() throws Exception {
    View excelView = new AbstractXlsxStreamingView() {
      @Override
      protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
              RequestContext request) throws Exception {
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Test Value");
      }
    };

    excelView.render(new HashMap<>(), requestContext);

    Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
    Assertions.assertThat(wb.getSheetName(0)).isEqualTo("Test Sheet");
    Sheet sheet = wb.getSheet("Test Sheet");
    Row row = sheet.getRow(0);
    Cell cell = row.getCell(0);
    Assertions.assertThat(cell.getStringCellValue()).isEqualTo("Test Value");
  }

}
