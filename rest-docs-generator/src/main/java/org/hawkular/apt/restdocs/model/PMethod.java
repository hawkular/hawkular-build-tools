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
package org.hawkular.apt.restdocs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a Method
 *
 * @author Heiko W. Rupp
 */
public class PMethod {

    public String method;
    public String name;
    public String description;
    public PTypeInfo returnType;
    public String notes;
    public List<ErrorCode> errors = new ArrayList<>();
    public List<PParam> params = new ArrayList<>();
    public List<String> produces = new ArrayList<>();
    public List<String> consumes = new ArrayList<>();
    public String path;
    public boolean gzip = false;

    @Override
    public String toString() {
        return "PMethod{" +
                "method='" + method + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", returnType=" + returnType +
                ", notes='" + notes + '\'' +
                ", errors=" + errors +
                ", params=" + params +
                ", path='" + path + '\'' +
                ", gzip=" + gzip +
                '}';
    }
}
