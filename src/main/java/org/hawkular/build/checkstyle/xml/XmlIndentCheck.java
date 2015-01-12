package org.hawkular.build.checkstyle.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FastStack;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 *
 */
public final class XmlIndentCheck extends AbstractFileSetCheck {
    /**
     * An entry that can be stored in a stack
     */
    private static class ElementEntry {
        private final String elementName;
        private final Indent expectedIndent;
        private final Indent foundIndent;

        public ElementEntry(String elementName, Indent foundIndent) {
            super();
            this.elementName = elementName;
            this.foundIndent = foundIndent;
            this.expectedIndent = foundIndent;
        }

        public ElementEntry(String elementName, Indent foundIndent,
                Indent expectedIndent) {
            super();
            this.elementName = elementName;
            this.foundIndent = foundIndent;
            this.expectedIndent = expectedIndent;
        }

        @Override
        public String toString() {
            return "<" + elementName + "> " + foundIndent;
        }
    }

    /**
     * An indent occurrence within a file characterized by {@link #lineNumber}
     * and {@link #size}.
     */
    private static class Indent {

        /** An {@link Indent} usable at the beginning of a typical XML file. */
        public static final Indent START = new Indent(1, 0);

        /** The number of spaces in this {@link Indent}. */
        private final int size;

        /**
         * The line number where this {@link Indent} occurs. The first line
         * number in a file is {@code 1}.
         */
        private final int lineNumber;

        public Indent(int lineNumber, int size) {
            super();
            this.lineNumber = lineNumber;
            this.size = size;
        }

        @Override
        public String toString() {
            return "Indent [size=" + size + ", lineNumber=" + lineNumber + "]";
        }
    }

    private class IndentHandler extends DefaultHandler {

        private final StringBuilder charBuffer = new StringBuilder();
        private int charLineNumber;
        private Indent lastIndent = Indent.START;

        private Locator locator;

        private FastStack<ElementEntry> stack = FastStack.newInstance();

        /**
         * Stores the passed characters into {@link #charBuffer}.
         *
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            charBuffer.append(ch, start, length);
            charLineNumber = locator.getLineNumber();
        }

        /**
         * Checks indentation for an end element.
         *
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            flushCharacters();
            if (stack.isEmpty()) {
                throw new IllegalStateException(
                        "Stack must not be empty when closing the element "
                                + qName + " around line "
                                + locator.getLineNumber() + " and column "
                                + locator.getColumnNumber());
            }
            ElementEntry startEntry = stack.pop();
            int indentDiff = lastIndent.size - startEntry.expectedIndent.size;
            int expectedIndent = startEntry.expectedIndent.size;
            if (lastIndent.lineNumber != startEntry.foundIndent.lineNumber
                    && indentDiff != 0) {
                /*
                 * diff should be zero unless we are on the same line as start
                 * element
                 */
                log(locator.getLineNumber(), locator.getColumnNumber(),
                        "Expected indent {0} before end element {1}",
                        expectedIndent, "</" + qName + ">");
            }
        }

        /**
         * Sets {@link lastIndent} based on {@link #charBuffer} and resets
         * {@link #charBuffer}.
         */
        private void flushCharacters() {
            int indentLength = 0;
            int len = charBuffer.length();
            /*
             * Count characters from end of ignorable whitespace to first end of
             * line we hit
             */
            for (int i = len - 1; i >= 0; i--) {
                char ch = charBuffer.charAt(i);
                switch (ch) {
                case '\n':
                case '\r':
                    lastIndent = new Indent(charLineNumber, indentLength);
                    charBuffer.setLength(0);
                    return;
                case ' ':
                case '\t':
                    indentLength++;
                    break;
                default:
                    /*
                     * No end of line foundIndent in the trailing whitespace.
                     * Leave the foundIndent from previous ignorable whitespace
                     * unchanged
                     */
                    charBuffer.setLength(0);
                    return;
                }
            }
        }

        /**
         * Just delegates to {@link #characters(char[], int, int)}, since this
         * method is not called in all situations where it could be naively
         * expected.
         *
         * @see org.xml.sax.helpers.DefaultHandler#ignorableWhitespace(char[],
         *      int, int)
         */
        @Override
        public void ignorableWhitespace(char[] chars, int start, int length)
                throws SAXException {
            characters(chars, start, length);
        }

        /**
         * Always returns an empty {@link InputSource} to avoid loading of any
         * DTDs.
         *
         * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String,
         *      java.lang.String)
         */
        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            return new InputSource(new StringReader(""));
        }

        /** @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator) */
        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /**
         * Checks indentation for a start element.
         *
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            flushCharacters();
            ElementEntry currentEntry = new ElementEntry(qName, lastIndent);
            if (!stack.isEmpty()) {
                ElementEntry parentEntry = stack.peek();
                /*
                 * note that we use parentEntry.expectedIndent rather than
                 * parentEntry.foundIndent this is to make the messages more
                 * useful
                 */
                int indentDiff = currentEntry.foundIndent.size
                        - parentEntry.expectedIndent.size;
                int expectedIndent = parentEntry.expectedIndent.size
                        + indentSize;
                if (indentDiff == 0
                        && currentEntry.foundIndent.lineNumber == parentEntry.foundIndent.lineNumber) {
                    /*
                     * Zero foundIndent acceptable only if current is on the
                     * same line as parent This is OK, therefore do nothing
                     */
                } else if (indentDiff != indentSize) {
                    /* generally unexpected foundIndent */
                    log(locator.getLineNumber(), locator.getColumnNumber(),
                            "Expected indent {0} before start element {1}",
                            expectedIndent, "<" + currentEntry.elementName
                                    + ">");
                    /* reset the expected inden in the entry we'll push */
                    currentEntry = new ElementEntry(qName, lastIndent,
                            new Indent(lastIndent.lineNumber, expectedIndent));
                }
            }
            stack.push(currentEntry);
        }

    }

    /** The default indent size: {@value} spaces. */
    public static final int DEFAULT_INDENT_SIZE = 4;

    /** The {@code "indentSize"} string. */
    public static final String INDENT_SIZE_ATTRIBUTE = "indentSize";

    /** The encoding used to read the XML files. */
    private String encoding;

    /** The number of spaces expected for indentation. */
    private int indentSize;

    /** A {@link SAXParserFactory} */
    private SAXParserFactory saxParserFactory;

    /**
     * Creates a new {@link XmlIndentCheck} instance.
     */
    public XmlIndentCheck() {
        super();
        this.indentSize = DEFAULT_INDENT_SIZE;
        setFileExtensions(new String[] { ".xml" });
        this.saxParserFactory = SAXParserFactory.newInstance();
        this.saxParserFactory.setValidating(false);
    }

    /** @see com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck#beginProcessing(java.lang.String) */
    @Override
    public void beginProcessing(String aCharset) {
        this.encoding = aCharset;
    }

    /**
     * @return the number of spaces for indentation
     */
    public int getIndentSize() {
        return indentSize;
    }

    /**
     * @see com.puppycrawl.tools.checkstyle
     *      .api.AbstractFileSetCheck#processFiltered(java.io.File,
     *      java.util.List)
     */
    @Override
    protected void processFiltered(File aFile, List<String> aLines) {
        Reader in = null;
        try {
            in = new InputStreamReader(new FileInputStream(aFile), encoding);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            DefaultHandler handler = new IndentHandler();
            saxParser.parse(new InputSource(in), handler);
        } catch (SAXException e) {
            log(0, "SAXException: {0}", e.getMessage());
        } catch (IOException e) {
            log(0, "IOException: {0}", e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Sets the number of spaces for indentation.
     *
     * @param indentSize
     *            the number of spaces
     */
    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

}
