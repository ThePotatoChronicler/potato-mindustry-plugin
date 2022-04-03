import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.jetbrains.annotations.NotNull;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.Log;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonReader;
import arc.util.CommandHandler;
import arc.util.Timer;
import mindustry.maps.Map;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.game.EventType.*;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;

public class PotatoPlugin extends Plugin {

	// Important variables
	public static final String configPath = "config/potato.json";
	protected JsonValue settings;
	protected ScheduledThreadPoolExecutor executor;
	public WebhookClient wc;
	protected CompletableFuture<?> lastMessage;

	// Voting variables
	public Optional<Map> votedmap = Optional.empty();

	public PotatoPlugin() {
		try {
			settings = readJson(configPath);
		} catch (FileNotFoundException exception) {
			Log.err("Missing config file for Potato plugin");
			return;
		}

		executor = new ScheduledThreadPoolExecutor(java.lang.Runtime.getRuntime().availableProcessors());

		wc = new WebhookClientBuilder(settings.getString("webhook_url"))
			.setExecutorService(executor)
			.build();

		lastMessage = CompletableFuture.completedFuture(null);
	}

	@Override
	public void init() {

		Events.on(PlayerJoin.class, event -> {
			int playerCount = Groups.player.size();
			dsend(
				String.format("**%s** joined the server (%d player%s)",
					event.player.name, playerCount, playerCount == 1 ? "" : "s"
					)
			);

			if (Vars.state.serverPaused && playerCount > 0) {
				Vars.state.serverPaused = false;
				dsend("Unpausing game.");
			}
		});

		Events.on(PlayerLeave.class, event -> {
			int playerCount = Groups.player.size() - 1;
			dsend(String.format("**%s** left the server (%d player%s)", event.player.name, playerCount, playerCount == 1 ? "" : "s"));
			if (playerCount == 0 && Vars.state.serverPaused == false) {
				Vars.state.serverPaused = true;
				dsend("All players left, pausing game.");
			}
		});

		Events.on(WorldLoadEvent.class, event -> {
			Timer.schedule(() -> {
				if (Groups.player.size() == 0 && !Vars.state.serverPaused) {
					Vars.state.serverPaused = true;
				}
			}, 5);
		});

		Events.on(GameOverEvent.class, event -> {
			dsend(String.format(
						"Game Over! Reached wave **%d** with **%d** players online on map **%s**",
						Vars.state.wave, Groups.player.size(), Vars.state.map.name()));
		});

		Events.on(WaveEvent.class, event -> {
			if ((Vars.state.wave % 25) == 0) {
				dsend(String.format("**%d** online players reached wave **%d**", Groups.player.size(), Vars.state.wave));
			}
		});
	}

	/* Asynchronously sends a discord message
	 */
	protected void dsend(@NotNull String s) {
		lastMessage = lastMessage.thenRunAsync(() -> wc.send(s));
	}

    public void registerClientCommands(CommandHandler handler) {
		handler.<Player>register("votemap", "[mapname/yes/no...]", "Selects the next map to vote for and some other things.",
				(args, player) -> {
					if (args.length == 0) {
						if (votedmap.isPresent()) {
							player.sendMessage("[green]Currently voting for map:\n[acid]" + votedmap.get().name());
						} else {
							player.sendMessage(getStyledMaps());
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
								Call.sendMessage(getColoredPlayerName(player) + "[white] started a vote for map [acid]" + map.name());
							}
						}
					}
				});
	}

	protected static JsonValue readJson(@NotNull String path) throws FileNotFoundException {
		Fi config = Core.files.local(path);
		if (config.exists()) {
			return new JsonReader().parse(config);
		} else {
			throw new FileNotFoundException("Missing config/potato.json");
		}
	}

	/* Gets a stylized list of maps
	 */
	public static String getStyledMaps(String titleColor, String customColor) {
		StringBuilder builder = new StringBuilder();
		builder.append(titleColor);
		builder.append("Maps: \n");

		Iterator<Map> iter = Vars.maps.all().iterator();
		while (iter.hasNext()) {
			Map m = iter.next();
			if (m.custom) {
				builder.append(m.name());
			} else {
				builder.append(customColor).append(m.name());
			}

			if (iter.hasNext()) {
				builder.append("[white], ");
			} else {
				builder.append("[white].");
			}
		}
		return builder.toString();
	}

	public static String getStyledMaps(String customColor) {
		return getStyledMaps("[orange]", customColor);
	}

	public static String getStyledMaps() {
		return getStyledMaps("[cyan]");
	}

	public static String getColoredPlayerName(Player player) {
		return "[#" + player.color.toString() + "]" + player.name + "[]";
	}
}
