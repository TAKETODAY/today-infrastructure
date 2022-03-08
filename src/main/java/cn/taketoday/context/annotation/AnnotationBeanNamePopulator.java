/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNamePopulator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link BeanNamePopulator} implementation for bean classes annotated with the
 * {@link cn.taketoday.lang.Component @Component} annotation or
 * with another annotation that is itself annotated with {@code @Component} as a
 * meta-annotation. For example, Framework's stereotype annotations (such as
 * {@link cn.taketoday.lang.Repository @Repository}) are
 * themselves annotated with {@code @Component}.
 *
 * <p>Also supports Jakarta EE's {@link jakarta.annotation.ManagedBean} and
 * JSR-330's {@link jakarta.inject.Named} annotations, if available. Note that
 * Framework component annotations always override such standard annotations.
 *
 * <p>If the annotation's value doesn't indicate a bean name, an appropriate
 * name will be built based on the short name of the class (with the first
 * letter lower-cased), unless the two first letters are uppercase. For example:
 *
 * <pre class="code">com.xyz.FooServiceImpl -&gt; fooServiceImpl</pre>
 * <pre class="code">com.xyz.URLFooServiceImpl -&gt; URLFooServiceImpl</pre>
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.lang.Component#value()
 * @see cn.taketoday.lang.Repository#value()
 * @see cn.taketoday.lang.Service#value()
 * @see jakarta.inject.Named#value()
 * @see FullyQualifiedAnnotationBeanNamePopulator
 * @since 4.0
 */
public class AnnotationBeanNamePopulator implements BeanNamePopulator {

  /**
   * A convenient constant for a default {@code AnnotationBeanNamePopulator} instance,
   * as used for component scanning purposes.
   */
  public static final AnnotationBeanNamePopulator INSTANCE = new AnnotationBeanNamePopulator();

  private static final String COMPONENT_ANNOTATION_CLASSNAME = "cn.taketoday.lang.Component";

  @Override
  public String populateName(BeanDefinition definition, BeanDefinitionRegistry registry) {
    if (definition instanceof AnnotatedBeanDefinition) {
      determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
    }
    if (!StringUtils.hasText(definition.getBeanName())) {
      // Fallback: generate a unique default bean name.
      String beanName = buildDefaultBeanName(definition, registry);
      definition.setBeanName(beanName);
    }
    return definition.getBeanName();
  }

  /**
   * Derive a bean name from one of the annotations on the class.
   *
   * @param annotatedDef the annotation-aware bean definition
   */
  protected void determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
    Object attribute = annotatedDef.getAttribute(Component.ANNOTATION);
    if (attribute instanceof MergedAnnotation annotation) {
      apply(annotatedDef, annotation);
    }
    else {
      AnnotationMetadata metadata = annotatedDef.getMetadata();
      Set<String> types = metadata.getAnnotationTypes();
      String beanName = null;
      for (String type : types) {
        MergedAnnotation<Annotation> annotation = metadata.getAnnotation(type);
        if (annotation.isPresent()) {
          Set<String> metaTypes = metadata.getMetaAnnotationTypes(type);
          if (isStereotypeWithNameValue(type, metaTypes, annotation)) {
            String strVal = apply(annotatedDef, annotation);
            if (StringUtils.isNotEmpty(strVal)) {
              if (beanName != null && !strVal.equals(beanName)) {
                throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
                        "component names: '" + beanName + "' versus '" + strVal + "'");
              }
              beanName = strVal;
            }
          }
        }
      }
    }
  }

  private String apply(AnnotatedBeanDefinition annotatedDef, MergedAnnotation<?> annotation) {
    Optional<Object> value = annotation.getValue("value");
    if (value.isPresent()) {
      Object attribute = value.get();
      if (attribute instanceof String beanName) {
        annotatedDef.setBeanName(beanName);
        return beanName;
      }
      else if (attribute instanceof String[] nameArray && ObjectUtils.isNotEmpty(nameArray)) {
        String beanName = nameArray[0];
        annotatedDef.setBeanName(beanName);
        if (nameArray.length > 1) {
          ArrayList<String> aliases = new ArrayList<>();
          for (int i = 1; i < nameArray.length; i++) {
            if (StringUtils.hasText(nameArray[i])) {
              aliases.add(nameArray[i]);
            }
          }
          if (!aliases.isEmpty()) {
            annotatedDef.setAliases(aliases.toArray(Constant.EMPTY_STRING_ARRAY));
          }
        }
        return beanName;
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
   * @param attributes the map of attributes for the given annotation
   * @return whether the annotation qualifies as a stereotype with component name
   */
  protected boolean isStereotypeWithNameValue(
          String annotationType, Set<String> metaAnnotationTypes, MergedAnnotation<Annotation> attributes) {

    boolean isStereotype = annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME)
            || metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME)
            || annotationType.equals("jakarta.annotation.ManagedBean")
            || annotationType.equals("jakarta.inject.Named");

    return isStereotype && attributes.getValue(MergedAnnotation.VALUE).isPresent();
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
    return BeanDefinitionBuilder.defaultBeanName(beanClassName);
  }

}
