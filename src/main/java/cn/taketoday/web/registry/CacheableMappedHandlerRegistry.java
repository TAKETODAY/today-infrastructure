package cn.taketoday.web.registry;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.ConcurrentMapCache;
import cn.taketoday.context.EmptyObject;

/**
 * Cache MappedHandlerRegistry
 *
 * @author TODAY
 * @date 2020/12/11 23:32
 * @since 3.0
 */
public class CacheableMappedHandlerRegistry extends MappedHandlerRegistry {
  static final String CACHE_NAME = CacheableMappedHandlerRegistry.class + "pattern-matching";

  private Cache patternMatchingCache;

  @Override
  protected Object lookupPatternHandler(String handlerKey) {
    final Cache patternMatchingCache = getPatternMatchingCache();
    Object handler = patternMatchingCache.get(handlerKey, false);
    if (handler == null) {
      handler = lookupCacheValue(handlerKey);
      patternMatchingCache.put(handlerKey, handler);
    }
    else if (handler == EmptyObject.INSTANCE) {
      return null;
    }
    return handler;
  }

  protected Object lookupCacheValue(final String handlerKey) {
    return super.lookupPatternHandler(handlerKey);
  }

  public final Cache getPatternMatchingCache() {
    if (patternMatchingCache == null) {
      patternMatchingCache = createPatternMatchingCache();
    }
    return patternMatchingCache;
  }

  protected ConcurrentMapCache createPatternMatchingCache() {
    return new ConcurrentMapCache(CACHE_NAME, 128);
  }

  /**
   * Set Pattern matching Cache
   *
   * @param patternMatchingCache
   *         a new Cache
   */
  public void setPatternMatchingCache(final Cache patternMatchingCache) {
    this.patternMatchingCache = patternMatchingCache;
  }
}
