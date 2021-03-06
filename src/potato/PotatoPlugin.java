package potato;

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Optional;
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

public class PotatoPlugin extends Plugin {

	// Important variables
	public static final String configPath = "config/potato.json";
	protected JsonValue settings;
	protected ScheduledThreadPoolExecutor executor;
	protected WebhookClient wc;
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

		wc = new WebhookClient(settings.getString("webhook_url"), Optional.of(executor));

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

			/* This method of getting the player list is used instead of directly
			 * using Groups.player, because Groups.player inconsistently removes
			 * players after leaving. Sometimes, they're left in this list after
			 * leaving, sometimes they're not. This is one such way to assure
			 * you always get the correct list.
			 */
			Seq<Player> players = Groups.player.copy(new Seq<>());
			players.remove(event.player);
			int playerCount = players.size;

			dsend(String.format("**%s** left the server (%d player%s)",
						event.player.name, playerCount, playerCount == 1 ? "" : "s"));
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

		Events.on(PlayerChatEvent.class, event -> {
			String url = "https://mindustrygame.github.io/wiki/images/unit-" + event.player.unit().type.name + "-ui.png";
			dsend(event.message, event.player.name(), url);
		});
	}

	/* Asynchronously sends a discord message
	 */
	protected void dsend(@NotNull String content) {
		// There is no check if it's non-null because WebhookClient.send already does that
		lastMessage = lastMessage.thenComposeAsync((r) -> wc.send(content));
	}

	public void dsend(@NotNull String content, String username) {
		lastMessage = lastMessage.thenComposeAsync((r) -> wc.send(content, username));
	}

	public void dsend(@NotNull String content, String username, String avatar) {
		lastMessage = lastMessage.thenComposeAsync((r) -> wc.send(content, username, avatar));
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
