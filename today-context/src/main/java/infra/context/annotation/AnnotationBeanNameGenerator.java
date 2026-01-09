/*
 * Copyright 2002-present the original author or authors.
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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanNameHolder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanNameGenerator;
import infra.core.annotation.AliasFor;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotationMetadata;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.stereotype.Repository;
import infra.stereotype.Service;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * {@link BeanNameGenerator} implementation for bean classes annotated with the
 * {@link Component @Component} annotation or
 * with another annotation that is itself annotated with {@code @Component} as a
 * meta-annotation. For example, Framework's stereotype annotations (such as
 * {@link Repository @Repository}) are
 * themselves annotated with {@code @Component}.
 *
 * <p>Also supports Jakarta EE's {@link jakarta.annotation.ManagedBean} and
 * JSR-330's {@link jakarta.inject.Named} annotations (as well as their pre-Jakarta
 * {@code javax.annotation.ManagedBean} and {@code javax.inject.Named} equivalents),
 * if available. Note that Infra component annotations always override such
 * standard annotations.
 *
 * <p>If the annotation's value doesn't indicate a bean name, an appropriate
 * name will be built based on the short name of the class (with the first
 * letter lower-cased), unless the first two letters are uppercase. For example:
 *
 * <pre class="code">com.xyz.FooServiceImpl -&gt; fooServiceImpl</pre>
 * <pre class="code">com.xyz.URLFooServiceImpl -&gt; URLFooServiceImpl</pre>
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Component#value()
 * @see Repository#value()
 * @see Service#value()
 * @see jakarta.inject.Named#value()
 * @see FullyQualifiedAnnotationBeanNameGenerator
 * @since 4.0
 */
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

  private static final Logger logger = LoggerFactory.getLogger(AnnotationBeanNameGenerator.class);

  /**
   * A convenient constant for a default {@code AnnotationBeanNameGenerator} instance,
   * as used for component scanning purposes.
   */
  public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

  private static final String COMPONENT_ANNOTATION_CLASSNAME = Component.class.getName();

  private final ConcurrentHashMap<String, Set<String>> metaAnnotationTypesCache = new ConcurrentHashMap<>();

  /**
   * Set used to track which stereotype annotations have already been checked
   * to see if they use a convention-based override for the {@code value}
   * attribute in {@code @Component}.
   *
   * @see #determineBeanNameFromAnnotation(AnnotatedBeanDefinition)
   */
  private static final Set<String> conventionBasedStereotypeCheckCache = ConcurrentHashMap.newKeySet();

  @Override
  public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
    if (definition instanceof AnnotatedBeanDefinition) {
      String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
      if (StringUtils.hasText(beanName)) {
        // Explicit bean name found.
        return beanName;
      }
    }
    // Fallback: generate a unique default bean name.
    return buildDefaultBeanName(definition, registry);
  }

  /**
   * Derive a bean name from one of the annotations on the class.
   *
   * @param annotatedDef the annotation-aware bean definition
   * @return the bean name, or {@code null} if none is found
   */
  @Nullable
  protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
    AnnotationMetadata metadata = annotatedDef.getMetadata();

    BeanNameHolder holder = getExplicitBeanName(metadata);
    if (holder != null) {
      annotatedDef.setAttribute(BeanNameHolder.AttributeName, holder);
      return holder.getBeanName();
    }

    String beanName = null;
    for (String annotationType : metadata.getAnnotationTypes()) {
      MergedAnnotation<Annotation> annotation = metadata.getAnnotation(annotationType);
      if (annotation.isPresent()) {
        Set<String> metaAnnotationTypes = metaAnnotationTypesCache.computeIfAbsent(annotationType, key -> {
          Set<String> result = metadata.getMetaAnnotationTypes(key);
          return result.isEmpty() ? Collections.emptySet() : result;
        });
        if (isStereotype(annotationType, metaAnnotationTypes)) {
          BeanNameHolder beanNameHolder = getBeanNameHolder(annotation);
          if (beanNameHolder != null) {
            String currentName = beanNameHolder.getBeanName();
            if (!currentName.isBlank()) {
              if (conventionBasedStereotypeCheckCache.add(annotationType)
                      && metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME) && logger.isWarnEnabled()) {
                logger.warn("""
                        Support for convention-based stereotype names is deprecated and will \
                        be removed in a future version of the framework. Please annotate the \
                        'value' attribute in @{} with @AliasFor(annotation=Component.class) \
                        to declare an explicit alias for @Component's 'value' attribute.""", annotationType);
              }

              if (beanName != null && !currentName.equals(beanName)) {
                throw new IllegalStateException("Stereotype annotations suggest inconsistent component names: '%s' versus '%s'"
                        .formatted(beanName, currentName));
              }
              annotatedDef.setAttribute(BeanNameHolder.AttributeName, beanNameHolder);
              beanName = currentName;
            }
          }
        }
      }
    }
    return beanName;
  }

  @Nullable
  private BeanNameHolder getBeanNameHolder(MergedAnnotation<?> annotation) {
    Object attribute = annotation.getValue(MergedAnnotation.VALUE);
    if (attribute != null) {
      if (attribute instanceof String beanName) {
        return new BeanNameHolder(beanName, null);
      }
      else if (attribute instanceof String[] nameArray && ObjectUtils.isNotEmpty(nameArray)) {
        String beanName = nameArray[0];
        String[] aliasesArray = null;
        if (nameArray.length > 1) {
          ArrayList<String> aliases = new ArrayList<>();
          for (int i = 1; i < nameArray.length; i++) {
            if (StringUtils.hasText(nameArray[i])) {
              aliases.add(nameArray[i]);
            }
          }
          if (!aliases.isEmpty()) {
            aliasesArray = StringUtils.toStringArray(aliases);
          }
        }

        return new BeanNameHolder(beanName, aliasesArray);
      }
    }

    return null;
  }

  /**
   * Get the explicit bean name for the underlying class, as configured via
   * {@link Component @Component} and taking into
   * account {@link AliasFor @AliasFor}
   * semantics for annotation attribute overrides for {@code @Component}'s
   * {@code value} attribute.
   *
   * @param metadata the {@link AnnotationMetadata} for the underlying class
   * @return the explicit bean name, or {@code null} if not found
   * @see Component#value()
   */
  @Nullable
  private BeanNameHolder getExplicitBeanName(AnnotationMetadata metadata) {
    List<BeanNameHolder> names = metadata.getAnnotations().stream(COMPONENT_ANNOTATION_CLASSNAME)
            .map(this::getBeanNameHolder)
            .filter(Objects::nonNull)
            .filter(holder -> StringUtils.hasText(holder.getBeanName()))
            .distinct()
            .toList();

    if (names.size() == 1) {
      return names.get(0);
    }
    if (names.size() > 1) {
      throw new IllegalStateException(
              "Stereotype annotations suggest inconsistent component names: " + names.stream().map(BeanNameHolder::getBeanName).toList());
    }
    return null;
  }

  /**
   * Check whether the given annotation is a stereotype that is allowed
   * to suggest a component name through its annotation {@code value()}.
   *
   * @param annotationType the name of the annotation class to check
   * @param metaAnnotationTypes the names of meta-annotations on the given annotation
   * @return whether the annotation qualifies as a stereotype with component name
   */
  protected boolean isStereotype(String annotationType, Set<String> metaAnnotationTypes) {
    return metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME)
            || annotationType.equals("jakarta.annotation.ManagedBean")
            || annotationType.equals("jakarta.inject.Named")
            || annotationType.equals("javax.annotation.ManagedBean")
            || annotationType.equals("javax.inject.Named");
  }

  /**
   * Derive a default bean name from the given bean definition.
   * <p>The default implementation delegates to {@link #buildDefaultBeanName(BeanDefinition)}.
   *
   * @param definition the bean definition to build a bean name for
   * @param registry the registry that the given bean definition is being registered with
   * @return the default bean name (never {@code null})
   */
  protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
    return buildDefaultBeanName(definition);
  }

  /**
   * Derive a default bean name from the given bean definition.
   * <p>The default implementation simply builds a decapitalized version
   * of the short class name: e.g. "mypackage.MyJdbcDao" &rarr; "myJdbcDao".
   * <p>Note that inner classes will thus have names of the form
   * "outerClassName.InnerClassName", which because of the period in the
   * name may be an issue if you are autowiring by name.
   *
   * @param definition the bean definition to build a bean name for
   * @return the default bean name (never {@code null})
   */
  protected String buildDefaultBeanName(BeanDefinition definition) {
    String beanClassName = definition.getBeanClassName();
    Assert.state(beanClassName != null, "No bean class name set");
    String shortClassName = ClassUtils.getShortName(beanClassName);
    return StringUtils.uncapitalizeAsProperty(shortClassName);
  }

}
