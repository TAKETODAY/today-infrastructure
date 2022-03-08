package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.Resource;

import static cn.taketoday.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 15:27
 */
class PropertyPathFactoryBeanTests {

  private static final Resource CONTEXT = qualifiedResource(PropertyPathFactoryBeanTests.class, "context.xml");

  @Test
  public void testPropertyPathFactoryBeanWithSingletonResult() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
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
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
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
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getType("tb.spouse.spouse")).isNull();
    assertThat(xbf.getBean("tb.spouse.spouse").toString()).isEqualTo("null");
  }

  @Test
  public void testPropertyPathFactoryBeanAsInnerBean() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
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
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getBean("tbWithNullReference", TestBean.class).getSpouse()).isNull();
  }

  @Test
  public void testPropertyPathFactoryBeanAsInnerNull() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getBean("tbWithInnerNull", TestBean.class).getSpouse()).isNull();
  }

}
