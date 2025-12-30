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

package infra.context.support;

import org.junit.jupiter.api.Test;

import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.context.ApplicationListener;
import infra.context.event.ContextRefreshedEvent;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
class ApplicationContextLifecycleTests {

  @Test
  void beansStart() {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());

    context.start();
    LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
    LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
    LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
    LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
    String error = "bean was not started";
    assertThat(bean1.isRunning()).as(error).isTrue();
    assertThat(bean2.isRunning()).as(error).isTrue();
    assertThat(bean3.isRunning()).as(error).isTrue();
    assertThat(bean4.isRunning()).as(error).isTrue();

    context.close();
  }

  @Test
  void beansStop() {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());

    context.start();
    LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
    LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
    LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
    LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
    String startError = "bean was not started";
    assertThat(bean1.isRunning()).as(startError).isTrue();
    assertThat(bean2.isRunning()).as(startError).isTrue();
    assertThat(bean3.isRunning()).as(startError).isTrue();
    assertThat(bean4.isRunning()).as(startError).isTrue();

    context.stop();
    String stopError = "bean was not stopped";
    assertThat(bean1.isRunning()).as(stopError).isFalse();
    assertThat(bean2.isRunning()).as(stopError).isFalse();
    assertThat(bean3.isRunning()).as(stopError).isFalse();
    assertThat(bean4.isRunning()).as(stopError).isFalse();

    context.close();
  }

  @Test
  void startOrder() {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());

    context.start();
    LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
    LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
    LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
    LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
    String notStartedError = "bean was not started";
    assertThat(bean1.getStartOrder()).as(notStartedError).isGreaterThan(0);
    assertThat(bean2.getStartOrder()).as(notStartedError).isGreaterThan(0);
    assertThat(bean3.getStartOrder()).as(notStartedError).isGreaterThan(0);
    assertThat(bean4.getStartOrder()).as(notStartedError).isGreaterThan(0);
    String orderError = "dependent bean must start after the bean it depends on";
    assertThat(bean2.getStartOrder()).as(orderError).isGreaterThan(bean1.getStartOrder());
    assertThat(bean3.getStartOrder()).as(orderError).isGreaterThan(bean2.getStartOrder());
    assertThat(bean4.getStartOrder()).as(orderError).isGreaterThan(bean2.getStartOrder());

    context.close();
  }

  @Test
  void stopOrder() {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());

    context.start();
    context.stop();
    LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
    LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
    LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
    LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
    String notStoppedError = "bean was not stopped";
    assertThat(bean1.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean2.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean3.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean4.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    String orderError = "dependent bean must stop before the bean it depends on";
    assertThat(bean2.getStopOrder()).as(orderError).isLessThan(bean1.getStopOrder());
    assertThat(bean3.getStopOrder()).as(orderError).isLessThan(bean2.getStopOrder());
    assertThat(bean4.getStopOrder()).as(orderError).isLessThan(bean2.getStopOrder());

    context.close();
  }

  @Test
  void autoStartup() {
    GenericApplicationContext context = new GenericApplicationContext();
    new XmlBeanDefinitionReader(context).loadBeanDefinitions(new ClassPathResource("smartLifecycleTests.xml", getClass()));

    context.refresh();
    LifecycleTestBean bean1 = (LifecycleTestBean) context.getBeanFactory().getBean("bean1");
    LifecycleTestBean bean2 = (LifecycleTestBean) context.getBeanFactory().getBean("bean2");
    LifecycleTestBean bean3 = (LifecycleTestBean) context.getBeanFactory().getBean("bean3");
    LifecycleTestBean bean4 = (LifecycleTestBean) context.getBeanFactory().getBean("bean4");

    context.close();
    String notStoppedError = "bean was not stopped";
    assertThat(bean1.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean2.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean3.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean4.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    String orderError = "dependent bean must stop before the bean it depends on";
    assertThat(bean2.getStopOrder()).as(orderError).isLessThan(bean1.getStopOrder());
    assertThat(bean3.getStopOrder()).as(orderError).isLessThan(bean2.getStopOrder());
    assertThat(bean4.getStopOrder()).as(orderError).isLessThan(bean2.getStopOrder());
  }

  @Test
  void cancelledRefresh() {
    GenericApplicationContext context = new GenericApplicationContext();
    new XmlBeanDefinitionReader(context).loadBeanDefinitions(new ClassPathResource("smartLifecycleTests.xml", getClass()));
    context.registerBean(FailingContextRefreshedListener.class);
    LifecycleTestBean bean1 = (LifecycleTestBean) context.getBeanFactory().getBean("bean1");
    LifecycleTestBean bean2 = (LifecycleTestBean) context.getBeanFactory().getBean("bean2");
    LifecycleTestBean bean3 = (LifecycleTestBean) context.getBeanFactory().getBean("bean3");
    LifecycleTestBean bean4 = (LifecycleTestBean) context.getBeanFactory().getBean("bean4");

    assertThatIllegalStateException().isThrownBy(context::refresh);
    String notStoppedError = "bean was not stopped";
    assertThat(bean1.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean2.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean3.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    assertThat(bean4.getStopOrder()).as(notStoppedError).isGreaterThan(0);
    String orderError = "dependent bean must stop before the bean it depends on";
    assertThat(bean2.getStopOrder()).as(orderError).isLessThan(bean1.getStopOrder());
    assertThat(bean3.getStopOrder()).as(orderError).isLessThan(bean2.getStopOrder());
    assertThat(bean4.getStopOrder()).as(orderError).isLessThan(bean2.getStopOrder());
  }

  private static class FailingContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {

    public void onApplicationEvent(ContextRefreshedEvent event) {
      throw new IllegalStateException();
    }
  }

}
