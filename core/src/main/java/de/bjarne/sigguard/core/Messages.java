package de.bjarne.sigguard.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.bjarne.sigguard.core.SigGuardConfiguration.Language;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.ChatExecutor;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.component.format.TextDecoration;
import net.labymod.api.util.I18n;

/**
 * Baut übersetzte Texte aus den Sprachdateien (assets/sigguard/i18n).
 * Die Sprache der Addon-Meldungen ist in den Einstellungen wählbar (Standard: Deutsch),
 * unabhängig von der Spielsprache. Platzhalter in den Sprachdateien: %s.
 */
public final class Messages {

  private static final String PREFIX = "sigguard.messages.";
  private static final Map<String, JsonObject> FILES = new HashMap<>();

  private static Supplier<Language> language = () -> Language.GERMAN;

  private Messages() {
  }

  public static void language(Supplier<Language> supplier) {
    language = supplier;
  }

  /** Übersetzter Text; Argumente werden als einfacher Text eingesetzt. */
  public static Component tr(String key, TextColor color, Object... arguments) {
    return Component.text(text(key, arguments), color);
  }

  /** Übersetzter Text mit vorangestelltem Symbol in derselben Farbe. */
  public static Component withSymbol(String symbol, String key, TextColor color,
      Object... arguments) {
    return Component.text(symbol + " ", color).append(tr(key, color, arguments));
  }

  /** Übersetzter Text als einfacher String (Platzhalter %s werden ersetzt). */
  public static String text(String key, Object... arguments) {
    Language selected = language.get();
    if (selected == Language.GAME) {
      return I18n.translate(PREFIX + key, arguments);
    }
    String template = lookup(selected == Language.GERMAN ? "de_de" : "en_us", key);
    if (template == null) {
      return I18n.translate(PREFIX + key, arguments);
    }
    for (Object argument : arguments) {
      int index = template.indexOf("%s");
      if (index < 0) {
        break;
      }
      template = template.substring(0, index) + argument + template.substring(index + 2);
    }
    return template;
  }

  // Liest den Text direkt aus der Sprachdatei des Addons
  private static synchronized String lookup(String file, String key) {
    JsonObject root = FILES.computeIfAbsent(file, Messages::load);
    JsonElement node = root;
    for (String part : key.split("\\.")) {
      if (node == null || !node.isJsonObject()) {
        return null;
      }
      node = node.getAsJsonObject().get(part);
    }
    return node != null && node.isJsonPrimitive() ? node.getAsString() : null;
  }

  private static JsonObject load(String file) {
    try (InputStream stream = Messages.class.getResourceAsStream(
        "/assets/sigguard/i18n/" + file + ".json")) {
      if (stream == null) {
        return new JsonObject();
      }
      JsonObject root = JsonParser.parseReader(
          new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
      return root.getAsJsonObject("sigguard").getAsJsonObject("messages");
    } catch (Exception exception) {
      return new JsonObject();
    }
  }

  /** Chat-Nachricht mit "[AH-Fake-Warner]"-Präfix. */
  public static void send(String key, Object... arguments) {
    emit(text("prefix") + " " + text(key, arguments));
  }

  /** Chat-Nachricht ohne Präfix, stattdessen mit vorangestelltem Symbol. */
  public static void sendWithSymbol(String symbol, String text) {
    emit(symbol + " " + text);
  }

  /**
   * Gibt eine Nachricht im Chat aus: eine Leerzeile davor und danach, die Wörter
   * abwechselnd fett in Dunkelrot (&4&l) und Weiß (&f&l). Nur clientseitig.
   */
  public static void emit(String message) {
    ChatExecutor chat = Laby.labyAPI().minecraft().chatExecutor();
    Component line = Component.empty();
    boolean red = true;
    for (String word : message.trim().split("\\s+")) {
      if (word.isEmpty()) {
        continue;
      }
      if (!red || line.children().size() > 0) {
        line = line.append(Component.text(" "));
      }
      line = line.append(Component.text(word, red ? NamedTextColor.DARK_RED : NamedTextColor.WHITE)
          .decorate(TextDecoration.BOLD));
      red = !red;
    }
    // Leerzeilen als Zeilenumbrüche in EINER Nachricht: einzelne leere Nachrichten
    // werden vom Chat nicht angezeigt.
    chat.displayClientMessage(Component.text("\n ").append(line).append(Component.text("\n ")));
  }
}
