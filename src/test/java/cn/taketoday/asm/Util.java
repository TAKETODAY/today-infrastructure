package cn.taketoday.asm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Provides utility methods for the asm.test package.
 *
 * @author Eric Bruneton
 */
final class Util {

  private Util() { }

  static int getMajorJavaVersion() {
    String javaVersion = System.getProperty("java.version");
    StringTokenizer tokenizer = new StringTokenizer(javaVersion, "._");
    String javaMajorVersionText = tokenizer.nextToken();
    int majorVersion = Integer.parseInt(javaMajorVersionText);
    if (majorVersion != 1) {
      return majorVersion;
    }
    javaMajorVersionText = tokenizer.nextToken();
    return Integer.parseInt(javaMajorVersionText);
  }

  static boolean previewFeatureEnabled() {
    try {
      Class<?> managementFactoryClass = Class.forName("java.lang.management.ManagementFactory");
      Method getRuntimeMxBean = managementFactoryClass.getMethod("getRuntimeMXBean");
      Object runtimeMxBean = getRuntimeMxBean.invoke(null);
      Class<?> runtimeMxBeanClass = Class.forName("java.lang.management.RuntimeMXBean");
      Method getInputArguments = runtimeMxBeanClass.getMethod("getInputArguments");
      List<?> argumentList = (List<?>) getInputArguments.invoke(runtimeMxBean);
      return argumentList.contains("--enable-preview");
    }
    catch (ClassNotFoundException e) { // JMX may be not available
      return false;
    }
    catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
    catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new AssertionError(cause); // NOPMD
    }
  }
}
