# Logback #

**WildFly**

# Install: #

### copy ###
*  standalone/configuration/logback-default.xml ([sample](https://github.com/Janario/wildfly-logback-subsystem/blob/master/src/test/java/examples/logback-default.xml))
*  standalone/configuration/logging.properties ([sample](https://github.com/Janario/wildfly-logback-subsystem/blob/master/src/test/java/examples/logging.properties))

### standalone/configuration/standalone.xml ###
*  remove &lt;extension module="org.jboss.as.logging"/&gt;
*  add &lt;extension module="me.janario.logback"/&gt;
 
*  remove &lt;subsystem xmlns="urn:jboss:domain:logging:2.0"&gt;...&lt;/subsystem&gt;
*  add &lt;subsystem xmlns="urn:me.janario.logback:1.0"/&gt;
 
* copy modules/system/layers/logback to wildfly-8.2.0.Final/modules/system/layers/logback
* copy modules/layers.conf to wildfly-8.2.0.Final/modules/layers.conf


### Know Issues ###
* NPE in async(MDC map) LogDiagnosticContextStorageInterceptor.processInvocation{MDC.getMap() &gt; LogbackMDCAdapter.getCopyOfContextMap}
* After stop LoggerContext old Logger still with a reference of it(stoped and no more contextual)
* Improve integration with LoggerContext logger-selector (levels and appenders)
