/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.io.Closeable;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class DestroyMethodInferenceTests {

  @Test
  public void beanMethods() {
    ConfigurableApplicationContext ctx = new StandardApplicationContext(Config.class);
    WithExplicitDestroyMethod c0 = ctx.getBean(WithExplicitDestroyMethod.class);
    WithLocalCloseMethod c1 = ctx.getBean("c1", WithLocalCloseMethod.class);
    WithLocalCloseMethod c2 = ctx.getBean("c2", WithLocalCloseMethod.class);
    WithInheritedCloseMethod c3 = ctx.getBean("c3", WithInheritedCloseMethod.class);
    WithInheritedCloseMethod c4 = ctx.getBean("c4", WithInheritedCloseMethod.class);
    WithInheritedCloseMethod c5 = ctx.getBean("c5", WithInheritedCloseMethod.class);
    WithNoCloseMethod c6 = ctx.getBean("c6", WithNoCloseMethod.class);
    WithLocalShutdownMethod c7 = ctx.getBean("c7", WithLocalShutdownMethod.class);
    WithInheritedCloseMethod c8 = ctx.getBean("c8", WithInheritedCloseMethod.class);
    WithDisposableBean c9 = ctx.getBean("c9", WithDisposableBean.class);
    WithAutoCloseable c10 = ctx.getBean("c10", WithAutoCloseable.class);

    assertThat(c0.closed).as("c0").isFalse();
    assertThat(c1.closed).as("c1").isFalse();
    assertThat(c2.closed).as("c2").isFalse();
    assertThat(c3.closed).as("c3").isFalse();
    assertThat(c4.closed).as("c4").isFalse();
    assertThat(c5.closed).as("c5").isFalse();
    assertThat(c6.closed).as("c6").isFalse();
    assertThat(c7.closed).as("c7").isFalse();
    assertThat(c8.closed).as("c8").isFalse();
    assertThat(c9.closed).as("c9").isFalse();
    assertThat(c10.closed).as("c10").isFalse();

    ctx.close();
    assertThat(c0.closed).as("c0").isTrue();
    assertThat(c1.closed).as("c1").isTrue();
    assertThat(c2.closed).as("c2").isTrue();
    assertThat(c3.closed).as("c3").isTrue();
    assertThat(c4.closed).as("c4").isTrue();
    assertThat(c5.closed).as("c5").isTrue();
    assertThat(c6.closed).as("c6").isFalse();
    assertThat(c7.closed).as("c7").isTrue();
    assertThat(c8.closed).as("c8").isFalse();
    assertThat(c9.closed).as("c9").isTrue();
    assertThat(c10.closed).as("c10").isTrue();
  }

  /*
  	<bean id="x1"
	      class="cn.taketoday.context.annotation.DestroyMethodInferenceTests$WithLocalCloseMethod"/>

	<bean id="x2"
	      class="cn.taketoday.context.annotation.DestroyMethodInferenceTests$WithLocalCloseMethod"
	      destroy-method="(inferred)"/>

	<bean id="x8"
		  class="cn.taketoday.context.annotation.DestroyMethodInferenceTests.WithInheritedCloseMethod"
		  destroy-method=""/>

	<bean id="x9"
		  class="cn.taketoday.context.annotation.DestroyMethodInferenceTests.WithDisposableBean"
		  destroy-method=""/>

	<bean id="x10"
		  class="cn.taketoday.context.annotation.DestroyMethodInferenceTests.WithAutoCloseable"/>

	<beans default-destroy-method="(inferred)">
		<bean id="x3"
		      class="cn.taketoday.context.annotation.DestroyMethodInferenceTests$WithLocalCloseMethod"/>
		<bean id="x4"
		      class="cn.taketoday.context.annotation.DestroyMethodInferenceTests$WithNoCloseMethod"/>
	</beans>

*/
  @Test
  public void xml() {
    GenericApplicationContext ctx = new GenericApplicationContext();

    ctx.registerBeanDefinition(new BeanDefinition("x1", WithLocalCloseMethod.class));

    BeanDefinition x21 = new BeanDefinition("x2", WithLocalCloseMethod.class);
    x21.setDestroyMethod(BeanDefinition.INFER_METHOD);
    ctx.registerBeanDefinition(x21);

    BeanDefinition x81 = new BeanDefinition("x8", WithInheritedCloseMethod.class);
    x81.setDestroyMethod("");
    ctx.registerBeanDefinition(x81);

    ctx.registerBeanDefinition(new BeanDefinition("x9", WithDisposableBean.class));
    ctx.registerBeanDefinition(new BeanDefinition("x10", WithAutoCloseable.class));

    BeanDefinition x31 = new BeanDefinition("x3", WithLocalCloseMethod.class);
    x31.setDestroyMethod(BeanDefinition.INFER_METHOD);
    ctx.registerBeanDefinition(x31);

    BeanDefinition x41 = new BeanDefinition("x4", WithNoCloseMethod.class);
    x41.setDestroyMethod(BeanDefinition.INFER_METHOD);

    ctx.registerBeanDefinition(x41);
    ctx.refresh();

    WithLocalCloseMethod x1 = ctx.getBean("x1", WithLocalCloseMethod.class);
    WithLocalCloseMethod x2 = ctx.getBean("x2", WithLocalCloseMethod.class);
    WithLocalCloseMethod x3 = ctx.getBean("x3", WithLocalCloseMethod.class);
    WithNoCloseMethod x4 = ctx.getBean("x4", WithNoCloseMethod.class);
    WithInheritedCloseMethod x8 = ctx.getBean("x8", WithInheritedCloseMethod.class);
    WithDisposableBean x9 = ctx.getBean("x9", WithDisposableBean.class);
    WithAutoCloseable x10 = ctx.getBean("x10", WithAutoCloseable.class);

    assertThat(x1.closed).isFalse();
    assertThat(x2.closed).isFalse();
    assertThat(x3.closed).isFalse();
    assertThat(x4.closed).isFalse();
    assertThat(x8.closed).isFalse();
    assertThat(x9.closed).isFalse();
    assertThat(x10.closed).isFalse();

    ctx.close();
    assertThat(x1.closed).isFalse();
    assertThat(x2.closed).isTrue();
    assertThat(x3.closed).isTrue();
    assertThat(x4.closed).isFalse();
    assertThat(x8.closed).isFalse();
    assertThat(x9.closed).isTrue();
    assertThat(x10.closed).isTrue();
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean(destroyMethod = "explicitClose")
    public WithExplicitDestroyMethod c0() {
      return new WithExplicitDestroyMethod();
    }

    @Bean
    public WithLocalCloseMethod c1() {
      return new WithLocalCloseMethod();
    }

    @Bean
    public Object c2() {
      return new WithLocalCloseMethod();
    }

    @Bean
    public WithInheritedCloseMethod c3() {
      return new WithInheritedCloseMethod();
    }

    @Bean
    public Closeable c4() {
      return new WithInheritedCloseMethod();
    }

    @Bean(destroyMethod = "other")
    public WithInheritedCloseMethod c5() {
      return new WithInheritedCloseMethod() {
        @Override
        public void close() {
          throw new IllegalStateException("close() should not be called");
        }

        @SuppressWarnings("unused")
        public void other() {
          this.closed = true;
        }
      };
    }

    @Bean
    public WithNoCloseMethod c6() {
      return new WithNoCloseMethod();
    }

    @Bean
    public WithLocalShutdownMethod c7() {
      return new WithLocalShutdownMethod();
    }

    @Bean(destroyMethod = "")
    public WithInheritedCloseMethod c8() {
      return new WithInheritedCloseMethod();
    }

    @Bean(destroyMethod = "")
    public WithDisposableBean c9() {
      return new WithDisposableBean();
    }

    @Bean
    public WithAutoCloseable c10() {
      return new WithAutoCloseable();
    }
  }

  static class WithExplicitDestroyMethod {

    boolean closed = false;

    public void explicitClose() {
      closed = true;
    }
  }

  static class WithLocalCloseMethod {

    boolean closed = false;

    public void close() {
      closed = true;
    }
  }

  static class WithInheritedCloseMethod implements Closeable {

    boolean closed = false;

    @Override
    public void close() {
      closed = true;
    }
  }

  static class WithNoCloseMethod {

    boolean closed = false;
  }

  static class WithLocalShutdownMethod {

    boolean closed = false;

    public void shutdown() {
      closed = true;
    }
  }

  static class WithDisposableBean implements DisposableBean {

    boolean closed = false;

    @Override
    public void destroy() {
      closed = true;
    }
  }

  static class WithAutoCloseable implements AutoCloseable {

    boolean closed = false;

    @Override
    public void close() {
      closed = true;
    }
  }

}
