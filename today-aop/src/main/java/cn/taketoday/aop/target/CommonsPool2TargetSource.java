/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.aop.target;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.Serial;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link TargetSource} implementation that holds objects in a
 * configurable Apache Commons2 Pool.
 *
 * <p>By default, an instance of {@code GenericObjectPool} is created.
 * Subclasses may change the type of {@code ObjectPool} used by
 * overriding the {@code createObjectPool()} method.
 *
 * <p>Provides many configuration properties mirroring those of the Commons Pool
 * {@code GenericObjectPool} class; these properties are passed to the
 * {@code GenericObjectPool} during construction. If creating a subclass of this
 * class to change the {@code ObjectPool} implementation type, pass in the values
 * of configuration properties that are relevant to your chosen implementation.
 *
 * <p>The {@code testOnBorrow}, {@code testOnReturn} and {@code testWhileIdle}
 * properties are explicitly not mirrored because the implementation of
 * {@code PoolableObjectFactory} used by this class does not implement
 * meaningful validation. All exposed Commons Pool properties use the
 * corresponding Commons Pool defaults.
 *
 * <p>Compatible with Apache Commons Pool 2.4
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GenericObjectPool
 * @see #createObjectPool()
 * @see #setMaxSize
 * @see #setMaxIdle
 * @see #setMinIdle
 * @see #setMaxWait
 * @see #setTimeBetweenEvictionRunsMillis
 * @see #setMinEvictableIdleTimeMillis
 * @since 4.0 2021/12/13 22:33
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonsPool2TargetSource
        extends AbstractPoolingTargetSource implements PooledObjectFactory<Object> {

  @Serial
  private static final long serialVersionUID = 1L;

  private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;
  private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;
  private long maxWait = GenericObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS;

  private boolean blockWhenExhausted = GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;
  private long minEvictableIdleTimeMillis = GenericObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
  private long timeBetweenEvictionRunsMillis = GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

  /**
   * The Apache Commons {@code ObjectPool} used to pool target objects.
   */
  @Nullable
  private ObjectPool pool;

  /**
   * Create a CommonsPoolTargetSource with default settings.
   * Default maximum size of the pool is 8.
   *
   * @see #setMaxSize
   * @see GenericObjectPoolConfig#setMaxTotal
   */
  public CommonsPool2TargetSource() {
    setMaxSize(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL);
  }

  /**
   * Set the maximum number of idle objects in the pool.
   * Default is 8.
   *
   * @see GenericObjectPool#setMaxIdle
   */
  public void setMaxIdle(int maxIdle) {
    this.maxIdle = maxIdle;
  }

  /**
   * Return the maximum number of idle objects in the pool.
   */
  public int getMaxIdle() {
    return this.maxIdle;
  }

  /**
   * Set the minimum number of idle objects in the pool.
   * Default is 0.
   *
   * @see GenericObjectPool#setMinIdle
   */
  public void setMinIdle(int minIdle) {
    this.minIdle = minIdle;
  }

  /**
   * Return the minimum number of idle objects in the pool.
   */
  public int getMinIdle() {
    return this.minIdle;
  }

  /**
   * Set the maximum waiting time for fetching an object from the pool.
   * Default is -1, waiting forever.
   *
   * @see GenericObjectPool#setMaxWaitMillis
   */
  public void setMaxWait(long maxWait) {
    this.maxWait = maxWait;
  }

  /**
   * Return the maximum waiting time for fetching an object from the pool.
   */
  public long getMaxWait() {
    return this.maxWait;
  }

  /**
   * Set the time between eviction runs that check idle objects whether
   * they have been idle for too long or have become invalid.
   * Default is -1, not performing any eviction.
   *
   * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
   */
  public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
    this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
  }

  /**
   * Return the time between eviction runs that check idle objects.
   */
  public long getTimeBetweenEvictionRunsMillis() {
    return this.timeBetweenEvictionRunsMillis;
  }

  /**
   * Set the minimum time that an idle object can sit in the pool before
   * it becomes subject to eviction. Default is 1800000 (30 minutes).
   * <p>Note that eviction runs need to be performed to take this
   * setting into effect.
   *
   * @see #setTimeBetweenEvictionRunsMillis
   * @see GenericObjectPool#setMinEvictableIdleTimeMillis
   */
  public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
    this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
  }

  /**
   * Return the minimum time that an idle object can sit in the pool.
   */
  public long getMinEvictableIdleTimeMillis() {
    return this.minEvictableIdleTimeMillis;
  }

  /**
   * Set whether the call should bock when the pool is exhausted.
   */
  public void setBlockWhenExhausted(boolean blockWhenExhausted) {
    this.blockWhenExhausted = blockWhenExhausted;
  }

  /**
   * Specify if the call should block when the pool is exhausted.
   */
  public boolean isBlockWhenExhausted() {
    return this.blockWhenExhausted;
  }

  /**
   * Creates and holds an ObjectPool instance.
   *
   * @see #createObjectPool()
   */
  @Override
  protected final void createPool() {
    logger.debug("Creating Commons object pool");
    this.pool = createObjectPool();
  }

  /**
   * Subclasses can override this if they want to return a specific Commons pool.
   * They should apply any configuration properties to the pool here.
   * <p>Default is a GenericObjectPool instance with the given pool size.
   *
   * @return an empty Commons {@code ObjectPool}.
   * @see GenericObjectPool
   * @see #setMaxSize
   */
  protected ObjectPool createObjectPool() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(getMaxSize());
    config.setMaxIdle(getMaxIdle());
    config.setMinIdle(getMinIdle());
    config.setMaxWaitMillis(getMaxWait());
    config.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
    config.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
    config.setBlockWhenExhausted(isBlockWhenExhausted());
    return new GenericObjectPool(this, config);
  }

  /**
   * Borrows an object from the {@code ObjectPool}.
   */
  @Override
  public Object getTarget() throws Exception {
    Assert.state(this.pool != null, "No Commons ObjectPool available");
    return this.pool.borrowObject();
  }

  /**
   * Returns the specified object to the underlying {@code ObjectPool}.
   */
  @Override
  public void releaseTarget(Object target) throws Exception {
    if (this.pool != null) {
      this.pool.returnObject(target);
    }
  }

  @Override
  public int getActiveCount() throws UnsupportedOperationException {
    return this.pool != null ? this.pool.getNumActive() : 0;
  }

  @Override
  public int getIdleCount() throws UnsupportedOperationException {
    return this.pool != null ? this.pool.getNumIdle() : 0;
  }

  /**
   * Closes the underlying {@code ObjectPool} when destroying this object.
   */
  @Override
  public void destroy() throws Exception {
    if (this.pool != null) {
      logger.debug("Closing Commons ObjectPool");
      this.pool.close();
    }
  }

  //----------------------------------------------------------------------------
  // Implementation of org.apache.commons.pool2.PooledObjectFactory interface
  //----------------------------------------------------------------------------

  @Override
  public PooledObject<Object> makeObject() throws Exception {
    return new DefaultPooledObject<>(newPrototypeInstance());
  }

  @Override
  public void destroyObject(PooledObject<Object> p) {
    destroyPrototypeInstance(p.getObject());
  }

  @Override
  public boolean validateObject(PooledObject<Object> p) {
    return true;
  }

  @Override
  public void activateObject(PooledObject<Object> p) { }

  @Override
  public void passivateObject(PooledObject<Object> p) { }

}
