
$ErrorActionPreference = "Continue"
try {
    .\gradlew.bat :androidApp:assembleDebug --stacktrace | Out-File -FilePath build_error.log -Encoding UTF8
} catch {
    $_ | Out-File -FilePath build_error.log -Append -Encoding UTF8
}
