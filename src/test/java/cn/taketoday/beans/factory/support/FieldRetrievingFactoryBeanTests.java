package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/6 17:31
 */
class FieldRetrievingFactoryBeanTests {

  @Test
  public void testStaticField() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setStaticField("java.sql.Connection.TRANSACTION_SERIALIZABLE");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testStaticFieldWithWhitespace() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setStaticField("  java.sql.Connection.TRANSACTION_SERIALIZABLE  ");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testStaticFieldViaClassAndFieldName() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetClass(Connection.class);
    fr.setTargetField("TRANSACTION_SERIALIZABLE");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testNonStaticField() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    PublicFieldHolder target = new PublicFieldHolder();
    fr.setTargetObject(target);
    fr.setTargetField("publicField");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(target.publicField);
  }

  @Test
  public void testNothingButBeanName() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setBeanName("java.sql.Connection.TRANSACTION_SERIALIZABLE");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testJustTargetField() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetField("TRANSACTION_SERIALIZABLE");
    try {
      fr.afterPropertiesSet();
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testJustTargetClass() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetClass(Connection.class);
    try {
      fr.afterPropertiesSet();
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testJustTargetObject() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetObject(new PublicFieldHolder());
    try {
      fr.afterPropertiesSet();
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testWithConstantOnClassWithPackageLevelVisibility() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setBeanName("cn.taketoday.beans.factory.support.PackageLevelVisibleBean.CONSTANT");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo("Wuby");
  }

  @Test
  public void testBeanNameSyntaxWithBeanFactory() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
/*
	<bean id="testBean" class="TestBean">
		<property name="someIntegerArray">
			<list>
				<bean name="java.sql.Connection.TRANSACTION_SERIALIZABLE" class="FieldRetrievingFactoryBean"/>
				<bean name="java.sql.Connection.TRANSACTION_SERIALIZABLE" class="FieldRetrievingFactoryBean"/>
			</list>
		</property>
	</bean>
*/

    BeanDefinition testBeanDef = new BeanDefinition("testBean", TestBean.class);
    testBeanDef.propertyValues().add("someIntegerArray", new PropertyValueRetriever() {
      @Override
      public Object retrieve(String propertyPath, BeanWrapper binder, AutowireCapableBeanFactory beanFactory) {
        return new Integer[] {
                (Integer) BeanDefinitionReference.from("java.sql.Connection.TRANSACTION_SERIALIZABLE", FieldRetrievingFactoryBean.class)
                        .retrieve(propertyPath, binder, beanFactory),
                (Integer) BeanDefinitionReference.from("java.sql.Connection.TRANSACTION_SERIALIZABLE", FieldRetrievingFactoryBean.class)
                        .retrieve(propertyPath, binder, beanFactory)
        };
      }
    });

    beanFactory.registerBeanDefinition(testBeanDef);

    TestBean testBean = (TestBean) beanFactory.getBean("testBean");
    assertThat(testBean.getSomeIntegerArray()[0]).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
    assertThat(testBean.getSomeIntegerArray()[1]).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  private static class PublicFieldHolder {

    public String publicField = "test";
  }

}
