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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.ContentDisposition;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.bind.NotMultipartRequestException;
import cn.taketoday.web.multipart.MaxUploadSizeExceededException;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.ServletPartMultipartFile;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * For Servlet Multipart
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 17:22
 */
public class ServletMultipartRequest extends AbstractMultipartRequest {

  private final HttpServletRequest request;

  /**
   * Create a new StandardMultipartHttpServletRequest wrapper for the given request,
   * immediately parsing the multipart content.
   *
   * @param request the servlet request to wrap
   * @throws MultipartException if parsing failed
   */
  public ServletMultipartRequest(HttpServletRequest request) throws MultipartException {
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
  public ServletMultipartRequest(HttpServletRequest request, boolean lazyParsing) throws MultipartException {
    this.request = request;
    if (!lazyParsing) {
      parseRequest(request);
    }
  }

  private MultiValueMap<String, MultipartFile> parseRequest(HttpServletRequest request) {
    try {
      Collection<Part> parts = request.getParts();
      LinkedMultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>(parts.size());

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
      }
      return files;
    }
    catch (IOException e) {
      throw new MultipartException("MultipartFile parsing failed.", e);
    }
    catch (ServletException e) {
      throw new NotMultipartRequestException("This is not a multipart request", e);
    }
    catch (Throwable ex) {
      String msg = ex.getMessage();
      if (msg != null && msg.contains("size") && msg.contains("exceed")) {
        throw new MaxUploadSizeExceededException(-1, ex);
      }
      throw new MultipartException("Failed to parse multipart servlet request", ex);
    }
  }

  @Override
  public HttpMethod getRequestMethod() {
    return null;
  }

  @Override
  public HttpHeaders getRequestHeaders() {
    return null;
  }

  @Override
  protected MultiValueMap<String, MultipartFile> parseRequest() {
    return parseRequest(request);
  }

  @Override
  public String getMultipartContentType(String paramOrFileName) {
    try {
      Part part = request.getPart(paramOrFileName);
      return part != null ? part.getContentType() : null;
    }
    catch (Throwable ex) {
      throw new MultipartException("Could not access multipart servlet request", ex);
    }
  }

  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    try {
      Part part = request.getPart(paramOrFileName);
      if (part != null) {
        HttpHeaders headers = HttpHeaders.create();
        for (String headerName : part.getHeaderNames()) {
          headers.addAll(headerName, part.getHeaders(headerName));
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
