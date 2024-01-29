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

package cn.taketoday.web.accept;

import java.util.Collections;
import java.util.List;

import cn.taketoday.http.MediaType;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;

/**
 * A strategy for resolving the requested media types for a request.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface ContentNegotiationStrategy {

  /**
   * A singleton list with {@link MediaType#ALL} that is returned from
   * {@link #resolveMediaTypes} when no specific media types are requested.
   */
  List<MediaType> MEDIA_TYPE_ALL_LIST = Collections.singletonList(MediaType.ALL);

  /**
   * Resolve the given request to a list of media types. The returned list is
   * ordered by specificity first and by quality parameter second.
   *
   * @param context the current request context
   * @return the requested media types, or {@link #MEDIA_TYPE_ALL_LIST} if none
   * were requested.
   * @throws HttpMediaTypeNotAcceptableException if the requested media
   * types cannot be parsed
   */
  List<MediaType> resolveMediaTypes(RequestContext context)
          throws HttpMediaTypeNotAcceptableException;

}
