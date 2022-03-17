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

package cn.taketoday.web.multipart.support;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Default implementation of the
 * {@link cn.taketoday.web.multipart.MultipartHttpServletRequest}
 * interface. Provides management of pre-generated parameter values.
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/17 17:28
 */
public class DefaultMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

  private static final String CONTENT_TYPE = "Content-Type";

  @Nullable
  private Map<String, String[]> multipartParameters;

  @Nullable
  private Map<String, String> multipartParameterContentTypes;

  /**
   * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
   *
   * @param request the servlet request to wrap
   * @param mpFiles a map of the multipart files
   * @param mpParams a map of the parameters to expose,
   * with Strings as keys and String arrays as values
   */
  public DefaultMultipartHttpServletRequest(HttpServletRequest request, MultiValueMap<String, MultipartFile> mpFiles,
          Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

    super(request);
    setMultipartFiles(mpFiles);
    setMultipartParameters(mpParams);
    setMultipartParameterContentTypes(mpParamContentTypes);
  }

  /**
   * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
   *
   * @param request the servlet request to wrap
   */
  public DefaultMultipartHttpServletRequest(HttpServletRequest request) {
    super(request);
  }

  @Override
  @Nullable
  public String getParameter(String name) {
    String[] values = getMultipartParameters().get(name);
    if (values != null) {
      return (values.length > 0 ? values[0] : null);
    }
    return super.getParameter(name);
  }

  @Override
  public String[] getParameterValues(String name) {
    String[] parameterValues = super.getParameterValues(name);
    String[] mpValues = getMultipartParameters().get(name);
    if (mpValues == null) {
      return parameterValues;
    }
    if (parameterValues == null || getQueryString() == null) {
      return mpValues;
    }
    else {
      String[] result = new String[mpValues.length + parameterValues.length];
      System.arraycopy(mpValues, 0, result, 0, mpValues.length);
      System.arraycopy(parameterValues, 0, result, mpValues.length, parameterValues.length);
      return result;
    }
  }

  @Override
  public Enumeration<String> getParameterNames() {
    Map<String, String[]> multipartParameters = getMultipartParameters();
    if (multipartParameters.isEmpty()) {
      return super.getParameterNames();
    }

    Set<String> paramNames = new LinkedHashSet<>();
    paramNames.addAll(Collections.list(super.getParameterNames()));
    paramNames.addAll(multipartParameters.keySet());
    return Collections.enumeration(paramNames);
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
  public String getMultipartContentType(String paramOrFileName) {
    MultipartFile file = getFile(paramOrFileName);
    if (file != null) {
      return file.getContentType();
    }
    else {
      return getMultipartParameterContentTypes().get(paramOrFileName);
    }
  }

  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    String contentType = getMultipartContentType(paramOrFileName);
    if (contentType != null) {
      HttpHeaders headers = HttpHeaders.create();
      headers.add(CONTENT_TYPE, contentType);
      return headers;
    }
    else {
      return null;
    }
  }

  /**
   * Set a Map with parameter names as keys and String array objects as values.
   * To be invoked by subclasses on initialization.
   */
  protected final void setMultipartParameters(Map<String, String[]> multipartParameters) {
    this.multipartParameters = multipartParameters;
  }

  /**
   * Obtain the multipart parameter Map for retrieval,
   * lazily initializing it if necessary.
   *
   * @see #initializeMultipart()
   */
  protected Map<String, String[]> getMultipartParameters() {
    if (this.multipartParameters == null) {
      initializeMultipart();
    }
    return this.multipartParameters;
  }

  /**
   * Set a Map with parameter names as keys and content type Strings as values.
   * To be invoked by subclasses on initialization.
   */
  protected final void setMultipartParameterContentTypes(Map<String, String> multipartParameterContentTypes) {
    this.multipartParameterContentTypes = multipartParameterContentTypes;
  }

  /**
   * Obtain the multipart parameter content type Map for retrieval,
   * lazily initializing it if necessary.
   *
   * @see #initializeMultipart()
   */
  protected Map<String, String> getMultipartParameterContentTypes() {
    if (this.multipartParameterContentTypes == null) {
      initializeMultipart();
    }
    return this.multipartParameterContentTypes;
  }

}
