# Raccourcis ADB pour Clara Speaker.
#
# make logs         Logs de l'app en temps réel sur le téléphone connecté
# make logs-all     Tous les logs du téléphone (non filtrés)
# make devices      Liste les appareils connectés
#
# Si plusieurs appareils sont connectés, préciser lequel :
#   make logs DEVICE=<numéro_de_série>

PACKAGE ?= pro.ongoua.claraspeaker
DEVICE  ?=

.PHONY: logs logs-all devices

logs:
	@dev="$(DEVICE)"; \
	if [ -z "$$dev" ]; then \
	  devs=$$(adb devices | awk '$$2=="device"{print $$1}'); \
	  n=$$(printf '%s\n' "$$devs" | grep -c .); \
	  if [ "$$n" -eq 0 ]; then echo "Aucun appareil connecté en état 'device'. Branchez le téléphone et autorisez le débogage USB."; exit 1; fi; \
	  if [ "$$n" -gt 1 ]; then echo "Plusieurs appareils détectés :"; printf '  - %s\n' $$devs; echo "Précisez lequel : make logs DEVICE=<serial>"; exit 1; fi; \
	  dev="$$devs"; \
	fi; \
	model=$$(adb -s $$dev shell getprop ro.product.model 2>/dev/null | tr -d '\r'); \
	echo "Appareil : $$dev ($$model)"; \
	echo "En attente de $(PACKAGE)… (lancez l'app si besoin)"; \
	pid=""; \
	while [ -z "$$pid" ]; do pid=$$(adb -s $$dev shell pidof -s $(PACKAGE) 2>/dev/null | tr -d '\r'); [ -z "$$pid" ] && sleep 1; done; \
	echo "Logs de $(PACKAGE) (PID $$pid) — Ctrl+C pour arrêter"; \
	adb -s $$dev logcat --pid=$$pid

logs-all:
	@dev="$(DEVICE)"; \
	if [ -z "$$dev" ]; then \
	  devs=$$(adb devices | awk '$$2=="device"{print $$1}'); \
	  n=$$(printf '%s\n' "$$devs" | grep -c .); \
	  if [ "$$n" -eq 0 ]; then echo "Aucun appareil connecté en état 'device'. Branchez le téléphone et autorisez le débogage USB."; exit 1; fi; \
	  if [ "$$n" -gt 1 ]; then echo "Plusieurs appareils détectés :"; printf '  - %s\n' $$devs; echo "Précisez lequel : make logs-all DEVICE=<serial>"; exit 1; fi; \
	  dev="$$devs"; \
	fi; \
	echo "Appareil : $$dev — logs complets (Ctrl+C pour arrêter)"; \
	adb -s $$dev logcat

devices:
	@adb devices -l
