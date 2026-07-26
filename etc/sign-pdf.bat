@echo off
REM Launches sign-pdf on Windows.
REM
REM   sign-pdf.bat                                                          -> Swing UI
REM   sign-pdf.bat in.pdf out.pdf "Name" "Purpose" "Contact" 1 50 700 200 80 -> CLI
REM
REM Looks for sign-pdf-1.0-jar-with-dependencies.jar next to this script first
REM (drop the jar in etc\ for a standalone deployment), then falls back to
REM ..\target (a fresh "mvn package" checkout).

setlocal

set "SCRIPT_DIR=%~dp0"
set "JAR_NAME=sign-pdf-1.0-jar-with-dependencies.jar"

set "JAR="
if exist "%SCRIPT_DIR%%JAR_NAME%" set "JAR=%SCRIPT_DIR%%JAR_NAME%"
if not defined JAR if exist "%SCRIPT_DIR%..\target\%JAR_NAME%" set "JAR=%SCRIPT_DIR%..\target\%JAR_NAME%"

if not defined JAR (
    echo Could not find %JAR_NAME% next to this script or in ..\target\. 1>&2
    echo Build it first with: mvn package 1>&2
    exit /b 1
)

if "%~1"=="" (
    java -jar "%JAR%"
) else (
    java -cp "%JAR%" org.r7c.pdf.pades.PadesUtils %*
)

endlocal
