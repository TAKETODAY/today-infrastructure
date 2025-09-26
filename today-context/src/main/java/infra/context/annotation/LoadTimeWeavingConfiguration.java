/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.BeanDefinition;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import infra.context.weaving.AspectJWeavingEnabler;
import infra.context.weaving.DefaultContextLoadTimeWeaver;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotationMetadata;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.lang.Assert;
import infra.stereotype.Component;

/**
 * {@code @Configuration} class that registers a {@link LoadTimeWeaver} bean.
 *
 * <p>This configuration class is automatically imported when using the
 * {@link EnableLoadTimeWeaving} annotation. See {@code @EnableLoadTimeWeaving}
 * javadoc for complete usage details.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LoadTimeWeavingConfigurer
 * @see ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class LoadTimeWeavingConfiguration implements ImportAware, BeanClassLoaderAware {

  @Nullable
  private MergedAnnotation<EnableLoadTimeWeaving> enableLTW;

  @Nullable
  private LoadTimeWeavingConfigurer ltwConfigurer;

  @Nullable
  private ClassLoader beanClassLoader;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.enableLTW = importMetadata.getAnnotation(EnableLoadTimeWeaving.class);
    if (!enableLTW.isPresent()) {
      throw new IllegalArgumentException(
              "@EnableLoadTimeWeaving is not present on importing class " + importMetadata.getClassName());
    }
  }

  @Autowired(required = false)
  public void setLoadTimeWeavingConfigurer(LoadTimeWeavingConfigurer ltwConfigurer) {
    this.ltwConfigurer = ltwConfigurer;
  }

  @Override
  public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME)
  public LoadTimeWeaver loadTimeWeaver() {
    Assert.state(this.beanClassLoader != null, "No ClassLoader set");
    LoadTimeWeaver loadTimeWeaver = null;

    if (this.ltwConfigurer != null) {
      // The user has provided a custom LoadTimeWeaver instance
      loadTimeWeaver = this.ltwConfigurer.getLoadTimeWeaver();
    }

    if (loadTimeWeaver == null) {
      // No custom LoadTimeWeaver provided -> fall back to the default
      loadTimeWeaver = new DefaultContextLoadTimeWeaver(this.beanClassLoader);
    }

    if (this.enableLTW != null) {
      AspectJWeaving aspectJWeaving = this.enableLTW.getEnum("aspectjWeaving", AspectJWeaving.class);
      switch (aspectJWeaving) {
        case DISABLED:
          // AJ weaving is disabled -> do nothing
          break;
        case AUTODETECT:
          if (this.beanClassLoader.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) == null) {
            // No aop.xml present on the classpath -> treat as 'disabled'
            break;
          }
          // aop.xml is present on the classpath -> enable
          AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
          break;
        case ENABLED:
          AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
          break;
      }
    }

    return loadTimeWeaver;
  }

}
