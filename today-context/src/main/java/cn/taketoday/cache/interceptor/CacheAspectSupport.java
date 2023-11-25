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

package cn.taketoday.cache.interceptor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ReactiveStreams;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.function.SingletonSupplier;
import cn.taketoday.util.function.SupplierUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base class for caching aspects, such as the {@link CacheInterceptor} or an
 * AspectJ aspect.
 *
 * <p>This enables the underlying Framework caching infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling relevant methods in the correct order.
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@link CacheOperationSource} is
 * used for determining caching operations, a {@link KeyGenerator} will build the
 * cache keys, and a {@link CacheResolver} will resolve the actual cache(s) to use.
 *
 * <p>Note: A cache aspect is serializable but does not perform any actual caching
 * after deserialization.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class CacheAspectSupport extends AbstractCacheInvoker
        implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {

  private static final Logger log = LoggerFactory.getLogger(CacheAspectSupport.class);

  private final ConcurrentHashMap<CacheOperationCacheKey, CacheOperationMetadata> metadataCache = new ConcurrentHashMap<>(1024);

  private final StandardEvaluationContext originalEvaluationContext = new StandardEvaluationContext();

  private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator(
          new CacheEvaluationContextFactory(originalEvaluationContext));

  @Nullable
  private final ReactiveCachingHandler reactiveCachingHandler = ReactiveStreams.isPresent ? new ReactiveCachingHandler() : null;

  @Nullable
  private CacheOperationSource cacheOperationSource;

  private SingletonSupplier<KeyGenerator> keyGenerator = SingletonSupplier.from(SimpleKeyGenerator::new);

  @Nullable
  private SingletonSupplier<CacheResolver> cacheResolver;

  @Nullable
  private BeanFactory beanFactory;

  private boolean initialized = false;

  /**
   * Configure this aspect with the given error handler, key generator and cache resolver/manager
   * suppliers, applying the corresponding default if a supplier is not resolvable.
   */
  public void configure(@Nullable Supplier<CacheErrorHandler> errorHandler, @Nullable Supplier<KeyGenerator> keyGenerator,
          @Nullable Supplier<CacheResolver> cacheResolver, @Nullable Supplier<CacheManager> cacheManager) {

    this.keyGenerator = new SingletonSupplier<>(keyGenerator, SimpleKeyGenerator::new);
    this.errorHandler = new SingletonSupplier<>(errorHandler, SimpleCacheErrorHandler::new);
    this.cacheResolver = new SingletonSupplier<>(cacheResolver,
            () -> SimpleCacheResolver.of(SupplierUtils.resolve(cacheManager)));
  }

  /**
   * Set one or more cache operation sources which are used to find the cache
   * attributes. If more than one source is provided, they will be aggregated
   * using a {@link CompositeCacheOperationSource}.
   *
   * @see #setCacheOperationSource
   */
  public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
    Assert.notEmpty(cacheOperationSources, "At least 1 CacheOperationSource needs to be specified");
    this.cacheOperationSource = cacheOperationSources.length > 1 ?
                                new CompositeCacheOperationSource(cacheOperationSources) :
                                cacheOperationSources[0];
  }

  /**
   * Set the CacheOperationSource for this cache aspect.
   *
   * @see #setCacheOperationSources
   */
  public void setCacheOperationSource(@Nullable CacheOperationSource cacheOperationSource) {
    this.cacheOperationSource = cacheOperationSource;
  }

  /**
   * Return the CacheOperationSource for this cache aspect.
   */
  @Nullable
  public CacheOperationSource getCacheOperationSource() {
    return this.cacheOperationSource;
  }

  /**
   * Set the default {@link KeyGenerator} that this cache aspect should delegate to
   * if no specific key generator has been set for the operation.
   * <p>The default is a {@link SimpleKeyGenerator}.
   */
  public void setKeyGenerator(KeyGenerator keyGenerator) {
    this.keyGenerator = SingletonSupplier.valueOf(keyGenerator);
  }

  /**
   * Return the default {@link KeyGenerator} that this cache aspect delegates to.
   */
  public KeyGenerator getKeyGenerator() {
    return this.keyGenerator.obtain();
  }

  /**
   * Set the default {@link CacheResolver} that this cache aspect should delegate
   * to if no specific cache resolver has been set for the operation.
   * <p>The default resolver resolves the caches against their names and the
   * default cache manager.
   *
   * @see #setCacheManager
   * @see SimpleCacheResolver
   */
  public void setCacheResolver(@Nullable CacheResolver cacheResolver) {
    this.cacheResolver = SingletonSupplier.ofNullable(cacheResolver);
  }

  /**
   * Return the default {@link CacheResolver} that this cache aspect delegates to.
   */
  @Nullable
  public CacheResolver getCacheResolver() {
    return SupplierUtils.resolve(this.cacheResolver);
  }

  /**
   * Set the {@link CacheManager} to use to create a default {@link CacheResolver}.
   * Replace the current {@link CacheResolver}, if any.
   *
   * @see #setCacheResolver
   * @see SimpleCacheResolver
   */
  public void setCacheManager(CacheManager cacheManager) {
    this.cacheResolver = SingletonSupplier.valueOf(new SimpleCacheResolver(cacheManager));
  }

  /**
   * Set the containing {@link BeanFactory} for {@link CacheManager} and other
   * service lookups.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    this.originalEvaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(getCacheOperationSource() != null, "The 'cacheOperationSources' property is required: " +
            "If there are no cacheable methods, then don't use a cache aspect.");
  }

  @Override
  public void afterSingletonsInstantiated(ConfigurableBeanFactory beanFactory) {
    if (getCacheResolver() == null) {
      // Lazily initialize cache resolver via default cache manager...
      try {
        setCacheManager(beanFactory.getBean(CacheManager.class));
      }
      catch (NoUniqueBeanDefinitionException ex) {
        throw new IllegalStateException("No CacheResolver specified, and no unique bean of type " +
                "CacheManager found. Mark one as primary or declare a specific CacheManager to use.", ex);
      }
      catch (NoSuchBeanDefinitionException ex) {
        throw new IllegalStateException("No CacheResolver specified, and no bean of type CacheManager found. " +
                "Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.", ex);
      }
    }
    this.initialized = true;
  }

  /**
   * Convenience method to return a String representation of this Method
   * for use in logging. Can be overridden in subclasses to provide a
   * different identifier for the given method.
   *
   * @param method the method we're interested in
   * @param targetClass class the method is on
   * @return log message identifying this method
   * @see cn.taketoday.util.ClassUtils#getQualifiedMethodName
   */
  protected String methodIdentification(Method method, Class<?> targetClass) {
    Method specificMethod = ReflectionUtils.getMostSpecificMethod(method, targetClass);
    return ClassUtils.getQualifiedMethodName(specificMethod);
  }

  protected Collection<? extends Cache> getCaches(CacheOperationInvocationContext<CacheOperation> context, CacheResolver cacheResolver) {
    Collection<? extends Cache> caches = cacheResolver.resolveCaches(context);
    if (caches.isEmpty()) {
      throw new IllegalStateException("No cache could be resolved for '" +
              context.getOperation() + "' using resolver '" + cacheResolver +
              "'. At least one cache should be provided per cache operation.");
    }
    return caches;
  }

  protected CacheOperationContext getOperationContext(CacheOperation operation,
          Method method, Object[] args, Object target, Class<?> targetClass) {

    CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
    return new CacheOperationContext(metadata, args, target);
  }

  /**
   * Return the {@link CacheOperationMetadata} for the specified operation.
   * <p>Resolve the {@link CacheResolver} and the {@link KeyGenerator} to be
   * used for the operation.
   *
   * @param operation the operation
   * @param method the method on which the operation is invoked
   * @param targetClass the target type
   * @return the resolved metadata for the operation
   */
  protected CacheOperationMetadata getCacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass) {

    CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
    CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
    if (metadata == null) {
      KeyGenerator operationKeyGenerator;
      if (StringUtils.hasText(operation.getKeyGenerator())) {
        operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
      }
      else {
        operationKeyGenerator = getKeyGenerator();
      }
      CacheResolver operationCacheResolver;
      if (StringUtils.hasText(operation.getCacheResolver())) {
        operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
      }
      else if (StringUtils.hasText(operation.getCacheManager())) {
        CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
        operationCacheResolver = new SimpleCacheResolver(cacheManager);
      }
      else {
        operationCacheResolver = getCacheResolver();
        Assert.state(operationCacheResolver != null, "No CacheResolver/CacheManager set");
      }
      metadata = new CacheOperationMetadata(operation, method, targetClass,
              operationKeyGenerator, operationCacheResolver);
      this.metadataCache.put(cacheKey, metadata);
    }
    return metadata;
  }

  /**
   * Retrieve a bean with the specified name and type.
   * Used to resolve services that are referenced by name in a {@link CacheOperation}.
   *
   * @param name the name of the bean, as defined by the cache operation
   * @param serviceType the type expected by the operation's service reference
   * @return the bean matching the expected type, qualified by the given name
   * @throws cn.taketoday.beans.factory.NoSuchBeanDefinitionException if such bean does not exist
   * @see CacheOperation#getKeyGenerator()
   * @see CacheOperation#getCacheManager()
   * @see CacheOperation#getCacheResolver()
   */
  protected <T> T getBean(String name, Class<T> serviceType) {
    if (this.beanFactory == null) {
      throw new IllegalStateException(
              "BeanFactory must be set on cache aspect for " + serviceType.getSimpleName() + " retrieval");
    }
    return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, serviceType, name);
  }

  /**
   * Clear the cached metadata.
   */
  protected void clearMetadataCache() {
    this.metadataCache.clear();
    this.evaluator.clear();
  }

  @Nullable
  protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
    // Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
    if (this.initialized) {
      Class<?> targetClass = getTargetClass(target);
      CacheOperationSource cacheOperationSource = getCacheOperationSource();
      if (cacheOperationSource != null) {
        Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
        if (CollectionUtils.isNotEmpty(operations)) {
          return execute(invoker, method, new CacheOperationContexts(operations, method, args, target, targetClass));
        }
      }
    }

    return invoker.invoke();
  }

  /**
   * Execute the underlying operation (typically in case of cache miss) and return
   * the result of the invocation. If an exception occurs it will be wrapped in a
   * {@link CacheOperationInvoker.ThrowableWrapper}: the exception can be handled
   * or modified but it <em>must</em> be wrapped in a
   * {@link CacheOperationInvoker.ThrowableWrapper} as well.
   *
   * @param invoker the invoker handling the operation being cached
   * @return the result of the invocation
   * @see CacheOperationInvoker#invoke()
   */
  @Nullable
  protected Object invokeOperation(CacheOperationInvoker invoker) {
    return invoker.invoke();
  }

  private Class<?> getTargetClass(Object target) {
    return AopProxyUtils.ultimateTargetClass(target);
  }

  @Nullable
  private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
    if (contexts.isSynchronized()) {
      // Special handling of synchronized invocation
      return executeSynchronized(invoker, method, contexts);
    }

    // Process any early evictions
    processCacheEvicts(contexts.get(CacheEvictOperation.class), true,
            CacheOperationExpressionEvaluator.NO_RESULT);

    // Check if we have a cached value matching the conditions
    Object cacheHit = findCachedValue(contexts.get(CacheableOperation.class));

    Object cacheValue;
    Object returnValue;

    if (cacheHit != null && !hasCachePut(contexts)) {
      // If there are no put requests, just use the cache hit
      cacheValue = (cacheHit instanceof Cache.ValueWrapper wrapper ? wrapper.get() : cacheHit);
      returnValue = wrapCacheValue(method, cacheValue);
    }
    else {
      // Invoke the method if we don't have a cache hit
      returnValue = invokeOperation(invoker);
      cacheValue = unwrapReturnValue(returnValue);
    }

    // Collect puts from any @Cacheable miss, if no cached value is found
    ArrayList<CachePutRequest> cachePutRequests = new ArrayList<>(1);
    if (cacheHit == null) {
      collectPutRequests(contexts.get(CacheableOperation.class), cacheValue, cachePutRequests);
    }

    // Collect any explicit @CachePuts
    collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);

    // Process any collected put requests, either from @CachePut or a @Cacheable miss
    for (CachePutRequest cachePutRequest : cachePutRequests) {
      Object returnOverride = cachePutRequest.apply(cacheValue);
      if (returnOverride != null) {
        returnValue = returnOverride;
      }
    }

    // Process any late evictions
    Object returnOverride = processCacheEvicts(
            contexts.get(CacheEvictOperation.class), false, returnValue);
    if (returnOverride != null) {
      returnValue = returnOverride;
    }

    return returnValue;
  }

  @Nullable
  private Object executeSynchronized(CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
    CacheOperationContext context = contexts.get(CacheableOperation.class).iterator().next();
    if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
      Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
      Cache cache = context.getCaches().iterator().next();
      if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
        return cache.retrieve(key, () -> (CompletableFuture<?>) invokeOperation(invoker));
      }
      if (reactiveCachingHandler != null) {
        Object returnValue = reactiveCachingHandler.executeSynchronized(invoker, method, cache, key);
        if (returnValue != ReactiveCachingHandler.NOT_HANDLED) {
          return returnValue;
        }
      }
      try {
        return wrapCacheValue(method, cache.get(key, () -> unwrapReturnValue(invokeOperation(invoker))));
      }
      catch (Cache.ValueRetrievalException ex) {
        // Directly propagate ThrowableWrapper from the invoker,
        // or potentially also an IllegalArgumentException etc.
        ReflectionUtils.rethrowRuntimeException(ex.getCause());
        // Never reached
        return null;
      }
    }
    else {
      // No caching required, just call the underlying method
      return invokeOperation(invoker);
    }
  }

  @Nullable
  private static Object wrapCacheValue(Method method, @Nullable Object cacheValue) {
    if (method.getReturnType() == Optional.class
            && (cacheValue == null || cacheValue.getClass() != Optional.class)) {
      return Optional.ofNullable(cacheValue);
    }
    return cacheValue;
  }

  @Nullable
  private static Object unwrapReturnValue(@Nullable Object returnValue) {
    return ObjectUtils.unwrapOptional(returnValue);
  }

  private boolean hasCachePut(CacheOperationContexts contexts) {
    // Evaluate the conditions *without* the result object because we don't have it yet...
    Collection<CacheOperationContext> cachePutContexts = contexts.get(CachePutOperation.class);
    ArrayList<CacheOperationContext> excluded = new ArrayList<>(1);
    for (CacheOperationContext context : cachePutContexts) {
      try {
        if (!context.isConditionPassing(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE)) {
          excluded.add(context);
        }
      }
      catch (VariableNotAvailableException ex) {
        // Ignoring failure due to missing result, consider the cache put has to proceed
      }
    }
    // Check if all puts have been excluded by condition
    return cachePutContexts.size() != excluded.size();
  }

  @Nullable
  private Object processCacheEvicts(Collection<CacheOperationContext> contexts,
          boolean beforeInvocation, @Nullable Object result) {

    if (contexts.isEmpty()) {
      return null;
    }
    List<CacheOperationContext> applicable = contexts.stream()
            .filter(context -> (context.metadata.operation instanceof CacheEvictOperation evict &&
                    beforeInvocation == evict.isBeforeInvocation())).toList();
    if (applicable.isEmpty()) {
      return null;
    }

    if (result instanceof CompletableFuture<?> future) {
      return future.whenComplete((value, ex) -> {
        if (ex == null) {
          performCacheEvicts(applicable, result);
        }
      });
    }
    if (reactiveCachingHandler != null) {
      Object returnValue = reactiveCachingHandler.processCacheEvicts(applicable, result);
      if (returnValue != ReactiveCachingHandler.NOT_HANDLED) {
        return returnValue;
      }
    }
    performCacheEvicts(applicable, result);
    return null;
  }

  private void performCacheEvicts(List<CacheOperationContext> contexts, @Nullable Object result) {
    for (CacheOperationContext context : contexts) {
      if (isConditionPassing(context, result)) {
        Object key = null;
        CacheEvictOperation operation = (CacheEvictOperation) context.metadata.operation;
        for (Cache cache : context.getCaches()) {
          if (operation.isCacheWide()) {
            logInvalidating(context, operation, null);
            doClear(cache, operation.isBeforeInvocation());
          }
          else {
            if (key == null) {
              key = generateKey(context, result);
            }
            logInvalidating(context, operation, key);
            doEvict(cache, key, operation.isBeforeInvocation());
          }
        }
      }
    }
  }

  private static void logInvalidating(CacheOperationContext context, CacheEvictOperation operation, @Nullable Object key) {
    if (log.isTraceEnabled()) {
      log.trace("Invalidating {} for operation {} on method {}",
              (key != null ? "cache key [" + key + "]" : "entire cache"), operation, context.metadata.method);
    }
  }

  /**
   * Find a cached value only for {@link CacheableOperation} that passes the condition.
   *
   * @param contexts the cacheable operations
   * @return a {@link Cache.ValueWrapper} holding the cached value,
   * or {@code null} if none is found
   */
  @Nullable
  private Object findCachedValue(Collection<CacheOperationContext> contexts) {
    for (CacheOperationContext context : contexts) {
      if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
        Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
        Object cached = findInCaches(context, key);
        if (cached != null) {
          if (log.isTraceEnabled()) {
            log.trace("Cache entry for key '{}' found in cache(s) {}", key, context.getCacheNames());
          }
          return cached;
        }
        else if (log.isTraceEnabled()) {
          log.trace("No cache entry for key '{}' in cache(s) {}", key, context.getCacheNames());
        }
      }
    }
    return null;
  }

  /**
   * Collect the {@link CachePutRequest} for all {@link CacheOperation} using
   * the specified result value.
   *
   * @param contexts the contexts to handle
   * @param result the result value (never {@code null})
   * @param putRequests the collection to update
   */
  private void collectPutRequests(Collection<CacheOperationContext> contexts,
          @Nullable Object result, ArrayList<CachePutRequest> putRequests) {

    for (CacheOperationContext context : contexts) {
      if (isConditionPassing(context, result) && context.canPutToCache(result)) {
        Object key = generateKey(context, result);
        putRequests.add(new CachePutRequest(context, key));
      }
    }
  }

  @Nullable
  private Object findInCaches(CacheOperationContext context, Object key) {
    for (Cache cache : context.getCaches()) {
      if (CompletableFuture.class.isAssignableFrom(context.getMethod().getReturnType())) {
        return cache.retrieve(key);
      }
      if (reactiveCachingHandler != null) {
        Object returnValue = reactiveCachingHandler.findInCaches(context, cache, key);
        if (returnValue != ReactiveCachingHandler.NOT_HANDLED) {
          return returnValue;
        }
      }
      return doGet(cache, key);
    }
    return null;
  }

  private boolean isConditionPassing(CacheOperationContext context, @Nullable Object result) {
    boolean passing = context.isConditionPassing(result);
    if (!passing && log.isTraceEnabled()) {
      log.trace("Cache condition failed on method {} for operation {}", context.metadata.method, context.metadata.operation);
    }
    return passing;
  }

  private Object generateKey(CacheOperationContext context, @Nullable Object result) {
    Object key = context.generateKey(result);
    if (key == null) {
      throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
              "using named params on classes without debug info?) " + context.metadata.operation);
    }
    if (log.isTraceEnabled()) {
      log.trace("Computed cache key '{}' for operation {]", key, context.metadata.operation);
    }
    return key;
  }

  private class CacheOperationContexts {

    private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts;

    private final boolean sync;

    public CacheOperationContexts(Collection<? extends CacheOperation> operations,
            Method method, Object[] args, Object target, Class<?> targetClass) {

      this.contexts = MultiValueMap.forLinkedHashMap(operations.size());
      for (CacheOperation op : operations) {
        this.contexts.add(op.getClass(), getOperationContext(op, method, args, target, targetClass));
      }
      this.sync = determineSyncFlag(method);
    }

    public Collection<CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
      Collection<CacheOperationContext> result = this.contexts.get(operationClass);
      return (result != null ? result : Collections.emptyList());
    }

    public boolean isSynchronized() {
      return this.sync;
    }

    private boolean determineSyncFlag(Method method) {
      List<CacheOperationContext> cacheableContexts = this.contexts.get(CacheableOperation.class);
      if (cacheableContexts == null) {  // no @Cacheable operation at all
        return false;
      }
      boolean syncEnabled = false;
      for (CacheOperationContext context : cacheableContexts) {
        if (context.getOperation() instanceof CacheableOperation cacheable && cacheable.isSync()) {
          syncEnabled = true;
          break;
        }
      }
      if (syncEnabled) {
        if (this.contexts.size() > 1) {
          throw new IllegalStateException(
                  "A sync=true operation cannot be combined with other cache operations on '" + method + "'");
        }
        if (cacheableContexts.size() > 1) {
          throw new IllegalStateException(
                  "Only one sync=true operation is allowed on '" + method + "'");
        }
        CacheOperationContext cacheableContext = cacheableContexts.iterator().next();
        CacheOperation operation = cacheableContext.getOperation();
        if (cacheableContext.getCaches().size() > 1) {
          throw new IllegalStateException(
                  "A sync=true operation is restricted to a single cache on '" + operation + "'");
        }
        if (operation instanceof CacheableOperation cacheable && StringUtils.hasText(cacheable.getUnless())) {
          throw new IllegalStateException(
                  "A sync=true operation does not support the unless attribute on '" + operation + "'");
        }
        return true;
      }
      return false;
    }
  }

  /**
   * Metadata of a cache operation that does not depend on a particular invocation
   * which makes it a good candidate for caching.
   */
  protected static class CacheOperationMetadata {
    public final CacheOperation operation;

    public final Method method;

    public final Class<?> targetClass;

    public final Method targetMethod;

    public final AnnotatedElementKey methodKey;

    public final KeyGenerator keyGenerator;

    public final CacheResolver cacheResolver;

    public CacheOperationMetadata(CacheOperation operation, Method method,
            Class<?> targetClass, KeyGenerator keyGenerator, CacheResolver cacheResolver) {

      this.operation = operation;
      this.targetClass = targetClass;
      this.method = BridgeMethodResolver.findBridgedMethod(method);
      this.targetMethod = !Proxy.isProxyClass(targetClass)
                          ? AopUtils.getMostSpecificMethod(method, targetClass)
                          : this.method;
      this.methodKey = new AnnotatedElementKey(this.targetMethod, targetClass);
      this.keyGenerator = keyGenerator;
      this.cacheResolver = cacheResolver;
    }
  }

  /**
   * A {@link CacheOperationInvocationContext} context for a {@link CacheOperation}.
   */
  protected class CacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {

    public final CacheOperationMetadata metadata;

    public final Object[] args;

    public final Object target;

    public final Collection<? extends Cache> caches;

    public final Collection<String> cacheNames;

    @Nullable
    public Boolean conditionPassing;

    public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
      this.metadata = metadata;
      this.args = extractArgs(metadata.method, args);
      this.target = target;
      this.caches = CacheAspectSupport.this.getCaches(this, metadata.cacheResolver);
      this.cacheNames = createCacheNames(this.caches);
    }

    @Override
    public CacheOperation getOperation() {
      return this.metadata.operation;
    }

    @Override
    public Object getTarget() {
      return this.target;
    }

    @Override
    public Method getMethod() {
      return this.metadata.method;
    }

    @Override
    public Object[] getArgs() {
      return this.args;
    }

    private Object[] extractArgs(Method method, Object[] args) {
      if (!method.isVarArgs()) {
        return args;
      }
      Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
      Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
      System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
      System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
      return combinedArgs;
    }

    protected boolean isConditionPassing(@Nullable Object result) {
      if (this.conditionPassing == null) {
        if (metadata.operation.hasConditionString()) {
          EvaluationContext evaluationContext = createEvaluationContext(result);
          this.conditionPassing = evaluator.condition(metadata.operation.getCondition(),
                  metadata.methodKey, evaluationContext);
        }
        else {
          this.conditionPassing = true;
        }
      }
      return this.conditionPassing;
    }

    protected boolean canPutToCache(@Nullable Object value) {
      String unless;
      if (metadata.operation instanceof CacheableOperation cacheable) {
        unless = cacheable.getUnless();
      }
      else if (metadata.operation instanceof CachePutOperation cachePut) {
        unless = cachePut.getUnless();
      }
      else {
        return true;
      }
      if (StringUtils.hasText(unless)) {
        EvaluationContext evaluationContext = createEvaluationContext(value);
        return !evaluator.unless(unless, metadata.methodKey, evaluationContext);
      }
      return true;
    }

    /**
     * Compute the key for the given caching operation.
     */
    @Nullable
    protected Object generateKey(@Nullable Object result) {
      if (metadata.operation.hasKeyString()) {
        EvaluationContext evaluationContext = createEvaluationContext(result);
        return evaluator.key(metadata.operation.getKey(), metadata.methodKey, evaluationContext);
      }
      return metadata.keyGenerator.generate(target, metadata.method, args);
    }

    private EvaluationContext createEvaluationContext(@Nullable Object result) {
      return evaluator.createEvaluationContext(this.caches, this.metadata.method, this.args,
              this.target, this.metadata.targetClass, this.metadata.targetMethod, result);
    }

    protected Collection<? extends Cache> getCaches() {
      return this.caches;
    }

    protected Collection<String> getCacheNames() {
      return this.cacheNames;
    }

    private Collection<String> createCacheNames(Collection<? extends Cache> caches) {
      ArrayList<String> names = new ArrayList<>(caches.size());
      for (Cache cache : caches) {
        names.add(cache.getName());
      }
      return names;
    }
  }

  private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {

    private final CacheOperation cacheOperation;

    private final AnnotatedElementKey methodCacheKey;

    private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
      this.cacheOperation = cacheOperation;
      this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return this == other
              || (other instanceof CacheOperationCacheKey that
              && this.cacheOperation.equals(that.cacheOperation)
              && this.methodCacheKey.equals(that.methodCacheKey));
    }

    @Override
    public int hashCode() {
      return (this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode());
    }

    @Override
    public String toString() {
      return this.cacheOperation + " on " + this.methodCacheKey;
    }

    @Override
    public int compareTo(CacheOperationCacheKey other) {
      int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
      if (result == 0) {
        result = this.methodCacheKey.compareTo(other.methodCacheKey);
      }
      return result;
    }
  }

  private class CachePutRequest {

    private final CacheOperationContext context;

    private final Object key;

    public CachePutRequest(CacheOperationContext context, Object key) {
      this.context = context;
      this.key = key;
    }

    @Nullable
    public Object apply(@Nullable Object result) {
      if (result instanceof CompletableFuture<?> future) {
        return future.whenComplete((value, ex) -> {
          if (ex != null) {
            performEvict(ex);
          }
          else {
            performPut(value);
          }
        });
      }
      if (reactiveCachingHandler != null) {
        Object returnValue = reactiveCachingHandler.processPutRequest(this, result);
        if (returnValue != ReactiveCachingHandler.NOT_HANDLED) {
          return returnValue;
        }
      }
      performPut(result);
      return null;
    }

    void performPut(@Nullable Object value) {
      if (log.isTraceEnabled()) {
        log.trace("Creating cache entry for key '{}' in cache(s) {}", context.getCacheNames(), key);
      }
      for (Cache cache : this.context.getCaches()) {
        doPut(cache, this.key, value);
      }
    }

    void performEvict(Throwable cause) {
      if (log.isTraceEnabled()) {
        log.trace("Removing cache entry for key '{}' from cache(s) {} due to exception: {}",
                key, context.getCacheNames(), cause);
      }
      for (Cache cache : this.context.getCaches()) {
        doEvict(cache, this.key, false);
      }
    }
  }

  /**
   * Reactive Streams Subscriber collection for collecting a List to cache.
   */
  private static class CachePutListSubscriber implements Subscriber<Object> {

    private final CachePutRequest request;

    private final List<Object> cacheValue = new ArrayList<>();

    public CachePutListSubscriber(CachePutRequest request) {
      this.request = request;
    }

    @Override
    public void onSubscribe(Subscription s) {
      s.request(Integer.MAX_VALUE);
    }

    @Override
    public void onNext(Object o) {
      this.cacheValue.add(o);
    }

    @Override
    public void onError(Throwable t) {
      this.request.performEvict(t);
    }

    @Override
    public void onComplete() {
      this.request.performPut(this.cacheValue);
    }
  }

  /**
   * Inner class to avoid a hard dependency on the Reactive Streams API at runtime.
   */
  private final class ReactiveCachingHandler {

    public static final Object NOT_HANDLED = new Object();

    private static final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

    public Object executeSynchronized(CacheOperationInvoker invoker, Method method, Cache cache, Object key) {
      ReactiveAdapter adapter = registry.getAdapter(method.getReturnType());
      if (adapter != null) {
        if (adapter.isMultiValue()) {
          // Flux or similar
          return adapter.fromPublisher(Flux.from(Mono.fromFuture(cache.retrieve(key, () -> Flux.from(adapter.toPublisher(invokeOperation(invoker)))
                          .collectList().toFuture())))
                  .flatMap(Flux::fromIterable));
        }
        else {
          // Mono or similar
          return adapter.fromPublisher(Mono.fromFuture(
                  cache.retrieve(key, () -> Mono.from(adapter.toPublisher(invokeOperation(invoker))).toFuture())));
        }
      }
      return NOT_HANDLED;
    }

    public Object processCacheEvicts(List<CacheOperationContext> contexts, @Nullable Object result) {
      ReactiveAdapter adapter = (result != null ? registry.getAdapter(result.getClass()) : null);
      if (adapter != null) {
        return adapter.fromPublisher(Mono.from(adapter.toPublisher(result))
                .doOnSuccess(value -> performCacheEvicts(contexts, result)));
      }
      return NOT_HANDLED;
    }

    @Nullable
    public Object findInCaches(CacheOperationContext context, Cache cache, Object key) {
      ReactiveAdapter adapter = registry.getAdapter(context.getMethod().getReturnType());
      if (adapter != null) {
        CompletableFuture<?> cachedFuture = cache.retrieve(key);
        if (cachedFuture == null) {
          return null;
        }
        if (adapter.isMultiValue()) {
          return adapter.fromPublisher(Flux.from(Mono.fromFuture(cachedFuture))
                  .flatMap(v -> (v instanceof Iterable<?> iv ? Flux.fromIterable(iv) : Flux.just(v))));
        }
        else {
          return adapter.fromPublisher(Mono.fromFuture(cachedFuture));
        }
      }
      return NOT_HANDLED;
    }

    public Object processPutRequest(CachePutRequest request, @Nullable Object result) {
      ReactiveAdapter adapter = (result != null ? registry.getAdapter(result.getClass()) : null);
      if (adapter != null) {
        if (adapter.isMultiValue()) {
          Flux<?> source = Flux.from(adapter.toPublisher(result));
          source.subscribe(new CachePutListSubscriber(request));
          return adapter.fromPublisher(source);
        }
        else {
          return adapter.fromPublisher(Mono.from(adapter.toPublisher(result))
                  .doOnSuccess(request::performPut).doOnError(request::performEvict));
        }
      }
      return NOT_HANDLED;
    }
  }

}
