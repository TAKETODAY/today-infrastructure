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
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import infra.web.RequestContext;
import infra.web.view.AbstractView;

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

    // Flush byte array to output stream.
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
