package cn.taketoday.aop.target;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.SerializationTestUtils;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.NameMatchMethodPointcutTests.SerializablePerson;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/4 17:32
 */
public class PrototypeBasedTargetSourceTests {

  @Test
  public void testSerializability() throws Exception {
    PropertyValues tsPvs = new PropertyValues();
    tsPvs.add("targetBeanName", "person");
    BeanDefinition tsBd = new BeanDefinition(TestTargetSource.class);
    tsBd.setPropertyValues(tsPvs);

    PropertyValues pvs = new PropertyValues();
    BeanDefinition bd = new BeanDefinition(SerializablePerson.class);
    bd.setPropertyValues(pvs);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);

    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("ts", tsBd);
    bf.registerBeanDefinition("person", bd);

    TestTargetSource cpts = (TestTargetSource) bf.getBean("ts");
    TargetSource serialized = SerializationTestUtils.serializeAndDeserialize(cpts);
    boolean condition = serialized instanceof SingletonTargetSource;
    assertThat(condition).as("Changed to SingletonTargetSource on deserialization").isTrue();
    SingletonTargetSource sts = (SingletonTargetSource) serialized;
    assertThat(sts.getTarget()).isNotNull();
  }

  private static class TestTargetSource extends AbstractPrototypeTargetSource {

    private static final long serialVersionUID = 1L;

    /**
     * Nonserializable test field to check that subclass
     * state can't prevent serialization from working
     */
    @SuppressWarnings("unused")
    private TestBean thisFieldIsNotSerializable = new TestBean();

    @Override
    public Object getTarget() throws Exception {
      return newPrototypeInstance();
    }

    @Override
    public void releaseTarget(Object target) throws Exception {
      // Do nothing
    }
  }

}
