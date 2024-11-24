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

package infra.test.web.client.match;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.client.ClientHttpRequest;
import infra.http.converter.FormHttpMessageConverter;
import infra.lang.Nullable;
import infra.mock.http.MockHttpInputMessage;
import infra.mock.http.client.MockClientHttpRequest;
import infra.test.util.JsonExpectationsHelper;
import infra.test.util.XmlExpectationsHelper;
import infra.test.web.client.RequestMatcher;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.StreamUtils;
import infra.mock.api.fileupload.FileItem;
import infra.mock.api.fileupload.FileUpload;
import infra.mock.api.fileupload.UploadContext;
import infra.mock.api.fileupload.disk.DiskFileItemFactory;

import static infra.test.util.AssertionErrors.assertEquals;
import static infra.test.util.AssertionErrors.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for request content {@code RequestMatcher}'s. An instance of this
 * class is typically accessed via {@link MockRestRequestMatchers#content()}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ContentRequestMatchers {

  private final XmlExpectationsHelper xmlHelper;

  private final JsonExpectationsHelper jsonHelper;

  /**
   * Class constructor, not for direct instantiation.
   * Use {@link MockRestRequestMatchers#content()}.
   */
  protected ContentRequestMatchers() {
    this.xmlHelper = new XmlExpectationsHelper();
    this.jsonHelper = new JsonExpectationsHelper();
  }

  /**
   * Assert the request content type as a String.
   */
  public RequestMatcher contentType(String expectedContentType) {
    return contentType(MediaType.parseMediaType(expectedContentType));
  }

  /**
   * Assert the request content type as a {@link MediaType}.
   */
  public RequestMatcher contentType(MediaType expectedContentType) {
    return request -> {
      MediaType actualContentType = request.getHeaders().getContentType();
      assertTrue("Content type not set", actualContentType != null);
      assertEquals("Content type", expectedContentType, actualContentType);
    };
  }

  /**
   * Assert the request content type is compatible with the given
   * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
   */
  public RequestMatcher contentTypeCompatibleWith(String contentType) {
    return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
  }

  /**
   * Assert the request content type is compatible with the given
   * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
   */
  public RequestMatcher contentTypeCompatibleWith(MediaType contentType) {
    return request -> {
      MediaType actualContentType = request.getHeaders().getContentType();
      assertTrue("Content type not set", actualContentType != null);
      if (actualContentType != null) {
        assertTrue("Content type [" + actualContentType + "] is not compatible with [" + contentType + "]",
                actualContentType.isCompatibleWith(contentType));
      }
    };
  }

  /**
   * Get the body of the request as a UTF-8 string and apply the given {@link Matcher}.
   */
  public RequestMatcher string(Matcher<? super String> matcher) {
    return request -> {
      MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
      assertThat("Request content", mockRequest.getBodyAsString(), matcher);
    };
  }

  /**
   * Get the body of the request as a UTF-8 string and compare it to the given String.
   */
  public RequestMatcher string(String expectedContent) {
    return request -> {
      MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
      assertEquals("Request content", expectedContent, mockRequest.getBodyAsString());
    };
  }

  /**
   * Compare the body of the request to the given byte array.
   */
  public RequestMatcher bytes(byte[] expectedContent) {
    return request -> {
      MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
      assertEquals("Request content", expectedContent, mockRequest.getBodyAsBytes());
    };
  }

  /**
   * Parse the body as form data and compare to the given {@code MultiValueMap}.
   */
  public RequestMatcher formData(MultiValueMap<String, String> expected) {
    return formData(expected, true);
  }

  /**
   * Variant of {@link #formData(MultiValueMap)} that matches the given subset
   * of expected form parameters.
   */
  public RequestMatcher formDataContains(Map<String, String> expected) {
    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>(expected.size());
    expected.forEach(multiValueMap::add);
    return formData(multiValueMap, false);
  }

  private RequestMatcher formData(MultiValueMap<String, String> expectedMap, boolean containsExactly) {
    return request -> {
      MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
      MockHttpInputMessage message = new MockHttpInputMessage(mockRequest.getBodyAsBytes());
      message.getHeaders().putAll(mockRequest.getHeaders());
      MultiValueMap<String, String> actualMap = new FormHttpMessageConverter().read(null, message);
      if (containsExactly) {
        assertEquals("Form data", expectedMap, actualMap);
      }
      else {
        assertTrue("Form data " + actualMap, expectedMap.size() <= actualMap.size());
        for (Map.Entry<String, ? extends List<?>> entry : expectedMap.entrySet()) {
          String name = entry.getKey();
          List<?> values = entry.getValue();
          assertTrue("No form parameter '" + name + "'", actualMap.get(name) != null);
          assertTrue("Parameter value count " + values.size(), values.size() <= actualMap.get(name).size());
          for (int i = 0; i < values.size(); i++) {
            assertEquals("Form parameter", values.get(i), actualMap.get(name).get(i));
          }
        }
      }
    };
  }

  /**
   * Parse the body as multipart data and assert it contains exactly the
   * values from the given {@code MultiValueMap}. Values may be of type:
   * <ul>
   * <li>{@code String} - form field
   * <li>{@link Resource} - content from a file
   * <li>{@code byte[]} - other raw content
   * </ul>
   * <p><strong>Note:</strong> This method uses the Apache Commons FileUpload
   * library to parse the multipart data and it must be on the test classpath.
   *
   * @param expectedMap the expected multipart values
   */
  public RequestMatcher multipartData(MultiValueMap<String, ?> expectedMap) {
    return multipartData(expectedMap, true);
  }

  /**
   * Variant of {@link #multipartData(MultiValueMap)} that does the same but
   * only for a subset of the actual values.
   *
   * @param expectedMap the expected multipart values
   */
  public RequestMatcher multipartDataContains(Map<String, ?> expectedMap) {
    MultiValueMap<String, Object> map = new LinkedMultiValueMap<>(expectedMap.size());
    expectedMap.forEach(map::add);
    return multipartData(map, false);
  }

  private RequestMatcher multipartData(MultiValueMap<String, ?> expectedMap, boolean containsExactly) {
    return request -> {
      MultiValueMap<String, ?> actualMap = MultipartHelper.parse((MockClientHttpRequest) request);
      if (containsExactly) {
        assertEquals("Multipart request content: " + actualMap, expectedMap.size(), actualMap.size());
      }
      for (Map.Entry<String, ? extends List<?>> entry : expectedMap.entrySet()) {
        String name = entry.getKey();
        List<?> values = entry.getValue();
        assertTrue("No Multipart '" + name + "'", actualMap.get(name) != null);
        assertTrue("Multipart value count " + values.size(), containsExactly ?
                values.size() == actualMap.get(name).size() :
                values.size() <= actualMap.get(name).size());
        for (int i = 0; i < values.size(); i++) {
          Object expected = values.get(i);
          Object actual = actualMap.get(name).get(i);
          if (expected instanceof Resource) {
            expected = StreamUtils.copyToByteArray(((Resource) expected).getInputStream());
          }
          if (expected instanceof byte[]) {
            assertTrue("Multipart is not a file", actual instanceof byte[]);
            assertEquals("Multipart content", expected, actual);
          }
          else if (expected instanceof String) {
            assertTrue("Multipart is not a String", actual instanceof String);
            assertEquals("Multipart content", expected, actual);
          }
          else {
            throw new IllegalArgumentException("Unexpected multipart value: " + expected.getClass());
          }
        }
      }
    };
  }

  /**
   * Parse the request body and the given String as XML and assert that the
   * two are "similar" - i.e. they contain the same elements and attributes
   * regardless of order.
   * <p>Use of this matcher assumes the
   * <a href="http://xmlunit.sourceforge.net/">XMLUnit</a> library is available.
   *
   * @param expectedXmlContent the expected XML content
   */
  public RequestMatcher xml(String expectedXmlContent) {
    return new AbstractXmlRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws Exception {
        xmlHelper.assertXmlEqual(expectedXmlContent, request.getBodyAsString());
      }
    };
  }

  /**
   * Parse the request content as {@link Node} and apply the given {@link Matcher}.
   */
  public RequestMatcher node(Matcher<? super Node> matcher) {
    return new AbstractXmlRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws Exception {
        xmlHelper.assertNode(request.getBodyAsString(), matcher);
      }
    };
  }

  /**
   * Parse the request content as {@link DOMSource} and apply the given {@link Matcher}.
   *
   * @see <a href="https://code.google.com/p/xml-matchers/">https://code.google.com/p/xml-matchers/</a>
   */
  public RequestMatcher source(Matcher<? super Source> matcher) {
    return new AbstractXmlRequestMatcher() {
      @Override
      protected void matchInternal(MockClientHttpRequest request) throws Exception {
        xmlHelper.assertSource(request.getBodyAsString(), matcher);
      }
    };
  }

  /**
   * Parse the expected and actual strings as JSON and assert the two
   * are "similar" - i.e. they contain the same attribute-value pairs
   * regardless of formatting with a lenient checking (extensible, and non-strict array
   * ordering).
   * <p>Use of this matcher requires the <a
   * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
   *
   * @param expectedJsonContent the expected JSON content
   */
  public RequestMatcher json(String expectedJsonContent) {
    return json(expectedJsonContent, false);
  }

  /**
   * Parse the request body and the given string as JSON and assert the two
   * are "similar" - i.e. they contain the same attribute-value pairs
   * regardless of formatting.
   * <p>Can compare in two modes, depending on {@code strict} parameter value:
   * <ul>
   * <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
   * <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
   * </ul>
   * <p>Use of this matcher requires the <a
   * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
   *
   * @param expectedJsonContent the expected JSON content
   * @param strict enables strict checking
   */
  public RequestMatcher json(String expectedJsonContent, boolean strict) {
    return request -> {
      try {
        MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
        this.jsonHelper.assertJsonEqual(expectedJsonContent, mockRequest.getBodyAsString(), strict);
      }
      catch (Exception ex) {
        throw new AssertionError("Failed to parse expected or actual JSON request content", ex);
      }
    };
  }

  /**
   * Abstract base class for XML {@link RequestMatcher}'s.
   */
  private abstract static class AbstractXmlRequestMatcher implements RequestMatcher {

    @Override
    public final void match(ClientHttpRequest request) throws IOException, AssertionError {
      try {
        MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
        matchInternal(mockRequest);
      }
      catch (Exception ex) {
        throw new AssertionError("Failed to parse expected or actual XML request content", ex);
      }
    }

    protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
  }

  private static class MultipartHelper {

    public static MultiValueMap<String, ?> parse(MockClientHttpRequest request) {
      try {
        FileUpload fileUpload = new FileUpload();
        fileUpload.setFileItemFactory(new DiskFileItemFactory());

        List<FileItem> fileItems = fileUpload.parseRequest(new UploadContext() {
          private final byte[] body = request.getBodyAsBytes();

          @Override
          @Nullable
          public String getCharacterEncoding() {
            return request.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
          }

          @Override
          @Nullable
          public String getContentType() {
            return request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
          }

          @Override
          public InputStream getInputStream() {
            return new ByteArrayInputStream(this.body);
          }

          @Override
          public long contentLength() {
            return this.body.length;
          }
        });

        MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
        for (FileItem fileItem : fileItems) {
          result.add(fileItem.getFieldName(),
                  (fileItem.isFormField() ? fileItem.getString() : fileItem.get()));
        }
        return result;
      }
      catch (Exception ex) {
        throw new IllegalStateException("Failed to parse multipart request", ex);
      }
    }
  }

}
