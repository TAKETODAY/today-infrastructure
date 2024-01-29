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
import java.util.Map;
import java.util.Optional;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.MediaTypeFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;

/**
 * Base class for {@code ContentNegotiationStrategy} implementations with the
 * steps to resolve a request to media types.
 *
 * <p>First a key (e.g. "json", "pdf") must be extracted from the request (e.g.
 * file extension, query param). The key must then be resolved to media type(s)
 * through the base class {@link MappingMediaTypeFileExtensionResolver} which
 * stores such mappings.
 *
 * <p>The method {@link #handleNoMatch} allow sub-classes to plug in additional
 * ways of looking up media types (e.g. through the Java Activation framework,
 * or {@link jakarta.servlet.ServletContext#getMimeType}. Media types resolved
 * via base classes are then added to the base class
 * {@link MappingMediaTypeFileExtensionResolver}, i.e. cached for new lookups.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractMappingContentNegotiationStrategy
        extends MappingMediaTypeFileExtensionResolver implements ContentNegotiationStrategy {

  private boolean useRegisteredExtensionsOnly = false;

  private boolean ignoreUnknownExtensions = false;

  /**
   * Create an instance with the given map of file extensions and media types.
   */
  public AbstractMappingContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
    super(mediaTypes);
  }

  /**
   * Whether to only use the registered mappings to look up file extensions,
   * or also to use dynamic resolution (e.g. via {@link MediaType}.
   * <p>By default this is set to {@code false}.
   */
  public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
    this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
  }

  public boolean isUseRegisteredExtensionsOnly() {
    return this.useRegisteredExtensionsOnly;
  }

  /**
   * Whether to ignore requests with unknown file extension. Setting this to
   * {@code false} results in {@code HttpMediaTypeNotAcceptableException}.
   * <p>By default this is set to {@literal false}.
   */
  public void setIgnoreUnknownExtensions(boolean ignoreUnknownExtensions) {
    this.ignoreUnknownExtensions = ignoreUnknownExtensions;
  }

  public boolean isIgnoreUnknownExtensions() {
    return this.ignoreUnknownExtensions;
  }

  @Override
  public List<MediaType> resolveMediaTypes(RequestContext context)
          throws HttpMediaTypeNotAcceptableException {

    return resolveMediaTypeKey(context, getMediaTypeKey(context));
  }

  /**
   * An alternative to {@link #resolveMediaTypes(RequestContext)} that accepts
   * an already extracted key.
   */
  public List<MediaType> resolveMediaTypeKey(RequestContext webRequest, @Nullable String key)
          throws HttpMediaTypeNotAcceptableException {

    if (StringUtils.hasText(key)) {
      MediaType mediaType = lookupMediaType(key);
      if (mediaType != null) {
        handleMatch(key, mediaType);
        return Collections.singletonList(mediaType);
      }
      mediaType = handleNoMatch(webRequest, key);
      if (mediaType != null) {
        addMapping(key, mediaType);
        return Collections.singletonList(mediaType);
      }
    }
    return MEDIA_TYPE_ALL_LIST;
  }

  /**
   * Extract a key from the request to use to look up media types.
   *
   * @return the lookup key, or {@code null} if none
   */
  @Nullable
  protected abstract String getMediaTypeKey(RequestContext request);

  /**
   * Override to provide handling when a key is successfully resolved via
   * {@link #lookupMediaType}.
   */
  protected void handleMatch(String key, MediaType mediaType) { }

  /**
   * Override to provide handling when a key is not resolved via.
   * {@link #lookupMediaType}. Sub-classes can take further steps to
   * determine the media type(s). If a MediaType is returned from
   * this method it will be added to the cache in the base class.
   */
  @Nullable
  protected MediaType handleNoMatch(RequestContext request, String key)
          throws HttpMediaTypeNotAcceptableException {

    if (!isUseRegisteredExtensionsOnly()) {
      Optional<MediaType> mediaType = MediaTypeFactory.getMediaType("file." + key);
      if (mediaType.isPresent()) {
        return mediaType.get();
      }
    }
    if (isIgnoreUnknownExtensions()) {
      return null;
    }
    throw new HttpMediaTypeNotAcceptableException(getAllMediaTypes());
  }

}
