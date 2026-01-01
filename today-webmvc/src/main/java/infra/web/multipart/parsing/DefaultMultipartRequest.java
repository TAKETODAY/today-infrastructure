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

package infra.web.multipart.parsing;

import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.multipart.AbstractMultipartRequest;
import infra.web.multipart.Part;

/**
 * Default implementation of {@link infra.web.multipart.MultipartRequest}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 21:39
 */
final class DefaultMultipartRequest extends AbstractMultipartRequest {

  private final DefaultMultipartParser multipartParser;

  private final RequestContext context;

  public DefaultMultipartRequest(DefaultMultipartParser multipartParser, RequestContext context) {
    this.multipartParser = multipartParser;
    this.context = context;
  }

  @Override
  protected MultiValueMap<String, Part> parseRequest() {
    return multipartParser.parseRequest(context);
  }
}
