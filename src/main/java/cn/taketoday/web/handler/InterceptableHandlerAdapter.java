package cn.taketoday.web.handler;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptorsCapable;

/**
 * @author TODAY
 * @date 2020/12/10 22:51
 */
public abstract class InterceptableHandlerAdapter
        extends AbstractHandlerAdapter implements HandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(InterceptableHandlerAdapter.class);

  @Override
  public final Object handle(final RequestContext context, final Object handler) throws Throwable {

    if(handler instanceof HandlerInterceptorsCapable) {
      final HandlerInterceptor[] interceptors = ((HandlerInterceptorsCapable) handler).getInterceptors();
      if (interceptors != null) {
        // before
        for (final HandlerInterceptor intercepter : interceptors) {
          if (!intercepter.beforeProcess(context, handler)) {
            if (log.isDebugEnabled()) {
              log.debug("Interceptor: [{}] return false", intercepter);
            }
            return HandlerAdapter.NONE_RETURN_VALUE;
          }
        }
        // handle
        final Object result = handleInternal(context, handler);
        // after
        for (final HandlerInterceptor intercepter : interceptors) {
          intercepter.afterProcess(context, handler, result);
        }
        return result;
      }
    }
    return handleInternal(context, handler);
  }

  protected abstract Object handleInternal(final RequestContext context,
                                           final Object handler) throws Throwable;


}
