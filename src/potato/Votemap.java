package potato;

import arc.ApplicationListener;
import arc.Core;
import arc.struct.ObjectMap;
import arc.util.Timer;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandRunner;
import arc.util.Log;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.Vars;

class Votemap implements ClientCommandRegister {

	public final static int voteLength = 60;
	public final static int voteInterval = 15;
	public final static double ratio = 0.5d;
	protected Optional<Map> votedmap = Optional.empty();
	protected ObjectMap<String, Boolean> votes = new ObjectMap<>();

	public void registerClientCommands(CommandHandler handler) {
		handler.<Player>register("votemap", "[mapname...]", "Selects the next map to vote for and some other things.", votemap);
		handler.<Player>register("vote", "<yes/no/clear>", "Votes for or against the next map, or clears a previous vote.", vote);
	}

	protected void initializeVote() {
		for (int delay = voteInterval; delay < voteLength; delay += voteInterval) {
			final int time_left = voteLength - delay; // delay can't be used in lambda, this is a workaround
			Timer.schedule(() -> {
				Call.sendMessage(String.valueOf(time_left) + " seconds until end of vote");
			}, delay);
		}

		Timer.schedule(finalizeVote, voteLength);
	}

	public static void overrideNextMap(@NotNull Map m) {
		// TODO: Implement this lol
	}

	Runnable finalizeVote =
		() -> {
			int yes = 0;
			for (Boolean vote : votes.values()) {
				if (vote) yes++;
			}
			if ((double)yes / (double)votes.size >= ratio) {
				Call.sendMessage("[cyan]Changing map to [acid]" + votedmap.get().name());
			} else {
				Call.sendMessage("[red]Vote failed");
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
						votedmap = Optional.of(map);
						Call.sendMessage(
								Util.getColoredPlayerName(player) + " [white]started a " +
								String.valueOf(voteLength) + " second vote for map [acid]" + map.name()
								);
						initializeVote();
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
