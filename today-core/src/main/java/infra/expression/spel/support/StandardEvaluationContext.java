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

package infra.expression.spel.support;

import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.core.TypeDescriptor;
import infra.expression.BeanResolver;
import infra.expression.ConstructorResolver;
import infra.expression.EvaluationContext;
import infra.expression.IndexAccessor;
import infra.expression.MethodFilter;
import infra.expression.MethodResolver;
import infra.expression.OperatorOverloader;
import infra.expression.PropertyAccessor;
import infra.expression.TypeComparator;
import infra.expression.TypeConverter;
import infra.expression.TypeLocator;
import infra.expression.TypedValue;
import infra.lang.Assert;

/**
 * A powerful and highly configurable {@link EvaluationContext} implementation.
 *
 * <p>This context uses standard implementations of all applicable strategies,
 * based on reflection to resolve properties, methods, and fields. Note, however,
 * that you may need to manually configure a {@code StandardTypeLocator} with a
 * specific {@link ClassLoader} to ensure that the SpEL expression parser is able
 * to reliably locate user types. See {@link #setTypeLocator(TypeLocator)} for
 * details.
 *
 * <p>In addition to support for setting and looking up variables as defined in
 * the {@link EvaluationContext} API, {@code StandardEvaluationContext} also
 * provides support for registering and looking up functions. The
 * {@code registerFunction(...)} methods provide a convenient way to register a
 * function as a {@link Method} or a {@link MethodHandle}; however, a function
 * can also be registered via {@link #setVariable(String, Object)} or
 * {@link #setVariables(Map)}. Since functions share a namespace with the variables
 * in this evaluation context, care must be taken to ensure that function names
 * and variable names do not overlap.
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
  private volatile List<IndexAccessor> indexAccessors;

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
  @SuppressWarnings("NullAway")
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

  /**
   * Specify the default root context object (including a type descriptor)
   * against which unqualified properties, methods, etc. should be resolved.
   *
   * @param rootObject the root object to use
   * @param typeDescriptor a corresponding type descriptor
   */
  public void setRootObject(@Nullable Object rootObject, @Nullable TypeDescriptor typeDescriptor) {
    this.rootObject = new TypedValue(rootObject, typeDescriptor);
  }

  /**
   * Specify the default root context object against which unqualified
   * properties, methods, etc. should be resolved.
   *
   * @param rootObject the root object to use
   */
  public void setRootObject(@Nullable Object rootObject) {
    this.rootObject = TypedValue.valueOf(rootObject);
  }

  /**
   * Return the configured default root context object against which unqualified
   * properties, methods, etc. should be resolved (can be {@link TypedValue#NULL}).
   */
  @Override
  public TypedValue getRootObject() {
    return this.rootObject;
  }

  /**
   * Set the list of property accessors to use in this evaluation context.
   * <p>Replaces any previously configured property accessors.
   */
  public void setPropertyAccessors(List<PropertyAccessor> propertyAccessors) {
    this.propertyAccessors = propertyAccessors;
  }

  /**
   * Get the list of property accessors configured in this evaluation context.
   */
  @Override
  public List<PropertyAccessor> getPropertyAccessors() {
    return initPropertyAccessors();
  }

  /**
   * Add the supplied property accessor to this evaluation context.
   *
   * @param accessor the property accessor to add
   * @see #getPropertyAccessors()
   * @see #setPropertyAccessors(List)
   * @see #removePropertyAccessor(PropertyAccessor)
   */
  public void addPropertyAccessor(PropertyAccessor accessor) {
    addBeforeDefault(initPropertyAccessors(), accessor);
  }

  /**
   * Remove the supplied property accessor from this evaluation context.
   *
   * @param accessor the property accessor to remove
   * @return {@code true} if the property accessor was removed, {@code false}
   * if the property accessor was not configured in this evaluation context
   * @see #getPropertyAccessors()
   * @see #setPropertyAccessors(List)
   * @see #addPropertyAccessor(PropertyAccessor)
   */
  public boolean removePropertyAccessor(PropertyAccessor accessor) {
    return initPropertyAccessors().remove(accessor);
  }

  /**
   * Set the list of constructor resolvers to use in this evaluation context.
   * <p>Replaces any previously configured constructor resolvers.
   */
  public void setConstructorResolvers(List<ConstructorResolver> constructorResolvers) {
    this.constructorResolvers = constructorResolvers;
  }

  /**
   * Get the list of constructor resolvers to use in this evaluation context.
   */
  @Override
  public List<ConstructorResolver> getConstructorResolvers() {
    return initConstructorResolvers();
  }

  /**
   * Add the supplied constructor resolver to this evaluation context.
   *
   * @param resolver the constructor resolver to add
   * @see #getConstructorResolvers()
   * @see #setConstructorResolvers(List)
   * @see #removeConstructorResolver(ConstructorResolver)
   */
  public void addConstructorResolver(ConstructorResolver resolver) {
    addBeforeDefault(initConstructorResolvers(), resolver);
  }

  /**
   * Remove the supplied constructor resolver from this evaluation context.
   *
   * @param resolver the constructor resolver to remove
   * @return {@code true} if the constructor resolver was removed, {@code false}
   * if the constructor resolver was not configured in this evaluation context
   * @see #getConstructorResolvers()
   * @see #setConstructorResolvers(List)
   * @see #addConstructorResolver(ConstructorResolver)
   */
  public boolean removeConstructorResolver(ConstructorResolver resolver) {
    return initConstructorResolvers().remove(resolver);
  }

  /**
   * Set the list of method resolvers to use in this evaluation context.
   * <p>Replaces any previously configured method resolvers.
   */
  public void setMethodResolvers(List<MethodResolver> methodResolvers) {
    this.methodResolvers = methodResolvers;
  }

  /**
   * Get the list of method resolvers to use in this evaluation context.
   */
  @Override
  public List<MethodResolver> getMethodResolvers() {
    return initMethodResolvers();
  }

  /**
   * Add the supplied method resolver to this evaluation context.
   *
   * @param resolver the method resolver to add
   * @see #getMethodResolvers()
   * @see #setMethodResolvers(List)
   * @see #removeMethodResolver(MethodResolver)
   */
  public void addMethodResolver(MethodResolver resolver) {
    addBeforeDefault(initMethodResolvers(), resolver);
  }

  /**
   * Remove the supplied method resolver from this evaluation context.
   *
   * @param methodResolver the method resolver to remove
   * @return {@code true} if the method resolver was removed, {@code false}
   * if the method resolver was not configured in this evaluation context
   * @see #getMethodResolvers()
   * @see #setMethodResolvers(List)
   * @see #addMethodResolver(MethodResolver)
   */
  public boolean removeMethodResolver(MethodResolver methodResolver) {
    return initMethodResolvers().remove(methodResolver);
  }

  /**
   * Set the {@link BeanResolver} to use for looking up beans, if any.
   */
  public void setBeanResolver(@Nullable BeanResolver beanResolver) {
    this.beanResolver = beanResolver;
  }

  /**
   * Get the configured {@link BeanResolver} for looking up beans, if any.
   */
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

  /**
   * Set the {@link TypeConverter} for value conversion.
   */
  public void setTypeConverter(TypeConverter typeConverter) {
    Assert.notNull(typeConverter, "TypeConverter is required");
    this.typeConverter = typeConverter;
  }

  /**
   * Get the configured {@link TypeConverter} for value conversion.
   */
  @Override
  public TypeConverter getTypeConverter() {
    if (this.typeConverter == null) {
      this.typeConverter = new StandardTypeConverter();
    }
    return this.typeConverter;
  }

  /**
   * Set the {@link TypeComparator} for comparing pairs of objects.
   */
  public void setTypeComparator(TypeComparator typeComparator) {
    Assert.notNull(typeComparator, "TypeComparator is required");
    this.typeComparator = typeComparator;
  }

  /**
   * Get the configured {@link TypeComparator} for comparing pairs of objects.
   */
  @Override
  public TypeComparator getTypeComparator() {
    return this.typeComparator;
  }

  /**
   * Set the {@link OperatorOverloader} for mathematical operations.
   */
  public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
    Assert.notNull(operatorOverloader, "OperatorOverloader is required");
    this.operatorOverloader = operatorOverloader;
  }

  /**
   * Get the configured {@link OperatorOverloader} for mathematical operations.
   */
  @Override
  public OperatorOverloader getOperatorOverloader() {
    return this.operatorOverloader;
  }

  /**
   * Set the list of index accessors to use in this evaluation context.
   * <p>Replaces any previously configured index accessors.
   *
   * @see #getIndexAccessors()
   * @see #addIndexAccessor(IndexAccessor)
   * @see #removeIndexAccessor(IndexAccessor)
   */
  public void setIndexAccessors(List<IndexAccessor> indexAccessors) {
    this.indexAccessors = indexAccessors;
  }

  /**
   * Get the list of index accessors configured in this evaluation context.
   *
   * @see #setIndexAccessors(List)
   * @see #addIndexAccessor(IndexAccessor)
   * @see #removeIndexAccessor(IndexAccessor)
   */
  @Override
  public List<IndexAccessor> getIndexAccessors() {
    return initIndexAccessors();
  }

  /**
   * Add the supplied index accessor to this evaluation context.
   *
   * @param indexAccessor the index accessor to add
   * @see #getIndexAccessors()
   * @see #setIndexAccessors(List)
   * @see #removeIndexAccessor(IndexAccessor)
   */
  public void addIndexAccessor(IndexAccessor indexAccessor) {
    initIndexAccessors().add(indexAccessor);
  }

  /**
   * Remove the supplied index accessor from this evaluation context.
   *
   * @param indexAccessor the index accessor to remove
   * @return {@code true} if the index accessor was removed, {@code false} if
   * the index accessor was not configured in this evaluation context
   * @see #getIndexAccessors()
   * @see #setIndexAccessors(List)
   * @see #addIndexAccessor(IndexAccessor)
   */
  public boolean removeIndexAccessor(IndexAccessor indexAccessor) {
    return initIndexAccessors().remove(indexAccessor);
  }

  /**
   * Set a named variable in this evaluation context to a specified value.
   * <p>If the specified {@code name} is {@code null}, it will be ignored. If
   * the specified {@code value} is {@code null}, the named variable will be
   * removed from this evaluation context.
   * <p>In contrast to {@link #assignVariable(String, java.util.function.Supplier)},
   * this method should only be invoked programmatically when interacting directly
   * with the {@code EvaluationContext} &mdash; for example, to provide initial
   * configuration for the context.
   * <p>Note that variables and functions share a common namespace in this
   * evaluation context. See the {@linkplain StandardEvaluationContext
   * class-level documentation} for details.
   *
   * @param name the name of the variable to set
   * @param value the value to be placed in the variable
   * @see #setVariables(Map)
   * @see #registerFunction(String, Method)
   * @see #registerFunction(String, MethodHandle)
   * @see #lookupVariable(String)
   */
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
   * Set multiple named variables in this evaluation context to the specified values.
   * <p>This is a convenience variant of {@link #setVariable(String, Object)}.
   * <p>Note that variables and functions share a common namespace in this
   * evaluation context. See the {@linkplain StandardEvaluationContext
   * class-level documentation} for details.
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
   * Register the specified {@link Method} as a SpEL function.
   * <p>Note that variables and functions share a common namespace in this
   * evaluation context. See the {@linkplain StandardEvaluationContext
   * class-level documentation} for details.
   *
   * @param name the name of the function
   * @param method the {@code Method} to register
   * @see #registerFunction(String, MethodHandle)
   */
  public void registerFunction(String name, Method method) {
    this.variables.put(name, method);
  }

  /**
   * Register the specified {@link MethodHandle} as a SpEL function.
   * <p>Note that variables and functions share a common namespace in this
   * evaluation context. See the {@linkplain StandardEvaluationContext
   * class-level documentation} for details.
   *
   * @param name the name of the function
   * @param methodHandle the {@link MethodHandle} to register
   * @see #registerFunction(String, Method)
   */
  public void registerFunction(String name, MethodHandle methodHandle) {
    this.variables.put(name, methodHandle);
  }

  /**
   * Look up a named variable or function within this evaluation context.
   * <p>Note that variables and functions share a common namespace in this
   * evaluation context. See the {@linkplain StandardEvaluationContext
   * class-level documentation} for details.
   *
   * @param name the name of the variable or function to look up
   * @return the value of the variable or function, or {@code null} if not found
   */
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

  private List<IndexAccessor> initIndexAccessors() {
    List<IndexAccessor> accessors = this.indexAccessors;
    if (accessors == null) {
      accessors = new ArrayList<>(5);
      this.indexAccessors = accessors;
    }
    return accessors;
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
