/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.loader;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author TODAY <br>
 * 2019-11-28 23:52
 */
public class CandidateComponentScannerTest {

  @Test
  @Ignore
  public void testScan() {
    final CandidateComponentScanner sharedInstance = CandidateComponentScanner.getSharedInstance();
    sharedInstance.clear();
    final Set<Class<?>> scan = sharedInstance.scan();
    final int size1 = scan.size();
    assertTrue(size1 > 0);
    assertTrue(sharedInstance.getScanningTimes() == 1);

//        // --------------------
    final Set<Class<?>> loader = sharedInstance.scan("cn.taketoday.context.loader"); // 14 + 12
    final int size2 = loader.size();
    assertTrue(size2 == size1);
//
//        // -------------------------------------
    sharedInstance.clear();
    final Set<Class<?>> loader2 = sharedInstance.scan("cn.taketoday.context.loader"); // 14 + 12
    final int loader2size2 = loader2.size();
    assertTrue(loader2size2 == 27);
  }

  @Test
  public void getDefaultIgnoreJarPrefix() {
    final String[] defaultIgnoreJarPrefix = CandidateComponentScanner.getDefaultIgnoreJarPrefix();
    assertThat(defaultIgnoreJarPrefix).hasSize(38);
  }

  @Test
  public void test() throws IOException {
    final ClassPathResource classPathResource = new ClassPathResource("test.jar");
    final URL location = classPathResource.getLocation();
    final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { location });

//    System.out.println(location);
//    System.out.println(classPathResource);
//    System.out.println(urlClassLoader);
    final CandidateComponentScanner componentScanner = new CandidateComponentScanner();
    componentScanner.setClassLoader(urlClassLoader);

    componentScanner.scan("com.sun.el.util");
    final Set<Class<?>> candidates = componentScanner.getCandidates();
    assertThat(candidates).hasSize(7);
  }

}
