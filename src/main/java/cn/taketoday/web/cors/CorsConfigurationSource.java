package cn.taketoday.web.cors;

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY
 * @date 2020/12/8 22:29
 */
public interface CorsConfigurationSource {

  /**
   * Return a {@link CorsConfiguration} based on the incoming request.
   * @return the associated {@link CorsConfiguration}, or {@code null} if none
   */
  CorsConfiguration getCorsConfiguration(RequestContext request);

}
