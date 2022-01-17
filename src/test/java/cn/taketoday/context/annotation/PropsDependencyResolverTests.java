package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import cn.taketoday.beans.factory.support.DependencyDescriptor;
import cn.taketoday.beans.factory.support.DependencyResolvingContext;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.env.MapPropertyResolver;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/14 10:20
 */
class PropsDependencyResolverTests {

  @Props
  public void props() { }

  @Props
  public String propsNotSupports() {
    return null;
  }

  public void voidNotSupports() { }

  public String notSupports() {
    return null;
  }

  @Test
  void supports() throws NoSuchMethodException {
    Method props = PropsDependencyResolverTests.class.getMethod("props");
    Method notSupports = PropsDependencyResolverTests.class.getMethod("notSupports");
    Method voidNotSupports = PropsDependencyResolverTests.class.getMethod("voidNotSupports");
    Method propsNotSupports = PropsDependencyResolverTests.class.getMethod("propsNotSupports");

    PropsDependencyResolver resolver = new PropsDependencyResolver(new PropsReader());

    assertThat(resolver.supports(props)).isTrue();
    assertThat(resolver.supports(notSupports)).isFalse();
    assertThat(resolver.supports(voidNotSupports)).isFalse();
    assertThat(resolver.supports(propsNotSupports)).isFalse();

  }

  @Test
  void resolveDependency() throws NoSuchMethodException {
    StandardBeanFactory factory = new StandardBeanFactory();
    Method autowired = PropsDependencyResolverTests.class.getDeclaredMethod("autowired", PropsBean.class);

    Map<String, Object> map = Map.of("name", "TODAY", "age", 21);

    MapPropertyResolver environment = new MapPropertyResolver(map);
    PropsReader propsReader = new PropsReader(environment);
    propsReader.setBeanFactory(factory);
    PropsDependencyResolver resolver = new PropsDependencyResolver(propsReader);

    MethodParameter methodParameter = MethodParameter.forExecutable(autowired, 0);
    DependencyDescriptor descriptor = new DependencyDescriptor(methodParameter, true);

    DependencyResolvingContext context = new DependencyResolvingContext(autowired, factory);
    resolver.resolveDependency(descriptor, context);

    Object dependency = context.getDependency();
    assertThat(dependency)
            .isNotNull()
            .isInstanceOf(PropsBean.class);
    PropsBean propsBean = (PropsBean) dependency;

    assertThat(propsBean.name)
            .isEqualTo("TODAY");

    assertThat(propsBean.age)
            .isEqualTo(21);
  }

  @Props
  void autowired(PropsBean propsBean) {

  }

  @Data
  static class PropsBean {
    int age;
    String name;
  }

}
