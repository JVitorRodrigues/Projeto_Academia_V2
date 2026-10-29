#!/bin/sh
# Uso: ./run_all.sh [usuario] [banco]   (ordem: tabelas -> functions -> views -> procedures -> inserts)
U=${1:-postgres}; DB=${2:-academia}
for d in tables functions views procedures inserts; do
  for f in "$(dirname "$0")"/$d/*.sql; do psql -v ON_ERROR_STOP=1 -U "$U" -d "$DB" -f "$f" || exit 1; done
done
