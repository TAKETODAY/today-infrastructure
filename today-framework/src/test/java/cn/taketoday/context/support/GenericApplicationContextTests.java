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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.InvalidPathException;

import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.config.AbstractFactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.FileSystemResourceLoader;
import cn.taketoday.core.io.FileUrlResource;
import cn.taketoday.core.io.ProtocolResolver;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class GenericApplicationContextTests {

  private final GenericApplicationContext context = new GenericApplicationContext();

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void getBeanForClass() {
    context.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
    context.refresh();

    assertThat(context.getBean("testBean")).isEqualTo("");
    assertThat(context.getBean(String.class)).isSameAs(context.getBean("testBean"));
    assertThat(context.getBean(CharSequence.class)).isSameAs(context.getBean("testBean"));

    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
            .isThrownBy(() -> context.getBean(Object.class));
  }

  @Test
  void withSingletonSupplier() {
    context.registerBeanDefinition("testBean", new RootBeanDefinition(String.class, context::toString));
    context.refresh();

    assertThat(context.getBean("testBean")).isSameAs(context.getBean("testBean"));
    assertThat(context.getBean(String.class)).isSameAs(context.getBean("testBean"));
    assertThat(context.getBean(CharSequence.class)).isSameAs(context.getBean("testBean"));
    assertThat(context.getBean("testBean")).isEqualTo(context.toString());
  }

  @Test
  void withScopedSupplier() {
    context.registerBeanDefinition("testBean",
            new RootBeanDefinition(String.class, BeanDefinition.SCOPE_PROTOTYPE, context::toString));
    context.refresh();

    assertThat(context.getBean("testBean")).isNotSameAs(context.getBean("testBean"));
    assertThat(context.getBean(String.class)).isEqualTo(context.getBean("testBean"));
    assertThat(context.getBean(CharSequence.class)).isEqualTo(context.getBean("testBean"));
    assertThat(context.getBean("testBean")).isEqualTo(context.toString());
  }

  @Test
  void accessAfterClosing() {
    context.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
    context.refresh();

    assertThat(context.getBean(String.class)).isSameAs(context.getBean("testBean"));
    assertThat(context.getAutowireCapableBeanFactory().getBean(String.class))
            .isSameAs(context.getAutowireCapableBeanFactory().getBean("testBean"));

    context.close();

    assertThatIllegalStateException()
            .isThrownBy(() -> context.getBean(String.class));
    assertThatIllegalStateException()
            .isThrownBy(() -> context.getAutowireCapableBeanFactory().getBean(String.class));
    assertThatIllegalStateException()
            .isThrownBy(() -> context.getAutowireCapableBeanFactory().getBean("testBean"));
  }

  @Test
  void individualBeans() {
    context.registerBean(BeanA.class);
    context.registerBean(BeanB.class);
    context.registerBean(BeanC.class);
    context.refresh();

    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeans() {
    context.registerBean("a", BeanA.class);
    context.registerBean("b", BeanB.class);
    context.registerBean("c", BeanC.class);
    context.refresh();

    assertThat(context.getBean("a", BeanA.class).b).isSameAs(context.getBean("b"));
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
    assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualBeanWithSupplier() {
    context.registerBean(BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isTrue();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);

    assertThat(context.getBeanFactory().getDependentBeans(BeanB.class.getName()))
            .containsExactly(BeanA.class.getName());
    assertThat(context.getBeanFactory().getDependentBeans(BeanC.class.getName()))
            .containsExactly(BeanA.class.getName());
  }

  @Test
  void individualBeanWithSupplierAndCustomizer() {
    context.registerBean(BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
            bd -> bd.setLazyInit(true));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isFalse();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeanWithSupplier() {
    context.registerBean("a", BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("a")).isTrue();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
    assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeanWithSupplierAndCustomizer() {
    context.registerBean("a", BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
            bd -> bd.setLazyInit(true));
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("a")).isFalse();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
    assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualBeanWithNullReturningSupplier() {
    context.registerBean("a", BeanA.class, () -> null);
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanNamesForType(BeanA.class)).containsExactly("a");
    assertThat(context.getBeanNamesForType(BeanB.class)).containsExactly("b");
    assertThat(context.getBeanNamesForType(BeanC.class)).containsExactly("c");
    assertThat(context.getBeansOfType(BeanA.class)).isEmpty();
    assertThat(context.getBeansOfType(BeanB.class).values().iterator().next())
            .isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBeansOfType(BeanC.class).values().iterator().next())
            .isSameAs(context.getBean(BeanC.class));
  }

  @Test
  void getResourceWithDefaultResourceLoader() {
    assertGetResourceSemantics(null, ClassPathResource.class);
  }

  @Test
  void getResourceWithCustomResourceLoader() {
    assertGetResourceSemantics(new FileSystemResourceLoader(), FileSystemResource.class);
  }

  private void assertGetResourceSemantics(ResourceLoader resourceLoader, Class<? extends Resource> defaultResourceType) {
    if (resourceLoader != null) {
      context.setResourceLoader(resourceLoader);
    }

    String relativePathLocation = "foo";
    String fileLocation = "file:foo";
    String pingLocation = "ping:foo";

    Resource resource = context.getResource(relativePathLocation);
    assertThat(resource).isInstanceOf(defaultResourceType);
    resource = context.getResource(fileLocation);
    assertThat(resource).isInstanceOf(FileUrlResource.class);

    // If we are using a FileSystemResourceLoader on Windows, we expect an error
    // similar to the following since "ping:foo" is not a valid file name in the
    // Windows file system and since the PingPongProtocolResolver has not yet been
    // registered.
    //
    // java.nio.file.InvalidPathException: Illegal char <:> at index 4: ping:foo
    if (resourceLoader instanceof FileSystemResourceLoader && OS.WINDOWS.isCurrentOs()) {
      assertThatExceptionOfType(InvalidPathException.class)
              .isThrownBy(() -> context.getResource(pingLocation))
              .withMessageContaining(pingLocation);
    }
    else {
      resource = context.getResource(pingLocation);
      assertThat(resource).isInstanceOf(defaultResourceType);
    }

    context.addProtocolResolver(new PingPongProtocolResolver());

    resource = context.getResource(relativePathLocation);
    assertThat(resource).isInstanceOf(defaultResourceType);
    resource = context.getResource(fileLocation);
    assertThat(resource).isInstanceOf(FileUrlResource.class);
    resource = context.getResource(pingLocation);
    assertThat(resource).asInstanceOf(type(ByteArrayResource.class))
            .extracting(bar -> new String(bar.getByteArray(), UTF_8))
            .isEqualTo("pong:foo");
  }

  private RootBeanDefinition getBeanDefinition(GenericApplicationContext context, String name) {
    return (RootBeanDefinition) context.getBeanFactory().getMergedBeanDefinition(name);
  }

  static class BeanA {

    BeanB b;
    BeanC c;

    public BeanA(BeanB b, BeanC c) {
      this.b = b;
      this.c = c;
    }
  }

  static class BeanB implements ApplicationContextAware {

    ApplicationContext applicationContext;

    public BeanB() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }
  }

  static class BeanC { }

  static class BeanD {

    @SuppressWarnings("unused")
    private Integer counter;

    BeanD(Integer counter) {
      this.counter = counter;
    }

    public BeanD() {
    }

    public void setCounter(Integer counter) {
      this.counter = counter;
    }

  }

  static class TestAotFactoryBean<T> extends AbstractFactoryBean<T> {

    TestAotFactoryBean() {
      throw new IllegalStateException("FactoryBean should not be instantied early");
    }

    @Override
    public Class<?> getObjectType() {
      return Object.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected T createBeanInstance() {
      return (T) new Object();
    }
  }

  static class PingPongProtocolResolver implements ProtocolResolver {

    @Override
    public Resource resolve(String location, ResourceLoader resourceLoader) {
      if (location.startsWith("ping:")) {
        return new ByteArrayResource(("pong:" + location.substring(5)).getBytes(UTF_8));
      }
      return null;
    }
  }

}
