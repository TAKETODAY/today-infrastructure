/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 11:22
 */
public class BaseTest {

  protected static RedissonClient redisson;

  @BeforeAll
  public static void beforeClass() throws IOException, InterruptedException {
    RedisRunner.startDefaultRedisServerInstance();
    redisson = createInstance();
  }

  @AfterAll
  public static void afterClass() throws InterruptedException {
    redisson.shutdown();
    RedisRunner.shutDownDefaultRedisServerInstance();
  }

  @BeforeEach
  public void before() throws IOException, InterruptedException {
    if (flushBetweenTests()) {
      redisson.getKeys().flushall();
    }
  }

  public static Config createConfig() {
//        String redisAddress = System.getProperty("redisAddress");
//        if (redisAddress == null) {
//            redisAddress = "127.0.0.1:6379";
//        }
    Config config = new Config();
//        config.setCodec(new MsgPackJacksonCodec());
//        config.useSentinelServers().setMasterName("mymaster").addSentinelAddress("127.0.0.1:26379", "127.0.0.1:26389");
//        config.useClusterServers().addNodeAddress("127.0.0.1:7004", "127.0.0.1:7001", "127.0.0.1:7000");
    config.useSingleServer()
            .setAddress(RedisRunner.getDefaultRedisServerBindAddressAndPort());
//        .setPassword("mypass1");
//        config.useMasterSlaveConnection()
//        .setMasterAddress("127.0.0.1:6379")
//        .addSlaveAddress("127.0.0.1:6399")
//        .addSlaveAddress("127.0.0.1:6389");
    return config;
  }

  public static RedissonClient createInstance() {
    Config config = createConfig();
    return Redisson.create(config);
  }

  protected boolean flushBetweenTests() {
    return true;
  }
}
