package de.bjarne.sigguard.core.collect;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.bjarne.sigguard.core.detect.ItemFingerprint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.labymod.api.Laby;

/**
 * Sammelmodus (/aht sammeln): schreibt die Fingerabdrücke gesehener Auktions-Items in eine Datei
 * im Spielordner, damit echte Admin-Items und Fälschungen verglichen werden können.
 * Die Datei bleibt lokal, es wird nichts gesendet. Geschrieben wird in einem eigenen Thread.
 */
public class CollectionStore {

  private static final String FILE_NAME = "ah-fake-warner-sammlung.jsonl";
  private static final int MAX_ENTRIES = 5_000;

  private final Set<String> known = ConcurrentHashMap.newKeySet();
  private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "ah-fake-warner-collect");
    thread.setDaemon(true);
    return thread;
  });

  private volatile boolean enabled;

  public boolean enabled() {
    return this.enabled;
  }

  /** Schaltet den Sammelmodus um und gibt den neuen Zustand zurück. */
  public boolean toggle() {
    this.enabled = !this.enabled;
    return this.enabled;
  }

  public Path file() {
    return Laby.labyAPI().labyModLoader().getGameDirectory().resolve(FILE_NAME);
  }

  /** Merkt sich das Item, falls es noch nicht in der Sammlung ist. */
  public void record(ItemFingerprint fingerprint) {
    if (!this.enabled || this.known.size() >= MAX_ENTRIES
        || !this.known.add(fingerprint.key())) {
      return;
    }

    JsonObject json = new JsonObject();
    json.addProperty("name", fingerprint.name());
    json.addProperty("material", fingerprint.material());
    JsonArray description = new JsonArray();
    fingerprint.description().forEach(description::add);
    json.add("description", description);
    json.addProperty("signer", fingerprint.signer());
    json.addProperty("verdict", fingerprint.verdict());
    String line = json + System.lineSeparator();

    Path file = this.file();
    this.writer.execute(() -> {
      try {
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      } catch (IOException ignored) {
        // Sammlung ist optional: bei Schreibfehlern still weitermachen
      }
    });
  }
}
