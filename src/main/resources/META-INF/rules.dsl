[condition][]Location data has been changed on a node=property : ChangedPropertyFact ( name in ("j:street", "j:zipCode", "j:town", "j:state", "j:country", "j:geocodeAutomatically"), propertyName : name, propertyValue : stringValues , node : node , $node : node, node.types contains "jnt:location" || node.types contains "jmix:locationAware")
[condition][]Automatic geocoding is enabled on the node=ChangedPropertyFact ( name == "j:geocodeAutomatically" , stringValue == "true" ) from $node.properties
[consequence][]Geocode the {node}=locationService.geocodeLocation({node}, drools);
