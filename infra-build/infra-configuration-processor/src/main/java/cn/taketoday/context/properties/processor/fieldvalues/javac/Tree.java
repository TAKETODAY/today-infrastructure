/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.processor.fieldvalues.javac;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Reflection based access to {@code com.sun.source.tree.Tree}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class Tree extends ReflectionWrapper {

  private final Class<?> treeVisitorType = findClass("com.sun.source.tree.TreeVisitor");

  private final Method acceptMethod = findMethod("accept", this.treeVisitorType, Object.class);

  private final Method getClassTreeMembers = findMethod(findClass("com.sun.source.tree.ClassTree"), "getMembers");

  Tree(Object instance) {
    super("com.sun.source.tree.Tree", instance);
  }

  void accept(TreeVisitor visitor) throws Exception {
    this.acceptMethod.invoke(getInstance(), Proxy.newProxyInstance(getInstance().getClass().getClassLoader(),
            new Class<?>[] { this.treeVisitorType }, new TreeVisitorInvocationHandler(visitor)), 0);
  }

  /**
   * {@link InvocationHandler} to call the {@link TreeVisitor}.
   */
  private class TreeVisitorInvocationHandler implements InvocationHandler {

    private TreeVisitor treeVisitor;

    TreeVisitorInvocationHandler(TreeVisitor treeVisitor) {
      this.treeVisitor = treeVisitor;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("visitClass") && (Integer) args[1] == 0) {
        Iterable members = (Iterable) Tree.this.getClassTreeMembers.invoke(args[0]);
        for (Object member : members) {
          if (member != null) {
            Tree.this.acceptMethod.invoke(member, proxy, ((Integer) args[1]) + 1);
          }
        }
      }
      if (method.getName().equals("visitVariable")) {
        this.treeVisitor.visitVariable(new VariableTree(args[0]));
      }
      return null;
    }

  }

}
