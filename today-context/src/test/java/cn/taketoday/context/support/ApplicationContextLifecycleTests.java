/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
public class ApplicationContextLifecycleTests {

	@Test
	public void testBeansStart() {
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
	}

	@Test
	public void testBeansStop() {
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
	}

	@Test
	public void testStartOrder() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String notStartedError = "bean was not started";
		assertThat(bean1.getStartOrder() > 0).as(notStartedError).isTrue();
		assertThat(bean2.getStartOrder() > 0).as(notStartedError).isTrue();
		assertThat(bean3.getStartOrder() > 0).as(notStartedError).isTrue();
		assertThat(bean4.getStartOrder() > 0).as(notStartedError).isTrue();
		String orderError = "dependent bean must start after the bean it depends on";
		assertThat(bean2.getStartOrder() > bean1.getStartOrder()).as(orderError).isTrue();
		assertThat(bean3.getStartOrder() > bean2.getStartOrder()).as(orderError).isTrue();
		assertThat(bean4.getStartOrder() > bean2.getStartOrder()).as(orderError).isTrue();
	}

	@Test
	public void testStopOrder() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		context.stop();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String notStoppedError = "bean was not stopped";
    assertThat(bean1.getStopOrder() > 0).as(notStoppedError).isTrue();
    assertThat(bean2.getStopOrder() > 0).as(notStoppedError).isTrue();
    assertThat(bean3.getStopOrder() > 0).as(notStoppedError).isTrue();
    assertThat(bean4.getStopOrder() > 0).as(notStoppedError).isTrue();
		String orderError = "dependent bean must stop before the bean it depends on";
    assertThat(bean2.getStopOrder() < bean1.getStopOrder()).as(orderError).isTrue();
    assertThat(bean3.getStopOrder() < bean2.getStopOrder()).as(orderError).isTrue();
    assertThat(bean4.getStopOrder() < bean2.getStopOrder()).as(orderError).isTrue();
	}

}
