/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.accept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

import static cn.taketoday.web.accept.PathExtensionContentNegotiationStrategy.assertServletContext;

/**
 * Factory to create a {@code ContentNegotiationManager} and configure it with
 * {@link ContentNegotiationStrategy} instances.
 *
 * <p>This factory offers properties that in turn result in configuring the
 * underlying strategies. The table below shows the property names, their
 * default settings, as well as the strategies that they help to configure:
 *
 * <table>
 * <tr>
 * <th>Property Setter</th>
 * <th>Default Value</th>
 * <th>Underlying Strategy</th>
 * <th>Enabled Or Not</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>Enabled</td>
 * </tr>
 * <tr>
 * <td>{@link #setFavorPathExtension favorPathExtension}</td>
 * <td>false</td>
 * <td>{@link PathExtensionContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * </table>
 *
 * <p>Alternatively you can avoid use of the above convenience builder
 * methods and set the exact strategies to use via
 * {@link #setStrategies(List)}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ContentNegotiationManagerFactoryBean
        implements FactoryBean<ContentNegotiationManager>, InitializingBean {

  @Nullable
  private List<ContentNegotiationStrategy> strategies;

  private boolean favorParameter = false;

  private String parameterName = "format";

  private boolean ignoreUnknownPathExtensions = true;

  private boolean favorPathExtension = false;

  private final Map<String, MediaType> mediaTypes = new HashMap<>();

  @Nullable
  private Boolean useRegisteredExtensionsOnly;

  private boolean ignoreAcceptHeader = false;

  @Nullable
  private ContentNegotiationStrategy defaultNegotiationStrategy;

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  @Nullable
  private Object servletContext;

  /**
   * Set the exact list of strategies to use.
   * <p><strong>Note:</strong> use of this method is mutually exclusive with
   * use of all other setters in this class which customize a default, fixed
   * set of strategies. See class level doc for more details.
   *
   * @param strategies the strategies to use
   */
  public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
    this.strategies = strategies != null ? new ArrayList<>(strategies) : null;
  }

  /**
   * Whether a request parameter ("format" by default) should be used to
   * determine the requested media type. For this option to work you must
   * register {@link #setMediaTypes media type mappings}.
   * <p>By default this is set to {@code false}.
   *
   * @see #setParameterName
   */
  public void setFavorParameter(boolean favorParameter) {
    this.favorParameter = favorParameter;
  }

  /**
   * Set the query parameter name to use when {@link #setFavorParameter} is on.
   * <p>The default parameter name is {@code "format"}.
   */
  public void setParameterName(String parameterName) {
    Assert.notNull(parameterName, "parameterName is required");
    this.parameterName = parameterName;
  }

  /**
   * Whether the path extension in the URL path should be used to determine
   * the requested media type.
   * <p>By default this is set to {@code false} in which case path extensions
   * have no impact on content negotiation.
   */
  public void setFavorPathExtension(boolean favorPathExtension) {
    this.favorPathExtension = favorPathExtension;
  }

  /**
   * Add a mapping from a key to a MediaType where the key are normalized to
   * lowercase and may have been extracted from a path extension, a filename
   * extension, or passed as a query parameter.
   * <p>The {@link #setFavorParameter(boolean) parameter strategy} requires
   * such mappings in order to work while the path extension strategy can fall
   * back on lookups via and {@link MediaType}.
   * <p><strong>Note:</strong> Mappings registered here may be accessed via
   * {@link ContentNegotiationManager#getMediaTypeMappings()} and may be used
   * not only in the parameter and path extension strategies.
   *
   * @param mediaTypes media type mappings
   * @see #addMediaType(String, MediaType)
   * @see #addMediaTypes(Map)
   */
  public void setMediaTypes(Properties mediaTypes) {
    if (CollectionUtils.isNotEmpty(mediaTypes)) {
      for (Map.Entry<Object, Object> entry : mediaTypes.entrySet()) {
        Object key = entry.getKey();
        Object value = entry.getValue();
        addMediaType((String) key, MediaType.valueOf((String) value));
      }
    }
  }

  /**
   * An alternative to {@link #setMediaTypes} for programmatic registrations.
   */
  public void addMediaType(String key, MediaType mediaType) {
    this.mediaTypes.put(key.toLowerCase(Locale.ENGLISH), mediaType);
  }

  /**
   * An alternative to {@link #setMediaTypes} for programmatic registrations.
   */
  public void addMediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
    if (mediaTypes != null) {
      for (Map.Entry<String, MediaType> entry : mediaTypes.entrySet()) {
        String key = entry.getKey();
        MediaType value = entry.getValue();
        addMediaType(key, value);
      }
    }
  }

  /**
   * Whether to ignore requests with path extension that cannot be resolved
   * to any media type. Setting this to {@code false} will result in an
   * {@code HttpMediaTypeNotAcceptableException} if there is no match.
   * <p>By default this is set to {@code true}.
   */
  public void setIgnoreUnknownPathExtensions(boolean ignore) {
    this.ignoreUnknownPathExtensions = ignore;
  }

  /**
   * When {@link #setFavorParameter(boolean)} is set, this property determines
   * whether to use only registered {@code MediaType} mappings or to allow
   * dynamic resolution, e.g. via {@link MediaType#fromFileName(String)}  MediaTypeFactory}.
   * <p>By default this is not set in which case dynamic resolution is on.
   */
  public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
    this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
  }

  /**
   * Whether to disable checking the 'Accept' request header.
   * <p>By default this value is set to {@code false}.
   */
  public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
    this.ignoreAcceptHeader = ignoreAcceptHeader;
  }

  /**
   * Set the default content type to use when no content type is requested.
   * <p>By default this is not set.
   *
   * @see #setDefaultContentTypeStrategy
   */
  public void setDefaultContentType(MediaType contentType) {
    this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
  }

  /**
   * Set the default content types to use when no content type is requested.
   * <p>By default this is not set.
   *
   * @see #setDefaultContentTypeStrategy
   */
  public void setDefaultContentTypes(List<MediaType> contentTypes) {
    this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentTypes);
  }

  /**
   * Set a custom {@link ContentNegotiationStrategy} to use to determine
   * the content type to use when no content type is requested.
   * <p>By default this is not set.
   *
   * @see #setDefaultContentType
   */
  public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
    this.defaultNegotiationStrategy = strategy;
  }

  public void setServletContext(@Nullable Object servletContext) {
    assertServletContext(servletContext);
    this.servletContext = servletContext;
  }

  @Override
  public void afterPropertiesSet() {
    build();
  }

  /**
   * Create and initialize a {@link ContentNegotiationManager} instance.
   */
  public ContentNegotiationManager build() {
    List<ContentNegotiationStrategy> strategies = new ArrayList<>();

    if (this.strategies != null) {
      strategies.addAll(this.strategies);
    }
    else {
      if (this.favorPathExtension) {
        var strategy = new PathExtensionContentNegotiationStrategy(servletContext, mediaTypes);
        strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
        if (this.useRegisteredExtensionsOnly != null) {
          strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
        }
        strategies.add(strategy);
      }

      if (this.favorParameter) {
        var strategy = new ParameterContentNegotiationStrategy(mediaTypes);
        strategy.setParameterName(this.parameterName);
        // backwards compatibility
        strategy.setUseRegisteredExtensionsOnly(Objects.requireNonNullElse(this.useRegisteredExtensionsOnly, true));
        strategies.add(strategy);
      }
      if (!this.ignoreAcceptHeader) {
        strategies.add(new HeaderContentNegotiationStrategy());
      }
      if (this.defaultNegotiationStrategy != null) {
        strategies.add(this.defaultNegotiationStrategy);
      }
    }

    this.contentNegotiationManager = new ContentNegotiationManager(strategies);

    // Ensure media type mappings are available via ContentNegotiationManager#getMediaTypeMappings()
    // independent of path extension or parameter strategies.

    if (CollectionUtils.isNotEmpty(this.mediaTypes) && !this.favorPathExtension && !this.favorParameter) {
      this.contentNegotiationManager.addFileExtensionResolvers(
              new MappingMediaTypeFileExtensionResolver(this.mediaTypes));
    }

    return this.contentNegotiationManager;
  }

  @Override
  @Nullable
  public ContentNegotiationManager getObject() {
    return this.contentNegotiationManager;
  }

  @Override
  public Class<?> getObjectType() {
    return ContentNegotiationManager.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
