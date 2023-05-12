/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.context.properties.bind;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.properties.bind.Binder.Context;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.ConfigurationPropertyState;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link DataObjectBinder} for mutable Java Beans.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JavaBeanBinder implements DataObjectBinder {

  static final JavaBeanBinder INSTANCE = new JavaBeanBinder();

  @Override
  public <T> T bind(
          ConfigurationPropertyName name,
          Bindable<T> target, Context context,
          DataObjectPropertyBinder propertyBinder) {
    boolean hasKnownBindableProperties = target.getValue() != null && hasKnownBindableProperties(name, context);
    Bean<T> bean = Bean.get(target, hasKnownBindableProperties);
    if (bean == null) {
      return null;
    }
    BeanSupplier<T> beanSupplier = bean.getSupplier(target);
    boolean bound = bind(propertyBinder, bean, beanSupplier, context);
    return (bound ? beanSupplier.get() : null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T create(Bindable<T> target, Context context) {
    Class<T> type = (Class<T>) target.getType().resolve();
    return (type != null) ? BeanUtils.newInstance(type) : null;
  }

  private boolean hasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
    for (ConfigurationPropertySource source : context.getSources()) {
      if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
        return true;
      }
    }
    return false;
  }

  private <T> boolean bind(
          DataObjectPropertyBinder propertyBinder,
          Bean<T> bean, BeanSupplier<T> beanSupplier, Context context) {
    boolean bound = false;
    for (BeanProperty beanProperty : bean.getProperties().values()) {
      bound |= bind(beanSupplier, propertyBinder, beanProperty);
      context.clearConfigurationProperty();
    }
    return bound;
  }

  private <T> boolean bind(
          BeanSupplier<T> beanSupplier,
          DataObjectPropertyBinder propertyBinder, BeanProperty property) {
    String propertyName = property.getName();
    ResolvableType type = property.getType();
    Supplier<Object> value = property.getValue(beanSupplier);
    Annotation[] annotations = property.getAnnotations();
    Object bound = propertyBinder.bindProperty(
            propertyName,
            Bindable.of(type).withSuppliedValue(value).withAnnotations(annotations)
    );
    if (bound == null) {
      return false;
    }
    if (property.isSettable()) {
      property.setValue(beanSupplier, bound);
    }
    else if (value == null || !bound.equals(value.get())) {
      throw new IllegalStateException("No setter found for property: " + property.getName());
    }
    return true;
  }

  /**
   * The bean being bound.
   *
   * @param <T> the bean type
   */
  static class Bean<T> {

    @Nullable
    private static Bean<?> cached;

    private final ResolvableType type;

    @Nullable
    private final Class<?> resolvedType;

    private final Map<String, BeanProperty> properties = new LinkedHashMap<>();

    Bean(ResolvableType type, @Nullable Class<?> resolvedType) {
      this.type = type;
      this.resolvedType = resolvedType;
      addProperties(resolvedType);
    }

    private void addProperties(@Nullable Class<?> type) {
      while (type != null && !Object.class.equals(type)) {
        Method[] declaredMethods = getSorted(type, this::getDeclaredMethods, Method::getName);
        Field[] declaredFields = getSorted(type, Class::getDeclaredFields, Field::getName);
        addProperties(declaredMethods, declaredFields);
        type = type.getSuperclass();
      }
    }

    private Method[] getDeclaredMethods(Class<?> type) {
      Method[] methods = type.getDeclaredMethods();
      Set<Method> result = new LinkedHashSet<>(methods.length);
      for (Method method : methods) {
        result.add(BridgeMethodResolver.findBridgedMethod(method));
      }
      return result.toArray(new Method[0]);
    }

    private <S, E> E[] getSorted(S source, Function<S, E[]> elements, Function<E, String> name) {
      E[] result = elements.apply(source);
      Arrays.sort(result, Comparator.comparing(name));
      return result;
    }

    protected void addProperties(Method[] declaredMethods, Field[] declaredFields) {
      for (int i = 0; i < declaredMethods.length; i++) {
        if (!isCandidate(declaredMethods[i])) {
          declaredMethods[i] = null;
        }
      }
      for (Method method : declaredMethods) {
        addMethodIfPossible(method, "is", 0, BeanProperty::addGetter);
      }
      for (Method method : declaredMethods) {
        addMethodIfPossible(method, "get", 0, BeanProperty::addGetter);
      }
      for (Method method : declaredMethods) {
        addMethodIfPossible(method, "set", 1, BeanProperty::addSetter);
      }
      for (Field field : declaredFields) {
        addField(field);
      }
    }

    private boolean isCandidate(Method method) {
      int modifiers = method.getModifiers();
      return !Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isAbstract(modifiers)
              && !Modifier.isStatic(modifiers) && !method.isBridge()
              && !Object.class.equals(method.getDeclaringClass())
              && !Class.class.equals(method.getDeclaringClass()) && method.getName().indexOf('$') == -1;
    }

    private void addMethodIfPossible(Method method, String prefix, int parameterCount,
            BiConsumer<BeanProperty, Method> consumer) {
      if (method != null && method.getParameterCount() == parameterCount
              && method.getName().startsWith(prefix)
              && method.getName().length() > prefix.length()) {
        String propertyName = Introspector.decapitalize(method.getName().substring(prefix.length()));
        consumer.accept(this.properties.computeIfAbsent(propertyName, this::getBeanProperty), method);
      }
    }

    private BeanProperty getBeanProperty(String name) {
      return new BeanProperty(name, this.type);
    }

    private void addField(Field field) {
      BeanProperty property = this.properties.get(field.getName());
      if (property != null) {
        property.addField(field);
      }
    }

    Map<String, BeanProperty> getProperties() {
      return this.properties;
    }

    @SuppressWarnings("unchecked")
    BeanSupplier<T> getSupplier(Bindable<T> target) {
      return new BeanSupplier<>(() -> {
        T instance = null;
        if (target.getValue() != null) {
          instance = target.getValue().get();
        }
        if (instance == null) {
          instance = (T) BeanUtils.newInstance(this.resolvedType);
        }
        return instance;
      });
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static <T> Bean<T> get(Bindable<T> bindable, boolean canCallGetValue) {
      ResolvableType type = bindable.getType();
      Class<?> resolvedType = type.resolve(Object.class);
      Supplier<T> value = bindable.getValue();
      T instance = null;
      if (canCallGetValue && value != null) {
        instance = value.get();
        resolvedType = (instance != null) ? instance.getClass() : resolvedType;
      }
      if (instance == null && !isInstantiable(resolvedType)) {
        return null;
      }
      Bean<?> bean = Bean.cached;
      if (bean == null || !bean.isOfType(type, resolvedType)) {
        bean = new Bean<>(type, resolvedType);
        cached = bean;
      }
      return (Bean<T>) bean;
    }

    private static boolean isInstantiable(Class<?> type) {
      if (type.isInterface()) {
        return false;
      }
      try {
        type.getDeclaredConstructor();
        return true;
      }
      catch (Exception ex) {
        return false;
      }
    }

    private boolean isOfType(ResolvableType type, Class<?> resolvedType) {
      if (this.type.hasGenerics() || type.hasGenerics()) {
        return this.type.equals(type);
      }
      return this.resolvedType != null && this.resolvedType.equals(resolvedType);
    }

  }

  private static class BeanSupplier<T> implements Supplier<T> {

    private final Supplier<T> factory;

    @Nullable
    private T instance;

    BeanSupplier(Supplier<T> factory) {
      this.factory = factory;
    }

    @Override
    public T get() {
      if (this.instance == null) {
        this.instance = this.factory.get();
      }
      return this.instance;
    }

  }

  /**
   * A bean property being bound.
   */
  static class BeanProperty {

    private final String name;

    private final ResolvableType declaringClassType;

    @Nullable
    private Method getter;

    @Nullable
    private Method setter;

    @Nullable
    private Field field;

    BeanProperty(String name, ResolvableType declaringClassType) {
      this.name = DataObjectPropertyName.toDashedForm(name);
      this.declaringClassType = declaringClassType;
    }

    void addGetter(Method getter) {
      if (this.getter == null || isBetterGetter(getter)) {
        this.getter = getter;
      }
    }

    private boolean isBetterGetter(Method getter) {
      return this.getter != null && this.getter.getName().startsWith("is");
    }

    void addSetter(Method setter) {
      if (this.setter == null || isBetterSetter(setter)) {
        this.setter = setter;
      }
    }

    private boolean isBetterSetter(Method setter) {
      return this.getter != null && this.getter.getReturnType().equals(setter.getParameterTypes()[0]);
    }

    void addField(Field field) {
      if (this.field == null) {
        this.field = field;
      }
    }

    String getName() {
      return this.name;
    }

    ResolvableType getType() {
      if (this.setter != null) {
        MethodParameter methodParameter = new MethodParameter(this.setter, 0);
        return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
      }
      MethodParameter methodParameter = new MethodParameter(this.getter, -1);
      return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
    }

    @Nullable
    Annotation[] getAnnotations() {
      try {
        return (this.field != null) ? this.field.getDeclaredAnnotations() : null;
      }
      catch (Exception ex) {
        return null;
      }
    }

    @Nullable
    Supplier<Object> getValue(Supplier<?> instance) {
      if (this.getter == null) {
        return null;
      }
      return () -> {
        try {
          ReflectionUtils.makeAccessible(getter);
          return getter.invoke(instance.get());
        }
        catch (Exception ex) {
          throw new IllegalStateException("Unable to get value for property " + this.name, ex);
        }
      };
    }

    boolean isSettable() {
      return this.setter != null;
    }

    void setValue(Supplier<?> instance, Object value) {
      try {
        ReflectionUtils.makeAccessible(setter);
        setter.invoke(instance.get(), value);
      }
      catch (Exception ex) {
        throw new IllegalStateException("Unable to set value for property " + this.name, ex);
      }
    }

  }

}
