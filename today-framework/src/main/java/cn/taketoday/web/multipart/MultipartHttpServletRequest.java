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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.multipart;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Provides additional methods for dealing with multipart content within a
 * servlet request, allowing to access uploaded files.
 *
 * <p>Implementations also need to override the standard
 * {@link jakarta.servlet.ServletRequest} methods for parameter access, making
 * multipart parameters available.
 *
 * <p>A concrete implementation is
 * {@link cn.taketoday.web.multipart.support.DefaultMultipartHttpServletRequest}.
 * As an intermediate step,
 * {@link cn.taketoday.web.multipart.support.AbstractMultipartHttpServletRequest}
 * can be subclassed.
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MultipartFile
 * @see jakarta.servlet.http.HttpServletRequest#getParameter
 * @see jakarta.servlet.http.HttpServletRequest#getParameterNames
 * @see jakarta.servlet.http.HttpServletRequest#getParameterMap
 * @see cn.taketoday.web.multipart.support.DefaultMultipartHttpServletRequest
 * @see cn.taketoday.web.multipart.support.AbstractMultipartHttpServletRequest
 * @since 4.0 2022/3/17 17:25
 */
public interface MultipartHttpServletRequest extends HttpServletRequest, MultipartRequest {

  /**
   * Return this request's method as a convenient HttpMethod instance.
   */
  HttpMethod getRequestMethod();

  /**
   * Return this request's headers as a convenient HttpHeaders instance.
   */
  HttpHeaders getRequestHeaders();

  /**
   * Return the headers for the specified part of the multipart request.
   * <p>If the underlying implementation supports access to part headers,
   * then all headers are returned. Otherwise, e.g. for a file upload, the
   * returned headers may expose a 'Content-Type' if available.
   */
  @Nullable
  HttpHeaders getMultipartHeaders(String paramOrFileName);

}
