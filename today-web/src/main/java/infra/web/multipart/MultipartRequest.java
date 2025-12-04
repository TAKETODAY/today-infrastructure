/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.multipart;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.http.HttpHeaders;
import infra.util.MultiValueMap;

/**
 * This interface defines the multipart request access operations that are exposed
 * for actual multipart requests.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NotMultipartRequestException
 * @see MultipartException
 * @since 4.0 2022/3/17 17:26
 */
public interface MultipartRequest {

  /**
   * Retrieve the parts of a multipart request
   *
   * @return the multipart data, mapping from name to part(s)
   * @throws NotMultipartRequestException if this request is not multipart request
   * @see infra.web.RequestContext#asMultipartRequest()
   * @see MultipartRequest#getParts()
   */
  MultiValueMap<String, Part> getParts();

  /**
   * Return the parts with the given name, or {@code null} if not found.
   *
   * @param name the name of the parts to retrieve
   * @return the parts with the given name, or {@code null} if not found
   * @see #getParts()
   */
  @Nullable
  List<Part> getParts(String name);

  /**
   * Return the first part with the given name, or {@code null} if not found.
   *
   * @param name the name of the part to retrieve
   * @return the first part with the given name, or {@code null} if not found
   * @see #getParts()
   */
  @Nullable
  Part getPart(String name);

  /**
   * Return an {@link java.lang.Iterable} of String objects containing the
   * parameter names of the parts contained in this request.
   *
   * @return the names of the parts
   * @see #getParts()
   */
  Iterable<String> getPartNames();

  /**
   * Return the headers for the specified part of the multipart request.
   * <p>If the underlying implementation supports access to part headers,
   * then all headers are returned. Otherwise, e.g. for a file upload, the
   * returned headers may expose a 'Content-Type' if available.
   */
  @Nullable
  HttpHeaders getHeaders(String paramOrFileName);

  /**
   * Cleanup any resources used for the multipart handling,
   * like a storage for the uploaded files.
   */
  void cleanup();

}
