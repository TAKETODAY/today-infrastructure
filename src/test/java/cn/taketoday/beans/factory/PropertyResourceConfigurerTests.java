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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.testfixture.beans.IndexedTestBean;
import cn.taketoday.beans.factory.support.PropertyOverrideConfigurer;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.core.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/12 15:32
 */
public class PropertyResourceConfigurerTests {

  static {
    System.setProperty("java.util.prefs.PreferencesFactory", MockPreferencesFactory.class.getName());
  }

  private static final Class<?> CLASS = PropertyResourceConfigurerTests.class;
  private static final Resource TEST_PROPS = qualifiedResource(CLASS, "test.properties");
  private static final Resource XTEST_PROPS = qualifiedResource(CLASS, "xtest.properties"); // does not exist
  private static final Resource TEST_PROPS_XML = qualifiedResource(CLASS, "test.properties.xml");

  private final StandardBeanFactory factory = new StandardBeanFactory();

  @Test
  public void testPropertyOverrideConfigurer() {
    BeanDefinition def1 = new BeanDefinition(TestBean.class);
    factory.registerBeanDefinition("tb1", def1);

    BeanDefinition def2 = new BeanDefinition(TestBean.class);
    factory.registerBeanDefinition("tb2", def2);

    PropertyOverrideConfigurer poc1;
    PropertyOverrideConfigurer poc2;

    {
      poc1 = new PropertyOverrideConfigurer();
      Properties props = new Properties();
      props.setProperty("tb1.age", "99");
      props.setProperty("tb2.name", "test");
      poc1.setProperties(props);
    }

    {
      poc2 = new PropertyOverrideConfigurer();
      Properties props = new Properties();
      props.setProperty("tb2.age", "99");
      props.setProperty("tb2.name", "test2");
      poc2.setProperties(props);
    }

    // emulate what happens when BFPPs are added to an application context: It's LIFO-based
    poc2.postProcessBeanFactory(factory);
    poc1.postProcessBeanFactory(factory);

    TestBean tb1 = (TestBean) factory.getBean("tb1");
    TestBean tb2 = (TestBean) factory.getBean("tb2");

    assertThat(tb1.getAge()).isEqualTo(99);
    assertThat(tb2.getAge()).isEqualTo(99);
    assertThat(tb1.getName()).isEqualTo(null);
    assertThat(tb2.getName()).isEqualTo("test");
  }

  @Test
  public void testPropertyOverrideConfigurerWithNestedProperty() {
    BeanDefinition def = new BeanDefinition(IndexedTestBean.class);
    factory.registerBeanDefinition("tb", def);

    PropertyOverrideConfigurer poc;
    poc = new PropertyOverrideConfigurer();
    Properties props = new Properties();
    props.setProperty("tb.array[0].age", "99");
    props.setProperty("tb.list[1].name", "test");
    poc.setProperties(props);
    poc.postProcessBeanFactory(factory);

    IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
    assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
    assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
  }

  @Test
  public void testPropertyOverrideConfigurerWithNestedPropertyAndDotInBeanName() {
    BeanDefinition def = new BeanDefinition(IndexedTestBean.class);
    factory.registerBeanDefinition("my.tb", def);

    PropertyOverrideConfigurer poc;
    poc = new PropertyOverrideConfigurer();
    Properties props = new Properties();
    props.setProperty("my.tb_array[0].age", "99");
    props.setProperty("my.tb_list[1].name", "test");
    poc.setProperties(props);
    poc.setBeanNameSeparator("_");
    poc.postProcessBeanFactory(factory);

    IndexedTestBean tb = (IndexedTestBean) factory.getBean("my.tb");
    assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
    assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
  }

  @Test
  public void testPropertyOverrideConfigurerWithNestedMapPropertyAndDotInMapKey() {
    BeanDefinition def = new BeanDefinition(IndexedTestBean.class);
    factory.registerBeanDefinition("tb", def);

    PropertyOverrideConfigurer poc;
    poc = new PropertyOverrideConfigurer();
    Properties props = new Properties();
    props.setProperty("tb.map[key1]", "99");
    props.setProperty("tb.map[key2.ext]", "test");
    poc.setProperties(props);
    poc.postProcessBeanFactory(factory);

    IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
    assertThat(tb.getMap().get("key1")).isEqualTo("99");
    assertThat(tb.getMap().get("key2.ext")).isEqualTo("test");
  }

  @Test
  public void testPropertyOverrideConfigurerWithHeldProperties() {
    BeanDefinition def = new BeanDefinition(PropertiesHolder.class);
    factory.registerBeanDefinition("tb", def);

    PropertyOverrideConfigurer poc;
    poc = new PropertyOverrideConfigurer();
    Properties props = new Properties();
    props.setProperty("tb.heldProperties[mail.smtp.auth]", "true");
    poc.setProperties(props);
    poc.postProcessBeanFactory(factory);

    PropertiesHolder tb = (PropertiesHolder) factory.getBean("tb");
    assertThat(tb.getHeldProperties().getProperty("mail.smtp.auth")).isEqualTo("true");
  }

  @Test
  public void testPropertyOverrideConfigurerWithPropertiesFile() {
    BeanDefinition def = new BeanDefinition(IndexedTestBean.class);
    factory.registerBeanDefinition("tb", def);

    PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
    poc.setLocation(TEST_PROPS);
    poc.postProcessBeanFactory(factory);

    IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
    assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
    assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
  }

  @Test
  public void testPropertyOverrideConfigurerWithInvalidPropertiesFile() {
    BeanDefinition def = new BeanDefinition(IndexedTestBean.class);
    factory.registerBeanDefinition("tb", def);

    PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
    poc.setLocations(TEST_PROPS, XTEST_PROPS);
    poc.setIgnoreResourceNotFound(true);
    poc.postProcessBeanFactory(factory);

    IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
    assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
    assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
  }

  @Test
  public void testPropertyOverrideConfigurerWithPropertiesXmlFile() {
    BeanDefinition def = new BeanDefinition(IndexedTestBean.class);
    factory.registerBeanDefinition("tb", def);

    PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
    poc.setLocation(TEST_PROPS_XML);
    poc.postProcessBeanFactory(factory);

    IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
    assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
    assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
  }

  @Test
  public void testPropertyOverrideConfigurerWithConvertProperties() {
    BeanDefinition def = new BeanDefinition(IndexedTestBean.class);
    factory.registerBeanDefinition("tb", def);

    ConvertingOverrideConfigurer bfpp = new ConvertingOverrideConfigurer();
    Properties props = new Properties();
    props.setProperty("tb.array[0].name", "99");
    props.setProperty("tb.list[1].name", "test");
    bfpp.setProperties(props);
    bfpp.postProcessBeanFactory(factory);

    IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
    assertThat(tb.getArray()[0].getName()).isEqualTo("X99");
    assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("Xtest");
  }

  @Test
  public void testPropertyOverrideConfigurerWithInvalidKey() {
    factory.registerBeanDefinition("tb1", new BeanDefinition(TestBean.class));
    factory.registerBeanDefinition("tb2", new BeanDefinition(TestBean.class));

    {
      PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
      poc.setIgnoreInvalidKeys(true);
      Properties props = new Properties();
      props.setProperty("argh", "hgra");
      props.setProperty("tb2.name", "test");
//      props.setProperty("tb2.nam", "test");// TODO ignoreInvalidKeys
      props.setProperty("tb3.name", "test");
      poc.setProperties(props);
      poc.postProcessBeanFactory(factory);
      assertThat(factory.getBean("tb2", TestBean.class).getName()).isEqualTo("test");
    }
    {
      PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
      Properties props = new Properties();
      props.setProperty("argh", "hgra");
      props.setProperty("tb2.age", "99");
      props.setProperty("tb2.name", "test2");
      poc.setProperties(props);
      poc.setOrder(0); // won't actually do anything since we're not processing through an app ctx
      try {
        poc.postProcessBeanFactory(factory);
      }
      catch (BeanInitializationException ex) {
        // prove that the processor chokes on the invalid key
        assertThat(ex.getMessage().toLowerCase().contains("argh")).isTrue();
      }
    }
  }

  @Test
  public void testPropertyOverrideConfigurerWithIgnoreInvalidKeys() {
    factory.registerBeanDefinition("tb1", new BeanDefinition(TestBean.class));
    factory.registerBeanDefinition("tb2", new BeanDefinition(TestBean.class));

    {
      PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
      Properties props = new Properties();
      props.setProperty("tb2.age", "99");
      props.setProperty("tb2.name", "test2");
      poc.setProperties(props);
      poc.setOrder(0); // won't actually do anything since we're not processing through an app ctx
      poc.postProcessBeanFactory(factory);
    }
    {
      PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
      poc.setIgnoreInvalidKeys(true);
      Properties props = new Properties();
      props.setProperty("argh", "hgra");
      props.setProperty("tb1.age", "99");
      props.setProperty("tb2.name", "test");
      poc.setProperties(props);
      poc.postProcessBeanFactory(factory);
    }

    TestBean tb1 = (TestBean) factory.getBean("tb1");
    TestBean tb2 = (TestBean) factory.getBean("tb2");
    assertThat(tb1.getAge()).isEqualTo(99);
    assertThat(tb2.getAge()).isEqualTo(99);
    assertThat(tb1.getName()).isEqualTo(null);
    assertThat(tb2.getName()).isEqualTo("test");
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithSystemPropertyNotUsed() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("country", "${os.name}"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Properties props = new Properties();
    props.setProperty("os.name", "myos");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getCountry()).isEqualTo("myos");
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithUnresolvablePlaceholder() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("name", "${ref}"));
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
                    ppc.postProcessBeanFactory(factory))
            .withMessageContaining("ref");
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithIgnoreUnresolvablePlaceholder() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("name", "${ref}"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getName()).isEqualTo("${ref}");
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithEmptyStringAsNull() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("name", ""));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setNullValue("");
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getName()).isNull();
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithEmptyStringInPlaceholderAsNull() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("name", "${ref}"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setNullValue("");
    Properties props = new Properties();
    props.put("ref", "");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getName()).isNull();
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithNestedPlaceholderInKey() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my${key}key}"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Properties props = new Properties();
    props.put("key", "new");
    props.put("mynewkey", "myname");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getName()).isEqualTo("myname");
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithPlaceholderInAlias() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class));
    factory.registerAlias("tb", "${alias}");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Properties props = new Properties();
    props.put("alias", "tb2");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    TestBean tb2 = (TestBean) factory.getBean("tb2");
    assertThat(tb2).isSameAs(tb);
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithSelfReferencingPlaceholderInAlias() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class));
    factory.registerAlias("tb", "${alias}");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Properties props = new Properties();
    props.put("alias", "tb");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb).isNotNull();
    assertThat(factory.getAliases("tb").length).isEqualTo(0);
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithCircularReference() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("age", "${age}")
            .addPropertyValue("name", "name${var}")
    );

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Properties props = new Properties();
    props.setProperty("age", "99");
    props.setProperty("var", "${m}");
    props.setProperty("m", "${var}");
    ppc.setProperties(props);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
            ppc.postProcessBeanFactory(factory));
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithDefaultProperties() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("touchy", "${test}"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Properties props = new Properties();
    props.put("test", "mytest");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getTouchy()).isEqualTo("mytest");
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithInlineDefault() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("touchy", "${test:mytest}"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getTouchy()).isEqualTo("mytest");
  }

  @Test
  public void testPropertySourcesPlaceholderConfigurerWithAliases() {
    factory.registerBeanDefinition("tb", new BeanDefinition(TestBean.class)
            .addPropertyValue("touchy", "${test}"));

    factory.registerAlias("tb", "${myAlias}");
    factory.registerAlias("${myTarget}", "alias2");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Properties props = new Properties();
    props.put("test", "mytest");
    props.put("myAlias", "alias");
    props.put("myTarget", "tb");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(factory);

    TestBean tb = (TestBean) factory.getBean("tb");
    assertThat(tb.getTouchy()).isEqualTo("mytest");
    tb = (TestBean) factory.getBean("alias");
    assertThat(tb.getTouchy()).isEqualTo("mytest");
    tb = (TestBean) factory.getBean("alias2");
    assertThat(tb.getTouchy()).isEqualTo("mytest");
  }

  static class PropertiesHolder {

    private Properties props = new Properties();

    public Properties getHeldProperties() {
      return props;
    }

    public void setHeldProperties(Properties props) {
      this.props = props;
    }
  }

  private static class ConvertingOverrideConfigurer extends PropertyOverrideConfigurer {

    @Override
    protected String convertPropertyValue(String originalValue) {
      return "X" + originalValue;
    }
  }

  /**
   * {@link PreferencesFactory} to create {@link MockPreferences}.
   */
  public static class MockPreferencesFactory implements PreferencesFactory {

    private final Preferences userRoot = new MockPreferences();

    private final Preferences systemRoot = new MockPreferences();

    @Override
    public Preferences systemRoot() {
      return this.systemRoot;
    }

    @Override
    public Preferences userRoot() {
      return this.userRoot;
    }
  }

  /**
   * Mock implementation of {@link Preferences} that behaves the same regardless of the
   * underlying operating system and will never throw security exceptions.
   */
  public static class MockPreferences extends AbstractPreferences {

    private static Map<String, String> values = new HashMap<>();

    private static Map<String, AbstractPreferences> children = new HashMap<>();

    public MockPreferences() {
      super(null, "");
    }

    protected MockPreferences(AbstractPreferences parent, String name) {
      super(parent, name);
    }

    @Override
    protected void putSpi(String key, String value) {
      values.put(key, value);
    }

    @Override
    protected String getSpi(String key) {
      return values.get(key);
    }

    @Override
    protected void removeSpi(String key) {
      values.remove(key);
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
      return StringUtils.toStringArray(values.keySet());
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
      return StringUtils.toStringArray(children.keySet());
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
      AbstractPreferences child = children.get(name);
      if (child == null) {
        child = new MockPreferences(this, name);
        children.put(name, child);
      }
      return child;
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
    }
  }

}
