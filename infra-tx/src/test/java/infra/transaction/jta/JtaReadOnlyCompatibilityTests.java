/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.transaction.jta;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import infra.bytecode.ClassReader;
import infra.bytecode.ClassWriter;
import infra.bytecode.Opcodes;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests the reflective integration against isolated JTA 2.1 API signatures. */
class JtaReadOnlyCompatibilityTests {

  @Test
  void readOnlyTransactionBeginsWithFlagAndRollsBack() throws Exception {
    ApiClassLoader loader = new ApiClassLoader();
    List<String> calls = new ArrayList<>();
    Class<?> userTransaction = loader.loadClass("jakarta.transaction.UserTransaction");
    Object user = Proxy.newProxyInstance(loader, new Class<?>[] { userTransaction }, (proxy, method, args) -> {
      if (method.getName().equals("getStatus")) {
        return calls.isEmpty() ? 6 : 0;
      }
      if (method.getName().equals("toString")) {
        return "JTA 2.1 test transaction";
      }
      calls.add(method.getName() + (args != null ? List.of(args) : ""));
      return null;
    });
    Class<?> managerType = loader.loadClass("infra.transaction.jta.JtaTransactionManager");
    Object manager = managerType.getConstructor(userTransaction).newInstance(user);
    managerType.getMethod("setEnforceReadOnly", boolean.class).invoke(manager, true);
    Class<?> templateType = loader.loadClass("infra.transaction.support.TransactionTemplate");
    Object template = templateType.getConstructor(loader.loadClass("infra.transaction.PlatformTransactionManager")).newInstance(manager);
    templateType.getMethod("setReadOnly", boolean.class).invoke(template, true);
    Class<?> callbackType = loader.loadClass("infra.transaction.support.TransactionCallbackWithoutResult");
    Object callback = Proxy.newProxyInstance(loader, new Class<?>[] { callbackType }, (proxy, method, args) -> null);
    templateType.getMethod("executeWithoutResult", callbackType).invoke(template, callback);
    assertThat(calls).containsExactly("begin[true]", "rollback");
  }

  @Test
  void adapterDelegatesReadOnlyMethods() throws Exception {
    ApiClassLoader loader = new ApiClassLoader();
    Class<?> transaction = loader.loadClass("jakarta.transaction.Transaction");
    Object tx = Proxy.newProxyInstance(loader, new Class<?>[] { transaction }, (proxy, method, args) -> true);
    List<Boolean> flags = new ArrayList<>();
    Class<?> manager = loader.loadClass("jakarta.transaction.TransactionManager");
    Object tm = Proxy.newProxyInstance(loader, new Class<?>[] { manager }, (proxy, method, args) -> {
      if (method.getName().equals("getTransaction")) {
        return tx;
      }
      if (method.getName().equals("begin")) {
        flags.add((Boolean) args[0]);
      }
      return null;
    });
    Class<?> adapterType = loader.loadClass("infra.transaction.jta.UserTransactionAdapter");
    Object adapter = adapterType.getConstructor(manager).newInstance(tm);
    adapterType.getMethod("begin", boolean.class).invoke(adapter, true);
    adapterType.getMethod("begin", boolean.class).invoke(adapter, false);
    assertThat(flags).containsExactly(true, false);
    assertThat(adapterType.getMethod("isReadOnly").invoke(adapter)).isEqualTo(true);
  }

  @Test
  void adapterUnwrapsProviderSystemException() throws Exception {
    ApiClassLoader loader = new ApiClassLoader();
    Class<?> manager = loader.loadClass("jakarta.transaction.TransactionManager");
    Throwable failure = (Throwable) loader.loadClass("jakarta.transaction.SystemException")
            .getConstructor(String.class).newInstance("provider failure");
    Object tm = Proxy.newProxyInstance(loader, new Class<?>[] { manager }, (proxy, method, args) -> {
      throw failure;
    });
    Class<?> adapterType = loader.loadClass("infra.transaction.jta.UserTransactionAdapter");
    Object adapter = adapterType.getConstructor(manager).newInstance(tm);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapterType.getMethod("begin", boolean.class).invoke(adapter, true))
            .isInstanceOf(java.lang.reflect.InvocationTargetException.class).hasCause(failure);
  }

  private static class ApiClassLoader extends ClassLoader {
    ApiClassLoader() {
      super(JtaReadOnlyCompatibilityTests.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (!name.startsWith("jakarta.transaction.") && !name.startsWith("infra.transaction.")) {
        return super.loadClass(name, resolve);
      }
      synchronized (getClassLoadingLock(name)) {
        Class<?> type = findLoadedClass(name);
        if (type == null) {
          try (InputStream input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (input == null) {
              throw new ClassNotFoundException(name);
            }
            ClassReader reader = new ClassReader(input);
            ClassWriter writer = new ClassWriter(0);
            reader.accept(writer, 0);
            if (name.equals("jakarta.transaction.UserTransaction") || name.equals("jakarta.transaction.TransactionManager")) {
              writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "begin", "(Z)V", null,
                      new String[] { "jakarta/transaction/NotSupportedException", "jakarta/transaction/SystemException" }).visitEnd();
            }
            if (name.equals("jakarta.transaction.Transaction")) {
              writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "isReadOnly", "()Z", null,
                      new String[] { "jakarta/transaction/SystemException" }).visitEnd();
            }
            byte[] bytes = writer.toByteArray();
            type = defineClass(name, bytes, 0, bytes.length);
          }
          catch (java.io.IOException ex) {
            throw new ClassNotFoundException(name, ex);
          }
        }
        if (resolve) {
          resolveClass(type);
        }
        return type;
      }
    }
  }
}
