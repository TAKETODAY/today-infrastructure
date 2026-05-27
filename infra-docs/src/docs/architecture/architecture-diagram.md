# TODAY Infrastructure 架构图

> 版本: 5.0-Draft.6-SNAPSHOT · 七层纵向分层架构

```mermaid
graph BT
    subgraph "Application Layer"
        APP["infra-app<br/>@InfraApplication, Bootstrap,<br/>Auto-Configuration, Health"]
        STARTERS["infra-starter-* (32 starters)<br/>Web / JDBC / Jackson / Logging / ..."]
    end

    subgraph "Web / HTTP Layer"
        WMVC["infra-webmvc<br/>DispatcherHandler, MVC"]
        WREACT["infra-web-reactive<br/>Reactive Web"]
        WBSOCK["infra-websocket<br/>WebSocket"]
        HTTP["infra-http<br/>HTTP types, Client/Server"]
        HTTPCLIENT["infra-http-client / infra-http-service<br/>HTTP Clients, Declarative HTTP"]
        WCONV["infra-http-converter<br/>HTTP Message Converters"]
        WCODE["infra-http-codec<br/>HTTP Codecs"]
        WSERV["infra-web-server<br/>WebServer Abstraction"]
        NETTY["infra-web-netty-server<br/>Netty Server"]
        REACTNETTY["infra-web-reactor-server<br/>Reactor Netty Server"]
    end

    subgraph "Data Layer"
        PERSIST["infra-persistence<br/>Lightweight ORM"]
        JDBC["infra-jdbc<br/>JDBC Abstraction"]
        TX["infra-tx<br/>Transaction Management"]
        OXM["infra-oxm<br/>Object/XML Mapping"]
        JCACHE["infra-jcache<br/>JCache (JSR-107)"]
        INTEGRATION["module/* (14 modules)<br/>Flyway / Jackson / Gson / Mail / ..."]
    end

    subgraph "Context / IoC Container"
        CTX["infra-context<br/>ApplicationContext, Events,<br/>@Conditional, Scheduling,<br/>Validation, JMX, Scripting"]
        CTXIDX["infra-context-indexer<br/>Component Indexing"]
        CTXSUP["infra-context-support<br/>Cache Support"]
    end

    subgraph "Bean Container"
        BEANS["infra-beans<br/>BeanFactory, DI,<br/>Bean Definitions,<br/>Property Accessors"]
    end

    subgraph "AOP"
        AOP["infra-aop<br/>JDK/CGLIB Proxies,<br/>Pointcuts, Interceptors,<br/>@AspectJ"]
        ASPECTS["infra-aspects<br/>AspectJ Aspects"]
    end

    subgraph "Core / Foundation"
        CORE["infra-core<br/>ASM Bytecode (Fork),<br/>Annotation Engine,<br/>Type Conversion,<br/>Environment/Properties,<br/>Resource Loading,<br/>AOT Generation,<br/>Logging, SSL, Codecs,<br/>Utilities (lang/util)"]
        EXPR["infra-expression<br/>SpEL"]
        INSTR["infra-instrument<br/>LTW Agent"]
    end

    subgraph "Build / Tooling / Testing"
        BUILD["infra-build/* (8 modules)<br/>Gradle/Maven Plugins,<br/>Annotation Processors,<br/>App Loader, Layered JAR"]
        BOM["infra-bom / infra-dependencies<br/>BOM & Dependency Mgmt"]
        TEST["infra-test / infra-test-support<br/>Test Utilities & Testcontainers"]
    end

    subgraph "JDK Support"
        JDK["JDK 17 (baseline)<br/>JDK 21 / 24 (multi-release)"]
    end

    JDK -.-> CORE
    JDK -.-> BEANS
    JDK -.-> CTX
    JDK -.-> APP

    CORE --> EXPR
    CORE --> INSTR
    CORE --> BEANS
    CORE --> AOP

    BEANS --> CTX
    AOP --> CTX

    CTX --> JDBC
    CTX --> TX
    CTX --> PERSIST
    CTX --> JCACHE
    CTX --> OXM
    CTX --> INTEGRATION

    CTX --> HTTP
    CTX --> WSERV
    WSERV --> NETTY
    WSERV --> REACTNETTY
    HTTP --> WCODE
    HTTP --> WCONV
    HTTP --> HTTPCLIENT

    HTTP --> WMVC
    HTTP --> WREACT
    HTTP --> WBSOCK

    WMVC --> APP
    WREACT --> APP
    WBSOCK --> APP
    HTTPCLIENT --> APP
    INTEGRATION --> APP
    PERSIST --> APP
    TX --> APP

    APP --> STARTERS
    BUILD -.-> STARTERS
    TEST -.-> APP
    BOM -.-> STARTERS
```

---

## 层次说明

| 层次 | 核心职责 | 关键模块 |
|------|----------|----------|
| **Core / Foundation** | 字节码操作、注解引擎、类型转换、资源加载、AOT | `infra-core`, `infra-expression` |
| **Bean Container** | IoC 容器核心、依赖注入、Bean 定义 | `infra-beans` |
| **AOP** | JDK/CGLIB 代理、@AspectJ | `infra-aop`, `infra-aspects` |
| **Context / IoC** | ApplicationContext、事件、条件配置、调度、校验 | `infra-context` |
| **Data** | JDBC 抽象、事务管理、轻量 ORM、缓存 | `infra-jdbc`, `infra-tx`, `infra-persistence` |
| **Web / HTTP** | Web MVC、Reactive、WebSocket、HTTP 客户端/服务器 | `infra-webmvc`, `infra-http`, 各种 server 模块 |
| **Application** | 应用启动、自动配置、健康检查 | `infra-app`, `infra-starter-*` |
