package cn.taketoday.aop.proxy;

import java.lang.reflect.Constructor;

import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.SunReflectionFactoryInstantiator;
import cn.taketoday.core.bytecode.proxy.Callback;
import cn.taketoday.core.bytecode.proxy.Enhancer;
import cn.taketoday.core.bytecode.proxy.Factory;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SunReflectionFactoryInstantiator
 * @since 4.0 2022/1/12 14:03
 */
public class SerializationCglibAopProxy extends CglibAopProxy {

  /**
   * Create a new SerializationCglibAopProxy for the given AOP configuration.
   *
   * @param config the AOP configuration as AdvisedSupport object
   * @throws AopConfigException if the config is invalid. We try to throw an informative
   * exception in this case, rather than let a mysterious failure
   * happen later.
   */
  public SerializationCglibAopProxy(AdvisedSupport config) {
    super(config);
  }

  @Override
  protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) throws Exception {
    if (constructorArgs != null && constructorArgTypes != null) {
      // use constructor
      enhancer.setCallbacks(callbacks);
      return enhancer.create(constructorArgTypes, constructorArgs);
    }
    else {
      Object proxy;
      // use default constructor
      Class<?> proxyClass = enhancer.createClass();
      Constructor<?> constructor = ReflectionUtils.getConstructorIfAvailable(proxyClass);
      if (constructor != null) {
        proxy = constructor.newInstance();
      }
      else {
        // use SunReflectionFactoryInstantiator
        proxy = BeanInstantiator.forSerialization(proxyClass).instantiate();
      }
      if (proxy instanceof Factory) {
        ((Factory) proxy).setCallbacks(callbacks);
      }
      return proxy;
    }
  }

}
