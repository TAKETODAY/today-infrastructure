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

import java.util.Map;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

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
