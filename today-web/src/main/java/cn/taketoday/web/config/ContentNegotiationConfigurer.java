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

package cn.taketoday.web.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.ContentNegotiationManagerFactoryBean;
import cn.taketoday.web.accept.ContentNegotiationStrategy;
import cn.taketoday.web.accept.FixedContentNegotiationStrategy;
import cn.taketoday.web.accept.HeaderContentNegotiationStrategy;
import cn.taketoday.web.accept.ParameterContentNegotiationStrategy;
import jakarta.servlet.ServletContext;

/**
 * Creates a {@code ContentNegotiationManager} and configures it with
 * one or more {@link ContentNegotiationStrategy} instances.
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
 * <td>{@link #favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>Enabled</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * </table>
 *
 * <p>you can set the exact strategies to use via
 * {@link #strategies(List)}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 16:10
 */
public class ContentNegotiationConfigurer {

  private final ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();

  private final Map<String, MediaType> mediaTypes = new HashMap<>();

  public ContentNegotiationConfigurer() { }

  public ContentNegotiationConfigurer(@Nullable Object servletContext) {
    factory.setServletContext(servletContext);
  }

  /**
   * Set the exact list of strategies to use.
   * <p><strong>Note:</strong> use of this method is mutually exclusive with
   * use of all other setters in this class which customize a default, fixed
   * set of strategies. See class level doc for more details.
   *
   * @param strategies the strategies to use
   */
  public void strategies(@Nullable List<ContentNegotiationStrategy> strategies) {
    this.factory.setStrategies(strategies);
  }

  /**
   * Whether a request parameter ("format" by default) should be used to
   * determine the requested media type. For this option to work you must
   * register {@link #mediaType(String, MediaType) media type mappings}.
   * <p>By default this is set to {@code false}.
   *
   * @see #parameterName(String)
   */
  public ContentNegotiationConfigurer favorParameter(boolean favorParameter) {
    this.factory.setFavorParameter(favorParameter);
    return this;
  }

  /**
   * Set the query parameter name to use when {@link #favorParameter} is on.
   * <p>The default parameter name is {@code "format"}.
   */
  public ContentNegotiationConfigurer parameterName(String parameterName) {
    this.factory.setParameterName(parameterName);
    return this;
  }

  /**
   * Add a mapping from a key, extracted from a path extension or a query
   * parameter, to a MediaType. This is required in order for the parameter
   * strategy to work. Any extensions explicitly registered here are also
   * treated as safe for the purpose of Reflected File Download attack
   * detection.
   * <p>The path extension strategy will also try to use
   * {@link ServletContext#getMimeType} and {@link MediaType#fromFileName(String)} to resolve path
   * extensions. To change this behavior see the {@link #useRegisteredExtensionsOnly} property.
   *
   * @param extension the key to look up
   * @param mediaType the media type
   * @see #mediaTypes(Map)
   * @see #replaceMediaTypes(Map)
   */
  public ContentNegotiationConfigurer mediaType(String extension, MediaType mediaType) {
    this.mediaTypes.put(extension, mediaType);
    return this;
  }

  /**
   * An alternative to {@link #mediaType}.
   *
   * @see #mediaType(String, MediaType)
   * @see #replaceMediaTypes(Map)
   */
  public ContentNegotiationConfigurer mediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
    if (mediaTypes != null) {
      this.mediaTypes.putAll(mediaTypes);
    }
    return this;
  }

  /**
   * Similar to {@link #mediaType} but for replacing existing mappings.
   *
   * @see #mediaType(String, MediaType)
   * @see #mediaTypes(Map)
   */
  public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
    this.mediaTypes.clear();
    mediaTypes(mediaTypes);
    return this;
  }

  /**
   * this property determines whether to use only registered {@code MediaType}
   * mappings to resolve a path extension to a specific MediaType.
   */
  public ContentNegotiationConfigurer useRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
    this.factory.setUseRegisteredExtensionsOnly(useRegisteredExtensionsOnly);
    return this;
  }

  /**
   * Whether to disable checking the 'Accept' request header.
   * <p>By default this value is set to {@code false}.
   */
  public ContentNegotiationConfigurer ignoreAcceptHeader(boolean ignoreAcceptHeader) {
    this.factory.setIgnoreAcceptHeader(ignoreAcceptHeader);
    return this;
  }

  /**
   * Set the default content type(s) to use when no content type is requested
   * in order of priority.
   * <p>If destinations are present that do not support any of the given media
   * types, consider appending {@link MediaType#ALL} at the end.
   * <p>By default this is not set.
   *
   * @see #defaultContentTypeStrategy
   */
  public ContentNegotiationConfigurer defaultContentType(MediaType... defaultContentTypes) {
    this.factory.setDefaultContentTypes(Arrays.asList(defaultContentTypes));
    return this;
  }

  /**
   * Set a custom {@link ContentNegotiationStrategy} to use to determine
   * the content type to use when no content type is requested.
   * <p>By default this is not set.
   *
   * @see #defaultContentType
   */
  public ContentNegotiationConfigurer defaultContentTypeStrategy(ContentNegotiationStrategy defaultStrategy) {
    this.factory.setDefaultContentTypeStrategy(defaultStrategy);
    return this;
  }

  /**
   * Build a {@link ContentNegotiationManager} based on this configurer's settings.
   *
   * @see ContentNegotiationManagerFactoryBean#getObject()
   */
  protected ContentNegotiationManager buildContentNegotiationManager() {
    this.factory.addMediaTypes(this.mediaTypes);
    return this.factory.build();
  }

}
