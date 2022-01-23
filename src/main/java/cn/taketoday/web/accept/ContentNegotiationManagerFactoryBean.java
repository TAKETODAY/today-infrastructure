/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import java.util.Properties;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MediaType;
import cn.taketoday.web.ServletContextAware;
import jakarta.servlet.ServletContext;

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
 * <p><strong>Deprecation Note:</strong> As of 5.2.4,
 * {@link #setFavorPathExtension(boolean) favorPathExtension} and
 * {@link #setIgnoreUnknownPathExtensions(boolean) ignoreUnknownPathExtensions}
 * are deprecated in order to discourage using path extensions for content
 * negotiation and for request mapping with similar deprecations on
 * {@link cn.taketoday.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 * RequestMappingHandlerMapping}. For further context, please read issue
 * <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24719</a>.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
public class ContentNegotiationManagerFactoryBean
        implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean {

  @Nullable
  private List<ContentNegotiationStrategy> strategies;

  private boolean favorParameter = false;

  private String parameterName = "format";

  private final Map<String, MediaType> mediaTypes = new HashMap<>();

  private boolean ignoreUnknownPathExtensions = true;

  @Nullable
  private Boolean useRegisteredExtensionsOnly;

  private boolean ignoreAcceptHeader = false;

  @Nullable
  private ContentNegotiationStrategy defaultNegotiationStrategy;

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  @Nullable
  private ServletContext servletContext;

  /**
   * Set the exact list of strategies to use.
   * <p><strong>Note:</strong> use of this method is mutually exclusive with
   * use of all other setters in this class which customize a default, fixed
   * set of strategies. See class level doc for more details.
   *
   * @param strategies the strategies to use
   * @since 5.0
   */
  public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
    this.strategies = (strategies != null ? new ArrayList<>(strategies) : null);
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
   * Add a mapping from a key to a MediaType where the key are normalized to
   * lowercase and may have been extracted from a path extension, a filename
   * extension, or passed as a query parameter.
   * <p>The {@link #setFavorParameter(boolean) parameter strategy} requires
   * such mappings in order to work while the {@link #setFavorPathExtension(boolean)
   * path extension strategy} can fall back on lookups via
   * {@link ServletContext#getMimeType} and
   * {@link cn.taketoday.http.MediaTypeFactory}.
   * <p><strong>Note:</strong> Mappings registered here may be accessed via
   * {@link ContentNegotiationManager#getMediaTypeMappings()} and may be used
   * not only in the parameter and path extension strategies. For example,
   * with the Spring MVC config, e.g. {@code @EnableWebMvc} or
   * {@code <mvc:annotation-driven>}, the media type mappings are also plugged
   * in to:
   * <ul>
   * <li>Determine the media type of static resources served with
   * {@code ResourceHttpRequestHandler}.
   * <li>Determine the media type of views rendered with
   * {@code ContentNegotiatingViewResolver}.
   * <li>List safe extensions for RFD attack detection (check the Spring
   * Framework reference docs for details).
   * </ul>
   *
   * @param mediaTypes media type mappings
   * @see #addMediaType(String, MediaType)
   * @see #addMediaTypes(Map)
   */
  public void setMediaTypes(Properties mediaTypes) {
    if (!CollectionUtils.isEmpty(mediaTypes)) {
      mediaTypes.forEach((key, value) ->
              addMediaType((String) key, MediaType.valueOf((String) value)));
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
      mediaTypes.forEach(this::addMediaType);
    }
  }

  /**
   * Whether to ignore requests with path extension that cannot be resolved
   * to any media type. Setting this to {@code false} will result in an
   * {@code HttpMediaTypeNotAcceptableException} if there is no match.
   * <p>By default this is set to {@code true}.
   *
   * @deprecated as of 5.2.4. See class-level note on the deprecation of path
   * extension config options.
   */
  @Deprecated
  public void setIgnoreUnknownPathExtensions(boolean ignore) {
    this.ignoreUnknownPathExtensions = ignore;
  }

  /**
   * When {@link #setFavorPathExtension favorPathExtension} or
   * {@link #setFavorParameter(boolean)} is set, this property determines
   * whether to use only registered {@code MediaType} mappings or to allow
   * dynamic resolution, e.g. via {@link MediaTypeFactory}.
   * <p>By default this is not set in which case dynamic resolution is on.
   */
  public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
    this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
  }

  private boolean useRegisteredExtensionsOnly() {
    return (this.useRegisteredExtensionsOnly != null && this.useRegisteredExtensionsOnly);
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

  /**
   * Invoked by Spring to inject the ServletContext.
   */
  @Override
  public void setServletContext(ServletContext servletContext) {
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
      if (this.favorParameter) {
        ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
        strategy.setParameterName(this.parameterName);
        if (this.useRegisteredExtensionsOnly != null) {
          strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
        }
        else {
          strategy.setUseRegisteredExtensionsOnly(true);  // backwards compatibility
        }
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

    if (!CollectionUtils.isEmpty(this.mediaTypes) && !this.favorParameter) {
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
