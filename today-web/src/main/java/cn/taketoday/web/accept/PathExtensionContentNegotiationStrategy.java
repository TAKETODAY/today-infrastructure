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

import java.util.Locale;
import java.util.Map;

import cn.taketoday.core.io.Resource;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.UriUtils;

/**
 * A {@code ContentNegotiationStrategy} that resolves the file extension in the
 * request path to a key to be used to look up a media type.
 *
 * <p>If the file extension is not found in the explicit registrations provided
 * to the constructor, the {@link MediaType#fromFileName(String)} is used as a fallback
 * mechanism.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/7 13:59
 */
public class PathExtensionContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

  /**
   * Create an instance without any mappings to start with. Mappings may be added
   * later on if any extensions are resolved through the Java Activation framework.
   */
  public PathExtensionContentNegotiationStrategy() {
    this(null);
  }

  /**
   * Create an instance with the given map of file extensions and media types.
   */
  public PathExtensionContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
    super(mediaTypes);
    setUseRegisteredExtensionsOnly(false);
    setIgnoreUnknownExtensions(true);

  }

  @Override
  @Nullable
  protected String getMediaTypeKey(RequestContext request) {
    String extension = UriUtils.extractFileExtension(request.getRequestURI());
    return StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ENGLISH) : null;
  }

  /**
   * A public method exposing the knowledge of the path extension strategy to
   * resolve file extensions to a {@link MediaType} in this case for a given
   * {@link Resource}. The method first looks up any explicitly registered
   * file extensions first and then falls back on {@link MediaType#fromFileName(String)} if available.
   *
   * @param resource the resource to look up
   * @return the MediaType for the extension, or {@code null} if none found
   */
  @Nullable
  public MediaType getMediaTypeForResource(Resource resource) {
    Assert.notNull(resource, "Resource is required");
    String filename = resource.getName();

    MediaType mediaType = null;
    String extension = StringUtils.getFilenameExtension(filename);
    if (extension != null) {
      mediaType = lookupMediaType(extension);
    }
    if (mediaType == null) {
      mediaType = MediaType.fromFileName(filename);
    }
    return mediaType;
  }

}

