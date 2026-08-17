# E4 — LE COFFRE

> La collection survit au téléphone.
> **10 sprints-personne · H1 (sprints 1, 4) + H2 (sprint 11) · pilote : MAIN**
> Contient la **dette D1**, premier ticket du projet.

---

## 1. Pourquoi

L'application sauvegarde automatiquement la collection tous les jours, garde les 7
dernières versions, écrit de façon atomique, ne remplace pas une bonne sauvegarde par
un instantané vide, et évite de faire tourner la rotation avec des copies identiques.
C'est du travail soigné.

**Et ces sauvegardes vivent dans `filesDir`.**

Désinstaller l'application efface la collection **et** les sept sauvegardes, dans le
même geste. Changer de téléphone les laisse sur l'ancien. Le filet de sécurité ne
couvre pas les deux cas de perte les plus courants — et l'utilisateur, lui, a lu
« sauvegarde automatique » et se croit protégé.

Le fichier documente honnêtement cette limite. L'utilisateur ne lit pas les fichiers
source. **C'est la dette la plus grave du projet** : elle peut détruire, aujourd'hui,
la collection d'un homme de 70 ans qui a fait confiance à l'application.

C'est pourquoi elle est le **premier ticket du sprint 1**, avant la vision, avant le
catalogue, avant tout.

---

## 2. Périmètre

### Dans le périmètre
- Sauvegarde automatique vers un dossier **choisi par l'utilisateur** (SAF, URI
  persistante) — survit à la désinstallation.
- Assistant de restauration avec prévisualisation avant écrasement.
- Transfert d'appareil à appareil par appairage local, sans cloud.
- Export chiffré optionnel (phrase secrète).
- Vérification d'intégrité des sauvegardes.

### Hors périmètre
- Sauvegarde vers un cloud que nous exploiterions (viole P3 et P5).
- Compte, identifiant, chiffrement à clé gérée par nous.
- Synchronisation continue entre appareils (≠ transfert ponctuel, [ADR-008](../DECISIONS.md#adr-008)).

---

## 3. Récits utilisateur

### US-4.1 — Survivre à une désinstallation *(dette D1)*
```
Étant donné  une sauvegarde automatique configurée vers un dossier choisi
Quand        je désinstalle puis réinstalle l'application
Alors        l'application propose de restaurer la sauvegarde trouvée
Et           ma collection est retrouvée à l'identique
```
```
Étant donné  qu'aucun dossier de sauvegarde n'a encore été choisi
Quand        ma collection dépasse 20 albums
Alors        l'application me propose une fois — sans insister, sans bloquer —
             de choisir où sauvegarder
Et           si je refuse, elle continue de sauvegarder en interne comme aujourd'hui
Et           l'écran Réglages indique honnêtement la portée de la protection en cours
```

### US-4.2 — Restaurer sans se tromper
```
Étant donné  un fichier de sauvegarde sélectionné
Quand        je demande à restaurer
Alors        je vois AVANT toute écriture : la date, le nombre de séries, le nombre
             d'albums, et un aperçu des 5 premières séries
Et           je vois ce que je vais perdre : « ta collection actuelle (47 séries,
             612 albums) sera remplacée »
Et           une sauvegarde de l'état actuel est prise automatiquement avant l'écrasement
Et           la confirmation exige un geste explicite, jamais un simple « OK »
```

### US-4.3 — Changer de téléphone
```
Étant donné  deux téléphones sur le même réseau Wi-Fi
Quand        je choisis « Transférer vers un nouveau téléphone » sur l'ancien
Alors        un code à 6 chiffres et un QR s'affichent
Et           le nouveau téléphone rejoint après saisie du code
Et           la collection est transférée, chiffrée de bout en bout, sans passer par
             aucun serveur
Et           l'ancien téléphone conserve sa collection intacte — un transfert n'est pas
             un déménagement
```

### US-4.4 — Une sauvegarde qu'on peut confier
```
Étant donné  que j'exporte ma collection pour l'envoyer à mon fils
Quand        j'active « Protéger par un mot de passe »
Alors        le fichier est chiffré (AES-256-GCM, clé dérivée par Argon2id)
Et           l'import demande le mot de passe
Et           un mot de passe oublié rend le fichier définitivement illisible, ce qui
             est annoncé clairement AVANT le chiffrement
```

### US-4.5 — Détecter une sauvegarde abîmée
```
Étant donné  un fichier de sauvegarde tronqué ou modifié
Quand        je tente de le restaurer
Alors        l'anomalie est détectée avant toute écriture
Et           un message en langage clair l'explique
Et           ma collection actuelle est intacte
```

---

## 4. Conception technique

### 4.1 Emplacement des sauvegardes

| Emplacement | Aujourd'hui | Cible |
|---|---|---|
| `filesDir/backups/` | 7 sauvegardes quotidiennes | **Conservé** — filet local rapide |
| Dossier utilisateur (SAF) | — | **Nouveau** — 14 sauvegardes, survit à la désinstallation |
| Sauvegarde système Android | `allowBackup=true` déclaré | Documenté, non fiable seul (dépend du compte Google, hors de notre maîtrise) |

**Implémentation SAF**

```kotlin
// UserPreferencesRepository
val backupFolderUri: Flow<String?>          // URI persistante, ACTION_OPEN_DOCUMENT_TREE
suspend fun setBackupFolderUri(uri: String?)
```

- Permission persistante prise via `takePersistableUriPermission`.
- `BackupWorker` écrit **aux deux emplacements** ; l'échec de l'un n'empêche pas l'autre.
- URI devenue invalide (dossier supprimé, carte SD retirée) : détectée, signalée une
  fois dans les réglages, jamais en boucle, et la sauvegarde interne continue.
- `BackupManager` conserve sa logique existante (pas de sauvegarde vide, pas de doublon
  consécutif, écriture atomique) — elle est bonne, on l'étend au second emplacement.

### 4.2 Format de sauvegarde v2

Rétrocompatible : le format actuel (`CollectionSnapshot` sérialisé) reste lisible.

```json
{
  "formatVersion": 2,
  "app": { "versionName": "2.0.0", "dbVersion": 3 },
  "createdAt": "2026-11-14T09:12:00Z",
  "counts": { "series": 47, "albums": 612 },
  "checksum": "sha256:…",
  "collection": { "series": [ … ], "albums": [ … ] }
}
```

- L'en-tête permet la **prévisualisation sans tout charger** — c'est ce qui rend US-4.2
  possible.
- `checksum` couvre l'objet `collection` uniquement, pour rester stable à la relecture.
- La validation existante (`SnapshotValidation`, déjà testée) est conservée et exécutée
  **avant** toute écriture.
- Une sauvegarde v1 sans en-tête est acceptée : lecture directe, prévisualisation
  calculée après analyse complète.

### 4.3 Transfert d'appareil à appareil

**Principe** : socket local sur le réseau Wi-Fi, chiffrement dérivé du code à 6 chiffres,
aucun intermédiaire.

```
Ancien téléphone                          Nouveau téléphone
  génère code 6 chiffres  ──── QR ────►     saisit ou scanne le code
  ouvre un socket local                     découvre par NSD (mDNS)
  dérive une clé du code (PBKDF2)           dérive la même clé
  chiffre et envoie (AES-256-GCM)  ────►    déchiffre, valide, prévisualise
                                            confirme, puis restaure
```

- Sans réseau commun : repli sur l'export de fichier, proposé automatiquement.
- Le code expire après 5 minutes.
- Trois échecs de code → session annulée.
- **L'ancien appareil n'est jamais modifié.**

### 4.4 Comportements en erreur

| Situation | Comportement |
|---|---|
| Dossier de sauvegarde devenu inaccessible | Signalé une fois, sauvegarde interne poursuivie, proposition de re-choisir |
| Disque plein | Sauvegarde abandonnée, anciennes conservées, message non bloquant |
| Sauvegarde corrompue | Détectée par empreinte, restauration refusée, collection intacte |
| Mauvais mot de passe | Message clair, nouvelle tentative, pas de blocage |
| Interruption pendant la restauration | Transaction Room annulée — mécanisme déjà en place dans `importSnapshot` |
| Transfert interrompu | Aucune écriture partielle ; le nouvel appareil reste dans son état antérieur |
| Sauvegarde d'une version d'application plus récente | Détectée par `formatVersion`, message explicite, refus propre |

---

## 5. Interface & accessibilité

### 5.1 Réglages → « Ma collection est-elle en sécurité ? »

Le titre de section est écrit tel quel. C'est la question que l'utilisateur se pose ;
le libellé « Sauvegarde et restauration » y répond moins bien.

```
Ma collection est-elle en sécurité ?

  ✓ Sauvegardée chaque jour
    Dernière : aujourd'hui à 4 h 12
    Dossier : Documents/BDteque
    Cette sauvegarde survit à la désinstallation.

  [ Changer de dossier ]
  [ Sauvegarder maintenant ]
  [ Restaurer une sauvegarde ]
  [ Transférer vers un nouveau téléphone ]
```

État dégradé, quand aucun dossier n'est choisi — **honnête, jamais alarmiste** :

```
  ⚠ Sauvegardée chaque jour, mais seulement dans l'application.
    Si tu désinstalles l'application, ces sauvegardes seront perdues.
  [ Choisir un dossier sûr ]
```

**Exigences d'accessibilité**
- L'état de sécurité est porté par le **texte**, pas par l'icône ou la couleur.
- Toute action destructive : confirmation explicite avec conséquence énoncée en clair.
- Contraste ≥ 4,5:1 pour l'état d'avertissement, en thème clair et sombre.
- Le mot « restaurer » n'apparaît **jamais** à côté du mot « importer »
  ([E3](E3-amorcage.md), Q2).

---

## 6. Plan de test

### Tests unitaires
- `BackupFormatTest` — v1 lue, v2 écrite, en-tête, empreinte, incompatibilité de version.
- `BackupRotationTest` — rotation à 7 et 14, pas de sauvegarde vide, pas de doublon
  consécutif (non-régression sur le comportement existant).
- `EncryptedBackupTest` — chiffrement, déchiffrement, mauvais mot de passe, fichier tronqué.

### Tests instrumentés
- `BackupSurvivesUninstallTest` — sauvegarde SAF, désinstallation simulée, réinstallation,
  restauration. **Le test qui valide la correction de D1.**
- `RestorePreviewTest` — prévisualisation exacte avant écrasement.
- `RestoreRollbackTest` — interruption pendant la restauration, collection intacte.
- `DeviceTransferTest` — deux instances, transfert complet, appareil source inchangé.

### Test manuel obligatoire, à chaque release
> Export sur appareil A → installation neuve sur appareil B → import → **comparaison
> album par album**. Rituel non négociable ([D-R6-5](../COMITES.md#r6--qualité-livraison--incidents)).

---

## 7. Risques

| # | Risque | Titulaire | Parade |
|---|---|---|---|
| Q1 | SAF est capricieux selon les constructeurs (Samsung, Xiaomi restreignent l'accès aux dossiers). | MAIN | Test sur les deux appareils de référence + 3 modèles du panel. Repli sur `Documents/` public si l'arborescence est refusée. |
| Q2 | Le transfert local échoue sur des réseaux Wi-Fi isolant les clients (box opérateur, réseaux d'hôtel). | MAIN | Détection préalable, repli automatique et explicite sur l'export de fichier. |
| Q3 | Un utilisateur oublie son mot de passe de sauvegarde chiffrée. | LISIBILITÉ | Avertissement **avant** chiffrement, formulation sans ambiguïté, chiffrement non activé par défaut. |

---

## 8. Ce qui prouve que c'est terminé

- [ ] Désinstaller puis réinstaller l'application permet de retrouver sa collection.
- [ ] Une restauration montre exactement ce qui sera perdu, avant d'écrire.
- [ ] Une sauvegarde est prise automatiquement avant tout écrasement.
- [ ] Un fichier abîmé est détecté avant écriture.
- [ ] Un transfert entre deux téléphones aboutit sans réseau extérieur.
- [ ] Le rituel export → installation neuve → import restitue la collection à l'identique.
</content>
