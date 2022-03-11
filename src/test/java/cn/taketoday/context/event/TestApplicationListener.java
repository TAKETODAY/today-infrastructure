package cn.taketoday.context.event;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;

/**
 * Listener that maintains a global count of events.
 *
 * @author Rod Johnson
 * @since January 21, 2001
 */
public class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

  private int eventCount;

  public int getEventCount() {
    return eventCount;
  }

  public void zeroCounter() {
    eventCount = 0;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent e) {
    ++eventCount;
  }

}
