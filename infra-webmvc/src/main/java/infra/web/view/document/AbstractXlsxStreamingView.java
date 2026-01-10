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

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.util.Map;

import infra.web.RequestContext;

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
