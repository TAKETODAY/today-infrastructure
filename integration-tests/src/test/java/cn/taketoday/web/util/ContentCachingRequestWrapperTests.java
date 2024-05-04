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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.web.servlet.ContentCachingRequestWrapper;
import cn.taketoday.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Brian Clozel
 */
public class ContentCachingRequestWrapperTests {

  protected static final String FORM_CONTENT_TYPE = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

  protected static final String CHARSET = StandardCharsets.UTF_8.name();

  protected static final String GET = HttpMethod.GET.name();

  protected static final String POST = HttpMethod.POST.name();

  protected static final int CONTENT_CACHE_LIMIT = 3;

  @Test
  void cachedContentToByteArrayWithNoRead() throws Exception {
    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello"));
    assertThat(wrapper.getContentAsByteArray()).isEmpty();
  }

  @Test
  void cachedContentToStringWithNoRead() throws Exception {
    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello"));
    assertThat(wrapper.getContentAsString()).isEqualTo("");
  }

  @Test
  void cachedContentToByteArray() throws Exception {
    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"));
    byte[] response = wrapper.getInputStream().readAllBytes();
    assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
  }

  @Test
  void cachedContentToString() throws Exception {
    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"));
    byte[] response = wrapper.getInputStream().readAllBytes();
    assertThat(wrapper.getContentAsString()).isEqualTo(new String(response, CHARSET));
  }

  @Test
  void cachedContentToByteArrayWithLimit() throws Exception {
    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"), CONTENT_CACHE_LIMIT);
    byte[] response = wrapper.getInputStream().readAllBytes();
    assertThat(response).isEqualTo("Hello World".getBytes(CHARSET));
    assertThat(wrapper.getContentAsByteArray()).isEqualTo("Hel".getBytes(CHARSET));
  }

  @Test
  void cachedContentToStringWithLimit() throws Exception {
    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"), CONTENT_CACHE_LIMIT);
    byte[] response = wrapper.getInputStream().readAllBytes();
    assertThat(response).isEqualTo("Hello World".getBytes(CHARSET));
    assertThat(wrapper.getContentAsString()).isEqualTo(new String("Hel".getBytes(CHARSET), CHARSET));
  }

  @Test
  void cachedContentWithOverflow() throws Exception {
    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(
            createGetRequest("Hello World"), CONTENT_CACHE_LIMIT) {
      @Override
      protected void handleContentOverflow(int contentCacheLimit) {
        throw new IllegalStateException(String.valueOf(contentCacheLimit));
      }
    };

    assertThatIllegalStateException().isThrownBy(() ->
                    wrapper.getInputStream().readAllBytes())
            .withMessage("3");
  }

  @Test
  void requestParams() throws Exception {
    MockHttpServletRequest request = createPostRequest();
    request.setParameter("first", "value");
    request.setParameter("second", "foo", "bar");

    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
    // getting request parameters will consume the request body
    assertThat(wrapper.getParameterMap().isEmpty()).isFalse();
    assertThat(new String(wrapper.getContentAsByteArray())).isEqualTo("first=value&second=foo&second=bar");
    // SPR-12810 : inputstream body should be consumed
    assertThat(new String(wrapper.getInputStream().readAllBytes())).isEmpty();
  }

  @Test
    // SPR-12810
  void inputStreamFormPostRequest() throws Exception {
    MockHttpServletRequest request = createPostRequest();
    request.setParameter("first", "value");
    request.setParameter("second", "foo", "bar");

    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);

    byte[] response = wrapper.getInputStream().readAllBytes();
    assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
  }

  private MockHttpServletRequest createGetRequest(String content) throws UnsupportedEncodingException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod(GET);
    request.setCharacterEncoding(CHARSET);
    request.setContent(content.getBytes(CHARSET));
    return request;
  }

  private MockHttpServletRequest createPostRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod(POST);
    request.setContentType(FORM_CONTENT_TYPE);
    request.setCharacterEncoding(CHARSET);
    return request;
  }

}
