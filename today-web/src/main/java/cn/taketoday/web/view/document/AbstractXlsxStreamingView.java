/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.view.document;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.util.Map;

import cn.taketoday.web.RequestContext;

/**
 * Convenient superclass for Excel document views in the Office 2007 XLSX format,
 * using POI's streaming variant. Compatible with Apache POI 3.9 and higher.
 *
 * <p>For working with the workbook in subclasses, see
 * <a href="https://poi.apache.org">Apache's POI site</a>.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractXlsxStreamingView extends AbstractXlsxView {

  /**
   * This implementation creates a {@link SXSSFWorkbook} for streaming the XLSX format.
   */
  @Override
  protected SXSSFWorkbook createWorkbook(Map<String, Object> model, RequestContext request) {
    return new SXSSFWorkbook();
  }

  /**
   * This implementation disposes of the {@link SXSSFWorkbook} when done with rendering.
   *
   * @see org.apache.poi.xssf.streaming.SXSSFWorkbook#dispose()
   */
  @Override
  protected void renderWorkbook(Workbook workbook, RequestContext response) throws IOException {
    super.renderWorkbook(workbook, response);

    // Dispose of temporary files in case of streaming variant...
    ((SXSSFWorkbook) workbook).dispose();
  }

}
