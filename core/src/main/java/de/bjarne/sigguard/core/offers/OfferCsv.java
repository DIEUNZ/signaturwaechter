package de.bjarne.sigguard.core.offers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest und schreibt Angebote als CSV (Semikolon, UTF-8).
 * Export: für deutsches Excel (BOM, Komma als Dezimaltrenner).
 * Daten: internes Speicherformat des Addons mit Kennung und Roh-Zeitstempel.
 */
public final class OfferCsv {

  public static final char SEPARATOR = ';';
  private static final char BOM = '﻿';
  private static final DateTimeFormatter EXPORT_TIME =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private static final String[] EXPORT_HEADER = {
      "Zeitstempel", "Item-Typ", "Anzeigename", "Signatur", "Menge", "Gesamtpreis",
      "Preis pro Stück", "Verkäufer", "Fake-Verdacht"
  };
  private static final String[] DATA_HEADER = {
      "key", "timestamp", "itemType", "displayName", "signature", "amount", "totalPrice",
      "seller", "fakeSuspect"
  };

  private OfferCsv() {
  }

  /** Schreibt die Export-Datei für Excel. */
  public static void writeExport(List<OfferRecord> offers, Writer out, ZoneId zone)
      throws IOException {
    BufferedWriter writer = new BufferedWriter(out);
    // BOM, damit Excel die Datei als UTF-8 erkennt (Umlaute)
    writer.write(BOM);
    writeRow(writer, true, EXPORT_HEADER);
    for (OfferRecord offer : offers) {
      writeRow(writer, true,
          EXPORT_TIME.format(Instant.ofEpochMilli(offer.timestamp()).atZone(zone)),
          offer.itemType(),
          offer.displayName(),
          offer.signature(),
          Integer.toString(offer.amount()),
          germanNumber(offer.totalPrice()),
          germanNumber(offer.pricePerUnit()),
          offer.seller(),
          offer.fakeSuspect() ? "ja" : "nein");
    }
    writer.flush();
  }

  /** Schreibt das interne Speicherformat. */
  public static void writeData(List<OfferRecord> offers, Writer out) throws IOException {
    BufferedWriter writer = new BufferedWriter(out);
    writeRow(writer, false, DATA_HEADER);
    for (OfferRecord offer : offers) {
      writeRow(writer, false,
          offer.key(),
          Long.toString(offer.timestamp()),
          offer.itemType(),
          offer.displayName(),
          offer.signature(),
          Integer.toString(offer.amount()),
          offer.totalPrice() == null ? "" : offer.totalPrice().toPlainString(),
          offer.seller(),
          Boolean.toString(offer.fakeSuspect()));
    }
    writer.flush();
  }

  /** Liest das interne Speicherformat; kaputte Zeilen werden übersprungen. */
  public static List<OfferRecord> readData(Reader in) throws IOException {
    List<OfferRecord> offers = new ArrayList<>();
    List<List<String>> rows = parse(new BufferedReader(in));
    for (int i = 1; i < rows.size(); i++) {
      List<String> row = rows.get(i);
      if (row.size() < DATA_HEADER.length) {
        continue;
      }
      try {
        offers.add(new OfferRecord(
            row.get(0),
            Long.parseLong(row.get(1)),
            row.get(2),
            row.get(3),
            row.get(4),
            Integer.parseInt(row.get(5)),
            row.get(6).isEmpty() ? null : new BigDecimal(row.get(6)),
            row.get(7),
            Boolean.parseBoolean(row.get(8))));
      } catch (NumberFormatException ignored) {
      }
    }
    return offers;
  }

  // Zahl ohne Tausenderpunkte, mit Dezimalkomma (deutsches Excel)
  static String germanNumber(BigDecimal value) {
    return value == null ? "" : value.toPlainString().replace('.', ',');
  }

  static String escape(String value, boolean forExcel) {
    String text = value == null ? "" : value;
    // Excel würde "+1 Clan-Home" oder "=HYPERLINK(...)" als Formel lesen;
    // ein führendes Leerzeichen macht daraus normalen Text
    if (forExcel && !text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
      text = " " + text;
    }
    if (text.indexOf(SEPARATOR) >= 0 || text.indexOf('"') >= 0
        || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
      return '"' + text.replace("\"", "\"\"") + '"';
    }
    return text;
  }

  private static void writeRow(BufferedWriter writer, boolean forExcel, String... values)
      throws IOException {
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        writer.write(SEPARATOR);
      }
      writer.write(escape(values[i], forExcel));
    }
    writer.write("\r\n");
  }

  // Einfacher CSV-Parser mit Anführungszeichen und Zeilenumbrüchen in Feldern
  static List<List<String>> parse(BufferedReader reader) throws IOException {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    boolean rowHasContent = false;
    int c;
    while ((c = reader.read()) != -1) {
      char ch = (char) c;
      if (ch == BOM && field.isEmpty() && row.isEmpty() && rows.isEmpty()) {
        continue;
      }
      if (quoted) {
        if (ch == '"') {
          reader.mark(1);
          int next = reader.read();
          if (next == '"') {
            field.append('"');
          } else {
            quoted = false;
            if (next != -1) {
              reader.reset();
            }
          }
        } else {
          field.append(ch);
        }
        continue;
      }

      if (ch == '"') {
        quoted = true;
        rowHasContent = true;
      } else if (ch == SEPARATOR) {
        row.add(field.toString());
        field.setLength(0);
        rowHasContent = true;
      } else if (ch == '\n' || ch == '\r') {
        if (ch == '\r') {
          reader.mark(1);
          if (reader.read() != '\n') {
            reader.reset();
          }
        }
        if (rowHasContent || !field.isEmpty()) {
          row.add(field.toString());
          rows.add(row);
        }
        row = new ArrayList<>();
        field.setLength(0);
        rowHasContent = false;
      } else {
        field.append(ch);
        rowHasContent = true;
      }
    }
    if (rowHasContent || !field.isEmpty()) {
      row.add(field.toString());
      rows.add(row);
    }
    return rows;
  }
}
