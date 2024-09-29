import os
import threading
import paho.mqtt.client as mqtt
import json
from datetime import datetime, timedelta
import time
import random
import requests

class Fotocamera:
    def __init__(self, tipo):
        self.tipo = tipo
        self.username = None
        self.targa = None
        self.timestamp = None
        self.flag = False
        self.token=None

    def set_dati(self, username, targa, timestamp,token):
        self.username = username
        self.targa = targa
        self.timestamp = timestamp
        self.flag = True
        self.token=token

    def rileva_modifica(self):
        if self.flag:
            self.flag = False
            return self.username, self.targa, self.timestamp
        return None, None, None

class SensoreOccupazione:
    def __init__(self, id, occupato, riservato):
        self.id = id
        self.occupato = occupato
        self.riservato = riservato
        self.modificato = False

class MWBot:
    def __init__(self):
        self.occupato = False
        self.fine_ricarica_time = None
        self.auto_in_ricarica = None

        self.auto_list = {
            "Tesla Model S": 40,  # Capacità della batteria in kW
            "Audi e-tron": 95,
            "BMW i4": 83,
            "Nissan Leaf": 62,
            "Ford Mustang Mach-E": 88,
            "Volkswagen ID.4": 82,
            "Mercedes EQC": 80,
            "Porsche Taycan": 93,
            "Rivian R1T": 135,
            "Hyundai Kona Electric": 64
        }

class Simulatore:
    def __init__(self, centralina):
        self.centralina = centralina
        self.centralina.set_callback(self.gestisci_callback)
        self.callbacks_to_wait = 0
        self.route_handler_url = "http://localhost:8080"
        self.token = None  # Variabile per memorizzare il token

    def login(self):
        username = input("Inserisci username amministratore: ").strip()
        password = input("Inserisci password amministratore: ").strip()
        
        credentials = {"Username": username, "Password": password}
        response = requests.post(f"{self.route_handler_url}/api/login", json=credentials)
        if response.status_code == 200:
            response_json = response.json()
            self.token = response_json.get("token")
            token= self.token
            print("Login effettuato con successo, token ricevuto:", self.token)
            return True
        else:
            print("Login fallito:", response.text)
            return False
        
    def get_token(self):
        """Restituisce il token generato dopo il login."""
        if not self.token:
            print("Devi effettuare il login per accedere al simulatore.")
        return self.token

    def connect(self):
        if not self.token:
            print("Devi effettuare il login prima!")
            return

        # Chiama il metodo on_connect per iniziare a connettersi al broker MQTT
        self.centralina.on_connect(self.centralina.client, None, None, None)
        # Effettua la connessione al broker MQTT
        self.centralina.client.connect(self.centralina.broker_address)
        self.centralina.client.loop_start()  # Avvia il loop per gestire i messaggi MQTT


    def run(self):
        if not self.login():  # Effettua il login prima di procedere
            exit(1)

        # Assegna il token alla centralina dopo il login
        self.centralina.token = self.token

        self.connect()  # Avvia la connessione dopo il login

        # Aspetta che i sensori siano pronti prima di procedere
        self.wait_for_sensori()

        while True:
            while self.callbacks_to_wait > 0:
                time.sleep(0.1)

            self.callbacks_to_wait = 1

            self.centralina.print_output_buffer()
            input("\nPremi invio per continuare...")  # attendi che l'utente prema invio
            print("\033[H\033[2J")

            # Usa il metodo get_simulated_timestamp e formatta direttamente l'oggetto datetime
            simulated_time = self.centralina.get_simulated_timestamp()
            formatted_time = simulated_time.strftime("%d %B %Y, %H:%M")
            print("\nOrario simulato:", formatted_time)

            print("\nScegli un'opzione:")
            print("1. Simulare ingresso")
            print("2. Simulare uscita")
            print("3. Visualizzare monitor all'ingresso")
            print("4. Arrestare programma")
            choice = input("Inserisci il numero dell'opzione: ")

            if choice == '1':
                self.simula_ingresso()
            elif choice == '2':
                self.simula_uscita()
            elif choice == '3':
                self.visualizza_monitor()
            elif choice == '4':
                os._exit(0)
            else:
                self.centralina.output_buffer.append("Opzione non valida.")
                self.callbacks_to_wait = 0

    def wait_for_sensori(self):
        self.centralina.sensori_ready_event.wait()  # Attendi che l'evento venga impostato
        self.centralina.output_buffer.append("Sensori inizializzati.")

    def simula_ingresso(self):
        self.centralina.waiting_for_ingresso_prenotato = True
        username = input("\nInserisci l'username: ").strip()
        targa = input("Inserisci la targa: ").strip()
        
        if not username or not targa:
            self.centralina.output_buffer.append("Errore: La fotocamera non ha rilevato correttamente uno dei due valori (username o targa).")
            self.callbacks_to_wait = 0
            self.centralina.waiting_for_ingresso_prenotato = False
            return
        
        timestamp = self.centralina.get_simulated_timestamp()
        payload = {
            "username": username,
            "targa": targa,
            "timestamp": timestamp.strftime("%d-%m-%Y %H:%M:%S"),
            "token": self.token  # Aggiungi il token al payload
        }
    
        print(payload)
        self.simula_rilevamento_fotocamera_ingresso(username, targa, timestamp,self.get_token())

    def simula_uscita(self):
        self.centralina.waiting_for_uscita_prenotata = True
        username = input("\nInserisci l'username: ").strip()
        targa = input("Inserisci la targa: ").strip()
        
        if not username or not targa:
            self.centralina.output_buffer.append("Errore: La fotocamera non ha rilevato correttamente uno dei due valori (username o targa).")
            self.callbacks_to_wait = 0
            self.centralina.waiting_for_uscita_prenotata = False
            return
        
        timestamp = self.centralina.get_simulated_timestamp()
        self.simula_rilevamento_fotocamera_uscita(username, targa, timestamp,self.token)
    
    def visualizza_monitor(self):
        # Verifica che i sensori siano pronti
        if not self.centralina.sensori_occupazione:
            self.centralina.output_buffer.append("I sensori non sono stati inizializzati correttamente.")
            return

        # Stampa lo stato di ogni posto auto
        num_auto_dentro = 0
        print("\nMonitor ingresso:\n")
        print("  ID  |   Stato  | Riservato")
        print("------|----------|----------")
        for sensore in self.centralina.sensori_occupazione:
            stato = "Occupato" if sensore.occupato else "Libero"
            riservato = "Si" if sensore.riservato else "No"
            print(f" {sensore.id:4} | {stato:8} | {riservato:9}")
            
            # Conta il numero di posti auto occupati
            if sensore.occupato:
                num_auto_dentro += 1

        # Stampa il numero di auto dentro il parcheggio
        print(f"\nNumero di auto dentro il parcheggio: {num_auto_dentro}")

        self.callbacks_to_wait = 0

    def gestisci_callback(self, topic, message):
        if topic == self.centralina.topic_verifica_ingresso_risposta:
            self.gestisci_risposta_verifica_ingresso(message)
        elif topic == self.centralina.topic_verifica_uscita_risposta:
            self.gestisci_risposta_verifica_uscita(message)
        elif topic == self.centralina.topic_sensore_occupazione_risposta:
            self.gestisci_risposta_sensore_occupazione(message)
        
        self.callbacks_to_wait -= 1

    def gestisci_risposta_verifica_ingresso(self, message):
        self.centralina.waiting_for_ingresso_prenotato = False
        try:
            # Decodifica e analisi del payload JSON
            json_response = json.loads(message.payload.decode("utf-8"))
            
            # Gestione delle risposte in base al messaggio JSON
            if json_response["message"].startswith("Ingresso permesso"):
                self.callbacks_to_wait += 1
                # Controllo se è entrato tramite prenotazione
                if "info" in json_response:
                    # Estrae le informazioni sulla prenotazione
                    info_list = json_response["info"].split(", ")
                    id_posto_auto = info_list[2].split(": ")[1]  # Estrai id_posto_auto

                    self.simula_rilevamento_sensore(id_posto_auto)
                else:
                    self.simula_rilevamento_sensore()
            else:
                self.centralina.output_buffer.append(json_response["message"])
                self.callbacks_to_wait = 0
        except json.JSONDecodeError:
            self.centralina.output_buffer.append("Errore nel decodificare il JSON ricevuto.")
        except Exception:
            self.centralina.output_buffer.append("Errore nella gestione della risposta.")

    def gestisci_risposta_verifica_uscita(self, message):
        self.centralina.waiting_for_uscita_prenotata = False
        try:
            # Decodifica e analisi del payload JSON
            json_response = json.loads(message.payload.decode("utf-8"))
            
            # Gestione delle risposte in base al messaggio JSON
            if json_response["message"].startswith("Uscita permessa"):
                self.callbacks_to_wait += 1
                id_posto_auto = int(json_response["message"].split(": ")[3])
                self.simula_rilevamento_sensore(id_posto_auto, True)
            else:
                self.centralina.output_buffer.append(json_response["message"])
                self.callbacks_to_wait = 0
        except json.JSONDecodeError:
            self.centralina.output_buffer.append("Errore nel decodificare il JSON ricevuto.")
        except Exception:
            self.centralina.output_buffer.append("Errore nella gestione della risposta.")
    
    def gestisci_risposta_sensore_occupazione(self, message):
        try:
            json_response = json.loads(message.payload.decode("utf-8"))
            if "message" in json_response:
                self.centralina.output_buffer.append(json_response["message"])
            elif "error" in json_response:
                self.centralina.output_buffer.append(json_response["error"])
        except json.JSONDecodeError:
            self.centralina.output_buffer.append("Errore nel decodificare il JSON ricevuto.")
        except Exception:
            self.centralina.output_buffer.append("Errore nella gestione della risposta del sensore di occupazione.")

    def simula_rilevamento_fotocamera_ingresso(self, username, targa, timestamp,token):
        self.centralina.fotocamera_ingresso.set_dati(username, targa, timestamp,token)

    def simula_rilevamento_fotocamera_uscita(self, username, targa, timestamp,token):
        self.centralina.fotocamera_uscita.set_dati(username, targa, timestamp,token)

    def simula_rilevamento_sensore(self, id_posto_auto=None, uscita=False):
        if id_posto_auto is not None and not uscita:
            id_posto_auto = int(id_posto_auto)
            # Recupera il sensore associato al posto auto
            for sensore in self.centralina.sensori_occupazione:
                if sensore.id == id_posto_auto:
                    # Aggiorna lo stato del sensore
                    sensore.occupato = True
                    sensore.modificato = True
                    return
                
            self.centralina.output_buffer.append(f"Nessun sensore associato al posto auto {id_posto_auto}")
        # gestisco uscita
        elif id_posto_auto is not None and uscita:
            id_posto_auto = int(id_posto_auto)
            # Recupera il sensore associato al posto auto
            for sensore in self.centralina.sensori_occupazione:
                if sensore.id == id_posto_auto:
                    # Aggiorna lo stato del sensore
                    sensore.occupato = False
                    sensore.modificato = True
                    return
        else:
            # Se non è stato passato l'ID del posto auto (senza prenotazione), cerca il primo sensore disponibile
            for sensore in self.centralina.sensori_occupazione:
                if not sensore.occupato and not sensore.riservato:
                    # Aggiorna lo stato del sensore
                    sensore.occupato = True
                    sensore.modificato = True
                    return
            
            self.centralina.output_buffer.append(f"Nessun sensore associato al posto auto {id_posto_auto}")

        self.centralina.output_buffer.append("Nessun posto auto disponibile")
        self.callbacks_to_wait = 0

class Centralina:
    def __init__(self, broker_address, topic_verifica_ingresso_risposta, topic_verifica_uscita_risposta, topic_risposta_tutti_id_posto_auto, 
                topic_sensore_occupazione_risposta, topic_risposta_tutte_prenotazioni_attive, topic_risposta_multe, topic_risposta_prenotazioni_attive_auto_dentro, 
                topic_risposta_ricariche, topic_presa_in_carico_bot,token=None):
        self.broker_address = broker_address
        self.topic_verifica_ingresso_risposta = topic_verifica_ingresso_risposta
        self.topic_verifica_uscita_risposta = topic_verifica_uscita_risposta
        self.topic_risposta_tutti_id_posto_auto = topic_risposta_tutti_id_posto_auto
        self.topic_sensore_occupazione_risposta = topic_sensore_occupazione_risposta
        self.topic_risposta_tutte_prenotazioni_attive = topic_risposta_tutte_prenotazioni_attive
        self.topic_uscite_automatiche = topic_risposta_prenotazioni_attive_auto_dentro
        self.topic_risposta_ricariche = topic_risposta_ricariche
        self.topic_risposta_multe = topic_risposta_multe
        self.topic_presa_in_carico_bot = topic_presa_in_carico_bot
        self.fotocamera_ingresso = Fotocamera('ingresso')
        self.fotocamera_uscita = Fotocamera('uscita')
        self.mwBot = MWBot()
        self.accelerazione_tempo = 10
        self.start_time = datetime.now()
        self.output_buffer = []
        self.sensori_occupazione = []
        self.prenotazioni_attive = []
        self.uscite_automatiche = []
        self.ricariche_da_effettuare = []
        self.client = mqtt.Client()
        self.sensori_ready_event = threading.Event()
        self.waiting_for_risposta_multe = False
        self.waiting_for_risposta_prenotazioni_attive = False
        # self.waiting_for_ingresso_prenotato = False
        self.waiting_for_uscita_prenotata = False
        self.waiting_risposta_ricariche = False
        self.waiting_for_risposta_bot = False  
        self.token=token
        self.ingressi_recenti = {}

        # Crea e avvia il thread che si occupa dei controlli
        # delle multe e delle uscite automatiche (chi è entrato tramite prenotazione)
        self.controlli_thread = threading.Thread(target=self.controlli_loop)
        self.controlli_thread.daemon = True
        self.controlli_thread.start()

        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message
        self.client.connect(self.broker_address)

    def on_connect(self, client, userdata, flags, rc):
        self.client.subscribe(self.topic_verifica_ingresso_risposta)
        self.client.subscribe(self.topic_verifica_uscita_risposta)
        self.client.subscribe(self.topic_risposta_tutti_id_posto_auto)
        self.client.subscribe(self.topic_sensore_occupazione_risposta)
        self.client.subscribe(self.topic_risposta_tutte_prenotazioni_attive)
        self.client.subscribe(self.topic_risposta_multe)
        self.client.subscribe(self.topic_uscite_automatiche)
        self.client.subscribe(self.topic_risposta_ricariche)
        self.client.subscribe(self.topic_presa_in_carico_bot)
        self.richiedi_tutti_id_posto_auto()

    def run(self):
        self.client.loop_start()
        while True:      
            # Verifico se la fotocamera di ingresso ha rilevato un auto 
            username, targa, timestamp = self.fotocamera_ingresso.rileva_modifica()
            if username and targa and timestamp:
                if targa in self.ingressi_recenti:
                    last_timestamp = self.ingressi_recenti[targa]
                    if (timestamp - last_timestamp).total_seconds() < 10:
                        continue  # Ignora questo ingresso poiché è troppo vicino al precedente

                self.invia_dati_ingresso(username, targa, timestamp, self.token)
                self.ingressi_recenti[targa] = timestamp  # Aggiorna l'ultimo timestamp per questa targa

            # Verifico se i sensori hanno subito delle modifiche
            for sensore in self.sensori_occupazione:
                if sensore.modificato and sensore.occupato:
                    self.invia_dati_sensore(sensore.id, sensore.occupato, self.fotocamera_ingresso.username, self.fotocamera_ingresso.targa,self.token)
                    sensore.modificato = False
                elif sensore.modificato and not sensore.occupato:
                    self.invia_dati_sensore(sensore.id, sensore.occupato, self.fotocamera_uscita.username, self.fotocamera_uscita.targa,self.token)
                    sensore.modificato = False

            # Verifico se la fotocamera di uscita ha rilevato un auto
            username, targa, timestamp= self.fotocamera_uscita.rileva_modifica()
            if username and targa and timestamp:
                self.invia_dati_uscita(username, targa, timestamp, self.token)

            self.check_next_ricarica()

            # Verifico se bot ha finito una ricarica
            if self.mwBot.occupato: # and not self.waiting_risposta_ricariche:
                if self.get_simulated_timestamp() >= self.mwBot.fine_ricarica_time:
                    self.mwBot.occupato = False
                    self.mwBot.fine_ricarica_time = None
                    self.invia_fine_ricarica(self.mwBot.auto_in_ricarica, self.get_simulated_timestamp(), self.mwBot.auto_in_ricarica,self.token)
                    self.mwBot.auto_in_ricarica = None
                    self.output_buffer.append("MWBot è disponibile")

        self.client.loop_stop()
        self.client.disconnect()

    # controlla se deve fare multe e se ci sono uscite automatiche 
    def controlli_loop(self):
        while True:
            time.sleep(6) # 6 secondi = 1 minuto simulato
            # Verifico se ci sono prenotazioni attive nuove
            self.richiedi_prenotazioni_attive()

            # Verifico se ci sono richieste di ricariche
            self.richiedi_ricariche()

            # Verifico se ci sono veicoli in posti riservati (hanno fatto prenotazione) per fare uscita automatica
            self.richiedi_prenotazioni_attive_auto_dentro()

            # Controllo mancate entrate prenotate (multa)
            self.check_multa()

            # Controllo ricariche
            # self.check_next_ricarica()
            
            # Controllo se deve uscire una macchina automaticamente (ha fatto prenotazione)
            self.check_simulazione_uscita_automatica()

    def richiedi_ricariche(self):
        self.waiting_risposta_ricariche = True
        topic_richiesta = "parcheggio/richiesta_ricariche"
        payload = {
            "message": "Richiesta ricariche",
            "token": self.token  # Aggiungi il token al payload
        }
        self.client.publish(topic_richiesta, json.dumps(payload))

    def check_next_ricarica(self):
        while self.waiting_for_risposta_bot:
            continue

        while self.waiting_risposta_ricariche:
            continue
            
        if self.mwBot.occupato:
            return

        if len(self.ricariche_da_effettuare) == 0:
            return
        
        # Scegli la prima ricarica nella lista
        ricarica_selezionata = self.ricariche_da_effettuare.pop(0)
        id_ricarica = ricarica_selezionata['idRicarica']
        id_veicolo = ricarica_selezionata['idVeicolo']
        id_posto_auto = ricarica_selezionata['id_posto_auto']
        percentuale_ricarica = ricarica_selezionata['percentualeRicarica']  # Richiesta dell'utente
        self.mwBot.auto_in_ricarica = id_ricarica

        # Scegli un modello a caso tra quelli inseriti in self.auto_list
        modello_veicolo = random.choice(list(self.mwBot.auto_list.keys()))

        # Recupera la capacità totale dell'auto dal dizionario self.auto_list
        capacita_totale = self.mwBot.auto_list[modello_veicolo]

        # Genera un numero casuale minore di "percentualeRicarica" (richiesta dell'utente)
        percentualeRicaricaAttuale = random.randint(0, percentuale_ricarica - 1)  # Percentuale attuale di ricarica

        # Calcola il tempo di ricarica
        tempo_base = 30  # minuti, tempo di ricarica da 0 a 100% per una auto da 40kW
        delta_percentuale = percentuale_ricarica - percentualeRicaricaAttuale
        tempo_ricarica = (delta_percentuale / 100) * (capacita_totale / 40) * tempo_base
        self.mwBot.fine_ricarica_time = self.get_simulated_timestamp() + timedelta(minutes=tempo_ricarica)
        self.mwBot.occupato = True

        energia_necessaria = (delta_percentuale / 100) * capacita_totale
        potenza_ricarica = energia_necessaria / (tempo_ricarica / 60)
        self.mwBot.potenza_ricarica = potenza_ricarica
        self.invia_dati_bot(id_veicolo, capacita_totale, modello_veicolo, id_ricarica, tempo_ricarica, id_posto_auto)

    def richiedi_prenotazioni_attive(self):
        # while self.waiting_for_ingresso_prenotato:
        #     continue

        while self.waiting_for_risposta_multe:
            continue

        self.waiting_for_risposta_prenotazioni_attive = True
        topic_richiesta = "parcheggio/richiesta_tutte_prenotazioni_attive"
        payload = {
            "message": "Richiesta prenotazioni attive",
            "token": self.token  # Aggiungi il token al payload
        }
        self.client.publish(topic_richiesta, json.dumps(payload))


    def check_multa(self):
        while self.waiting_for_risposta_prenotazioni_attive:
            continue

        # Ottieni l'orario simulato corrente
        simulated_time = self.get_simulated_timestamp()
        multe_da_effettuare = []
        
        i = 0
        while i < len(self.prenotazioni_attive):
            info = self.prenotazioni_attive[i]

            ingresso = datetime.strptime(info['tempo_arrivo'], "%d/%m/%Y %H:%M")
            ingresso_plus_15_min = ingresso + timedelta(minutes=15)
            if ingresso_plus_15_min < simulated_time:
                # Aggiungi il timestamp corrente alla info
                info['timestamp_multa'] = simulated_time.strftime("%d-%m-%Y %H:%M:%S")
                # Inserisco prenotazione scaduta nella lista di multe da effettuare
                multe_da_effettuare.append(info)
                self.prenotazioni_attive.pop(i)
                continue
            
            i += 1

        # Controllo se ci sono multe da effettuare
        if multe_da_effettuare:
            self.invia_multe(multe_da_effettuare)
            multe_da_effettuare = []
    
    def richiedi_prenotazioni_attive_auto_dentro(self):
        self.waiting_for_uscita_prenotata = True
        topic_richiesta = "parcheggio/richiesta_tutte_prenotazioni_attive_auto_dentro"
        payload = {
            "message": "Richiesta prenotazioni attive auto dentro",
            "token": self.token  # Aggiungi il token al payload
        }
        self.client.publish(topic_richiesta, json.dumps(payload))

    def check_simulazione_uscita_automatica(self):
        #while self.waiting_for_uscita_prenotata:
            #print("f")
            #ontinue

        for uscita_automatica in self.uscite_automatiche:
            uscita_time = datetime.strptime(uscita_automatica["uscita"], "%d-%m-%Y %H:%M:%S")
            simulated_time = self.get_simulated_timestamp()
            if uscita_time - timedelta(minutes=15) <= simulated_time < uscita_time:
                # Uscita automatica
                username = uscita_automatica["username"]
                targa = uscita_automatica["targa"]
                self.fotocamera_uscita.set_dati(username, targa, uscita_time,token)
                self.uscite_automatiche.remove(uscita_automatica)    

    def print_output_buffer(self):
        print()
        while self.output_buffer:
            print(self.output_buffer.pop(0))

    def get_simulated_timestamp(self):
        elapsed_time = datetime.now() - self.start_time
        simulated_time = self.start_time + timedelta(seconds=(elapsed_time.total_seconds() * self.accelerazione_tempo))
        return simulated_time

    def richiedi_tutti_id_posto_auto(self):
        topic_richiesta = "parcheggio/richiesta_tutti_id_posto_auto"
        self.client.publish(topic_richiesta, "Richiesta lista ID posti auto")

    def gestisci_risposta_prenotazioni_attive_auto_dentro(self, message):
        try:
            uscite = json.loads(message)
            self.uscite_automatiche = uscite
            self.waiting_for_uscita_prenotata = False
        except json.JSONDecodeError:
            self.output_buffer.append("Errore nel decodificare il JSON ricevuto per le prenotazioni attive.")
        except Exception:
            self.output_buffer.append("Errore inaspettato durante la gestione della risposta delle prenotazioni attive.")

    def gestisci_risposta_prenotazioni_attive(self, message):
        try:
            prenotazioni = json.loads(message)
            self.prenotazioni_attive = prenotazioni
            self.waiting_for_risposta_prenotazioni_attive = False
        except json.JSONDecodeError:
            self.output_buffer.append("Errore nel decodificare il JSON ricevuto per le prenotazioni attive.")
        except Exception:
            self.output_buffer.append("Errore inaspettato durante la gestione della risposta delle prenotazioni attive.")

    def gestisci_risposta_ricariche(self, message):
        try:
            ricariche = json.loads(message)
            self.ricariche_da_effettuare = ricariche
            self.waiting_risposta_ricariche = False
        except json.JSONDecodeError:
            self.output_buffer.append("Errore nel decodificare il JSON ricevuto per le prenotazioni attive.")
        except Exception:
            self.output_buffer.append("Errore inaspettato durante la gestione della risposta delle prenotazioni attive.")

    def gestisci_risposta_id_posto_auto(self, message):
        try:
            parking_spots = json.loads(message)
            self.sensori_occupazione = [
                SensoreOccupazione(id=spot["id"], occupato=spot["occupato"], riservato=spot["riservato"]) 
                for spot in parking_spots
            ]
            # Stampa i sensori inizializzati
            for sensore in self.sensori_occupazione:
                stato = "Occupato" if sensore.occupato else "Libero"
                riservato = "Si" if sensore.riservato else "No" 
                self.output_buffer.append(f"Sensore ID: {sensore.id}, Stato: {stato}, Riservato: {riservato}")
            
            self.sensori_ready_event.set()  # Imposta l'evento per indicare che i sensori sono pronti
        except json.JSONDecodeError:
            self.output_buffer.append("Errore nel decodificare il JSON ricevuto.")
        except Exception:
            self.output_buffer.append("Errore nella gestione della risposta")

    def gestisci_risposta_multe(self, message):
        try:
            response_json = json.loads(message)
            
            if response_json['message'] == 'Multe effettuate':
                multati_users = response_json['multati']

                for user in multati_users:
                    self.output_buffer.append(f"Utente {user} multato.")
                
                self.waiting_for_risposta_multe = False
            else:
                self.output_buffer.append("Errore del Server")
        except Exception:
            self.output_buffer.append("Errore nella gestione della risposta")

    def gestisci_risposta_bot(self, message):
        try:
            response_json = json.loads(message)
            self.waiting_for_risposta_bot = False
            
            if 'message' in response_json:
                self.output_buffer.append(response_json['message'])
            elif 'error' in response_json:
                self.output_buffer.append("Errore del Serverina")
            else:
                self.output_buffer.append("Risposta non valida")
        except Exception:
            self.output_buffer.append("Errore nella gestione della risposta")

    def set_callback(self, callback):
        self.callback = callback    

    def on_message(self, client, userdata, message):
        topic = message.topic
        payload = message.payload.decode()

        if topic == self.topic_risposta_tutti_id_posto_auto:
            self.gestisci_risposta_id_posto_auto(payload)
        elif topic == self.topic_risposta_tutte_prenotazioni_attive:
            self.gestisci_risposta_prenotazioni_attive(payload)
        elif topic == self.topic_risposta_multe:
            self.gestisci_risposta_multe(payload)
        elif topic == self.topic_uscite_automatiche:
            self.gestisci_risposta_prenotazioni_attive_auto_dentro(payload)
        elif topic == self.topic_risposta_ricariche:
            self.gestisci_risposta_ricariche(payload)
        elif topic == self.topic_presa_in_carico_bot:
            self.gestisci_risposta_bot(payload)
        elif topic in (self.topic_verifica_ingresso_risposta, self.topic_verifica_uscita_risposta, self.topic_sensore_occupazione_risposta):
            if self.callback:
                self.callback(topic, message)
        else:
            print(f'\n\n[TOPIC SCONOSCIUTO]: {payload}\n\n')

    def invia_dati_ingresso(self, username, targa, timestamp, token ):
        print(f"Invio dati ingresso: {username}, {targa}, {timestamp}, {token}")
        if username and targa:
            timestamp_str = timestamp.strftime("%d-%m-%Y %H:%M:%S")
            data = {
                "tipo": "ingresso",
                "username": username,
                "targa": targa,
                "timestamp": timestamp_str,
                "token": token  # Aggiungi il token al payload
            }
            message = json.dumps(data)
            topic_richiesta = "parcheggio/verifica_ingresso"
            self.client.publish(topic_richiesta, message)

    def invia_dati_uscita(self, username, targa, timestamp, token):
        if username and targa:
            timestamp_str = timestamp.strftime("%d-%m-%Y %H:%M:%S")
            data = {
                "tipo": "uscita",
                "username": username,
                "targa": targa,
                "timestamp": timestamp_str,
                "token": token  # Aggiungi il token al payload
            }
            message = json.dumps(data)
            topic_richiesta = "parcheggio/verifica_uscita"
            self.client.publish(topic_richiesta, message)
    
    def invia_dati_sensore(self, id_sensore, stato_occupazione, username, targa, token):
        data = {
            "id_sensore": id_sensore,
            "stato_occupazione": stato_occupazione,
            "username": username,
            "targa": targa,
            "token": token  # Aggiungi il token al payload
        }
        message = json.dumps(data)
        topic_richiesta = "parcheggio/sensore_occupazione"
        self.client.publish(topic_richiesta, message)

    def invia_multe(self, multe):
        self.waiting_for_risposta_multe = True
        
        message = {"multe": []}
        for multa in multe:
            message["multe"].append(multa)
        
        message_json = json.dumps(message) 
        topic = "parcheggio/multe"
        self.client.publish(topic, message_json)

    def invia_dati_bot(self, id_veicolo, capacita, modello, id_ricarica, tempo_ricarica, id_posto_auto):
        self.waiting_for_risposta_bot = True
        data = {
            "id_veicolo": id_veicolo,
            "capacita": capacita,
            "modello": modello,
            "id_ricarica": id_ricarica,
            "durata": tempo_ricarica,
            "id_posto_auto": id_posto_auto,
            "token": self.token  # Aggiungi il token al payload
        }
        
        message = json.dumps(data)
        topic_richiesta = "parcheggio/aggiornamento_ricarica"
        self.client.publish(topic_richiesta, message)
    
    def invia_fine_ricarica(self, idRicarica, timestamp, kw_usati, token):
        timestamp_str = timestamp.strftime("%d-%m-%Y %H:%M:%S")
        data = {
            "id_ricarica": idRicarica,
            "timestamp": timestamp_str,
            "kw_usati": kw_usati,
            "token": token  # Aggiungi il token al payload
        }

        message = json.dumps(data)
        topic_richiesta = "parcheggio/ricarica_effettuata"
        self.client.publish(topic_richiesta, message)

broker_address = "localhost"
topic_verifica_ingresso_risposta = "parcheggio/verifica_ingresso/risposta"
topic_verifica_uscita_risposta = "parcheggio/verifica_uscita/risposta"
topic_risposta_tutti_id_posto_auto = "parcheggio/richiesta_tutti_id_posto_auto/risposta"
topic_sensore_occupazione_risposta = "parcheggio/sensore_occupazione/risposta"
topic_risposta_tutte_prenotazioni_attive = "parcheggio/richiesta_uscite_automatiche/risposta"
topic_risposta_multe = "parcheggio/multe/risposta"
topic_risposta_prenotazioni_attive_auto_dentro = "parcheggio/richiesta_uscite_automatiche/risposta"
topic_risposta_ricariche = "parcheggio/richiesta_ricariche/risposta"
topic_presa_in_carico_bot = "parcheggio/presa_in_carico_bot"

centralina = Centralina(
    broker_address,
    topic_verifica_ingresso_risposta,
    topic_verifica_uscita_risposta,
    topic_risposta_tutti_id_posto_auto,
    topic_sensore_occupazione_risposta,
    topic_risposta_tutte_prenotazioni_attive,
    topic_risposta_multe,
    topic_risposta_prenotazioni_attive_auto_dentro,
    topic_risposta_ricariche,
    topic_presa_in_carico_bot,
    token=None
)

simulatore = Simulatore(centralina)

# Ottieni il token dall'istanza del simulatore
token = simulatore.get_token()

centralina.token=token

t1 = threading.Thread(target=centralina.run)
t2 = threading.Thread(target=simulatore.run)


t1.start()
t2.start()

t1.join()
t2.join()