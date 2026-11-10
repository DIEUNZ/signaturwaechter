package de.bjarne.sigguard.core;

import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.loader.MinecraftVersions;

/**
 * Unterschiede zwischen den unterstützten Minecraft-Versionen (1.8.9, 1.12.2, 26.3).
 */
public final class VersionCompat {

  // Ältere Schriften können ⚠ und ⏰ nicht sicher darstellen
  private static final boolean MODERN_FONT = MinecraftVersions.V1_13.orNewer();

  public static final String WARNING = MODERN_FONT ? "⚠" : "!";
  public static final String CLOCK = MODERN_FONT ? "⏰" : "»";
  public static final String CHECK = MODERN_FONT ? "✔" : "+";
  public static final String USER_MARK = MODERN_FONT ? "✔" : "*";

  /** Notenblock-Ton; der Name hat sich mit 1.9 und 1.13 geändert. */
  public static final ResourceLocation REMINDER_SOUND = ResourceLocation.create("minecraft",
      MinecraftVersions.V1_13.orNewer() ? "block.note_block.pling"
          : MinecraftVersions.V1_9.orNewer() ? "block.note.pling"
              : "note.pling");

  /** Ton für die Erinnerung ({@code null} = aus); die Namen unterscheiden sich je Version. */
  public static ResourceLocation sound(SigGuardConfiguration.ReminderSound sound) {
    boolean modern = MinecraftVersions.V1_9.orNewer();
    return switch (sound) {
      case PLING -> REMINDER_SOUND;
      case XP -> ResourceLocation.create("minecraft",
          modern ? "entity.experience_orb.pickup" : "random.orb");
      case LEVELUP -> ResourceLocation.create("minecraft",
          modern ? "entity.player.levelup" : "random.levelup");
      case VILLAGER -> ResourceLocation.create("minecraft",
          modern ? "entity.villager.yes" : "mob.villager.yes");
      case OFF -> null;
    };
  }

  private VersionCompat() {
  }
}
