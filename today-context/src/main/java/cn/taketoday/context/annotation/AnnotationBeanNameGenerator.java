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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanNameHolder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link BeanNameGenerator} implementation for bean classes annotated with the
 * {@link cn.taketoday.stereotype.Component @Component} annotation or
 * with another annotation that is itself annotated with {@code @Component} as a
 * meta-annotation. For example, Framework's stereotype annotations (such as
 * {@link cn.taketoday.stereotype.Repository @Repository}) are
 * themselves annotated with {@code @Component}.
 *
 * <p>Also supports Jakarta EE's {@link jakarta.annotation.ManagedBean} and
 * JSR-330's {@link jakarta.inject.Named} annotations (as well as their pre-Jakarta
 * {@code javax.annotation.ManagedBean} and {@code javax.inject.Named} equivalents),
 * if available. Note that Spring component annotations always override such
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
 * @see cn.taketoday.stereotype.Component#value()
 * @see cn.taketoday.stereotype.Repository#value()
 * @see cn.taketoday.stereotype.Service#value()
 * @see jakarta.inject.Named#value()
 * @see FullyQualifiedAnnotationBeanNameGenerator
 * @since 4.0
 */
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

  /**
   * A convenient constant for a default {@code AnnotationBeanNameGenerator} instance,
   * as used for component scanning purposes.
   */
  public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

  private static final String COMPONENT_ANNOTATION_CLASSNAME = "cn.taketoday.stereotype.Component";

  private final Map<String, Set<String>> metaAnnotationTypesCache = new ConcurrentHashMap<>();

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
    AnnotationMetadata amd = annotatedDef.getMetadata();
    Set<String> types = amd.getAnnotationTypes();
    String beanName = null;
    for (String type : types) {
      MergedAnnotation<Annotation> annotation = amd.getAnnotation(type);
      if (annotation.isPresent()) {
        Set<String> metaTypes = this.metaAnnotationTypesCache.computeIfAbsent(type, key -> {
          Set<String> result = amd.getMetaAnnotationTypes(key);
          return result.isEmpty() ? Collections.emptySet() : result;
        });
        if (isStereotype(type, metaTypes)) {
          BeanNameHolder beanNameHolder = getBeanNameHolder(annotation);
          if (beanNameHolder != null) {
            String strVal = beanNameHolder.getBeanName();
            if (StringUtils.isNotEmpty(strVal)) {
              if (beanName != null && !strVal.equals(beanName)) {
                throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
                        "component names: '" + beanName + "' versus '" + strVal + "'");
              }
              annotatedDef.setAttribute(BeanNameHolder.AttributeName, beanNameHolder);
              beanName = strVal;
            }
          }
        }
      }
    }
    return beanName;
  }

  @Nullable
  private BeanNameHolder getBeanNameHolder(MergedAnnotation<?> annotation) {
    Optional<Object> value = annotation.getValue(MergedAnnotation.VALUE);
    if (value.isPresent()) {
      Object attribute = value.get();
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
            aliasesArray = aliases.toArray(Constant.EMPTY_STRING_ARRAY);
          }
        }

        return new BeanNameHolder(beanName, aliasesArray);
      }
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
    return annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME)
            || metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME)
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
