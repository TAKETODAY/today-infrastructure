/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.framework.server.light;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import cn.taketoday.context.utils.MediaType;
import cn.taketoday.web.http.ContentDisposition;
import cn.taketoday.web.http.HttpHeaders;

/**
 * The {@code MultipartIterator} iterates over the parts of a multipart/form-data request.
 * <p>
 * For example, to support file upload from a web browser:
 * <ol>
 * <li>Create an HTML form which includes an input field of type "file", attributes
 *     method="post" and enctype="multipart/form-data", and an action URL of your choice,
 *     for example action="/upload". This form can be served normally like any other
 *     resource, e.g. from an HTML file on disk.
 * <li>Add a context handler for the action path ("/upload" in this example), using either
 *     the explicit {@link VirtualHost#addContext} method or the {@link Context} annotation.
 * <li>In the context handler implementation, construct a {@code MultipartIterator} from
 *     the client {@code Request}.
 * <li>Iterate over the form {@link Part}s, processing each named field as appropriate -
 *     for the file input field, read the uploaded file using the body input stream.
 * </ol>
 *
 * @author TODAY 2021/4/13 10:53
 */
public class MultipartIterator implements Iterator<Part> {

  protected final MultipartInputStream in;
  protected boolean next;

  /**
   * Creates a new MultipartIterator from the given request.
   *
   * @param req
   *         the multipart/form-data request
   *
   * @throws IOException
   *         if an IO error occurs
   * @throws IllegalArgumentException
   *         if the given request's content type
   *         is not multipart/form-data, or is missing the boundary
   */
  public MultipartIterator(LightRequest req) throws IOException {
    final HttpHeaders headers = req.getHeaders();
    final MediaType contentType = headers.getContentType();
    if (!contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)) {
      throw new IllegalArgumentException("Content-Type is not multipart/form-data");
    }
    final String boundary = contentType.getParameter("boundary"); // should be US-ASCII
    if (boundary == null)
      throw new IllegalArgumentException("Content-Type is missing boundary");
    in = new MultipartInputStream(req.getBody(), Utils.getBytes(boundary));
  }

  public boolean hasNext() {
    try {
      return next || (next = in.nextPart());
    }
    catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public Part next() {
    if (!hasNext())
      throw new NoSuchElementException();
    next = false;
    Part p = new Part();
    try {
      p.headers = Utils.readHeaders(in);
    }
    catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    final ContentDisposition contentDisposition = p.headers.getContentDisposition();
    p.name = contentDisposition.getName();
    p.filename = contentDisposition.getFilename();
    p.body = in;
    return p;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
