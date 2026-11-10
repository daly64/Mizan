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

### Intégration avec l'app Claude (v0.6)

Mizan reste 100 % locale (aucun accès internet, aucune clé) : les échanges passent
par le **partage Android** avec l'application Claude installée sur le téléphone.

- **Bilan du mois** (Budgets, ou Profil → Claude) : envoie à Claude une consigne,
  les totaux, les budgets et les transactions du mois. Claude répond avec un bilan et des conseils.
- **Préparer une demande** : envoie à Claude la consigne qui lui explique le format Mizan.
  On ajoute la photo d'un ticket, une facture ou une liste, et Claude répond en lignes CSV.
- **Importer depuis Claude** : partager la réponse de Claude vers Mizan (ou la copier puis
  « Coller »). Aperçu des transactions reconnues, puis import. Un fichier `.csv` partagé
  vers Mizan est aussi accepté.
- **Exporter en CSV** (Profil → Données) : toutes les transactions, à envoyer où l'on veut.

Format d'échange (`util/MizanCsv.kt`) :

```
date;type;montant;categorie;paiement;note
2026-11-10;depense;12.500;Transport;Especes;Taxi
```

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
- Modifier les budgets, sauvegarde
- Historique des mois précédents
