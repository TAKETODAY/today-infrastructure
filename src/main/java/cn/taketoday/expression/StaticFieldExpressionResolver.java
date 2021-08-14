/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package cn.taketoday.expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.core.utils.ReflectionUtils;

import static cn.taketoday.expression.util.ReflectionUtil.findConstructor;
import static cn.taketoday.expression.util.ReflectionUtil.findMethod;
import static cn.taketoday.expression.util.ReflectionUtil.invokeConstructor;
import static cn.taketoday.expression.util.ReflectionUtil.invokeMethod;

/**
 * <p>
 * An {@link ExpressionResolver} for resolving static fields, enum constants and static
 * methods. Also handles constructor calls as a special case.
 * </p>
 * <p>
 * The resolver handles base objects of the type {@link Class}, which is
 * usually generated by an EL implementation.
 * </p>
 *
 * @since EL 3.0
 */
public class StaticFieldExpressionResolver extends ExpressionResolver {

  /**
   * <p>
   * Returns the value of a static field.
   * </p>
   * <p>
   * If the base object is an instance of <code>ELClass</code> and the property is
   * String, the <code>propertyResolved</code> property of the
   * <code>ELContext</code> object must be set to <code>true</code> by this
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller should ignore the return value.
   * </p>
   *
   * If the property is a public static field of class specified in
   * <code>ELClass</code>, return the value of the static field. An Enum constant
   * is a public static field of an Enum object, and is a special case of this.
   *
   * @param context
   *         The context of this evaluation.
   * @param base
   *         An <code>ELClass</code>.
   * @param property
   *         A static field name.
   *
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the static
   * field value.
   *
   * @throws NullPointerException
   *         if context is <code>null</code>.
   * @throws PropertyNotFoundException
   *         if the specified class does not exist, or if the field is not a
   *         public static filed of the class, or if the field is
   *         inaccessible.
   */
  @Override
  public Object getValue(ExpressionContext context, Object base, Object property) {
    if (base instanceof Class && property instanceof String) {

      context.setPropertyResolved(base, property);
      final Field field = ReflectionUtils.findField((Class<?>) base, (String) property);
      if (field == null) {
        throw new PropertyNotFoundException(
                ((Class<?>) base).getName() + " there is not a property: '" + property + '\'');
      }
      try {
        final int mod = field.getModifiers();
        if (Modifier.isPublic(mod) && Modifier.isStatic(mod)) {
          return field.get(null);
        }
      }
      catch (IllegalAccessException ex) {
        throw new PropertyNotFoundException(
                "Either '" + ((Class<?>) base).getName() +
                        "' is not a public static field of the class '" + property + "' or field is inaccessible");
      }
    }
    return null;
  }

  /**
   * <p>
   * Attempts to write to a static field.
   * </p>
   * <p>
   * If the base object is an instance of <code>ELClass</code>and the property is
   * String, a <code>PropertyNotWritableException</code> will always be thrown,
   * because writing to a static field is not allowed.
   *
   * @param context
   *         The context of this evaluation.
   * @param base
   *         An <code>ELClass</code>
   * @param property
   *         The name of the field
   * @param value
   *         The value to set the field of the class to.
   *
   * @throws NullPointerException
   *         if context is <code>null</code>
   * @throws PropertyNotWritableException
   */
  @Override
  public void setValue(ExpressionContext context, Object base, Object property, Object value) {

    if (base instanceof Class && property instanceof String) {

      Class<?> klass = ((Class<?>) base);
      String fieldName = (String) property;

      throw new PropertyNotWritableException("Cannot write to the field '" + klass.getName() + "' of the class '" + fieldName + "'");
    }
  }

  /**
   * <p>
   * Invokes a public static method or the constructor for a class.
   * </p>
   *
   * If the base object is an instance of <code>ELClass</code> and the method is a
   * String, the <code>propertyResolved</code> property of the
   * <code>ELContext</code> object must be set to <code>true</code> by the
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller should ignore the return value.
   * </p>
   * <p>
   * Invoke the public static method specified by <code>method</code>.
   * </p>
   * <p>
   * The process involved in the method selection is the same as that used in
   * {@link BeanExpressionResolver}.
   * </p>
   *
   * <p>
   * As a special case, if the name of the method is "&lt;init>", the constructor
   * for the class will be invoked.
   * </p>
   *
   * @param base
   *         An <code>ELClass</code>
   * @param method
   *         When coerced to a <code>String</code>, the simple name of the
   *         method.
   * @param paramTypes
   *         An array of Class objects identifying the method's formal
   *         parameter types, in declared order. Use an empty array if the
   *         method has no parameters. Can be <code>null</code>, in which case
   *         the method's formal parameter types are assumed to be unknown.
   * @param params
   *         The parameters to pass to the method, or <code>null</code> if no
   *         parameters.
   *
   * @return The result of the method invocation (<code>null</code> if the method
   * has a <code>void</code> return type).
   *
   * @throws MethodNotFoundException
   *         if no suitable method can be found.
   * @throws ExpressionException
   *         if an exception was thrown while performing (base, method)
   *         resolution. The thrown exception must be included as the cause
   *         property of this exception, if available. If the exception thrown
   *         is an <code>InvocationTargetException</code>, extract its
   *         <code>cause</code> and pass it to the <code>ELException</code>
   *         constructor.
   */
  @Override
  public Object invoke(
          ExpressionContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {

    if (!(base instanceof Class && method instanceof String)) {
      return null;
    }
    Class<?> klass = ((Class<?>) base);
    String name = (String) method;

    Object ret;
    if ("<init>".equals(name)) {
      Constructor<?> constructor = findConstructor(klass, paramTypes, params);
      ret = invokeConstructor(context, constructor, params);
    }
    else {
      Method meth = findMethod(klass, name, paramTypes, params, true);
      ret = invokeMethod(context, meth, null, params);
    }
    context.setPropertyResolved(base, method);
    return ret;
  }

  /**
   * <p>
   * Returns the type of a static field.
   * </p>
   * <p>
   * If the base object is an instance of <code>ELClass</code>and the property is
   * a String, the <code>propertyResolved</code> property of the
   * <code>ELContext</code> object must be set to <code>true</code> by the
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller can safely assume no value has been set.
   * </p>
   *
   * If the property string is a public static field of class specified in
   * ELClass, return the type of the static field.
   * </p>
   *
   * @param context
   *         The context of this evaluation.
   * @param base
   *         An <code>ELClass</code>.
   * @param property
   *         The name of the field.
   *
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the type of
   * the type of the field.
   *
   * @throws NullPointerException
   *         if context is <code>null</code>.
   * @throws PropertyNotFoundException
   *         if field is not a public static filed of the class, or if the
   *         field is inaccessible.
   */
  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {

    if (base instanceof Class && property instanceof String) {
      try {

        context.setPropertyResolved(true);
        final Field field = ((Class<?>) base).getField((String) property);
        return field.getType();
      }
      catch (NoSuchFieldException ex) {
        throw new PropertyNotFoundException("Either '" + ((Class<?>) base).getName() + //
                                                    "' is not a public static field of the class '" + property + "' or field is inaccessible");
      }
    }
    return null;
  }

  /**
   * <p>
   * Inquires whether the static field is writable.
   * </p>
   * <p>
   * If the base object is an instance of <code>ELClass</code>and the property is
   * a String, the <code>propertyResolved</code> property of the
   * <code>ELContext</code> object must be set to <code>true</code> by the
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller can safely assume no value has been set.
   * </p>
   *
   * <p>
   * Always returns a <code>true</code> because writing to a static field is not
   * allowed.
   * </p>
   *
   * @param context
   *         The context of this evaluation.
   * @param base
   *         An <code>ELClass</code>.
   * @param property
   *         The name of the bean.
   *
   * @return <code>true</code>
   *
   * @throws NullPointerException
   *         if context is <code>null</code>.
   */
  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {

    if (base instanceof Class && property instanceof String) {
      context.setPropertyResolved(true);
    }
    return true;
  }

}
