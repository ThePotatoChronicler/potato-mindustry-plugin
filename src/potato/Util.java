package potato;

import mindustry.Vars;
import mindustry.gen.*;
import mindustry.maps.Map;
import arc.files.Fi;
import arc.Core;
import java.util.Iterator;
import java.io.FileNotFoundException;
import org.jetbrains.annotations.NotNull;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonReader;

public class Util {
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
}