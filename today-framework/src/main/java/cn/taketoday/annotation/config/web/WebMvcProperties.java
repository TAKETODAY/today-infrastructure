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

package cn.taketoday.annotation.config.web;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.validation.DefaultMessageCodesResolver;
import cn.taketoday.web.view.UrlBasedViewResolver;

/**
 * @author Phillip Webb
 * @author Sébastien Deleuze
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/16 15:34
 */
@ConfigurationProperties(prefix = "web.mvc")
public class WebMvcProperties {

  /**
   * Formatting strategy for message codes. For instance, 'PREFIX_ERROR_CODE'.
   */
  public DefaultMessageCodesResolver.Format messageCodesResolverFormat;

  public final Format format = new Format();

  /**
   * Whether to publish a RequestHandledEvent at the end of each request.
   *
   * @see cn.taketoday.web.context.support.RequestHandledEventPublisher
   */
  public boolean publishRequestHandledEvents = false;

  /**
   * Whether a "HandlerNotFoundException" should be thrown if no Handler was found to
   * process a request.
   */
  public boolean throwExceptionIfNoHandlerFound = false;

  /**
   * Whether logging of (potentially sensitive) request details at DEBUG and TRACE level
   * is allowed.
   */
  public boolean logRequestDetails;

  /**
   * Whether to enable warn logging of exceptions resolved by a
   * "HandlerExceptionHandler", except for "SimpleHandlerExceptionHandler".
   */
  public boolean logResolvedException = false;

  /**
   * Path pattern used for static resources.
   */
  public String staticPathPattern = "/**";

  /**
   * Path pattern used for WebJar assets.
   */
  public String webjarsPathPattern = "/webjars/**";

  public final Async async = new Async();

  public final View view = new View();

  public final Contentnegotiation contentnegotiation = new Contentnegotiation();

  public static class Async {

    /**
     * Amount of time before asynchronous request handling times out. If this value is
     * not set, the default timeout of the underlying implementation is used.
     */
    public Duration requestTimeout;

  }

  public static class View {

    /**
     * Web MVC view prefix.
     */
    @Nullable
    public String prefix;

    /**
     * Web MVC view suffix.
     */
    @Nullable
    public String suffix;

    public boolean exposeOutputRedirectModel = false;

    /**
     * apply this properties to {@code viewResolver}
     *
     * @param viewResolver viewResolver
     */
    public void applyTo(UrlBasedViewResolver viewResolver) {
      if (prefix != null) {
        viewResolver.setPrefix(prefix);
      }
      if (suffix != null) {
        viewResolver.setSuffix(suffix);
      }
      viewResolver.setExposeOutputRedirectModel(exposeOutputRedirectModel);
    }

  }

  public static class Contentnegotiation {

    /**
     * Whether a request parameter ("format" by default) should be used to determine
     * the requested media type.
     */
    public boolean favorParameter = false;

    /**
     * Map file extensions to media types for content negotiation. For instance, yml
     * to text/yaml.
     */
    public Map<String, MediaType> mediaTypes = new LinkedHashMap<>();

    /**
     * Query parameter name to use when "favor-parameter" is enabled.
     */
    @Nullable
    public String parameterName;

  }

  public static class Format {

    /**
     * Date format to use, for example 'dd/MM/yyyy'.
     */
    public String date;

    /**
     * Time format to use, for example 'HH:mm:ss'.
     */
    public String time;

    /**
     * Date-time format to use, for example 'yyyy-MM-dd HH:mm:ss'.
     */
    public String dateTime;

  }

}
