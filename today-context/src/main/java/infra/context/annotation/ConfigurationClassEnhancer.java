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

package infra.context.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import infra.aop.framework.StandardProxy;
import infra.aop.scope.ScopedProxyFactoryBean;
import infra.aot.AotDetector;
import infra.beans.BeanInstantiationException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.InstantiationStrategy;
import infra.beans.support.BeanInstantiator;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.ClassGenerator;
import infra.bytecode.core.ClassLoaderAwareGeneratorStrategy;
import infra.bytecode.core.CodeGenerationException;
import infra.bytecode.core.NamingPolicy;
import infra.bytecode.proxy.Callback;
import infra.bytecode.proxy.CallbackFilter;
import infra.bytecode.proxy.Enhancer;
import infra.bytecode.proxy.Factory;
import infra.bytecode.proxy.MethodInterceptor;
import infra.bytecode.proxy.MethodProxy;
import infra.bytecode.proxy.NoOp;
import infra.bytecode.transform.TransformingClassGenerator;
import infra.core.SmartClassLoader;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;

/**
 * Enhances {@link Configuration} classes by generating a CGLIB subclass which
 * interacts with the IoC to respect bean scoping semantics for
 * {@code @Component} methods. Each such {@code @Component} method will be overridden in
 * the generated subclass, only delegating to the actual {@code @Component} method
 * implementation if the container actually requests the construction of a new
 * instance. Otherwise, a call to such an {@code @Component} method serves as a
 * reference back to the container, obtaining the corresponding bean by name.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #enhance
 * @see ConfigurationClassPostProcessor
 * @since 4.0
 */
class ConfigurationClassEnhancer {

  // The callbacks to use. Note that these callbacks must be stateless.
  static final Callback[] CALLBACKS = new Callback[] {
          new ComponentMethodInterceptor(),
          new BeanFactoryAwareMethodInterceptor(),
          NoOp.INSTANCE
  };

  private static final ConditionalCallbackFilter CALLBACK_FILTER = new ConditionalCallbackFilter(CALLBACKS);

  private static final String BEAN_FACTORY_FIELD = "$$beanFactory";

  private static final Logger log = LoggerFactory.getLogger(ConfigurationClassEnhancer.class);

  /**
   * Loads the specified class and generates a CGLIB subclass of it equipped with
   * container-aware callbacks capable of respecting scoping and other bean semantics.
   *
   * @return the enhanced subclass
   */
  public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
    if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
      if (log.isDebugEnabled()) {
        log.debug("Ignoring request to enhance {} as it has " +
                        "already been enhanced. This usually indicates that more than one " +
                        "ConfigurationClassPostProcessor has been registered. This is harmless, but you may " +
                        "want check your configuration and remove one CCPP if possible",
                configClass.getName());
      }
      return configClass;
    }
    try {
      // Use original ClassLoader if config class not locally loaded in overriding class loader
      boolean classLoaderMismatch = (classLoader != null && classLoader != configClass.getClassLoader());
      if (classLoaderMismatch && classLoader instanceof SmartClassLoader smartClassLoader) {
        classLoader = smartClassLoader.getOriginalClassLoader();
        classLoaderMismatch = (classLoader != configClass.getClassLoader());
      }
      // Use original ClassLoader if config class relies on package visibility
      if (classLoaderMismatch && reliesOnPackageVisibility(configClass)) {
        classLoader = configClass.getClassLoader();
        classLoaderMismatch = false;
      }
      Enhancer enhancer = newEnhancer(configClass, classLoader);
      Class<?> enhancedClass = createClass(enhancer, classLoaderMismatch);
      if (log.isTraceEnabled()) {
        log.trace("Successfully enhanced {}; enhanced class name is: {}",
                configClass.getName(), enhancedClass.getName());
      }
      return enhancedClass;
    }
    catch (CodeGenerationException ex) {
      throw new BeanDefinitionStoreException("Could not enhance configuration class [" + configClass.getName() +
              "]. Consider declaring @Configuration(proxyBeanMethods=false) without inter-bean references " +
              "between @Bean methods on the configuration class, avoiding the need for CGLIB enhancement.", ex);
    }
  }

  /**
   * Checks whether the given config class relies on package visibility, either for
   * the class and any of its constructors or for any of its {@code @Bean} methods.
   */
  private boolean reliesOnPackageVisibility(Class<?> configSuperClass) {
    int mod = configSuperClass.getModifiers();
    if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
      return true;
    }
    for (Constructor<?> ctor : configSuperClass.getDeclaredConstructors()) {
      mod = ctor.getModifiers();
      if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
        return true;
      }
    }
    for (Method method : ReflectionUtils.getDeclaredMethods(configSuperClass)) {
      if (BeanAnnotationHelper.isBeanAnnotated(method)) {
        mod = method.getModifiers();
        if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Creates a new CGLIB {@link Enhancer} instance.
   */
  private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
    Enhancer enhancer = new Enhancer();
    if (classLoader != null) {
      enhancer.setClassLoader(classLoader);
      if (classLoader instanceof SmartClassLoader scl && scl.isClassReloadable(configSuperClass)) {
        enhancer.setUseCache(false);
      }
    }
    enhancer.setUseFactory(false);
    enhancer.setSuperclass(configSuperClass);
    enhancer.setInterfaces(EnhancedConfiguration.class);
    enhancer.setNamingPolicy(NamingPolicy.forInfrastructure());
    enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
    enhancer.setAttemptLoad(enhancer.getUseCache() && AotDetector.useGeneratedArtifacts());
    enhancer.setCallbackFilter(CALLBACK_FILTER);
    enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
    return enhancer;
  }

  /**
   * Uses enhancer to generate a subclass of superclass,
   * ensuring that callbacks are registered for the new subclass.
   */
  private Class<?> createClass(Enhancer enhancer, boolean fallback) {
    Class<?> subclass;
    try {
      subclass = enhancer.createClass();
    }
    catch (Throwable ex) {
      if (!fallback) {
        throw (ex instanceof CodeGenerationException cgex ? cgex : new CodeGenerationException(ex));
      }
      // Possibly a package-visible @Bean method declaration not accessible
      // in the given ClassLoader -> retry with original ClassLoader
      enhancer.setClassLoader(null);
      subclass = enhancer.createClass();
    }

    // Registering callbacks statically (as opposed to thread-local)
    Enhancer.registerStaticCallbacks(subclass, CALLBACKS);
    return subclass;
  }

  /**
   * Marker interface to be implemented by all @Configuration CGLIB subclasses.
   * Facilitates idempotent behavior for {@link ConfigurationClassEnhancer#enhance}
   * through checking to see if candidate classes are already assignable to it, e.g.
   * have already been enhanced.
   * <p>Also extends {@link BeanFactoryAware}, as all enhanced {@code @Configuration}
   * classes require access to the {@link BeanFactory} that created them.
   * <p>Note that this interface is intended for framework-internal use only, however
   * must remain public in order to allow access to subclasses generated from other
   * packages (i.e. user code).
   */
  public interface EnhancedConfiguration extends BeanFactoryAware, StandardProxy { }

  /**
   * Conditional {@link Callback}.
   *
   * @see ConditionalCallbackFilter
   */
  private interface ConditionalCallback extends Callback {

    boolean isMatch(Method candidateMethod);
  }

  /**
   * A {@link CallbackFilter} that works by interrogating {@link Callback Callbacks} in the order
   * that they are defined via {@link ConditionalCallback}.
   */
  private static class ConditionalCallbackFilter implements CallbackFilter {

    private final Callback[] callbacks;

    private final Class<?>[] callbackTypes;

    public ConditionalCallbackFilter(Callback[] callbacks) {
      this.callbacks = callbacks;
      this.callbackTypes = new Class<?>[callbacks.length];
      for (int i = 0; i < callbacks.length; i++) {
        this.callbackTypes[i] = callbacks[i].getClass();
      }
    }

    @Override
    public int accept(Method method) {
      for (int i = 0; i < this.callbacks.length; i++) {
        Callback callback = this.callbacks[i];
        if (!(callback instanceof ConditionalCallback) || ((ConditionalCallback) callback).isMatch(method)) {
          return i;
        }
      }
      throw new IllegalStateException("No callback available for method " + method.getName());
    }

    public Class<?>[] getCallbackTypes() {
      return this.callbackTypes;
    }
  }

  /**
   * Custom extension of CGLIB's DefaultGeneratorStrategy, introducing a {@link BeanFactory} field.
   * Also exposes the application ClassLoader as thread context ClassLoader for the time of
   * class generation (in order for ASM to pick it up when doing common superclass resolution).
   */
  private static final class BeanFactoryAwareGeneratorStrategy extends ClassLoaderAwareGeneratorStrategy {

    public BeanFactoryAwareGeneratorStrategy(@Nullable ClassLoader classLoader) {
      super(classLoader);
    }

    @Override
    protected ClassGenerator transform(ClassGenerator cg) {
      ClassEmitter transformer = new ClassEmitter() {
        @Override
        public void endClass() {
          declare_field(Opcodes.ACC_PUBLIC, BEAN_FACTORY_FIELD, Type.forClass(BeanFactory.class), null);
          super.endClass();
        }
      };
      return new TransformingClassGenerator(cg, transformer);
    }

  }

  /**
   * Intercepts the invocation of any {@link BeanFactoryAware#setBeanFactory(BeanFactory)} on
   * {@code @Configuration} class instances for the purpose of recording the {@link BeanFactory}.
   *
   * @see EnhancedConfiguration
   */
  private static final class BeanFactoryAwareMethodInterceptor implements MethodInterceptor, ConditionalCallback {

    @Override
    @Nullable
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
      Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);
      Assert.state(field != null, "Unable to find generated BeanFactory field");
      field.set(obj, args[0]);

      // Does the actual (non-CGLIB) superclass implement BeanFactoryAware?
      // If so, call its setBeanFactory() method. If not, just exit.
      if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {
        return proxy.invokeSuper(obj, args);
      }
      return null;
    }

    @Override
    public boolean isMatch(Method candidateMethod) {
      return isSetBeanFactory(candidateMethod);
    }

    public static boolean isSetBeanFactory(Method candidateMethod) {
      return candidateMethod.getName().equals("setBeanFactory")
              && candidateMethod.getParameterCount() == 1
              && BeanFactory.class == candidateMethod.getParameterTypes()[0]
              && BeanFactoryAware.class.isAssignableFrom(candidateMethod.getDeclaringClass());
    }
  }

  /**
   * Intercepts the invocation of any {@link Component}-annotated methods in order to ensure proper
   * handling of bean semantics such as scoping and AOP proxying.
   *
   * @see Component
   * @see ConfigurationClassEnhancer
   */
  private static final class ComponentMethodInterceptor implements MethodInterceptor, ConditionalCallback {

    /**
     * Enhance a {@link Component @Component} method to check the supplied BeanFactory for the
     * existence of this bean object.
     *
     * @throws Throwable as a catch-all for any exception that may be thrown when invoking the
     * super implementation of the proxied method i.e., the actual {@code @Component} method
     */
    @Override
    @Nullable
    public Object intercept(Object enhancedConfigInstance, Method beanMethod,
            Object[] beanMethodArgs, MethodProxy cglibMethodProxy) throws Throwable {

      ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
      String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod, beanFactory);

      // Determine whether this bean is a scoped-proxy
      if (BeanAnnotationHelper.isScopedProxy(beanMethod)) {
        String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
        if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {
          beanName = scopedBeanName;
        }
      }

      // To handle the case of an inter-bean method reference, we must explicitly check the
      // container for already cached instances.

      // First, check to see if the requested bean is a FactoryBean. If so, create a subclass
      // proxy that intercepts calls to getObject() and returns any cached bean instance.
      // This ensures that the semantics of calling a FactoryBean from within @Component methods
      String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
      if (factoryContainsBean(beanFactory, factoryBeanName) && factoryContainsBean(beanFactory, beanName)) {
        Object factoryBean = beanFactory.getBean(factoryBeanName);
        if (factoryBean instanceof ScopedProxyFactoryBean) {
          // Scoped proxy factory beans are a special case and should not be further proxied
        }
        else if (factoryBean != null) {
          // It is a candidate FactoryBean - go ahead with enhancement
          return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);
        }
      }

      // fix circular bean creation
      if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
        // The factory is calling the bean method in order to instantiate and register the bean
        // (i.e. via a getBean() call) -> invoke the super implementation of the method to actually
        // create the bean instance.
        if (log.isInfoEnabled() &&
                BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
          log.info("@Component method {}.{} is non-static and returns an object " +
                          "assignable to Framework's BeanFactoryPostProcessor interface. This will " +
                          "result in a failure to process annotations such as @Autowired, " +
                          "@Resource and @PostConstruct within the method's declaring " +
                          "@Configuration class. Add the 'static' modifier to this method to avoid " +
                          "these container lifecycle issues; see @Component javadoc for complete details.",
                  beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName());
        }
        return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
      }

      return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);
    }

    @Nullable
    private Object resolveBeanReference(Method beanMethod, Object[] beanMethodArgs,
            ConfigurableBeanFactory beanFactory, String beanName) {

      // The user (i.e. not the factory) is requesting this bean through a call to
      // the bean method, direct or indirect. The bean may have already been marked
      // as 'in creation' in certain autowiring scenarios; if so, temporarily set
      // the in-creation status to false in order to avoid an exception.
      boolean alreadyInCreation = beanFactory.isCurrentlyInCreation(beanName);
      try {
        if (alreadyInCreation) {
          beanFactory.setCurrentlyInCreation(beanName, false);
        }
        boolean useArgs = ObjectUtils.isNotEmpty(beanMethodArgs);
        if (useArgs && beanFactory.isSingleton(beanName)) {
          // Stubbed null arguments just for reference purposes,
          // expecting them to be autowired for regular singleton references?
          // A safe assumption since @Component singleton arguments cannot be optional...
          for (Object arg : beanMethodArgs) {
            if (arg == null) {
              useArgs = false;
              break;
            }
          }
        }
        Object beanInstance = useArgs ? beanFactory.getBean(beanName, beanMethodArgs)
                : beanFactory.getBean(beanName);
        if (!ClassUtils.isAssignableValue(beanMethod.getReturnType(), beanInstance)) {
          if (beanInstance == null) { // maybe null from factory-method
            if (log.isDebugEnabled()) {
              log.debug(String.format("@Component method %s.%s called as bean reference " +
                              "for type [%s] returned null bean; resolving to null value.",
                      beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
                      beanMethod.getReturnType().getName()));
            }
          }
          else {
            String msg = String.format("@Component method %s.%s called as bean reference " +
                            "for type [%s] but overridden by non-compatible bean instance of type [%s].",
                    beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
                    beanMethod.getReturnType().getName(), beanInstance.getClass().getName());
            try {
              BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
              msg += " Overriding bean of same name declared in: " + beanDefinition.getResourceDescription();
            }
            catch (NoSuchBeanDefinitionException ex) {
              // Ignore - simply no detailed message then.
            }
            throw new IllegalStateException(msg);
          }
        }
        Method currentlyInvoked = InstantiationStrategy.getCurrentlyInvokedFactoryMethod();
        if (currentlyInvoked != null) {
          String outerBeanName = BeanAnnotationHelper.determineBeanNameFor(currentlyInvoked, beanFactory);
          beanFactory.registerDependentBean(beanName, outerBeanName);
        }
        return beanInstance;
      }
      finally {
        if (alreadyInCreation) {
          beanFactory.setCurrentlyInCreation(beanName, true);
        }
      }
    }

    @Override
    public boolean isMatch(Method candidateMethod) {
      return candidateMethod.getDeclaringClass() != Object.class
              && !BeanFactoryAwareMethodInterceptor.isSetBeanFactory(candidateMethod)
              && BeanAnnotationHelper.isBeanAnnotated(candidateMethod);
    }

    private ConfigurableBeanFactory getBeanFactory(Object enhancedConfigInstance) {
      Field field = ReflectionUtils.findField(enhancedConfigInstance.getClass(), BEAN_FACTORY_FIELD);
      Assert.state(field != null, "Unable to find generated bean factory field");
      Object beanFactory = ReflectionUtils.getField(field, enhancedConfigInstance);
      Assert.state(beanFactory != null, "BeanFactory has not been injected into @Configuration class");
      Assert.state(beanFactory instanceof ConfigurableBeanFactory,
              "Injected BeanFactory is not a ConfigurableBeanFactory");
      return (ConfigurableBeanFactory) beanFactory;
    }

    /**
     * Check the BeanFactory to see whether the bean named <var>beanName</var> already
     * exists. Accounts for the fact that the requested bean may be "in creation", i.e.:
     * we're in the middle of servicing the initial request for this bean. From an enhanced
     * factory method's perspective, this means that the bean does not actually yet exist,
     * and that it is now our job to create it for the first time by executing the logic
     * in the corresponding factory method.
     * <p>Said another way, this check repurposes
     * {@link ConfigurableBeanFactory#isCurrentlyInCreation(String)} to determine whether
     * the container is calling this method or the user is calling this method.
     *
     * @param beanName name of bean to check for
     * @return whether <var>beanName</var> already exists in the factory
     */
    private boolean factoryContainsBean(ConfigurableBeanFactory beanFactory, String beanName) {
      return beanFactory.containsBean(beanName) && !beanFactory.isCurrentlyInCreation(beanName);
    }

    /**
     * Check whether the given method corresponds to the container's currently invoked
     * factory method. Compares method name and parameter types only in order to work
     * around a potential problem with covariant return types (currently only known
     * to happen on Groovy classes).
     */
    private boolean isCurrentlyInvokedFactoryMethod(Method method) {
      Method currentlyInvoked = InstantiationStrategy.getCurrentlyInvokedFactoryMethod();
      return currentlyInvoked != null && method.getName().equals(currentlyInvoked.getName())
              && Arrays.equals(method.getParameterTypes(), currentlyInvoked.getParameterTypes());
    }

    /**
     * Create a subclass proxy that intercepts calls to getObject(), delegating to the current BeanFactory
     * instead of creating a new instance. These proxies are created only when calling a FactoryBean from
     * within a Bean method, allowing for proper scoping semantics even when working against the FactoryBean
     * instance directly. If a FactoryBean instance is fetched through the container via &-dereferencing,
     * it will not be proxied. This too is aligned with the way XML configuration works.
     */
    private Object enhanceFactoryBean(Object factoryBean, Class<?> exposedType,
            ConfigurableBeanFactory beanFactory, String beanName) {

      try {
        Class<?> clazz = factoryBean.getClass();
        boolean finalClass = Modifier.isFinal(clazz.getModifiers());
        boolean finalMethod = Modifier.isFinal(clazz.getMethod("getObject").getModifiers());
        if (finalClass || finalMethod) {
          if (exposedType.isInterface()) {
            if (log.isTraceEnabled()) {
              log.trace("Creating interface proxy for FactoryBean '{}' of type [{}] for use within another @Component " +
                              "method because its {} is final: Otherwise a getObject() call would not be routed to the factory.",
                      beanName, clazz.getName(), (finalClass ? "implementation class" : "getObject() method"));
            }
            return createInterfaceProxyForFactoryBean(factoryBean, exposedType, beanFactory, beanName);
          }
          else {
            if (log.isDebugEnabled()) {
              log.debug("Unable to proxy FactoryBean '{}' of type [{}] for use within another @Component method " +
                              "because its {} is final: A getObject() call will NOT be routed to the factory. " +
                              "Consider declaring the return type as a FactoryBean interface.",
                      beanName, clazz.getName(), finalClass ? "implementation class" : "getObject() method");
            }
            return factoryBean;
          }
        }
      }
      catch (NoSuchMethodException ex) {
        // No getObject() method -> shouldn't happen, but as long as nobody is trying to call it...
      }

      return createCglibProxyForFactoryBean(factoryBean, beanFactory, beanName);
    }

    private Object createInterfaceProxyForFactoryBean(
            Object factoryBean, Class<?> interfaceType,
            ConfigurableBeanFactory beanFactory, String beanName) {

      return Proxy.newProxyInstance(
              factoryBean.getClass().getClassLoader(), new Class<?>[] { interfaceType },
              (proxy, method, args) -> {
                if (method.getName().equals("getObject") && args == null) {
                  return beanFactory.getBean(beanName);
                }
                return ReflectionUtils.invokeMethod(method, factoryBean, args);
              });
    }

    private Object createCglibProxyForFactoryBean(
            Object factoryBean, ConfigurableBeanFactory beanFactory, String beanName) {

      Enhancer enhancer = new Enhancer();
      enhancer.setAttemptLoad(true);
      enhancer.setSuperclass(factoryBean.getClass());
      enhancer.setCallbackType(MethodInterceptor.class);
      enhancer.setNamingPolicy(NamingPolicy.forInfrastructure());

      // Ideally create enhanced FactoryBean proxy without constructor side effects,
      // analogous to AOP proxy creation in ObjenesisCglibAopProxy...
      Class<?> fbClass = enhancer.createClass();
      Object fbProxy;
      try {
        fbProxy = ReflectionUtils.accessibleConstructor(fbClass).newInstance();
      }
      catch (Throwable ex) {
        try {
          // fallback using BeanInstantiator.forSerialization
          fbProxy = BeanInstantiator.forSerialization(fbClass).instantiate();
        }
        catch (BeanInstantiationException ignored) {
          throw new IllegalStateException("Unable to instantiate enhanced FactoryBean using constructor, " +
                  "and regular FactoryBean instantiation via default constructor fails as well", ex);
        }
      }

      ((Factory) fbProxy).setCallback(0, (MethodInterceptor) (obj, method, args, proxy) -> {
        if (method.getName().equals("getObject") && args.length == 0) {
          return beanFactory.getBean(beanName);
        }
        return proxy.invoke(factoryBean, args);
      });

      return fbProxy;
    }
  }

}
