/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.aot;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import infra.aot.hint.BindingReflectionHintsRegistrar;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.annotation.ReflectiveProcessor;
import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.beans.BeanUtils;
import infra.beans.support.BeanInstantiator;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.reflect.PropertyAccessor;

/**
 * A {@link ReflectiveProcessor} implementation that registers reflection hints for bean metadata.
 * <p>
 * This processor analyzes classes annotated with {@link RegisterBeanMetadata} and automatically
 * registers the necessary reflection hints for the bean class itself, its constructors, properties,
 * and property accessors (getters/setters/fields). This ensures that bean introspection and
 * instantiation work correctly in native image environments.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see RegisterBeanMetadata
 * @see BeanMetadata
 * @since 5.0 2026/4/7 13:21
 */
class BeanMetadataReflectiveProcessor implements ReflectiveProcessor {

  private static final Logger log = LoggerFactory.getLogger(BeanMetadataReflectiveProcessor.class);

  private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

  @Override
  public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    if (!(element instanceof Class<?> beanClass)) {
      log.warn("@RegisterBeanMetadata can only be applied to classes, but was found on: {}", element);
      return;
    }

    if (Modifier.isAbstract(beanClass.getModifiers())) {
      return;
    }

    log.debug("Registering bean metadata reflection hints for: [{}]", beanClass.getName());
    registerBeanClassHints(hints, beanClass);
    registerBeanPropertiesHints(hints, beanClass);
  }

  /**
   * Register reflection hints for the bean class itself.
   */
  private void registerBeanClassHints(ReflectionHints hints, Class<?> beanClass) {
    bindingRegistrar.registerReflectionHints(hints, beanClass);

    BeanMetadata beanMetadata = BeanMetadata.forClass(beanClass);
    Constructor<?> constructor = beanMetadata.getInstantiator().getConstructor();
    if (constructor != null) {
      hints.registerConstructor(constructor, ExecutableMode.INVOKE);
      log.trace("Registered constructor hint: {}", constructor);
    }
  }

  /**
   * Register reflection hints for all bean properties.
   */
  private void registerBeanPropertiesHints(ReflectionHints hints, Class<?> beanClass) {
    BeanMetadata beanMetadata = BeanMetadata.forClass(beanClass);

    for (BeanProperty property : beanMetadata.beanProperties()) {
      registerPropertyTypeHints(hints, property);
      registerPropertyAccessorsHints(hints, property);
    }
  }

  /**
   * Register reflection hints for property types.
   */
  private void registerPropertyTypeHints(ReflectionHints hints, BeanProperty property) {
    Class<?> propertyType = property.getType();

    bindingRegistrar.registerReflectionHints(hints, property.getResolvableType().getType());

    if (propertyType != Optional.class && !BeanUtils.isSimpleValueType(propertyType)) {
      BeanInstantiator instantiator = BeanInstantiator.forConstructor(propertyType);
      Constructor<?> constructor = instantiator.getConstructor();
      if (constructor != null) {
        hints.registerConstructor(constructor, ExecutableMode.INVOKE);
        log.trace("Registered property type constructor hint: {} for property '{}'",
                constructor, property.getName());
      }
    }
  }

  /**
   * Register reflection hints for property accessors (getters/setters/fields).
   */
  private void registerPropertyAccessorsHints(ReflectionHints hints, BeanProperty property) {
    PropertyAccessor accessor = property.accessor();

    Method readMethod = accessor.getReadMethod();
    if (readMethod != null) {
      hints.registerMethod(readMethod, ExecutableMode.INVOKE);
      log.trace("Registered read method hint: {}", readMethod);
    }

    Method writeMethod = accessor.getWriteMethod();
    if (writeMethod != null) {
      hints.registerMethod(writeMethod, ExecutableMode.INVOKE);
      log.trace("Registered write method hint: {}", writeMethod);
    }

    Field field = property.getField();
    if (field != null) {
      hints.registerField(field);
      log.trace("Registered field hint: {}", field);
    }

    hints.registerType(accessor.getClass(), MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
  }

}
