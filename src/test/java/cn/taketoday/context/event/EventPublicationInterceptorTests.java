/*
 * Copyright 2002-2019 the original author or authors.
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

package cn.taketoday.context.event;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.event.test.TestEvent;
import cn.taketoday.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class EventPublicationInterceptorTests {

  private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

  @Test
  public void testWithNoApplicationEventClassSupplied() {
    EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
    interceptor.setApplicationEventPublisher(this.publisher);
    assertThatIllegalArgumentException().isThrownBy(
            interceptor::afterPropertiesSet);
  }

  @Test
  public void testWithNonApplicationEventClassSupplied() {
    EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
    interceptor.setApplicationEventPublisher(this.publisher);
    assertThatIllegalArgumentException().isThrownBy(() -> {
      interceptor.setApplicationEventClass(getClass());
      interceptor.afterPropertiesSet();
    });
  }

  @Test
  public void testWithAbstractStraightApplicationEventClassSupplied() {
    EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
    interceptor.setApplicationEventPublisher(this.publisher);
    assertThatIllegalArgumentException().isThrownBy(() -> {
      interceptor.setApplicationEventClass(ApplicationEvent.class);
      interceptor.afterPropertiesSet();
    });
  }

  @Test
  public void testWithApplicationEventClassThatDoesntExposeAValidCtor() {
    EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
    interceptor.setApplicationEventPublisher(this.publisher);
    assertThatIllegalArgumentException().isThrownBy(() -> {
      interceptor.setApplicationEventClass(TestEventWithNoValidOneArgObjectCtor.class);
      interceptor.afterPropertiesSet();
    });
  }

  @Test
  public void testExpectedBehavior() {
    TestBean target = new TestBean();
    final TestApplicationListener listener = new TestApplicationListener();

    class TestContext extends StaticApplicationContext {
      @Override
      protected void onRefresh() throws BeansException {
        addApplicationListener(listener);
      }
    }

    StaticApplicationContext ctx = new TestContext();
    PropertyValues pvs = new PropertyValues();
    pvs.add("applicationEventClass", TestEvent.class.getName());
    // should automatically receive applicationEventPublisher reference
    ctx.registerSingleton("publisher", EventPublicationInterceptor.class, pvs);
    ctx.registerSingleton("otherListener", FactoryBeanTestListener.class);
    ctx.refresh();

    EventPublicationInterceptor interceptor =
            (EventPublicationInterceptor) ctx.getBean("publisher");
    ProxyFactory factory = new ProxyFactory(target);
    factory.addAdvice(0, interceptor);

    ITestBean testBean = (ITestBean) factory.getProxy();

    // invoke any method on the advised proxy to see if the interceptor has been invoked
    testBean.getAge();

    // two events: ContextRefreshedEvent and TestEvent
    assertThat(listener.getEventCount() == 2).as("Interceptor must have published 2 events").isTrue();
    TestApplicationListener otherListener = (TestApplicationListener) ctx.getBean("&otherListener");
    assertThat(otherListener.getEventCount() == 2).as("Interceptor must have published 2 events").isTrue();
  }

  @SuppressWarnings("serial")
  public static final class TestEventWithNoValidOneArgObjectCtor extends ApplicationEvent {

    public TestEventWithNoValidOneArgObjectCtor() {
      super("");
    }
  }

  public static class FactoryBeanTestListener extends TestApplicationListener implements FactoryBean<Object> {

    @Override
    public Object getObject() {
      return "test";
    }

    @Override
    public Class<String> getObjectType() {
      return String.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

}
