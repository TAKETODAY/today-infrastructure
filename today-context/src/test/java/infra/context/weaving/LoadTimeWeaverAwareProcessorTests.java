/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.context.weaving;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.instrument.classloading.LoadTimeWeaver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 19:31
 */
class LoadTimeWeaverAwareProcessorTests {

  @Test
  void processorWithoutLoadTimeWeaverUsesContextBean() {
    LoadTimeWeaver contextWeaver = mock(LoadTimeWeaver.class);
    BeanFactory factory = mock(BeanFactory.class);
    when(factory.getBean(ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class))
            .thenReturn(contextWeaver);

    LoadTimeWeaverAwareProcessor processor = new LoadTimeWeaverAwareProcessor();
    processor.setBeanFactory(factory);

    LoadTimeWeaverAware bean = mock(LoadTimeWeaverAware.class);
    processor.postProcessBeforeInitialization(bean, "testBean");

    verify(bean).setLoadTimeWeaver(contextWeaver);
  }

  @Test
  void processorWithExplicitLoadTimeWeaver() {
    LoadTimeWeaver weaver = mock(LoadTimeWeaver.class);
    LoadTimeWeaverAwareProcessor processor = new LoadTimeWeaverAwareProcessor(weaver);

    LoadTimeWeaverAware bean = mock(LoadTimeWeaverAware.class);
    processor.postProcessBeforeInitialization(bean, "testBean");

    verify(bean).setLoadTimeWeaver(weaver);
  }

  @Test
  void nonLoadTimeWeaverAwareBeanUnmodified() {
    Object bean = new Object();
    LoadTimeWeaverAwareProcessor processor = new LoadTimeWeaverAwareProcessor();

    Object result = processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(result).isSameAs(bean);
  }

  @Test
  void missingBeanFactoryThrowsException() {
    LoadTimeWeaverAwareProcessor processor = new LoadTimeWeaverAwareProcessor();
    LoadTimeWeaverAware bean = mock(LoadTimeWeaverAware.class);

    assertThatIllegalStateException()
            .isThrownBy(() -> processor.postProcessBeforeInitialization(bean, "testBean"))
            .withMessage("BeanFactory required if no LoadTimeWeaver explicitly specified");
  }

  @Test
  void constructorWithBeanFactory() {
    BeanFactory factory = mock(BeanFactory.class);
    LoadTimeWeaver weaver = mock(LoadTimeWeaver.class);
    when(factory.getBean(ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class))
            .thenReturn(weaver);

    LoadTimeWeaverAwareProcessor processor = new LoadTimeWeaverAwareProcessor(factory);
    LoadTimeWeaverAware bean = mock(LoadTimeWeaverAware.class);

    processor.postProcessBeforeInitialization(bean, "testBean");
    verify(bean).setLoadTimeWeaver(weaver);
  }

  @Test
  void afterInitializationReturnsSameBean() {
    LoadTimeWeaverAwareProcessor processor = new LoadTimeWeaverAwareProcessor();
    Object bean = new Object();

    Object result = processor.postProcessAfterInitialization(bean, "testBean");
    assertThat(result).isSameAs(bean);
  }

}