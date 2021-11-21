package cn.taketoday.beans.dependency;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.CollectionUtils;

import java.util.Map;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/17 16:13
 */
@SuppressWarnings("rawtypes")
public class MapBeanDependencyResolver implements DependencyResolvingStrategy {

  @Override
  public void resolveDependency(InjectionPoint injectionPoint, DependencyResolvingContext context) {
    BeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory != null
            && !context.hasDependency()
            && Map.class.isAssignableFrom(injectionPoint.getDependencyType())) { // TODO Properties 排除？

      Class<?> type = injectionPoint.getDependencyType();
      Map beansOfType = getBeansOfType(injectionPoint, beanFactory);
      Map convert = adaptMap(beansOfType, type);
      context.setDependency(convert);
    }
  }

  protected Map getBeansOfType(InjectionPoint injectionPoint, BeanFactory beanFactory) {
    ResolvableType resolvableType = injectionPoint.getResolvableType();
    ResolvableType generic = resolvableType.asMap().getGeneric(1);
    Class<?> beanClass = generic.toClass();
    return beanFactory.getBeansOfType(beanClass);
  }

  @SuppressWarnings("unchecked")
  public static Map adaptMap(Map map, Class<?> type) {
    if (type != Map.class) {
      Map newMap = CollectionUtils.createMap(type, map.size());
      newMap.putAll(map);
      map = newMap;
    }
    return map;
  }

}
