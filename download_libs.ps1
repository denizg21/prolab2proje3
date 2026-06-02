# Gerekli JAR dosyalarını indir
Write-Host "Kutuphaneler indiriliyor..."

# Gson - JSON isleme
$gson = "lib\gson-2.10.1.jar"
if (-not (Test-Path $gson)) {
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -OutFile $gson
    Write-Host "Gson indirildi."
} else {
    Write-Host "Gson zaten mevcut."
}

# SQLite JDBC 3.36.0.3 - SLF4J bagimliligi olmayan son stabil surum
$sqlite = "lib\sqlite-jdbc-3.36.0.3.jar"
if (-not (Test-Path $sqlite)) {
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar" -OutFile $sqlite
    Write-Host "SQLite JDBC indirildi."
} else {
    Write-Host "SQLite JDBC zaten mevcut."
}

# Eski surumu sil (varsa)
if (Test-Path "lib\sqlite-jdbc-3.45.3.0.jar") {
    Remove-Item "lib\sqlite-jdbc-3.45.3.0.jar"
    Write-Host "Eski SQLite JDBC silindi."
}

Write-Host "Hazir!"
