package de.bjarne.sigguard.core.reminder;

import java.util.List;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.world.item.ItemStack;

/**
 * Merkt sich das zuletzt gehoverte Item, damit die Timer-Taste weiß, worauf sie sich bezieht.
 */
public class HoverTracker {

  // Tooltip wird jeden Frame neu gebaut; älter als das gilt als "nicht mehr gehovert"
  private static final long MAX_AGE_MILLIS = 250;

  private ItemStack itemStack;
  private List<String> lines = List.of();
  private List<Component> rawLines = List.of();
  private List<String> verdict = List.of();
  private List<String> codes = List.of();
  private String signer;
  private String signature = "KEINE_SIGNATUR";
  private long lastSeen;

  /**
   * @param rawLines Tooltip-Zeilen vor unseren Zusätzen (für Fehlermeldungen)
   * @param verdict  Zeilen, die das Addon selbst angehängt hat (Warnung oder "Echt"-Hinweis)
   * @param codes    dieselben Urteile als sprachunabhängige Kürzel
   * @param signer   gefundener Signierer oder {@code null}
   * @param signature Ergebnis der Signaturprüfung oder "KEINE_SIGNATUR"
   */
  public synchronized void update(ItemStack itemStack, List<String> lines,
      List<Component> rawLines, List<String> verdict, List<String> codes, String signer,
      String signature) {
    this.itemStack = itemStack;
    this.lines = List.copyOf(lines);
    this.rawLines = rawLines;
    this.verdict = List.copyOf(verdict);
    this.codes = List.copyOf(codes);
    this.signer = signer;
    this.signature = signature;
    this.lastSeen = System.currentTimeMillis();
  }

  public synchronized Hovered current() {
    if (this.itemStack == null || System.currentTimeMillis() - this.lastSeen > MAX_AGE_MILLIS) {
      return null;
    }
    return new Hovered(this.itemStack, this.lines, this.rawLines, this.verdict, this.codes,
        this.signer, this.signature);
  }

  public record Hovered(ItemStack itemStack, List<String> lines, List<Component> rawLines,
      List<String> verdict, List<String> codes, String signer, String signature) {
  }
}
