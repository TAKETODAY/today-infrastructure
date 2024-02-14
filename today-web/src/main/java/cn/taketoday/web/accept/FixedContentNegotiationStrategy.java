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
import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;

/**
 * A {@code ContentNegotiationStrategy} that returns a fixed content type.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FixedContentNegotiationStrategy implements ContentNegotiationStrategy {

  private final List<MediaType> contentTypes;

  /**
   * Constructor with a single default {@code MediaType}.
   */
  public FixedContentNegotiationStrategy(MediaType contentType) {
    this(Collections.singletonList(contentType));
  }

  /**
   * Constructor with an ordered List of default {@code MediaType}'s to return
   * for use in applications that support a variety of content types.
   * <p>Consider appending {@link MediaType#ALL} at the end if destinations
   * are present which do not support any of the other default media types.
   */
  public FixedContentNegotiationStrategy(List<MediaType> contentTypes) {
    Assert.notNull(contentTypes, "'contentTypes' is required");
    this.contentTypes = Collections.unmodifiableList(contentTypes);
  }

  /**
   * Return the configured list of media types.
   */
  public List<MediaType> getContentTypes() {
    return this.contentTypes;
  }

  @Override
  public List<MediaType> resolveMediaTypes(RequestContext request) {
    return this.contentTypes;
  }

}
