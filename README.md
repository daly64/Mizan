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

### Avec Claude (v0.7) — tout depuis l'accueil

Mizan reste 100 % locale (aucun accès internet, aucune clé) : les échanges passent
par le **partage Android** avec l'application Claude du téléphone.
Une seule carte « Avec Claude » sur l'accueil :

- **Bilan** : envoie le mois (consigne, totaux, budgets, transactions) à Claude.
- **Ticket** : photo ou galerie → la photo et la consigne partent ensemble vers Claude,
  qui répond en lignes au format Mizan.
- **Retour automatique** : partager la réponse de Claude vers Mizan, **ou simplement la copier**
  et revenir dans Mizan : la réponse est détectée et Mizan propose « Ajouter » en un geste.
- **Exporter en CSV** reste dans Profil → Données.

### Claude directement dans Mizan (v0.8) — facultatif

- **Profil → Claude → Clé API Claude** : coller une clé créée sur console.anthropic.com
  (crédit payé à part de l'abonnement Claude). Modèle par défaut : `claude-haiku-5-5`.
  La clé est **chiffrée sur le téléphone**, exclue des sauvegardes, envoyée seulement à
  api.anthropic.com. **Jamais dans le code ni sur GitHub.**
- Avec la clé, **Bilan** affiche la réponse de Claude dans Mizan, et **Ticket** envoie la photo
  (réduite) à Claude, qui la lit : les dépenses reconnues s'ajoutent en un geste.
- Sans clé, tout passe par l'app Claude comme en v0.7.
- La connexion d'un compte ou abonnement Claude dans une app tierce n'est pas autorisée par
  Anthropic : c'est pourquoi l'intégration directe passe par une clé API.

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
