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

package infra.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.util.Set;

import infra.beans.BeansException;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * A {@link BeanFactoryPostProcessor}
 * implementation that allows for convenient registration of custom autowire
 * qualifier types.
 *
 * <pre class="code">
 * &lt;bean id="customAutowireConfigurer" class="infra.beans.factory.annotation.CustomAutowireConfigurer"&gt;
 *   &lt;property name="customQualifierTypes"&gt;
 *     &lt;set&gt;
 *       &lt;value&gt;mypackage.MyQualifier&lt;/value&gt;
 *     &lt;/set&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Qualifier
 * @since 4.0 2022/1/1 18:00
 */
public class CustomAutowireConfigurer
        extends OrderedSupport implements BeanFactoryPostProcessor, BeanClassLoaderAware, Ordered {

  @Nullable
  private Set<?> customQualifierTypes;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Override
  public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  /**
   * Register custom qualifier annotation types to be considered
   * when autowiring beans. Each element of the provided set may
   * be either a Class instance or a String representation of the
   * fully-qualified class name of the custom annotation.
   * <p>Note that any annotation that is itself annotated with Framework's
   * {@link Qualifier} does not require explicit registration.
   *
   * @param customQualifierTypes the custom types to register
   */
  public void setCustomQualifierTypes(Set<?> customQualifierTypes) {
    this.customQualifierTypes = customQualifierTypes;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    if (this.customQualifierTypes != null) {
      StandardBeanFactory factory = beanFactory.unwrap(StandardBeanFactory.class);
      if (!(factory.getAutowireCandidateResolver() instanceof QualifierAnnotationAutowireCandidateResolver)) {
        factory.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());
      }
      QualifierAnnotationAutowireCandidateResolver resolver =
              (QualifierAnnotationAutowireCandidateResolver) factory.getAutowireCandidateResolver();
      for (Object value : this.customQualifierTypes) {
        Class<? extends Annotation> customType;
        if (value instanceof Class) {
          customType = (Class<? extends Annotation>) value;
        }
        else if (value instanceof String className) {
          customType = ClassUtils.resolveClassName(className, this.beanClassLoader);
        }
        else {
          throw new IllegalArgumentException(
                  "Invalid value [" + value + "] for custom qualifier type: needs to be Class or String.");
        }
        if (!Annotation.class.isAssignableFrom(customType)) {
          throw new IllegalArgumentException(
                  "Qualifier type [" + customType.getName() + "] needs to be annotation type");
        }
        resolver.addQualifierType(customType);
      }
    }
  }

}
