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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.bind.NotMultipartRequestException;

/**
 * This interface defines the multipart request access operations that are exposed
 * for actual multipart requests.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NotMultipartRequestException
 * @since 4.0 2022/3/17 17:26
 */
public interface MultipartRequest {

  /**
   * Retrieve the parts of a multipart request, provided the Content-Type is
   * {@code "multipart/form-data"}, or throw an exception otherwise.
   *
   * @return the multipart data, mapping from name to part(s)
   * @throws IOException if an I/O error occurred during the retrieval
   * @throws NotMultipartRequestException if this request is not multipart request
   * @see RequestContext#asMultipartRequest()
   * @see MultipartRequest#getParts()
   */
  MultiValueMap<String, Part> getParts() throws IOException;

  @Nullable
  List<Part> getParts(String name) throws IOException;

  @Nullable
  Part getPart(String name) throws IOException;

  Iterable<String> getPartNames();

  /**
   * Return a {@link MultiValueMap} of the multipart files contained in this request.
   *
   * @return a map containing the parameter names as keys, and a list of
   * {@link MultipartFile} objects as values
   * @throws NotMultipartRequestException if this request is not multipart request
   * @see #getParts()
   */
  MultiValueMap<String, MultipartFile> getFiles();

  /**
   * Return the contents plus description of uploaded files in this request,
   * or an empty list if it does not exist.
   *
   * @param name a String specifying the parameter name of the multipart file
   * @return the uploaded content in the form of a {@link MultipartFile} list
   * @throws NotMultipartRequestException if this request is not multipart request
   * @see #getParts()
   */
  @Nullable
  List<MultipartFile> getFiles(String name);

  /**
   * Return the contents plus description of an uploaded file in this request,
   * or {@code null} if it does not exist.
   *
   * @param name a String specifying the parameter name of the multipart file
   * @return the uploaded content in the form of a {@link MultipartFile} object
   * @throws NotMultipartRequestException if this request is not multipart request
   * @see #getParts()
   */
  @Nullable
  MultipartFile getFile(String name);

  /**
   * Return an {@link java.lang.Iterable} of String objects containing the
   * parameter names of the multipart files contained in this request. These
   * are the field names of the form (like with normal parameters), not the
   * original file names.
   *
   * @return the names of the files
   */
  Iterable<String> getFileNames();

  /**
   * Return a {@link java.util.Map} of the multipart files contained in this request.
   *
   * @return a map containing the parameter names as keys, and the
   * {@link MultipartFile} objects as values
   */
  Map<String, MultipartFile> getFileMap();

  /**
   * Return the headers for the specified part of the multipart request.
   * <p>If the underlying implementation supports access to part headers,
   * then all headers are returned. Otherwise, e.g. for a file upload, the
   * returned headers may expose a 'Content-Type' if available.
   */
  @Nullable
  HttpHeaders getMultipartHeaders(String paramOrFileName);

  /**
   * Cleanup any resources used for the multipart handling,
   * like a storage for the uploaded files.
   */
  void cleanup();

}
