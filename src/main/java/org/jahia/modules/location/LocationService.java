/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.location;

import org.drools.core.spi.KnowledgeHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.rules.AddedNodeFact;

import javax.jcr.RepositoryException;

public class LocationService {

    public void geocodeLocation(AddedNodeFact node, KnowledgeHelper drools) throws RepositoryException {
        JCRNodeWrapper nodeWrapper = node.getNode();

        StringBuilder address = new StringBuilder();
        if (nodeWrapper.hasProperty("j:street")) {
            address.append(nodeWrapper.getProperty("j:street").getString());
        }
        if (nodeWrapper.hasProperty("j:zipCode")) {
            address.append(" ").append(nodeWrapper.getProperty("j:zipCode").getString());
        }
        if (nodeWrapper.hasProperty("j:town")) {
            address.append(" ").append(nodeWrapper.getProperty("j:town").getString());
        }
        if (nodeWrapper.hasProperty("j:country")) {
            address.append(" ").append(nodeWrapper.getProperty("j:country").getString());
        }
        if (!nodeWrapper.isNodeType("jnt:location") && !nodeWrapper.isNodeType("jmix:geotagged")) {
            nodeWrapper.addMixin("jmix:geotagged");
        }
    }
}
