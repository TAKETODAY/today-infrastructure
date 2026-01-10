/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.target;

import org.junit.jupiter.api.Test;

import infra.aop.TargetSource;
import infra.beans.PropertyValues;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.SerializablePerson;
import infra.beans.testfixture.beans.TestBean;
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
