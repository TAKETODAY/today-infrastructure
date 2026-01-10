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

package infra.test.context.aot.samples.management;

import java.lang.reflect.Executable;

import infra.aot.generate.GenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.aot.BeanRegistrationCode;
import infra.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.aot.ApplicationContextAotGenerator;
import infra.context.support.GenericApplicationContext;
import infra.core.annotation.AnnotatedElementUtils;

/**
 * Configuration class that mimics AOT support for child management
 * contexts in
 * {@code ChildManagementContextInitializer}.
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
      InstantiationDescriptor instantiationDescriptor = registeredBean.resolveInstantiationDescriptor();
      Executable factoryMethod = instantiationDescriptor.executable();
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
