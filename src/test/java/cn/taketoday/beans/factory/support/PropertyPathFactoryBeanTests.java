package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 15:27
 */
class PropertyPathFactoryBeanTests {

  private void load(StandardBeanFactory factory) {
    BeanDefinition definition = new BeanDefinition("tb", TestBean.class);
    definition.addPropertyValue("age", 10);
    definition.addPropertyValue("spouse", BeanDefinitionReference.from(
            new BeanDefinitionBuilder()
                    .beanClass(TestBean.class)
                    .propertyValues(new PropertyValues().add("age", 11))
    ));
    definition.setScope(Scope.PROTOTYPE);
    factory.registerBeanDefinition(definition);

    //
    BeanDefinition otb = new BeanDefinition("otb", TestBean.class);
    otb.addPropertyValue("age", 98);
    otb.addPropertyValue("spouse", BeanDefinitionReference.from(
            new BeanDefinitionBuilder()
                    .beanClass(TestBean.class)
                    .propertyValues(new PropertyValues().add("age", 99))
    ));
    factory.registerBeanDefinition(otb);

    BeanDefinition propertyPath1 = new BeanDefinition("propertyPath1", PropertyPathFactoryBean.class);
    propertyPath1.addPropertyValue("propertyPath", "age");
    propertyPath1.addPropertyValue("targetObject", BeanDefinitionReference.from(
            new BeanDefinitionBuilder()
                    .beanClass(TestBean.class)
                    .propertyValues(new PropertyValues().add("age", 12))
    ));
    factory.registerBeanDefinition(propertyPath1);

    BeanDefinition propertyPath2 = new BeanDefinition("propertyPath2", PropertyPathFactoryBean.class);
    propertyPath2.addPropertyValue("propertyPath", "spouse.age");
    propertyPath2.addPropertyValue("targetBeanName", "tb");
    factory.registerBeanDefinition(propertyPath2);

    // <bean id="tb.age" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean"/>
    factory.registerBeanDefinition(new BeanDefinition("tb.age", PropertyPathFactoryBean.class));

    // <bean id="otb.spouse" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean"/>
    factory.registerBeanDefinition(new BeanDefinition("otb.spouse", PropertyPathFactoryBean.class));

    // <bean id="tb.spouse" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean"/>
    factory.registerBeanDefinition(new BeanDefinition("tb.spouse", PropertyPathFactoryBean.class));

    // <bean id="tb.spouse.spouse" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean"/>
    factory.registerBeanDefinition(new BeanDefinition("tb.spouse.spouse", PropertyPathFactoryBean.class));

    //	<bean id="propertyPath3" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean">
    //		<property name="targetBeanName"><value>tb</value></property>
    //		<property name="propertyPath"><value>spouse</value></property>
    //		<property name="resultType"><value>cn.taketoday.beans.testfixture.beans.TestBean</value></property>
    //	</bean>

    BeanDefinition propertyPath3 = new BeanDefinition("propertyPath3", PropertyPathFactoryBean.class);
    propertyPath3.addPropertyValue("propertyPath", "spouse");
    propertyPath3.addPropertyValue("targetBeanName", "tb");
    propertyPath3.addPropertyValue("resultType", TestBean.class);

    factory.registerBeanDefinition(propertyPath3);

    // <bean id="tbWithInner" class="TestBean">
    //		<property name="age" value="10"/>
    //		<property name="spouse">
    //			<bean name="otb.spouse" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean"/>
    //		</property>
    //		<property name="friends">
    //			<bean name="otb.spouse" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean"/>
    //		</property>
    //	</bean>

    BeanDefinition tbWithInner = new BeanDefinition("tbWithInner", TestBean.class);
    tbWithInner.addPropertyValue("age", "10");
    tbWithInner.addPropertyValue("spouse", BeanDefinitionReference.from(
            "otb.spouse", PropertyPathFactoryBean.class
    ));
    tbWithInner.addPropertyValue("friends", BeanDefinitionReference.from("otb.spouse", PropertyPathFactoryBean.class));
    factory.registerBeanDefinition(tbWithInner);

    //
    //	<bean id="tbWithNullReference" class="beans.TestBean">
    //		<property name="spouse" ref="tb.spouse.spouse"/>
    //	</bean>

    BeanDefinition tbWithNullReference = new BeanDefinition("tbWithNullReference", TestBean.class);
    tbWithNullReference.addPropertyValue("spouse", RuntimeBeanReference.from("tb.spouse.spouse"));
    factory.registerBeanDefinition(tbWithNullReference);

    //	<bean id="tbWithInnerNull" class="TestBean">
    //		<property name="spouse">
    //			<bean name="tb.spouse.spouse" class="cn.taketoday.beans.factory.support.PropertyPathFactoryBean"/>
    //		</property>
    //	</bean>

    BeanDefinition tbWithInnerNull = new BeanDefinition("tbWithInnerNull", TestBean.class);
    tbWithInnerNull.addPropertyValue("spouse", BeanDefinitionReference.from("tb.spouse.spouse", PropertyPathFactoryBean.class));
    factory.registerBeanDefinition(tbWithInnerNull);

  }

  @Test
  public void testPropertyPathFactoryBeanWithSingletonResult() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    load(xbf);

    assertThat(xbf.getBean("propertyPath1")).isEqualTo(12);
    assertThat(xbf.getBean("propertyPath2")).isEqualTo(11);
    assertThat(xbf.getBean("tb.age")).isEqualTo(10);
    assertThat(xbf.getType("otb.spouse")).isEqualTo(ITestBean.class);
    Object result1 = xbf.getBean("otb.spouse");
    Object result2 = xbf.getBean("otb.spouse");
    boolean condition = result1 instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(result1 == result2).isTrue();
    assertThat(((TestBean) result1).getAge()).isEqualTo(99);
  }

  @Test
  public void testPropertyPathFactoryBeanWithPrototypeResult() {
    StandardBeanFactory xbf = new StandardBeanFactory();

    load(xbf);
    assertThat(xbf.getType("tb.spouse")).isNull();
    assertThat(xbf.getType("propertyPath3")).isEqualTo(TestBean.class);
    Object result1 = xbf.getBean("tb.spouse");
    Object result2 = xbf.getBean("propertyPath3");
    Object result3 = xbf.getBean("propertyPath3");
    boolean condition2 = result1 instanceof TestBean;
    assertThat(condition2).isTrue();
    boolean condition1 = result2 instanceof TestBean;
    assertThat(condition1).isTrue();
    boolean condition = result3 instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(((TestBean) result1).getAge()).isEqualTo(11);
    assertThat(((TestBean) result2).getAge()).isEqualTo(11);
    assertThat(((TestBean) result3).getAge()).isEqualTo(11);
    assertThat(result1 != result2).isTrue();
    assertThat(result1 != result3).isTrue();
    assertThat(result2 != result3).isTrue();
  }

  @Test
  public void testPropertyPathFactoryBeanWithNullResult() {
    StandardBeanFactory xbf = new StandardBeanFactory();

    load(xbf);
    assertThat(xbf.getType("tb.spouse.spouse")).isNull();
    assertThat(xbf.getBean("tb.spouse.spouse")).isNull();
  }

  @Test
  public void testPropertyPathFactoryBeanAsInnerBean() {
    StandardBeanFactory xbf = new StandardBeanFactory();

    load(xbf);
    TestBean spouse = (TestBean) xbf.getBean("otb.spouse");
    TestBean tbWithInner = (TestBean) xbf.getBean("tbWithInner");
    assertThat(tbWithInner.getSpouse()).isSameAs(spouse);
    boolean condition = !tbWithInner.getFriends().isEmpty();
    assertThat(condition).isTrue();
    assertThat(tbWithInner.getFriends().iterator().next()).isSameAs(spouse);
  }

  @Test
  public void testPropertyPathFactoryBeanAsNullReference() {
    StandardBeanFactory xbf = new StandardBeanFactory();

    load(xbf);
    assertThat(xbf.getBean("tbWithNullReference", TestBean.class).getSpouse()).isNull();
  }

  @Test
  public void testPropertyPathFactoryBeanAsInnerNull() {
    StandardBeanFactory xbf = new StandardBeanFactory();

    load(xbf);
    assertThat(xbf.getBean("tbWithInnerNull", TestBean.class).getSpouse()).isNull();
  }

}
