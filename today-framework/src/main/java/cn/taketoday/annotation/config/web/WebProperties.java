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
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.http.CacheControl;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.PropertyMapper;

/**
 * {@link ConfigurationProperties Configuration properties} for general web concerns.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/16 15:38
 */
@ConfigurationProperties("web")
public class WebProperties {

  /**
   * Locale to use. By default, this locale is overridden by the "Accept-Language"
   * header.
   */
  @Nullable
  public Locale locale;

  /**
   * Define how the locale should be resolved.
   */
  public LocaleResolver localeResolver = LocaleResolver.ACCEPT_HEADER;

  public final Resources resources = new Resources();

  public enum LocaleResolver {

    /**
     * Always use the configured locale.
     */
    FIXED,

    /**
     * Use the "Accept-Language" header or the configured locale if the header is not
     * set.
     */
    ACCEPT_HEADER

  }

  public static class Resources {

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/"
    };

    /**
     * Locations of static resources. Defaults to classpath:[/META-INF/resources/,
     * /resources/, /static/, /public/].
     */
    public String[] staticLocations = CLASSPATH_RESOURCE_LOCATIONS;

    /**
     * Whether to enable default resource handling.
     */
    public boolean addDefaultMappings = true;

    public final Chain chain = new Chain();

    public final Cache cache = new Cache();

    public void setStaticLocations(String[] staticLocations) {
      this.staticLocations = appendSlashIfNecessary(staticLocations);
    }

    private String[] appendSlashIfNecessary(String[] staticLocations) {
      String[] normalized = new String[staticLocations.length];
      for (int i = 0; i < staticLocations.length; i++) {
        String location = staticLocations[i];
        normalized[i] = location.endsWith("/") ? location : location + "/";
      }
      return normalized;
    }

    /**
     * Configuration for the Framework Resource Handling chain.
     */
    public static class Chain {

      /**
       * Whether to enable the Infra Resource Handling chain. By default, disabled
       * unless at least one strategy has been enabled.
       */
      private Boolean enabled;

      /**
       * Whether to enable caching in the Resource chain.
       */
      public boolean cache = true;

      /**
       * Whether to enable resolution of already compressed resources (gzip,
       * brotli). Checks for a resource name with the '.gz' or '.br' file
       * extensions.
       */
      public boolean compressed = false;

      public final Strategy strategy = new Strategy();

      /**
       * Return whether the resource chain is enabled. Return {@code null} if no
       * specific settings are present.
       *
       * @return whether the resource chain is enabled or {@code null} if no
       * specified settings are present.
       */
      public Boolean getEnabled() {
        return getEnabled(strategy.fixed.enabled, strategy.content.enabled, enabled);
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      static Boolean getEnabled(boolean fixedEnabled, boolean contentEnabled, Boolean chainEnabled) {
        return (fixedEnabled || contentEnabled) ? Boolean.TRUE : chainEnabled;
      }

      /**
       * Strategies for extracting and embedding a resource version in its URL path.
       */
      public static class Strategy {

        public final Fixed fixed = new Fixed();

        public final Content content = new Content();

        /**
         * Version Strategy based on content hashing.
         */
        public static class Content {

          /**
           * Whether to enable the content Version Strategy.
           */
          public boolean enabled;

          /**
           * Comma-separated list of patterns to apply to the content Version
           * Strategy.
           */
          public String[] paths = new String[] { "/**" };

        }

        /**
         * Version Strategy based on a fixed version string.
         */
        public static class Fixed {

          /**
           * Whether to enable the fixed Version Strategy.
           */
          public boolean enabled;

          /**
           * Comma-separated list of patterns to apply to the fixed Version
           * Strategy.
           */
          public String[] paths = new String[] { "/**" };

          /**
           * Version string to use for the fixed Version Strategy.
           */
          public String version;

        }

      }

    }

    /**
     * Cache configuration.
     */
    public static class Cache {

      /**
       * Cache period for the resources served by the resource handler. If a
       * duration suffix is not specified, seconds will be used. Can be overridden
       * by the 'web.resources.cache.cachecontrol' properties.
       */
      @Nullable
      @DurationUnit(ChronoUnit.SECONDS)
      public Duration period;

      /**
       * Cache control HTTP headers, only allows valid directive combinations.
       * Overrides the 'web.resources.cache.period' property.
       */
      public final Cachecontrol cachecontrol = new Cachecontrol();

      /**
       * Whether we should use the "lastModified" metadata of the files in HTTP
       * caching headers.
       */
      public boolean useLastModified = true;

      public CacheControl getHttpCacheControl() {
        PropertyMapper map = PropertyMapper.get();
        CacheControl control = createCacheControl();
        map.from(cachecontrol.mustRevalidate).whenTrue().toCall(control::mustRevalidate);
        map.from(cachecontrol.noTransform).whenTrue().toCall(control::noTransform);
        map.from(cachecontrol.cachePublic).whenTrue().toCall(control::cachePublic);
        map.from(cachecontrol.cachePrivate).whenTrue().toCall(control::cachePrivate);
        map.from(cachecontrol.proxyRevalidate).whenTrue().toCall(control::proxyRevalidate);

        map.from(cachecontrol.sMaxAge).whenNonNull().to(duration -> control.sMaxAge(duration.getSeconds(), TimeUnit.SECONDS));
        map.from(cachecontrol.staleIfError).whenNonNull().to(duration -> control.staleIfError(duration.getSeconds(), TimeUnit.SECONDS));
        map.from(cachecontrol.staleWhileRevalidate).whenNonNull().to(duration -> control.staleWhileRevalidate(duration.getSeconds(), TimeUnit.SECONDS));

        // check if cacheControl remained untouched
        if (control.getHeaderValue() == null) {
          return null;
        }
        return control;
      }

      private CacheControl createCacheControl() {
        if (Boolean.TRUE.equals(cachecontrol.noStore)) {
          return CacheControl.noStore();
        }
        if (Boolean.TRUE.equals(cachecontrol.noCache)) {
          return CacheControl.noCache();
        }
        if (cachecontrol.maxAge != null) {
          return CacheControl.maxAge(cachecontrol.maxAge.getSeconds(), TimeUnit.SECONDS);
        }
        return CacheControl.empty();
      }

      /**
       * Cache Control HTTP header configuration.
       */
      public static class Cachecontrol {

        /**
         * Maximum time the response should be cached, in seconds if no duration
         * suffix is not specified.
         */
        @Nullable
        @DurationUnit(ChronoUnit.SECONDS)
        public Duration maxAge;

        /**
         * Indicate that the cached response can be reused only if re-validated
         * with the server.
         */
        @Nullable
        public Boolean noCache;

        /**
         * Indicate to not cache the response in any case.
         */
        @Nullable
        public Boolean noStore;

        /**
         * Indicate that once it has become stale, a cache must not use the
         * response without re-validating it with the server.
         */
        @Nullable
        public Boolean mustRevalidate;

        /**
         * Indicate intermediaries (caches and others) that they should not
         * transform the response content.
         */
        @Nullable
        public Boolean noTransform;

        /**
         * Indicate that any cache may store the response.
         */
        @Nullable
        public Boolean cachePublic;

        /**
         * Indicate that the response message is intended for a single user and
         * must not be stored by a shared cache.
         */
        @Nullable
        public Boolean cachePrivate;

        /**
         * Same meaning as the "must-revalidate" directive, except that it does
         * not apply to private caches.
         */
        @Nullable
        public Boolean proxyRevalidate;

        /**
         * Maximum time the response can be served after it becomes stale, in
         * seconds if no duration suffix is not specified.
         */
        @Nullable
        @DurationUnit(ChronoUnit.SECONDS)
        public Duration staleWhileRevalidate;

        /**
         * Maximum time the response may be used when errors are encountered, in
         * seconds if no duration suffix is not specified.
         */
        @Nullable
        @DurationUnit(ChronoUnit.SECONDS)
        public Duration staleIfError;

        /**
         * Maximum time the response should be cached by shared caches, in seconds
         * if no duration suffix is not specified.
         */
        @Nullable
        @DurationUnit(ChronoUnit.SECONDS)
        public Duration sMaxAge;

      }

    }

  }

}
