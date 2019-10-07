package cn.taketoday.framework.aware;

import cn.taketoday.context.aware.Aware;
import cn.taketoday.framework.WebServerApplicationContext;

/**
 * @author Today <br>
 * 
 *         2018-09-14 20:17
 */
public interface WebServerApplicationContextAware extends Aware {

    /**
     * 
     * @param applicationContext
     */
    void setWebServerApplicationContext(WebServerApplicationContext applicationContext);
}
