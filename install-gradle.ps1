#!/usr/bin/env pwsh

echo "Installation du wrapper Gradle..."

# Télécharger Gradle Wrapper
$gradleVersion = "8.2.1"
$gradleUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
$gradleZip = "gradle.zip"
$gradleHome = "$PWD/gradle"

# Créer le répertoire gradle si nécessaire
if (-Not (Test-Path "gradle")) {
    New-Item -ItemType Directory -Path "gradle"
}

# Télécharger uniquement si pas déjà présent
if (-Not (Test-Path "$gradleHome/gradle-$gradleVersion")) {
    Write-Host "Téléchargement de Gradle $gradleVersion..."
    Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip
    
    Write-Host "Extraction de Gradle..."
    Expand-Archive -Path $gradleZip -DestinationPath $gradleHome -Force
    Remove-Item $gradleZip
}

Write-Host "Installation terminée!"
Write-Host "Vous pouvez maintenant utiliser './gradlew' pour compiler le projet."
