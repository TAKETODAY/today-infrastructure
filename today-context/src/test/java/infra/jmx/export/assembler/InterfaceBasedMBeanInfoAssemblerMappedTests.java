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

package infra.jmx.export.assembler;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class InterfaceBasedMBeanInfoAssemblerMappedTests extends AbstractJmxAssemblerTests {

  protected static final String OBJECT_NAME = "bean:name=testBean4";

  @Test
  public void testGetAgeIsReadOnly() throws Exception {
    ModelMBeanInfo info = getMBeanInfoFromAssembler();
    ModelMBeanAttributeInfo attr = info.getAttribute(AGE_ATTRIBUTE);

    assertThat(attr.isReadable()).as("Age is not readable").isTrue();
    assertThat(attr.isWritable()).as("Age is not writable").isFalse();
  }

  @Test
  public void testWithUnknownClass() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            getWithMapping("com.foo.bar.Unknown"));
  }

  @Test
  public void testWithNonInterface() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            getWithMapping("JmxTestBean"));
  }

  @Test
  public void testWithFallThrough() throws Exception {
    InterfaceBasedMBeanInfoAssembler assembler =
            getWithMapping("foobar", "infra.jmx.export.assembler.ICustomJmxBean");
    assembler.setManagedInterfaces(new Class<?>[] { IAdditionalTestMethods.class });

    ModelMBeanInfo inf = assembler.getMBeanInfo(getBean(), getObjectName());
    MBeanAttributeInfo attr = inf.getAttribute("NickName");

    assertNickName(attr);
  }

  @Test
  public void testNickNameIsExposed() throws Exception {
    ModelMBeanInfo inf = (ModelMBeanInfo) getMBeanInfo();
    MBeanAttributeInfo attr = inf.getAttribute("NickName");

    assertNickName(attr);
  }

  @Override
  protected String getObjectName() {
    return OBJECT_NAME;
  }

  @Override
  protected int getExpectedOperationCount() {
    return 7;
  }

  @Override
  protected int getExpectedAttributeCount() {
    return 3;
  }

  @Override
  protected MBeanInfoAssembler getAssembler() throws Exception {
    return getWithMapping(
            "infra.jmx.export.assembler.IAdditionalTestMethods, " +
                    "infra.jmx.export.assembler.ICustomJmxBean");
  }

  @Override
  protected String getApplicationContextPath() {
    return "infra/jmx/export/assembler/interfaceAssemblerMapped.xml";
  }

  private InterfaceBasedMBeanInfoAssembler getWithMapping(String mapping) {
    return getWithMapping(OBJECT_NAME, mapping);
  }

  private InterfaceBasedMBeanInfoAssembler getWithMapping(String name, String mapping) {
    InterfaceBasedMBeanInfoAssembler assembler = new InterfaceBasedMBeanInfoAssembler();
    Properties props = new Properties();
    props.setProperty(name, mapping);
    assembler.setInterfaceMappings(props);
    assembler.afterPropertiesSet();
    return assembler;
  }

  private void assertNickName(MBeanAttributeInfo attr) {
    assertThat(attr).as("Nick Name should not be null").isNotNull();
    assertThat(attr.isWritable()).as("Nick Name should be writable").isTrue();
    assertThat(attr.isReadable()).as("Nick Name should be readable").isTrue();
  }

}
