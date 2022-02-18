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

import java.util.Collection;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.lang.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ensuring that nested static @Configuration classes are automatically detected
 * and registered without the need for explicit registration or @Import. See SPR-8186.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
public class NestedConfigurationClassTests {

  @Test
  public void oneLevelDeep() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(L0Config.L1Config.class);
    ctx.refresh();

    assertThat(ctx.containsBean("l0Bean")).isFalse();

    ctx.getBean(L0Config.L1Config.class);
    ctx.getBean("l1Bean");

    ctx.getBean(L0Config.L1Config.L2Config.class);
    ctx.getBean("l2Bean");

    // ensure that override order is correct
    assertThat(ctx.getBean("overrideBean", TestBean.class).getName()).isEqualTo("override-l1");
  }

  @Test
  public void twoLevelsDeep() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(L0Config.class);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("nestedConfigurationClassTests.L0Config")).isFalse();
    ctx.getBean(L0Config.class);
    ctx.getBean("l0Bean");

    assertThat(ctx.getBeanFactory().containsSingleton(L0Config.L1Config.class.getName())).isTrue();
    L0Config.L1Config bean = ctx.getBean(L0Config.L1Config.class);
    Object l1Bean = ctx.getBean("l1Bean");

    assertThat(ctx.getBeanFactory().containsSingleton(L0Config.L1Config.L2Config.class.getName())).isFalse();
    ctx.getBean(L0Config.L1Config.L2Config.class);
    ctx.getBean("l2Bean");

    // ensure that override order is correct
    assertThat(ctx.getBean("overrideBean", TestBean.class).getName()).isEqualTo("override-l0");
  }

  @Test
  public void twoLevelsInLiteMode() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(L0ConfigLight.class);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("nestedConfigurationClassTests.L0ConfigLight")).isFalse();
    ctx.getBean(L0ConfigLight.class);
    ctx.getBean("l0Bean");

    assertThat(ctx.getBeanFactory().containsSingleton(L0ConfigLight.L1ConfigLight.class.getName())).isTrue();
    ctx.getBean(L0ConfigLight.L1ConfigLight.class);
    ctx.getBean("l1Bean");

    assertThat(ctx.getBeanFactory().containsSingleton(L0ConfigLight.L1ConfigLight.L2ConfigLight.class.getName())).isFalse();
    ctx.getBean(L0ConfigLight.L1ConfigLight.L2ConfigLight.class);
    ctx.getBean("l2Bean");

    // ensure that override order is correct
    assertThat(ctx.getBean("overrideBean", TestBean.class).getName()).isEqualTo("override-l0");
  }

  @Test
  public void twoLevelsDeepWithInheritance() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(S1Config.class);
    ctx.refresh();

    S1Config config = ctx.getBean(S1Config.class);
    assertThat(config != ctx.getBean(S1Config.class)).isTrue();
    TestBean tb = ctx.getBean("l0Bean", TestBean.class);
    assertThat(tb == ctx.getBean("l0Bean", TestBean.class)).isTrue();

    ctx.getBean(L0Config.L1Config.class);
    ctx.getBean("l1Bean");

    ctx.getBean(L0Config.L1Config.L2Config.class);
    ctx.getBean("l2Bean");

    // ensure that override order is correct and that it is a singleton
    TestBean ob = ctx.getBean("overrideBean", TestBean.class);
    assertThat(ob.getName()).isEqualTo("override-s1");
    assertThat(ob == ctx.getBean("overrideBean", TestBean.class)).isTrue();

    TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
    TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
    assertThat(pb1 != pb2).isTrue();
    assertThat(pb1.getFriends().iterator().next() != pb2.getFriends().iterator().next()).isTrue();
  }

  @Test
  public void twoLevelsDeepWithInheritanceThroughImport() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(S1Importer.class);
    ctx.refresh();

    S1Config config = ctx.getBean(S1Config.class);
    assertThat(config != ctx.getBean(S1Config.class)).isTrue();
    TestBean tb = ctx.getBean("l0Bean", TestBean.class);
    TestBean l0Bean = ctx.getBean("l0Bean", TestBean.class);
    assertThat(tb == l0Bean).isTrue();

    ctx.getBean(L0Config.L1Config.class);
    ctx.getBean("l1Bean");

    ctx.getBean(L0Config.L1Config.L2Config.class);
    ctx.getBean("l2Bean");

    // ensure that override order is correct and that it is a singleton
    TestBean ob = ctx.getBean("overrideBean", TestBean.class);
    assertThat(ob.getName()).isEqualTo("override-s1");
    assertThat(ob == ctx.getBean("overrideBean", TestBean.class)).isTrue();

    TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
    TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
    assertThat(pb1 != pb2).isTrue();
    Collection<? super Object> friends = pb1.getFriends();
    Collection<? super Object> friends1 = pb2.getFriends();
    assertThat(friends.iterator().next() != friends1.iterator().next()).isTrue();
  }

  @Test
  public void twoLevelsDeepWithInheritanceAndScopedProxy() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(S1ImporterWithProxy.class);
    ctx.refresh();

    S1ConfigWithProxy config = ctx.getBean(S1ConfigWithProxy.class);
    assertThat(config != ctx.getBean(S1ConfigWithProxy.class)).isTrue();
    TestBean tb = ctx.getBean("l0Bean", TestBean.class);
    assertThat(tb == ctx.getBean("l0Bean", TestBean.class)).isTrue();

    ctx.getBean(L0Config.L1Config.class);
    ctx.getBean("l1Bean");

    ctx.getBean(L0Config.L1Config.L2Config.class);
    ctx.getBean("l2Bean");

    // ensure that override order is correct and that it is a singleton
    TestBean ob = ctx.getBean("overrideBean", TestBean.class);
    assertThat(ob.getName()).isEqualTo("override-s1");
    assertThat(ob == ctx.getBean("overrideBean", TestBean.class)).isTrue();

    TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
    TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
    assertThat(pb1 != pb2).isTrue();
    assertThat(pb1.getFriends().iterator().next() != pb2.getFriends().iterator().next()).isTrue();
  }

  @Test
  public void twoLevelsWithNoBeanMethods() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(L0ConfigEmpty.class);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("l0ConfigEmpty")).isFalse();
    Object l0i1 = ctx.getBean(L0ConfigEmpty.class);
    Object l0i2 = ctx.getBean(L0ConfigEmpty.class);
    assertThat(l0i1 == l0i2).isTrue();

    Object l1i1 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.class);
    Object l1i2 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.class);
    assertThat(l1i1 != l1i2).isTrue();

    Object l2i1 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.L2ConfigEmpty.class);
    Object l2i2 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.L2ConfigEmpty.class);
    assertThat(l2i1 != l2i2).isTrue();
    assertThat(l2i2.toString()).isNotEqualTo(l2i1.toString());
  }

  @Test
  public void twoLevelsOnNonAnnotatedBaseClass() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(L0ConfigConcrete.class);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("l0ConfigConcrete")).isFalse();
    Object l0i1 = ctx.getBean(L0ConfigConcrete.class);
    Object l0i2 = ctx.getBean(L0ConfigConcrete.class);
    assertThat(l0i1 == l0i2).isTrue();

    Object l1i1 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.class);
    Object l1i2 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.class);
    assertThat(l1i1 != l1i2).isTrue();

    Object l2i1 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.L2ConfigEmpty.class);
    Object l2i2 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.L2ConfigEmpty.class);
    assertThat(l2i1 != l2i2).isTrue();
    assertThat(l2i2.toString()).isNotEqualTo(l2i1.toString());
  }

  @Configuration
  @Lazy
  static class L0Config {

    @Bean
    @Lazy
    public TestBean l0Bean() {
      return new TestBean("l0");
    }

    @Bean
    @Lazy
    public TestBean overrideBean() {
      return new TestBean("override-l0");
    }

    @Configuration
    static class L1Config {

      @Bean
      public TestBean l1Bean() {
        return new TestBean("l1");
      }

      @Bean
      public TestBean overrideBean() {
        return new TestBean("override-l1");
      }

      @Configuration
      @Lazy
      protected static class L2Config {

        @Bean
        @Lazy
        public TestBean l2Bean() {
          return new TestBean("l2");
        }

        @Bean
        @Lazy
        public TestBean overrideBean() {
          return new TestBean("override-l2");
        }
      }
    }
  }

  @Component
  @Lazy
  static class L0ConfigLight {

    @Bean
    @Lazy
    public TestBean l0Bean() {
      return new TestBean("l0");
    }

    @Bean
    @Lazy
    public TestBean overrideBean() {
      return new TestBean("override-l0");
    }

    @Component
    static class L1ConfigLight {

      @Bean
      public TestBean l1Bean() {
        return new TestBean("l1");
      }

      @Bean
      public TestBean overrideBean() {
        return new TestBean("override-l1");
      }

      @Component
      @Lazy
      protected static class L2ConfigLight {

        @Bean
        @Lazy
        public TestBean l2Bean() {
          return new TestBean("l2");
        }

        @Bean
        @Lazy
        public TestBean overrideBean() {
          return new TestBean("override-l2");
        }
      }
    }
  }

  @Configuration
  @Scope("prototype")
  static class S1Config extends L0Config {

    @Override
    @Bean
    public TestBean overrideBean() {
      return new TestBean("override-s1");
    }

    @Bean
    @Scope("prototype")
    public TestBean prototypeBean() {
      TestBean tb = new TestBean("override-s1");
      tb.getFriends().add(this);
      return tb;
    }
  }

  @Configuration
  @Scope(value = "prototype"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
  static class S1ConfigWithProxy extends L0Config {

    @Override
    @Bean
    public TestBean overrideBean() {
      return new TestBean("override-s1");
    }

    @Bean
    @Scope("prototype")
    public TestBean prototypeBean() {
      TestBean tb = new TestBean("override-s1");
      tb.getFriends().add(this);
      return tb;
    }
  }

  @Import(S1Config.class)
  static class S1Importer {
  }

  @Import(S1ConfigWithProxy.class)
  static class S1ImporterWithProxy {
  }

  @Component
  @Lazy
  static class L0ConfigEmpty {

    @Component
    @Scope("prototype")
    static class L1ConfigEmpty {

      @Component
      @Scope(value = "prototype"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
      protected static class L2ConfigEmpty {
      }
    }
  }

  static class L0ConfigBase {

    @Component
    @Scope("prototype")
    static class L1ConfigEmpty {

      @Component
      @Scope(value = "prototype"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
      protected static class L2ConfigEmpty {
      }
    }
  }

  @Component
  @Lazy
  static class L0ConfigConcrete extends L0ConfigBase {
  }

}
