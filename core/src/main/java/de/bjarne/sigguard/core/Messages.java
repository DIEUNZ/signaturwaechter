package de.bjarne.sigguard.core;

import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;

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

  /** Chat-Nachricht mit "[AH-Fake-Warner]"-Präfix. */
  public static Component chat(String key, TextColor color, Object... arguments) {
    return Component.translatable(PREFIX + "prefix", NamedTextColor.DARK_AQUA)
        .append(Component.text(" "))
        .append(tr(key, color, arguments));
  }
}
