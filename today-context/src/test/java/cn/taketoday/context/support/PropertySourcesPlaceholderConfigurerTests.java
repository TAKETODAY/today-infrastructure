/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.env.MockPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;

import static cn.taketoday.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static cn.taketoday.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/12 15:37
 */
class PropertySourcesPlaceholderConfigurerTests {

  @Test
  public void replacementFromEnvironmentProperties() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    MockEnvironment env = new MockEnvironment();
    env.setProperty("my.name", "myValue");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
    assertThat(ppc.getAppliedPropertySources()).isNotNull();
  }

  @Test
  public void localPropertiesViaResource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Resource resource = new ClassPathResource("PropertySourcesPlaceholderConfigurerTests.properties", this.getClass());
    ppc.setLocation(resource);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("foo");
  }

  @Test
  public void localPropertiesOverrideFalse() {
    localPropertiesOverride(false);
  }

  @Test
  public void localPropertiesOverrideTrue() {
    localPropertiesOverride(true);
  }

  @Test
  public void explicitPropertySources() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySources propertySources = new PropertySources();
    propertySources.addLast(new MockPropertySource().withProperty("my.name", "foo"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPropertySources(propertySources);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("foo");
    assertThat(propertySources.iterator().next()).isEqualTo(ppc.getAppliedPropertySources().iterator().next());
  }

  @Test
  public void explicitPropertySourcesExcludesEnvironment() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySources propertySources = new PropertySources();
    propertySources.addLast(new MockPropertySource());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPropertySources(propertySources);
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", "env"));
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
    assertThat(propertySources.iterator().next()).isEqualTo(ppc.getAppliedPropertySources().iterator().next());
  }

  @Test
  @SuppressWarnings("serial")
  public void explicitPropertySourcesExcludesLocalProperties() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySources propertySources = new PropertySources();
    propertySources.addLast(new MockPropertySource());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPropertySources(propertySources);
    ppc.setProperties(new Properties() {{
      put("my.name", "local");
    }});
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
  }

  @Test
  public void ignoreUnresolvablePlaceholders_falseIsDefault() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    //pc.setIgnoreUnresolvablePlaceholders(false); // the default
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> ppc.postProcessBeanFactory(bf))
            .havingCause()
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .withMessage("Could not resolve placeholder 'my.name' in value \"${my.name}\"");
  }

  @Test
  public void ignoreUnresolvablePlaceholders_true() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
  }

  @Test
  // https://github.com/spring-projects/spring-framework/issues/27947
  public void ignoreUnresolvablePlaceholdersInAtValueAnnotation__falseIsDefault() {
    MockPropertySource mockPropertySource = new MockPropertySource("test");
    mockPropertySource.setProperty("my.key", "${enigma}");
    @SuppressWarnings("resource")
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addLast(mockPropertySource);
    context.register(IgnoreUnresolvablePlaceholdersFalseConfig.class);

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .withMessage("Could not resolve placeholder 'enigma' in value \"${enigma}\"");
  }

  @Test
  // https://github.com/spring-projects/spring-framework/issues/27947
  public void ignoreUnresolvablePlaceholdersInAtValueAnnotation_true() {
    MockPropertySource mockPropertySource = new MockPropertySource("test");
    mockPropertySource.setProperty("my.key", "${enigma}");
    @SuppressWarnings("resource")
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addLast(mockPropertySource);
    context.register(IgnoreUnresolvablePlaceholdersTrueConfig.class);
    context.refresh();

    IgnoreUnresolvablePlaceholdersTrueConfig config = context.getBean(IgnoreUnresolvablePlaceholdersTrueConfig.class);
    assertThat(config.value).isEqualTo("${enigma}");
  }

  @Test
  @SuppressWarnings("serial")
  public void nestedUnresolvablePlaceholder() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setProperties(new Properties() {{
      put("my.name", "${bogus}");
    }});
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
            ppc.postProcessBeanFactory(bf));
  }

  @Test
  @SuppressWarnings("serial")
  public void ignoredNestedUnresolvablePlaceholder() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setProperties(new Properties() {{
      put("my.name", "${bogus}");
    }});
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${bogus}");
  }

  @Test
  public void withNonEnumerablePropertySource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${foo}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

    PropertySource<?> ps = new PropertySource<>("simplePropertySource", new Object()) {
      @Override
      public Object getProperty(String key) {
        return "bar";
      }
    };

    MockEnvironment env = new MockEnvironment();
    env.getPropertySources().addFirst(ps);
    ppc.setEnvironment(env);

    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("bar");
  }

  @SuppressWarnings("serial")
  private void localPropertiesOverride(boolean override) {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${foo}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

    ppc.setLocalOverride(override);
    ppc.setProperties(new Properties() {{
      setProperty("foo", "local");
    }});
    ppc.setEnvironment(new MockEnvironment().withProperty("foo", "enclosing"));
    ppc.postProcessBeanFactory(bf);
    if (override) {
      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("local");
    }
    else {
      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("enclosing");
    }
  }

  @Test
  public void customPlaceholderPrefixAndSuffix() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPlaceholderPrefix("@<");
    ppc.setPlaceholderSuffix(">");

    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            rootBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "@<key1>")
                    .addPropertyValue("sex", "${key2}")
                    .getBeanDefinition());

    System.setProperty("key1", "systemKey1Value");
    System.setProperty("key2", "systemKey2Value");
    ppc.setEnvironment(new StandardEnvironment());
    ppc.postProcessBeanFactory(bf);
    System.clearProperty("key1");
    System.clearProperty("key2");

    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("systemKey1Value");
    assertThat(bf.getBean(TestBean.class).getSex()).isEqualTo("${key2}");
  }

  @Test
  public void nullValueIsPreserved() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setNullValue("customNull");
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", "customNull"));
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isNull();
  }

  @Test
  public void trimValuesIsOffByDefault() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo(" myValue  ");
  }

  @Test
  public void trimValuesIsApplied() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setTrimValues(true);
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
  }

  @Test
  public void getAppliedPropertySourcesTooEarly() throws Exception {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    assertThatIllegalStateException().isThrownBy(
            ppc::getAppliedPropertySources);
  }

  @Test
  public void multipleLocationsWithDefaultResolvedValue() throws Exception {
    // SPR-10619
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ClassPathResource doesNotHave = new ClassPathResource("test.properties", getClass());
    ClassPathResource setToTrue = new ClassPathResource("placeholder.properties", getClass());
    ppc.setLocations(doesNotHave, setToTrue);
    ppc.setIgnoreResourceNotFound(true);
    ppc.setIgnoreUnresolvablePlaceholders(true);
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("jedi", "${jedi:false}")
                    .getBeanDefinition());
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).isJedi()).isTrue();
  }

  @Test
  public void optionalPropertyWithValue() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setConversionService(new DefaultConversionService());
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(OptionalTestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    MockEnvironment env = new MockEnvironment();
    env.setProperty("my.name", "myValue");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(OptionalTestBean.class).getName()).isEqualTo(Optional.of("myValue"));
  }

  @Test
  public void optionalPropertyWithoutValue() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setConversionService(new DefaultConversionService());
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(OptionalTestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    MockEnvironment env = new MockEnvironment();
    env.setProperty("my.name", "");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.setNullValue("");
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(OptionalTestBean.class).getName()).isEqualTo(Optional.empty());
  }

  private static class OptionalTestBean {

    private Optional<String> name;

    public Optional<String> getName() {
      return name;
    }

    @SuppressWarnings("unused")
    public void setName(Optional<String> name) {
      this.name = name;
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class IgnoreUnresolvablePlaceholdersFalseConfig {

    @Value("${my.key}")
    String value;

    @Bean
    static PropertySourcesPlaceholderConfigurer pspc() {
      return new PropertySourcesPlaceholderConfigurer();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class IgnoreUnresolvablePlaceholdersTrueConfig {

    @Value("${my.key}")
    String value;

    @Bean
    static PropertySourcesPlaceholderConfigurer pspc() {
      PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
      pspc.setIgnoreUnresolvablePlaceholders(true);
      return pspc;
    }
  }

}
