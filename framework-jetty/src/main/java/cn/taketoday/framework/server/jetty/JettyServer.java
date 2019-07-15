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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.framework.server.jetty;

import java.io.IOException;
import java.net.BindException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;
import javax.servlet.Servlet;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.io.ClassPathResource;
import cn.taketoday.context.io.FileBasedResource;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.WebServerException;
import cn.taketoday.framework.bean.ErrorPage;
import cn.taketoday.framework.bean.MimeMappings;
import cn.taketoday.framework.config.CompressionConfiguration;
import cn.taketoday.framework.config.SessionConfiguration;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.framework.server.WebServer;
import cn.taketoday.web.servlet.initializer.ServletContextInitializer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Jetty web server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author David Liu
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author Kristine Jetzke
 * 
 * @author TODAY <br>
 *         2018-10-15 20:44
 */
@Slf4j
@Setter
@Getter
@MissingBean(type = WebServer.class)
@Props(prefix = { "server.", "server.jetty." })
public class JettyServer extends AbstractWebServer implements WebServer {

    private Server server;

    private boolean autoStart = true;

    private Connector[] connectors;

    private List<Configuration> configurations = new ArrayList<>();

    private boolean useForwardHeaders;

    /** The number of acceptor threads to use. default value */
    private int acceptors = -1;

    /** The number of selector threads to use. default value */
    private int selectors = -1;

    private ThreadPool threadPool;

    private boolean sendVersion;

    @Override
    protected synchronized void contextInitialized() throws Throwable {

        super.contextInitialized();

        try {

            // Cache the connectors and then remove them to prevent requests being
            // handled before the application context is ready.
            this.connectors = this.server.getConnectors();
            this.server.addBean(new AbstractLifeCycle() {

                @Override
                protected void doStart() throws Exception {
                    for (Connector connector : JettyServer.this.connectors) {
                        state(connector.isStopped(), () -> "Connector " + connector + " has been started prematurely");
                    }
                    JettyServer.this.server.setConnectors(null);
                }

            });
            // Start the server so that the ServletContext is available
            this.server.start();
            this.server.setStopAtShutdown(false);
        }
        catch (Throwable ex) {
            // Ensure process isn't left running
            stopSilently();
            throw new WebServerException("Unable to start embedded Jetty web server", ex);
        }
    }

    public static void state(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalStateException(nullSafeGet(messageSupplier));
        }
    }

    private static String nullSafeGet(Supplier<String> messageSupplier) {
        return (messageSupplier != null ? messageSupplier.get() : null);
    }

    //@off
    private void stopSilently() {
        try {
            this.server.stop();
        }
        catch (Exception ex) {}
    }
    // @on

    @Override
    public synchronized void start() throws WebServerException {
        if (getStarted().get()) {
            return;
        }
        this.server.setConnectors(this.connectors);
        if (!this.autoStart) {
            return;
        }
        try {

            this.server.start();

            Connector[] connectors = this.server.getConnectors();
            for (Connector connector : connectors) {
                try {
                    connector.start();
                }
                catch (BindException ex) {
                    if (connector instanceof NetworkConnector) {
                        log.error("The port: [{}] is already in use", ((NetworkConnector) connector).getPort(), ex);
                    }
                    throw ex;
                }
            }
            getStarted().set(true);

            log.info("Jetty started on port(s) '{}' with context path '{}'", //
                    getActualPortsDescription(), getContextPath());
        }
        catch (WebServerException ex) {
            stopSilently();
            throw ex;
        }
        catch (Exception ex) {
            stopSilently();
            throw new WebServerException("Unable to start embedded Jetty server", ex);
        }
    }

    private String getActualPortsDescription() {
        StringBuilder ports = new StringBuilder();
        for (Connector connector : this.server.getConnectors()) {
            if (ports.length() != 0) {
                ports.append(", ");
            }
            ports.append(getPort()).append(getProtocols(connector));
        }
        return ports.toString();
    }

    private String getProtocols(Connector connector) {
        List<String> protocols = connector.getProtocols();
        return " (" + StringUtils.arrayToString(protocols.toArray(Constant.EMPTY_STRING_ARRAY)) + ")";
    }

    @Override
    @PreDestroy
    public synchronized void stop() {

        getStarted().set(false);

        try {
            this.server.stop();
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        catch (Exception ex) {
            throw new WebServerException("Unable to stop embedded Jetty server", ex);
        }
    }

    public Server getJetty() {
        return this.server;
    }

    @Override
    protected void initializeContext(ServletContextInitializer... contextInitializers) throws Throwable {

        super.initializeContext(contextInitializers);

        log.info("Jetty Server initializing with port: {}", getPort());

        final WebAppContext context = new WebAppContext();

        final Server server = new Server(getThreadPool());
        this.server = server;
        server.setConnectors(new Connector[] { getServerConnector(getHost(), getPort(), server) });

        configureWebAppContext(context, contextInitializers);
        server.setHandler(getHandlerWrappers(context));

        if (this.useForwardHeaders) {

            final ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
            for (final Connector connector : server.getConnectors()) {
                for (final ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
                    if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {

                        ((HttpConfiguration.ConnectionFactory) connectionFactory).getHttpConfiguration()//
                                .addCustomizer(customizer);
                    }
                }
            }
        }
    }

    /**
     * Create a sever {@link Connector}
     * 
     * @param host
     *            server host
     * @param port
     *            server port
     * @param server
     *            server instance
     * @return a {@link ServerConnector}
     */
    protected ServerConnector getServerConnector(final String host, final int port, final Server server) {

        final ServerConnector connector = new ServerConnector(server, this.acceptors, this.selectors);

        connector.setHost(host);
        connector.setPort(port);

        for (final ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
            if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
                // send version
                ((HttpConfiguration.ConnectionFactory) connectionFactory).getHttpConfiguration()//
                        .setSendServerVersion(sendVersion);
            }
        }
        return connector;
    }

    protected Handler getHandlerWrappers(Handler handler) {

        final CompressionConfiguration compression = getCompression();
        if (compression != null) {

            getWebApplicationConfiguration().configureCompression(compression);

            if (compression.isEnable()) {
                handler = applyHandler(handler, configureCompression(compression));
            }
        }
//        if (StringUtils.isNotEmpty(getServerHeader())) { // TODO server header }
        return handler;
    }

    protected Handler applyHandler(Handler handler, HandlerWrapper wrapper) {
        wrapper.setHandler(handler);
        return wrapper;
    }

    /**
     * Configure the given Jetty {@link WebAppContext} for use.
     * 
     * @param context
     *            the context to configure
     * @param initializers
     *            the set of initializers to apply
     * @throws Throwable
     */
    protected void configureWebAppContext(final WebAppContext context, //
            final ServletContextInitializer... initializers) throws Throwable //
    {

        Objects.requireNonNull(context, "WebAppContext must not be null");

        context.setTempDirectory(getTemporalDirectory()); // base temp dir

        final String contextPath = getContextPath();

        context.setContextPath(StringUtils.isNotEmpty(contextPath) ? contextPath : "/");
        context.setDisplayName(getDisplayName());

        configureDocumentRoot(context);

        configureLocaleMappings(context);
        configureWelcomePages(context);

        final Configuration[] configurations = //
                getWebAppContextConfigurations(context, getAllInitializers(initializers));

        context.setConfigurations(configurations);

        context.setThrowUnavailableOnStartupException(true);

        configureSession(context);
    }

    protected void configureWelcomePages(WebAppContext context) {

        final Set<String> welcomePages = getWelcomePages();
        getWebApplicationConfiguration().configureWelcomePages(welcomePages);

        context.setWelcomeFiles(welcomePages.toArray(Constant.EMPTY_STRING_ARRAY));
    }

    /**
     * Return the Jetty {@link Configuration}s that should be applied to the server.
     * 
     * @param webAppContext
     *            the Jetty {@link WebAppContext}
     * @param initializers
     *            the {@link ServletContextInitializer}s to apply
     * @return configurations to apply
     */
    protected Configuration[] getWebAppContextConfigurations(WebAppContext webAppContext, //
            ServletContextInitializer... initializers)//
    {

        final List<Configuration> configurations = new ArrayList<>();
        configurations.add(getJettyServletContextInitializer(webAppContext, initializers));

        configurations.addAll(getConfigurations()); // user define

        configurations.add(getErrorPageConfiguration());
        configurations.add(getMimeTypeConfiguration());

        return configurations.toArray(new Configuration[0]);
    }

    /**
     * Create a configuration that adds mime type mappings.
     * 
     * @return a configuration for adding mime type mappings
     */
    protected Configuration getMimeTypeConfiguration() {

        return new AbstractConfiguration() {
            @Override
            public void configure(WebAppContext context) throws Exception {
                final MimeTypes mimeTypes = context.getMimeTypes();

                final MimeMappings mimeMappings = getMimeMappings();

                getWebApplicationConfiguration().configureMimeMappings(mimeMappings);

                for (MimeMappings.Mapping mapping : mimeMappings) {
                    mimeTypes.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
                }
            }
        };
    }

    /**
     * Get a configuration that adds error pages.
     * 
     * @return a configuration to add error pages
     */
    protected Configuration getErrorPageConfiguration() {
        return new AbstractConfiguration() {
            @Override
            public void configure(WebAppContext context) throws Exception {
                addJettyErrorPages(context.getErrorHandler(), getErrorPages());
            }
        };
    }

    /**
     * Add jetty {@link ErrorPage}
     * 
     * @param errorHandler
     * @param errorPages
     */
    protected void addJettyErrorPages(ErrorHandler errorHandler, Set<ErrorPage> errorPages) {

        getWebApplicationConfiguration().configureErrorPages(errorPages);

        if (errorHandler instanceof ErrorPageErrorHandler) {

            ErrorPageErrorHandler handler = (ErrorPageErrorHandler) errorHandler;

            for (ErrorPage errorPage : errorPages) {
                if (errorPage.getException() != null) {
                    handler.addErrorPage(errorPage.getException(), errorPage.getPath());
                }
                if (errorPage.getStatus() != 0) {
                    handler.addErrorPage(errorPage.getStatus(), errorPage.getPath());
                }
//                handler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, errorPage.getPath());
            }
        }
    }

    /**
     * Return a Jetty {@link Configuration} that will invoke the specified
     * {@link ServletContextInitializer}s. By default this method will return a
     * {@link ServletContextInitializerConfiguration}.
     * 
     * @param webAppContext
     *            the Jetty {@link WebAppContext}
     * @param initializers
     *            the {@link ServletContextInitializer}s to apply
     * @return the {@link Configuration} instance
     */
    protected Configuration getJettyServletContextInitializer(//
            WebAppContext webAppContext, ServletContextInitializer... initializers) //
    {
        return new ServletContextInitializerConfiguration(initializers);
    }

    /**
     * Configure session timeout, store directory
     * 
     * @param context
     *            jetty web app context
     */
    protected void configureSession(final WebAppContext context) throws Throwable {

        final SessionHandler sessionHandler = context.getSessionHandler();
        final SessionConfiguration sessionConfiguration = getSessionConfiguration();
        final Duration sessionTimeout = sessionConfiguration.getTimeout();

        sessionHandler.setMaxInactiveInterval(isNegative(sessionTimeout) ? -1 : (int) sessionTimeout.getSeconds());

        if (sessionConfiguration.isPersistent()) {

            final DefaultSessionCache cache = new DefaultSessionCache(sessionHandler);
            final FileSessionDataStore store = new FileSessionDataStore();

            store.setStoreDir(sessionConfiguration.getStoreDirectory(getStartupClass()));

            cache.setSessionDataStore(store);
            sessionHandler.setSessionCache(cache);
        }
    }

    private boolean isNegative(Duration sessionTimeout) {
        return sessionTimeout == null || sessionTimeout.isNegative();
    }

    protected void configureLocaleMappings(WebAppContext context) {

        for (Entry<Locale, Charset> entry : getLocaleCharsetMappings().entrySet()) {
            context.addLocaleEncoding(entry.getKey().toString(), entry.getValue().toString());
        }

    }

    /**
     * Configure jetty root document dir
     * 
     * @param webAppContext
     * @throws Throwable
     */
    protected void configureDocumentRoot(WebAppContext webAppContext) throws Throwable {

        webAppContext.setBaseResource(getRootResource(getWebDocumentConfiguration().getValidDocumentDirectory()));
    }

    protected Resource getRootResource(final cn.taketoday.context.io.Resource validDocBase) throws IOException {

        if (validDocBase instanceof cn.taketoday.context.io.JarResource) {
            return JarResource.newJarResource(Resource.newResource(validDocBase.getFile()));
        }
        if (validDocBase instanceof FileBasedResource) {
            return Resource.newResource(validDocBase.getFile());
        }
        if (validDocBase instanceof ClassPathResource) {
            return getRootResource(((ClassPathResource) validDocBase).getOriginalResource());
        }
        return Resource.newResource(getTemporalDirectory("jetty-docbase"));
    }

    @Override
    protected Servlet getDefaultServlet() {
        return new DefaultServlet();
    }

    /**
     * @param compression
     * @return
     */
    protected GzipHandler configureCompression(CompressionConfiguration compression) {

        GzipHandler handler = new GzipHandler();

//        handler.setCompressionLevel(compression.getLevel());

        handler.setMinGzipSize((int) compression.getMinResponseSize().toBytes());
        handler.addIncludedMimeTypes(compression.getMimeTypes());

        // ---path
        if (StringUtils.isArrayNotEmpty(compression.getIncludedPaths())) {
            handler.addIncludedPaths(compression.getIncludedPaths());
        }
        if (StringUtils.isArrayNotEmpty(compression.getExcludePaths())) {
            handler.addExcludedPaths(compression.getExcludePaths());
        }
        // --- method
        if (StringUtils.isArrayNotEmpty(compression.getIncludeMethods())) {
            handler.addIncludedMethods(compression.getIncludeMethods());
        }

        if (StringUtils.isArrayNotEmpty(compression.getExcludeMethods())) {
            handler.addExcludedMethods(compression.getExcludeMethods());
        }
        // --- agent

        if (StringUtils.isArrayNotEmpty(compression.getExcludeUserAgents())) {
            handler.addExcludedAgentPatterns(compression.getExcludeUserAgents());
        }

        if (StringUtils.isArrayNotEmpty(compression.getIncludeAgentPatterns())) {
            handler.addIncludedAgentPatterns(compression.getIncludeAgentPatterns());
        }

        if (StringUtils.isArrayNotEmpty(compression.getExcludeAgentPatterns())) {
            handler.addExcludedAgentPatterns(compression.getExcludeAgentPatterns());
        }

        return handler;
    }

    /**
     * Jetty {@link Configuration} that calls {@link ServletContextInitializer}s.
     */
    public static class ServletContextInitializerConfiguration extends AbstractConfiguration {

        private final ServletContextInitializer[] initializers;

        public ServletContextInitializerConfiguration(ServletContextInitializer... initializers) {
            this.initializers = Objects.requireNonNull(initializers, "Initializers must not be null");
        }

        @Override
        public void configure(WebAppContext context) throws Exception {
            context.addBean(new Initializer(context), true);
        }

        /**
         * Jetty {@link AbstractLifeCycle} to call the {@link ServletContextInitializer
         * ServletContextInitializers}.
         */
        private class Initializer extends AbstractLifeCycle {

            private final WebAppContext context;

            Initializer(WebAppContext context) {
                this.context = context;
            }

            @Override
            protected void doStart() throws Exception {

                final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.context.getClassLoader());
                try {

                    setExtendedListenerTypes(true);
                    final Context servletContext = this.context.getServletContext();

                    for (ServletContextInitializer initializer : ServletContextInitializerConfiguration.this.initializers) {
                        initializer.onStartup(servletContext);
                    }
                }
                catch (Throwable e) {
                    throw new WebServerException(e);
                } finally {
                    setExtendedListenerTypes(false);
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            }

            private final void setExtendedListenerTypes(boolean extended) {
                this.context.getServletContext().setExtendedListenerTypes(extended);
            }
        }
    }

}
