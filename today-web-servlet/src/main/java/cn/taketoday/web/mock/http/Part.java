/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.mock.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * <p>
 * This class represents a part or form item that was received within a <code>multipart/form-data</code> POST request.
 *
 * @since Servlet 3.0
 */
public interface Part {

  /**
   * Gets the content of this part as an <tt>InputStream</tt>
   *
   * @return The content of this part as an <tt>InputStream</tt>
   * @throws IOException If an error occurs in retrieving the content as an <tt>InputStream</tt>
   */
  public InputStream getInputStream() throws IOException;

  /**
   * Gets the content type of this part.
   *
   * @return The content type of this part.
   */
  public String getContentType();

  /**
   * Gets the name of this part
   *
   * @return The name of this part as a <tt>String</tt>
   */
  public String getName();

  /**
   * Gets the file name specified by the client
   *
   * @return the submitted file name
   * @since Servlet 3.1
   */
  public String getSubmittedFileName();

  /**
   * Returns the size of this file.
   *
   * @return a <code>long</code> specifying the size of this part, in bytes.
   */
  public long getSize();

  /**
   * A convenience method to write this uploaded item to disk.
   *
   * <p>
   * This method is not guaranteed to succeed if called more than once for the same part. This allows a particular
   * implementation to use, for example, file renaming, where possible, rather than copying all of the underlying data,
   * thus gaining a significant performance benefit.
   *
   * @param fileName The location into which the uploaded part should be stored. Relative paths are relative to
   * {@link cn.taketoday.web.mock.MultipartConfigElement#getLocation()}. Absolute paths are used as provided. Note: that this is
   * a system dependent string and URI notation may not be acceptable on all systems. For portability, this string should
   * be generated with the File or Path APIs.
   * @throws IOException if an error occurs.
   */
  public void write(String fileName) throws IOException;

  /**
   * Deletes the underlying storage for a file item, including deleting any associated temporary disk file.
   *
   * @throws IOException if an error occurs.
   */
  public void delete() throws IOException;

  /**
   * Returns the value of the specified mime header as a <code>String</code>. If the Part did not include a header of the
   * specified name, this method returns <code>null</code>. If there are multiple headers with the same name, this method
   * returns the first header in the part. The header name is case insensitive. You can use this method with any request
   * header.
   *
   * @param name a <code>String</code> specifying the header name
   * @return a <code>String</code> containing the value of the requested header, or <code>null</code> if the part does not
   * have a header of that name
   */
  public String getHeader(String name);

  /**
   * Gets the values of the Part header with the given name.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>Part</code>.
   *
   * <p>
   * Part header names are case insensitive.
   *
   * @param name the header name whose values to return
   * @return a (possibly empty) <code>Collection</code> of the values of the header with the given name
   */
  public Collection<String> getHeaders(String name);

  /**
   * Gets the header names of this Part.
   *
   * <p>
   * Some servlet containers do not allow servlets to access headers using this method, in which case this method returns
   * <code>null</code>
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>Part</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the header names of this Part
   */
  public Collection<String> getHeaderNames();

}
