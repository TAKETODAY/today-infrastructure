/*
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

package cn.taketoday.framework.server.light;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.net.ssl.SSLServerSocketFactory;

import static cn.taketoday.framework.server.light.Utils.*;

/**
 * @author TODAY 2021/4/13 11:17
 */
public class HTTPServerTests {

  /**
   * Starts a stand-alone HTTP server, serving files from disk.
   *
   * @param args
   *         the command line arguments
   */
  public static void main(String[] args) {
    try {
      if (args.length == 0) {
        System.err.printf("Usage: java [-options] %s <directory> [port]%n" +
                                  "To enable SSL: specify options -Djavax.net.ssl.keyStore, " +
                                  "-Djavax.net.ssl.keyStorePassword, etc.%n", HTTPServer.class.getName());
        return;
      }
      File dir = new File(args[0]);
      if (!dir.canRead())
        throw new FileNotFoundException(dir.getAbsolutePath());
      int port = args.length < 2 ? 80 : (int) parseULong(args[1], 10);
      // set up server
      for (File f : Arrays.asList(new File("/etc/mime.types"), new File(dir, ".mime.types")))
        if (f.exists())
          Utils.addContentTypes(new FileInputStream(f));
      HTTPServer server = new HTTPServer(port);
      if (System.getProperty("javax.net.ssl.keyStore") != null) // enable SSL if configured
        server.setServerSocketFactory(SSLServerSocketFactory.getDefault());
      VirtualHost host = server.getVirtualHost(null); // default host
      host.setAllowGeneratedIndex(true); // with directory index pages
      host.addContext("/", new FileContextHandler(dir));
//      host.addContext("/api/time", new ContextHandler() {
//        public int serve(LightRequest req, Response resp) throws IOException {
//          long now = System.currentTimeMillis();
//          resp.getHeaders().add("Content-Type", "text/plain");
//          resp.send(200, String.format("%tF %<tT", now));
//          return 0;
//        }
//      });
      server.start();
      System.out.println("HTTPServer is listening on port " + port);
    }
    catch (Exception e) {
      System.err.println("error: " + e);
    }
  }
}
