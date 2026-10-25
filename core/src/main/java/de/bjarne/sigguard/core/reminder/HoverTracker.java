package de.bjarne.sigguard.core.reminder;

import java.util.List;
import net.labymod.api.client.world.item.ItemStack;

/**
 * Merkt sich das zuletzt gehoverte Item, damit die Timer-Taste weiß, worauf sie sich bezieht.
 */
public class HoverTracker {

  // Tooltip wird jeden Frame neu gebaut; älter als das gilt als "nicht mehr gehovert"
  private static final long MAX_AGE_MILLIS = 250;

  private ItemStack itemStack;
  private List<String> lines = List.of();
  private long lastSeen;

  public synchronized void update(ItemStack itemStack, List<String> lines) {
    this.itemStack = itemStack;
    this.lines = List.copyOf(lines);
    this.lastSeen = System.currentTimeMillis();
  }

  public synchronized Hovered current() {
    if (this.itemStack == null || System.currentTimeMillis() - this.lastSeen > MAX_AGE_MILLIS) {
      return null;
    }
    return new Hovered(this.itemStack, this.lines);
  }

  public record Hovered(ItemStack itemStack, List<String> lines) {
  }
}
