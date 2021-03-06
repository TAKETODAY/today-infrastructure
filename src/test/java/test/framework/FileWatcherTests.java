/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

package test.framework;

import java.io.File;

import cn.taketoday.framework.hotswap.FileAlterationListener;
import cn.taketoday.framework.hotswap.FileAlterationMonitor;
import cn.taketoday.framework.hotswap.FileAlterationObserver;

/**
 * @author TODAY 2021/2/18 11:12
 */
public abstract class FileWatcherTests {

  public static void main(String[] args) throws Exception {
    final FileAlterationMonitor fileAlterationMonitor = new FileAlterationMonitor(1000);
    final File file = new File("D:\\Projects\\Git\\github\\today-framework\\framework-core\\target\\classes");
    final FileAlterationObserver observer = new FileAlterationObserver(file);
    fileAlterationMonitor.addObserver(observer);

    observer.addListener(new FileAlterationListener() {

      @Override
      public void onFileCreate(File file) {
        System.out.println("文件创建 " + file);
      }

      @Override
      public void onFileDelete(File file) {
        System.out.println("文件删除 " + file);
      }

      @Override
      public void onFileChange(File file) {
        System.out.println("文件改变 " + file);
      }
    });
    fileAlterationMonitor.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        System.out.println("STOP");
        fileAlterationMonitor.stop();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }));
  }

}
