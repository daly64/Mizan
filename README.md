# Mizan — ميـزان

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

### Assistant IA local (v0.11, branche `agent-ia`)

- Icône ✨ en haut de l'accueil. Le modèle tourne **sur le téléphone** avec LiteRT-LM :
  Mizan n'a toujours pas la permission internet.
- Deux modèles au choix (Apache 2.0, sans compte) : **léger** Qwen3 0.6B (`Qwen3-0.6B.litertlm`, ~0,6 Go)
  ou **complet** Gemma 4 E2B (`gemma-4-E2B-it.litertlm`, ~2,6 Go). On le télécharge avec le navigateur,
  puis on l'importe dans l'assistant (copié dans le dossier privé de l'app).
- Outils (`agent/MizanTools.kt`) : ajouter une dépense, un revenu, une note, chercher dans les notes,
  calculer (calculatrice exacte en Kotlin), bilan du mois (totaux, budgets, comparaison,
  projection). Le modèle ne calcule jamais lui-même : il appelle les outils et rédige.
- Notes stockées dans Room (base v2, migration automatique) et incluses dans la sauvegarde.
- Menu ⋮ : nouvelle discussion, accélération GPU (essai, retour au processeur si elle échoue),
  changer / supprimer le modèle.
- Le son du splash a été retiré (v0.11.1).

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
