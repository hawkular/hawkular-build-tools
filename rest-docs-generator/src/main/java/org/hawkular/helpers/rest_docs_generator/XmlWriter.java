/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.helpers.rest_docs_generator;

import org.hawkular.helpers.rest_docs_generator.model.ErrorCode;
import org.hawkular.helpers.rest_docs_generator.model.PApi;
import org.hawkular.helpers.rest_docs_generator.model.PClass;
import org.hawkular.helpers.rest_docs_generator.model.PData;
import org.hawkular.helpers.rest_docs_generator.model.PMethod;
import org.hawkular.helpers.rest_docs_generator.model.PParam;
import org.hawkular.helpers.rest_docs_generator.model.PProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Logger;


/**
 * Writer to generate teh classical rest-api-out.xml file, that
 * can be post-processed by xslt to e.g. form Docbook XML
 *
 * @author Heiko W. Rupp
 */
public class XmlWriter implements DataWriter {
    private Logger log = Logger.getLogger("XMlWriter");

    @Override
    public void write(File out, PApi api) {
        Document doc;
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = documentBuilder.newDocument();
        }
        catch (Exception e) {
            log.severe(e.getMessage());
            return;
        }

        Element root = doc.createElement("api");
        doc.appendChild(root);

        processClasses(doc,root,api.classes);
        processDataClasses(doc,root,api.data);


        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2); // xml indent 2 spaces
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // do xml indent

            // We initialize here for String writing to be able to also see the result on stdout
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();


            String path = out.getAbsolutePath();
            String s = "..... writing to [" + path + "] ......";
            log.info(s);
            System.err.println(s);

            try {
                FileWriter fw = new FileWriter(out);
                fw.write(xmlString);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                log.severe(e.getMessage());
            }

        } catch (TransformerException e) {
            log.severe(e.getMessage());
        }

    }


    private void processClasses(Document doc, Element root, List<PClass> classes) {

        for (PClass clazz : classes) {
            Element classElement = doc.createElement("class");
            root.appendChild(classElement);
            addOptionalAttribute(classElement, "name", clazz.name);
            addOptionalAttribute(classElement, "basePath", clazz.basePath);
            addOptionalAttribute(classElement, "shortDesc", clazz.shortDesc);
            addOptionalAttribute(classElement, "description", clazz.description);
            addOptionalAttribute(classElement, "path", clazz.path);

            handleProduces(doc, classElement, clazz.produces);

            handleMethods(doc,classElement, clazz.methods);
        }

    }


    private void handleProduces(Document doc, Element classElement, List<String> produces) {
        if (produces.isEmpty()) {
            return;
        }

        Element producesElement = doc.createElement("produces");
        classElement.appendChild(producesElement);
        for (String type : produces) {
            Element typeElement = doc.createElement("type");
            producesElement.appendChild(typeElement);
            typeElement.setTextContent(type);
        }
    }

    private void handleMethods(Document doc, Element classElement, List<PMethod> methods) {
        if (methods.isEmpty()) {
            return;
        }

        for (PMethod method : methods) {
            Element methodElement = doc.createElement("method");
            classElement.appendChild(methodElement);
            handleMethod(doc,methodElement,method);
        }

    }

    private void handleMethod(Document doc, Element methodElement, PMethod method) {
        addOptionalAttribute(methodElement,"name",method.name);
        addOptionalAttribute(methodElement,"method",method.method);
        addOptionalAttribute(methodElement,"path",method.path);
        addOptionalAttribute(methodElement,"gzip",String.valueOf(method.gzip));
        addOptionalAttribute(methodElement,"description",method.description);
        addOptionalAttribute(methodElement,"notes",method.notes); // TODO simpara handling?
        addOptionalAttribute(methodElement,"returnType",method.returnType.typeString);
        handleParams(doc,methodElement,method.params);
        handleErrors(doc,methodElement,method.errors);

    }

    private void handleParams(Document doc, Element methodElement, List<PParam> params) {
        if (params.isEmpty()) {
            return;
        }

        for (PParam param : params) {
            Element pe = doc.createElement("param");
            methodElement.appendChild(pe);
            addOptionalAttribute(pe,"name",param.name);
            addOptionalAttribute(pe,"required",String.valueOf(param.required));
            addOptionalAttribute(pe,"type",param.paramType.name());
            addOptionalAttribute(pe,"allowedValues",param.allowableValues);
            addOptionalAttribute(pe,"defaultValue",param.defaultValue);
            addOptionalAttribute(pe,"description",param.description);
        }
    }

    private void handleErrors(Document doc, Element methodElement, List<ErrorCode> errors) {
        if (errors.isEmpty()) {
            return;
        }

        for (ErrorCode ec : errors) {
            Element ee = doc.createElement("error");
            methodElement.appendChild(ee);
            addOptionalAttribute(ee, "code", String.valueOf(ec.code));
            addOptionalAttribute(ee, "reason", ec.reason);
        }
    }

    private void processDataClasses(Document doc, Element root, List<PData> data) {

        for (PData pData : data) {
            Element de = doc.createElement("dataClass");
            root.appendChild(de);
            addOptionalAttribute(de,"name",pData.name);
            addOptionalAttribute(de,"shortDescription",pData.shortDescription);
            addOptionalAttribute(de,"description",pData.description);

            for (PProperty property : pData.properties) {
                Element pe = doc.createElement("property");
                de.appendChild(pe);
                addOptionalAttribute(pe, "name", property.name);
                addOptionalAttribute(pe, "type", property.type);
                addOptionalAttribute(pe, "description", property.description);
                addOptionalAttribute(pe, "notes", property.notes);
            }


        }

    }


    void addOptionalAttribute(Element baseElement, String attributeName, String value) {
        if (value!=null) {
            baseElement.setAttribute(attributeName,value);
        }
    }
}
