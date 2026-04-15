@echo off
echo =========================================
echo   SSN FoodApp - Build
echo =========================================
if not exist out mkdir out
echo [1] Mengumpulkan file Java...
dir /s /b src\main\java\*.java > sources.txt
echo [2] Kompilasi...
javac -d out -encoding UTF-8 --release 11 @sources.txt
if %ERRORLEVEL% NEQ 0 ( echo [ERROR] Kompilasi gagal! & pause & exit /b 1 )
echo [3] Membuat JAR...
jar cfm foodapp.jar manifest.txt -C out .
if %ERRORLEVEL% NEQ 0 ( echo [ERROR] Gagal buat JAR! & pause & exit /b 1 )
echo.
echo =========================================
echo  SELESAI!  Jalankan: java -jar foodapp.jar
echo =========================================
pause
