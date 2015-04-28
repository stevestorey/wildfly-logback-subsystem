package me.janario.logback.extension;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(LogbackExtension.SUBSYSTEM_NAME, new LogbackExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"" + LogbackExtension.NAMESPACE + "\">" +
                "</subsystem>";
    }

}
