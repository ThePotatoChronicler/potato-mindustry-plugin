package potato;

import mindustry.game.EventType.GameOverEvent;
import mindustry.game.Team;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.server.ServerControl;
import arc.files.Fi;
import arc.Core;
import arc.Events;
import arc.util.Reflect;
import java.util.Iterator;
import java.util.Arrays;
import java.io.FileNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonReader;

public class Util {

	public static final String[] trueStrings = { "y", "yes", "true" };
	public static final String[] falseStrings = { "n", "no", "false" };

	public static JsonValue readJson(@NotNull String path) throws FileNotFoundException {
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

	@Nullable
	public static Boolean stringToBool(String s) {
		String ls = s.toLowerCase();

		if (Arrays.stream(trueStrings).anyMatch(a -> ls.equals(a))) {
			return Boolean.TRUE;
		} else if (Arrays.stream(falseStrings).anyMatch(a -> ls.equals(a))) {
			return Boolean.FALSE;
		}
		return null;
	}

	/* Overrides next map, returning the old one
	 */
	public static Map overrideNextMap(Map override) {
		ServerControl ctrl = (ServerControl)Core.app.getListeners().find(appl -> appl instanceof ServerControl);
		Map old = Reflect.get(ctrl, "nextMapOverride");
		Reflect.set(ctrl, "nextMapOverride", override);
		return old;
	}

	/* Fires a game over
	 */
	public static void gameOver() {
		Events.fire(new GameOverEvent(Team.derelict));
	}
}
