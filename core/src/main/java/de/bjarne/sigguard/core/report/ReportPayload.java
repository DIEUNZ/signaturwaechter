package de.bjarne.sigguard.core.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.List;

/**
 * Baut den Discord-Webhook-Inhalt (ein Embed) für eine Fehlermeldung und kürzt zu lange Texte
 * auf die Discord-Grenzen. Enthält keinen Minecraft-Code.
 */
public final class ReportPayload {

  static final int FIELD_LIMIT = 1024;
  static final int DESCRIPTION_LIMIT = 4096;
  private static final String CUT_MARK = "\n… [gekürzt]";

  private ReportPayload() {
  }

  /**
   * @param kind     Fehlerart (Klartext)
   * @param machine  maschinenlesbare Zusammenfassung (kompaktes JSON, steht oben im Embed)
   * @param itemName Itemname mit Farbcodes als Rohtext
   * @param lore     komplette Lore mit Farbcodes als Rohtext
   * @param nbt      relevante NBT-Daten als Text
   * @param verdict  Urteil des Addons samt Begründung
   */
  public static JsonObject build(String kind, JsonObject machine, String itemName,
      List<String> lore, String nbt, String verdict, String addonVersion, String minecraftVersion, String player, Instant time) {
    JsonObject embed = new JsonObject();
    embed.addProperty("title", "Fehlermeldung: " + kind);
    // Erst die maschinenlesbare Zeile (bleibt vollständig), danach die Lore im Rest des Platzes
    String head = "```json\n" + machine.toString().replace("```", "'''") + "\n```\n";
    embed.addProperty("description",
        head + cut(code(String.join("\n", lore)), DESCRIPTION_LIMIT - head.length()));
    embed.addProperty("timestamp", time.toString());

    JsonArray fields = new JsonArray();
    field(fields, "Item-Name", code(itemName), false);
    field(fields, "NBT-Daten", code(nbt), false);
    field(fields, "Urteil des Addons", verdict, false);
    field(fields, "Addon-Version", addonVersion, true);
    field(fields, "Minecraft-Version", minecraftVersion, true);
    field(fields, "Melder", player, true);
    embed.add("fields", fields);

    JsonArray embeds = new JsonArray();
    embeds.add(embed);
    JsonObject body = new JsonObject();
    body.addProperty("username", "AH-Fake-Warner");
    body.add("embeds", embeds);
    return body;
  }

  private static void field(JsonArray fields, String name, String value, boolean inline) {
    JsonObject field = new JsonObject();
    field.addProperty("name", name);
    field.addProperty("value", cut(value.isBlank() ? "-" : value, FIELD_LIMIT));
    field.addProperty("inline", inline);
    fields.add(field);
  }

  // Als Codeblock, damit Farbcodes (&4) und Markdown-Zeichen unverändert lesbar bleiben
  private static String code(String text) {
    return "```\n" + text.replace("```", "'''") + "\n```";
  }

  /** Kürzt auf die Grenze und kennzeichnet das; der Codeblock bleibt dabei geschlossen. */
  static String cut(String text, int limit) {
    if (text.length() <= limit) {
      return text;
    }
    boolean block = text.startsWith("```");
    String tail = block ? CUT_MARK + "\n```" : CUT_MARK;
    return text.substring(0, limit - tail.length()) + tail;
  }
}
