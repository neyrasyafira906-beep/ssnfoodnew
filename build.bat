@echo off
echo =========================================
echo   SSN FoodApp - Build
echo =========================================
if not exist out mkdir out
echo [1] Mengumpulkan file Java...
if exist sources.txt del sources.txt
dir /s /b src\main\java\*.java > sources_tmp.txt
powershell -Command "(gc sources_tmp.txt) | ForEach-Object { '\"' + $_.Replace('\', '/') + '\"' } | Out-File -encoding ASCII sources.txt"
del sources_tmp.txt

echo [2] Kompilasi...
javac -encoding UTF-8 -d out -cp ".;lib/*" src/main/java/com/ssn/food/App.java src/main/java/com/ssn/food/model/*.java src/main/java/com/ssn/food/service/*.java src/main/java/com/ssn/food/ui/*.java
if %ERRORLEVEL% NEQ 0 ( echo [ERROR] Kompilasi gagal! & pause & exit /b 1 )
echo Main-Class: com.ssn.food.App > manifest.txt
echo Class-Path: . lib/mysql-connector-j-9.7.0.jar >> manifest.txt
jar cfm foodapp.jar manifest.txt -C out .
if %ERRORLEVEL% NEQ 0 ( echo [ERROR] Gagal buat JAR! & pause & exit /b 1 )
echo.
echo =========================================
echo  SELESAI!  Jalankan: java -jar foodapp.jar
echo =========================================
pause
