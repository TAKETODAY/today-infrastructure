/*
 * Copyright 2012-present the original author or authors.
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

package infra.webmvc.config;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.bind.Name;
import infra.http.MediaType;
import infra.validation.DefaultMessageCodesResolver;
import infra.web.view.UrlBasedViewResolver;
import infra.webmvc.config.annotation.ViewControllerRegistry;

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
  public DefaultMessageCodesResolver.@Nullable Format messageCodesResolverFormat;

  public final Format format = new Format();

  /**
   * Whether to publish a RequestHandledEvent at the end of each request.
   *
   * @see infra.web.context.support.RequestHandledEventPublisher
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
   * Whether to register the WebView XML configuration.
   *
   * @see ViewControllerRegistry#registerWebViewXml()
   */
  public boolean registerWebViewXml = false;

  public final Async async = new Async();

  public final View view = new View();

  public final Contentnegotiation contentnegotiation = new Contentnegotiation();

  public final ApiVersion apiVersion = new ApiVersion();

  public static class Async {

    /**
     * Amount of time before asynchronous request handling times out. If this value is
     * not set, the default timeout of the underlying implementation is used.
     */
    @Nullable
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

    public @Nullable String parameterName;

  }

  /**
   * API Version.
   */
  public static class ApiVersion {

    /**
     * Whether the API version is required with each request.
     */
    public @Nullable Boolean required;

    /**
     * Default version that should be used for each request.
     */
    @Name("default")
    public @Nullable String defaultVersion;

    /**
     * Supported versions.
     */
    public @Nullable List<String> supported;

    /**
     * Whether supported versions should be detected from controllers.
     */
    public @Nullable Boolean detectSupported;

    /**
     * How version details should be inserted into requests.
     */
    public final Use use = new Use();

    public static class Use {

      /**
       * Use the HTTP header with the given name to obtain the version.
       */
      public @Nullable String header;

      /**
       * Use the request parameter with the given name to obtain the version.
       */
      public @Nullable String requestParameter;

      /**
       * Use the path segment at the given index to obtain the version.
       */
      public @Nullable Integer pathSegment;

      /**
       * Use the media type parameter with the given name to obtain the version.
       */
      public Map<MediaType, String> mediaTypeParameter = new LinkedHashMap<>();

    }

  }

  @SuppressWarnings("NullAway")
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
