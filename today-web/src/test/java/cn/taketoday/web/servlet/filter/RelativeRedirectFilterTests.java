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

package cn.taketoday.web.servlet.filter;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.resource.MockFilterChain;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 22:04
 */
class RelativeRedirectFilterTests {

  private RelativeRedirectFilter filter = new RelativeRedirectFilter();

  private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

  @Test
  void sendRedirectHttpStatusWhenNullThenIllegalArgumentException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.filter.setRedirectStatus(null));
  }

  @Test
  void sendRedirectHttpStatusWhenNot3xxThenIllegalArgumentException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.filter.setRedirectStatus(HttpStatus.OK));
  }

  @Test
  void doFilterSendRedirectWhenDefaultsThenLocationAnd303() throws Exception {
    String location = "/foo";
    sendRedirect(location);

    InOrder inOrder = Mockito.inOrder(this.response);
    inOrder.verify(this.response).resetBuffer();
    inOrder.verify(this.response).setStatus(HttpStatus.SEE_OTHER.value());
    inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
    inOrder.verify(this.response).flushBuffer();
  }

  @Test
  void doFilterSendRedirectWhenCustomSendRedirectHttpStatusThenLocationAnd301() throws Exception {
    String location = "/foo";
    HttpStatus status = HttpStatus.MOVED_PERMANENTLY;
    this.filter.setRedirectStatus(status);
    sendRedirect(location);

    InOrder inOrder = Mockito.inOrder(this.response);
    inOrder.verify(this.response).resetBuffer();
    inOrder.verify(this.response).setStatus(status.value());
    inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
    inOrder.verify(this.response).flushBuffer();
  }

  @Test
  void wrapOnceOnly() throws Exception {
    HttpServletResponse original = new MockHttpServletResponse();

    MockFilterChain chain = new MockFilterChain();
    this.filter.doFilterInternal(new MockHttpServletRequest(), original, chain);

    HttpServletResponse wrapped1 = (HttpServletResponse) chain.getResponse();
    assertThat(wrapped1).isNotSameAs(original);

    chain.reset();
    this.filter.doFilterInternal(new MockHttpServletRequest(), wrapped1, chain);
    HttpServletResponse current = (HttpServletResponse) chain.getResponse();
    assertThat(current).isSameAs(wrapped1);

    chain.reset();
    HttpServletResponse wrapped2 = new HttpServletResponseWrapper(wrapped1);
    this.filter.doFilterInternal(new MockHttpServletRequest(), wrapped2, chain);
    current = (HttpServletResponse) chain.getResponse();
    assertThat(current).isSameAs(wrapped2);
  }

  private void sendRedirect(String location) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    this.filter.doFilterInternal(new MockHttpServletRequest(), this.response, chain);

    HttpServletResponse wrappedResponse = (HttpServletResponse) chain.getResponse();
    wrappedResponse.sendRedirect(location);
  }

}
