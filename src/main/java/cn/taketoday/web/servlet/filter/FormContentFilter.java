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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.FormHttpMessageConverter;
import cn.taketoday.http.server.ServletServerHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@code Filter} that parses form data for HTTP PUT, PATCH, and DELETE requests
 * and exposes it as Servlet request parameters. By default the Servlet spec
 * only requires this for HTTP POST.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:42
 */
public class FormContentFilter extends OncePerRequestFilter {

  private static final List<String> HTTP_METHODS = Arrays.asList("PUT", "PATCH", "DELETE");

  private FormHttpMessageConverter formConverter = new AllEncompassingFormHttpMessageConverter();

  /**
   * Set the converter to use for parsing form content.
   * <p>By default this is an instance of {@link AllEncompassingFormHttpMessageConverter}.
   */
  public void setFormConverter(FormHttpMessageConverter converter) {
    Assert.notNull(converter, "FormHttpMessageConverter is required");
    this.formConverter = converter;
  }

  /**
   * The default character set to use for reading form data.
   * This is a shortcut for:<br>
   * {@code getFormConverter.setCharset(charset)}.
   */
  public void setCharset(Charset charset) {
    this.formConverter.setCharset(charset);
  }

  @Override
  protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    MultiValueMap<String, String> params = parseIfNecessary(request);
    if (!CollectionUtils.isEmpty(params)) {
      filterChain.doFilter(new FormContentRequestWrapper(request, params), response);
    }
    else {
      filterChain.doFilter(request, response);
    }
  }

  @Nullable
  private MultiValueMap<String, String> parseIfNecessary(HttpServletRequest request) throws IOException {
    if (!shouldParse(request)) {
      return null;
    }

    HttpInputMessage inputMessage = new ServletServerHttpRequest(request) {
      @Override
      public InputStream getBody() throws IOException {
        return request.getInputStream();
      }
    };
    return this.formConverter.read(null, inputMessage);
  }

  private boolean shouldParse(HttpServletRequest request) {
    String contentType = request.getContentType();
    String method = request.getMethod();
    if (StringUtils.isNotEmpty(contentType) && HTTP_METHODS.contains(method)) {
      try {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        return MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType);
      }
      catch (IllegalArgumentException ignored) { }
    }
    return false;
  }

  private static class FormContentRequestWrapper extends HttpServletRequestWrapper {

    private final MultiValueMap<String, String> formParams;

    public FormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> params) {
      super(request);
      this.formParams = params;
    }

    @Override
    @Nullable
    public String getParameter(String name) {
      String queryStringValue = super.getParameter(name);
      String formValue = this.formParams.getFirst(name);
      return (queryStringValue != null ? queryStringValue : formValue);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      Map<String, String[]> result = new LinkedHashMap<>();
      Enumeration<String> names = getParameterNames();
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        result.put(name, getParameterValues(name));
      }
      return result;
    }

    @Override
    public Enumeration<String> getParameterNames() {
      Set<String> names = new LinkedHashSet<>();
      names.addAll(Collections.list(super.getParameterNames()));
      names.addAll(this.formParams.keySet());
      return Collections.enumeration(names);
    }

    @Override
    @Nullable
    public String[] getParameterValues(String name) {
      String[] parameterValues = super.getParameterValues(name);
      List<String> formParam = this.formParams.get(name);
      if (formParam == null) {
        return parameterValues;
      }
      if (parameterValues == null || getQueryString() == null) {
        return StringUtils.toStringArray(formParam);
      }
      else {
        List<String> result = new ArrayList<>(parameterValues.length + formParam.size());
        result.addAll(Arrays.asList(parameterValues));
        result.addAll(formParam);
        return StringUtils.toStringArray(result);
      }
    }
  }

}

