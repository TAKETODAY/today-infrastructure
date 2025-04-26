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