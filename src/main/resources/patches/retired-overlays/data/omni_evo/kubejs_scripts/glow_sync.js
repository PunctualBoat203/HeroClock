let glowCheckTickCounter = 0;

ServerEvents.tick(event => {
    glowCheckTickCounter++;
    if (glowCheckTickCounter % 40 !== 0) return;

    event.server.getPlayers().forEach(player => {
        let watch = palladium.getProperty(player, 'watch');

        if (watch === 'recal' || abilityUtil.hasPower(player, "alienevo:recal_omnitrix")) {
            ensureGlowDefaults(player, 'recal');
        }
        if (watch === 'ult' || abilityUtil.hasPower(player, "alienevo:ult_omnitrix")) {
            ensureGlowDefaults(player, 'ult');
        }
    });
});

function ensureGlowDefaults(entity, prefix) {
    let c1 = palladium.getProperty(entity, prefix + '_glow_color_1');
    if (!c1 || c1 === 'null' || c1 === 'undefined' || c1 === '') {
        palladium.setProperty(entity, prefix + '_glow_color_base', '0,0,0');
        palladium.setProperty(entity, prefix + '_glow_color_1', 'b3ff40');
        palladium.setProperty(entity, prefix + '_glow_color_2', 'a7f72e');
        palladium.setProperty(entity, prefix + '_glow_color_3', '8ed721');
        palladium.setProperty(entity, prefix + '_glow_color_4', '77b81a');
        palladium.setProperty(entity, prefix + '_glow_color_5', '639d11');
    }

    let u1 = palladium.getProperty(entity, 'uniform_glow_color_1');
    if (!u1 || u1 === 'null' || u1 === 'undefined' || u1 === '') {
        palladium.setProperty(entity, 'uniform_glow_color_base', '0,0,0');
        palladium.setProperty(entity, 'uniform_glow_color_1', 'ffffff');
        palladium.setProperty(entity, 'uniform_glow_color_2', 'eaeaea');
        palladium.setProperty(entity, 'uniform_glow_color_3', 'cfcfdd');
        palladium.setProperty(entity, 'uniform_glow_color_4', 'b9b7cd');
        palladium.setProperty(entity, 'uniform_glow_color_5', '9f9cb6');
    }

    syncWatchToUniform(entity, prefix);
}

function syncWatchToUniform(entity, prefix) {
    for (let i = 1; i <= 5; i++) {
        let watchColor = palladium.getProperty(entity, prefix + '_glow_color_' + i);
        if (watchColor && watchColor !== 'null' && watchColor !== 'undefined' && watchColor !== '') {
            let uniformKey = 'uniform_glow_color_' + i;
            if (palladium.getProperty(entity, uniformKey) !== watchColor) {
                palladium.setProperty(entity, uniformKey, watchColor);
            }
        }
    }
    let watchBase = palladium.getProperty(entity, prefix + '_glow_color_base');
    if (watchBase && watchBase !== 'null' && watchBase !== 'undefined' && watchBase !== '') {
        if (palladium.getProperty(entity, 'uniform_glow_color_base') !== watchBase) {
            palladium.setProperty(entity, 'uniform_glow_color_base', watchBase);
        }
    }
}
