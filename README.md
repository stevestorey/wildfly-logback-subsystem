# Logback #

**WildFly**

# Install: #

### copy ###
*  standalone/configuration/logback-default.xml
*  standalone/configuration/logging.properties

### standalone/configuration/standalone.xml ###
*  remove <extension module="org.jboss.as.logging"/>
*  add <extension module="me.janario.logback"/>
 
*  remove <subsystem xmlns="urn:jboss:domain:logging:2.0">...</subsystem>
*  add <subsystem xmlns="urn:me.janario.logback:1.0"/>
 
* copy modules/system/layers/logback to wildfly-8.2.0.Final/modules/system/layers/logback
* copy modules/layers.conf to wildfly-8.2.0.Final/modules/layers.conf


### Know Issues ###
* NPE in async(MDC map) LogDiagnosticContextStorageInterceptor.processInvocation{MDC.getMap() > LogbackMDCAdapter.getCopyOfContextMap}
* After stop LoggerContext old Logger still with a reference of it(stoped and no more contextual)
* Improve integration with LoggerContext logger-selector (levels and appenders)
