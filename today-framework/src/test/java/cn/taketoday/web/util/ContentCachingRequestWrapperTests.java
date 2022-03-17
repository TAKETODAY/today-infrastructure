/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Test;

import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.servlet.ContentCachingRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Brian Clozel
 */
public class ContentCachingRequestWrapperTests {

  protected static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

  protected static final String CHARSET = "UTF-8";

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  public void cachedContent() throws Exception {
    this.request.setMethod("GET");
    this.request.setCharacterEncoding(CHARSET);
    this.request.setContent("Hello World".getBytes(CHARSET));

    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);
    byte[] response = FileCopyUtils.copyToByteArray(wrapper.getInputStream());
    assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
  }

  @Test
  public void cachedContentWithLimit() throws Exception {
    this.request.setMethod("GET");
    this.request.setCharacterEncoding(CHARSET);
    this.request.setContent("Hello World".getBytes(CHARSET));

    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request, 3);
    byte[] response = FileCopyUtils.copyToByteArray(wrapper.getInputStream());
    assertThat(response).isEqualTo("Hello World".getBytes(CHARSET));
    assertThat(wrapper.getContentAsByteArray()).isEqualTo("Hel".getBytes(CHARSET));
  }

  @Test
  public void cachedContentWithOverflow() throws Exception {
    this.request.setMethod("GET");
    this.request.setCharacterEncoding(CHARSET);
    this.request.setContent("Hello World".getBytes(CHARSET));

    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request, 3) {
      @Override
      protected void handleContentOverflow(int contentCacheLimit) {
        throw new IllegalStateException(String.valueOf(contentCacheLimit));
      }
    };

    assertThatIllegalStateException().isThrownBy(() ->
                    FileCopyUtils.copyToByteArray(wrapper.getInputStream()))
            .withMessage("3");
  }

  @Test
  public void requestParams() throws Exception {
    this.request.setMethod("POST");
    this.request.setContentType(FORM_CONTENT_TYPE);
    this.request.setCharacterEncoding(CHARSET);
    this.request.setParameter("first", "value");
    this.request.setParameter("second", "foo", "bar");

    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);
    // getting request parameters will consume the request body
    assertThat(wrapper.getParameterMap().isEmpty()).isFalse();
    assertThat(new String(wrapper.getContentAsByteArray())).isEqualTo("first=value&second=foo&second=bar");
    // SPR-12810 : inputstream body should be consumed
    assertThat(new String(FileCopyUtils.copyToByteArray(wrapper.getInputStream()))).isEqualTo("");
  }

  @Test  // SPR-12810
  public void inputStreamFormPostRequest() throws Exception {
    this.request.setMethod("POST");
    this.request.setContentType(FORM_CONTENT_TYPE);
    this.request.setCharacterEncoding(CHARSET);
    this.request.setParameter("first", "value");
    this.request.setParameter("second", "foo", "bar");

    ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);

    byte[] response = FileCopyUtils.copyToByteArray(wrapper.getInputStream());
    assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
  }

}
