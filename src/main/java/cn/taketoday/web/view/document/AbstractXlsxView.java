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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Map;

import cn.taketoday.web.RequestContext;

/**
 * Convenient superclass for Excel document views in the Office 2007 XLSX format
 * (as supported by POI-OOXML). Compatible with Apache POI 3.5 and higher.
 *
 * <p>For working with the workbook in subclasses, see
 * <a href="https://poi.apache.org">Apache's POI site</a>.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractXlsxView extends AbstractXlsView {

  /**
   * Default Constructor.
   * <p>Sets the content type of the view to
   * {@code "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}.
   */
  public AbstractXlsxView() {
    setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }

  /**
   * This implementation creates an {@link XSSFWorkbook} for the XLSX format.
   */
  @Override
  protected Workbook createWorkbook(Map<String, Object> model, RequestContext request) {
    return new XSSFWorkbook();
  }

}
