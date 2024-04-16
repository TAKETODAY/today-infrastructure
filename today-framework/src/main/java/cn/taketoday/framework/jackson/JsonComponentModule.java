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

package cn.taketoday.framework.jackson;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.HierarchicalBeanFactory;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationCode;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.framework.jackson.JsonComponent.Scope;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;

/**
 * Bean and Jackson {@link Module} to register {@link JsonComponent @JsonComponent}
 * annotated beans.
 *
 * @author Phillip Webb
 * @author Paul Aly
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JsonComponent
 * @since 4.0
 */
public class JsonComponentModule extends SimpleModule implements BeanFactoryAware, InitializingBean {

  private BeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public void afterPropertiesSet() {
    registerJsonComponents();
  }

  public void registerJsonComponents() {
    BeanFactory beanFactory = this.beanFactory;
    while (beanFactory != null) {
      addJsonBeans(beanFactory);
      beanFactory = beanFactory instanceof HierarchicalBeanFactory hbf
              ? hbf.getParentBeanFactory() : null;
    }
  }

  private void addJsonBeans(BeanFactory beanFactory) {
    Map<String, Object> beans = beanFactory.getBeansWithAnnotation(JsonComponent.class);
    for (Object bean : beans.values()) {
      addJsonBean(bean);
    }
  }

  private void addJsonBean(Object bean) {
    MergedAnnotation<JsonComponent> annotation = MergedAnnotations.from(
            bean.getClass(), SearchStrategy.TYPE_HIERARCHY).get(JsonComponent.class);
    Class<?>[] types = annotation.getClassArray("type");
    Scope scope = annotation.getEnum("scope", Scope.class);
    addJsonBean(bean, types, scope);
  }

  private void addJsonBean(Object bean, Class<?>[] types, Scope scope) {
    if (bean instanceof JsonSerializer) {
      addJsonSerializerBean((JsonSerializer<?>) bean, scope, types);
    }
    else if (bean instanceof JsonDeserializer) {
      addJsonDeserializerBean((JsonDeserializer<?>) bean, types);
    }
    else if (bean instanceof KeyDeserializer) {
      addKeyDeserializerBean((KeyDeserializer) bean, types);
    }
    for (Class<?> innerClass : bean.getClass().getDeclaredClasses()) {
      if (isSuitableInnerClass(innerClass)) {
        Object innerInstance = BeanUtils.newInstance(innerClass);
        addJsonBean(innerInstance, types, scope);
      }
    }
  }

  private static boolean isSuitableInnerClass(Class<?> innerClass) {
    return !Modifier.isAbstract(innerClass.getModifiers())
            && (JsonSerializer.class.isAssignableFrom(innerClass)
            || JsonDeserializer.class.isAssignableFrom(innerClass)
            || KeyDeserializer.class.isAssignableFrom(innerClass));
  }

  @SuppressWarnings("unchecked")
  private <T> void addJsonSerializerBean(JsonSerializer<T> serializer, Scope scope, Class<?>[] types) {
    Class<T> baseType = (Class<T>) ResolvableType.forClass(JsonSerializer.class, serializer.getClass())
            .resolveGeneric();
    addBeanToModule(serializer, baseType, types,
            scope == Scope.VALUES ? this::addSerializer : this::addKeySerializer);

  }

  @SuppressWarnings("unchecked")
  private <T> void addJsonDeserializerBean(JsonDeserializer<T> deserializer, Class<?>[] types) {
    Class<T> baseType = (Class<T>) ResolvableType.forClass(
            JsonDeserializer.class, deserializer.getClass()).resolveGeneric();
    addBeanToModule(deserializer, baseType, types, this::addDeserializer);
  }

  private void addKeyDeserializerBean(KeyDeserializer deserializer, Class<?>[] types) {
    Assert.notEmpty(types, "Type must be specified for KeyDeserializer");
    addBeanToModule(deserializer, Object.class, types, this::addKeyDeserializer);
  }

  @SuppressWarnings("unchecked")
  private <E, T> void addBeanToModule(E element, Class<T> baseType, Class<?>[] types, BiConsumer<Class<T>, E> consumer) {
    if (ObjectUtils.isEmpty(types)) {
      consumer.accept(baseType, element);
      return;
    }
    for (Class<?> type : types) {
      Assert.isAssignable(baseType, type);
      consumer.accept((Class<T>) type, element);
    }
  }

  static class JsonComponentBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      Set<String> jsonComponents = beanFactory.getBeanNamesForAnnotation(JsonComponent.class);
      HashMap<Class<?>, List<Class<?>>> innerComponents = new HashMap<>();
      for (String jsonComponent : jsonComponents) {
        Class<?> type = beanFactory.getType(jsonComponent, true);
        Assert.state(type != null, "Cannot determine JsonComponent bean type");
        for (Class<?> declaredClass : type.getDeclaredClasses()) {
          if (isSuitableInnerClass(declaredClass)) {
            innerComponents.computeIfAbsent(type, (t) -> new ArrayList<>()).add(declaredClass);
          }
        }
      }
      return innerComponents.isEmpty() ? null : new JsonComponentAotContribution(innerComponents);
    }
  }

  private static final class JsonComponentAotContribution implements BeanFactoryInitializationAotContribution {

    private final Map<Class<?>, List<Class<?>>> innerComponents;

    private JsonComponentAotContribution(Map<Class<?>, List<Class<?>>> innerComponents) {
      this.innerComponents = innerComponents;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode code) {
      ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
      this.innerComponents.forEach((outer, inners) -> {
        reflection.registerType(outer, MemberCategory.DECLARED_CLASSES);
        inners.forEach((inner) -> reflection.registerType(inner, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
      });
    }

  }

}
