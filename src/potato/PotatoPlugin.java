package potato;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.jetbrains.annotations.NotNull;
import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.serialization.JsonValue;
import arc.util.CommandHandler;
import arc.util.Timer;
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

	// Command Registers
	protected ClientCommandRegister[] clientCommandRegisters = { new Votemap(), new Voteskip() };
	protected ServerCommandRegister[] serverCommandRegisters = {};

	public PotatoPlugin() {
		try {
			settings = Util.readJson(configPath);
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
			int oldPlayerCount = Groups.player.size() - 1;
			Seq<Player> players = Groups.player.copy(new Seq<>());
			players.remove(event.player);
			int playerCount = players.size;

			dsend(String.format("**%s** left the server (%d player%s [old: %d])",
						event.player.name, playerCount, playerCount == 1 ? "" : "s", oldPlayerCount));
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

	@Override
	public void registerClientCommands(CommandHandler handler) {
		for (ClientCommandRegister register : clientCommandRegisters) {
			register.registerClientCommands(handler);
		}
	}

	@Override
	public void registerServerCommands(CommandHandler handler) {
		for (ServerCommandRegister register : serverCommandRegisters) {
			register.registerServerCommands(handler);
		}
	}
}
