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

package cn.taketoday.test.context.aot.samples.management;

import java.lang.reflect.Executable;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aot.ApplicationContextAotGenerator;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.annotation.AnnotatedElementUtils;

/**
 * Configuration class that mimics AOT support for child management
 * contexts in
 * {@code org.springframework.boot.actuate.autoconfigure.web.server.ChildManagementContextInitializer}.
 *
 * <p>See <a href="https://github.com/spring-projects/spring-framework/issues/30861">gh-30861</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Configuration
public class ManagementConfiguration {

  @Bean
  static BeanRegistrationAotProcessor beanRegistrationAotProcessor() {
    return registeredBean -> {
      Executable factoryMethod = registeredBean.resolveConstructorOrFactoryMethod();
      // Make AOT contribution for @Managed @Bean methods.
      if (AnnotatedElementUtils.hasAnnotation(factoryMethod, Managed.class)) {
        return new AotContribution(createManagementContext());
      }
      return null;
    };
  }

  private static GenericApplicationContext createManagementContext() {
    GenericApplicationContext managementContext = new GenericApplicationContext();
    managementContext.registerBean(ManagementMessageService.class);
    return managementContext;
  }

  private static class AotContribution implements BeanRegistrationAotContribution {

    private final GenericApplicationContext managementContext;

    AotContribution(GenericApplicationContext managementContext) {
      this.managementContext = managementContext;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      GenerationContext managementGenerationContext = generationContext.withName("Management");
      new ApplicationContextAotGenerator().processAheadOfTime(this.managementContext, managementGenerationContext);
    }

  }

}
