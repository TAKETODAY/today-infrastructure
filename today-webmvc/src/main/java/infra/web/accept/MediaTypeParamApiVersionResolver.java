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

import org.jspecify.annotations.Nullable;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.web.RequestContext;

/**
 * {@link ApiVersionResolver} that extracts the version from a media type
 * parameter found in the Accept or Content-Type headers.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class MediaTypeParamApiVersionResolver implements ApiVersionResolver {

  private final MediaType compatibleMediaType;

  private final String parameterName;

  /**
   * Create an instance.
   *
   * @param compatibleMediaType the media type to extract the parameter from with
   * the match established via {@link MediaType#isCompatibleWith(MediaType)}
   * @param paramName the name of the parameter
   */
  public MediaTypeParamApiVersionResolver(MediaType compatibleMediaType, String paramName) {
    this.compatibleMediaType = compatibleMediaType;
    this.parameterName = paramName;
  }

  @Nullable
  @Override
  public String resolveVersion(RequestContext request) {
    for (String header : request.getHeaders(HttpHeaders.ACCEPT)) {
      for (MediaType mediaType : MediaType.parseMediaTypes(header)) {
        if (compatibleMediaType.isCompatibleWith(mediaType)) {
          return mediaType.getParameter(parameterName);
        }
      }
    }

    MediaType contentType = request.getContentType();
    if (contentType != null) {
      if (compatibleMediaType.isCompatibleWith(contentType)) {
        return contentType.getParameter(parameterName);
      }
    }
    return null;
  }

}
