<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<template:addResources type="css" resources="map.css"/>
<template:addResources type="javascript" resources="lodash.js"/>
<template:addResources type="javascript" resources="angular.js"/>
<template:addResources type="javascript" resources="logger.js"/>
<template:addResources type="javascript" resources="angular-google-maps.min.js"/>
<template:addResources type="javascript" resources="jahia.map.js"/>
<c:set var="boundComponent" value="${ui:getBindedComponent(currentNode, renderContext, 'j:bindedComponent')}"/>
<!-- Icon picture url Generation -->
<c:set var="currentContext" value="${url.context}"/>
<c:if test="${empty currentContext}">
    <c:set var="currentContext" value="/" />
</c:if>
<c:url context="${currentContext}" value="/files/${renderContext.workspace}${currentNode.properties['j:markerImage'].node.path}" var="markerURL"/>
<!-- Apply sizes parameters to the map div -->
<style>
    #jnt-map-${currentNode.identifier} .angular-google-map-container {
        height: ${currentNode.properties['j:height'].long}px;
        width: ${currentNode.properties['j:width'].long}px;
    }
</style>
<c:choose>
    <c:when test="${not empty boundComponent && jcr:isNodeType(boundComponent, 'jmix:geotagged,jmix:locationAware,jnt:location')}">
        <c:set var="props" value="${currentNode.propertiesAsString}"/>
        <template:addResources type="javascript" resources="http://maps.google.com/maps/api/js?sensor=false&amp;language=${currentResource.locale.language}"/>
        <template:addResources>
            <script type="text/javascript">
                angular.element(document).ready(function() {
                    angular.bootstrap(document.getElementById("jnt-map-${currentNode.identifier}"),['jahiaMapApp']);
                });
                locale='${renderContext.mainResourceLocale.language}';
                //Prepare the map object with the component parameters,
                //this object will be used to create an angular googleMap object in the controller -->
                if(typeof maps === 'undefined'){
                    maps = [];
                }
                //Objects are declared as list just for the case on which there will be multiple maps on the same page
                //Each map is distinguished by its jcr-uuid-->
                maps['jnt-map-${currentNode.identifier}'] = {
                    mapType : google.maps.MapTypeId.${fn:toUpperCase(props['j:mapType'])},
                    mapZoom : "${props['j:zoom']}",
                    markers : [],
                    markerIcon : "${markerURL}"
                }
            </script>
        </template:addResources>
        <c:set var="targetProps" value="${boundComponent.propertiesAsString}"/>
        <c:choose>
            <c:when test="${not empty targetProps['j:latitude'] && not empty targetProps['j:longitude']}">
                <c:set var="location" value="${targetProps['j:latitude']},${targetProps['j:longitude']}" />
            </c:when>
            <c:otherwise>
                <c:set var="location" value="${targetProps['j:street']}" />
                <c:set var="location" value="${location}${not empty location ? ', ' : ''}${targetProps['j:zipCode']}" />
                <c:set var="location" value="${location}${not empty location ? ', ' : ''}${targetProps['j:town']}" />
                <jcr:nodePropertyRenderer name="j:country" node="${boundComponent}" renderer="country" var="country" />
                <c:set var="location" value="${location}${not empty location ? ', ' : ''}${country.displayName}" />
            </c:otherwise>
        </c:choose>
        <template:addResources>
            <!-- For each location in the list we declare a marker
            This way a map object will contain the map parameters and the list of the markers on the map-->
            <script type="text/javascript">
                maps['jnt-map-${currentNode.identifier}'].markers.push({
                    id:0,
                    <c:if test="${not empty targetProps['j:latitude']}">
                    coords:true,
                    latitude: '${targetProps['j:latitude']}',
                    longitude: '${targetProps['j:longitude']}',
                    </c:if>
                    <c:if test="${empty targetProps['j:latitude']}">
                    coords:false,
                    address: "${functions:escapeJavaScript(location)}",
                    </c:if>
                    <c:if test="${not empty targetProps['jcr:title']}">
                    title: "${functions:escapeJavaScript(targetProps['jcr:title'])}",
                    </c:if>
                    info: ""
                    <c:if test="${not empty targetProps['jcr:title']}">
                    + "<strong>${functions:escapeJavaScript(targetProps['jcr:title'])}</strong>"
                    </c:if>
                    <c:if test="${not empty targetProps['j:street']}">
                    + "<br/>${functions:escapeJavaScript(targetProps['j:street'])}"
                    </c:if>
                    <c:if test="${not empty targetProps['j:zipCode'] || not empty targetProps['j:town']}">
                    + "<br/>"
                    <c:if test="${not empty targetProps['j:zipCode']}">
                    + "${functions:escapeJavaScript(targetProps['j:zipCode'])}&nbsp;"
                    </c:if>
                    + "${not empty targetProps['j:town'] ? functions:escapeJavaScript(targetProps['j:town']) : ''}"
                    </c:if>
                    <jcr:nodePropertyRenderer name="j:country" node="${currentNode}" renderer="country" var="country"/>
                    +"<br/>${functions:escapeJavaScript(country.displayName)}"
                });
            </script>
        </template:addResources>
        <div>
            <c:if test="${not empty props['jcr:title']}">
                <h3>${fn:escapeXml(props['jcr:title'])}</h3>
            </c:if>
            <div id="map-${currentNode.identifier}" style="width:${props['j:width']}px; height:${props['j:height']}px">
                <div id="jnt-map-${currentNode.identifier}">
                    <div ng-controller="jahiaMapAppCtrl">
                        <!-- The createMap function in the controller gets the map javascript object
                             and create a model for the google-map directive for the current node parameters-->
                        <div ng-init="createMap('jnt-map-${currentNode.identifier}')">
                            <ui-gmap-google-map center="generatedMaps['jnt-map-${currentNode.identifier}'].center" zoom="generatedMaps['jnt-map-${currentNode.identifier}'].zoom" options="generatedMaps['jnt-map-${currentNode.identifier}'].options">
                                <ui-gmap-markers models="generatedMaps['jnt-map-${currentNode.identifier}'].markers" coords="'coords'" options="'options'" idkey="'id'" <c:if test="${not empty currentNode.properties['j:markerImage']}">icon="'icon'"</c:if>>
                                </ui-gmap-markers>
                            </ui-gmap-google-map>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <c:if test="${renderContext.editMode}">
            <!-- We notify the user in case the component is bound on the wrong nodeType-->
            <fmt:message key="jnt_map.misbound.location"/>
        </c:if>
    </c:otherwise>
</c:choose>
