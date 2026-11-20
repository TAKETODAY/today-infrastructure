/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.view.document;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import infra.context.support.GenericApplicationContext;
import infra.http.HttpHeaders;
import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;

class PdfViewTests {

  @Test
  void rendersModelIntoDownloadablePdf() throws Exception {
    MockHttpContext context = new MockHttpContext();

    pdfView().render(Map.of("body", "Quarterly report"), context);

    byte[] content = assertPdfResponse(context);
    PdfReader reader = new PdfReader(content);
    try {
      assertThat(reader.getNumberOfPages()).isEqualTo(1);
      assertThat(reader.getPageSize(1).getWidth()).isEqualTo(PageSize.A4.getWidth());
      assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("Quarterly report");
    }
    finally {
      reader.close();
    }
  }

  @Test
  void stampsPdfLoadedFromConfiguredResource(@TempDir Path directory) throws Exception {
    MockHttpContext templateResponse = new MockHttpContext();
    pdfView().render(Map.of("body", "Original document"), templateResponse);
    Path template = Files.write(directory.resolve("template.pdf"), templateResponse.response.getContentAsByteArray());
    MockHttpContext context = new MockHttpContext();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {
      applicationContext.refresh();
      AbstractPdfStamperView view = new AbstractPdfStamperView() {
        @Override
        protected void mergePdfDocument(Map<String, Object> model, PdfStamper stamper, HttpContext context) {
          stamper.setInfoDictionary(Map.of("Title", (String) model.get("title")));
        }
      };
      view.setUrl(template.toUri().toString());
      view.setApplicationContext(applicationContext);

      view.render(Map.of("title", "Stamped report"), context);
    }

    PdfReader reader = new PdfReader(assertPdfResponse(context));
    try {
      assertThat(reader.getInfo()).containsEntry("Title", "Stamped report");
      assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("Original document");
    }
    finally {
      reader.close();
    }
  }

  private static AbstractPdfView pdfView() {
    return new AbstractPdfView() {
      @Override
      protected void buildPdfDocument(Map<String, Object> model, Document document, PdfWriter writer,
              HttpContext context) throws Exception {
        document.add(new Paragraph((String) model.get("body")));
      }
    };
  }

  private static byte[] assertPdfResponse(MockHttpContext context) {
    byte[] content = context.response.getContentAsByteArray();
    assertThat(context.getResponseContentType()).isEqualTo("application/pdf");
    assertThat(context.response.getContentLengthLong()).isEqualTo(content.length);
    assertThat(context.responseHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("private, must-revalidate");
    return content;
  }

}
