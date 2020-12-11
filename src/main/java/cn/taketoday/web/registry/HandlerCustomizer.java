package cn.taketoday.web.registry;

/**
 * @author TODAY
 * @date 2020/12/10 23:30
 */
public interface HandlerCustomizer {

  /**
   * @param handler
   *         http handler
   *
   * @return a modified handler
   */
  Object customize(Object handler);
}
