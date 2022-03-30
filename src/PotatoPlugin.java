import java.io.FileNotFoundException;
import arc.Events;
import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonReader;
import arc.util.CommandHandler;
import mindustry.maps.Map;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.game.EventType.*;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;

public class PotatoPlugin extends Plugin {

	public final String configPath = "config/potato.json";
	protected JsonValue settings;
	public WebhookClient wc;

	@Override
	public void init() {

		try {
			settings = readJson(configPath);
		} catch (FileNotFoundException exception) {
			Log.err("Missing config file for Potato plugin");
			return;
		}

		wc = (new WebhookClientBuilder(settings.getString("webhook_url"))).build();
		Events.on(PlayerJoin.class, event -> {
			int playerCount = Groups.player.size();
			if (Vars.state.serverPaused && playerCount > 0) {
				Vars.state.serverPaused = false;
			}

			wc.send(String.format("**%s** joined the server (%d player%s)", event.player.name, playerCount, playerCount == 1 ? "" : "s")).join();
		});

		Events.on(PlayerLeave.class, event -> {
			int playerCount = Groups.player.size() - 1;
			wc.send(String.format("**%s** left the server (%d player%s)", event.player.name, playerCount, playerCount == 1 ? "" : "s")).join();
			if (playerCount == 0 && Vars.state.serverPaused == false) {
				Vars.state.serverPaused = true;
			}
		});

		Events.on(WorldLoadEvent.class, event -> {
			int playerCount = Groups.player.size();
			if (playerCount == 0 && Vars.state.serverPaused == false) {
				Vars.state.serverPaused = true;
			}
		});

		Events.on(GameOverEvent.class, event -> {
			wc.send(String.format(
						"Game Over! Reached wave **%d** with **%d** players online on map **%s**",
						Vars.state.wave, Groups.player.size(), Vars.state.map.name())).join();
		});

		Events.on(WaveEvent.class, event -> {
			if ((Vars.state.wave % 25) == 0) {
				wc.send(String.format("**%d** online players reached wave **%d**", Groups.player.size(), Vars.state.wave)).join();
			}
		});
	}

    public void registerClientCommands(CommandHandler handler) {
		handler.<Player>register("votemap", "[mapname...]", "Selects the next map to vote for, or shows a list of all installed maps.",
				(args, player) -> {
					if (args.length == 0) {
						StringBuilder builder = new StringBuilder();
						builder.append("[orange]Maps: \n[]");
						int id = 0;
						for (Map m : Vars.maps.all()) {
							builder.append(m.name()).append("\n");
							id++;
						}
						player.sendMessage(builder.toString());
						return;
					}
				});
	}

	protected static JsonValue readJson(String path) throws FileNotFoundException {
		Fi config = Core.files.local(path);
		if (config.exists()) {
			return new JsonReader().parse(config);
		} else {
			throw new FileNotFoundException("Missing config/potato.json");
		}
	}
}
