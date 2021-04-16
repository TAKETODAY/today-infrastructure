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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cn.taketoday.context.utils.MediaType;
import cn.taketoday.web.Constant;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.resolver.MultipartFileParsingException;
import io.undertow.server.handlers.proxy.mod_cluster.VirtualHost;

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
public class MultipartIterator implements Iterator<RequestPart> {
  protected final LightHttpConfig config;

  protected final MultipartInputStream in;
  protected boolean next;

  /**
   * Creates a new MultipartIterator from the given request.
   *
   * @param req
   *         the multipart/form-data request
   * @param config
   *         light http config
   */
  public MultipartIterator(final HttpRequest req, LightHttpConfig config) {
    this.config = config;
    final HttpHeaders headers = req.getHeaders();
    final MediaType contentType = headers.getContentType();
    if (!contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)) {
      throw new IllegalArgumentException("Content-Type is not multipart/form-data");
    }
    final String boundary = contentType.getParameter("boundary"); // should be US-ASCII
    if (boundary == null)
      throw new IllegalArgumentException("Content-Type is missing boundary");
    in = new MultipartInputStream(req.getBody(), boundary.getBytes()); // todo charset
  }

  public boolean hasNext() {
    try {
      return next || (next = in.nextPart());
    }
    catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * @throws cn.taketoday.web.resolver.NotMultipartRequestException
   *         if this request is not of type multipart/form-data
   * @throws cn.taketoday.web.resolver.MultipartFileParsingException
   *         multipart parse failed
   */
  @Override
  public RequestPart next() throws MultipartFileParsingException {
    if (!hasNext())
      throw new NoSuchElementException();
    next = false;
    try {
      // 先解析 header
      final HttpHeaders httpHeaders = Utils.readHeaders(in, config);
      final int len = in.tail - in.head;
      byte[] bytes = new byte[len]; // TODO 防止过大
      in.read(bytes);
      if (httpHeaders.containsKey(Constant.CONTENT_TYPE)) {
        return new LightMultipartFile(new ByteArrayInputStream(bytes), httpHeaders);
      }
      return new RequestPart(new ByteArrayInputStream(bytes), httpHeaders);
    }
    catch (IOException e) {
      throw new MultipartFileParsingException(e);
    }
  }

}
