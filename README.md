# DungeonsGenerator
A simple generator for Hypixel SkyBlock-like dungeons.
Made for: mc.skysim.sbs

The generation process is divided into 8 different phases:
- Generating layout - the generator generates a 2-dimensional integer
array representing a room layout of a dungeon. Each cell contains
an ID which can be used to access a specific room object.
- Picking starting room, fairy room, blood room - because of each
dungeon having to contain those 3 rooms, they will be choosen from
available 1x1 rooms matching certain conditions.
- Creating wither doors - in Hypixel SkyBlock each dungeon has
a path connecting starting room, fairy room and blood room. It's impossible
to access a "Wither Room" without having direct access to the previous one.
The generator tries to replicate this in this generation phase.
- Creating other doors - generates other doors so that there is a path
from the starting room to each other room and it's impossible to access a wither
room without opening the doors of the previous one.
- Choosing rooms - looks for schematics which can be used to represent the rooms.
Each schematic should contain signs with their 1st lines having a string "Door::this"
in order to mark a possible door location.
- Pasting rooms - pastes room schematics in their respective locations.
- Pasting doors - pastes door schematics.
- Finished - is a placeholder for the GenerationStage enum
which clarifies the fact that the generation has finished.

In case of the generator being unable to generate a proper layout or
find schematics matching the rooms, the generation will be restarted.
The generation function, without taking into accounting a need
to restart the generation, has a time complexity of O(n^2).
