/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.accept;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;

/**
 * Central class to determine requested {@linkplain MediaType media types}
 * for a request. This is done by delegating to a list of configured
 * {@code ContentNegotiationStrategy} instances.
 *
 * <p>Also provides methods to look up file extensions for a media type.
 * This is done by delegating to the list of configured
 * {@code MediaTypeFileExtensionResolver} instances.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {

  private final ArrayList<ContentNegotiationStrategy> strategies = new ArrayList<>();

  private final LinkedHashSet<MediaTypeFileExtensionResolver> resolvers = new LinkedHashSet<>();

  /**
   * Create an instance with the given list of
   * {@code ContentNegotiationStrategy} strategies each of which may also be
   * an instance of {@code MediaTypeFileExtensionResolver}.
   *
   * @param strategies the strategies to use
   */
  public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
    this(Arrays.asList(strategies));
  }

  /**
   * A collection-based alternative to
   * {@link #ContentNegotiationManager(ContentNegotiationStrategy...)}.
   *
   * @param strategies the strategies to use
   */
  public ContentNegotiationManager(Collection<ContentNegotiationStrategy> strategies) {
    Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
    this.strategies.addAll(strategies);
    for (ContentNegotiationStrategy strategy : this.strategies) {
      if (strategy instanceof MediaTypeFileExtensionResolver) {
        this.resolvers.add((MediaTypeFileExtensionResolver) strategy);
      }
    }
  }

  /**
   * Create a default instance with a {@link HeaderContentNegotiationStrategy}.
   */
  public ContentNegotiationManager() {
    this(new HeaderContentNegotiationStrategy());
  }

  /**
   * Return the configured content negotiation strategies.
   */
  public List<ContentNegotiationStrategy> getStrategies() {
    return this.strategies;
  }

  /**
   * Find a {@code ContentNegotiationStrategy} of the given type.
   *
   * @param strategyType the strategy type
   * @return the first matching strategy, or {@code null} if none
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T extends ContentNegotiationStrategy> T getStrategy(Class<T> strategyType) {
    for (ContentNegotiationStrategy strategy : getStrategies()) {
      if (strategyType.isInstance(strategy)) {
        return (T) strategy;
      }
    }
    return null;
  }

  /**
   * Register more {@code MediaTypeFileExtensionResolver} instances in addition
   * to those detected at construction.
   *
   * @param resolvers the resolvers to add
   */
  public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
    Collections.addAll(this.resolvers, resolvers);
  }

  @Override
  public List<MediaType> resolveMediaTypes(RequestContext request) throws HttpMediaTypeNotAcceptableException {
    for (ContentNegotiationStrategy strategy : this.strategies) {
      List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);
      if (mediaTypes.equals(MEDIA_TYPE_ALL_LIST)) {
        continue;
      }
      return mediaTypes;
    }
    return MEDIA_TYPE_ALL_LIST;
  }

  @Override
  public List<String> resolveFileExtensions(MediaType mediaType) {
    return doResolveExtensions(resolver -> resolver.resolveFileExtensions(mediaType));
  }

  /**
   * {@inheritDoc}
   * <p>At startup this method returns extensions explicitly registered with
   * {@link ParameterContentNegotiationStrategy}.
   * {@link MediaType} and cached.
   */
  @Override
  public List<String> getAllFileExtensions() {
    return doResolveExtensions(MediaTypeFileExtensionResolver::getAllFileExtensions);
  }

  private List<String> doResolveExtensions(Function<MediaTypeFileExtensionResolver, List<String>> extractor) {
    ArrayList<String> result = null;
    for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
      List<String> extensions = extractor.apply(resolver);
      if (CollectionUtils.isEmpty(extensions)) {
        continue;
      }
      if (result == null) {
        result = new ArrayList<>(4);
      }

      for (String extension : extensions) {
        if (!result.contains(extension)) {
          result.add(extension);
        }
      }
    }
    return result != null ? result : Collections.emptyList();
  }

  /**
   * Return all registered lookup key to media type mappings by iterating
   * {@link MediaTypeFileExtensionResolver}s.
   */
  public Map<String, MediaType> getMediaTypeMappings() {
    HashMap<String, MediaType> result = null;
    for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
      if (resolver instanceof MappingMediaTypeFileExtensionResolver mediaTypeMappings) {
        Map<String, MediaType> map = mediaTypeMappings.getMediaTypes();
        if (CollectionUtils.isEmpty(map)) {
          continue;
        }
        if (result == null) {
          result = new HashMap<>(4);
        }
        result.putAll(map);
      }
    }
    return result != null ? result : Collections.emptyMap();
  }

}
