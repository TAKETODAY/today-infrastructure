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
package cn.taketoday.scripting.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.target.dynamic.AbstractRefreshableTargetSource;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Dave Syer
 */
@SuppressWarnings("resource")
public class ScriptingDefaultsTests {

  private static final String CONFIG =
          "org/springframework/scripting/config/scriptingDefaultsTests.xml";

  private static final String PROXY_CONFIG =
          "org/springframework/scripting/config/scriptingDefaultsProxyTargetClassTests.xml";

  @Test
  public void defaultRefreshCheckDelay() throws Exception {
    ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
    Advised advised = (Advised) context.getBean("testBean");
    AbstractRefreshableTargetSource targetSource =
            ((AbstractRefreshableTargetSource) advised.getTargetSource());
    Field field = AbstractRefreshableTargetSource.class.getDeclaredField("refreshCheckDelay");
    field.setAccessible(true);
    long delay = ((Long) field.get(targetSource)).longValue();
    assertThat(delay).isEqualTo(5000L);
  }

  @Test
  public void defaultInitMethod() {
    ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
    ITestBean testBean = (ITestBean) context.getBean("testBean");
    assertThat(testBean.isInitialized()).isTrue();
  }

  @Test
  public void nameAsAlias() {
    ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
    ITestBean testBean = (ITestBean) context.getBean("/url");
    assertThat(testBean.isInitialized()).isTrue();
  }

  @Test
  public void defaultDestroyMethod() {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
    ITestBean testBean = (ITestBean) context.getBean("nonRefreshableTestBean");
    assertThat(testBean.isDestroyed()).isFalse();
    context.close();
    assertThat(testBean.isDestroyed()).isTrue();
  }

  @Test
  public void defaultAutowire() {
    ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
    ITestBean testBean = (ITestBean) context.getBean("testBean");
    ITestBean otherBean = (ITestBean) context.getBean("otherBean");
    assertThat(testBean.getOtherBean()).isEqualTo(otherBean);
  }

  @Test
  public void defaultProxyTargetClass() {
    ApplicationContext context = new ClassPathXmlApplicationContext(PROXY_CONFIG);
    Object testBean = context.getBean("testBean");
    assertThat(AopUtils.isCglibProxy(testBean)).isTrue();
  }

}
