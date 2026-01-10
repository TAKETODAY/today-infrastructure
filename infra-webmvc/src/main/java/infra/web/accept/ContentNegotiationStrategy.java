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

package infra.web.accept;

import java.util.Collections;
import java.util.List;

import infra.http.MediaType;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;

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
