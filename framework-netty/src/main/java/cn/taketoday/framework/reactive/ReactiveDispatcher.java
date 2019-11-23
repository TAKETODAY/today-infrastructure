/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.framework.reactive;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingRegistry;
import cn.taketoday.web.mapping.RegexMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.servlet.DispatcherHandler;
import cn.taketoday.web.utils.ResultUtils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2019-07-04 21:50
 */
@Slf4j
//@Sharable
@Singleton
public class ReactiveDispatcher
        extends SimpleChannelInboundHandler<FullHttpRequest> implements ChannelInboundHandler, DisposableBean {

//  private static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class);

    /** context path */
    private final String contextPath;
    /** exception resolver */
    private final ExceptionResolver exceptionResolver;
    /** Action mapping registry */
    private final HandlerMappingRegistry handlerMappingRegistry;

    private final WebServerApplicationContext applicationContext;

    @Autowired
    public ReactiveDispatcher(ExceptionResolver exceptionResolver, //@off
                              HandlerMappingRegistry handlerMappingRegistry,
                              WebServerApplicationContext applicationContext) //@on
    {
        if (exceptionResolver == null) {
            throw new ConfigurationException("You must provide an 'exceptionResolver'");
        }
        this.exceptionResolver = exceptionResolver;
        this.applicationContext = applicationContext;
        this.handlerMappingRegistry = handlerMappingRegistry;

        this.contextPath = StringUtils.isEmpty(applicationContext.getContextPath())
                ? Constant.BLANK
                : applicationContext.getContextPath();
    }

    @Override
    protected void ensureNotSharable() {}

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return msg instanceof FullHttpRequest;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

//        System.err.println(msg);

        async(ctx, msg);
//      sync(ctx, msg);
    }

    protected void sync(ChannelHandlerContext ctx, FullHttpRequest msg) {
        // Lookup handler mapping
        final HandlerMapping mapping = lookupHandlerMapping(msg);

        if (mapping == null) {
            ctx.writeAndFlush(notFound());
            return;
        }

        final NettyRequestContext nettyRequestContext = new NettyRequestContext(contextPath, ctx, msg);

        service(ctx, mapping, nettyRequestContext);
        nettyRequestContext.send();
    }

    private void async(ChannelHandlerContext ctx, FullHttpRequest request) {

        final Executor executor = ctx.executor(); //ctx.channel().eventLoop()
        final CompletableFuture<FullHttpRequest> future = CompletableFuture.completedFuture(request);

        future.thenApplyAsync(this::lookupHandlerMapping, executor)
                .thenApplyAsync(mapping -> {
                    if (mapping == null) {
                        ctx.writeAndFlush(notFound());
                        future.complete(request);
                        return null;
                    }
                    final NettyRequestContext context = new NettyRequestContext(contextPath, ctx, request);
                    try {
                        DispatcherHandler.service(mapping, context);
                    }
                    catch (Throwable e) {
                        try {
                            ResultUtils.resolveException(context, exceptionResolver, mapping, e);
                        }
                        catch (Throwable e1) {
                            ctx.fireExceptionCaught(e);
                        }
                    }
                    return context;
                }, executor)
                .thenAcceptAsync(context -> {
                    context.send();
                }, executor);
    }

    protected FullHttpResponse notFound() {

        final FullHttpResponse notFound = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                                      HttpResponseStatus.NOT_FOUND);

        notFound.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        notFound.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        return notFound;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        log.error("cause :{}", cause.toString(), cause);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    public void service(final ChannelHandlerContext ctx, final HandlerMapping mapping, final RequestContext context) {

        try {
            DispatcherHandler.service(mapping, context);
        }
        catch (Throwable e) {
            e.printStackTrace();
            try {
                ResultUtils.resolveException(context, exceptionResolver, mapping, e);
            }
            catch (Throwable e1) {
                ctx.fireExceptionCaught(e);
            }
        }
    }

    /**
     * Looking for {@link HandlerMapping}
     * 
     * @param request
     *            current request
     * @return mapped {@link HandlerMapping}
     * @since 2.3.7
     */
    protected HandlerMapping lookupHandlerMapping(final HttpRequest request) {

        String key = StringUtils.decodeUrl(request.uri());
        request.setUri(key);

        // The key of handler
        key = request.method().name().concat(key);

        final HandlerMapping ret = handlerMappingRegistry.get(key);
        if (ret == null) { // path variable
            for (final RegexMapping regexMapping : handlerMappingRegistry.getRegexMappings()) {
                // TODO path matcher pathMatcher.match(requestURI, requestURI)
                if (regexMapping.pattern.matcher(key).matches()) {
                    return regexMapping.handlerMapping;
                }
            }
            log.debug("NOT FOUND -> [{}]", key);
            return null;
        }
        return ret;
    }

    @Override
    public void destroy() {
        DispatcherHandler.destroy(applicationContext);
    }

    public final HandlerMappingRegistry getHandlerMappingRegistry() {
        return this.handlerMappingRegistry;
    }

    public final String getContextPath() {
        return this.contextPath;
    }

    public final ExceptionResolver getExceptionResolver() {
        return this.exceptionResolver;
    }

}
