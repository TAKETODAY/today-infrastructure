/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.web.testfixture.servlet.MockFilterConfig;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/2 16:03
 */
class CharacterEncodingFilterTests {

  private static final String FILTER_NAME = "boot";

  private static final String ENCODING = "UTF-8";

  @Test
  public void forceEncodingAlwaysSetsEncoding() throws Exception {
    HttpServletRequest request = mock();
    request.setCharacterEncoding(ENCODING);
//    given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
    given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
    given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

    HttpServletResponse response = mock();
    FilterChain filterChain = mock();

    CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING, true);
    filter.init(new MockFilterConfig(FILTER_NAME));
    filter.doFilter(request, response, filterChain);

    verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
    verify(request).removeAttribute(filteredName(FILTER_NAME));
    verify(response).setCharacterEncoding(ENCODING);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void encodingIfEmptyAndNotForced() throws Exception {
    HttpServletRequest request = mock();
    given(request.getCharacterEncoding()).willReturn(null);
//    given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
    given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
    given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = mock();

    CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING);
    filter.init(new MockFilterConfig(FILTER_NAME));
    filter.doFilter(request, response, filterChain);

    verify(request).setCharacterEncoding(ENCODING);
    verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
    verify(request).removeAttribute(filteredName(FILTER_NAME));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void doesNotIfEncodingIsNotEmptyAndNotForced() throws Exception {
    HttpServletRequest request = mock();
    given(request.getCharacterEncoding()).willReturn(ENCODING);
//    given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
    given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
    given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = mock();

    CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING);
    filter.init(new MockFilterConfig(FILTER_NAME));
    filter.doFilter(request, response, filterChain);

    verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
    verify(request).removeAttribute(filteredName(FILTER_NAME));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void withBeanInitialization() throws Exception {
    HttpServletRequest request = mock();
    given(request.getCharacterEncoding()).willReturn(null);
//    given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
    given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
    given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = mock();

    CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.setEncoding(ENCODING);
    filter.setBeanName(FILTER_NAME);
    filter.setServletContext(new MockServletContext());
    filter.doFilter(request, response, filterChain);

    verify(request).setCharacterEncoding(ENCODING);
    verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
    verify(request).removeAttribute(filteredName(FILTER_NAME));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void withIncompleteInitialization() throws Exception {
    HttpServletRequest request = mock();
    given(request.getCharacterEncoding()).willReturn(null);
//    given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
    given(request.getAttribute(filteredName(CharacterEncodingFilter.class.getName()))).willReturn(null);
    given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = mock();

    CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING);
    filter.doFilter(request, response, filterChain);

    verify(request).setCharacterEncoding(ENCODING);
    verify(request).setAttribute(filteredName(CharacterEncodingFilter.class.getName()), Boolean.TRUE);
    verify(request).removeAttribute(filteredName(CharacterEncodingFilter.class.getName()));
    verify(filterChain).doFilter(request, response);
  }

  // SPR-14240
  @Test
  public void setForceEncodingOnRequestOnly() throws Exception {
    HttpServletRequest request = mock();
    request.setCharacterEncoding(ENCODING);
//    given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
    given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
    given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

    HttpServletResponse response = mock();
    FilterChain filterChain = mock();

    CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING, true, false);
    filter.init(new MockFilterConfig(FILTER_NAME));
    filter.doFilter(request, response, filterChain);

    verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
    verify(request).removeAttribute(filteredName(FILTER_NAME));
    verify(request, times(2)).setCharacterEncoding(ENCODING);
    verify(response, never()).setCharacterEncoding(ENCODING);
    verify(filterChain).doFilter(request, response);
  }

  private String filteredName(String prefix) {
    return prefix + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX;
  }

}