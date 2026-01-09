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

import java.util.Locale;
import java.util.Map;

import infra.core.io.Resource;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.util.UriUtils;

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
    return StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ROOT) : null;
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

