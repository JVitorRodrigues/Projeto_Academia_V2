#!/bin/sh
# Compila e executa a aplicação (Linux/macOS). Rode na raiz do projeto.
mkdir -p out
javac -encoding UTF-8 -cp "lib/postgresql-42.7.10.jar" -d out src/*.java || exit 1
java -Dfile.encoding=UTF-8 -cp "out:lib/postgresql-42.7.10.jar" Login
