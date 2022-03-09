package cn.taketoday.aop.target;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.ProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyCreationTargetSourceTests {

  @Test
  public void testCreateLazy() {
    TargetSource targetSource = new AbstractLazyCreationTargetSource() {
      @Override
      protected Object createObject() {
        return new InitCountingBean();
      }

      @Override
      public Class<?> getTargetClass() {
        return InitCountingBean.class;
      }
    };

    InitCountingBean proxy = (InitCountingBean) ProxyFactory.getProxy(targetSource);
    assertThat(InitCountingBean.initCount).as("Init count should be 0").isEqualTo(0);
    assertThat(targetSource.getTargetClass()).as("Target class incorrect").isEqualTo(InitCountingBean.class);
    assertThat(InitCountingBean.initCount).as("Init count should still be 0 after getTargetClass()").isEqualTo(0);

    proxy.doSomething();
    assertThat(InitCountingBean.initCount).as("Init count should now be 1").isEqualTo(1);

    proxy.doSomething();
    assertThat(InitCountingBean.initCount).as("Init count should still be 1").isEqualTo(1);
  }

  private static class InitCountingBean {

    public static int initCount;

    public InitCountingBean() {
      if (InitCountingBean.class.equals(getClass())) {
        // only increment when creating the actual target - not the proxy
        initCount++;
      }
    }

    public void doSomething() {
      //no-op
    }
  }

}
