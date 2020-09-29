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
 *
 * @author Kin-man Chung
 */

package cn.taketoday.expression.stream;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionResolver;

import static java.util.Objects.requireNonNull;

/**
 * This ELResolver intercepts method calls to a Collections, to provide support
 * for collection operations.
 *
 * @author TODAY <br>
 * 2019-02-20 17:21
 */
public class StreamExpressionResolver extends ExpressionResolver {

  private static final StreamExpressionResolver INSTANCE = new StreamExpressionResolver();

  public final static StreamExpressionResolver getInstance() {
    return INSTANCE;
  }

  @SuppressWarnings("unchecked")
  public Object invoke(final ExpressionContext context, final Object base, final Object method, //
                       final Class<?>[] paramTypes, final Object[] params) //
  {
//        if (base instanceof Collection
//            && "stream".equals(method)
//            && ObjectUtils.isEmpty(params)) {
//            requireNonNull(context).setPropertyResolved(true);
//            return new Stream(((Collection<Object>) base).iterator());
//        }
//        if (base.getClass().isArray()
//            && "stream".equals(method)
//            && ObjectUtils.isEmpty(params)) {
//            requireNonNull(context).setPropertyResolved(true);
//            return new Stream(arrayIterator(base));
//        }
    if ("stream".equals(method) && ObjectUtils.isEmpty(params)) {
      requireNonNull(context).setPropertyResolved(true);
      if (base.getClass().isArray()) {
        return new Stream(arrayIterator(base));
      }
      if (base instanceof Collection) {
        return new Stream(((Collection<Object>) base).iterator());
      }
    }
    return null;
  }

  private static Iterator<Object> arrayIterator(final Object base) {
    final int size = Array.getLength(base);
    return new Iterator<Object>() {

      private int index = 0;
      private boolean yielded;
      private Object current;

      @Override
      public boolean hasNext() {
        if ((!yielded) && index < size) {
          current = Array.get(base, index++);
          yielded = true;
        }
        return yielded;
      }

      @Override
      public Object next() {
        yielded = false;
        return current;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /*
   * private LambdaExpression getLambda(Object obj, String method) { if (obj ==
   * null || ! (obj instanceof LambdaExpression)) { throw new ELException
   * ("When calling " + method + ", expecting an " +
   * "EL lambda expression, but found " + obj); } return (LambdaExpression) obj; }
   */
  public Object getValue(ExpressionContext context, Object base, Object property) {
    return null;
  }

  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    return null;
  }

  public void setValue(ExpressionContext context, Object base, Object property, Object value) {}

  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    return false;
  }

}
