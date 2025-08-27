@echo off
cd /d "C:\Users\arsur\Downloads\Nodesa\minecraft-nodes-dev\ports"
echo Compilando plugin con Gradle...
call gradlew.bat build

IF EXIST build\libs (
    echo.
    echo Compilación finalizada. Archivo generado en:
    dir build\libs\*.jar
) ELSE (
    echo Error: No se generó el archivo .jar.
)

pause