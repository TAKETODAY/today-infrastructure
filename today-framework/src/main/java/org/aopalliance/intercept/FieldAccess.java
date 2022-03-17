package org.aopalliance.intercept;

import java.lang.reflect.Field;

/**
 * This interface represents a field access in the program.
 *
 * <p>
 * A field access is a joinpoint and can be intercepted by a field interceptor.
 *
 * @see FieldInterceptor
 */
public interface FieldAccess extends Joinpoint {

  /** The read access type (see {@link #getAccessType()}). */
  int READ = 0;
  /** The write access type (see {@link #getAccessType()}). */
  int WRITE = 1;

  /**
   * Gets the field being accessed.
   *
   * <p>
   * This method is a frienly implementation of the
   * {@link Joinpoint#getStaticPart()} method (same result).
   *
   * @return the field being accessed.
   */
  Field getField();

  /**
   * Gets the value that must be set to the field.
   *
   * <p>
   * This value can be intercepted and changed by a field interceptor.
   */
  Object getValueToSet();

  /**
   * Returns the access type.
   *
   * @return FieldAccess.READ || FieldAccess.WRITE
   */
  int getAccessType();

}
