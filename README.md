# PlayerStatsExpansion

Adds placeholders for [PlayerStats](https://www.spigotmc.org/resources/playerstats.102347/)!  
On this page, you'll find an explanation of how to use the placeholders, including some detailed [examples](https://github.com/Artemis-the-gr8/PlayerStatsExpansion/edit/main/README.md#examples). 
&nbsp;  
&nbsp;  


## General Formatting Rules
* Each placeholder starts with `%playerstats_`
* The different keywords of a placeholder are separated by **commas**: 
    - `%playerstats_ x, y, z%`
* If a keyword requires an additional argument, the keyword is followed by the argument with a **colon** in between: 
    - `%playerstats_ x:1, y, z%`
* Whitespaces are allowed

## Structure
You can use placeholders to display 3 different kinds of statistics:

|     prefix    | selection |:| specification | statistic |       example     |
| ------------- | --------- |-| ------------- | --------- |  ---------------- |
| %playerstats_ | top       |:| line-number   | [stat_name](https://github.com/Artemis-the-gr8/PlayerStatsExpansion/edit/main/README.md#statistic-choices) |  `%playerstats_ top:1, animals_bred%` |
|               | player    |:| player-name   |           |  `%playerstats_ player:Artemis_the_gr8, jump%` |
|               | server    | |               |           |  `%playerstats_ server, deaths%` |

&nbsp;  

## Keywords
...

## Statistic Choices
The placeholders support all vanilla Minecraft statistics, as they are declared in the 
[Bukkit API](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Statistic.html).  
These statistics can be divided into two types: **general** ones, and ones that need a 
[block](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html), 
[item](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html) or 
[entity](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html) as **sub-statistic**.

```
- mine_block: block_name
- craft_item: item_name
- use_item: item_name
- break_item: item_name
- pickup: item_name
- drop: item_name
- kill_entity: entity_name
- entity_killed_by: entity_name
```
All of the above need a sub-statistic as an argument to work (the block, item, or entity-name).  
These are all the general statistics:
```
- animals_bred
- armor_cleaned
- aviate_one_cm
- banner_cleaned
- beacon_interaction
- bell_ring
- boat_one_cm
- brewingstand_interaction
- cake_slices_eaten
- cauldron_filled
- cauldron_used
- chest_opened
- clean_shulker_box
- climb_one_cm
- crafting_table_interaction
- crouch_one_cm
- damage_dealt
- damage_dealt_absorbed
- damage_dealt_resisted
- damage_taken
- damage_blocked_by_shield
- damage_absorbed
- damage_resisted
- deaths
- dispenser_inspected
- drop_count
- dropper_inspected
- enderchest_opened
- fall_one_cm
- flower_potted
- fly_one_cm
- fish_caught
- furnace_interaction
- hopper_inspected
- horse_one_cm
- interact_with_anvil
- interact_with_blast_furnace
- interact_with_campfire
- interact_with_cartography_table
- interact_with_grindstone
- interact_with_lectern
- interact_with_loom
- interact_with_smithing_table
- interact_with_smoker
- interact_with_stonecutter
- item_enchanted
- jump
- leave_game
- minecart_one_cm
- mob_kills
- noteblock_played
- noteblock_tuned
- open_barrel
- pig_one_cm
- play_one_minute
- player_kills
- raid_trigger
- raid_win
- record_played
- shulker_box_opened
- sleep_in_bed
- sneak_time
- sprint_one_cm
- strider_one_cm
- swim_one_cm
- talked_to_villager
- target_hit
- time_since_death
- time_since_rest
- total_world_time
- traded_with_villager
- trapped_chest_triggered
- walk_on_water_one_cm
- walk_one_cm
- walk_under_water_one_cm
```

&nbsp;
&nbsp;
## Examples

*(the below picture includes the following placeholders...)*
<p align="center">
   <img src="src/main/resources/images/placeholders.png">
</p>
