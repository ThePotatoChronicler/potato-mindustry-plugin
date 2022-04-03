package potato;
import java.util.Optional;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.Vars;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandRunner;

class Votemap implements ClientCommandRegister {

	public Optional<Map> votedmap = Optional.empty();

	public void registerClientCommands(CommandHandler handler) {
		handler.<Player>register("votemap", "[mapname...]", "Selects the next map to vote for and some other things.", votemap);
	}

	CommandRunner<Player> votemap =
		(args, player) -> {
			if (args.length == 0) {
				if (votedmap.isPresent()) {
					player.sendMessage("[green]Currently voting for map:\n[acid]" + votedmap.get().name());
				} else {
					player.sendMessage(Util.getStyledMaps());
				}
			} else {
				if (votedmap.isPresent()) {
					player.sendMessage("Already voting for map [cyan]" + votedmap.get().name());
				} else {
					Map map = Vars.maps.byName(args[0]);
					if (map == null) {
						player.sendMessage("[red]This map doesn't exist");
					} else {
						votedmap = Optional.of(map);
						Call.sendMessage(Util.getColoredPlayerName(player) + "[white] started a vote for map [acid]" + map.name());
					}
				}
			}
		};
}
