package cn.taketoday.web.utils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.servlet.ServletRequestContext;

import static cn.taketoday.web.RequestContextHolder.prepareContext;

/**
 * @author TODAY
 * @date 2020/12/8 23:07
 */
public abstract class ServletUtils {
  // context

  public static RequestContext getRequestContext(ServletRequest request, ServletResponse response) {
    return getRequestContext((HttpServletRequest) request, (HttpServletResponse) response);
  }

  public static RequestContext getRequestContext(HttpServletRequest request, HttpServletResponse response) {
    RequestContext context = RequestContextHolder.getContext();
    if (context == null) {
      context = prepareContext(new ServletRequestContext(request, response));
    }
    return context;
  }

}
