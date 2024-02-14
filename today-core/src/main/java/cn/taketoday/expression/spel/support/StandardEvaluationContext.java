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

package cn.taketoday.expression.spel.support;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.BeanResolver;
import cn.taketoday.expression.ConstructorResolver;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.MethodFilter;
import cn.taketoday.expression.MethodResolver;
import cn.taketoday.expression.OperatorOverloader;
import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypeLocator;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A powerful and highly configurable {@link EvaluationContext} implementation.
 * This context uses standard implementations of all applicable strategies,
 * based on reflection to resolve properties, methods and fields.
 *
 * <p>For a simpler builder-style context variant for data-binding purposes,
 * consider using {@link SimpleEvaluationContext} instead which allows for
 * opting into several SpEL features as needed by specific evaluation cases.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleEvaluationContext
 * @see ReflectivePropertyAccessor
 * @see ReflectiveConstructorResolver
 * @see ReflectiveMethodResolver
 * @see StandardTypeLocator
 * @see StandardTypeConverter
 * @see StandardTypeComparator
 * @see StandardOperatorOverloader
 * @since 4.0
 */
public class StandardEvaluationContext implements EvaluationContext {

  private TypedValue rootObject;

  @Nullable
  private volatile List<PropertyAccessor> propertyAccessors;

  @Nullable
  private volatile List<ConstructorResolver> constructorResolvers;

  @Nullable
  private volatile List<MethodResolver> methodResolvers;

  @Nullable
  private volatile ReflectiveMethodResolver reflectiveMethodResolver;

  @Nullable
  private BeanResolver beanResolver;

  @Nullable
  private TypeLocator typeLocator;

  @Nullable
  private TypeConverter typeConverter;

  private TypeComparator typeComparator = TypeComparator.STANDARD;

  private OperatorOverloader operatorOverloader = OperatorOverloader.STANDARD;

  private final ConcurrentHashMap<String, Object> variables = new ConcurrentHashMap<>();

  /**
   * Create a {@code StandardEvaluationContext} with a null root object.
   */
  public StandardEvaluationContext() {
    this.rootObject = TypedValue.NULL;
  }

  /**
   * Create a {@code StandardEvaluationContext} with the given root object.
   *
   * @param rootObject the root object to use
   * @see #setRootObject
   */
  public StandardEvaluationContext(@Nullable Object rootObject) {
    this.rootObject = TypedValue.valueOf(rootObject);
  }

  /**
   * Create a {@code StandardEvaluationContext} with the given root object.
   */
  protected StandardEvaluationContext(StandardEvaluationContext shared) {
    this.typeLocator = shared.getTypeLocator();
    this.typeConverter = shared.getTypeConverter();
    this.methodResolvers = new ArrayList<>(shared.getMethodResolvers());
    this.propertyAccessors = new ArrayList<>(shared.getPropertyAccessors());
    this.constructorResolvers = new ArrayList<>(shared.getConstructorResolvers());

    this.beanResolver = shared.beanResolver;
    this.typeComparator = shared.typeComparator;
    this.operatorOverloader = shared.operatorOverloader;
    this.reflectiveMethodResolver = shared.reflectiveMethodResolver;
  }

  /**
   * Create a {@code StandardEvaluationContext} with the given root object.
   *
   * @param rootObject the root object to use
   * @see #setRootObject
   */
  protected StandardEvaluationContext(@Nullable Object rootObject, StandardEvaluationContext shared) {
    this(shared);
    this.rootObject = TypedValue.valueOf(rootObject);
  }

  public void setRootObject(@Nullable Object rootObject, @Nullable TypeDescriptor typeDescriptor) {
    this.rootObject = new TypedValue(rootObject, typeDescriptor);
  }

  public void setRootObject(@Nullable Object rootObject) {
    this.rootObject = TypedValue.valueOf(rootObject);
  }

  @Override
  public TypedValue getRootObject() {
    return this.rootObject;
  }

  public void setPropertyAccessors(List<PropertyAccessor> propertyAccessors) {
    this.propertyAccessors = propertyAccessors;
  }

  @Override
  public List<PropertyAccessor> getPropertyAccessors() {
    return initPropertyAccessors();
  }

  public void addPropertyAccessor(PropertyAccessor accessor) {
    addBeforeDefault(initPropertyAccessors(), accessor);
  }

  public boolean removePropertyAccessor(PropertyAccessor accessor) {
    return initPropertyAccessors().remove(accessor);
  }

  public void setConstructorResolvers(List<ConstructorResolver> constructorResolvers) {
    this.constructorResolvers = constructorResolvers;
  }

  @Override
  public List<ConstructorResolver> getConstructorResolvers() {
    return initConstructorResolvers();
  }

  public void addConstructorResolver(ConstructorResolver resolver) {
    addBeforeDefault(initConstructorResolvers(), resolver);
  }

  public boolean removeConstructorResolver(ConstructorResolver resolver) {
    return initConstructorResolvers().remove(resolver);
  }

  public void setMethodResolvers(List<MethodResolver> methodResolvers) {
    this.methodResolvers = methodResolvers;
  }

  @Override
  public List<MethodResolver> getMethodResolvers() {
    return initMethodResolvers();
  }

  public void addMethodResolver(MethodResolver resolver) {
    addBeforeDefault(initMethodResolvers(), resolver);
  }

  public boolean removeMethodResolver(MethodResolver methodResolver) {
    return initMethodResolvers().remove(methodResolver);
  }

  public void setBeanResolver(BeanResolver beanResolver) {
    this.beanResolver = beanResolver;
  }

  @Override
  @Nullable
  public BeanResolver getBeanResolver() {
    return this.beanResolver;
  }

  /**
   * Set the {@link TypeLocator} to use to find types, either by short or
   * fully-qualified name.
   * <p>By default, a {@link StandardTypeLocator} will be used.
   * <p><strong>NOTE</strong>: Even if a {@code StandardTypeLocator} is
   * sufficient, you may need to manually configure a {@code StandardTypeLocator}
   * with a specific {@link ClassLoader} to ensure that the SpEL expression
   * parser is able to reliably locate user types.
   *
   * @param typeLocator the {@code TypeLocator} to use
   * @see StandardTypeLocator#StandardTypeLocator(ClassLoader)
   * @see #getTypeLocator()
   */
  public void setTypeLocator(TypeLocator typeLocator) {
    Assert.notNull(typeLocator, "TypeLocator is required");
    this.typeLocator = typeLocator;
  }

  /**
   * Get the configured {@link TypeLocator} that will be used to find types,
   * either by short or fully-qualified name.
   * <p>See {@link #setTypeLocator(TypeLocator)} for further details.
   *
   * @see #setTypeLocator(TypeLocator)
   */
  @Override
  public TypeLocator getTypeLocator() {
    if (this.typeLocator == null) {
      this.typeLocator = new StandardTypeLocator();
    }
    return this.typeLocator;
  }

  public void setTypeConverter(TypeConverter typeConverter) {
    Assert.notNull(typeConverter, "TypeConverter is required");
    this.typeConverter = typeConverter;
  }

  @Override
  public TypeConverter getTypeConverter() {
    if (this.typeConverter == null) {
      this.typeConverter = new StandardTypeConverter();
    }
    return this.typeConverter;
  }

  public void setTypeComparator(TypeComparator typeComparator) {
    Assert.notNull(typeComparator, "TypeComparator is required");
    this.typeComparator = typeComparator;
  }

  @Override
  public TypeComparator getTypeComparator() {
    return this.typeComparator;
  }

  public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
    Assert.notNull(operatorOverloader, "OperatorOverloader is required");
    this.operatorOverloader = operatorOverloader;
  }

  @Override
  public OperatorOverloader getOperatorOverloader() {
    return this.operatorOverloader;
  }

  @Override
  public void setVariable(@Nullable String name, @Nullable Object value) {
    // For backwards compatibility, we ignore null names here...
    // And since ConcurrentHashMap cannot store null values, we simply take null
    // as a remove from the Map (with the same result from lookupVariable below).
    if (name != null) {
      if (value != null) {
        this.variables.put(name, value);
      }
      else {
        this.variables.remove(name);
      }
    }
  }

  /**
   * Set multiple named variables in this evaluation context to given values.
   * <p>This is a convenience variant of {@link #setVariable(String, Object)}.
   *
   * @param variables the names and values of the variables to set
   * @see #setVariable(String, Object)
   */
  public void setVariables(Map<String, Object> variables) {
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      setVariable(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Register the specified Method as a SpEL function.
   * <p>Note: Function names share a namespace with the variables in this
   * evaluation context, as populated by {@link #setVariable(String, Object)}.
   * Make sure that specified function names and variable names do not overlap.
   *
   * @param name the name of the function
   * @param method the Method to register
   * @see #registerFunction(String, MethodHandle)
   */
  public void registerFunction(String name, Method method) {
    this.variables.put(name, method);
  }

  /**
   * Register the specified MethodHandle as a SpEL function.
   * <p>Note: Function names share a namespace with the variables in this
   * evaluation context, as populated by {@link #setVariable(String, Object)}.
   * Make sure that specified function names and variable names do not overlap.
   *
   * @param name the name of the function
   * @param methodHandle the MethodHandle to register
   * @see #registerFunction(String, Method)
   */
  public void registerFunction(String name, MethodHandle methodHandle) {
    this.variables.put(name, methodHandle);
  }

  @Override
  @Nullable
  public Object lookupVariable(String name) {
    return this.variables.get(name);
  }

  /**
   * Register a {@code MethodFilter} which will be called during method resolution
   * for the specified type.
   * <p>The {@code MethodFilter} may remove methods and/or sort the methods which
   * will then be used by SpEL as the candidates to look through for a match.
   *
   * @param type the type for which the filter should be called
   * @param filter a {@code MethodFilter}, or {@code null} to unregister a filter for the type
   * @throws IllegalStateException if the {@link ReflectiveMethodResolver} is not in use
   */
  public void registerMethodFilter(Class<?> type, MethodFilter filter) throws IllegalStateException {
    initMethodResolvers();
    ReflectiveMethodResolver resolver = this.reflectiveMethodResolver;
    if (resolver == null) {
      throw new IllegalStateException(
              "Method filter cannot be set as the reflective method resolver is not in use");
    }
    resolver.registerMethodFilter(type, filter);
  }

  private List<PropertyAccessor> initPropertyAccessors() {
    List<PropertyAccessor> accessors = this.propertyAccessors;
    if (accessors == null) {
      accessors = new ArrayList<>(5);
      accessors.add(new ReflectivePropertyAccessor());
      this.propertyAccessors = accessors;
    }
    return accessors;
  }

  private List<ConstructorResolver> initConstructorResolvers() {
    List<ConstructorResolver> resolvers = this.constructorResolvers;
    if (resolvers == null) {
      resolvers = new ArrayList<>(1);
      resolvers.add(new ReflectiveConstructorResolver());
      this.constructorResolvers = resolvers;
    }
    return resolvers;
  }

  private List<MethodResolver> initMethodResolvers() {
    List<MethodResolver> resolvers = this.methodResolvers;
    if (resolvers == null) {
      resolvers = new ArrayList<>(1);
      this.reflectiveMethodResolver = new ReflectiveMethodResolver();
      resolvers.add(this.reflectiveMethodResolver);
      this.methodResolvers = resolvers;
    }
    return resolvers;
  }

  private static <T> void addBeforeDefault(List<T> resolvers, T resolver) {
    resolvers.add(resolvers.size() - 1, resolver);
  }

}
