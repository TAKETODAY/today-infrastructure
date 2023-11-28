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

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.AbstractUrlBasedView;

/**
 * Abstract superclass for PDF views that operate on an existing
 * document with an AcroForm. Application-specific view classes
 * will extend this class to merge the PDF form with model data.
 *
 * <p>This view implementation uses Bruno Lowagie's
 * <a href="https://www.lowagie.com/iText">iText</a> API.
 * Known to work with the original iText 2.1.7 as well as its fork
 * <a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a>.
 * <b>We strongly recommend OpenPDF since it is actively maintained
 * and fixes an important vulnerability for untrusted PDF content.</b>
 *
 * <p>Thanks to Bryant Larsen for the suggestion and the original prototype!
 *
 * @author Juergen Hoeller
 * @see AbstractPdfView
 * @since 4.0
 */
public abstract class AbstractPdfStamperView extends AbstractUrlBasedView {

  public AbstractPdfStamperView() {
    setContentType("application/pdf");
  }

  @Override
  protected boolean generatesDownloadContent() {
    return true;
  }

  @Override
  protected final void renderMergedOutputModel(
          Map<String, Object> model, RequestContext request) throws Exception {

    // IE workaround: write into byte array first.
    ByteArrayOutputStream baos = createTemporaryOutputStream();

    PdfReader reader = readPdfResource();
    PdfStamper stamper = new PdfStamper(reader, baos);
    mergePdfDocument(model, stamper, request);
    stamper.close();

    // Flush to HTTP response.
    writeToResponse(request, baos);
  }

  /**
   * Read the raw PDF resource into an iText PdfReader.
   * <p>The default implementation resolve the specified "url" property
   * as ApplicationContext resource.
   *
   * @return the PdfReader instance
   * @throws IOException if resource access failed
   * @see #setUrl
   */
  protected PdfReader readPdfResource() throws IOException {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");
    return new PdfReader(obtainApplicationContext().getResource(url).getInputStream());
  }

  /**
   * Subclasses must implement this method to merge the PDF form
   * with the given model data.
   * <p>This is where you are able to set values on the AcroForm.
   * An example of what can be done at this level is:
   * <pre class="code">
   * // get the form from the document
   * AcroFields form = stamper.getAcroFields();
   *
   * // set some values on the form
   * form.setField("field1", "value1");
   * form.setField("field2", "Vvlue2");
   *
   * // set the disposition and filename
   * response.setHeader("Content-disposition", "attachment; FILENAME=someName.pdf");</pre>
   * <p>Note that the passed-in HTTP response is just supposed to be used
   * for setting cookies or other HTTP headers. The built PDF document itself
   * will automatically get written to the response after this method returns.
   *
   * @param model the model Map
   * @param stamper the PdfStamper instance that will contain the AcroFields.
   * You may also customize this PdfStamper instance according to your needs,
   * e.g. setting the "formFlattening" property.
   * @param context in case we need locale etc. Shouldn't look at attributes.
   * @throws Exception any exception that occurred during document building
   */
  protected abstract void mergePdfDocument(
          Map<String, Object> model, PdfStamper stamper, RequestContext context) throws Exception;

}
