/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/8/21 00:41
 */
class FileSystemUtilsTests {

  @Test
  void deleteRecursively() throws Exception {
    File root = new File("./tmp/root");
    File child = new File(root, "child");
    File grandchild = new File(child, "grandchild");

    grandchild.mkdirs();

    File bar = new File(child, "bar.txt");
    bar.createNewFile();

    assertThat(root.exists()).isTrue();
    assertThat(child.exists()).isTrue();
    assertThat(grandchild.exists()).isTrue();
    assertThat(bar.exists()).isTrue();

    FileSystemUtils.deleteRecursively(root);

    assertThat(root.exists()).isFalse();
    assertThat(child.exists()).isFalse();
    assertThat(grandchild.exists()).isFalse();
    assertThat(bar.exists()).isFalse();
  }

  @Test
  void copyRecursively() throws Exception {
    File src = new File("./tmp/src");
    File child = new File(src, "child");
    File grandchild = new File(child, "grandchild");

    grandchild.mkdirs();

    File bar = new File(child, "bar.txt");
    bar.createNewFile();

    assertThat(src).exists();
    assertThat(child).exists();
    assertThat(grandchild).exists();
    assertThat(bar).exists();

    File dest = new File("./dest");
    FileSystemUtils.copyRecursively(src, dest);

    assertThat(dest).exists();
    assertThat(new File(dest, child.getName())).exists();

    FileSystemUtils.deleteRecursively(src);
    assertThat(src).doesNotExist();
  }

  @AfterEach
  void tearDown() throws Exception {
    File tmp = new File("./tmp");
    if (tmp.exists()) {
      FileSystemUtils.deleteRecursively(tmp);
    }
    File dest = new File("./dest");
    if (dest.exists()) {
      FileSystemUtils.deleteRecursively(dest);
    }
  }

}
