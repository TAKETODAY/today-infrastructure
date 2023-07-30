/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.AbstractView;

/**
 * Convenient superclass for Excel document views in traditional XLS format.
 * Compatible with Apache POI 3.5 and higher.
 *
 * <p>For working with the workbook in the subclass, see
 * <a href="https://poi.apache.org">Apache's POI site</a>
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractXlsView extends AbstractView {

  /**
   * Default Constructor.
   * Sets the content type of the view to "application/vnd.ms-excel".
   */
  public AbstractXlsView() {
    setContentType("application/vnd.ms-excel");
  }

  @Override
  protected boolean generatesDownloadContent() {
    return true;
  }

  /**
   * Renders the Excel view, given the specified model.
   */
  @Override
  protected final void renderMergedOutputModel(
          Map<String, Object> model, RequestContext request) throws Exception {

    // Create a fresh workbook instance for this render step.
    Workbook workbook = createWorkbook(model, request);

    // Delegate to application-provided document code.
    buildExcelDocument(model, workbook, request);

    // Set the content type.
    request.setContentType(getContentType());

    // Flush byte array to servlet output stream.
    renderWorkbook(workbook, request);
  }

  /**
   * Template method for creating the POI {@link Workbook} instance.
   * <p>The default implementation creates a traditional {@link HSSFWorkbook}.
   * Framework-provided subclasses are overriding this for the OOXML-based variants;
   * custom subclasses may override this for reading a workbook from a file.
   *
   * @param model the model Map
   * @param request current HTTP request (for taking the URL or headers into account)
   * @return the new {@link Workbook} instance
   */
  protected Workbook createWorkbook(Map<String, Object> model, RequestContext request) {
    return new HSSFWorkbook();
  }

  /**
   * The actual render step: taking the POI {@link Workbook} and rendering
   * it to the given response.
   *
   * @param workbook the POI Workbook to render
   * @param response current HTTP response
   * @throws IOException when thrown by I/O methods that we're delegating to
   */
  protected void renderWorkbook(Workbook workbook, RequestContext response) throws IOException {
    OutputStream out = response.getOutputStream();
    workbook.write(out);
    workbook.close();
  }

  /**
   * Application-provided subclasses must implement this method to populate
   * the Excel workbook document, given the model.
   *
   * @param model the model Map
   * @param workbook the Excel workbook to populate
   * @param context in case we need locale etc. Shouldn't look at attributes.
   */
  protected abstract void buildExcelDocument(
          Map<String, Object> model, Workbook workbook, RequestContext context)
          throws Exception;

}
