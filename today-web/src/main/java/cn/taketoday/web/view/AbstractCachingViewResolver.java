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

package cn.taketoday.web.view;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.context.support.ApplicationObjectSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Convenient base class for {@link ViewResolver}
 * implementations. Caches {@link View} objects
 * once resolved: This means that view resolution won't be a performance problem,
 * no matter how costly initial view retrieval is.
 *
 * <p>Subclasses need to implement the {@link #loadView} template method,
 * building the View object for a specific view name and locale.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #loadView
 */
public abstract class AbstractCachingViewResolver extends ApplicationObjectSupport implements ViewResolver {

  /** Default maximum number of entries for the view cache: 1024. */
  public static final int DEFAULT_CACHE_LIMIT = 1024;

  /** Dummy marker object for unresolved views in the cache Maps. */
  public static final View UNRESOLVED_VIEW = (model, context) -> { };

  /** Default cache filter that always caches. */
  public static final CacheFilter DEFAULT_CACHE_FILTER = (view, viewName, locale) -> true;

  /** The maximum number of entries in the cache. */
  private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

  /** Whether we should refrain from resolving views again if unresolved once. */
  private boolean cacheUnresolved = true;

  /** Filter function that determines if view should be cached. */
  private CacheFilter cacheFilter = DEFAULT_CACHE_FILTER;

  /** Fast access cache for Views, returning already cached instances without a global lock. */
  private final ConcurrentHashMap<Object, View> viewAccessCache = new ConcurrentHashMap<>(DEFAULT_CACHE_LIMIT);

  /** Map from view key to View instance, synchronized for View creation. */
  private final LinkedHashMap<Object, View> viewCreationCache =
          new LinkedHashMap<>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, View> eldest) {
              if (size() > getCacheLimit()) {
                viewAccessCache.remove(eldest.getKey());
                return true;
              }
              else {
                return false;
              }
            }
          };

  /**
   * Specify the maximum number of entries for the view cache.
   * Default is 1024.
   */
  public void setCacheLimit(int cacheLimit) {
    this.cacheLimit = cacheLimit;
  }

  /**
   * Return the maximum number of entries for the view cache.
   */
  public int getCacheLimit() {
    return cacheLimit;
  }

  /**
   * Enable or disable caching.
   * <p>This is equivalent to setting the {@link #setCacheLimit "cacheLimit"}
   * property to the default limit (1024) or to 0, respectively.
   * <p>Default is "true": caching is enabled.
   * Disable this only for debugging and development.
   */
  public void setCache(boolean cache) {
    this.cacheLimit = cache ? DEFAULT_CACHE_LIMIT : 0;
  }

  /**
   * Return if caching is enabled.
   */
  public boolean isCache() {
    return cacheLimit > 0;
  }

  /**
   * Whether a view name once resolved to {@code null} should be cached and
   * automatically resolved to {@code null} subsequently.
   * <p>Default is "true": unresolved view names are being cached, as of Framework 3.1.
   * Note that this flag only applies if the general {@link #setCache "cache"}
   * flag is kept at its default of "true" as well.
   * <p>Of specific interest is the ability for some AbstractUrlBasedView
   * implementations (FreeMarker, Tiles) to check if an underlying resource
   * exists via {@link AbstractUrlBasedView#checkResource(Locale)}.
   * With this flag set to "false", an underlying resource that re-appears
   * is noticed and used. With the flag set to "true", one check is made only.
   */
  public void setCacheUnresolved(boolean cacheUnresolved) {
    this.cacheUnresolved = cacheUnresolved;
  }

  /**
   * Return if caching of unresolved views is enabled.
   */
  public boolean isCacheUnresolved() {
    return this.cacheUnresolved;
  }

  /**
   * Sets the filter that determines if view should be cached.
   * Default behaviour is to cache all views.
   */
  public void setCacheFilter(CacheFilter cacheFilter) {
    Assert.notNull(cacheFilter, "CacheFilter is required");
    this.cacheFilter = cacheFilter;
  }

  /**
   * Return filter function that determines if view should be cached.
   */
  public CacheFilter getCacheFilter() {
    return this.cacheFilter;
  }

  @Override
  @Nullable
  public View resolveViewName(String viewName, Locale locale) throws Exception {
    if (isCache()) {
      Object cacheKey = getCacheKey(viewName, locale);
      View view = viewAccessCache.get(cacheKey);
      if (view == null) {
        synchronized(viewCreationCache) {
          view = viewCreationCache.get(cacheKey);
          if (view == null) {
            // Ask the subclass to create the View object.
            view = createView(viewName, locale);
            if (view == null && cacheUnresolved) {
              view = UNRESOLVED_VIEW;
            }
            if (view != null && cacheFilter.shouldCaching(view, viewName, locale)) {
              viewAccessCache.put(cacheKey, view);
              viewCreationCache.put(cacheKey, view);
            }
          }
        }
      }
      else {
        if (logger.isTraceEnabled()) {
          logger.trace("{}served from cache", formatKey(cacheKey));
        }
      }
      return view != UNRESOLVED_VIEW ? view : null;
    }
    else {
      return createView(viewName, locale);
    }
  }

  private static String formatKey(Object cacheKey) {
    return "View with key [" + cacheKey + "] ";
  }

  /**
   * Return the cache key for the given view name and the given locale.
   * <p>Default is a String consisting of view name and locale suffix.
   * Can be overridden in subclasses.
   * <p>Needs to respect the locale in general, as a different locale can
   * lead to a different view resource.
   */
  protected Object getCacheKey(String viewName, Locale locale) {
    return viewName + '_' + locale;
  }

  /**
   * Provides functionality to clear the cache for a certain view.
   * <p>This can be handy in case developer are able to modify views
   * (e.g. FreeMarker templates) at runtime after which you'd need to
   * clear the cache for the specified view.
   *
   * @param viewName the view name for which the cached view object
   * (if any) needs to be removed
   * @param locale the locale for which the view object should be removed
   */
  public void removeFromCache(String viewName, Locale locale) {
    if (!isCache()) {
      logger.warn("Caching is OFF (removal not necessary)");
    }
    else {
      Object cacheKey = getCacheKey(viewName, locale);
      Object cachedView;
      synchronized(viewCreationCache) {
        viewAccessCache.remove(cacheKey);
        cachedView = viewCreationCache.remove(cacheKey);
      }
      if (logger.isDebugEnabled()) {
        // Some debug output might be useful...
        logger.debug(formatKey(cacheKey) +
                (cachedView != null ? "cleared from cache" : "not found in the cache"));
      }
    }
  }

  /**
   * Clear the entire view cache, removing all cached view objects.
   * Subsequent resolve calls will lead to recreation of demanded view objects.
   */
  public void clearCache() {
    logger.debug("Clearing all views from the cache");
    synchronized(viewCreationCache) {
      viewAccessCache.clear();
      viewCreationCache.clear();
    }
  }

  /**
   * Create the actual View object.
   * <p>The default implementation delegates to {@link #loadView}.
   * This can be overridden to resolve certain view names in a special fashion,
   * before delegating to the actual {@code loadView} implementation
   * provided by the subclass.
   *
   * @param viewName the name of the view to retrieve
   * @param locale the Locale to retrieve the view for
   * @return the View instance, or {@code null} if not found
   * (optional, to allow for ViewResolver chaining)
   * @throws Exception if the view couldn't be resolved
   * @see #loadView
   */
  @Nullable
  protected View createView(String viewName, Locale locale) throws Exception {
    return loadView(viewName, locale);
  }

  /**
   * Subclasses must implement this method, building a View object
   * for the specified view. The returned View objects will be
   * cached by this ViewResolver base class.
   * <p>Subclasses are not forced to support internationalization:
   * A subclass that does not may simply ignore the locale parameter.
   *
   * @param viewName the name of the view to retrieve
   * @param locale the Locale to retrieve the view for
   * @return the View instance, or {@code null} if not found
   * (optional, to allow for ViewResolver chaining)
   * @throws Exception if the view couldn't be resolved
   * @see #resolveViewName
   */
  @Nullable
  protected abstract View loadView(String viewName, Locale locale) throws Exception;

  /**
   * Filter that determines if view should be cached.
   *
   * @author Sergey Galkin
   * @author Arjen Poutsma
   */
  @FunctionalInterface
  public interface CacheFilter {

    /**
     * Indicates whether the given view should be cached.
     * The name and locale used to resolve the view are also provided.
     *
     * @param view the view
     * @param viewName the name used to resolve the {@code view}
     * @param locale the locale used to resolve the {@code view}
     * @return {@code true} if the view should be cached; {@code false} otherwise
     */
    boolean shouldCaching(View view, String viewName, Locale locale);
  }

}
