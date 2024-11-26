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

package infra.context.event;

import org.junit.jupiter.api.Test;

import infra.aop.framework.ProxyFactory;
import infra.beans.BeansException;
import infra.beans.PropertyValues;
import infra.beans.factory.FactoryBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.ApplicationEvent;
import infra.context.ApplicationEventPublisher;
import infra.context.event.test.TestEvent;
import infra.context.support.StaticApplicationContext;

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
