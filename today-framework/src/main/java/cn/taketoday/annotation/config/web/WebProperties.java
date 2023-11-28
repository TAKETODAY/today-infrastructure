/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.web;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.http.CacheControl;
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
  private Locale locale;

  /**
   * Define how the locale should be resolved.
   */
  private LocaleResolver localeResolver = LocaleResolver.ACCEPT_HEADER;

  private final Resources resources = new Resources();

  public Locale getLocale() {
    return this.locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public LocaleResolver getLocaleResolver() {
    return this.localeResolver;
  }

  public void setLocaleResolver(LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  public Resources getResources() {
    return this.resources;
  }

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
    private String[] staticLocations = CLASSPATH_RESOURCE_LOCATIONS;

    /**
     * Whether to enable default resource handling.
     */
    private boolean addDefaultMappings = true;

    private boolean customized = false;

    private final Chain chain = new Chain();

    private final Cache cache = new Cache();

    public String[] getStaticLocations() {
      return this.staticLocations;
    }

    public void setStaticLocations(String[] staticLocations) {
      this.staticLocations = appendSlashIfNecessary(staticLocations);
      this.customized = true;
    }

    private String[] appendSlashIfNecessary(String[] staticLocations) {
      String[] normalized = new String[staticLocations.length];
      for (int i = 0; i < staticLocations.length; i++) {
        String location = staticLocations[i];
        normalized[i] = location.endsWith("/") ? location : location + "/";
      }
      return normalized;
    }

    public boolean isAddDefaultMappings() {
      return this.addDefaultMappings;
    }

    public void setAddDefaultMappings(boolean addDefaultMappings) {
      this.addDefaultMappings = addDefaultMappings;
      this.customized = true;
    }

    public Chain getChain() {
      return this.chain;
    }

    public Cache getCache() {
      return this.cache;
    }

    public boolean hasBeenCustomized() {
      return this.customized || chain.hasBeenCustomized() || cache.hasBeenCustomized();
    }

    /**
     * Configuration for the Framework Resource Handling chain.
     */
    public static class Chain {

      boolean customized = false;

      /**
       * Whether to enable the Infra Resource Handling chain. By default, disabled
       * unless at least one strategy has been enabled.
       */
      private Boolean enabled;

      /**
       * Whether to enable caching in the Resource chain.
       */
      private boolean cache = true;

      /**
       * Whether to enable resolution of already compressed resources (gzip,
       * brotli). Checks for a resource name with the '.gz' or '.br' file
       * extensions.
       */
      private boolean compressed = false;

      private final Strategy strategy = new Strategy();

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

      public boolean hasBeenCustomized() {
        return this.customized || getStrategy().hasBeenCustomized();
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.customized = true;
      }

      public boolean isCache() {
        return this.cache;
      }

      public void setCache(boolean cache) {
        this.cache = cache;
        this.customized = true;
      }

      public Strategy getStrategy() {
        return this.strategy;
      }

      public boolean isCompressed() {
        return this.compressed;
      }

      public void setCompressed(boolean compressed) {
        this.compressed = compressed;
        this.customized = true;
      }

      static Boolean getEnabled(boolean fixedEnabled, boolean contentEnabled, Boolean chainEnabled) {
        return (fixedEnabled || contentEnabled) ? Boolean.TRUE : chainEnabled;
      }

      /**
       * Strategies for extracting and embedding a resource version in its URL path.
       */
      public static class Strategy {

        private final Fixed fixed = new Fixed();

        private final Content content = new Content();

        public Fixed getFixed() {
          return this.fixed;
        }

        public Content getContent() {
          return this.content;
        }

        public boolean hasBeenCustomized() {
          return getFixed().hasBeenCustomized() || getContent().hasBeenCustomized();
        }

        /**
         * Version Strategy based on content hashing.
         */
        public static class Content {

          private boolean customized = false;

          /**
           * Whether to enable the content Version Strategy.
           */
          private boolean enabled;

          /**
           * Comma-separated list of patterns to apply to the content Version
           * Strategy.
           */
          private String[] paths = new String[] { "/**" };

          public boolean isEnabled() {
            return this.enabled;
          }

          public void setEnabled(boolean enabled) {
            this.customized = true;
            this.enabled = enabled;
          }

          public String[] getPaths() {
            return this.paths;
          }

          public void setPaths(String[] paths) {
            this.customized = true;
            this.paths = paths;
          }

          public boolean hasBeenCustomized() {
            return this.customized;
          }

        }

        /**
         * Version Strategy based on a fixed version string.
         */
        public static class Fixed {

          private boolean customized = false;

          /**
           * Whether to enable the fixed Version Strategy.
           */
          private boolean enabled;

          /**
           * Comma-separated list of patterns to apply to the fixed Version
           * Strategy.
           */
          private String[] paths = new String[] { "/**" };

          /**
           * Version string to use for the fixed Version Strategy.
           */
          private String version;

          public boolean isEnabled() {
            return this.enabled;
          }

          public void setEnabled(boolean enabled) {
            this.customized = true;
            this.enabled = enabled;
          }

          public String[] getPaths() {
            return this.paths;
          }

          public void setPaths(String[] paths) {
            this.customized = true;
            this.paths = paths;
          }

          public String getVersion() {
            return this.version;
          }

          public void setVersion(String version) {
            this.customized = true;
            this.version = version;
          }

          public boolean hasBeenCustomized() {
            return this.customized;
          }

        }

      }

    }

    /**
     * Cache configuration.
     */
    public static class Cache {

      private boolean customized = false;

      /**
       * Cache period for the resources served by the resource handler. If a
       * duration suffix is not specified, seconds will be used. Can be overridden
       * by the 'web.resources.cache.cachecontrol' properties.
       */
      @DurationUnit(ChronoUnit.SECONDS)
      private Duration period;

      /**
       * Cache control HTTP headers, only allows valid directive combinations.
       * Overrides the 'web.resources.cache.period' property.
       */
      private final Cachecontrol cachecontrol = new Cachecontrol();

      /**
       * Whether we should use the "lastModified" metadata of the files in HTTP
       * caching headers.
       */
      private boolean useLastModified = true;

      public Duration getPeriod() {
        return this.period;
      }

      public void setPeriod(Duration period) {
        this.customized = true;
        this.period = period;
      }

      public Cachecontrol getCachecontrol() {
        return this.cachecontrol;
      }

      public boolean isUseLastModified() {
        return this.useLastModified;
      }

      public void setUseLastModified(boolean useLastModified) {
        this.useLastModified = useLastModified;
      }

      public boolean hasBeenCustomized() {
        return this.customized || cachecontrol.customized;
      }

      public CacheControl getHttpCacheControl() {
        PropertyMapper map = PropertyMapper.get();
        CacheControl control = createCacheControl();
        map.from(cachecontrol::getMustRevalidate).whenTrue().toCall(control::mustRevalidate);
        map.from(cachecontrol::getNoTransform).whenTrue().toCall(control::noTransform);
        map.from(cachecontrol::getCachePublic).whenTrue().toCall(control::cachePublic);
        map.from(cachecontrol::getCachePrivate).whenTrue().toCall(control::cachePrivate);
        map.from(cachecontrol::getProxyRevalidate).whenTrue().toCall(control::proxyRevalidate);

        map.from(cachecontrol::getSMaxAge).whenNonNull().to(duration -> control.sMaxAge(duration.getSeconds(), TimeUnit.SECONDS));
        map.from(cachecontrol::getStaleIfError).whenNonNull().to(duration -> control.staleIfError(duration.getSeconds(), TimeUnit.SECONDS));
        map.from(cachecontrol::getStaleWhileRevalidate).whenNonNull().to(duration -> control.staleWhileRevalidate(duration.getSeconds(), TimeUnit.SECONDS));

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

        private boolean customized = false;

        /**
         * Maximum time the response should be cached, in seconds if no duration
         * suffix is not specified.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration maxAge;

        /**
         * Indicate that the cached response can be reused only if re-validated
         * with the server.
         */
        private Boolean noCache;

        /**
         * Indicate to not cache the response in any case.
         */
        private Boolean noStore;

        /**
         * Indicate that once it has become stale, a cache must not use the
         * response without re-validating it with the server.
         */
        private Boolean mustRevalidate;

        /**
         * Indicate intermediaries (caches and others) that they should not
         * transform the response content.
         */
        private Boolean noTransform;

        /**
         * Indicate that any cache may store the response.
         */
        private Boolean cachePublic;

        /**
         * Indicate that the response message is intended for a single user and
         * must not be stored by a shared cache.
         */
        private Boolean cachePrivate;

        /**
         * Same meaning as the "must-revalidate" directive, except that it does
         * not apply to private caches.
         */
        private Boolean proxyRevalidate;

        /**
         * Maximum time the response can be served after it becomes stale, in
         * seconds if no duration suffix is not specified.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration staleWhileRevalidate;

        /**
         * Maximum time the response may be used when errors are encountered, in
         * seconds if no duration suffix is not specified.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration staleIfError;

        /**
         * Maximum time the response should be cached by shared caches, in seconds
         * if no duration suffix is not specified.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration sMaxAge;

        public Duration getMaxAge() {
          return this.maxAge;
        }

        public void setMaxAge(Duration maxAge) {
          this.customized = true;
          this.maxAge = maxAge;
        }

        public Boolean getNoCache() {
          return this.noCache;
        }

        public void setNoCache(Boolean noCache) {
          this.customized = true;
          this.noCache = noCache;
        }

        public Boolean getNoStore() {
          return this.noStore;
        }

        public void setNoStore(Boolean noStore) {
          this.customized = true;
          this.noStore = noStore;
        }

        public Boolean getMustRevalidate() {
          return this.mustRevalidate;
        }

        public void setMustRevalidate(Boolean mustRevalidate) {
          this.customized = true;
          this.mustRevalidate = mustRevalidate;
        }

        public Boolean getNoTransform() {
          return this.noTransform;
        }

        public void setNoTransform(Boolean noTransform) {
          this.customized = true;
          this.noTransform = noTransform;
        }

        public Boolean getCachePublic() {
          return this.cachePublic;
        }

        public void setCachePublic(Boolean cachePublic) {
          this.customized = true;
          this.cachePublic = cachePublic;
        }

        public Boolean getCachePrivate() {
          return this.cachePrivate;
        }

        public void setCachePrivate(Boolean cachePrivate) {
          this.customized = true;
          this.cachePrivate = cachePrivate;
        }

        public Boolean getProxyRevalidate() {
          return this.proxyRevalidate;
        }

        public void setProxyRevalidate(Boolean proxyRevalidate) {
          this.customized = true;
          this.proxyRevalidate = proxyRevalidate;
        }

        public Duration getStaleWhileRevalidate() {
          return this.staleWhileRevalidate;
        }

        public void setStaleWhileRevalidate(Duration staleWhileRevalidate) {
          this.customized = true;
          this.staleWhileRevalidate = staleWhileRevalidate;
        }

        public Duration getStaleIfError() {
          return this.staleIfError;
        }

        public void setStaleIfError(Duration staleIfError) {
          this.customized = true;
          this.staleIfError = staleIfError;
        }

        public Duration getSMaxAge() {
          return this.sMaxAge;
        }

        public void setSMaxAge(Duration sMaxAge) {
          this.customized = true;
          this.sMaxAge = sMaxAge;
        }

      }

    }

  }

}
