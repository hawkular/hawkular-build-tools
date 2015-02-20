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
package org.hawkular.apt.restdocs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a Data class - that is one that is
 * used as complex type of data to be transferred - most
 * often in the body of a REST-Request
 *
 * @author Heiko W. Rupp
 */
public class PData {
    public String name;
    public String nameId;
    public String shortDescription;
    public String description;
    public String objectName;
    public List<PProperty> properties = new ArrayList<>();
}
