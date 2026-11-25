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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.PropertyResourceBundle;

import infra.beans.factory.BeanClassLoadFailedException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ByteArrayResource;
import infra.core.io.ClassPathResource;
import infra.core.io.EncodedResource;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.util.DefaultPropertiesPersister;
import infra.util.PropertiesPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/19 18:35
 */
class PropertiesBeanDefinitionReaderTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final PropertiesBeanDefinitionReader reader = new PropertiesBeanDefinitionReader(this.beanFactory);

  @Test
  void withSimpleConstructorArg() {
    this.reader.loadBeanDefinitions(new ClassPathResource("simpleConstructorArg.properties", getClass()));
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    assertThat(bean.getName()).isEqualTo("Rob Harrop");
  }

  @Test
  void withConstructorArgRef() {
    this.reader.loadBeanDefinitions(new ClassPathResource("refConstructorArg.properties", getClass()));
    TestBean rob = (TestBean) this.beanFactory.getBean("rob");
    TestBean sally = (TestBean) this.beanFactory.getBean("sally");
    assertThat(rob.getSpouse()).isEqualTo(sally);
  }

  @Test
  void withMultipleConstructorsArgs() {
    this.reader.loadBeanDefinitions(new ClassPathResource("multiConstructorArgs.properties", getClass()));
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    assertThat(bean.getName()).isEqualTo("Rob Harrop");
    assertThat(bean.getAge()).isEqualTo(23);
  }

  @Test
  void loadsOnlyMatchingDefinitionsAndInheritsFromTheDefaultParent() {
    var resource = new ByteArrayResource("""
            selected.parent.(class)=%s
            selected.parent.(abstract)=true
            selected.parent.age=42
            selected.child.(singleton)=false
            selected.child.(lazy-init)=true
            selected.child.name=**literal
            selected.unqualified
            ignored.other.(class)=java.lang.String
            """.formatted(TestBean.class.getName()).getBytes(StandardCharsets.UTF_8));
    this.reader.setDefaultParentBean("parent");

    assertThat(this.reader.getDefaultParentBean()).isEqualTo("parent");
    assertThat(this.reader.loadBeanDefinitions(resource, "selected.")).isEqualTo(2);
    assertThat(this.beanFactory.getBeanDefinitionNames()).containsExactlyInAnyOrder("parent", "child");
    assertThat(this.beanFactory.getBeanDefinition("parent").isAbstract()).isTrue();
    var definition = this.beanFactory.getBeanDefinition("child");
    assertThat(definition.getParentName()).isEqualTo("parent");
    assertThat(definition.isLazyInit()).isTrue();
    assertThat(definition.isPrototype()).isTrue();
    var child = this.beanFactory.getBean("child", TestBean.class);
    assertThat(child.getName()).isEqualTo("*literal");
    assertThat(child.getAge()).isEqualTo(42);
    assertThat(this.beanFactory.getBean("child")).isNotSameAs(child);
  }

  @Test
  void registersDefinitionsFromResourceBundlesWithoutOverridingExistingDefinitions() throws IOException {
    var bundle = new PropertyResourceBundle(new StringReader("""
            bean.(class)=%s
            bean.(singleton)=true
            bean.name=bundle
            """.formatted(TestBean.class.getName())));

    assertThat(this.reader.registerBeanDefinitions(bundle)).isEqualTo(1);
    assertThat(this.reader.registerBeanDefinitions(Map.of("bean.name", "replacement"))).isZero();
    assertThat(this.beanFactory.getBean("bean", TestBean.class).getName()).isEqualTo("bundle");
  }

  @Test
  void reportsInvalidKeysAndClassLoadingFailures() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.reader.registerBeanDefinitions(Map.of(42, "value")))
            .withMessageContaining("only Strings allowed");
    this.reader.setBeanClassLoader(getClass().getClassLoader());
    assertThatExceptionOfType(BeanClassLoadFailedException.class)
            .isThrownBy(() -> this.reader.registerBeanDefinitions(Map.of("bean.(class)", "infra.missing.DoesNotExist")))
            .withCauseInstanceOf(ClassNotFoundException.class);

    this.reader.setBeanClassLoader(new ClassLoader(getClass().getClassLoader()) {
      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.equals("infra.missing.Broken")) {
          throw new NoClassDefFoundError(name);
        }
        return super.loadClass(name);
      }
    });
    assertThatExceptionOfType(BeanClassLoadFailedException.class)
            .isThrownBy(() -> this.reader.registerBeanDefinitions(Map.of("bean.(class)", "infra.missing.Broken")))
            .withCauseInstanceOf(NoClassDefFoundError.class);
  }

  @Test
  void wrapsReadErrorsAndAllowsRestoringTheDefaultPersister() {
    var persister = mock(PropertiesPersister.class);
    var failure = new IOException("unreadable properties");
    var resource = new ByteArrayResource(new byte[0]) {
      @Override
      public InputStream getInputStream() throws IOException {
        throw failure;
      }
    };
    this.reader.setPropertiesPersister(persister);
    assertThat(this.reader.getPropertiesPersister()).isSameAs(persister);

    this.reader.setPropertiesPersister(null);
    assertThat(this.reader.getPropertiesPersister()).isSameAs(DefaultPropertiesPersister.INSTANCE);
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> this.reader.loadBeanDefinitions(new EncodedResource(resource)))
            .withMessageContaining("Could not parse properties").withCause(failure);
  }

  @Test
  void resolvesLocationsThroughConfiguredResourceLoaders() throws IOException {
    var first = new ByteArrayResource("one.(class)=java.lang.String".getBytes(StandardCharsets.UTF_8));
    var second = new ByteArrayResource("two.(class)=java.lang.String".getBytes(StandardCharsets.UTF_8));
    var plain = mock(ResourceLoader.class);
    given(plain.getResource("one")).willReturn(first);
    given(plain.getResource("two")).willReturn(second);
    this.reader.setResourceLoader(plain);
    var actual = new HashSet<Resource>();
    assertThat(this.reader.loadBeanDefinitions("one", actual)).isEqualTo(1);
    assertThat(actual).containsExactly(first);
    assertThat(this.reader.loadBeanDefinitions("one", "two")).isEqualTo(1);

    var patterns = mock(PatternResourceLoader.class);
    var failure = new IOException("invalid pattern");
    given(patterns.getResourcesArray("*")).willReturn(new Resource[] { first, second });
    given(patterns.getResourcesArray("broken")).willThrow(failure);
    this.reader.setResourceLoader(patterns);
    actual.clear();
    assertThat(this.reader.loadBeanDefinitions("*", actual)).isZero();
    assertThat(actual).containsExactlyInAnyOrder(first, second);
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> this.reader.loadBeanDefinitions("broken")).withCause(failure);

    this.reader.setResourceLoader(null);
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> this.reader.loadBeanDefinitions("one")).withMessageContaining("no ResourceLoader");
  }

}
