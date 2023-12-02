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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link DataObjectBinder} for immutable value objects.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ValueObjectBinder implements DataObjectBinder {

  private static final Logger logger = LoggerFactory.getLogger(ValueObjectBinder.class);

  private final BindConstructorProvider constructorProvider;

  ValueObjectBinder(BindConstructorProvider constructorProvider) {
    this.constructorProvider = constructorProvider;
  }

  @Override
  public <T> T bind(ConfigurationPropertyName name, Bindable<T> target,
          Binder.Context context, DataObjectPropertyBinder propertyBinder) {

    ValueObject<T> valueObject = ValueObject.get(target, this.constructorProvider, context);
    if (valueObject == null) {
      return null;
    }
    context.pushConstructorBoundTypes(target.getType().resolve());
    List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
    ArrayList<Object> args = new ArrayList<>(parameters.size());
    boolean bound = false;
    for (ConstructorParameter parameter : parameters) {
      Object arg = parameter.bind(propertyBinder);
      bound = bound || arg != null;
      arg = (arg != null) ? arg : getDefaultValue(context, parameter);
      args.add(arg);
    }
    context.clearConfigurationProperty();
    context.popConstructorBoundTypes();
    return bound ? valueObject.instantiate(args) : null;
  }

  @Override
  public <T> T create(Bindable<T> target, Binder.Context context) {
    ValueObject<T> valueObject = ValueObject.get(target, this.constructorProvider, context);
    if (valueObject == null) {
      return null;
    }
    List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
    ArrayList<Object> args = new ArrayList<>(parameters.size());
    for (ConstructorParameter parameter : parameters) {
      args.add(getDefaultValue(context, parameter));
    }
    return valueObject.instantiate(args);
  }

  @Nullable
  private <T> T getDefaultValue(Binder.Context context, ConstructorParameter parameter) {
    ResolvableType type = parameter.type;
    Annotation[] annotations = parameter.annotations;
    for (Annotation annotation : annotations) {
      if (annotation instanceof DefaultValue defaultValueAnnotation) {
        String[] defaultValue = defaultValueAnnotation.value();
        if (defaultValue.length == 0) {
          return getNewDefaultValueInstanceIfPossible(context, type);
        }
        return convertDefaultValue(context.getConverter(), defaultValue, type, annotations);
      }
    }
    return null;
  }

  @Nullable
  private <T> T convertDefaultValue(BindConverter converter,
          String[] defaultValue, ResolvableType type, Annotation[] annotations) {
    try {
      return converter.convert(defaultValue, type, annotations);
    }
    catch (ConversionException ex) {
      // Try again in case ArrayToObjectConverter is not in play
      if (defaultValue.length == 1) {
        return converter.convert(defaultValue[0], type, annotations);
      }
      throw ex;
    }
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> T getNewDefaultValueInstanceIfPossible(Binder.Context context, ResolvableType type) {
    Class<T> resolved = (Class<T>) type.resolve();
    Assert.state(resolved == null || isEmptyDefaultValueAllowed(resolved),
            () -> "Parameter of type " + type + " must have a non-empty default value.");
    if (resolved != null) {
      if (Optional.class == resolved) {
        return (T) Optional.empty();
      }
      if (Collection.class.isAssignableFrom(resolved)) {
        return (T) CollectionUtils.createCollection(resolved, 0);
      }
      if (Map.class.isAssignableFrom(resolved)) {
        return (T) CollectionUtils.createMap(resolved, 0);
      }
      if (resolved.isArray()) {
        return (T) Array.newInstance(resolved.getComponentType(), 0);
      }
    }
    T instance = create(Bindable.of(type), context);
    if (instance != null) {
      return instance;
    }
    return resolved != null ? BeanUtils.newInstance(resolved) : null;
  }

  private boolean isEmptyDefaultValueAllowed(Class<?> type) {
    return (Optional.class == type || isAggregate(type))
            || !(type.isPrimitive() || type.isEnum() || type.getName().startsWith("java.lang"));
  }

  private boolean isAggregate(Class<?> type) {
    return type.isArray() || Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
  }

  /**
   * The value object being bound.
   *
   * @param <T> the value object type
   */
  private abstract static class ValueObject<T> {

    private final Constructor<T> constructor;

    protected ValueObject(Constructor<T> constructor) {
      this.constructor = constructor;
    }

    T instantiate(List<Object> args) {
      return BeanUtils.newInstance(this.constructor, args.toArray());
    }

    abstract List<ConstructorParameter> getConstructorParameters();

    @Nullable
    @SuppressWarnings("unchecked")
    static <T> ValueObject<T> get(Bindable<T> bindable,
            BindConstructorProvider constructorProvider, Binder.Context context) {
      Class<T> type = (Class<T>) bindable.getType().resolve();
      if (type == null || type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
        return null;
      }
      Constructor<?> bindConstructor = constructorProvider.getBindConstructor(bindable,
              context.isNestedConstructorBinding());
      if (bindConstructor == null) {
        return null;
      }
      return DefaultValueObject.get(bindConstructor, bindable.getType());
    }

  }

  /**
   * A default {@link ValueObject} implementation that uses only standard Java
   * reflection calls.
   */
  private static final class DefaultValueObject<T> extends ValueObject<T> {

    private final List<ConstructorParameter> constructorParameters;

    private DefaultValueObject(Constructor<T> constructor, List<ConstructorParameter> constructorParameters) {
      super(constructor);
      this.constructorParameters = constructorParameters;
    }

    @Override
    List<ConstructorParameter> getConstructorParameters() {
      return this.constructorParameters;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T> ValueObject<T> get(Constructor<?> constructor, ResolvableType type) {
      String[] names = ParameterNameDiscoverer.findParameterNames(constructor);
      if (names == null) {
        logger.debug("Unable to use value object binding with {} as parameter names cannot be discovered", constructor);
        return null;
      }
      List<ConstructorParameter> constructorParameters = parseConstructorParameters(constructor, type, names);
      return new DefaultValueObject<>((Constructor<T>) constructor, constructorParameters);
    }

    private static List<ConstructorParameter> parseConstructorParameters(
            Constructor<?> constructor, ResolvableType type, String[] names) {
      Parameter[] parameters = constructor.getParameters();
      List<ConstructorParameter> result = new ArrayList<>(parameters.length);
      for (int i = 0; i < parameters.length; i++) {
        String name = MergedAnnotations.from(parameters[i])
                .get(Name.class)
                .getValue(MergedAnnotation.VALUE, String.class)
                .orElse(names[i]);
        ResolvableType parameterType = ResolvableType.forMethodParameter(new MethodParameter(constructor, i), type);
        Annotation[] annotations = parameters[i].getDeclaredAnnotations();
        result.add(new ConstructorParameter(name, parameterType, annotations));
      }
      return Collections.unmodifiableList(result);
    }

  }

  /**
   * A constructor parameter being bound.
   */
  private static class ConstructorParameter {

    public final String name;
    public final ResolvableType type;
    public final Annotation[] annotations;

    ConstructorParameter(String name, ResolvableType type, Annotation[] annotations) {
      this.name = DataObjectPropertyName.toDashedForm(name);
      this.type = type;
      this.annotations = annotations;
    }

    @Nullable
    Object bind(DataObjectPropertyBinder propertyBinder) {
      return propertyBinder.bindProperty(this.name, Bindable.of(this.type).withAnnotations(this.annotations));
    }

  }

}
