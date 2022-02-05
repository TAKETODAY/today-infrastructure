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
package cn.taketoday.scripting.bsh;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.target.dynamic.Refreshable;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.scripting.Calculator;
import cn.taketoday.scripting.ConfigurableMessenger;
import cn.taketoday.scripting.Messenger;
import cn.taketoday.scripting.ScriptCompilationException;
import cn.taketoday.scripting.ScriptSource;
import cn.taketoday.scripting.TestBeanAwareMessenger;
import cn.taketoday.scripting.support.ScriptFactoryPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class BshScriptFactoryTests {

  @Test
  public void staticScript() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());

    assertThat(Arrays.asList(ctx.getBeanNamesForType(Calculator.class)).contains("calculator")).isTrue();
    assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messenger")).isTrue();

    Calculator calc = (Calculator) ctx.getBean("calculator");
    Messenger messenger = (Messenger) ctx.getBean("messenger");

    boolean condition3 = calc instanceof Refreshable;
    assertThat(condition3).as("Scripted object should not be instance of Refreshable").isFalse();
    boolean condition2 = messenger instanceof Refreshable;
    assertThat(condition2).as("Scripted object should not be instance of Refreshable").isFalse();

    assertThat(calc).isEqualTo(calc);
    assertThat(messenger).isEqualTo(messenger);
    boolean condition1 = !messenger.equals(calc);
    assertThat(condition1).isTrue();
    assertThat(messenger.hashCode() != calc.hashCode()).isTrue();
    boolean condition = !messenger.toString().equals(calc.toString());
    assertThat(condition).isTrue();

    assertThat(calc.add(2, 3)).isEqualTo(5);

    String desiredMessage = "Hello World!";
    assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);

    assertThat(ctx.getBeansOfType(Calculator.class).values().contains(calc)).isTrue();
    assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();
  }

  @Test
  public void staticScriptWithNullReturnValue() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
    assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfig")).isTrue();

    ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfig");
    messenger.setMessage(null);
    assertThat(messenger.getMessage()).isNull();
    assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();
  }

  @Test
  public void staticScriptWithTwoInterfacesSpecified() {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
    assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfigExtra")).isTrue();

    ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfigExtra");
    messenger.setMessage(null);
    assertThat(messenger.getMessage()).isNull();
    assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

    ctx.close();
    assertThat(messenger.getMessage()).isNull();
  }

  @Test
  public void staticWithScriptReturningInstance() {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
    assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerInstance")).isTrue();

    Messenger messenger = (Messenger) ctx.getBean("messengerInstance");
    String desiredMessage = "Hello World!";
    assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
    assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

    ctx.close();
    assertThat(messenger.getMessage()).isNull();
  }

  @Test
  public void staticScriptImplementingInterface() {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
    assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerImpl")).isTrue();

    Messenger messenger = (Messenger) ctx.getBean("messengerImpl");
    String desiredMessage = "Hello World!";
    assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
    assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

    ctx.close();
    assertThat(messenger.getMessage()).isNull();
  }

  @Test
  public void staticPrototypeScript() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
    ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
    ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

    assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
    boolean condition = messenger instanceof Refreshable;
    assertThat(condition).as("Scripted object should not be instance of Refreshable").isFalse();

    assertThat(messenger2).isNotSameAs(messenger);
    assertThat(messenger2.getClass()).isSameAs(messenger.getClass());
    assertThat(messenger.getMessage()).isEqualTo("Hello World!");
    assertThat(messenger2.getMessage()).isEqualTo("Hello World!");
    messenger.setMessage("Bye World!");
    messenger2.setMessage("Byebye World!");
    assertThat(messenger.getMessage()).isEqualTo("Bye World!");
    assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");
  }

  @Test
  public void nonStaticScript() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
    Messenger messenger = (Messenger) ctx.getBean("messenger");

    assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
    boolean condition = messenger instanceof Refreshable;
    assertThat(condition).as("Should be an instance of Refreshable").isTrue();

    String desiredMessage = "Hello World!";
    assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);

    Refreshable refreshable = (Refreshable) messenger;
    refreshable.refresh();

    assertThat(messenger.getMessage()).as("Message is incorrect after refresh").isEqualTo(desiredMessage);
    assertThat(refreshable.getRefreshCount()).as("Incorrect refresh count").isEqualTo(2);
  }

  @Test
  public void nonStaticPrototypeScript() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
    ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
    ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

    assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
    boolean condition = messenger instanceof Refreshable;
    assertThat(condition).as("Should be an instance of Refreshable").isTrue();

    assertThat(messenger.getMessage()).isEqualTo("Hello World!");
    assertThat(messenger2.getMessage()).isEqualTo("Hello World!");
    messenger.setMessage("Bye World!");
    messenger2.setMessage("Byebye World!");
    assertThat(messenger.getMessage()).isEqualTo("Bye World!");
    assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");

    Refreshable refreshable = (Refreshable) messenger;
    refreshable.refresh();

    assertThat(messenger.getMessage()).isEqualTo("Hello World!");
    assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");
    assertThat(refreshable.getRefreshCount()).as("Incorrect refresh count").isEqualTo(2);
  }

  @Test
  public void scriptCompilationException() {
    assertThatExceptionOfType(NestedRuntimeException.class).isThrownBy(() ->
                    new ClassPathXmlApplicationContext("org/springframework/scripting/bsh/bshBrokenContext.xml"))
            .matches(ex -> ex.contains(ScriptCompilationException.class));
  }

  @Test
  public void scriptThatCompilesButIsJustPlainBad() throws IOException {
    ScriptSource script = mock(ScriptSource.class);
    final String badScript = "String getMessage() { throw new IllegalArgumentException(); }";
    given(script.getScriptAsString()).willReturn(badScript);
    given(script.isModified()).willReturn(true);
    BshScriptFactory factory = new BshScriptFactory(
            ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript, Messenger.class);
    assertThatExceptionOfType(BshScriptUtils.BshExecutionException.class).isThrownBy(() -> {
      Messenger messenger = (Messenger) factory.getScriptedObject(script, Messenger.class);
      messenger.getMessage();
    });
  }

  @Test
  public void ctorWithNullScriptSourceLocator() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new BshScriptFactory(null, Messenger.class));
  }

  @Test
  public void ctorWithEmptyScriptSourceLocator() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new BshScriptFactory("", Messenger.class));
  }

  @Test
  public void ctorWithWhitespacedScriptSourceLocator() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new BshScriptFactory("\n   ", Messenger.class));
  }

  @Test
  public void resourceScriptFromTag() {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
    TestBean testBean = (TestBean) ctx.getBean("testBean");

    Collection<String> beanNames = Arrays.asList(ctx.getBeanNamesForType(Messenger.class));
    assertThat(beanNames.contains("messenger")).isTrue();
    assertThat(beanNames.contains("messengerImpl")).isTrue();
    assertThat(beanNames.contains("messengerInstance")).isTrue();

    Messenger messenger = (Messenger) ctx.getBean("messenger");
    assertThat(messenger.getMessage()).isEqualTo("Hello World!");
    boolean condition = messenger instanceof Refreshable;
    assertThat(condition).isFalse();

    Messenger messengerImpl = (Messenger) ctx.getBean("messengerImpl");
    assertThat(messengerImpl.getMessage()).isEqualTo("Hello World!");

    Messenger messengerInstance = (Messenger) ctx.getBean("messengerInstance");
    assertThat(messengerInstance.getMessage()).isEqualTo("Hello World!");

    TestBeanAwareMessenger messengerByType = (TestBeanAwareMessenger) ctx.getBean("messengerByType");
    assertThat(messengerByType.getTestBean()).isEqualTo(testBean);

    TestBeanAwareMessenger messengerByName = (TestBeanAwareMessenger) ctx.getBean("messengerByName");
    assertThat(messengerByName.getTestBean()).isEqualTo(testBean);

    Collection<Messenger> beans = ctx.getBeansOfType(Messenger.class).values();
    assertThat(beans.contains(messenger)).isTrue();
    assertThat(beans.contains(messengerImpl)).isTrue();
    assertThat(beans.contains(messengerInstance)).isTrue();
    assertThat(beans.contains(messengerByType)).isTrue();
    assertThat(beans.contains(messengerByName)).isTrue();

    ctx.close();
    assertThat(messenger.getMessage()).isNull();
    assertThat(messengerImpl.getMessage()).isNull();
    assertThat(messengerInstance.getMessage()).isNull();
  }

  @Test
  public void prototypeScriptFromTag() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
    ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
    ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

    assertThat(messenger2).isNotSameAs(messenger);
    assertThat(messenger2.getClass()).isSameAs(messenger.getClass());
    assertThat(messenger.getMessage()).isEqualTo("Hello World!");
    assertThat(messenger2.getMessage()).isEqualTo("Hello World!");
    messenger.setMessage("Bye World!");
    messenger2.setMessage("Byebye World!");
    assertThat(messenger.getMessage()).isEqualTo("Bye World!");
    assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");
  }

  @Test
  public void inlineScriptFromTag() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
    Calculator calculator = (Calculator) ctx.getBean("calculator");
    assertThat(calculator).isNotNull();
    boolean condition = calculator instanceof Refreshable;
    assertThat(condition).isFalse();
  }

  @Test
  public void refreshableFromTag() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
    Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
    assertThat(messenger.getMessage()).isEqualTo("Hello World!");
    boolean condition = messenger instanceof Refreshable;
    assertThat(condition).as("Messenger should be Refreshable").isTrue();
  }

  @Test
  public void applicationEventListener() {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
    Messenger eventListener = (Messenger) ctx.getBean("eventListener");
    ctx.publishEvent(new MyEvent(ctx));
    assertThat(eventListener.getMessage()).isEqualTo("count=2");
  }

  @SuppressWarnings("serial")
  private static class MyEvent extends ApplicationEvent {

    public MyEvent(Object source) {
      super(source);
    }
  }

}
