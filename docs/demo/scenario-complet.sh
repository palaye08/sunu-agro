#!/usr/bin/env bash
# =============================================================================
# SunuAgri - Démonstration d'un scénario complet :
# de l'enregistrement d'un producteur jusqu'au paiement dû (et au SMS).
#
# Prérequis : application démarrée (mvn spring-boot:run), curl et jq installés.
# Usage     : ./docs/demo/scenario-complet.sh [url]   (défaut : http://localhost:8080)
# Conseil   : à jouer sur une base vide pour que la consommation FIFO des lots
#             ne porte que sur les lots créés par ce scénario.
# =============================================================================
set -euo pipefail

API="${1:-http://localhost:8080}/api/v1"
AGENT=(-H "X-Role: AGENT")
JSON=(-H "Content-Type: application/json")

# Données uniques à chaque exécution (téléphone et campagne doivent être uniques)
TEL_AWA="77$(printf '%07d' $((RANDOM * RANDOM % 10000000)))"
TEL_MODOU="78$(printf '%07d' $((RANDOM * RANDOM % 10000000)))"
ANNEE=$((2030 + RANDOM % 70))

etape() { printf '\n\033[1;32m==> %s\033[0m\n' "$1"; }
appel() { # méthode chemin [corps] [en-têtes supplémentaires...]
  local methode=$1 chemin=$2 corps=${3:-}; shift 3 || shift $#
  if [[ -n "$corps" ]]; then
    curl -sS -X "$methode" "$API$chemin" "${JSON[@]}" "$@" -d "$corps"
  else
    curl -sS -X "$methode" "$API$chemin" "$@"
  fi
}

etape "US-01 · L'agent enregistre deux producteurs"
AWA=$(appel POST /producteurs "{\"nom\":\"Diop\",\"prenom\":\"Awa\",\"telephone\":\"$TEL_AWA\",\"village\":\"Ndiaganiao\"}" "${AGENT[@]}")
echo "$AWA" | jq .
AWA_ID=$(echo "$AWA" | jq -r .id)
MODOU_ID=$(appel POST /producteurs "{\"nom\":\"Fall\",\"prenom\":\"Modou\",\"telephone\":\"$TEL_MODOU\",\"village\":\"Kaffrine\"}" "${AGENT[@]}" | jq -r .id)

etape "US-01 · Un téléphone déjà utilisé est refusé (format d'erreur unique)"
appel POST /producteurs "{\"nom\":\"Ba\",\"prenom\":\"Fatou\",\"telephone\":\"+221 $TEL_AWA\",\"village\":\"Louga\"}" "${AGENT[@]}" | jq .

etape "US-02 · Déclaration des parcelles"
PARCELLE_AWA=$(appel POST "/producteurs/$AWA_ID/parcelles" '{"nom":"Champ du baobab","superficieHa":3,"commune":"Fatick"}' "${AGENT[@]}" | jq -r .id)
PARCELLE_MODOU=$(appel POST "/producteurs/$MODOU_ID/parcelles" '{"nom":"Champ du puits","superficieHa":2,"commune":"Kaffrine"}' "${AGENT[@]}" | jq -r .id)
appel GET "/producteurs/$AWA_ID/parcelles" "" "${AGENT[@]}" | jq .

etape "US-04 · Ouverture de la campagne hivernage $ANNEE"
CAMPAGNE=$(appel POST /campagnes "{\"saison\":\"HIVERNAGE\",\"annee\":$ANNEE,\"dateDebut\":\"$ANNEE-06-01\",\"dateFin\":\"$ANNEE-12-31\"}" "${AGENT[@]}" | jq -r .id)
echo "Campagne : $CAMPAGNE"

etape "US-05 · Déclaration des cultures d'arachide"
CULTURE_AWA=$(appel POST /cultures "{\"parcelleId\":\"$PARCELLE_AWA\",\"campagneId\":\"$CAMPAGNE\",\"culture\":\"ARACHIDE\",\"superficieEmblaveeHa\":2}" "${AGENT[@]}" | jq -r .id)
CULTURE_MODOU=$(appel POST /cultures "{\"parcelleId\":\"$PARCELLE_MODOU\",\"campagneId\":\"$CAMPAGNE\",\"culture\":\"ARACHIDE\",\"superficieEmblaveeHa\":1}" "${AGENT[@]}" | jq -r .id)

etape "US-06 · Semis"
appel POST "/cultures/$CULTURE_AWA/semis" "{\"dateSemis\":\"$ANNEE-07-01\"}" "${AGENT[@]}" | jq -c '{statut, dateSemis}'
appel POST "/cultures/$CULTURE_MODOU/semis" "{\"dateSemis\":\"$ANNEE-07-05\"}" "${AGENT[@]}" | jq -c '{statut, dateSemis}'

etape "US-07 · Une récolte suspecte (> 2 x 1 200 kg/ha) est refusée"
appel POST "/cultures/$CULTURE_AWA/recolte" "{\"dateRecolte\":\"$ANNEE-10-15\",\"quantiteKg\":5000}" "${AGENT[@]}" | jq .

etape "US-06/07 · Récoltes"
appel POST "/cultures/$CULTURE_AWA/recolte" "{\"dateRecolte\":\"$ANNEE-10-15\",\"quantiteKg\":2000}" "${AGENT[@]}" | jq -c '{statut, quantiteRecolteeKg, rendementKgParHa}'
appel POST "/cultures/$CULTURE_MODOU/recolte" "{\"dateRecolte\":\"$ANNEE-10-20\",\"quantiteKg\":1000}" "${AGENT[@]}" | jq -c '{statut, quantiteRecolteeKg, rendementKgParHa}'

etape "US-08 · Une livraison trop humide (> 9 % pour l'arachide) est refusée"
appel POST "/cultures/$CULTURE_AWA/livraisons" '{"quantiteKg":400,"tauxHumidite":11,"qualite":"A"}' "${AGENT[@]}" | jq .

etape "US-08 · Livraisons à l'entrepôt (création des lots ; Awa livre en premier)"
appel POST "/cultures/$CULTURE_AWA/livraisons" '{"quantiteKg":400,"tauxHumidite":8.5,"qualite":"A"}' "${AGENT[@]}" | jq .
sleep 1
appel POST "/cultures/$CULTURE_MODOU/livraisons" '{"quantiteKg":500,"tauxHumidite":8,"qualite":"A"}' "${AGENT[@]}" | jq -c '{id, producteurId, quantiteKg, dateReception}'

etape "US-09 · Stock d'arachide de qualité A"
appel GET "/stocks?culture=ARACHIDE&qualite=A" "" "${AGENT[@]}" | jq .

etape "US-18 · Prix de référence du marché"
appel GET /prix-marche/ARACHIDE "" "${AGENT[@]}" | jq .

etape "US-10 · Publication d'une offre de 900 kg à 350 XOF/kg"
OFFRE=$(appel POST /offres '{"culture":"ARACHIDE","qualite":"A","quantiteKg":900,"prixParKg":350}' "${AGENT[@]}")
echo "$OFFRE" | jq .
OFFRE_ID=$(echo "$OFFRE" | jq -r .id)
appel GET "/stocks?culture=ARACHIDE&qualite=A" "" "${AGENT[@]}" | jq -c '.[0]'

etape "US-11 · L'acheteur ACH-001 commande 600 kg"
ACHETEUR=(-H "X-Role: ACHETEUR" -H "X-Utilisateur-Id: ACH-001")
COMMANDE=$(appel POST /commandes "{\"offreId\":\"$OFFRE_ID\",\"quantiteKg\":600}" "${ACHETEUR[@]}")
echo "$COMMANDE" | jq .
COMMANDE_ID=$(echo "$COMMANDE" | jq -r .id)

etape "US-17 · Un autre acheteur ne voit pas cette commande (404, sans révéler son existence)"
appel GET "/commandes/$COMMANDE_ID" "" -H "X-Role: ACHETEUR" -H "X-Utilisateur-Id: ACH-002" | jq .

etape "US-13/14 · L'agent confirme la livraison : lots FIFO, commission 5 %, paiements dus, SMS"
appel POST "/commandes/$COMMANDE_ID/livraison" "" "${AGENT[@]}" | jq .

etape "US-13 · Une commande déjà livrée ne peut pas être livrée à nouveau"
appel POST "/commandes/$COMMANDE_ID/livraison" "" "${AGENT[@]}" | jq -c '{status, code, detail}'

etape "US-09 · Stock après la vente"
appel GET "/stocks?culture=ARACHIDE&qualite=A" "" "${AGENT[@]}" | jq -c '.[0]'

etape "US-17 · Awa consulte SES paiements dus"
appel GET "/producteurs/$AWA_ID/paiements" "" -H "X-Role: PRODUCTEUR" -H "X-Utilisateur-Id: $AWA_ID" | jq .

etape "US-17 · Awa ne peut pas consulter les paiements de Modou (404)"
appel GET "/producteurs/$MODOU_ID/paiements" "" -H "X-Role: PRODUCTEUR" -H "X-Utilisateur-Id: $AWA_ID" | jq -c '{status, code}'

etape "US-16 · Historique paginé de l'acheteur"
appel GET "/commandes?page=0&taille=10" "" "${ACHETEUR[@]}" | jq -c '{totalElements, elements: [.elements[] | {id, statut, montant}]}'

printf '\n\033[1;32mScénario terminé. Les SMS simulés sont visibles dans le journal de l application ([SMS SIMULÉ]).\033[0m\n'
