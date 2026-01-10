
/**
 * AOP-based solution for declarative caching demarcation using JSR-107 annotations.
 *
 * <p>Strongly based on the infrastructure in infra.cache.interceptor
 * that deals with Framework's caching annotations.
 *
 * <p>Builds on the AOP infrastructure in infra.aop.framework.
 * Any POJO can be cache-advised with Framework.
 */
@NullMarked
package infra.cache.jcache.interceptor;

import org.jspecify.annotations.NullMarked;
