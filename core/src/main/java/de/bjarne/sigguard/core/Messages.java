package de.bjarne.sigguard.core;

import net.labymod.api.Laby;
import net.labymod.api.client.chat.ChatExecutor;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.component.format.TextDecoration;
import net.labymod.api.util.I18n;

/**
 * Baut übersetzte Texte aus den Sprachdateien (assets/sigguard/i18n).
 * Platzhalter in den Sprachdateien: %s.
 */
public final class Messages {

  private static final String PREFIX = "sigguard.messages.";

  private Messages() {
  }

  /** Übersetzter Text; Argumente werden als einfacher Text eingesetzt. */
  public static Component tr(String key, TextColor color, Object... arguments) {
    Component[] components = new Component[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      Object argument = arguments[i];
      components[i] = argument instanceof Component component
          ? component
          : Component.text(String.valueOf(argument));
    }
    return Component.translatable(PREFIX + key, color, components);
  }

  /** Übersetzter Text mit vorangestelltem Symbol in derselben Farbe. */
  public static Component withSymbol(String symbol, String key, TextColor color,
      Object... arguments) {
    return Component.text(symbol + " ", color).append(tr(key, color, arguments));
  }

  /** Übersetzter Text als einfacher String (Platzhalter %s werden ersetzt). */
  public static String text(String key, Object... arguments) {
    return I18n.translate(PREFIX + key, arguments);
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
    for (String word : message.trim().split("\s+")) {
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
    chat.displayClientMessage(Component.text(" "));
    chat.displayClientMessage(line);
    chat.displayClientMessage(Component.text(" "));
  }
}
