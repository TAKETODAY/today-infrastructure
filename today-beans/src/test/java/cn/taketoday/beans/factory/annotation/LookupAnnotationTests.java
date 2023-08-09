/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Karl Pietrzak
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/8 14:36
 */
public class LookupAnnotationTests {

  private StandardBeanFactory beanFactory;

  @BeforeEach
  public void setup() {
    beanFactory = new StandardBeanFactory();
    AutowiredAnnotationBeanPostProcessor aabpp = new AutowiredAnnotationBeanPostProcessor();
    aabpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(aabpp);
    beanFactory.registerBeanDefinition("abstractBean", new RootBeanDefinition(AbstractBean.class));
    beanFactory.registerBeanDefinition("beanConsumer", new RootBeanDefinition(BeanConsumer.class));
    RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
    tbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("testBean", tbd);
  }

  @Test
  public void testWithoutConstructorArg() {
    AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
    Object expected = bean.get();
    assertThat(expected.getClass()).isEqualTo(TestBean.class);
    assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
  }

  @Test
  public void testWithOverloadedArg() {
    AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
    TestBean expected = bean.get("haha");
    assertThat(expected.getClass()).isEqualTo(TestBean.class);
    assertThat(expected.getName()).isEqualTo("haha");
    assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
  }

  @Test
  public void testWithOneConstructorArg() {
    AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
    TestBean expected = bean.getOneArgument("haha");
    assertThat(expected.getClass()).isEqualTo(TestBean.class);
    assertThat(expected.getName()).isEqualTo("haha");
    assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
  }

  @Test
  public void testWithTwoConstructorArg() {
    AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
    TestBean expected = bean.getTwoArguments("haha", 72);
    assertThat(expected.getClass()).isEqualTo(TestBean.class);
    assertThat(expected.getName()).isEqualTo("haha");
    assertThat(expected.getAge()).isEqualTo(72);
    assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
  }

  @Test
  public void testWithThreeArgsShouldFail() {
    AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
    assertThatExceptionOfType(AbstractMethodError.class).as("TestBean has no three arg constructor").isThrownBy(() ->
        bean.getThreeArguments("name", 1, 2));
    assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
  }

  @Test
  public void testWithEarlyInjection() {
    AbstractBean bean = beanFactory.getBean("beanConsumer", BeanConsumer.class).abstractBean;
    Object expected = bean.get();
    assertThat(expected.getClass()).isEqualTo(TestBean.class);
    assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
  }

  @Test  // gh-25806
  public void testWithNullBean() {
    RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class, () -> null);
    tbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("testBean", tbd);

    AbstractBean bean = beanFactory.getBean("beanConsumer", BeanConsumer.class).abstractBean;
    Object expected = bean.get();
    assertThat(expected).isNull();
    assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
  }

  @Test
  public void testWithGenericBean() {
    beanFactory.registerBeanDefinition("numberBean", new RootBeanDefinition(NumberBean.class));
    beanFactory.registerBeanDefinition("doubleStore", new RootBeanDefinition(DoubleStore.class));
    beanFactory.registerBeanDefinition("floatStore", new RootBeanDefinition(FloatStore.class));

    NumberBean bean = (NumberBean) beanFactory.getBean("numberBean");
    assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
    assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());
  }

  @Test
  public void testSingletonWithoutMetadataCaching() {
    beanFactory.setCacheBeanMetadata(false);

    beanFactory.registerBeanDefinition("numberBean", new RootBeanDefinition(NumberBean.class));
    beanFactory.registerBeanDefinition("doubleStore", new RootBeanDefinition(DoubleStore.class));
    beanFactory.registerBeanDefinition("floatStore", new RootBeanDefinition(FloatStore.class));

    NumberBean bean = (NumberBean) beanFactory.getBean("numberBean");
    assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
    assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());
  }

  @Test
  public void testPrototypeWithoutMetadataCaching() {
    beanFactory.setCacheBeanMetadata(false);

    beanFactory.registerBeanDefinition("numberBean", new RootBeanDefinition(NumberBean.class, BeanDefinition.SCOPE_PROTOTYPE, null));
    beanFactory.registerBeanDefinition("doubleStore", new RootBeanDefinition(DoubleStore.class));
    beanFactory.registerBeanDefinition("floatStore", new RootBeanDefinition(FloatStore.class));

    NumberBean bean = (NumberBean) beanFactory.getBean("numberBean");
    assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
    assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());

    bean = (NumberBean) beanFactory.getBean("numberBean");
    assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
    assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());
  }

  public static abstract class AbstractBean {

    @Lookup("testBean")
    public abstract TestBean get();

    @Lookup
    public abstract TestBean get(String name);  // overloaded

    @Lookup
    public abstract TestBean getOneArgument(String name);

    @Lookup
    public abstract TestBean getTwoArguments(String name, int age);

    // no @Lookup annotation
    public abstract TestBean getThreeArguments(String name, int age, int anotherArg);
  }

  public static class BeanConsumer {

    @Autowired
    AbstractBean abstractBean;
  }

  public static class NumberStore<T extends Number> {
  }

  public static class DoubleStore extends NumberStore<Double> {
  }

  public static class FloatStore extends NumberStore<Float> {
  }

  public static abstract class NumberBean {

    @Lookup
    public abstract NumberStore<Double> getDoubleStore();

    @Lookup
    public abstract NumberStore<Float> getFloatStore();
  }

}
