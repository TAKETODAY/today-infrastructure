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

import java.util.Map;

import infra.http.MediaType;
import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * Strategy that resolves the requested content type from a query parameter.
 * The default query parameter name is {@literal "format"}.
 *
 * <p>You can register static mappings between keys (i.e. the expected value of
 * the query parameter) and MediaType's via {@link #addMapping(String, MediaType)}.
 * this strategy also supports dynamic lookups of keys via
 * {@link MediaType#fromFileName(String)}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ParameterContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

  private String parameterName = "format";

  /**
   * Create an instance with the given map of file extensions and media types.
   */
  public ParameterContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
    super(mediaTypes);
  }

  /**
   * Set the name of the parameter to use to determine requested media types.
   * <p>By default this is set to {@code "format"}.
   */
  public void setParameterName(String parameterName) {
    Assert.notNull(parameterName, "'parameterName' is required");
    this.parameterName = parameterName;
  }

  public String getParameterName() {
    return this.parameterName;
  }

  @Override
  @Nullable
  protected String getMediaTypeKey(RequestContext request) {
    return request.getParameter(getParameterName());
  }

}
