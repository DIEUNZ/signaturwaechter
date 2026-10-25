package de.bjarne.sigguard.core.detect;

/**
 * Ergebnis einer Prüfung inkl. des ähnlichsten Whitelist-Namens.
 *
 * @param verdict   Urteil
 * @param signer    geprüfter Signatur-Name
 * @param matchedName ähnlichster Whitelist-Name oder {@code null}
 * @param distance  Editierdistanz der normalisierten Namen, -1 wenn nicht berechnet
 */
public record DetectionResult(
    SignatureVerdict verdict,
    String signer,
    String matchedName,
    int distance
) {

  public static DetectionResult unknown(String signer) {
    return new DetectionResult(SignatureVerdict.UNBEKANNT, signer, null, -1);
  }
}
