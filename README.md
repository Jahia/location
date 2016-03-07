# Jahia Location Module
Jahia location repository

This branch contains the angular views for the jnt_map component.
Two new views have been added to the component : 

- map.singleLocation.view.jsp

and 

- map.multipleLocation.jsp

The angular google map framework (http://angular-ui.github.io/angular-google-maps/#!/use) has been used to develop those views.

##Multiple markers view
If you want to create a map with multiples markers all you have to do is to put the map component on your page, link it to a list of component that define an adress
And of course select the multiple markers view for the map component.

##Simple marker view
In the other case if you want to create a map with only one single marker, then you can put the component on your page and link it directly to a component wich defined an adress
and finally select the single point view for this component.

##Component parameters
As with other views you will be able to define some parameters for your map : 

- The map type between those values : 'roadmap','satellite','terrain','hybrid' ('roadmap is selected by default')
- The width in pixels (500px by default), 
- The height in pixels (300px by default), 
- The zoom (mandatory and with no default value)
- The picture for the markers on the map (the standard google marker picture by default)

##Styling
Those two new views display the name of the bound component under the marker if you want to customize this label styling you'll have to override the mapMarkerLabel css class
