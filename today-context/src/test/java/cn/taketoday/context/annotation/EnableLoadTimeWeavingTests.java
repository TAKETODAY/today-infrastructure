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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;

import cn.taketoday.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import cn.taketoday.instrument.classloading.LoadTimeWeaver;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for @EnableLoadTimeWeaving
 *
 * @author Chris Beams
 * @since 4.0
 */
public class EnableLoadTimeWeavingTests {

  @Test
  public void enableLTW_withAjWeavingDisabled() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EnableLTWConfig_withAjWeavingDisabled.class);
    ctx.refresh();
    LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
    verifyNoInteractions(loadTimeWeaver);
  }

  @Test
  public void enableLTW_withAjWeavingAutodetect() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EnableLTWConfig_withAjWeavingAutodetect.class);
    ctx.refresh();
    LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
    // no expectations -> a class file transformer should NOT be added
    // because no META-INF/aop.xml is present on the classpath
    verifyNoInteractions(loadTimeWeaver);
  }

  @Test
  public void enableLTW_withAjWeavingEnabled() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EnableLTWConfig_withAjWeavingEnabled.class);
    ctx.refresh();
    LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
    verify(loadTimeWeaver).addTransformer(isA(ClassFileTransformer.class));
  }

  @Configuration
  @EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.DISABLED)
  static class EnableLTWConfig_withAjWeavingDisabled implements LoadTimeWeavingConfigurer {

    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
      return mock(LoadTimeWeaver.class);
    }
  }

  @Configuration
  @EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.AUTODETECT)
  static class EnableLTWConfig_withAjWeavingAutodetect implements LoadTimeWeavingConfigurer {

    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
      return mock(LoadTimeWeaver.class);
    }
  }

  @Configuration
  @EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.ENABLED)
  static class EnableLTWConfig_withAjWeavingEnabled implements LoadTimeWeavingConfigurer {

    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
      return mock(LoadTimeWeaver.class);
    }
  }

}
