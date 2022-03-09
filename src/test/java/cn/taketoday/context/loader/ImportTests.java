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
package cn.taketoday.context.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.SingletonBeanRegistry;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Singleton;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY <br>
 * 2019-10-01 22:19
 */
class ImportTests {

  @Import(TEST.class)
  public static class ErrorImportTESTBean {

  }

  @Singleton
  @Import(TEST.class)
  public static class ImportTESTBean {

  }

  @Configuration
  public static class TEST {

    @Singleton
    @Import(TEST.class)
    public Object objTest() {

      return new Object() {
        @PreDestroy
        void destroy() {
          System.err.println("objTest destroy");
        }
      };
    }
  }

  @Configuration
  public static class ImportConfigurationBean {

    @Singleton
    @Import(TEST.class)
    public Object obj() {

      return new Object() {

        @PreDestroy
        void destroy() {
          System.err.println("obj destroy");
        }
      };
    }
  }

  // ImportSelector
  // -------------------------------

  public static class TESTSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      return NO_IMPORTS;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Import({ AopSelector.class, BeanDefinitionRegistrar.class })
  public @interface EnableAop {

    boolean proxyTargetClass() default false;

  }

  static class AopSelector implements AnnotationImportSelector<EnableAop>, BeanFactoryAware {

    private EnableAop enableAop;
    private AnnotationMetadata annotatedMetadata;
    SingletonBeanRegistry registry;

    @Nullable
    @Override
    public String[] selectImports(EnableAop target, AnnotationMetadata annotatedMetadata) {
      this.enableAop = target;
      this.annotatedMetadata = annotatedMetadata;
      registry.registerSingleton(this);
      return NO_IMPORTS;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      registry = beanFactory.unwrap(SingletonBeanRegistry.class);
    }
  }

  @EnableAop(proxyTargetClass = true)
  static class AopConfig {

  }

  static class BeanDefinitionRegistrar implements AnnotationBeanDefinitionRegistrar<EnableAop> {

    private EnableAop enableAop;

    //    @Autowired
    private final ConfigurableApplicationContext context;

    BeanDefinitionRegistrar(ConfigurableApplicationContext context) {
      this.context = context;
    }

    @Override
    public void registerBeanDefinitions(EnableAop enableAop, AnnotationMetadata annotatedMetadata, BootstrapContext context) {
      this.enableAop = enableAop;
      this.context.unwrapFactory(SingletonBeanRegistry.class).registerSingleton(this);
    }

  }

  @Test
  void importConfiguration() throws BeanDefinitionStoreException, ConfigurationException {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.scan("cn.taketoday.context.loader");
      context.register(AopConfig.class);
      context.refresh();

      Assertions.assertTrue(context.containsBeanDefinition("objTest"));
      Assertions.assertFalse(context.containsBeanDefinition(ErrorImportTESTBean.class));
      Assertions.assertTrue(context.containsBeanDefinition(ImportTESTBean.class));
      Assertions.assertTrue(context.containsBeanDefinition(TEST.class));

      AopSelector bean = context.getBean(AopSelector.class);
      BeanDefinitionRegistrar beanDefinitionRegistrar = context.getBean(BeanDefinitionRegistrar.class);

      assertThat(bean.enableAop.proxyTargetClass())
              .isEqualTo(beanDefinitionRegistrar.enableAop.proxyTargetClass())
              .isTrue();

//      BeanDefinition def = context.getBeanDefinition(AopConfig.class);
//      Object attribute = def.getAttribute(ImportAware.ImportAnnotatedMetadata);
//      assertThat(attribute).isEqualTo(bean.annotatedMetadata);

    }
  }

}
