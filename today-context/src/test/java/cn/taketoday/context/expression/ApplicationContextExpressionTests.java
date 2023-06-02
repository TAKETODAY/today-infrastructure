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

package cn.taketoday.context.expression;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.PropertyPlaceholderConfigurer;
import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.beans.factory.support.AutowireCandidateQualifier;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.SerializationTestUtils;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.conversion.support.GenericConversionService;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
class ApplicationContextExpressionTests {

  @Test
  @SuppressWarnings("deprecation")
  void genericApplicationContext() throws Exception {
    GenericApplicationContext ac = new GenericApplicationContext();
    AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);

    ac.getBeanFactory().registerScope("myScope", new Scope() {
      @Override
      public Object get(String name, Supplier<?> objectFactory) {
        return objectFactory.get();
      }

      @Override
      public Object remove(String name) {
        return null;
      }

      @Override
      public void registerDestructionCallback(String name, Runnable callback) {
      }

      @Override
      public Object resolveContextualObject(String key) {
        if (key.equals("mySpecialAttr")) {
          return "42";
        }
        else {
          return null;
        }
      }

      @Override
      public String getConversationId() {
        return null;
      }
    });

    ac.getBeanFactory().setConversionService(new DefaultConversionService());

    PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
    Properties placeholders = new Properties();
    placeholders.setProperty("code", "123");
    ppc.setProperties(placeholders);
    ac.addBeanFactoryPostProcessor(ppc);

    GenericBeanDefinition bd0 = new GenericBeanDefinition();
    bd0.setBeanClass(TestBean.class);
    bd0.getPropertyValues().add("name", "myName");
    bd0.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "original"));
    ac.registerBeanDefinition("tb0", bd0);

    GenericBeanDefinition bd1 = new GenericBeanDefinition();
    bd1.setBeanClassName("#{tb0.class}");
    bd1.setScope("myScope");
    bd1.getConstructorArgumentValues().addGenericArgumentValue("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ");
    bd1.getConstructorArgumentValues().addGenericArgumentValue("#{mySpecialAttr}");
    ac.registerBeanDefinition("tb1", bd1);

    GenericBeanDefinition bd2 = new GenericBeanDefinition();
    bd2.setBeanClassName("#{tb1.class}");
    bd2.setScope("myScope");
    bd2.getPropertyValues().add("name", "{ XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ }");
    bd2.getPropertyValues().add("age", "#{mySpecialAttr}");
    bd2.getPropertyValues().add("country", "${code} #{systemProperties.country}");
    ac.registerBeanDefinition("tb2", bd2);

    GenericBeanDefinition bd3 = new GenericBeanDefinition();
    bd3.setBeanClass(ValueTestBean.class);
    bd3.setScope("myScope");
    ac.registerBeanDefinition("tb3", bd3);

    GenericBeanDefinition bd4 = new GenericBeanDefinition();
    bd4.setBeanClass(ConstructorValueTestBean.class);
    bd4.setScope("myScope");
    ac.registerBeanDefinition("tb4", bd4);

    GenericBeanDefinition bd5 = new GenericBeanDefinition();
    bd5.setBeanClass(MethodValueTestBean.class);
    bd5.setScope("myScope");
    ac.registerBeanDefinition("tb5", bd5);

    GenericBeanDefinition bd6 = new GenericBeanDefinition();
    bd6.setBeanClass(PropertyValueTestBean.class);
    bd6.setScope("myScope");
    ac.registerBeanDefinition("tb6", bd6);

    System.getProperties().put("country", "UK");
    try {
      ac.refresh();

      TestBean tb0 = ac.getBean("tb0", TestBean.class);

      TestBean tb1 = ac.getBean("tb1", TestBean.class);
      assertThat(tb1.getName()).isEqualTo("XXXmyNameYYY42ZZZ");
      assertThat(tb1.getAge()).isEqualTo(42);

      TestBean tb2 = ac.getBean("tb2", TestBean.class);
      assertThat(tb2.getName()).isEqualTo("{ XXXmyNameYYY42ZZZ }");
      assertThat(tb2.getAge()).isEqualTo(42);
      assertThat(tb2.getCountry()).isEqualTo("123 UK");

      ValueTestBean tb3 = ac.getBean("tb3", ValueTestBean.class);
      assertThat(tb3.name).isEqualTo("XXXmyNameYYY42ZZZ");
      assertThat(tb3.age).isEqualTo(42);
      assertThat(tb3.ageFactory.get().intValue()).isEqualTo(42);
      assertThat(tb3.country).isEqualTo("123 UK");
      assertThat(tb3.countryFactory.get()).isEqualTo("123 UK");
      System.getProperties().put("country", "US");
      assertThat(tb3.country).isEqualTo("123 UK");
      assertThat(tb3.countryFactory.get()).isEqualTo("123 US");
      System.getProperties().put("country", "UK");
      assertThat(tb3.country).isEqualTo("123 UK");
      assertThat(tb3.countryFactory.get()).isEqualTo("123 UK");
      assertThat(tb3.optionalValue1.get()).isEqualTo("123");
      assertThat(tb3.optionalValue2.get()).isEqualTo("123");
      assertThat(tb3.optionalValue3.isPresent()).isFalse();
      assertThat(tb3.tb).isSameAs(tb0);

      tb3 = SerializationTestUtils.serializeAndDeserialize(tb3);
      assertThat(tb3.countryFactory.get()).isEqualTo("123 UK");

      ConstructorValueTestBean tb4 = ac.getBean("tb4", ConstructorValueTestBean.class);
      assertThat(tb4.name).isEqualTo("XXXmyNameYYY42ZZZ");
      assertThat(tb4.age).isEqualTo(42);
      assertThat(tb4.country).isEqualTo("123 UK");
      assertThat(tb4.tb).isSameAs(tb0);

      MethodValueTestBean tb5 = ac.getBean("tb5", MethodValueTestBean.class);
      assertThat(tb5.name).isEqualTo("XXXmyNameYYY42ZZZ");
      assertThat(tb5.age).isEqualTo(42);
      assertThat(tb5.country).isEqualTo("123 UK");
      assertThat(tb5.tb).isSameAs(tb0);

      PropertyValueTestBean tb6 = ac.getBean("tb6", PropertyValueTestBean.class);
      assertThat(tb6.name).isEqualTo("XXXmyNameYYY42ZZZ");
      assertThat(tb6.age).isEqualTo(42);
      assertThat(tb6.country).isEqualTo("123 UK");
      assertThat(tb6.tb).isSameAs(tb0);
    }
    finally {
      System.getProperties().remove("country");
    }
  }

  @Test
  void prototypeCreationReevaluatesExpressions() {
    GenericApplicationContext ac = new GenericApplicationContext();
    AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
    GenericConversionService cs = new GenericConversionService();
    cs.addConverter(String.class, String.class, String::trim);
    ac.getBeanFactory().registerSingleton(GenericApplicationContext.CONVERSION_SERVICE_BEAN_NAME, cs);
    RootBeanDefinition rbd = new RootBeanDefinition(PrototypeTestBean.class);
    rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    rbd.getPropertyValues().add("country", "#{systemProperties.country}");
    rbd.getPropertyValues().add("country2", new TypedStringValue("-#{systemProperties.country}-"));
    ac.registerBeanDefinition("test", rbd);
    ac.refresh();

    try {
      System.getProperties().put("name", "juergen1");
      System.getProperties().put("country", " UK1 ");
      PrototypeTestBean tb = (PrototypeTestBean) ac.getBean("test");
      assertThat(tb.getName()).isEqualTo("juergen1");
      assertThat(tb.getCountry()).isEqualTo("UK1");
      assertThat(tb.getCountry2()).isEqualTo("-UK1-");

      System.getProperties().put("name", "juergen2");
      System.getProperties().put("country", "  UK2  ");
      tb = (PrototypeTestBean) ac.getBean("test");
      assertThat(tb.getName()).isEqualTo("juergen2");
      assertThat(tb.getCountry()).isEqualTo("UK2");
      assertThat(tb.getCountry2()).isEqualTo("-UK2-");
    }
    finally {
      System.getProperties().remove("name");
      System.getProperties().remove("country");
    }
  }

  @Test
  void stringConcatenationWithDebugLogging() {
    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();

    GenericBeanDefinition bd = new GenericBeanDefinition();
    bd.setBeanClass(String.class);
    bd.getConstructorArgumentValues().addGenericArgumentValue("test-#{ T(java.lang.System).currentTimeMillis() }");
    ac.registerBeanDefinition("str", bd);
    ac.refresh();

    String str = ac.getBean("str", String.class);
    assertThat(str.startsWith("test-")).isTrue();
    ac.close();
  }

  @Test
  void resourceInjection() throws IOException {
    try {
      System.setProperty("logfile", "do_not_delete_me.txt");
      var ac = new AnnotationConfigApplicationContext(ResourceInjectionBean.class);
      ResourceInjectionBean resourceInjectionBean = ac.getBean(ResourceInjectionBean.class);
      Resource resource = new ClassPathResource("do_not_delete_me.txt");
      assertThat(resourceInjectionBean.resource).isEqualTo(resource);
      assertThat(resourceInjectionBean.url).isEqualTo(resource.getURL());
      assertThat(resourceInjectionBean.uri).isEqualTo(resource.getURI());
      assertThat(resourceInjectionBean.file).isEqualTo(resource.getFile());
      assertThat(FileCopyUtils.copyToByteArray(resourceInjectionBean.inputStream)).isEqualTo(FileCopyUtils.copyToByteArray(resource.getInputStream()));
      assertThat(FileCopyUtils.copyToString(resourceInjectionBean.reader)).isEqualTo(FileCopyUtils.copyToString(new EncodedResource(resource).getReader()));
    }
    finally {
      System.getProperties().remove("logfile");
    }
  }

  @SuppressWarnings("serial")
  public static class ValueTestBean implements Serializable {

    @Autowired
    @Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ")
    public String name;

    @Autowired
    @Value("#{mySpecialAttr}")
    public int age;

    @Value("#{mySpecialAttr}")
    public Supplier<Integer> ageFactory;

    @Value("${code} #{systemProperties.country}")
    public String country;

    @Value("${code} #{systemProperties.country}")
    public Supplier<String> countryFactory;

    @Value("${code}")
    private transient Optional<String> optionalValue1;

    @Value("${code:#{null}}")
    private transient Optional<String> optionalValue2;

    @Value("${codeX:#{null}}")
    private transient Optional<String> optionalValue3;

    @Autowired
    @Qualifier("original")
    public transient TestBean tb;
  }

  public static class ConstructorValueTestBean {

    public String name;

    public int age;

    public String country;

    public TestBean tb;

    @Autowired
    public ConstructorValueTestBean(
            @Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ") String name,
            @Value("#{mySpecialAttr}") int age,
            @Qualifier("original") TestBean tb,
            @Value("${code} #{systemProperties.country}") String country) {
      this.name = name;
      this.age = age;
      this.country = country;
      this.tb = tb;
    }
  }

  public static class MethodValueTestBean {

    public String name;

    public int age;

    public String country;

    public TestBean tb;

    @Autowired
    public void configure(
            @Qualifier("original") TestBean tb,
            @Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ") String name,
            @Value("#{mySpecialAttr}") int age,
            @Value("${code} #{systemProperties.country}") String country) {
      this.name = name;
      this.age = age;
      this.country = country;
      this.tb = tb;
    }
  }

  public static class PropertyValueTestBean {

    public String name;

    public int age;

    public String country;

    public TestBean tb;

    @Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ")
    public void setName(String name) {
      this.name = name;
    }

    @Value("#{mySpecialAttr}")
    public void setAge(int age) {
      this.age = age;
    }

    @Value("${code} #{systemProperties.country}")
    public void setCountry(String country) {
      this.country = country;
    }

    @Autowired
    @Qualifier("original")
    public void setTb(TestBean tb) {
      this.tb = tb;
    }
  }

  public static class PrototypeTestBean {

    public String name;

    public String country;

    public String country2;

    @Value("#{systemProperties.name}")
    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setCountry(String country) {
      this.country = country;
    }

    public String getCountry() {
      return country;
    }

    public void setCountry2(String country2) {
      this.country2 = country2;
    }

    public String getCountry2() {
      return country2;
    }
  }

  public static class ResourceInjectionBean {

    @Value("classpath:#{systemProperties.logfile}")
    Resource resource;

    @Value("classpath:#{systemProperties.logfile}")
    URL url;

    @Value("classpath:#{systemProperties.logfile}")
    URI uri;

    @Value("classpath:#{systemProperties.logfile}")
    File file;

    @Value("classpath:#{systemProperties.logfile}")
    InputStream inputStream;

    @Value("classpath:#{systemProperties.logfile}")
    Reader reader;
  }

}
