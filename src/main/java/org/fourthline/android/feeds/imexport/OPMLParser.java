/*
 * Copyright (C) 2012 4th Line GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fourthline.android.feeds.imexport;

import org.seamless.xml.SAXParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.fourthline.android.feeds.imexport.OPML.ATTRIBUTE;
import static org.fourthline.android.feeds.imexport.OPML.ELEMENT;

/**
 * @author Christian Bauer
 */
public class OPMLParser {

    final private static Logger log = Logger.getLogger(OPMLParser.class.getName());

    static {
        // This should be the default on Android 2.1 but it's not set by default
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    public List<OPML.Outline> read(String descriptorXml) throws ImportExportException {

        if (descriptorXml == null || descriptorXml.length() == 0) {
            throw new ImportExportException("Null or empty XML");
        }

        try {
            log.fine("Reading OPML file...");

            SAXParser parser = new SAXParser();

            List<OPML.Outline> result = new ArrayList<OPML.Outline>();
            new BodyHandler(result, parser);

            parser.parse(new InputSource(new StringReader(descriptorXml.trim())));
            return result;

        } catch (Exception ex) {
            throw new ImportExportException("Could not parse OPML: " + ex.toString(), ex);
        }
    }

    public String write(List<OPML.Outline> outlines) throws ImportExportException {
        try {
            log.fine("Generating XML from outlines: " + outlines.size());

            return documentToString(buildDOM(outlines));

        } catch (Exception ex) {
            throw new ImportExportException("Could not build DOM: " + ex.getMessage(), ex);
        }

    }

    public Document buildDOM(List<OPML.Outline> outlines) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document d = factory.newDocumentBuilder().newDocument();

        Element opmlElement = d.createElement(ELEMENT.opml.name());
        d.appendChild(opmlElement);
        opmlElement.setAttribute(ATTRIBUTE.version.name(), "1.0");

        Element headElement = d.createElement(ELEMENT.head.name());
        opmlElement.appendChild(headElement);

        Element titleElement = d.createElement(ELEMENT.title.name());
        titleElement.setTextContent("4th Line Feeds Export");
        headElement.appendChild(titleElement);

        Element bodyElement = d.createElement(ELEMENT.body.name());
        opmlElement.appendChild(bodyElement);

        for (OPML.Outline outline : outlines) {
            Element outlineElement = d.createElement(ELEMENT.outline.name());
            bodyElement.appendChild(outlineElement);
            outlineElement.setAttribute(ATTRIBUTE.type.name(), "rss");
            outline.setAttributes(outlineElement);
        }

        return d;
    }

    public static String documentToString(Document document) throws Exception {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        // As usual, none of this shit works...
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
    }

    protected static class BodyHandler extends OPMLHandler<List<OPML.Outline>> {

        public static final ELEMENT EL = ELEMENT.body;

        public BodyHandler(List<OPML.Outline> instance, SAXParser parser) {
            super(instance, parser);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ELEMENT.outline)) {
                String outlineType = attributes.getValue(ATTRIBUTE.type.name());
                if (!"rss".equals(outlineType))
                    return;

                try {
                    OPML.Outline outline = new OPML.Outline(attributes);
                    getInstance().add(outline);
                } catch (Exception ex) {
                    log.severe("Ignoring invalid OPML outline: " + attributes.getValue(ATTRIBUTE.text.name()));
                }
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class OPMLHandler<I> extends SAXParser.Handler<I> {

        public OPMLHandler(I instance) {
            super(instance);
        }

        public OPMLHandler(I instance, SAXParser parser) {
            super(instance, parser);
        }

        public OPMLHandler(I instance, OPMLHandler parent) {
            super(instance, parent);
        }

        public OPMLHandler(I instance, SAXParser parser, OPMLHandler parent) {
            super(instance, parser, parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) return;
            startElement(el, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) return;
            endElement(el);
        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            return el != null && isLastElement(el);
        }

        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

        }

        public void endElement(ELEMENT element) throws SAXException {

        }

        public boolean isLastElement(ELEMENT element) {
            return false;
        }
    }

}
