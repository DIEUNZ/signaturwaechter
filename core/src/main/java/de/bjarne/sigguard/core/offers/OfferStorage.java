package de.bjarne.sigguard.core.offers;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Speichert die Angebote im Addon-Ordner und schreibt Exporte.
 * Ohne Minecraft-Abhängigkeiten; der Ordner wird von außen übergeben.
 */
public class OfferStorage {

  private static final String DATA_FILE = "angebote-daten.csv";
  private static final DateTimeFormatter FILE_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private final Path directory;

  public OfferStorage(Path directory) {
    this.directory = directory;
  }

  public Path directory() {
    return this.directory;
  }

  public List<OfferRecord> load() throws IOException {
    Path file = this.directory.resolve(DATA_FILE);
    if (!Files.exists(file)) {
      return List.of();
    }
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return OfferCsv.readData(reader);
    }
  }

  // Erst in eine Temp-Datei schreiben, dann ersetzen – so geht bei Absturz nichts kaputt
  public synchronized void save(List<OfferRecord> offers) throws IOException {
    Files.createDirectories(this.directory);
    Path temp = this.directory.resolve(DATA_FILE + ".tmp");
    try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
      OfferCsv.writeData(offers, writer);
    }
    Files.move(temp, this.directory.resolve(DATA_FILE),
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  /** Schreibt einen Excel-Export mit Datum im Dateinamen und gibt den Pfad zurück. */
  public Path export(List<OfferRecord> offers, LocalDateTime now, ZoneId zone)
      throws IOException {
    Files.createDirectories(this.directory);
    Path file = this.directory.resolve("angebote_" + FILE_DATE.format(now) + ".csv");
    try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      OfferCsv.writeExport(offers, writer, zone);
    }
    return file;
  }
}
