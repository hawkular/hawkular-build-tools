/*
 * Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.build.checkstyle.xml;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collections;
import java.util.Locale;

import org.hawkular.build.checkstyle.xml.XmlIndentCheck;
import org.junit.Before;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class XmlIndentCheckTest {
    protected static class SilentLogger extends DefaultLogger {
        public SilentLogger(OutputStream out) throws UnsupportedEncodingException {
            super(out, true);
        }

        @Override
        public void auditStarted(AuditEvent evt) {
        }

        @Override
        public void fileFinished(AuditEvent evt) {
        }

        @Override
        public void fileStarted(AuditEvent evt) {
        }
    }

    private DefaultConfiguration configuration;

    protected final ByteArrayOutputStream logStream = new ByteArrayOutputStream();

    protected Checker createChecker(Configuration checkConfig)
            throws Exception {
        final Checker result = new Checker();
        final Locale locale = Locale.ENGLISH;
        result.setLocaleCountry(locale.getCountry());
        result.setLocaleLanguage(locale.getLanguage());
        result.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        final DefaultConfiguration rootConfig = new DefaultConfiguration("root");
        rootConfig.addChild(checkConfig);
        result.configure(rootConfig);
        result.addListener(new SilentLogger(new PrintStream(logStream, false, "UTF-8")));
        return result;
    }

    /**
     * Test with a file that is properly indented using 4 spaces, but expect
     * indentation with 8 spaces. Note that 8 is divisible by 4.
     *
     * @throws IOException
     * @throws Exception
     */
    @Test
    public void failIndentSizeGreaterDivisible() throws Exception {
        configuration.addAttribute(XmlIndentCheck.INDENT_SIZE_ATTRIBUTE, "8");
        verify("4-spaces-correct-no-schema-short.xml", new String[] {
                "4:15: Expected indent 8 before start element <parent-1>",
                "5:17: Expected indent 16 before start element <text-1>",
                "6:16: Expected indent 8 before end element </parent-1>", });
    }

    /**
     * Test with a file that is properly indented using 4 spaces, but expect
     * indentation with 2 spaces. Note that 4 is divisible by 2.
     *
     * @throws IOException
     * @throws Exception
     */
    @Test
    public void failIndentSizeSmallerDivisible() throws IOException, Exception {
        configuration.addAttribute(XmlIndentCheck.INDENT_SIZE_ATTRIBUTE, "2");
        verify("4-spaces-correct-no-schema-short.xml", new String[] {
                "4:15: Expected indent 2 before start element <parent-1>",
                "5:17: Expected indent 4 before start element <text-1>",
                "6:16: Expected indent 2 before end element </parent-1>" });
    }

    @Test
    public void passCorrect() throws Exception {
        configuration.addAttribute(XmlIndentCheck.INDENT_SIZE_ATTRIBUTE, "4");
        verify("4-spaces-correct-no-schema.xml", new String[0]);
    }

    @Before
    public void setUp() {
        configuration = new DefaultConfiguration(XmlIndentCheck.class.getName());
    }

    protected void verify(String fileName, String[] expectedViolations)
            throws Exception {
        URL url = getClass().getResource(fileName);
        File file = new File(url.toURI());
        Checker checker = createChecker(configuration);
        final int foundViolationsCount = checker.process(Collections
                .singletonList(file));
        logStream.flush();
        final InputStream in = new ByteArrayInputStream(logStream.toByteArray());
        final BufferedReader reader = new LineNumberReader(
                new InputStreamReader(in, "UTF-8"));

        for (int i = 0; i < expectedViolations.length; i++) {
            final String expected = file.getAbsolutePath() + ":"
                    + expectedViolations[i];
            final String actual = reader.readLine();
            assertEquals("error message " + i, expected, actual);
        }

        String nextLine = reader.readLine();
        assertEquals("unexpected output: " + nextLine,
                expectedViolations.length, foundViolationsCount);
        checker.destroy();
    }

}
