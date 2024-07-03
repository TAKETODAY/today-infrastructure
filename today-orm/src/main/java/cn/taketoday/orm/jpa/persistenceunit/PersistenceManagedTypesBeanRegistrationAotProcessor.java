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

package cn.taketoday.orm.jpa.persistenceunit;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.BindingReflectionHintsRegistrar;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragments;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * {@link BeanRegistrationAotProcessor} implementations for persistence managed
 * types.
 *
 * <p>Allows a {@link PersistenceManagedTypes} to be instantiated at build-time
 * and replaced by a hard-coded list of managed class names and packages.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PersistenceManagedTypesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  private static final boolean jpaPresent = ClassUtils.isPresent("jakarta.persistence.Entity",
          PersistenceManagedTypesBeanRegistrationAotProcessor.class.getClassLoader());

  @Nullable
  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    if (jpaPresent) {
      if (PersistenceManagedTypes.class.isAssignableFrom(registeredBean.getBeanClass())) {
        return BeanRegistrationAotContribution.withCustomCodeFragments(codeFragments ->
                new JpaManagedTypesBeanRegistrationCodeFragments(codeFragments, registeredBean));
      }
    }
    return null;
  }

  private static final class JpaManagedTypesBeanRegistrationCodeFragments extends BeanRegistrationCodeFragmentsDecorator {

    private static final List<Class<? extends Annotation>> CALLBACK_TYPES = List.of(PreUpdate.class,
            PostUpdate.class, PrePersist.class, PostPersist.class, PreRemove.class, PostRemove.class, PostLoad.class);

    private static final ParameterizedTypeName LIST_OF_STRINGS_TYPE = ParameterizedTypeName.get(List.class, String.class);

    private final RegisteredBean registeredBean;

    private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

    private JpaManagedTypesBeanRegistrationCodeFragments(BeanRegistrationCodeFragments codeFragments,
            RegisteredBean registeredBean) {
      super(codeFragments);
      this.registeredBean = registeredBean;
    }

    @Override
    public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode,
            boolean allowDirectSupplierShortcut) {
      PersistenceManagedTypes persistenceManagedTypes = this.registeredBean.getBeanFactory()
              .getBean(this.registeredBean.getBeanName(), PersistenceManagedTypes.class);
      contributeHints(generationContext.getRuntimeHints(),
              this.registeredBean.getBeanFactory().getBeanClassLoader(), persistenceManagedTypes.getManagedClassNames());
      GeneratedMethod generatedMethod = beanRegistrationCode.getMethods()
              .add("getInstance", method -> {
                Class<?> beanType = PersistenceManagedTypes.class;
                method.addJavadoc("Get the bean instance for '$L'.",
                        this.registeredBean.getBeanName());
                method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
                method.returns(beanType);
                method.addStatement("$T managedClassNames = $T.of($L)", LIST_OF_STRINGS_TYPE,
                        List.class, toCodeBlock(persistenceManagedTypes.getManagedClassNames()));
                method.addStatement("$T managedPackages = $T.of($L)", LIST_OF_STRINGS_TYPE,
                        List.class, toCodeBlock(persistenceManagedTypes.getManagedPackages()));
                method.addStatement("return $T.of($L, $L)", beanType, "managedClassNames", "managedPackages");
              });
      return generatedMethod.toMethodReference().toCodeBlock();
    }

    private CodeBlock toCodeBlock(List<String> values) {
      return CodeBlock.join(values.stream().map(value -> CodeBlock.of("$S", value)).toList(), ", ");
    }

    private void contributeHints(RuntimeHints hints, @Nullable ClassLoader classLoader, List<String> managedClassNames) {
      for (String managedClassName : managedClassNames) {
        try {
          Class<?> managedClass = ClassUtils.forName(managedClassName, null);
          this.bindingRegistrar.registerReflectionHints(hints.reflection(), managedClass);
          contributeEntityListenersHints(hints, managedClass);
          contributeIdClassHints(hints, managedClass);
          contributeConverterHints(hints, managedClass);
          contributeCallbackHints(hints, managedClass);
          contributeHibernateHints(hints, classLoader, managedClass);
        }
        catch (ClassNotFoundException ex) {
          throw new IllegalArgumentException("Failed to instantiate the managed class: " + managedClassName, ex);
        }
      }
    }

    private void contributeEntityListenersHints(RuntimeHints hints, Class<?> managedClass) {
      EntityListeners entityListeners = AnnotationUtils.findAnnotation(managedClass, EntityListeners.class);
      if (entityListeners != null) {
        for (Class<?> entityListener : entityListeners.value()) {
          hints.reflection().registerType(entityListener, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
        }
      }
    }

    private void contributeIdClassHints(RuntimeHints hints, Class<?> managedClass) {
      IdClass idClass = AnnotationUtils.findAnnotation(managedClass, IdClass.class);
      if (idClass != null) {
        this.bindingRegistrar.registerReflectionHints(hints.reflection(), idClass.value());
      }
    }

    private void contributeConverterHints(RuntimeHints hints, Class<?> managedClass) {
      Converter converter = AnnotationUtils.findAnnotation(managedClass, Converter.class);
      ReflectionHints reflectionHints = hints.reflection();
      if (converter != null) {
        reflectionHints.registerType(managedClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      }
      Convert convertClassAnnotation = AnnotationUtils.findAnnotation(managedClass, Convert.class);
      if (convertClassAnnotation != null) {
        reflectionHints.registerType(convertClassAnnotation.converter(), MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      }
      ReflectionUtils.doWithFields(managedClass, field -> {
        Convert convertFieldAnnotation = AnnotationUtils.findAnnotation(field, Convert.class);
        if (convertFieldAnnotation != null && convertFieldAnnotation.converter() != void.class) {
          reflectionHints.registerType(convertFieldAnnotation.converter(), MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }
      });
    }

    private void contributeCallbackHints(RuntimeHints hints, Class<?> managedClass) {
      ReflectionHints reflection = hints.reflection();
      ReflectionUtils.doWithMethods(managedClass, method ->
                      reflection.registerMethod(method, ExecutableMode.INVOKE),
              method -> CALLBACK_TYPES.stream().anyMatch(method::isAnnotationPresent));
    }

    private void contributeHibernateHints(RuntimeHints hints, @Nullable ClassLoader classLoader, Class<?> managedClass) {
      ReflectionHints reflection = hints.reflection();

      Class<? extends Annotation> embeddableInstantiatorClass = loadClass("org.hibernate.annotations.EmbeddableInstantiator", classLoader);
      if (embeddableInstantiatorClass != null) {
        registerForReflection(reflection,
                AnnotationUtils.findAnnotation(managedClass, embeddableInstantiatorClass), "value");
        ReflectionUtils.doWithFields(managedClass, field -> {
          registerForReflection(reflection,
                  AnnotationUtils.findAnnotation(field, embeddableInstantiatorClass), "value");
          registerForReflection(reflection,
                  AnnotationUtils.findAnnotation(field.getType(), embeddableInstantiatorClass), "value");
        });
        ReflectionUtils.doWithMethods(managedClass, method -> registerForReflection(reflection,
                AnnotationUtils.findAnnotation(method, embeddableInstantiatorClass), "value"));
      }

      Class<? extends Annotation> valueGenerationTypeClass = loadClass("org.hibernate.annotations.ValueGenerationType", classLoader);
      if (valueGenerationTypeClass != null) {
        ReflectionUtils.doWithFields(managedClass, field -> registerForReflection(reflection,
                AnnotationUtils.findAnnotation(field, valueGenerationTypeClass), "generatedBy"));
        ReflectionUtils.doWithMethods(managedClass, method -> registerForReflection(reflection,
                AnnotationUtils.findAnnotation(method, valueGenerationTypeClass), "generatedBy"));
      }

      Class<? extends Annotation> idGeneratorTypeClass = loadClass("org.hibernate.annotations.IdGeneratorType", classLoader);
      if (idGeneratorTypeClass != null) {
        ReflectionUtils.doWithFields(managedClass, field -> registerForReflection(reflection,
                AnnotationUtils.findAnnotation(field, idGeneratorTypeClass), "value"));
        ReflectionUtils.doWithMethods(managedClass, method -> registerForReflection(reflection,
                AnnotationUtils.findAnnotation(method, idGeneratorTypeClass), "value"));
      }

      Class<? extends Annotation> attributeBinderTypeClass = loadClass("org.hibernate.annotations.AttributeBinderType", classLoader);
      if (attributeBinderTypeClass != null) {
        ReflectionUtils.doWithFields(managedClass, field -> registerForReflection(reflection,
                AnnotationUtils.findAnnotation(field, attributeBinderTypeClass), "binder"));
        ReflectionUtils.doWithMethods(managedClass, method -> registerForReflection(reflection,
                AnnotationUtils.findAnnotation(method, attributeBinderTypeClass), "binder"));
      }
    }

    @Nullable
    private static Class<? extends Annotation> loadClass(String className, @Nullable ClassLoader classLoader) {
      try {
        return ClassUtils.forName(className, classLoader);
      }
      catch (ClassNotFoundException ex) {
        return null;
      }
    }

    @SuppressWarnings("NullAway")
    private void registerForReflection(ReflectionHints reflection, @Nullable Annotation annotation, String attribute) {
      if (annotation == null) {
        return;
      }
      Class<?> embeddableInstantiatorClass = (Class<?>) AnnotationUtils.getAnnotationAttributes(annotation).get(attribute);
      reflection.registerType(embeddableInstantiatorClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
  }
}
