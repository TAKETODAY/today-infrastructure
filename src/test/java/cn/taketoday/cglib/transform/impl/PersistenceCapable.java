
package cn.taketoday.cglib.transform.impl;

/**
 *
 * @author baliuka
 */
public interface PersistenceCapable {

    void setPersistenceManager(Object manager);

    Object getPersistenceManager();
}
