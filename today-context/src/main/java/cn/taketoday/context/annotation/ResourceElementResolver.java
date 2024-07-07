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

package cn.taketoday.context.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Resolver for the injection of named beans on a field or method element,
 * following the rules of the {@link jakarta.annotation.Resource} annotation
 * but without any JNDI support. This is primarily intended for AOT processing.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CommonAnnotationBeanPostProcessor
 * @see jakarta.annotation.Resource
 * @since 5.0
 */
public abstract class ResourceElementResolver {

  private final String name;

  private final boolean defaultName;

  ResourceElementResolver(String name, boolean defaultName) {
    this.name = name;
    this.defaultName = defaultName;
  }

  /**
   * Create a new {@link ResourceFieldResolver} for the specified field.
   *
   * @param fieldName the field name
   * @return a new {@link ResourceFieldResolver} instance
   */
  public static ResourceElementResolver forField(String fieldName) {
    return new ResourceFieldResolver(fieldName, true, fieldName);
  }

  /**
   * Create a new {@link ResourceFieldResolver} for the specified field and resource name.
   *
   * @param fieldName the field name
   * @param resourceName the resource name
   * @return a new {@link ResourceFieldResolver} instance
   */
  public static ResourceElementResolver forField(String fieldName, String resourceName) {
    return new ResourceFieldResolver(resourceName, false, fieldName);
  }

  /**
   * Create a new {@link ResourceMethodResolver} for the specified method
   * using a resource name that infers from the method name.
   *
   * @param methodName the method name
   * @param parameterType the parameter type.
   * @return a new {@link ResourceMethodResolver} instance
   */
  public static ResourceElementResolver forMethod(String methodName, Class<?> parameterType) {
    return new ResourceMethodResolver(defaultResourceNameForMethod(methodName), true,
            methodName, parameterType);
  }

  /**
   * Create a new {@link ResourceMethodResolver} for the specified method
   * and resource name.
   *
   * @param methodName the method name
   * @param parameterType the parameter type
   * @param resourceName the resource name
   * @return a new {@link ResourceMethodResolver} instance
   */
  public static ResourceElementResolver forMethod(String methodName, Class<?> parameterType, String resourceName) {
    return new ResourceMethodResolver(resourceName, false, methodName, parameterType);
  }

  private static String defaultResourceNameForMethod(String methodName) {
    if (methodName.startsWith("set") && methodName.length() > 3) {
      return StringUtils.uncapitalizeAsProperty(methodName.substring(3));
    }
    return methodName;
  }

  /**
   * Resolve the value for the specified registered bean.
   *
   * @param registeredBean the registered bean
   * @return the resolved field or method parameter value
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T resolve(RegisteredBean registeredBean) {
    Assert.notNull(registeredBean, "'registeredBean' must not be null");
    return (T) (isLazyLookup(registeredBean) ? buildLazyResourceProxy(registeredBean) :
            resolveValue(registeredBean));
  }

  /**
   * Resolve the value for the specified registered bean and set it using reflection.
   *
   * @param registeredBean the registered bean
   * @param instance the bean instance
   */
  public abstract void resolveAndSet(RegisteredBean registeredBean, Object instance);

  /**
   * Create a suitable {@link DependencyDescriptor} for the specified bean.
   *
   * @param registeredBean the registered bean
   * @return a descriptor for that bean
   */
  abstract DependencyDescriptor createDependencyDescriptor(RegisteredBean registeredBean);

  abstract Class<?> getLookupType(RegisteredBean registeredBean);

  abstract AnnotatedElement getAnnotatedElement(RegisteredBean registeredBean);

  boolean isLazyLookup(RegisteredBean registeredBean) {
    AnnotatedElement ae = getAnnotatedElement(registeredBean);
    Lazy lazy = ae.getAnnotation(Lazy.class);
    return (lazy != null && lazy.value());
  }

  private Object buildLazyResourceProxy(RegisteredBean registeredBean) {
    Class<?> lookupType = getLookupType(registeredBean);

    TargetSource ts = new TargetSource() {
      @Override
      public Class<?> getTargetClass() {
        return lookupType;
      }

      @Override
      public Object getTarget() {
        return resolveValue(registeredBean);
      }
    };

    ProxyFactory pf = new ProxyFactory();
    pf.setTargetSource(ts);
    if (lookupType.isInterface()) {
      pf.addInterface(lookupType);
    }
    return pf.getProxy(registeredBean.getBeanFactory().getBeanClassLoader());
  }

  /**
   * Resolve the value to inject for this instance.
   *
   * @param registeredBean the bean registration
   * @return the value to inject
   */
  private Object resolveValue(RegisteredBean registeredBean) {
    ConfigurableBeanFactory factory = registeredBean.getBeanFactory();

    Object resource;
    Set<String> autowiredBeanNames;
    DependencyDescriptor descriptor = createDependencyDescriptor(registeredBean);
    if (this.defaultName && !factory.containsBean(this.name)) {
      autowiredBeanNames = new LinkedHashSet<>();
      resource = factory.resolveDependency(descriptor, registeredBean.getBeanName(), autowiredBeanNames, null);
      if (resource == null) {
        throw new NoSuchBeanDefinitionException(descriptor.getDependencyType(), "No resolvable resource object");
      }
    }
    else {
      resource = factory.resolveBeanByName(this.name, descriptor);
      autowiredBeanNames = Collections.singleton(this.name);
    }

    for (String autowiredBeanName : autowiredBeanNames) {
      if (factory.containsBean(autowiredBeanName)) {
        factory.registerDependentBean(autowiredBeanName, registeredBean.getBeanName());
      }
    }
    return resource;
  }

  private static final class ResourceFieldResolver extends ResourceElementResolver {

    private final String fieldName;

    public ResourceFieldResolver(String name, boolean defaultName, String fieldName) {
      super(name, defaultName);
      this.fieldName = fieldName;
    }

    @Override
    public void resolveAndSet(RegisteredBean registeredBean, Object instance) {
      Assert.notNull(registeredBean, "'registeredBean' must not be null");
      Assert.notNull(instance, "'instance' must not be null");
      Field field = getField(registeredBean);
      Object resolved = resolve(registeredBean);
      ReflectionUtils.makeAccessible(field);
      ReflectionUtils.setField(field, instance, resolved);
    }

    @Override
    protected DependencyDescriptor createDependencyDescriptor(RegisteredBean registeredBean) {
      Field field = getField(registeredBean);
      return new LookupDependencyDescriptor(field, field.getType(), isLazyLookup(registeredBean));
    }

    @Override
    protected Class<?> getLookupType(RegisteredBean registeredBean) {
      return getField(registeredBean).getType();
    }

    @Override
    protected AnnotatedElement getAnnotatedElement(RegisteredBean registeredBean) {
      return getField(registeredBean);
    }

    private Field getField(RegisteredBean registeredBean) {
      Field field = ReflectionUtils.findField(registeredBean.getBeanClass(), this.fieldName);
      Assert.notNull(field,
              () -> "No field '" + this.fieldName + "' found on " + registeredBean.getBeanClass().getName());
      return field;
    }
  }

  private static final class ResourceMethodResolver extends ResourceElementResolver {

    private final String methodName;

    private final Class<?> lookupType;

    private ResourceMethodResolver(String name, boolean defaultName, String methodName, Class<?> lookupType) {
      super(name, defaultName);
      this.methodName = methodName;
      this.lookupType = lookupType;
    }

    @Override
    public void resolveAndSet(RegisteredBean registeredBean, Object instance) {
      Assert.notNull(registeredBean, "'registeredBean' must not be null");
      Assert.notNull(instance, "'instance' must not be null");
      Method method = getMethod(registeredBean);
      Object resolved = resolve(registeredBean);
      ReflectionUtils.makeAccessible(method);
      ReflectionUtils.invokeMethod(method, instance, resolved);
    }

    @Override
    protected DependencyDescriptor createDependencyDescriptor(RegisteredBean registeredBean) {
      return new LookupDependencyDescriptor(
              getMethod(registeredBean), this.lookupType, isLazyLookup(registeredBean));
    }

    @Override
    protected Class<?> getLookupType(RegisteredBean bean) {
      return this.lookupType;
    }

    @Override
    protected AnnotatedElement getAnnotatedElement(RegisteredBean registeredBean) {
      return getMethod(registeredBean);
    }

    private Method getMethod(RegisteredBean registeredBean) {
      Method method = ReflectionUtils.findMethod(registeredBean.getBeanClass(), this.methodName, this.lookupType);
      Assert.notNull(method,
              () -> "Method '%s' with parameter type '%s' declared on %s could not be found.".formatted(
                      this.methodName, this.lookupType.getName(), registeredBean.getBeanClass().getName()));
      return method;
    }
  }

  /**
   * Extension of the DependencyDescriptor class,
   * overriding the dependency type with the specified resource type.
   */
  @SuppressWarnings("serial")
  static class LookupDependencyDescriptor extends DependencyDescriptor {

    private final Class<?> lookupType;

    private final boolean lazyLookup;

    public LookupDependencyDescriptor(Field field, Class<?> lookupType, boolean lazyLookup) {
      super(field, true);
      this.lookupType = lookupType;
      this.lazyLookup = lazyLookup;
    }

    public LookupDependencyDescriptor(Method method, Class<?> lookupType, boolean lazyLookup) {
      super(new MethodParameter(method, 0), true);
      this.lookupType = lookupType;
      this.lazyLookup = lazyLookup;
    }

    @Override
    public Class<?> getDependencyType() {
      return this.lookupType;
    }

    @Override
    public boolean supportsLazyResolution() {
      return !this.lazyLookup;
    }
  }

}
