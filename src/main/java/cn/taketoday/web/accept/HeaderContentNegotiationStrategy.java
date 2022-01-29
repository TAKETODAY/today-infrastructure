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

package cn.taketoday.web.accept;

import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.InvalidMediaTypeException;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;

/**
 * A {@code ContentNegotiationStrategy} that checks the 'Accept' request header.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class HeaderContentNegotiationStrategy implements ContentNegotiationStrategy {

  /**
   * {@inheritDoc}
   *
   * @return
   * @throws HttpMediaTypeNotAcceptableException if the 'Accept' header cannot be parsed
   */
  @Override
  public List<MediaType> resolveMediaTypes(RequestContext request)
          throws HttpMediaTypeNotAcceptableException {
    List<String> headerValues = request.requestHeaders().get(HttpHeaders.ACCEPT);
    if (headerValues == null) {
      return MEDIA_TYPE_ALL_LIST;
    }

    try {
      List<MediaType> mediaTypes = MediaType.parseMediaTypes(headerValues);
      MimeTypeUtils.sortBySpecificity(mediaTypes);
      return CollectionUtils.isNotEmpty(mediaTypes) ? mediaTypes : MEDIA_TYPE_ALL_LIST;
    }
    catch (InvalidMediaTypeException ex) {
      throw new HttpMediaTypeNotAcceptableException(
              "Could not parse 'Accept' header " + headerValues + ": " + ex.getMessage());
    }
  }

}
