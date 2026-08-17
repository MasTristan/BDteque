package com.bdshelf.app.domain

/**
 * Validation et normalisation des codes-barres scannés ou saisis (§6.3/§6.4).
 *
 * ML Kit décode le code-barres image par image et peut, rarement, lire un
 * chiffre de travers ; la saisie manuelle peut contenir tirets et espaces ou
 * un ISBN-10 (dos des albums anciens). Tout converge ici vers une forme
 * canonique unique : EAN-13 ou EAN-8 en chiffres nus, somme de contrôle
 * vérifiée, ISBN-10 converti en EAN-13. C'est cette forme qui circule dans
 * l'app (verdict, EAN appris sur un album, clé du cache d'identification).
 */

/** Garde uniquement les chiffres et un éventuel X final d'ISBN-10 ("978-2-8036…", "2-205-05458-X"). */
fun normalizeEanInput(raw: String): String =
    raw.filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase()

/**
 * Forme canonique d'un code scanné ou saisi : EAN-13 valide, EAN-8 valide,
 * ou ISBN-10 valide converti en EAN-13. `null` si le code est illisible ou
 * que sa somme de contrôle ne tombe pas juste (lecture erronée, faute de
 * frappe) : mieux vaut redemander que de chercher un faux code.
 */
fun canonicalEan(raw: String): String? {
    val code = normalizeEanInput(raw)
    return when {
        isValidEan13(code) -> code
        isValidEan8(code) -> code
        isValidIsbn10(code) -> isbn10To13(code)
        else -> null
    }
}

/** Somme de contrôle EAN-13 (dernier chiffre) : poids 1/3 alternés sur les 12 premiers. */
fun isValidEan13(code: String): Boolean =
    code.length == 13 && code.all(Char::isDigit) && ean13CheckDigit(code.take(12)) == code.last()

/** Somme de contrôle EAN-8 : poids 3/1 alternés sur les 7 premiers chiffres. */
fun isValidEan8(code: String): Boolean {
    if (code.length != 8 || !code.all(Char::isDigit)) return false
    val sum = code.take(7).mapIndexed { i, c -> c.digitToInt() * if (i % 2 == 0) 3 else 1 }.sum()
    return (10 - sum % 10) % 10 == code.last().digitToInt()
}

/** Somme de contrôle ISBN-10 : poids 10..2, clé mod 11 (X = 10) en dernière position. */
fun isValidIsbn10(code: String): Boolean {
    if (code.length != 10 || !code.take(9).all(Char::isDigit)) return false
    val last = code.last()
    if (!last.isDigit() && last != 'X') return false
    val sum = code.take(9).mapIndexed { i, c -> c.digitToInt() * (10 - i) }.sum()
    val check = (11 - sum % 11) % 11
    return check == if (last == 'X') 10 else last.digitToInt()
}

/** Convertit un ISBN-10 valide en EAN-13 (préfixe 978, somme de contrôle recalculée). */
fun isbn10To13(isbn10: String): String {
    val body = "978" + isbn10.take(9)
    return body + ean13CheckDigit(body)
}

/**
 * Forme ISBN-10 d'un EAN-13 préfixé 978, pour interroger les sources qui
 * n'indexent que l'ancienne forme (notices antérieures à 2007). `null` pour
 * les préfixes 979 (sans équivalent ISBN-10) et les autres EAN.
 */
fun isbn13To10(ean13: String): String? {
    if (!isValidEan13(ean13) || !ean13.startsWith("978")) return null
    val body = ean13.substring(3, 12)
    val sum = body.mapIndexed { i, c -> c.digitToInt() * (10 - i) }.sum()
    val check = (11 - sum % 11) % 11
    return body + if (check == 10) "X" else check.digitToChar()
}

private fun ean13CheckDigit(first12: String): Char {
    val sum = first12.mapIndexed { i, c -> c.digitToInt() * if (i % 2 == 0) 1 else 3 }.sum()
    return ((10 - sum % 10) % 10).digitToChar()
}
