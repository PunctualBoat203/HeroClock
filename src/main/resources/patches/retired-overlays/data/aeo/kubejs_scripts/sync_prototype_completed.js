ServerEvents.tick(function (event) {

  var server = event.server;
  var players = server.players || [];

  for (var i = 0; i < players.length; i++) {
    var player = players[i];
    try {
      try {
        var inited = !!player.persistentData.getBoolean('aeo_completed_initialized');
        if (!inited) {
          try { palladium.scoreboard.setScore(player, 'AEO.CompletedSkillP', 0); }
          catch (errSetInit) {
            try {
              var unameInit = player.getGameProfile().getName();
              player.server.runCommandSilent('scoreboard players set ' + unameInit + ' AEO.CompletedSkillP 0');
            } catch (errCmdInit) {  }
          }
          try { player.persistentData.putBoolean('aeo_completed_initialized', true); } catch (e) { }

          try { player.persistentData.putInt('aeo_last_prototype_skillp', palladium.scoreboard.getScore(player, 'AlienEvo.PrototypeSkillP', 0) || 0); } catch (e) { }
        }
      } catch (e) { }

      var current = palladium.scoreboard.getScore(player, 'AlienEvo.PrototypeSkillP', 0) || 0;
      var last = player.persistentData.getInt('aeo_last_prototype_skillp') || 0;

      if (current > last) {
        var delta = current - last;

        var completed = palladium.scoreboard.getScore(player, 'AEO.CompletedSkillP', 0) || 0;
        var newVal = completed + delta;
        try {
          palladium.scoreboard.setScore(player, 'AEO.CompletedSkillP', newVal);
        } catch (errSet) {
          try {
            var username = player.getGameProfile().getName();
            player.server.runCommandSilent('scoreboard players set ' + username + ' AEO.CompletedSkillP ' + newVal);
          } catch (errCmd) {  }
        }
      }

      if (current !== last) { try { player.persistentData.putInt('aeo_last_prototype_skillp', current); } catch (e) { } }
    } catch (err) {
    }
  }
});

