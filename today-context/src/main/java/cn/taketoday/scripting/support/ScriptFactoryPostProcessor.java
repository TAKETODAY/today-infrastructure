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
package cn.taketoday.scripting.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.DelegatingIntroductionInterceptor;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.bytecode.proxy.InterfaceMaker;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.scripting.ScriptFactory;
import cn.taketoday.scripting.ScriptSource;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link BeanPostProcessor} that
 * handles {@link ScriptFactory} definitions,
 * replacing each factory with the actual scripted Java object generated by it.
 *
 * <p>This is similar to the
 * {@link cn.taketoday.beans.factory.FactoryBean} mechanism, but is
 * specifically tailored for scripts and not built into Framework's core
 * container itself but rather implemented as an extension.
 *
 * <p><b>NOTE:</b> The most important characteristic of this post-processor
 * is that constructor arguments are applied to the
 * {@link ScriptFactory} instance
 * while bean property values are applied to the generated scripted object.
 * Typically, constructor arguments include a script source locator and
 * potentially script interfaces, while bean property values include
 * references and config values to inject into the scripted object itself.
 *
 * <p>The following {@link ScriptFactoryPostProcessor} will automatically
 * be applied to the two
 * {@link ScriptFactory} definitions below.
 * At runtime, the actual scripted objects will be exposed for
 * "bshMessenger" and "groovyMessenger", rather than the
 * {@link ScriptFactory} instances. Both of
 * those are supposed to be castable to the example's {@code Messenger}
 * interfaces here.
 *
 * <pre class="code">&lt;bean class="cn.taketoday.scripting.support.ScriptFactoryPostProcessor"/&gt;
 *
 * &lt;bean id="bshMessenger" class="cn.taketoday.scripting.bsh.BshScriptFactory"&gt;
 *   &lt;constructor-arg value="classpath:mypackage/Messenger.bsh"/&gt;
 *   &lt;constructor-arg value="mypackage.Messenger"/&gt;
 *   &lt;property name="message" value="Hello World!"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="groovyMessenger" class="cn.taketoday.scripting.groovy.GroovyScriptFactory"&gt;
 *   &lt;constructor-arg value="classpath:mypackage/Messenger.groovy"/&gt;
 *   &lt;property name="message" value="Hello World!"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p><b>NOTE:</b> Please note that the above excerpt from a Framework
 * XML bean definition file uses just the &lt;bean/&gt;-style syntax
 * (in an effort to illustrate using the {@link ScriptFactoryPostProcessor} itself).
 * In reality, you would never create a &lt;bean/&gt; definition for a
 * {@link ScriptFactoryPostProcessor} explicitly; rather you would import the
 * tags from the {@code 'lang'} namespace and simply create scripted
 * beans using the tags in that namespace... as part of doing so, a
 * {@link ScriptFactoryPostProcessor} will implicitly be created for you.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 4.0
 */
public class ScriptFactoryPostProcessor
        implements SmartInstantiationAwareBeanPostProcessor,
        BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware, DisposableBean, Ordered {

  /**
   * The {@link cn.taketoday.core.io.Resource}-style prefix that denotes
   * an inline script.
   * <p>An inline script is a script that is defined right there in the (typically XML)
   * configuration, as opposed to being defined in an external file.
   */
  public static final String INLINE_SCRIPT_PREFIX = "inline:";

  /**
   * The {@code refreshCheckDelay} attribute.
   */
  public static final String REFRESH_CHECK_DELAY_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          ScriptFactoryPostProcessor.class, "refreshCheckDelay");

  /**
   * The {@code proxyTargetClass} attribute.
   */
  public static final String PROXY_TARGET_CLASS_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          ScriptFactoryPostProcessor.class, "proxyTargetClass");

  /**
   * The {@code language} attribute.
   */
  public static final String LANGUAGE_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          ScriptFactoryPostProcessor.class, "language");

  private static final String SCRIPT_FACTORY_NAME_PREFIX = "scriptFactory.";

  private static final String SCRIPTED_OBJECT_NAME_PREFIX = "scriptedObject.";

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private long defaultRefreshCheckDelay = -1;

  private boolean defaultProxyTargetClass = false;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  final StandardBeanFactory scriptBeanFactory = new StandardBeanFactory();

  /** Map from bean name String to ScriptSource object. */
  private final Map<String, ScriptSource> scriptSourceCache = new ConcurrentHashMap<>();

  /**
   * Set the delay between refresh checks, in milliseconds.
   * Default is -1, indicating no refresh checks at all.
   * <p>Note that an actual refresh will only happen when
   * the {@link ScriptSource} indicates
   * that it has been modified.
   *
   * @see ScriptSource#isModified()
   */
  public void setDefaultRefreshCheckDelay(long defaultRefreshCheckDelay) {
    this.defaultRefreshCheckDelay = defaultRefreshCheckDelay;
  }

  /**
   * Flag to signal that refreshable proxies should be created to proxy the target class not its interfaces.
   *
   * @param defaultProxyTargetClass the flag value to set
   */
  public void setDefaultProxyTargetClass(boolean defaultProxyTargetClass) {
    this.defaultProxyTargetClass = defaultProxyTargetClass;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableBeanFactory)) {
      throw new IllegalStateException("ScriptFactoryPostProcessor doesn't work with " +
              "non-ConfigurableBeanFactory: " + beanFactory.getClass());
    }
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;

    // Required so that references (up container hierarchies) are correctly resolved.
    this.scriptBeanFactory.setParentBeanFactory(this.beanFactory);

    // Required so that all BeanPostProcessors, Scopes, etc become available.
    this.scriptBeanFactory.copyConfigurationFrom(this.beanFactory);

    // Filter out BeanPostProcessors that are part of the AOP infrastructure,
    // since those are only meant to apply to beans defined in the original factory.
    this.scriptBeanFactory.getBeanPostProcessors()
            .removeIf(beanPostProcessor -> beanPostProcessor instanceof AopInfrastructureBean);
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public int getOrder() {
    return Integer.MIN_VALUE;
  }

  @Override
  @Nullable
  public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
    // We only apply special treatment to ScriptFactory implementations here.
    if (!ScriptFactory.class.isAssignableFrom(beanClass)) {
      return null;
    }

    Assert.state(this.beanFactory != null, "No BeanFactory set");
    BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);

    try {
      String scriptFactoryBeanName = SCRIPT_FACTORY_NAME_PREFIX + beanName;
      String scriptedObjectBeanName = SCRIPTED_OBJECT_NAME_PREFIX + beanName;
      prepareScriptBeans(bd, scriptFactoryBeanName, scriptedObjectBeanName);

      ScriptFactory scriptFactory = scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
      ScriptSource scriptSource = getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
      Class<?>[] interfaces = scriptFactory.getScriptInterfaces();

      Class<?> scriptedType = scriptFactory.getScriptedObjectType(scriptSource);
      if (scriptedType != null) {
        return scriptedType;
      }
      else if (ObjectUtils.isNotEmpty(interfaces)) {
        return (interfaces.length == 1 ? interfaces[0] : createCompositeInterface(interfaces));
      }
      else {
        if (bd.isSingleton()) {
          return scriptBeanFactory.getBean(scriptedObjectBeanName).getClass();
        }
      }
    }
    catch (Exception ex) {
      if (ex instanceof BeanCreationException
              && ((BeanCreationException) ex).getMostSpecificCause() instanceof BeanCurrentlyInCreationException) {
        if (logger.isTraceEnabled()) {
          logger.trace("Could not determine scripted object type for bean '{}': {}", beanName, ex.getMessage());
        }
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not determine scripted object type for bean '{}'", beanName, ex);
        }
      }
    }

    return null;
  }

  @Override
  public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
    // We only apply special treatment to ScriptFactory implementations here.
    if (!ScriptFactory.class.isAssignableFrom(beanClass)) {
      return null;
    }
    Assert.state(this.beanFactory != null, "No BeanFactory set");
    BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);

    String scriptFactoryBeanName = SCRIPT_FACTORY_NAME_PREFIX + beanName;
    String scriptedObjectBeanName = SCRIPTED_OBJECT_NAME_PREFIX + beanName;
    prepareScriptBeans(bd, scriptFactoryBeanName, scriptedObjectBeanName);

    ScriptFactory scriptFactory = scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
    ScriptSource scriptSource = getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
    boolean isFactoryBean = false;
    try {
      Class<?> scriptedObjectType = scriptFactory.getScriptedObjectType(scriptSource);
      // Returned type may be null if the factory is unable to determine the type.
      if (scriptedObjectType != null) {
        isFactoryBean = FactoryBean.class.isAssignableFrom(scriptedObjectType);
      }
    }
    catch (Exception ex) {
      throw new BeanCreationException(beanName,
              "Could not determine scripted object type for " + scriptFactory, ex);
    }

    long refreshCheckDelay = resolveRefreshCheckDelay(bd);
    if (refreshCheckDelay >= 0) {
      Class<?>[] interfaces = scriptFactory.getScriptInterfaces();
      RefreshableScriptTargetSource ts = new RefreshableScriptTargetSource(this.scriptBeanFactory,
              scriptedObjectBeanName, scriptFactory, scriptSource, isFactoryBean);
      boolean proxyTargetClass = resolveProxyTargetClass(bd);
      String language = (String) bd.getAttribute(LANGUAGE_ATTRIBUTE);
      if (proxyTargetClass && (language == null || !language.equals("groovy"))) {
        throw new BeanDefinitionValidationException(
                "Cannot use proxyTargetClass=true with script beans where language is not 'groovy': '" +
                        language + "'");
      }
      ts.setRefreshCheckDelay(refreshCheckDelay);
      return createRefreshableProxy(ts, interfaces, proxyTargetClass);
    }

    if (isFactoryBean) {
      scriptedObjectBeanName = BeanFactory.FACTORY_BEAN_PREFIX + scriptedObjectBeanName;
    }
    return this.scriptBeanFactory.getBean(scriptedObjectBeanName);
  }

  /**
   * Prepare the script beans in the internal BeanFactory that this
   * post-processor uses. Each original bean definition will be split
   * into a ScriptFactory definition and a scripted object definition.
   *
   * @param bd the original bean definition in the main BeanFactory
   * @param scriptFactoryBeanName the name of the internal ScriptFactory bean
   * @param scriptedObjectBeanName the name of the internal scripted object bean
   */
  protected void prepareScriptBeans(
          BeanDefinition bd, String scriptFactoryBeanName, String scriptedObjectBeanName) {
    // Avoid recreation of the script bean definition in case of a prototype.
    synchronized(scriptBeanFactory) {
      if (!scriptBeanFactory.containsBeanDefinition(scriptedObjectBeanName)) {

        scriptBeanFactory.registerBeanDefinition(
                scriptFactoryBeanName, createScriptFactoryBeanDefinition(bd));

        ScriptFactory scriptFactory = scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);

        ScriptSource scriptSource =
                getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
        Class<?>[] interfaces = scriptFactory.getScriptInterfaces();

        Class<?>[] scriptedInterfaces = interfaces;
        if (scriptFactory.requiresConfigInterface() && bd.hasPropertyValues()) {
          Class<?> configInterface = createConfigInterface(bd, interfaces);
          scriptedInterfaces = ObjectUtils.addObjectToArray(interfaces, configInterface);
        }

        BeanDefinition objectBd = createScriptedObjectBeanDefinition(
                bd, scriptFactoryBeanName, scriptSource, scriptedInterfaces);
        long refreshCheckDelay = resolveRefreshCheckDelay(bd);
        if (refreshCheckDelay >= 0) {
          objectBd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        }

        this.scriptBeanFactory.registerBeanDefinition(scriptedObjectBeanName, objectBd);
      }
    }
  }

  /**
   * Get the refresh check delay for the given {@link ScriptFactory} {@link BeanDefinition}.
   * If the {@link BeanDefinition} has a
   * {@link cn.taketoday.core.AttributeAccessor metadata attribute}
   * under the key {@link #REFRESH_CHECK_DELAY_ATTRIBUTE} which is a valid {@link Number}
   * type, then this value is used. Otherwise, the {@link #defaultRefreshCheckDelay}
   * value is used.
   *
   * @param beanDefinition the BeanDefinition to check
   * @return the refresh check delay
   */
  protected long resolveRefreshCheckDelay(BeanDefinition beanDefinition) {
    long refreshCheckDelay = this.defaultRefreshCheckDelay;
    Object attributeValue = beanDefinition.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
    if (attributeValue instanceof Number) {
      refreshCheckDelay = ((Number) attributeValue).longValue();
    }
    else if (attributeValue instanceof String) {
      refreshCheckDelay = Long.parseLong((String) attributeValue);
    }
    else if (attributeValue != null) {
      throw new BeanDefinitionStoreException("Invalid refresh check delay attribute [" +
              REFRESH_CHECK_DELAY_ATTRIBUTE + "] with value '" + attributeValue +
              "': needs to be of type Number or String");
    }
    return refreshCheckDelay;
  }

  protected boolean resolveProxyTargetClass(BeanDefinition beanDefinition) {
    boolean proxyTargetClass = this.defaultProxyTargetClass;
    Object attributeValue = beanDefinition.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE);
    if (attributeValue instanceof Boolean) {
      proxyTargetClass = (Boolean) attributeValue;
    }
    else if (attributeValue instanceof String) {
      proxyTargetClass = Boolean.parseBoolean((String) attributeValue);
    }
    else if (attributeValue != null) {
      throw new BeanDefinitionStoreException("Invalid proxy target class attribute [" +
              PROXY_TARGET_CLASS_ATTRIBUTE + "] with value '" + attributeValue +
              "': needs to be of type Boolean or String");
    }
    return proxyTargetClass;
  }

  /**
   * Create a ScriptFactory bean definition based on the given script definition,
   * extracting only the definition data that is relevant for the ScriptFactory
   * (that is, only bean class and constructor arguments).
   *
   * @param bd the full script bean definition
   * @return the extracted ScriptFactory bean definition
   * @see ScriptFactory
   */
  protected BeanDefinition createScriptFactoryBeanDefinition(BeanDefinition bd) {
    GenericBeanDefinition scriptBd = new GenericBeanDefinition();
    scriptBd.setBeanClassName(bd.getBeanClassName());
    scriptBd.getConstructorArgumentValues().addArgumentValues(bd.getConstructorArgumentValues());
    return scriptBd;
  }

  /**
   * Obtain a ScriptSource for the given bean, lazily creating it
   * if not cached already.
   *
   * @param beanName the name of the scripted bean
   * @param scriptSourceLocator the script source locator associated with the bean
   * @return the corresponding ScriptSource instance
   * @see #convertToScriptSource
   */
  protected ScriptSource getScriptSource(String beanName, String scriptSourceLocator) {
    return this.scriptSourceCache.computeIfAbsent(beanName, key ->
            convertToScriptSource(beanName, scriptSourceLocator, this.resourceLoader));
  }

  /**
   * Convert the given script source locator to a ScriptSource instance.
   * <p>By default, supported locators are Framework resource locations
   * (such as "file:C:/myScript.bsh" or "classpath:myPackage/myScript.bsh")
   * and inline scripts ("inline:myScriptText...").
   *
   * @param beanName the name of the scripted bean
   * @param scriptSourceLocator the script source locator
   * @param resourceLoader the ResourceLoader to use (if necessary)
   * @return the ScriptSource instance
   */
  protected ScriptSource convertToScriptSource(
          String beanName, String scriptSourceLocator, ResourceLoader resourceLoader) {

    if (scriptSourceLocator.startsWith(INLINE_SCRIPT_PREFIX)) {
      return new StaticScriptSource(scriptSourceLocator.substring(INLINE_SCRIPT_PREFIX.length()), beanName);
    }
    else {
      return new ResourceScriptSource(resourceLoader.getResource(scriptSourceLocator));
    }
  }

  /**
   * Create a config interface for the given bean definition, defining setter
   * methods for the defined property values as well as an init method and
   * a destroy method (if defined).
   * <p>This implementation creates the interface via CGLIB's InterfaceMaker,
   * determining the property types from the given interfaces (as far as possible).
   *
   * @param bd the bean definition (property values etc) to create a
   * config interface for
   * @param interfaces the interfaces to check against (might define
   * getters corresponding to the setters we're supposed to generate)
   * @return the config interface
   * @see InterfaceMaker
   */
  protected Class<?> createConfigInterface(BeanDefinition bd, @Nullable Class<?>[] interfaces) {
    InterfaceMaker maker = new InterfaceMaker();
    PropertyValues propertyValues = bd.getPropertyValues();
    if (propertyValues != null) {
      for (PropertyValue pv : propertyValues) {
        String propertyName = pv.getName();
        Class<?> propertyType = BeanUtils.findPropertyType(propertyName, interfaces);
        String setterName = "set" + StringUtils.capitalize(propertyName);
        MethodSignature signature = new MethodSignature(
                Type.VOID_TYPE, setterName, Type.forClass(propertyType));
        maker.add(signature, Type.EMPTY_ARRAY);
      }
    }

    if (bd instanceof AbstractBeanDefinition abd) {
      if (ObjectUtils.isNotEmpty(abd.getInitMethodNames())) {
        for (String initMethodName : abd.getInitMethodNames()) {
          MethodSignature signature = new MethodSignature(Type.VOID_TYPE, initMethodName);
          maker.add(signature, Type.EMPTY_ARRAY);
        }
      }
      if (ObjectUtils.isNotEmpty(abd.getDestroyMethodNames())) {
        for (String destroyMethodName : abd.getDestroyMethodNames()) {
          MethodSignature signature = new MethodSignature(Type.VOID_TYPE, destroyMethodName);
          maker.add(signature, Type.EMPTY_ARRAY);
        }
      }
    }
    else {
      if (StringUtils.hasText(bd.getDestroyMethodName())) {
        MethodSignature signature = new MethodSignature(Type.VOID_TYPE, bd.getDestroyMethodName());
        maker.add(signature, Type.EMPTY_ARRAY);
      }

      if (StringUtils.hasText(bd.getDestroyMethodName())) {
        MethodSignature signature = new MethodSignature(Type.VOID_TYPE, bd.getDestroyMethodName());
        maker.add(signature, Type.EMPTY_ARRAY);
      }
    }

    return maker.create();
  }

  /**
   * Create a composite interface Class for the given interfaces,
   * implementing the given interfaces in one single Class.
   * <p>The default implementation builds a JDK proxy class
   * for the given interfaces.
   *
   * @param interfaces the interfaces to merge
   * @return the merged interface as Class
   * @see java.lang.reflect.Proxy#getProxyClass
   */
  protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
    return ClassUtils.createCompositeInterface(interfaces, this.beanClassLoader);
  }

  /**
   * Create a bean definition for the scripted object, based on the given script
   * definition, extracting the definition data that is relevant for the scripted
   * object (that is, everything but bean class and constructor arguments).
   *
   * @param bd the full script bean definition
   * @param scriptFactoryBeanName the name of the internal ScriptFactory bean
   * @param scriptSource the ScriptSource for the scripted bean
   * @param interfaces the interfaces that the scripted bean is supposed to implement
   * @return the extracted ScriptFactory bean definition
   * @see ScriptFactory#getScriptedObject
   */
  protected BeanDefinition createScriptedObjectBeanDefinition(
          BeanDefinition bd, String scriptFactoryBeanName,
          ScriptSource scriptSource, @Nullable Class<?>[] interfaces) {

    BeanDefinition objectBd = bd.cloneBeanDefinition();
    objectBd.setFactoryBeanName(scriptFactoryBeanName);
    objectBd.setFactoryMethodName("getScriptedObject");
    ConstructorArgumentValues argumentValues = objectBd.getConstructorArgumentValues();
    argumentValues.clear();
    argumentValues.addIndexedArgumentValue(0, scriptSource);
    argumentValues.addIndexedArgumentValue(1, interfaces);
    return objectBd;
  }

  /**
   * Create a refreshable proxy for the given AOP TargetSource.
   *
   * @param ts the refreshable TargetSource
   * @param interfaces the proxy interfaces (may be {@code null} to
   * indicate proxying of all interfaces implemented by the target class)
   * @return the generated proxy
   * @see RefreshableScriptTargetSource
   */
  protected Object createRefreshableProxy(TargetSource ts, @Nullable Class<?>[] interfaces, boolean proxyTargetClass) {
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setTargetSource(ts);
    ClassLoader classLoader = this.beanClassLoader;

    if (interfaces != null) {
      proxyFactory.setInterfaces(interfaces);
    }
    else {
      Class<?> targetClass = ts.getTargetClass();
      if (targetClass != null) {
        proxyFactory.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.beanClassLoader));
      }
    }

    if (proxyTargetClass) {
      classLoader = null;  // force use of Class.getClassLoader()
      proxyFactory.setProxyTargetClass(true);
    }

    DelegatingIntroductionInterceptor introduction = new DelegatingIntroductionInterceptor(ts);
    introduction.suppressInterface(TargetSource.class);
    proxyFactory.addAdvice(introduction);

    return proxyFactory.getProxy(classLoader);
  }

  /**
   * Destroy the inner bean factory (used for scripts) on shutdown.
   */
  @Override
  public void destroy() {
    this.scriptBeanFactory.destroySingletons();
  }

}
