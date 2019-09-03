/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.context.cglib.core;

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.context.asm.ClassReader;
import cn.taketoday.context.cglib.core.internal.LoadingCache;
import cn.taketoday.context.utils.ClassUtils;

/**
 * Abstract class for all code-generating CGLIB utilities. In addition to
 * caching generated classes for performance, it provides hooks for customizing
 * the <code>ClassLoader</code>, name of the generated class, and
 * transformations applied before generation.
 */
@SuppressWarnings("all")
public abstract class AbstractClassGenerator<T> implements ClassGenerator {

    private static volatile Map<ClassLoader, ClassLoaderData> CACHE = new WeakHashMap<>();
    private static final ThreadLocal<AbstractClassGenerator> CURRENT = new ThreadLocal<>();

    private GeneratorStrategy strategy = DefaultGeneratorStrategy.INSTANCE;
    private NamingPolicy namingPolicy = DefaultNamingPolicy.INSTANCE;

    private final Source source;
    private ClassLoader classLoader;
    private String namePrefix;
    private Object key;
    private boolean useCache = true;
    private String className;
    private boolean attemptLoad;

    protected static class ClassLoaderData {

        private final Set<String> reservedClassNames = new HashSet<>();

        /**
         * {@link AbstractClassGenerator} here holds "cache key" (e.g.
         * {@link cn.taketoday.context.cglib.proxy.Enhancer} configuration), and the
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

        private final Predicate uniqueNamePredicate = new Predicate() {
            public boolean test(Object name) {
                return reservedClassNames.contains(name);
            }
        };

        private static final Function<AbstractClassGenerator, Object> GET_KEY = new Function<AbstractClassGenerator, Object>() {
            public Object apply(AbstractClassGenerator gen) {
                return gen.key;
            }
        };

        public ClassLoaderData(ClassLoader classLoader) {
            if (classLoader == null) {
                throw new IllegalArgumentException("classLoader == null is not yet supported");
            }

            this.classLoader = new WeakReference<>(classLoader);

            final Function<AbstractClassGenerator, Object> load = (gen) -> gen.wrapCachedClass(gen.generate(this));

            this.generatedClasses = new LoadingCache<AbstractClassGenerator, Object, Object>(GET_KEY, load);
        }

        public ClassLoader getClassLoader() {
            return classLoader.get();
        }

        public void reserveName(String name) {
            reservedClassNames.add(name);
        }

        public Predicate getUniqueNamePredicate() {
            return uniqueNamePredicate;
        }

        public Object get(AbstractClassGenerator gen, boolean useCache) {
            if (!useCache) {
                return gen.generate(ClassLoaderData.this);
            }
            Object cachedValue = generatedClasses.get(gen);
            return gen.unwrapCachedValue(cachedValue);
        }

    }

    protected T wrapCachedClass(Class klass) {
        return (T) new WeakReference(klass);
    }

    protected Object unwrapCachedValue(T cached) {
        return ((WeakReference) cached).get();
    }

    protected static class Source {
        final String name;

        public Source(String name) {
            this.name = name;
        }
    }

    protected AbstractClassGenerator(Source source) {
        this.source = source;
    }

    protected AbstractClassGenerator setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    final protected String getClassName() {
        return className;
    }

    private AbstractClassGenerator setClassName(String className) {
        this.className = className;
        return this;
    }

    private String generateClassName(Predicate nameTestPredicate) {
        return namingPolicy.getClassName(namePrefix, source.name, key, nameTestPredicate);
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
     * @param classLoader
     *            the loader to generate the new class with, or null to use the
     *            default
     */
    public AbstractClassGenerator setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Override the default naming policy.
     * 
     * @see DefaultNamingPolicy
     * @param namingPolicy
     *            the custom policy, or null to use the default
     */
    public AbstractClassGenerator setNamingPolicy(NamingPolicy namingPolicy) {
        if (namingPolicy == null)
            namingPolicy = DefaultNamingPolicy.INSTANCE;
        this.namingPolicy = namingPolicy;
        return this;
    }

    /**
     * @see #setNamingPolicy
     */
    public NamingPolicy getNamingPolicy() {
        return namingPolicy;
    }

    /**
     * Whether use and update the static cache of generated classes for a class with
     * the same properties. Default is <code>true</code>.
     */
    public AbstractClassGenerator setUseCache(boolean useCache) {
        this.useCache = useCache;
        return this;
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
    public AbstractClassGenerator setAttemptLoad(boolean attemptLoad) {
        this.attemptLoad = attemptLoad;
        return this;
    }

    public boolean getAttemptLoad() {
        return attemptLoad;
    }

    /**
     * Set the strategy to use to create the bytecode from this generator. By
     * default an instance of {@see DefaultGeneratorStrategy} is used.
     */
    public AbstractClassGenerator setStrategy(GeneratorStrategy strategy) {
        if (strategy == null)
            strategy = DefaultGeneratorStrategy.INSTANCE;
        this.strategy = strategy;
        return this;
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
    public static AbstractClassGenerator getCurrent() {
        return CURRENT.get();
    }

    public ClassLoader getClassLoader() {
        ClassLoader t = classLoader;
        if (t == null) {
            t = getDefaultClassLoader();
        }
        if (t == null) {
            t = getClass().getClassLoader();
        }
        if (t == null) {
            t = Thread.currentThread().getContextClassLoader();
        }
        if (t == null) {
            throw new IllegalStateException("Cannot determine classloader");
        }
        return t;
    }

    abstract protected ClassLoader getDefaultClassLoader();

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
    protected ProtectionDomain getProtectionDomain() {
        return null;
    }

    protected Object create(Object key) {
        try {

            ClassLoader loader = getClassLoader();
            Map<ClassLoader, ClassLoaderData> cache = CACHE;
            ClassLoaderData data = cache.get(loader);
            if (data == null) {
                synchronized (AbstractClassGenerator.class) {
                    cache = CACHE;
                    data = cache.get(loader);
                    if (data == null) {
                        Map<ClassLoader, ClassLoaderData> newCache = //
                                new WeakHashMap<ClassLoader, ClassLoaderData>(cache);

                        newCache.put(loader, data = new ClassLoaderData(loader));
                        CACHE = newCache;
                    }
                }
            }
            this.key = key;
            Object obj = data.get(this, getUseCache());
            if (obj instanceof Class) {
                return firstInstance((Class) obj);
            }
            return nextInstance(obj);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Error e) {
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
                throw new IllegalStateException("ClassLoader is null while trying to define class " + getClassName() + ". It seems that the loader has been expired from a weak reference somehow. " + "Please file an issue at cglib's issue tracker.");
            }
            synchronized (classLoader) {
                String name = generateClassName(data.getUniqueNamePredicate());
                data.reserveName(name);
                this.setClassName(name);
            }
            if (attemptLoad) {
                try {
                    return classLoader.loadClass(getClassName());
                }
                catch (ClassNotFoundException e) {
                    // ignore
                }
            }
            final byte[] b = strategy.generate(this);
            String className = ClassUtils.getClassName(new ClassReader(b));
            ProtectionDomain protectionDomain = getProtectionDomain();
            synchronized (classLoader) { // just in case
                if (protectionDomain == null) {
                    return ReflectUtils.defineClass(className, b, classLoader);
                }
                return ReflectUtils.defineClass(className, b, classLoader, protectionDomain);
            }
        }
        catch (RuntimeException | Error e) {
            throw e;
        }
        catch (Exception e) {
            throw new CodeGenerationException(e);
        } finally {
            CURRENT.set(save);
        }
    }

    abstract protected Object firstInstance(Class<T> type) throws Exception;

    abstract protected Object nextInstance(Object instance) throws Exception;

}
