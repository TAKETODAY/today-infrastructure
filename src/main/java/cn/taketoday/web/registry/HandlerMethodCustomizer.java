package cn.taketoday.web.registry;

import cn.taketoday.web.handler.HandlerMethod;

/**
 * @author TODAY
 * @date 2020/12/12 0:02
 */
public interface HandlerMethodCustomizer extends HandlerCustomizer {

  @Override
  default Object customize(Object handler) {
    return handler instanceof HandlerMethod
           ? customize((HandlerMethod) handler)
           : handler;
  }

  /**
   * @param handlerMethod
   *         HandlerMethod
   *
   * @return a modified handler
   */
  Object customize(HandlerMethod handlerMethod);
}
