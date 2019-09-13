# Logback LogDNA

### 1. Add maven repository
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Add this project as maven dependency
```xml
<dependency>
    <groupId>com.gitlab.muhammadkholidb</groupId>
    <artifactId>logback-logdna</artifactId>
    <version>master-SNAPSHOT</version>
</dependency>
```

### 3. Add appender to logback.xml
```xml
<appender name="LOGDNA" class="com.gitlab.muhammadkholidb.logbacklogdna.LogDNAAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%15.15thread] %-40.40logger{39} : %msg%n</pattern>
    </encoder>
    <appName><!-- Your application name --></appName>
    <ingestionKey><!-- Your LogDNA ingestion key --></ingestionKey>
    <includeStacktrace><!-- Log stacktraces. Default: true --></includeStacktrace>
    <sendMDC><!-- Send logback's Mapped Diagnostic Context (MDC). Default: true --></sendMDC>
</appender>

<!-- This is required to send your log asynchronously or your application will become very slow -->
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="LOGDNA" />
</appender>

<root level="DEBUG">
    <appender-ref ref="ASYNC" />
</root>
```
