package cn.taketoday.web.framework.reactive;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestThreadLocal;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * Netty fast ThreadLocal
 *
 * @author TODAY 2021/4/2 17:17
 * @see FastThreadLocal
 */
public final class FastRequestThreadLocal extends RequestThreadLocal {
  private final FastThreadLocal<RequestContext> threadLocal = new FastThreadLocal<>();

  @Override
  public void remove() {
    threadLocal.remove();
  }

  @Override
  public RequestContext get() {
    return threadLocal.get();
  }

  @Override
  public void set(RequestContext context) {
    threadLocal.set(context);
  }
}
