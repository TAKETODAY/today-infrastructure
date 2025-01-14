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
package infra.context.index;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.BeansException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.SingletonBeanRegistry;
import infra.context.BootstrapContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.annotation.ImportSelector;
import infra.core.type.AnnotationMetadata;
import infra.stereotype.Singleton;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public String[] selectImports(AnnotationMetadata importMetadata) {
      return NO_IMPORTS;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Import({ AopSelector.class, BeanDefinitionRegistrar.class })
  public @interface EnableAop {

    boolean proxyTargetClass() default false;

  }

  static class AopSelector implements ImportSelector, BeanFactoryAware {

    private EnableAop enableAop;

    SingletonBeanRegistry registry;

    @Override
    public String[] selectImports(AnnotationMetadata importMetadata) {
      this.enableAop = importMetadata.getAnnotation(EnableAop.class).synthesize();
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

  static class BeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    private EnableAop enableAop;

    //    @Autowired
    private final ConfigurableApplicationContext context;

    BeanDefinitionRegistrar(ConfigurableApplicationContext context) {
      this.context = context;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      this.enableAop = importMetadata.getAnnotation(EnableAop.class).synthesize();
      this.context.unwrapFactory(SingletonBeanRegistry.class).registerSingleton(this);
    }
  }

  @Test
  void importConfiguration() throws BeanDefinitionStoreException {

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.scan("infra.context.index");
    context.register(AopConfig.class);
    context.refresh();

    assertTrue(context.containsBeanDefinition("objTest"));
    assertFalse(context.containsBeanDefinition(ErrorImportTESTBean.class));
    assertTrue(context.containsBeanDefinition(ImportTESTBean.class));
    assertTrue(context.containsBeanDefinition(TEST.class));

    AopSelector bean = context.getBean(AopSelector.class);
    BeanDefinitionRegistrar beanDefinitionRegistrar = context.getBean(BeanDefinitionRegistrar.class);

    assertThat(bean.enableAop.proxyTargetClass())
            .isEqualTo(beanDefinitionRegistrar.enableAop.proxyTargetClass())
            .isTrue();

//      BeanDefinition def = context.getBeanDefinition(AopConfig.class);
//      Object attribute = def.getAttribute(ImportAware.ImportAnnotatedMetadata);
//      assertThat(attribute).isEqualTo(bean.annotatedMetadata);

    context.close();
  }

}
