package de.bjarne.sigguard.core.detect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Erkennt Spawn-Eier, deren Name ein anderes Mob nennt als das Ei wirklich enthält
 * (z. B. Ghast-Ei, das "Erschaffe Skelett" heißt). Enthält keinen Minecraft-Code.
 */
public final class SpawnEggDetector {

  private static final String EGG_SUFFIX = "_spawn_egg";

  // Mob-ID (ohne "minecraft:" und "_spawn_egg") -> Namen, mit denen es im Itemnamen vorkommt.
  // Längere Namen zuerst auflösen, damit "Wither-Skelett" nicht als "Skelett" zählt.
  private static final Map<String, List<String>> MOBS = new LinkedHashMap<>();

  static {
    mob("wither_skeleton", "wither skelett", "wither skeleton");
    mob("skeleton_horse", "skelettpferd", "skeleton horse");
    mob("zombie_horse", "zombiepferd", "zombie horse");
    mob("zombie_villager", "zombiedorfbewohner", "zombie dorfbewohner", "zombie villager");
    mob("zombified_piglin", "zombifizierter piglin", "zombified piglin", "schweinezombie",
        "zombie pigman");
    mob("mooshroom", "pilzkuh", "mooshroom");
    mob("cave_spider", "höhlenspinne", "hoehlenspinne", "cave spider");
    mob("magma_cube", "magmawürfel", "magmawuerfel", "magma cube", "magmaslime");
    mob("iron_golem", "eisengolem", "iron golem");
    mob("snow_golem", "schneegolem", "snow golem");
    mob("elder_guardian", "großer wächter", "grosser waechter", "elder guardian");
    mob("ender_dragon", "enderdrache", "ender dragon");
    mob("tropical_fish", "tropenfisch", "tropical fish");
    mob("polar_bear", "eisbär", "eisbaer", "polar bear");
    mob("glow_squid", "leuchttintenfisch", "glow squid");
    mob("skeleton", "skelett", "skeleton");
    mob("zombie", "zombie");
    mob("creeper", "creeper");
    mob("spider", "spinne", "spider");
    mob("enderman", "enderman");
    mob("endermite", "endermite");
    mob("ghast", "ghast");
    mob("blaze", "lohe", "blaze");
    mob("slime", "schleim", "slime");
    mob("witch", "hexe", "witch");
    mob("pig", "schwein", "pig");
    mob("cow", "kuh", "cow");
    mob("sheep", "schaf", "sheep");
    mob("chicken", "huhn", "chicken");
    mob("horse", "pferd", "horse");
    mob("donkey", "esel", "donkey");
    mob("mule", "maultier", "mule");
    mob("llama", "lama", "llama");
    mob("wolf", "wolf");
    mob("cat", "katze", "cat");
    mob("ocelot", "ozelot", "ocelot");
    mob("parrot", "papagei", "parrot");
    mob("rabbit", "kaninchen", "hase", "rabbit");
    mob("fox", "fuchs", "fox");
    mob("panda", "panda");
    mob("bee", "biene", "bee");
    mob("turtle", "schildkröte", "schildkroete", "turtle");
    mob("dolphin", "delfin", "dolphin");
    mob("squid", "tintenfisch", "squid");
    mob("cod", "kabeljau", "cod");
    mob("salmon", "lachs", "salmon");
    mob("pufferfish", "kugelfisch", "pufferfish");
    mob("bat", "fledermaus", "bat");
    mob("villager", "dorfbewohner", "villager");
    mob("wandering_trader", "fahrender händler", "fahrender haendler", "wandering trader");
    mob("pillager", "plünderer", "pluenderer", "pillager");
    mob("vindicator", "diener", "vindicator");
    mob("evoker", "magier", "evoker");
    mob("vex", "plagegeist", "vex");
    mob("ravager", "verwüster", "verwuester", "ravager");
    mob("guardian", "wächter", "waechter", "guardian");
    mob("shulker", "shulker");
    mob("silverfish", "silberfischchen", "silverfish");
    mob("phantom", "phantom");
    mob("drowned", "ertrunkener", "drowned");
    mob("husk", "wüstenzombie", "wuestenzombie", "husk");
    mob("stray", "eiswanderer", "stray");
    mob("hoglin", "hoglin");
    mob("piglin", "piglin");
    mob("strider", "schreiter", "strider");
    mob("goat", "ziege", "goat");
    mob("axolotl", "axolotl");
    mob("frog", "frosch", "frog");
    mob("allay", "hilfsgeist", "allay");
    mob("camel", "kamel", "camel");
    mob("sniffer", "schnüffler", "schnueffler", "sniffer");
    mob("warden", "wärter", "waerter", "warden");
    mob("wither", "wither");
  }

  private SpawnEggDetector() {
  }

  private static void mob(String id, String... names) {
    MOBS.put(id, List.of(names));
  }

  /**
   * @param itemId      ID des Items, z. B. {@code minecraft:ghast_spawn_egg}
   * @param displayName Itemname als reiner Text
   * @return {@code true}, wenn es ein Spawn-Ei ist und der Name nur andere Mobs nennt
   */
  public static boolean isMismatch(String itemId, String displayName) {
    if (itemId == null || displayName == null) {
      return false;
    }
    String id = itemId.substring(itemId.indexOf(':') + 1);
    if (!id.endsWith(EGG_SUFFIX)) {
      return false;
    }
    String eggMob = id.substring(0, id.length() - EGG_SUFFIX.length());
    List<String> named = mentionedMobs(displayName);
    return !named.isEmpty() && !named.contains(eggMob);
  }

  /** Mobs, die im Namen vorkommen (längste Namen zuerst, bereits Gefundenes wird entfernt). */
  static List<String> mentionedMobs(String displayName) {
    String text = " " + displayName.toLowerCase(Locale.ROOT)
        .replace('-', ' ').replace('_', ' ') + " ";
    List<String> found = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : MOBS.entrySet()) {
      for (String name : entry.getValue()) {
        Pattern pattern = Pattern.compile("(?<=[^\\p{L}])" + Pattern.quote(name) + "(?:e?n)?(?:ei)?(?=[^\\p{L}])");
        if (pattern.matcher(text).find()) {
          found.add(entry.getKey());
          text = pattern.matcher(text).replaceAll(" ");
          break;
        }
      }
    }
    return found;
  }

  /** Name des Mobs aus der Ei-ID, z. B. "ghast" (für die Anzeige in der Warnung). */
  public static String eggMob(String itemId) {
    String id = itemId.substring(itemId.indexOf(':') + 1);
    return id.endsWith(EGG_SUFFIX) ? id.substring(0, id.length() - EGG_SUFFIX.length()) : id;
  }
}
