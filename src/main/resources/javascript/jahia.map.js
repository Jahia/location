/**
 * This file contain angular App and controller for Google map angular framework.
 * See the following page for more details : http://angular-ui.github.io/angular-google-maps/#!/
 */
var jahiaMapApp = angular.module('jahiaMapApp', ['uiGmapgoogle-maps']);

/**
 * Load Google Map API with the context locale
 * See the following page for more details : http://angular-ui.github.io/angular-google-maps/#!/api/GoogleMapApi
 * */
jahiaMapApp.config(['uiGmapGoogleMapApiProvider', function (GoogleMapApi) {
    GoogleMapApi.configure({
        //key: 'your api key',
        libraries: 'weather,geometry,visualization',
        sensor:false,
        language:locale
    });
}]);

/**
 * Map resolution is done by the controller below
 * @param siteName
 */
jahiaMapApp.controller('jahiaMapAppCtrl', ['$scope', '$http',function ($scope, $http) {
    $scope.generatedMaps = [];
    /**
     *This function generate a google map object basing on the component instance parameters
     * @param id
     * @returns {{id: boolean, coords: {latitude: *, longitude: *}, icon: string, options: {draggable: boolean, labelContent: *, labelClass: string, title: *}, events: {}}}
     */
    $scope.createMap = function(mapId){
        if($scope.generatedMaps[mapId]){
            return $scope.generatedMaps[mapId];
        } else {
            $scope.generatedMaps[mapId]={
                center: { latitude: 45,
                    longitude: -73 },
                zoom: parseInt(maps[mapId].mapZoom),
                options:{mapTypeControl: true,
                    mapTypeId     : maps[mapId].mapType},
                markers:[]
            }
            //Check for existing markers
            if(maps[mapId].markers.length>0){
                for(var mindex = 0 ; mindex<maps[mapId].markers.length;mindex++){
                    var marker = maps[mapId].markers[mindex];
                    if(marker.coords){
                        //If marker has location properties
                        //create and push the marker
                        $scope.generatedMaps[mapId].markers.push($scope.createMarker(marker.latitude, marker.longitude, marker.id, mapId, mindex ));
                        //Center the map on the first marker
                        if(mindex==0){
                            $scope.generatedMaps[mapId].center.latitude = marker.latitude;
                            $scope.generatedMaps[mapId].center.longitude = marker.longitude;
                        }
                    } else {
                        //geocode address pushing the marker
                        $scope.geocode(marker.address, marker.id, mapId,mindex);
                    }
                }
            }
        }
    }

    /**
     * This function create and return a marker object using the given parameters
     * @param lat
     * @param lng
     * @param identifier
     * @returns {{id: *, coords: {latitude: *, longitude: *}, icon: string, options: {draggable: boolean, labelContent: *, labelClass: string, title: *}, events: {}}}
     */
    $scope.createMarker = function(lat, lng, markerId, mapId, mindex){
        return {
            id: markerId,
            coords: {
                latitude: lat,
                longitude: lng
            },
            icon:maps[mapId].markerIcon,
            options: {
                draggable: false,
                labelContent: maps[mapId].markers[mindex].title,
                labelClass:'mapMarkerLabel',
                title: maps[mapId].markers[mindex].title
            },
            events: {}
        };
    }

    /**
     * This function geocode an adress to coords using google maps API
     * @param address
     */
    $scope.geocode = function(address, markerId, mapId,mindex){
        //Define geocode url basing on address
        var geocodeURL = "https://maps.googleapis.com/maps/api/geocode/json?address=";
        var escapedAddress = address.replace(" ","+");
        geocodeURL+=escapedAddress;
        //Call google api geocoding service
        $http.get(geocodeURL).then(function(response){
            if(response.status == 200){
                //Get location in the response
                var location = response.data.results[0].geometry.location;

                //Push the created marker
                $scope.generatedMaps[mapId].markers.push($scope.createMarker(location.lat, location.lng, markerId,mapId,  mindex ));

                //Center the map on the first marker
                if(mindex==0){
                    $scope.generatedMaps[mapId].center.latitude = location.lat;
                    $scope.generatedMaps[mapId].center.longitude = location.lng;
                }
            }
        });
    };
}]);