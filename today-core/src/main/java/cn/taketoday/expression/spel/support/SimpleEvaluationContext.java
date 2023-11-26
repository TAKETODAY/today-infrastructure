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

package cn.taketoday.expression.spel.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.expression.BeanResolver;
import cn.taketoday.expression.ConstructorResolver;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.MethodResolver;
import cn.taketoday.expression.OperatorOverloader;
import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypeLocator;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Nullable;

/**
 * A basic implementation of {@link EvaluationContext} that focuses on a subset
 * of essential SpEL features and customization options, targeting simple
 * condition evaluation and in particular data binding scenarios.
 *
 * <p>In many cases, the full extent of the SpEL language is not required and
 * should be meaningfully restricted. Examples include but are not limited to
 * data binding expressions, property-based filters, and others. To that effect,
 * {@code SimpleEvaluationContext} is tailored to support only a subset of the
 * SpEL language syntax, e.g. excluding references to Java types, constructors,
 * and bean references.
 *
 * <p>When creating a {@code SimpleEvaluationContext} you need to choose the
 * level of support that you need for property access in SpEL expressions:
 * <ul>
 * <li>A custom {@code PropertyAccessor} (typically not reflection-based),
 * potentially combined with a {@link DataBindingPropertyAccessor}</li>
 * <li>Data binding properties for read-only access</li>
 * <li>Data binding properties for read and write</li>
 * </ul>
 *
 * <p>Conveniently, {@link SimpleEvaluationContext#forReadOnlyDataBinding()}
 * enables read access to properties via {@link DataBindingPropertyAccessor};
 * same for {@link SimpleEvaluationContext#forReadWriteDataBinding()} when
 * write access is needed as well. Alternatively, configure custom accessors
 * via {@link SimpleEvaluationContext#forPropertyAccessors}, and potentially
 * activate method resolution and/or a type converter through the builder.
 *
 * <p>Note that {@code SimpleEvaluationContext} is typically not configured
 * with a default root object. Instead it is meant to be created once and
 * used repeatedly through {@code getValue} calls on a pre-compiled
 * {@link Expression} with both an
 * {@code EvaluationContext} and a root object as arguments:
 * {@link Expression#getValue(EvaluationContext, Object)}.
 *
 * <p>For more power and flexibility, in particular for internal configuration
 * scenarios, consider using {@link StandardEvaluationContext} instead.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #forPropertyAccessors
 * @see #forReadOnlyDataBinding()
 * @see #forReadWriteDataBinding()
 * @see StandardEvaluationContext
 * @see StandardTypeConverter
 * @see DataBindingPropertyAccessor
 * @since 4.0
 */
public final class SimpleEvaluationContext implements EvaluationContext {

  private static final TypeLocator typeNotFoundTypeLocator = typeName -> {
    throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
  };

  private final TypedValue rootObject;

  private final List<PropertyAccessor> propertyAccessors;

  private final List<MethodResolver> methodResolvers;

  private final TypeConverter typeConverter;

  private final HashMap<String, Object> variables = new HashMap<>();

  private SimpleEvaluationContext(List<PropertyAccessor> accessors, List<MethodResolver> resolvers,
          @Nullable TypeConverter converter, @Nullable TypedValue rootObject) {

    this.propertyAccessors = accessors;
    this.methodResolvers = resolvers;
    this.typeConverter = (converter != null ? converter : new StandardTypeConverter());
    this.rootObject = (rootObject != null ? rootObject : TypedValue.NULL);
  }

  /**
   * Return the specified root object, if any.
   */
  @Override
  public TypedValue getRootObject() {
    return this.rootObject;
  }

  /**
   * Return the specified {@link PropertyAccessor} delegates, if any.
   *
   * @see #forPropertyAccessors
   */
  @Override
  public List<PropertyAccessor> getPropertyAccessors() {
    return this.propertyAccessors;
  }

  /**
   * Return an empty list, always, since this context does not support the
   * use of type references.
   */
  @Override
  public List<ConstructorResolver> getConstructorResolvers() {
    return Collections.emptyList();
  }

  /**
   * Return the specified {@link MethodResolver} delegates, if any.
   *
   * @see Builder#withMethodResolvers
   */
  @Override
  public List<MethodResolver> getMethodResolvers() {
    return this.methodResolvers;
  }

  /**
   * {@code SimpleEvaluationContext} does not support the use of bean references.
   *
   * @return always {@code null}
   */
  @Override
  @Nullable
  public BeanResolver getBeanResolver() {
    return null;
  }

  /**
   * {@code SimpleEvaluationContext} does not support use of type references.
   *
   * @return {@code TypeLocator} implementation that raises a
   * {@link SpelEvaluationException} with {@link SpelMessage#TYPE_NOT_FOUND}.
   */
  @Override
  public TypeLocator getTypeLocator() {
    return typeNotFoundTypeLocator;
  }

  /**
   * The configured {@link TypeConverter}.
   * <p>By default this is {@link StandardTypeConverter}.
   *
   * @see Builder#withTypeConverter
   * @see Builder#withConversionService
   */
  @Override
  public TypeConverter getTypeConverter() {
    return this.typeConverter;
  }

  /**
   * Return an instance of {@link StandardTypeComparator}.
   */
  @Override
  public TypeComparator getTypeComparator() {
    return TypeComparator.STANDARD;
  }

  /**
   * Return an instance of {@link StandardOperatorOverloader}.
   */
  @Override
  public OperatorOverloader getOperatorOverloader() {
    return OperatorOverloader.STANDARD;
  }

  /**
   * {@code SimpleEvaluationContext} does not support variable assignment within
   * expressions.
   *
   * @throws SpelEvaluationException with {@link SpelMessage#VARIABLE_ASSIGNMENT_NOT_SUPPORTED}
   */
  @Override
  public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
    throw new SpelEvaluationException(SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED, "#" + name);
  }

  @Override
  public void setVariable(String name, @Nullable Object value) {
    this.variables.put(name, value);
  }

  @Override
  @Nullable
  public Object lookupVariable(String name) {
    return this.variables.get(name);
  }

  /**
   * Create a {@code SimpleEvaluationContext} for the specified {@link PropertyAccessor}
   * delegates: typically a custom {@code PropertyAccessor} specific to a use case
   * (e.g. attribute resolution in a custom data structure), potentially combined with
   * a {@link DataBindingPropertyAccessor} if property dereferences are needed as well.
   *
   * @param accessors the accessor delegates to use
   * @see DataBindingPropertyAccessor#forReadOnlyAccess()
   * @see DataBindingPropertyAccessor#forReadWriteAccess()
   */
  public static Builder forPropertyAccessors(PropertyAccessor... accessors) {
    for (PropertyAccessor accessor : accessors) {
      if (accessor.getClass() == ReflectivePropertyAccessor.class) {
        throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
                "ReflectivePropertyAccessor. Consider using DataBindingPropertyAccessor or a custom subclass.");
      }
    }
    return new Builder(accessors);
  }

  /**
   * Create a {@code SimpleEvaluationContext} for read-only access to
   * public properties via {@link DataBindingPropertyAccessor}.
   *
   * @see DataBindingPropertyAccessor#forReadOnlyAccess()
   * @see #forPropertyAccessors
   */
  public static Builder forReadOnlyDataBinding() {
    return new Builder(DataBindingPropertyAccessor.forReadOnlyAccess());
  }

  /**
   * Create a {@code SimpleEvaluationContext} for read-write access to
   * public properties via {@link DataBindingPropertyAccessor}.
   *
   * @see DataBindingPropertyAccessor#forReadWriteAccess()
   * @see #forPropertyAccessors
   */
  public static Builder forReadWriteDataBinding() {
    return new Builder(DataBindingPropertyAccessor.forReadWriteAccess());
  }

  /**
   * Builder for {@code SimpleEvaluationContext}.
   */
  public static class Builder {

    private final List<PropertyAccessor> accessors;

    private List<MethodResolver> resolvers = Collections.emptyList();

    @Nullable
    private TypeConverter typeConverter;

    @Nullable
    private TypedValue rootObject;

    public Builder(PropertyAccessor... accessors) {
      this.accessors = Arrays.asList(accessors);
    }

    /**
     * Register the specified {@link MethodResolver} delegates for
     * a combination of property access and method resolution.
     *
     * @param resolvers the resolver delegates to use
     * @see #withInstanceMethods()
     * @see SimpleEvaluationContext#forPropertyAccessors
     */
    public Builder withMethodResolvers(MethodResolver... resolvers) {
      for (MethodResolver resolver : resolvers) {
        if (resolver.getClass() == ReflectiveMethodResolver.class) {
          throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
                  "ReflectiveMethodResolver. Consider using DataBindingMethodResolver or a custom subclass.");
        }
      }
      this.resolvers = Arrays.asList(resolvers);
      return this;
    }

    /**
     * Register a {@link DataBindingMethodResolver} for instance method invocation purposes
     * (i.e. not supporting static methods) in addition to the specified property accessors,
     * typically in combination with a {@link DataBindingPropertyAccessor}.
     *
     * @see #withMethodResolvers
     * @see SimpleEvaluationContext#forReadOnlyDataBinding()
     * @see SimpleEvaluationContext#forReadWriteDataBinding()
     */
    public Builder withInstanceMethods() {
      this.resolvers = Collections.singletonList(DataBindingMethodResolver.forInstanceMethodInvocation());
      return this;
    }

    /**
     * Register a custom {@link ConversionService}.
     * <p>By default a {@link StandardTypeConverter} backed by a
     * {@link cn.taketoday.core.conversion.support.DefaultConversionService} is used.
     *
     * @see #withTypeConverter
     * @see StandardTypeConverter#StandardTypeConverter(ConversionService)
     */
    public Builder withConversionService(ConversionService conversionService) {
      this.typeConverter = new StandardTypeConverter(conversionService);
      return this;
    }

    /**
     * Register a custom {@link TypeConverter}.
     * <p>By default a {@link StandardTypeConverter} backed by a
     * {@link cn.taketoday.core.conversion.support.DefaultConversionService} is used.
     *
     * @see #withConversionService
     * @see StandardTypeConverter#StandardTypeConverter()
     */
    public Builder withTypeConverter(TypeConverter converter) {
      this.typeConverter = converter;
      return this;
    }

    /**
     * Specify a default root object to resolve against.
     * <p>Default is none, expecting an object argument at evaluation time.
     *
     * @see Expression#getValue(EvaluationContext)
     * @see Expression#getValue(EvaluationContext, Object)
     */
    public Builder withRootObject(Object rootObject) {
      this.rootObject = new TypedValue(rootObject);
      return this;
    }

    /**
     * Specify a typed root object to resolve against.
     * <p>Default is none, expecting an object argument at evaluation time.
     *
     * @see Expression#getValue(EvaluationContext)
     * @see Expression#getValue(EvaluationContext, Object)
     */
    public Builder withTypedRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
      this.rootObject = new TypedValue(rootObject, typeDescriptor);
      return this;
    }

    public SimpleEvaluationContext build() {
      return new SimpleEvaluationContext(this.accessors, this.resolvers, this.typeConverter, this.rootObject);
    }
  }

}
