StartupEvents.registry('palladium:abilities', (event) => {
   event.create('mypowers:property_transfer')
       .icon(palladium.createItemIcon('minecraft:armor_stand'))
       .addProperty("distance", "float", 5.0, "Distance to search for armor stands")
       .addProperty("tag", "string", "crystal_spike", "Tag to look for on armor stands")
       .tick((entity, entry, holder, enabled) => {
           if (enabled && entity.isPlayer()) {
               let distance = entry.getPropertyByName("distance");
               let tag = entry.getPropertyByName("tag");
               
               let nearbyEntities = entity.level.getEntitiesWithin(AABB.of(
                   entity.x - distance, entity.y - distance, entity.z - distance,
                   entity.x + distance, entity.y + distance, entity.z + distance
               ));
               
               nearbyEntities.forEach(nearbyEntity => {
                   if (nearbyEntity.type === 'minecraft:armor_stand' && nearbyEntity.tags.contains(tag)) {
                       let uniform = palladium.getProperty(entity, 'uniform') || "prototype";
                       if (palladium.getProperty(nearbyEntity, 'uniform') !== uniform) {
                           palladium.setProperty(nearbyEntity, 'uniform', uniform);
                       }
                       
                       let color1 = palladium.getProperty(entity, 'celestialsapien_' + uniform + '_glowcolor_2_color_1') || "EF0909";
                       let color2 = palladium.getProperty(entity, 'celestialsapien_' + uniform + '_glowcolor_2_color_2') || "CE4E4A";
                       let color3 = palladium.getProperty(entity, 'celestialsapien_' + uniform + '_glowcolor_2_color_3') || "A99C86";
                       let color4 = palladium.getProperty(entity, 'celestialsapien_' + uniform + '_glowcolor_2_color_4') || "870B18";
                       let color5 = palladium.getProperty(entity, 'celestialsapien_' + uniform + '_glowcolor_2_color_5') || "6DE1F8";
                       let color6 = palladium.getProperty(entity, 'celestialsapien_' + uniform + '_glowcolor_2_color_6') || "58B764";
                       
                       if (palladium.getProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_1') !== color1) {
                           palladium.setProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_1', color1);
                       }
                       if (palladium.getProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_2') !== color2) {
                           palladium.setProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_2', color2);
                       }
                       if (palladium.getProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_3') !== color3) {
                           palladium.setProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_3', color3);
                       }
                       if (palladium.getProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_4') !== color4) {
                           palladium.setProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_4', color4);
                       }
                       if (palladium.getProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_5') !== color5) {
                           palladium.setProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_5', color5);
                       }
                       if (palladium.getProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_6') !== color6) {
                           palladium.setProperty(nearbyEntity, 'celestialsapien_' + uniform + '_glowcolor_2_color_6', color6);
                       }
                   }
               });
           }
       });
});