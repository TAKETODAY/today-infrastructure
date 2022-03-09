package cn.taketoday.aop.target;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.SerializableNopInterceptor;
import cn.taketoday.aop.SerializationTestUtils;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.NameMatchMethodPointcutTests.SerializablePerson;

import static cn.taketoday.aop.support.NameMatchMethodPointcutTests.Person;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/4 16:21
 */
public class HotSwappableTargetSourceTests {

  @Test
  public void testSerialization() throws Exception {
    SerializablePerson sp1 = new SerializablePerson();
    sp1.setName("Tony");
    SerializablePerson sp2 = new SerializablePerson();
    sp1.setName("Gordon");

    HotSwappableTargetSource hts = new HotSwappableTargetSource(sp1);
    ProxyFactory pf = new ProxyFactory();
    pf.addInterface(Person.class);
    pf.setTargetSource(hts);
    pf.addAdvisor(new DefaultPointcutAdvisor(new SerializableNopInterceptor()));
    Person p = (Person) pf.getProxy();

    assertThat(p.getName()).isEqualTo(sp1.getName());
    hts.swap(sp2);
    assertThat(p.getName()).isEqualTo(sp2.getName());

    p = SerializationTestUtils.serializeAndDeserialize(p);
    // We need to get a reference to the client-side targetsource
    hts = (HotSwappableTargetSource) ((Advised) p).getTargetSource();
    assertThat(p.getName()).isEqualTo(sp2.getName());
    hts.swap(sp1);
    assertThat(p.getName()).isEqualTo(sp1.getName());

  }

}
