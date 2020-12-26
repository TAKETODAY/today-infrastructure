package cn.taketoday.web.handler;

import java.util.List;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY
 * @date 2020/12/23 21:53
 */
public class CompositeHandlerExceptionHandler
        extends OrderedSupport implements HandlerExceptionHandler {

  private List<HandlerExceptionHandler> handlers;

  public CompositeHandlerExceptionHandler() {}

  public CompositeHandlerExceptionHandler(final List<HandlerExceptionHandler> handlers) {
    this.handlers = handlers;
  }

  /**
   * Set the list of exception resolvers to delegate to.
   */
  public void setExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    this.handlers = handlers;
  }

  /**
   * Return the list of exception resolvers to delegate to.
   */
  public List<HandlerExceptionHandler> getExceptionHandlers() {
    return this.handlers;
  }

  @Override
  public Object handleException(final RequestContext context,
                                final Throwable exception, final Object handler) throws Throwable {

    if (this.handlers != null) {
      for (final HandlerExceptionHandler exceptionHandler : handlers) {
        final Object view = exceptionHandler.handleException(context, exception, handler);
        if (view != null) {
          return view;
        }
      }
    }
    return HandlerAdapter.NONE_RETURN_VALUE;
  }
}
