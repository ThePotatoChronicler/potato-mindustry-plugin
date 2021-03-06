package potato;

import arc.struct.ObjectMap;
import arc.util.Timer;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandRunner;
import java.util.Optional;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.Vars;

class Votemap implements ClientCommandRegister {

	public final static int voteLength = 30;
	public final static int voteInterval = 10;
	public final static double ratio = 0.5d;
	protected Optional<Map> votedmap = Optional.empty();
	protected ObjectMap<String, Boolean> votes = new ObjectMap<>();

	public void registerClientCommands(CommandHandler handler) {
		handler.<Player>register("votemap", "[mapname...]", "Selects the next map to vote for and some other things.", votemap);
		handler.<Player>register("vote", "<yes/no/clear>", "Votes for or against the next map, or clears a previous vote.", vote);
	}

	protected void initializeVote(Map map) {
		if (Groups.player.size() == 1) {
			Call.sendMessage("[cyan]Skipping vote due to only one player being online");
			Util.overrideNextMap(map);
			return;
		}

		votedmap = Optional.of(map);

		for (int delay = voteInterval; delay < voteLength; delay += voteInterval) {
			final int time_left = voteLength - delay; // delay can't be used in lambda, this is a workaround
			Timer.schedule(() -> {
				Call.sendMessage(String.valueOf(time_left) + " seconds until end of map vote");
			}, delay);
		}

		Timer.schedule(finalizeVote, voteLength);
	}

	Runnable finalizeVote =
		() -> {
			int yes = 0;
			for (Boolean vote : votes.values()) {
				if (vote) yes++;
			}

			String rateString =
				String.format("[[[green]%d[]/[red]%d[]/[yellow]%d[]]",
					yes,
					votes.size - yes,
					votes.size
					);
			if ((double)yes / (double)votes.size >= ratio) {
				Call.sendMessage("[cyan]Changing next map to [acid]" + votedmap.get().name() + " []" + rateString);
				Util.overrideNextMap(votedmap.get());
			} else {
				Call.sendMessage("[red]Map change vote failed " + rateString);
			}
			votes.clear();
			votedmap = Optional.empty();
		};

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
						initializeVote(map);
						Call.sendMessage(
								Util.getColoredPlayerName(player) + " [cyan]started a " +
								String.valueOf(voteLength) + " second vote for map [acid]" + map.name()
								);
					}
				}
			}
		};

	CommandRunner<Player> vote =
		(args, player) -> {
			if (votedmap.isEmpty()) {
				player.sendMessage("[red]Not voting for a map");
			} else {
				Boolean val = Util.stringToBool(args[0]);
				if (val == null) {
					if (args[0].equals("c") || args[0].equals("clear")) {
						votes.remove(player.uuid());
						player.sendMessage("[cyan]Cleared vote");
					} else {
						player.sendMessage("[red]Not a valid argument");
					}
				} else {
					String votestr = (val ? "[green]" : "[red]") + args[0];
					if (votes.containsKey(player.uuid())) {
						player.sendMessage("[cyan]Changed vote to " + votestr + "[].");
					} else {
						player.sendMessage("[cyan]Set vote to " + votestr + "[].");
					}
					votes.put(player.uuid(), val);
				}
			}
		};
}
