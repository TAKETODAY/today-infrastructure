
package cn.taketoday.context.cglib.transform.impl;

/**
 *
 * @author baliuka
 */
public interface PersistenceCapable {

    void setPersistenceManager(Object manager);

    Object getPersistenceManager();
}
