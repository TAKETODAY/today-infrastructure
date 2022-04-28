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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.ContentDisposition;
import cn.taketoday.http.FileSizeExceededException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.ServletPartMultipartFile;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * Spring MultipartHttpServletRequest adapter, wrapping a Servlet HttpServletRequest
 * and its Part objects. Parameters get exposed through the native request's getParameter
 * methods - without any custom processing on our side.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

  @Nullable
  private Set<String> multipartParameterNames;

  /**
   * Create a new StandardMultipartHttpServletRequest wrapper for the given request,
   * immediately parsing the multipart content.
   *
   * @param request the servlet request to wrap
   * @throws MultipartException if parsing failed
   */
  public StandardMultipartHttpServletRequest(HttpServletRequest request) throws MultipartException {
    this(request, false);
  }

  /**
   * Create a new StandardMultipartHttpServletRequest wrapper for the given request.
   *
   * @param request the servlet request to wrap
   * @param lazyParsing whether multipart parsing should be triggered lazily on
   * first access of multipart files or parameters
   * @throws MultipartException if an immediate parsing attempt failed
   */
  public StandardMultipartHttpServletRequest(HttpServletRequest request, boolean lazyParsing)
          throws MultipartException {

    super(request);
    if (!lazyParsing) {
      parseRequest(request);
    }
  }

  private void parseRequest(HttpServletRequest request) {
    try {
      Collection<Part> parts = request.getParts();
      this.multipartParameterNames = new LinkedHashSet<>(parts.size());
      MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>(parts.size());
      for (Part part : parts) {
        String headerValue = part.getHeader(HttpHeaders.CONTENT_DISPOSITION);
        ContentDisposition disposition = ContentDisposition.parse(headerValue);
        String filename = disposition.getFilename();
        if (filename != null) {
          if (filename.startsWith("=?") && filename.endsWith("?=")) {
            filename = MimeDelegate.decode(filename);
          }
          files.add(part.getName(), new ServletPartMultipartFile(part, filename));
        }
        else {
          this.multipartParameterNames.add(part.getName());
        }
      }
      setMultipartFiles(files);
    }
    catch (Throwable ex) {
      handleParseFailure(ex);
    }
  }

  protected void handleParseFailure(Throwable ex) {
    String msg = ex.getMessage();
    if (msg != null && msg.contains("size") && msg.contains("exceed")) {
      throw new FileSizeExceededException(-1, ex);
    }
    throw new MultipartException("Failed to parse multipart servlet request", ex);
  }

  @Override
  protected void initializeMultipart() {
    parseRequest(getRequest());
  }

  @Override
  public Enumeration<String> getParameterNames() {
    if (this.multipartParameterNames == null) {
      initializeMultipart();
    }
    if (this.multipartParameterNames.isEmpty()) {
      return super.getParameterNames();
    }

    // Servlet getParameterNames() not guaranteed to include multipart form items
    // (e.g. on WebLogic 12) -> need to merge them here to be on the safe side
    Set<String> paramNames = new LinkedHashSet<>();
    Enumeration<String> paramEnum = super.getParameterNames();
    while (paramEnum.hasMoreElements()) {
      paramNames.add(paramEnum.nextElement());
    }
    paramNames.addAll(this.multipartParameterNames);
    return Collections.enumeration(paramNames);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    if (this.multipartParameterNames == null) {
      initializeMultipart();
    }
    if (this.multipartParameterNames.isEmpty()) {
      return super.getParameterMap();
    }

    // Servlet getParameterMap() not guaranteed to include multipart form items
    // (e.g. on WebLogic 12) -> need to merge them here to be on the safe side
    Map<String, String[]> paramMap = new LinkedHashMap<>(super.getParameterMap());
    for (String paramName : this.multipartParameterNames) {
      if (!paramMap.containsKey(paramName)) {
        paramMap.put(paramName, getParameterValues(paramName));
      }
    }
    return paramMap;
  }

  @Override
  public String getMultipartContentType(String paramOrFileName) {
    try {
      Part part = getPart(paramOrFileName);
      return (part != null ? part.getContentType() : null);
    }
    catch (Throwable ex) {
      throw new MultipartException("Could not access multipart servlet request", ex);
    }
  }

  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    try {
      Part part = getPart(paramOrFileName);
      if (part != null) {
        HttpHeaders headers = HttpHeaders.create();
        for (String headerName : part.getHeaderNames()) {
          headers.put(headerName, new ArrayList<>(part.getHeaders(headerName)));
        }
        return headers;
      }
      else {
        return null;
      }
    }
    catch (Throwable ex) {
      throw new MultipartException("Could not access multipart servlet request", ex);
    }
  }

  /**
   * Inner class to avoid a hard dependency on the JavaMail API.
   */
  private static class MimeDelegate {

    public static String decode(String value) {
      try {
        return MimeUtility.decodeText(value);
      }
      catch (UnsupportedEncodingException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

}
