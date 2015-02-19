/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.hawkular.helpers.rest_docs_generator;

import org.hawkular.helpers.rest_docs_generator.model.ErrorCode;
import org.hawkular.helpers.rest_docs_generator.model.PApi;
import org.hawkular.helpers.rest_docs_generator.model.PClass;
import org.hawkular.helpers.rest_docs_generator.model.PData;
import org.hawkular.helpers.rest_docs_generator.model.PMethod;
import org.hawkular.helpers.rest_docs_generator.model.PParam;
import org.hawkular.helpers.rest_docs_generator.model.PProperty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Data writer for AsciiDoc output
 *
 * @author Heiko W. Rupp
 */
public class AsciiDocWriter implements DataWriter {

    private Writer writer;
    private static final String LF = "\n"; // TODO get from env


    @Override
    public void write(File out, PApi pApi) {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(out))) {
            this.writer = bufferedWriter;
            writeIntro(pApi);
            processClasses(pApi.classes);
            processDataClasses(pApi.data);
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }

    }


    private void writeIntro(PApi pApi) throws IOException {
        writeLine("= " + pApi.name);
        lf();
    }


    private void processClasses(List<PClass> classes) throws IOException {
        for (PClass pClass : classes) {
            processClass(pClass);
        }
    }

    private void processClass(PClass pClass) throws IOException {
        if (pClass.shortDesc!=null && !pClass.shortDesc.isEmpty()) {
            writeLine("== " + pClass.shortDesc);
        } else {
            writeLine("== " + pClass.path);
        }
        lf();
        writeLine("Implemented in: " + pClass.name);
        lf();
        if (pClass.description!=null && !pClass.description.isEmpty()) {
            writeLine("Description: " + pClass.description);
            lf();
        }

        if (!pClass.produces.isEmpty()) {
            writeLine("Produces:" );
            lf();
            for (String name : pClass.produces ) {
                writeLine("* " + name);
            }
            lf();
        }


        for (PMethod method : pClass.methods) {
            processMethod(method, pClass.path);
        }

        lf();
    }

    private void processMethod(PMethod method, String outerPath) throws IOException {
        writeLine("=== " +method.method + " /" + outerPath + "/" + method.path );
        lf();
        if (method.gzip) {
            writeLine("Supports GZIP'd responses");
        }

        writeLine("[.lead]");
        writeLine("Description: " + method.description);
        lf();

        if (method.notes!=null && !method.notes.isEmpty()) {
            writeLine(".NOTE");
            writeLine(method.notes);
            lf();
        }

        if (method.returnType!=null) {
            write("Return type: ");
            if (method.returnType.typeId.startsWith("...")) {
                write("[[" + method.returnType.typeId + "]] ");
            }
            writeLine(method.returnType.typeString);
            lf();
        }


        if (!method.params.isEmpty()) {
            processParams(method.params);
        }

        if (!method.errors.isEmpty()) {
            processErrorCodes(method.errors);
        }


        lf();
    }

    private void processParams(List<PParam> params) throws IOException {
        writeLine(".Parameters");
        writeLine("|===");
        writeLine("|Name|Required|Type|Allowed Values|Default Value|Description");
        lf();
        for (PParam pParam : params) {
            writeLine("|" + pParam.name + "|" + pParam.required + "|" + pParam.paramType.name()
                    + "|" + pParam.allowableValues + "|" + pParam.defaultValue
                    + "|" + pParam.description );
        }
        writeLine("|===");
        lf();
    }

    private void processErrorCodes(List<ErrorCode> errors) throws IOException {
        writeLine(".Error codes");
        writeLine("|===");
        writeLine("|Code|Reason");
        lf();
        for (ErrorCode ec : errors) {
            writeLine("| " + ec.code + "|" + ec.reason );
        }
        writeLine("|===");
        lf();
    }


    private void processDataClasses(List<PData> data) throws IOException {

        if (data==null || data.isEmpty()) {
            return;
        }

        writeLine("== Data Classes" );
        lf();
        for (PData pData : data) {
            processDataClass(pData);
        }
        lf();
    }

    private void processDataClass(PData pData) throws IOException {
        writeLine("[#" + pData.nameId+"]");
        writeLine("=== " + pData.name + " - " + pData.shortDescription);
        lf();

        if (pData.description!=null && !pData.description.isEmpty()) {
            writeLine("Description: " + pData.description);
            lf();
        }

        lf();
        if (!pData.properties.isEmpty()) {
            writeLine(".Properties");
            writeLine("|===");
            writeLine("|Name|Type|Description");
            lf();
            int notecount=1;
            for (PProperty property : pData.properties) {
                String descr = property.description == null ? "" : property
                        .description;
                StringBuilder builder = new StringBuilder()
                        .append("|").append(property.name)
                        .append("|").append(property.type)
                        .append("|").append(descr);
                if (property.notes!=null) {
                    builder.append(" ("+notecount+")");
                    notecount++;
                }
                writeLine(builder.toString());
            }
            writeLine("|===");
            // There were some notes - let's list them
            if (notecount>1) {
                notecount=1;
                for (PProperty property : pData.properties) {
                    if (property.notes!=null) {
                        writeLine("<"+notecount+"> " + property.notes);
                        notecount++;
                    }
                }

            }
        }


        lf();
    }


    private void lf() throws IOException {
        writeLine("");
    }

    private void writeLine(String item) throws IOException {
        writer.write(item);
        writer.write(LF);

    }

    private void write(String item) throws IOException {
        writer.write(item);
    }

}
