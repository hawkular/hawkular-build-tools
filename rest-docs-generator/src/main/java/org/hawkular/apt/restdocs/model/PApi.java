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
 * Top level API element
 *
 * @author Heiko W. Rupp
 */
public class PApi {

    public List<PClass> classes = new ArrayList<>();
    public String name;
    public List<PData> data = new ArrayList<>();

    @Override
    public String toString() {
        return "PApi{" +
                "classes=" + classes +
                ", name='" + name + '\'' +
                ", data=" + data +
                '}';
    }
}
