# Mizan — ميـزان

Application Android de suivi de dépenses personnelles, 100 % locale (aucun accès internet).
Kotlin · Jetpack Compose · Room · minSdk 26 (Android 8).

## Ce que contient cette ébauche (v0.1)

- **Splash** : splash système (logo sur fond émeraude), puis splash animé
  (la balance apparaît avec un rebond, puis ميـزان / Mizan et le slogan).
- **Accueil** : dépenses du mois et jauge du budget, revenus, solde,
  catégories, dernières transactions.
- **Transactions** : recherche, filtres par catégorie, liste groupée par jour.
  Appui long sur une transaction pour la supprimer.
- **Nouvelle dépense** (bouton +) : dépense ou revenu, montant en dinars
  (3 décimales), catégorie, date, note, paiement (espèces / carte).
- **Budgets** : anneau de progression global et jauge par catégorie
  (dépassement en rouge). Budgets par défaut : 2 500 dinars au total.
- **Profil** : réglages (affichage seulement pour l'instant).

### Saisie rapide — hors ligne, sans IA

Écrire une phrase comme « 20 taxi », « 3.500 café hier »,
« 20 dinars 500 épicerie carte » ou « salaire 1500 ». L'app comprend :
le montant, la catégorie (mots-clés : taxi, louage, steg, sonede, épicerie…),
la date (« hier », « avant-hier »), le paiement (« carte », « espèces »)
et les revenus (« salaire », « reçu »…). Le reste devient la note.

Accès : la barre « Ex. : 20 taxi » sur l'accueil (au clavier).

Une carte de confirmation montre ce qui a été compris : **Valider**
(avec « Annuler » pendant quelques secondes) ou **Modifier** (formulaire pré-rempli).
La logique est dans `util/QuickParser.kt`.

Les montants sont enregistrés en **millimes** (1 dinar = 1000 millimes)
et affichés au format `د 2,825.225`, avec le symbole Mizan du dinar.

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
