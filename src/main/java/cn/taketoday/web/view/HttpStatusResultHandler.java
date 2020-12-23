package cn.taketoday.web.view;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.http.HttpStatus;

/**
 * @author TODAY
 * @date 2020/12/23 20:12
 */
public class HttpStatusResultHandler
        extends HandlerMethodResultHandler implements RuntimeResultHandler {

  @Override
  protected boolean supports(final HandlerMethod handler) {
    return handler.is(HttpStatus.class);
  }

  @Override
  public boolean supportsResult(final Object result) {
    return result instanceof HttpStatus;
  }

  @Override
  public void handleResult(final RequestContext context,
                           final Object handler, final Object result) throws Throwable {
    if (result instanceof HttpStatus) {
      context.status((HttpStatus) result);
    }
  }

}
