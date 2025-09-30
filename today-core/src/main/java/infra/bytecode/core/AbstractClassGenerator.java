/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import infra.bytecode.BytecodeCompiler;
import infra.bytecode.proxy.Enhancer;
import infra.core.NativeDetector;
import infra.core.NativeDetector.Context;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;

/**
 * Abstract class for all code-generating CGLIB utilities. In addition to
 * caching generated classes for performance, it provides hooks for customizing
 * the <code>ClassLoader</code>, name of the generated class, and
 * transformations applied before generation.
 *
 * @param <T> target
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-04 20:12
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractClassGenerator<T> implements ClassGenerator {

  private static volatile WeakHashMap<ClassLoader, ClassLoaderData> CACHE = new WeakHashMap<>();

  private static final ThreadLocal<AbstractClassGenerator> CURRENT = new ThreadLocal<>();

  private static final boolean DEFAULT_USE_CACHE =
          TodayStrategies.getFlag("bytecode.useCache", true);

  private static final boolean inNativeImage = NativeDetector.inNativeImage(Context.RUN, Context.BUILD);

  private GeneratorStrategy strategy = DefaultGeneratorStrategy.INSTANCE;

  private NamingPolicy namingPolicy = DefaultNamingPolicy.INSTANCE;

  private final String source;

  @Nullable
  private ClassLoader classLoader;

  private String namePrefix;

  private Object key;

  private boolean useCache = DEFAULT_USE_CACHE;

  private String className;

  private boolean attemptLoad;

  // @since 4.0
  private Class<?> neighbor;

  // @since 4.0
  @Nullable
  private DefineClassStrategy defineClassStrategy;

  protected AbstractClassGenerator(String source) {
    this.source = source;
  }

  protected T wrapCachedClass(Class klass) {
    return (T) new WeakReference(klass);
  }

  protected Object unwrapCachedValue(T cached) {
    return ((WeakReference) cached).get();
  }

  protected AbstractClassGenerator(Class<?> source) {
    this(source.getSimpleName());
  }

  protected void setNamePrefix(String namePrefix) {
    this.namePrefix = namePrefix;
  }

  protected final String getClassName() {
    return className;
  }

  private void setClassName(String className) {
    this.className = className;
  }

  private String generateClassName(Predicate<String> nameTestPredicate) {
    return namingPolicy.getClassName(namePrefix, source, key, nameTestPredicate);
  }

  /**
   * Set the <code>ClassLoader</code> in which the class will be generated.
   * Concrete subclasses of <code>AbstractClassGenerator</code> (such as
   * <code>Enhancer</code>) will try to choose an appropriate default if this is
   * unset.
   * <p>
   * Classes are cached per-<code>ClassLoader</code> using a
   * <code>WeakHashMap</code>, to allow the generated classes to be removed when
   * the associated loader is garbage collected.
   *
   * @param classLoader the loader to generate the new class with, or null to use the
   * default
   */
  public void setClassLoader(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Override the default naming policy.
   *
   * @param namingPolicy the custom policy, or null to use the default
   * @see DefaultNamingPolicy
   */
  public void setNamingPolicy(@Nullable NamingPolicy namingPolicy) {
    this.namingPolicy = namingPolicy == null ? DefaultNamingPolicy.INSTANCE : namingPolicy;
  }

  /**
   * @see #setNamingPolicy
   */
  public NamingPolicy getNamingPolicy() {
    return namingPolicy;
  }

  public void setDefineClassStrategy(@Nullable DefineClassStrategy defineClassStrategy) {
    this.defineClassStrategy = defineClassStrategy;
  }

  @Nullable
  public DefineClassStrategy getDefineClassStrategy() {
    return defineClassStrategy;
  }

  /**
   * Whether use and update the static cache of generated classes for a class with
   * the same properties. Default is <code>true</code>.
   */
  public void setUseCache(boolean useCache) {
    this.useCache = useCache;
  }

  /**
   * @see #setUseCache
   */
  public boolean getUseCache() {
    return useCache;
  }

  /**
   * If set, CGLIB will attempt to load classes from the specified
   * <code>ClassLoader</code> before generating them. Because generated class
   * names are not guaranteed to be unique, the default is <code>false</code>.
   */
  public void setAttemptLoad(boolean attemptLoad) {
    this.attemptLoad = attemptLoad;
  }

  public boolean isAttemptLoad() {
    return attemptLoad;
  }

  /**
   * Set the strategy to use to create the bytecode from this generator. By
   * default an instance of {@see DefaultGeneratorStrategy} is used.
   */
  public void setStrategy(GeneratorStrategy strategy) {
    this.strategy = strategy == null ? DefaultGeneratorStrategy.INSTANCE : strategy;
  }

  /**
   * @see #setStrategy
   */
  public GeneratorStrategy getStrategy() {
    return strategy;
  }

  /**
   * Used internally by CGLIB. Returns the <code>AbstractClassGenerator</code>
   * that is being used to generate a class in the current thread.
   */
  @Nullable
  public static AbstractClassGenerator getCurrent() {
    return CURRENT.get();
  }

  public ClassLoader getClassLoader() {
    ClassLoader t = classLoader;
    if (t == null) {
      t = getDefaultClassLoader();
    }
    if (t == null) {
      return ClassUtils.getDefaultClassLoader();
    }
    return t;
  }

  @Nullable
  protected abstract ClassLoader getDefaultClassLoader();

  /**
   * Returns the protection domain to use when defining the class.
   * <p>
   * Default implementation returns <code>null</code> for using a default
   * protection domain. Sub-classes may override to use a more specific protection
   * domain.
   * </p>
   *
   * @return the protection domain (<code>null</code> for using a default)
   */
  @Nullable
  protected ProtectionDomain getProtectionDomain() {
    return null;
  }

  protected Object create(Object key) {
    this.key = key;

    try {
      final ClassLoader loader = getClassLoader();
      ClassLoaderData data = CACHE.get(loader);
      if (data == null) {
        synchronized(AbstractClassGenerator.class) {
          data = CACHE.get(loader);
          if (data == null) {
            WeakHashMap<ClassLoader, ClassLoaderData> newCache = new WeakHashMap<>(CACHE);
            newCache.put(loader, data = new ClassLoaderData(loader));
            CACHE = newCache;
          }
        }
      }

      final Object obj = data.get(this, getUseCache());
      return obj instanceof Class
              ? firstInstance((Class<T>) obj)
              : nextInstance(obj);
    }
    catch (RuntimeException | Error e) {
      throw e;
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }

  protected Class generate(ClassLoaderData data) {

    AbstractClassGenerator save = CURRENT.get();
    CURRENT.set(this);
    try {
      ClassLoader classLoader = data.getClassLoader();
      if (classLoader == null) {
        throw new IllegalStateException(
                "ClassLoader is null while trying to define class " + getClassName()
                        + ". It seems that the loader has been expired from a weak reference somehow. "
                        + "Please file an issue at cglib's issue tracker.");
      }
      synchronized(classLoader) {
        String name = generateClassName(data);
        data.reserveName(name);
        this.setClassName(name);
      }
      if (isAttemptLoad()) {
        try {
          synchronized(classLoader) { // just in case
            return BytecodeCompiler.loadClass(getClassName(), classLoader);
          }
        }
        catch (ClassNotFoundException ignored) {
        }
      }

      if (inNativeImage) {
        throw new UnsupportedOperationException("CGLIB runtime enhancement not supported on native image. " +
                "Make sure to enable Infra AOT processing to pre-generate '" + getClassName() + "' at build time.");
      }

      final byte[] bytes = getStrategy().generate(this);
      synchronized(classLoader) {
        DefineClassStrategy defineClassStrategy = getDefineClassStrategy();
        if (defineClassStrategy != null) {
          return defineClassStrategy.defineClass(
                  getClassName(), classLoader, getProtectionDomain(), neighbor, bytes);
        }
        return BytecodeCompiler.compile(getClassName(), neighbor, classLoader, getProtectionDomain(), bytes);
      }
    }
    catch (RuntimeException | Error e) {
      throw e;
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
    finally {
      CURRENT.set(save);
    }
  }

  // @since 4.0
  public void setNeighbor(Class<?> neighbor) {
    this.neighbor = neighbor;
  }

  protected abstract Object firstInstance(Class<T> type) throws Exception;

  protected abstract Object nextInstance(Object instance) throws Exception;

  protected static class ClassLoaderData implements Predicate<String> {

    private final HashSet<String> reservedClassNames = new HashSet<>();

    /**
     * {@link AbstractClassGenerator} here holds "cache key" (e.g.
     * {@link Enhancer} configuration), and the
     * value is the generated class plus some additional values (see
     * {@link #unwrapCachedValue(Object)}.
     * <p>
     * The generated classes can be reused as long as their classloader is
     * reachable.
     * </p>
     * <p>
     * Note: the only way to access a class is to find it through generatedClasses
     * cache, thus the key should not expire as long as the class itself is alive
     * (its classloader is alive).
     * </p>
     */
    private final LoadingCache<AbstractClassGenerator, Object, Object> generatedClasses;

    /**
     * Note: ClassLoaderData object is stored as a value of
     * {@code WeakHashMap<ClassLoader, ...>} thus this classLoader reference should
     * be weak otherwise it would make classLoader strongly reachable and alive
     * forever. Reference queue is not required since the cleanup is handled by
     * {@link WeakHashMap}.
     */
    private final WeakReference<ClassLoader> classLoader;

    private static final Function<AbstractClassGenerator, Object> GET_KEY = gen -> gen.key;

    public ClassLoaderData(ClassLoader classLoader) {
      Assert.notNull(classLoader, "classLoader == null is not yet supported");
      this.classLoader = new WeakReference<>(classLoader);
      this.generatedClasses = new LoadingCache<>(GET_KEY, gen -> gen.wrapCachedClass(gen.generate(this)));
    }

    @Nullable
    public ClassLoader getClassLoader() {
      return classLoader.get();
    }

    public void reserveName(String name) {
      reservedClassNames.add(name);
    }

    @Override
    public boolean test(String name) {
      return reservedClassNames.contains(name);
    }

    public Object get(AbstractClassGenerator gen, boolean useCache) {
      Object value = null;
      if (useCache) {
        Object cachedValue = generatedClasses.get(gen);
        value = gen.unwrapCachedValue(cachedValue);
      }
      if (value == null) {  // fallback when cached WeakReference returns null
        value = gen.generate(ClassLoaderData.this);
      }
      return value;
    }

  }

}
