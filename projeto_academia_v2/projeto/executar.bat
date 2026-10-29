@echo off
REM Compila e executa a aplicação (Windows). Rode na raiz do projeto.
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib\postgresql-42.7.10.jar" -d out src\*.java || exit /b 1
java -Dfile.encoding=UTF-8 -cp "out;lib\postgresql-42.7.10.jar" Login
