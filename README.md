# AL MIZAN — الميزان

Dépôt : `daly64/al-mizan`

Application Android de suivi de dépenses personnelles, 100 % locale (aucun accès internet).
Kotlin · Jetpack Compose · Room · minSdk 26 (Android 8).

## Ce que contient cette ébauche (v0.1)

- **Splash** : splash système (logo sur fond émeraude), puis splash animé :
  la balance apparaît avec un rebond, puis ميزان s'écrit en calligraphie Ruqaa
  (police Aref Ruqaa embarquée, licence OFL dans `licenses/`), puis Mizan et le slogan.
- **Accueil** : dépenses du mois et jauge du budget, revenus, solde,
  catégories, dernières transactions.
- **Transactions** : recherche, filtres par catégorie, liste groupée par jour.
  Appui long sur une transaction pour la supprimer.
- **Nouvelle dépense** (bouton +) : dépense ou revenu, montant en dinars
  (3 décimales), catégorie, date, note, paiement (espèces / carte).
- **Budgets** : anneau de progression global et jauge par catégorie
  (dépassement en rouge). Budgets par défaut : 2 500 dinars au total.
- **Profil** : réglages (affichage seulement pour l'instant).

### Thème clair / sombre et sauvegarde automatique (v0.10)

- **Thème** (Profil → Thème) : Comme le téléphone, Clair ou Sombre. Le changement est immédiat.
  Le splash garde ses couleurs de marque.
- **Sauvegarde automatique** à la fin de chaque session (quand on quitte l'app) :
  `mizan-derniere.json` + une copie par jour (7 jours), dans le dossier de l'app,
  et dans un dossier du téléphone si on en choisit un (Profil → Dossier de sauvegarde).
  Jamais écrasée par une base vide. **Restaurer une sauvegarde** remplace les données
  par celles d'un fichier choisi. Tout reste local (`data/Backup.kt`).

### Interface simplifiée (v0.12)

- Barre du bas réduite à **Accueil · + · Transactions**.
- Accueil : dépenses du mois et budget, revenus et solde, **budgets par catégorie** (l'ancien onglet
  Budgets), dernières transactions. ⚙ en haut à droite ouvre les Réglages.
- **Réglages** : thème, sauvegarde, dossier de sauvegarde, restauration. Les lignes qui ne faisaient
  rien (devise, rappel, export CSV « bientôt », etc.) sont retirées.
- Nouvelle dépense : montant, catégorie, date, note (le choix espèces / carte est retiré).
- Splash sans son. L'assistant IA testé en 0.11 (branche `agent-ia`) n'est pas repris.

## Ouvrir et lancer

1. Ouvrir le dossier `Mizan` dans **Android Studio** (Ladybug ou plus récent).
2. Laisser Gradle synchroniser (première fois : quelques minutes).
3. Lancer sur un téléphone ou un émulateur avec ▶.

## Obtenir l'APK sans Android Studio

Pousser le projet sur GitHub : le workflow `.github/workflows/build-apk.yml`
compile l'APK de test. Il se télécharge dans l'onglet **Actions** du dépôt,
dans l'artefact `mizan-debug-apk`.

## Structure

```
app/src/main/java/tn/mizan/app/
├── MainActivity.kt          splash système + Compose
├── data/                    Room : dépenses, budgets
├── util/Format.kt           format des montants et des dates
└── ui/
    ├── MizanApp.kt          navigation + barre du bas
    ├── MizanViewModel.kt    données du mois en cours
    ├── components/          symbole dinar, logo, tuiles, lignes…
    ├── screens/             splash, accueil, transactions, ajout, budgets, profil
    └── theme/               couleurs Mizan
```

## Prochaines étapes

- Polices Poppins / Inter / Cairo
- Modifier les budgets, export CSV, sauvegarde
- Historique des mois précédents

### AL MIZAN, plein écran et mises à jour (v0.13)

- **Nouveau nom** : AL MIZAN / الميزان, couleurs de la planche UI/UX (vert #0F5C4D, or #C9A45C, crème #F3EFE3).
- **Bienvenue** au premier lancement : 3 pages illustrées qui glissent, avec entrées animées
  (montée + fondu, courbe douce), illustration qui flotte, points de pagination animés, « Passer ».
- **Plein écran automatique** : barres d'état et de navigation cachées ; un glissement depuis le bord
  les affiche un instant.
- **Mises à jour** : au démarrage (au plus toutes les 12 h) et dans Réglages → « Rechercher une mise à jour »,
  l'app lit les versions publiées sur GitHub Releases et propose « Télécharger la mise à jour » :
  le navigateur télécharge l'APK, on le touche pour l'installer (pas de permission d'installation dans l'app).
  C'est le seul usage d'internet de l'app (permission INTERNET), aucune donnée n'est envoyée.
  Le dépôt des versions doit être lisible publiquement pour que la vérification fonctionne (sinon l'app l'indique).
- **Signature fixe** : la clé n'est pas dans le dépôt, la compilation la reçoit par les secrets GitHub
  `MIZAN_KEYSTORE_B64` et `MIZAN_KEYSTORE_PASSWORD`. Ainsi les APK successifs s'installent par-dessus l'app.
  Le passage à cette signature demande une seule désinstallation de l'ancienne version
  (faire une sauvegarde avant, puis la restaurer).
- Numéro de version automatique : `0.13.N` (N = numéro de compilation GitHub Actions).

### Scan de ticket (v0.13)

- Nouvelle dépense → **Scanner un ticket** (Photo ou Galerie) : le texte est lu sur le téléphone avec ML Kit
  (modèle embarqué, hors ligne), puis l'app remplit **le total, la date, le magasin (note) et la catégorie**.
  Tout reste modifiable, l'utilisateur confirme. La photo est effacée après lecture.
- Total : lignes « NET A PAYER », « TOTAL TTC », « TOTAL »… (montants à 3 décimales : 64,300 / 1 250,500).
- Catégorie : règles par magasin (STEG → Factures, Agil → Transport, Carrefour → Courses, Café → Loisirs…).
  Quand on corrige la catégorie d'un magasin scanné, l'app s'en souvient pour les tickets suivants.
- Limite : ML Kit ne lit pas l'arabe ; sur un ticket en arabe, seuls les chiffres sont reconnus.

### Play Protect (v0.13)

- APK publié en **release** (non débogable), signé avec une clé fixe (secrets GitHub), `targetSdk` 35.
- Aucune permission sensible : seulement INTERNET (liste des versions). Pas de `REQUEST_INSTALL_PACKAGES`,
  pas d'accès aux SMS, contacts, fichiers ou appareil photo (la photo de ticket passe par l'appareil photo du téléphone).
