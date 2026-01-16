/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.jackson;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.app.jackson.JacksonComponent.Scope;
import infra.beans.BeanUtils;
import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.HierarchicalBeanFactory;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.lang.Assert;
import infra.util.ObjectUtils;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Bean and Jackson {@link Module} to register {@link JacksonComponent @JsonComponent}
 * annotated beans.
 *
 * @author Phillip Webb
 * @author Paul Aly
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JacksonComponent
 * @since 4.0
 */
@SuppressWarnings("NullAway")
public class JacksonComponentModule extends SimpleModule implements BeanFactoryAware, InitializingBean {

  @SuppressWarnings("NullAway.Init")
  private BeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public void afterPropertiesSet() {
    registerJacksonComponents();
  }

  public void registerJacksonComponents() {
    BeanFactory beanFactory = this.beanFactory;
    while (beanFactory != null) {
      addJacksonComponentBeans(beanFactory);
      beanFactory = (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory)
              ? hierarchicalBeanFactory.getParentBeanFactory() : null;
    }
  }

  private void addJacksonComponentBeans(BeanFactory beanFactory) {
    Map<String, Object> beans = beanFactory.getBeansWithAnnotation(JacksonComponent.class);
    for (Object bean : beans.values()) {
      addJacksonComponentBean(bean);
    }
  }

  private void addJacksonComponentBean(Object bean) {
    MergedAnnotation<JacksonComponent> annotation = MergedAnnotations
            .from(bean.getClass(), SearchStrategy.TYPE_HIERARCHY)
            .get(JacksonComponent.class);
    Class<?>[] types = annotation.getClassArray("type");
    Scope scope = annotation.getEnum("scope", Scope.class);
    addJacksonComponentBean(bean, types, scope);
  }

  private void addJacksonComponentBean(Object bean, Class<?>[] types, Scope scope) {
    if (bean instanceof ValueSerializer<?> jsonSerializer) {
      addValueSerializerBean(jsonSerializer, scope, types);
    }
    else if (bean instanceof ValueDeserializer<?> jsonDeserializer) {
      addValueDeserializerBean(jsonDeserializer, types);
    }
    else if (bean instanceof KeyDeserializer keyDeserializer) {
      addKeyDeserializerBean(keyDeserializer, types);
    }
    for (Class<?> innerClass : bean.getClass().getDeclaredClasses()) {
      if (isSuitableInnerClass(innerClass)) {
        Object innerInstance = BeanUtils.newInstance(innerClass);
        addJacksonComponentBean(innerInstance, types, scope);
      }
    }
  }

  private static boolean isSuitableInnerClass(Class<?> innerClass) {
    return !Modifier.isAbstract(innerClass.getModifiers())
            && (
            ValueSerializer.class.isAssignableFrom(innerClass)
                    || ValueDeserializer.class.isAssignableFrom(innerClass)
                    || KeyDeserializer.class.isAssignableFrom(innerClass));
  }

  @SuppressWarnings("unchecked")
  private <T> void addValueSerializerBean(ValueSerializer<T> serializer, Scope scope, Class<?>[] types) {
    Class<T> baseType = (Class<T>) ResolvableType.forClass(ValueSerializer.class, serializer.getClass()).resolveGeneric();
    Assert.state(baseType != null, "'baseType' is required");
    addBeanToModule(serializer, baseType, types,
            (scope == Scope.VALUES) ? this::addSerializer : this::addKeySerializer);
  }

  @SuppressWarnings("unchecked")
  private <T> void addValueDeserializerBean(ValueDeserializer<T> deserializer, Class<?>[] types) {
    Class<T> baseType = (Class<T>) ResolvableType.forClass(ValueDeserializer.class, deserializer.getClass()).resolveGeneric();
    Assert.state(baseType != null, "'baseType' is required");
    addBeanToModule(deserializer, baseType, types, this::addDeserializer);
  }

  private void addKeyDeserializerBean(KeyDeserializer deserializer, Class<?>[] types) {
    Assert.notEmpty(types, "'types' must not be empty");
    addBeanToModule(deserializer, Object.class, types, this::addKeyDeserializer);
  }

  @SuppressWarnings("unchecked")
  private <E, T> void addBeanToModule(E element, Class<T> baseType, Class<?>[] types,
          BiConsumer<Class<T>, E> consumer) {
    if (ObjectUtils.isEmpty(types)) {
      consumer.accept(baseType, element);
      return;
    }
    for (Class<?> type : types) {
      Assert.isAssignable(baseType, type);
      consumer.accept((Class<T>) type, element);
    }
  }

  static class JacksonComponentBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      String[] jacksonComponents = beanFactory.getBeanNamesForAnnotation(JacksonComponent.class);
      Map<Class<?>, List<Class<?>>> innerComponents = new HashMap<>();
      for (String jacksonComponent : jacksonComponents) {
        Class<?> type = beanFactory.getType(jacksonComponent, true);
        Assert.state(type != null, "'type' is required");
        for (Class<?> declaredClass : type.getDeclaredClasses()) {
          if (isSuitableInnerClass(declaredClass)) {
            innerComponents.computeIfAbsent(type, (t) -> new ArrayList<>()).add(declaredClass);
          }
        }
      }
      return innerComponents.isEmpty() ? null : new JacksonComponentAotContribution(innerComponents);
    }

  }

  private static final class JacksonComponentAotContribution implements BeanFactoryInitializationAotContribution {

    private final Map<Class<?>, List<Class<?>>> innerComponents;

    private JacksonComponentAotContribution(Map<Class<?>, List<Class<?>>> innerComponents) {
      this.innerComponents = innerComponents;
    }

    @Override
    public void applyTo(GenerationContext generationContext,
            BeanFactoryInitializationCode beanFactoryInitializationCode) {
      ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
      this.innerComponents.forEach((outer, inners) -> {
        reflection.registerType(outer);
        inners.forEach((inner) -> reflection.registerType(inner, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
      });
    }

  }

}
