/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
   *
   * @param name the name of the part to retrieve headers for
   * @return the headers for the specified part, or {@code null} if not found
   */
  @Nullable
  HttpHeaders getHeaders(String name);

  /**
   * Check if a part with the given name exists.
   *
   * @param name the name of the part to check
   * @return {@code true} if a part with the given name exists, {@code false} otherwise
   * @see #getParts()
   */
  default boolean contains(String name) {
    return getParts().containsKey(name);
  }

  /**
   * Cleanup any resources used for the multipart handling,
   * like a storage for the uploaded files.
   */
  void cleanup();

}
