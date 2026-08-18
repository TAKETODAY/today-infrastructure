package infra.app.test;

import infra.util.ClassUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/8/18 10:00
 */
public class InfraApp {

  public static final boolean isPresent = ClassUtils.isPresent("infra.app.Application");

}
