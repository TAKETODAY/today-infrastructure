/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.config.json;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.app.test.json.AbstractJsonMarshalTester;
import infra.app.test.json.BasicJsonTester;
import infra.beans.BeansException;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.config.InstantiationAwareBeanPostProcessor;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.annotation.config.AutoConfiguration;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.stereotype.Component;
import infra.stereotype.Prototype;
import infra.test.util.ReflectionTestUtils;
import infra.util.ReflectionUtils;

/**
 * Auto-configuration for Json testers.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @see AutoConfigureJsonTesters
 * @since 5.0
 */
@AutoConfiguration
@ConditionalOnJsonTesters
public final class JsonTestersAutoConfiguration {

  @Component
  static JsonMarshalTestersBeanPostProcessor jsonMarshalTestersBeanPostProcessor() {
    return new JsonMarshalTestersBeanPostProcessor();
  }

  @Prototype
  @ImportRuntimeHints(BasicJsonTesterRuntimeHints.class)
  static FactoryBean<BasicJsonTester> basicJsonTesterFactoryBean() {
    return new JsonTesterFactoryBean<BasicJsonTester, Void>(BasicJsonTester.class, null);
  }

  /**
   * {@link BeanPostProcessor} used to initialize JSON testers.
   */
  static class JsonMarshalTestersBeanPostProcessor implements InstantiationAwareBeanPostProcessor, InitializationBeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      ReflectionUtils.doWithFields(bean.getClass(), (field) -> processField(bean, field));
      return bean;
    }

    private void processField(Object bean, Field field) {
      if (AbstractJsonMarshalTester.class.isAssignableFrom(field.getType())) {
        initializeTester(bean, field, bean.getClass(), ResolvableType.forField(field).getGeneric());
      }
      else if (BasicJsonTester.class.isAssignableFrom(field.getType())) {
        initializeTester(bean, field, bean.getClass());
      }
    }

    private void initializeTester(Object bean, Field field, Object... args) {
      ReflectionUtils.makeAccessible(field);
      Object tester = ReflectionUtils.getField(field, bean);
      if (tester != null) {
        ReflectionTestUtils.invokeMethod(tester, "initialize", args);
      }
    }

  }

  static class BasicJsonTesterRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      ReflectionHints reflection = hints.reflection();
      reflection.registerType(BasicJsonTester.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      Method method = ReflectionUtils.findMethod(BasicJsonTester.class, "initialize", Class.class);
      Assert.state(method != null, "'method' is required");
      reflection.registerMethod(method, ExecutableMode.INVOKE);
    }

  }

}
