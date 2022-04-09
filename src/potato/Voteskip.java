package potato;

import arc.struct.ObjectMap;
import arc.util.Timer;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandRunner;
import mindustry.gen.*;

class Voteskip implements ClientCommandRegister {

	public final static int voteLength = 30;
	public final static int voteInterval = 10;
	public final static double ratio = 0.5d;
	protected boolean voting = false;
	protected ObjectMap<String, Boolean> votes = new ObjectMap<>();

	public void registerClientCommands(CommandHandler handler) {
		handler.<Player>register("voteskip", "[yes/no/clear]", "Starts a vote to skip, or sets your choice", voteskip);
	}

	protected void initializeVote() {
		if (Groups.player.size() == 1) {
			Call.sendMessage("[cyan]Skipping vote due to only one player being online");
			Util.gameOver();
			return;
		}

		voting = true;
		for (int delay = voteInterval; delay < voteLength; delay += voteInterval) {
			final int time_left = voteLength - delay; // delay can't be used in lambda, this is a workaround
			Timer.schedule(() -> {
				Call.sendMessage(String.valueOf(time_left) + " seconds until end of skip vote");
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
				Call.sendMessage("[cyan]Skipping map " + rateString);
				Util.gameOver();
			} else {
				Call.sendMessage("[red]Map skip vote failed " + rateString);
			}
			votes.clear();
			voting = false;
		};

	CommandRunner<Player> voteskip =
		(args, player) -> {
			if (args.length == 0) {
				if (voting) {
					player.sendMessage("[red]A vote is already ongoing.");
				} else {
					initializeVote();
					Call.sendMessage(Util.getColoredPlayerName(player) + " [cyan]started a " + String.valueOf(voteLength) + " second vote to skip map.");
				}
			} else {
				if (voting) {
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
				} else {
					player.sendMessage("[red]No ongoing vote.");
				}
			}
		};
}
