/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.server.support;

import java.net.SocketAddress;

import cn.taketoday.lang.Nullable;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/8/29 17:29
 */
class HandshakeChannel implements Channel, Runnable {

  private final Channel channel;

  private final ChannelPromise writePromise;

  @Nullable
  public FullHttpResponse response;

  HandshakeChannel(Channel channel, ChannelPromise writePromise) {
    this.channel = channel;
    this.writePromise = writePromise;
  }

  public void release() {
    if (response != null) {
      response.release();
      response = null;
    }
  }

  @Override
  public void run() {
    writePromise.trySuccess();
  }

  @Override
  public ChannelFuture writeAndFlush(Object msg) {
    response = (FullHttpResponse) msg;
    return writePromise;
  }

  @Override
  public ChannelFuture write(Object msg) {
    return channel.write(msg);
  }

  @Override
  public ChannelFuture write(Object msg, ChannelPromise promise) {
    return channel.write(msg, promise);
  }

  @Override
  public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
    return channel.writeAndFlush(msg, promise);
  }

  @Override
  public ByteBufAllocator alloc() {
    return channel.alloc();
  }

  @Override
  public long bytesBeforeUnwritable() {
    return channel.bytesBeforeUnwritable();
  }

  @Override
  public long bytesBeforeWritable() {
    return channel.bytesBeforeWritable();
  }

  @Override
  public ChannelFuture closeFuture() {
    return channel.closeFuture();
  }

  @Override
  public ChannelConfig config() {
    return channel.config();
  }

  @Override
  public EventLoop eventLoop() {
    return channel.eventLoop();
  }

  @Override
  public Channel flush() {
    return channel.flush();
  }

  @Override
  public ChannelId id() {
    return channel.id();
  }

  @Override
  public boolean isActive() {
    return channel.isActive();
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }

  @Override
  public boolean isRegistered() {
    return channel.isRegistered();
  }

  @Override
  public boolean isWritable() {
    return channel.isWritable();
  }

  @Override
  public SocketAddress localAddress() {
    return channel.localAddress();
  }

  @Override
  public ChannelMetadata metadata() {
    return channel.metadata();
  }

  @Override
  public Channel parent() {
    return channel.parent();
  }

  @Override
  public ChannelPipeline pipeline() {
    return channel.pipeline();
  }

  @Override
  public Channel read() {
    return channel.read();
  }

  @Override
  public SocketAddress remoteAddress() {
    return channel.remoteAddress();
  }

  @Override
  public Channel.Unsafe unsafe() {
    return channel.unsafe();
  }

  @Override
  public <T> Attribute<T> attr(AttributeKey<T> key) {
    return channel.attr(key);
  }

  @Override
  public <T> boolean hasAttr(AttributeKey<T> key) {
    return channel.hasAttr(key);
  }

  @Override
  public ChannelFuture bind(SocketAddress localAddress) {
    return channel.bind(localAddress);
  }

  @Override
  public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
    return channel.bind(localAddress, promise);
  }

  @Override
  public ChannelFuture close() {
    return channel.close();
  }

  @Override
  public ChannelFuture close(ChannelPromise promise) {
    return channel.close(promise);
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress) {
    return channel.connect(remoteAddress);
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
    return channel.connect(remoteAddress, localAddress);
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
    return channel.connect(remoteAddress, localAddress, promise);
  }

  @Override
  public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
    return channel.connect(remoteAddress, promise);
  }

  @Override
  public ChannelFuture deregister() {
    return channel.deregister();
  }

  @Override
  public ChannelFuture deregister(ChannelPromise promise) {
    return channel.deregister(promise);
  }

  @Override
  public ChannelFuture disconnect() {
    return channel.disconnect();
  }

  @Override
  public ChannelFuture disconnect(ChannelPromise promise) {
    return channel.disconnect(promise);
  }

  @Override
  public ChannelFuture newFailedFuture(Throwable cause) {
    return channel.newFailedFuture(cause);
  }

  @Override
  public ChannelProgressivePromise newProgressivePromise() {
    return channel.newProgressivePromise();
  }

  @Override
  public ChannelPromise newPromise() {
    return channel.newPromise();
  }

  @Override
  public ChannelFuture newSucceededFuture() {
    return channel.newSucceededFuture();
  }

  @Override
  public ChannelPromise voidPromise() {
    return channel.voidPromise();
  }

  @Override
  public int compareTo(Channel o) {
    return channel.compareTo(o);
  }

}
