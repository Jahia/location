/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.location;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import org.drools.core.spi.KnowledgeHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.rules.AddedNodeFact;

import javax.jcr.RepositoryException;
import java.util.List;

public class LocationService {

    public void geocodeLocation(AddedNodeFact node, KnowledgeHelper drools) throws RepositoryException {
        final Geocoder geocoder = new Geocoder();
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
        if (address.length() > 0) {
            GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(address.toString()).getGeocoderRequest();
            GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);
            List<GeocoderResult> results = geocoderResponse.getResults();
            if (results.size() > 0) {
                nodeWrapper.setProperty("j:latitude", results.get(0).getGeometry().getLocation().getLat().toString());
                nodeWrapper.setProperty("j:longitude", results.get(0).getGeometry().getLocation().getLng().toString());
            }
        }
    }
}
