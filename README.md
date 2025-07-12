# <h>PlayerWaypoints</h>

Server-side mod for adding custom waypoints to the Locator Bar using Polymer's Virtual-Entity API


<h1>How to use:</h1>

### Global Waypoints:
###### <sub>Global waypoints will be sent to all players in the world the waypoint is in.</sub>

`PlayerWaypointManager.addGlobalWaypoint(serverWorld, pos, id) -> WaypointElement`
`PlayerWaypointManager.addGlobalWaypoint(serverWorld, waypointElement) -> WaypointElement`

##### Removing a waypoint:

`PlayerWaypointManager.removeGlobalWaypoint(serverWorld, waypointElement) -> void`


### Per-Player Waypoints:
###### <sub> Per-Player waypoints will be sent to the player if they are in the same world. 

`PlayerWaypointManager.addPlayerWaypoint(player, serverWorld, pos, id) -> WaypointElement`
`PlayerWaypointManager.addPlayerWaypoint(player, serverWorld, waypointElement) -> WaypointElement`

##### Removing a waypoint:

`PlayerWaypointManager.removePlayerWaypoint(player, serverWorld, waypointElement) -> void`



## Using with Polymer
The waypoints are built upon [Polymer's Virtual Entity Element](https://polymer.pb4.eu/latest/polymer-virtual-entity/basics/), so you can add a `WaypointElement` the same way you would any other element.


## The WaypointElement Class
WaypointElements are the basic way of representing a waypoint for the Locator Bar

`<ctor> WaypointElement(ServerWorld, Identifier)`

### Config
`WaypointElement::getConfig(ServerPlayerEntity)` : Gets the Waypoint's client-bound config

`WaypointElement::setColor(int)` : Sets the color - <sub><sup>RGB

`WaypointElement::setStyle(RegistryKey<WaypointStyle>)` : Sets the Waypoint's style <sub><sup>[See: Minecraft Wiki / Waypoint Style](https://minecraft.wiki/w/Waypoint_style)<br></sup></sub>
<sup>- Note for Waypoint Styles: Distance calculation isn't working, so it will always show the "furthest" sprite. This is unfortunately a bug.<br></sup>
<sup>- However, you can override `getConfig(...)` and change the style manually depending on the distance using `getDistanceTo`. 

### Positioning and transmission
`WaypointElement::setWaypointTransmitRange(double)` : Sets the range the player can see the waypoint. The waypoint will stop transmitting after this distance.

`WaypointElement::setPosition(Vec3d)` : Sets the position of the waypoint.<br>
<sup> - You can also use `setOffset(...)` for the same result.

`WaypointElement::isPlayerInRange(PlayerEntity) -> boolean` : Whether the waypoint should transmit to this player or not.

`WaypointElement::getObservers() -> Set<PlayerEntity>` : Gets all players which this Waypoint is currently transmitting to

`WaypointElement::getDistanceTo(PlayerEntity)`<br>`WaypointElement::getSquaredDistanceTo(PlayerEntity` Get distance to a given player from the Waypoint

### Waypoint Holograms (WIP)
WaypointElements can have Holograms attached, to display information to the user.<br>
By default, holograms will display directly above the Waypoint.

This is very experimental, and may not work. I'd recommend just building holograms separately. 

This can be overridden using `getHologramElementPosition` 

`WaypointElement::setHologram(List<Text>)` : Sets the Hologram of the Waypoint using MarkerElements<br>
`WaypointElement::setHologram(List<HologramData>)` : Sets the hologram of the Waypoint

`WaypointElement::removeHologram(HologramData)` : Removes a Hologram from the Waypoint.

`getHologramElementPosition(Vec3d origin, int line, HologramData) -> Vec3d` : Gets the position of a Hologram element<br>
<sup> Defaults to `origin.offset(Direction.UP, (line * 0.25f))`



## Other utilities

`SVPlayerWaypoints.getHolderForWorld(ServerWorld)` : Gets a holder at exactly [0,0,0]