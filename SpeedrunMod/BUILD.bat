@echo off
title SpeedrunBot — Compilation du mod
color 0b
echo.
echo  =============================================
echo   SPEEDRUNBOT — Compilation Mod Fabric
echo   Minecraft 1.21.1
echo  =============================================
echo.

:: Verifie Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo  [!] Java introuvable !
    echo  Telecharge Java 21 depuis :
    echo  https://adoptium.net/
    start https://adoptium.net/
    pause & exit /b 1
)
echo  [OK] Java detecte.

:: Verifie si le jar existe deja
if exist "build\libs\speedrunbot-1.0.0.jar" (
    echo  [OK] Mod deja compile !
    goto :copy
)

:: Telecharge Gradle si necessaire
if not exist "gradle_dist\bin\gradle.bat" (
    echo  [*] Telechargement de Gradle 8.8...
    powershell -Command "Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.8-bin.zip' -OutFile 'gradle.zip'"
    if not exist "gradle.zip" (
        echo  [ERREUR] Telechargement echoue. Verifie ta connexion internet.
        pause & exit /b 1
    )
    echo  [*] Extraction Gradle...
    tar -xf gradle.zip
    rename gradle-8.8 gradle_dist
    del gradle.zip
    echo  [OK] Gradle pret.
)

:: Telecharge gradle-wrapper.jar si absent
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo  [*] Telechargement gradle-wrapper.jar...
    powershell -Command "Invoke-WebRequest 'https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
)

echo.
echo  [*] Compilation du mod (premiere fois = 5-10 min, telechargement des deps)...
echo  [*] Ne ferme pas cette fenetre !
echo.

gradle_dist\bin\gradle.bat build --no-daemon

if %errorlevel% neq 0 (
    echo.
    echo  [ERREUR] La compilation a echoue !
    echo  Solutions:
    echo    1. Verifie ta connexion internet
    echo    2. Lance en tant qu'Administrateur
    echo    3. Desactive l'antivirus temporairement
    pause & exit /b 1
)

:copy
echo.
echo  [*] Copie du mod dans le dossier mods...

:: Trouve le dossier .minecraft
set MCDIR=%APPDATA%\.minecraft

if exist "%MCDIR%" (
    if not exist "%MCDIR%\mods" mkdir "%MCDIR%\mods"
    copy /Y "build\libs\speedrunbot-1.0.0.jar" "%MCDIR%\mods\" >nul
    echo  [OK] Mod copie dans %MCDIR%\mods\
) else (
    echo  [!] Dossier .minecraft introuvable a %MCDIR%
    echo  [*] Copie le fichier build\libs\speedrunbot-1.0.0.jar manuellement dans ton dossier mods.
)

echo.
echo  =============================================
echo   INSTALLATION TERMINEE !
echo  =============================================
echo.
echo  Etapes suivantes :
echo   1. Lance TLauncher
echo   2. Installe Fabric Loader 1.21.1 (si pas fait)
echo      -> https://fabricmc.net/use/installer/
echo   3. Lance Minecraft avec le profil Fabric 1.21.1
echo   4. En jeu, appuie sur B pour ouvrir le GUI du bot
echo.
echo  IMPORTANT: Le mod necessite Fabric Loader,
echo  pas Forge ! Installe bien le bon.
echo.
pause
