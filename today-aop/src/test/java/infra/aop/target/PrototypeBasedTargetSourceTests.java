/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.target;

import org.junit.jupiter.api.Test;

import infra.aop.TargetSource;
import infra.beans.testfixture.beans.SerializablePerson;
import infra.beans.testfixture.beans.TestBean;
import infra.beans.PropertyValues;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests relating to the abstract {@link AbstractPrototypeTargetSource}
 * and not subclasses.
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public class PrototypeBasedTargetSourceTests {

  @Test
  public void testSerializability() throws Exception {
    PropertyValues tsPvs = new PropertyValues();
    tsPvs.add("targetBeanName", "person");
    RootBeanDefinition tsBd = new RootBeanDefinition(TestTargetSource.class);
    tsBd.setPropertyValues(tsPvs);

    PropertyValues pvs = new PropertyValues();
    RootBeanDefinition bd = new RootBeanDefinition(SerializablePerson.class);
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
