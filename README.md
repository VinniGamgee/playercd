# MoonPlayer

Player de música Android nativo — Kotlin + Jetpack Compose + Media3.

## Requisitos
- Android Studio Hedgehog ou superior
- JDK 17
- Android SDK 34

## Build
```bash
./gradlew assembleDebug
```

## Funcionalidades
- Biblioteca local (músicas, artistas, álbuns, pastas, playlists, favoritos)
- Reprodução com Media3/ExoPlayer + MediaSession
- Mini-player e tela full player
- Pesquisa, fila, favoritos, playlists
- Tema claro/escuro (sistema)
- Material 3

## Estrutura
```
app/src/main/java/com/moonplayer/app/
├── data/          # models, dao, db, repository
├── player/        # PlaybackService, PlayerManager
├── ui/            # screens, components, theme, navigation
├── viewmodel/
└── MainActivity.kt
```
