/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
