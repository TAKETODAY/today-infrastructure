/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.multipart.parsing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/13 11:58
 */
class MultipartParsingTests {

  /**
   * The content type used in several tests.
   */
  public static final String CONTENT_TYPE = "multipart/form-data; boundary=---1234";

  private final DefaultMultipartParser multipartParser = new DefaultMultipartParser();

  @Test
  void maxFields() {
    multipartParser.setMaxFields(1);
    assertThatThrownBy(() -> parseRequest("""
            -----1234\r
            content-disposition: form-data; name="field1"\r
            \r
            Joe Blow\r
            -----1234\r
            content-disposition: form-data; name="pics"\r
            Content-type: multipart/mixed; boundary=---9876\r
            \r
            -----9876\r
            Content-disposition: attachment; filename="file1.txt"\r
            Content-Type: text/plain\r
            \r
            ... contents of file1.txt ...\r
            -----9876--\r
            -----1234--\r
            """))
            .isInstanceOf(MultipartFieldCountLimitException.class)
            .hasMessageContaining("Maximum file count %,d exceeded.".formatted(multipartParser.getMaxFields()));
  }

  @Test
  void contentTypeAttachment() throws IOException {
    final var fileItems = parseRequest("""
            -----1234\r
            content-disposition: form-data; name="field1"\r
            \r
            Joe Blow\r
            -----1234\r
            content-disposition: form-data; name="pics"\r
            Content-type: multipart/mixed; boundary=---9876\r
            \r
            -----9876\r
            Content-disposition: attachment; filename="file1.txt"\r
            Content-Type: text/plain\r
            \r
            ... contents of file1.txt ...\r
            -----9876--\r
            -----1234--\r
            """);
    assertEquals(2, fileItems.size());

    final var field = fileItems.get(0);
    assertEquals("field1", field.getName());
    assertTrue(field.isFormField());
    assertEquals("Joe Blow", field.getContentAsString());

    final var fileItem = fileItems.get(1);
    assertEquals("pics", fileItem.getName());
    assertFalse(fileItem.isFormField());
    assertEquals("... contents of file1.txt ...", fileItem.getContentAsString());
    assertEquals("text/plain", fileItem.getContentTypeAsString());
    assertEquals("file1.txt", fileItem.getOriginalFilename());
  }

  /**
   * This is what the browser does if you submit the form without choosing a file.
   *
   */
  @Test
  void emptyFile() throws IOException {
    final var fileItems = parseRequest(
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                    "\r\n" +
                    "\r\n" +
                    "-----1234--\r\n");
    assertEquals(1, fileItems.size());

    final var file = fileItems.get(0);
    assertFalse(file.isFormField());
    assertEquals("", file.getContentAsString());
    assertEquals("", file.getOriginalFilename());
  }

  @Test
  void fileNameCaseSensitivity() throws IOException {

    final var fileItems = parseRequest(
            "-----1234\r\n" +
                    "Content-Disposition: form-data; "
                    + "name=\"FiLe\"; filename=\"FOO.tab\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234--\r\n");

    assertEquals(1, fileItems.size());

    final var file = fileItems.get(0);
    assertEquals("FiLe", file.getName());
    assertEquals("FOO.tab", file.getOriginalFilename());
  }

  @Test
  public void fileUpload() throws IOException {

    final var fileItems = parseRequest("""
            -----1234\r
            Content-Disposition: \
            form-data; name="file"; filename="foo.tab"\r
            Content-Type: text/whatever\r
            \r
            This is the content of the file
            \r
            -----1234\r
            Content-Disposition: form-data; name="field"\r
            \r
            fieldValue\r
            -----1234\r
            Content-Disposition: form-data; name="multi"\r
            \r
            value1\r
            -----1234\r
            Content-Disposition: form-data; name="multi"\r
            \r
            value2\r
            -----1234--\r
            """);

    assertEquals(4, fileItems.size());

    final var file = fileItems.get(0);
    assertEquals("file", file.getName());
    assertFalse(file.isFormField());
    assertEquals("This is the content of the file\n", file.getContentAsString());
    assertEquals("text/whatever", file.getContentTypeAsString());
    assertEquals("foo.tab", file.getOriginalFilename());

    final var field = fileItems.get(1);
    assertEquals("field", field.getName());
    assertTrue(field.isFormField());
    assertEquals("fieldValue", field.getContentAsString());

    final var multi0 = fileItems.get(2);
    assertEquals("multi", multi0.getName());
    assertTrue(multi0.isFormField());
    assertEquals("value1", multi0.getContentAsString());

    final var multi1 = fileItems.get(3);
    assertEquals("multi", multi1.getName());
    assertTrue(multi1.isFormField());
    assertEquals("value2", multi1.getContentAsString());
  }

  @Test
  void fileUpload130() throws IOException {
    final String[] headerNames = { "SomeHeader", "OtherHeader", "YetAnotherHeader", "WhatAHeader" };
    final String[] headerValues = { "present", "Is there", "Here", "Is That" };

    final var fileItems = parseRequest(
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; "
                    + "filename=\"foo.tab\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    headerNames[0] + ": " + headerValues[0] + "\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; \r\n" +
                    "\tname=\"field\"\r\n" +
                    headerNames[1] + ": " + headerValues[1] + "\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data;\r\n" +
                    "     name=\"multi\"\r\n" +
                    headerNames[2] + ": " + headerValues[2] + "\r\n" +
                    "\r\n" +
                    "value1\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    headerNames[3] + ": " + headerValues[3] + "\r\n" +
                    "\r\n" +
                    "value2\r\n" +
                    "-----1234--\r\n");

    assertEquals(4, fileItems.size());

    final var file = fileItems.get(0);
    assertHeaders(headerNames, headerValues, file, 0);

    final var field = fileItems.get(1);
    assertHeaders(headerNames, headerValues, field, 1);

    final var multi0 = fileItems.get(2);
    assertHeaders(headerNames, headerValues, multi0, 2);

    final var multi1 = fileItems.get(3);
    assertHeaders(headerNames, headerValues, multi1, 3);
  }

  @Test
  void fileupload62() throws IOException {

    final var contentType = "multipart/form-data; boundary=AaB03x";
    final var request = """
            --AaB03x\r
            content-disposition: form-data; name="field1"\r
            \r
            Joe Blow\r
            --AaB03x\r
            content-disposition: form-data; name="pics"\r
            Content-type: multipart/mixed; boundary=BbC04y\r
            \r
            --BbC04y\r
            Content-disposition: attachment; filename="file1.txt"\r
            Content-Type: text/plain\r
            \r
            ... contents of file1.txt ...\r
            --BbC04y\r
            Content-disposition: attachment; filename="file2.gif"\r
            Content-type: image/gif\r
            Content-Transfer-Encoding: binary\r
            \r
            ...contents of file2.gif...\r
            --BbC04y--\r
            --AaB03x--""";

    final var fileItems = parseRequest(request.getBytes(StandardCharsets.US_ASCII), contentType);
    assertEquals(3, fileItems.size());
    final var item0 = fileItems.get(0);
    assertEquals("field1", item0.getName());
    assertNull(item0.getOriginalFilename());
    assertEquals("Joe Blow", new String(item0.getContentAsByteArray()));
    final var item1 = fileItems.get(1);
    assertEquals("pics", item1.getName());
    assertEquals("file1.txt", item1.getOriginalFilename());
    assertEquals("... contents of file1.txt ...", new String(item1.getContentAsByteArray()));
    final var item2 = fileItems.get(2);
    assertEquals("pics", item2.getName());
    assertEquals("file2.gif", item2.getOriginalFilename());
    assertEquals("...contents of file2.gif...", new String(item2.getContentAsByteArray()));
  }

  @Test
  void foldedHeaders() throws IOException {
    final var fileItems = parseRequest("-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data; \r\n" +
            "\tname=\"field\"\r\n" +
            "\r\n" +
            "fieldValue\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data;\r\n" +
            "     name=\"multi\"\r\n" +
            "\r\n" +
            "value1\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"multi\"\r\n" +
            "\r\n" +
            "value2\r\n" +
            "-----1234--\r\n");

    assertEquals(4, fileItems.size());

    final var file = fileItems.get(0);
    assertEquals("file", file.getName());
    assertFalse(file.isFormField());
    assertEquals("This is the content of the file\n", file.getContentAsString());
    assertEquals("text/whatever", file.getContentTypeAsString());
    assertEquals("foo.tab", file.getOriginalFilename());

    final var field = fileItems.get(1);
    assertEquals("field", field.getName());
    assertTrue(field.isFormField());
    assertEquals("fieldValue", field.getContentAsString());

    final var multi0 = fileItems.get(2);
    assertEquals("multi", multi0.getName());
    assertTrue(multi0.isFormField());
    assertEquals("value1", multi0.getContentAsString());

    final var multi1 = fileItems.get(3);
    assertEquals("multi", multi1.getName());
    assertTrue(multi1.isFormField());
    assertEquals("value2", multi1.getContentAsString());
  }

  /**
   * Internet Explorer 5 for the Mac has a bug where the carriage return is missing on any boundary line immediately preceding an input with type=image.
   * (type=submit does not have the bug.)
   */
  @Test
  void ie5macbug() throws IOException {
    final var fileItems = parseRequest(
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"field1\"\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\n" + // NOTE \r missing
                    "Content-Disposition: form-data; name=\"submitName.x\"\r\n" +
                    "\r\n" +
                    "42\r\n" +
                    "-----1234\n" + // NOTE \r missing
                    "Content-Disposition: form-data; name=\"submitName.y\"\r\n" +
                    "\r\n" +
                    "21\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"field2\"\r\n" +
                    "\r\n" +
                    "fieldValue2\r\n" +
                    "-----1234--\r\n");

    assertEquals(4, fileItems.size());

    final var field1 = fileItems.get(0);
    assertEquals("field1", field1.getName());
    assertTrue(field1.isFormField());
    assertEquals("fieldValue", field1.getContentAsString());

    final var submitX = fileItems.get(1);
    assertEquals("submitName.x", submitX.getName());
    assertTrue(submitX.isFormField());
    assertEquals("42", submitX.getContentAsString());

    final var submitY = fileItems.get(2);
    assertEquals("submitName.y", submitY.getName());
    assertTrue(submitY.isFormField());
    assertEquals("21", submitY.getContentAsString());

    final var field2 = fileItems.get(3);
    assertEquals("field2", field2.getName());
    assertTrue(field2.isFormField());
    assertEquals("fieldValue2", field2.getContentAsString());
  }

  /**
   * Test for multipart/mixed with no boundary defined
   */
  @Test
  void multipartMixedNoBoundary() {
    final var contentType = "multipart/form-data; boundary=AaB03x";
    final var request = """
            --AaB03x\r
            content-disposition: form-data; name="field1"\r
            \r
            Joe Blow\r
            --AaB03x\r
            content-disposition: form-data; name="pics"\r
            Content-type: multipart/mixed\r
            \r
            --BbC04y\r
            Content-disposition: attachment; filename="file1.txt"\r
            Content-Type: text/plain\r
            \r
            ... contents of file1.txt ...\r
            --BbC04y\r
            Content-disposition: attachment; filename="file2.gif"\r
            Content-type: image/gif\r
            Content-Transfer-Encoding: binary\r
            \r
            ...contents of file2.gif...\r
            --BbC04y--\r
            --AaB03x--""";
    assertThrows(MultipartBoundaryException.class,
            () -> parseRequest(request.getBytes(StandardCharsets.US_ASCII), contentType));
  }

  /**
   * Test for multipart/related without any content-disposition Header.
   * This kind of Content-Type is commonly used by SOAP-Requests with Attachments (MTOM)
   */
  @Test
  void multipleRelated() throws Exception {
    final String soapEnvelope =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\r\n" +
                    "  <soap:Header></soap:Header>\r\n" +
                    "  <soap:Body>\r\n" +
                    "    <ns1:Test xmlns:ns1=\"http://www.test.org/some-test-namespace\">\r\n" +
                    "      <ns1:Attachment>\r\n" +
                    "        <xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\"" +
                    " href=\"ref-to-attachment%40some.domain.org\"/>\r\n" +
                    "      </ns1:Attachment>\r\n" +
                    "    </ns1:Test>\r\n" +
                    "  </soap:Body>\r\n" +
                    "</soap:Envelope>";

    final String text =
            "-----1234\r\n" +
                    "content-type: application/xop+xml; type=\"application/soap+xml\"\r\n" +
                    "\r\n" +
                    soapEnvelope + "\r\n" +
                    "-----1234\r\n" +
                    "Content-type: text/plain\r\n" +
                    "content-id: <ref-to-attachment@some.domain.org>\r\n" +
                    "\r\n" +
                    "some text/plain content\r\n" +
                    "-----1234--\r\n";

    final var bytes = text.getBytes(StandardCharsets.US_ASCII);
    final var fileItems = parseRequest(bytes, "multipart/related; boundary=---1234;" +
            " type=\"application/xop+xml\"; start-info=\"application/soap+xml\"");
    assertEquals(2, fileItems.size());

    final var part1 = fileItems.get(0);
    assertThat(part1.getName()).isEmpty();
    assertFalse(part1.isFormField());
    assertEquals(soapEnvelope, part1.getContentAsString());

    final var part2 = fileItems.get(1);
    assertThat(part2.getName()).isEmpty();

    assertFalse(part2.isFormField());
    assertEquals("some text/plain content", part2.getContentAsString());
    assertEquals("text/plain", part2.getContentTypeAsString());
    assertNull(part2.getOriginalFilename());
  }

  //

  @Test
  void parseImpliedUtf8() throws Exception {
    // utf8 encoded form-data without explicit content-type encoding
    final var text = """
            -----1234\r
            Content-Disposition: form-data; name="utf8Html"\r
            \r
            Thís ís the coñteñt of the fíle
            \r
            -----1234--\r
            """;

    final var fileItems = parseRequest(text.getBytes(StandardCharsets.UTF_8));
    final var fileItem = fileItems.get(0);
    assertTrue(fileItem.getContentAsString().contains("coñteñt"), fileItem.getContentAsString());
  }

  @Test
  void parseParameterMap() {
    final var text = """
            -----1234\r
            Content-Disposition: form-data; name="file"; filename="foo.tab"\r
            Content-Type: text/whatever\r
            \r
            This is the content of the file
            \r
            -----1234\r
            Content-Disposition: form-data; name="field"\r
            \r
            fieldValue\r
            -----1234\r
            Content-Disposition: form-data; name="multi"\r
            \r
            value1\r
            -----1234\r
            Content-Disposition: form-data; name="multi"\r
            \r
            value2\r
            -----1234--\r
            """;

    final var multipartRequest = parse(text);

    assertTrue(multipartRequest.contains("file"));
    assertEquals(1, multipartRequest.getParts("file").size());

    assertTrue(multipartRequest.contains("field"));
    assertEquals(1, multipartRequest.getParts("field").size());

    assertTrue(multipartRequest.contains("multi"));
    assertEquals(2, multipartRequest.getParts("multi").size());
  }

  private void assertHeaders(final String[] headerNames, final String[] headerValues, final Part fileItems, final int index) {
    for (var i = 0; i < headerNames.length; i++) {
      final var value = fileItems.getHeader(headerNames[i]);
      if (i == index) {
        assertEquals(headerValues[i], value);
      }
      else {
        assertNull(value);
      }
    }
  }

  public List<Part> parseRequest(final byte[] bytes) {
    return parseRequest(bytes, CONTENT_TYPE);
  }

  public List<Part> parseRequest(final byte[] bytes, final String contentType) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/part");
    MockRequestContext context = new MockRequestContext(request);
    request.setContentType(contentType);
    request.setContent(bytes);

    ArrayList<Part> res = new ArrayList<>();
    for (var entry : multipartParser.parseRequest(context).entrySet()) {
      res.addAll(entry.getValue());
    }
    return res;
  }

  public List<Part> parseRequest(final String content) {
    final byte[] bytes = content.getBytes(StandardCharsets.US_ASCII);
    return parseRequest(bytes, CONTENT_TYPE);
  }

  public MultipartRequest parse(final String content) {
    final byte[] bytes = content.getBytes(StandardCharsets.US_ASCII);
    return parse(bytes, CONTENT_TYPE);
  }

  public MultipartRequest parse(final byte[] bytes, final String contentType) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/part");
    MockRequestContext context = new MockRequestContext(request);
    request.setContentType(contentType);
    request.setContent(bytes);

    return multipartParser.parse(context);
  }
}
