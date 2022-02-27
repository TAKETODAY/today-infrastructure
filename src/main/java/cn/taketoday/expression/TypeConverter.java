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

/**
 * A convenient class for writing an ELResolver to do custom type conversions.
 *
 * <p>
 * For example, to convert a String to an instance of MyDate, one can write
 * <blockquote>
 *
 * <pre>
 *     ELProcessor elp = new ELProcessor();
 *     elp.getELManager().addELResolver(new TypeConverter() {
 *         Object convertToType(ELContext context, Object obj, Class<?> type) {
 *             if ((obj instanceof String) && type == MyDate.class) {
 *                 context.setPropertyResolved(obj, type);
 *                 return (obj == null)? null: new MyDate(obj.toString());
 *             }
 *             return null;
 *         }
 *      };
 * </pre>
 *
 * </blockquote>
 *
 * @since EL 3.0
 */
public abstract class TypeConverter extends ExpressionResolver {

  @Override
  public Object getValue(ExpressionContext context, Object base, Object property) {
    return null;
  }

  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    return null;
  }

  @Override
  public void setValue(ExpressionContext context, Object base, Object property, Object value) { }

  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    return false;
  }

  /**
   * Converts an object to a specific type.
   *
   * <p>
   * An <code>ELException</code> is thrown if an error occurs during the
   * conversion.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param obj The object to convert.
   * @param targetType The target type for the conversion.
   * @throws ExpressionException thrown if errors occur.
   */
  @Override
  public abstract Object convertToType(ExpressionContext context, Object obj, Class<?> targetType);
}
