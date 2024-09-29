-- Elimina le tabelle esistenti, se presenti
DROP TABLE IF EXISTS Prenotazioni CASCADE;
DROP TABLE IF EXISTS Ricariche CASCADE;
DROP TABLE IF EXISTS Costi CASCADE;
DROP TABLE IF EXISTS Transazioni CASCADE;
DROP TABLE IF EXISTS Parcheggio CASCADE;
DROP TABLE IF EXISTS CartaDiCredito CASCADE;
DROP TABLE IF EXISTS Veicoli CASCADE;
DROP TABLE IF EXISTS Utenti CASCADE;

DROP TYPE IF EXISTS TipoTransazione;
DROP TYPE IF EXISTS StatoPrenotazione;
DROP TYPE IF EXISTS TipoCarta;
DROP TYPE IF EXISTS TipoUtente;

-- Definizione dei tipi ENUM
CREATE TYPE TipoUtente AS ENUM ('Base', 'Premium', 'Admin');
CREATE TYPE StatoPrenotazione AS ENUM ('attiva', 'cancellata', 'pagata');
CREATE TYPE TipoTransazione AS ENUM ('ricarica', 'sosta', 'premium', 'multa');
CREATE TYPE TipoCarta AS ENUM ('VISA', 'MASTERCARD');

-- Creazione della tabella Utenti
CREATE TABLE Utenti (
    qrcodeId VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(128) NOT NULL,
    tipo_utente TipoUtente NOT NULL
);

-- Creazione della tabella Veicoli
CREATE TABLE Veicoli (
    id_veicolo SERIAL PRIMARY KEY,
    targa VARCHAR(20) NOT NULL,
    modello VARCHAR(100),
    id_utente VARCHAR(50),
    capacita_batteria FLOAT,
    dentroParcheggio BOOLEAN NOT NULL,
    data_ingresso TIMESTAMP,
    FOREIGN KEY (id_utente) REFERENCES Utenti(qrcodeId)
);

-- Creazione della tabella CartaDiCredito
CREATE TABLE CartaDiCredito (
    id_carta SERIAL PRIMARY KEY,
    tipo_carta TipoCarta NOT NULL,
    numero_carta VARCHAR(16) NOT NULL,
    data_scadenza DATE NOT NULL,
    cvv VARCHAR(3) NOT NULL,
    id_utente VARCHAR(50),
    FOREIGN KEY (id_utente) REFERENCES Utenti(qrcodeId)
);

-- Creazione della tabella Parcheggio
CREATE TABLE Parcheggio (
    id_posto_auto SERIAL PRIMARY KEY,
    occupato BOOLEAN NOT NULL,
    riservato BOOLEAN NOT NULL,
    id_veicolo INT,
    id_utente VARCHAR(50),
    FOREIGN KEY (id_veicolo) REFERENCES Veicoli(id_veicolo),
    FOREIGN KEY (id_utente) REFERENCES Utenti(qrcodeId)
);

-- Creazione della tabella Prenotazioni
CREATE TABLE Prenotazioni (
    id_prenotazione SERIAL PRIMARY KEY,
    id_utente VARCHAR(50),
    id_veicolo INT,
    tempo_arrivo TIMESTAMP,
    durata_permanenza INT,
    carta_credito INT NOT NULL,
    stato StatoPrenotazione NOT NULL,
    multa_pagata BOOLEAN,
    id_posto_auto INT NOT NULL,
    timestamp_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_utente) REFERENCES Utenti(qrcodeId),
    FOREIGN KEY (id_veicolo) REFERENCES Veicoli(id_veicolo),
    FOREIGN KEY (carta_credito) REFERENCES CartaDiCredito(id_carta),
    FOREIGN KEY (id_posto_auto) REFERENCES Parcheggio(id_posto_auto)
);

-- Creazione della tabella Ricariche
CREATE TABLE Ricariche (
    id_ricarica SERIAL PRIMARY KEY,
    id_veicolo INT,
    percentualeRicarica INT NOT NULL,
    durataRicarica INT NOT NULL,
    id_utente VARCHAR(50),
    effettuata BOOLEAN,
    iniziata BOOLEAN DEFAULT FALSE,
    timestamp_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_veicolo) REFERENCES Veicoli(id_veicolo),
    FOREIGN KEY (id_utente) REFERENCES Utenti(qrcodeId)
);

-- Creazione della tabella Costi
CREATE TABLE Costi (
    id_costo SERIAL PRIMARY KEY,
    costo_euro_ora FLOAT,
    costo_euro_kw FLOAT
);

-- Creazione della tabella Transazioni
CREATE TABLE Transazioni (
    id_transazione SERIAL PRIMARY KEY,
    id_utente VARCHAR(50) NOT NULL,
    id_veicolo INT,
    tipo_transazione TipoTransazione NOT NULL,
    importo FLOAT,
    timestamp_transazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_utente) REFERENCES Utenti(qrcodeId),
    FOREIGN KEY (id_veicolo) REFERENCES Veicoli(id_veicolo)
);

-- Inserimento di esempio di Utenti
INSERT INTO Utenti (qrcodeId, username, password, tipo_utente)
VALUES ('admin', 'admin', 'admin', 'Admin');

INSERT INTO Utenti (qrcodeId, username, password, tipo_utente)
VALUES ('39604f7c-fc9e-443c-b478-07cfcfe928b7', 'UtenteBase1', '@UtenteBase1', 'Base');

INSERT INTO Utenti (qrcodeId, username, password, tipo_utente)
VALUES ('716f44a5-a1e8-4578-9424-42b47535ebe2', 'UtentePremium1', '@UtentePremium1', 'Premium');

INSERT INTO Utenti (qrcodeId, username, password, tipo_utente)
VALUES ('716f44a5-a1e8-4578-9424-42b47535ebe3', 'UtentePremium2', '@UtentePremium2', 'Premium');

-- Inserimento di esempio di CartaDiCredito
INSERT INTO CartaDiCredito (tipo_carta, numero_carta, data_scadenza, cvv, id_utente)
VALUES ('VISA', '4311865193542299', '2025-10-31', '308', '39604f7c-fc9e-443c-b478-07cfcfe928b7');

INSERT INTO CartaDiCredito (tipo_carta, numero_carta, data_scadenza, cvv, id_utente)
VALUES ('MASTERCARD', '5494457461419999', '2029-12-31', '775', '716f44a5-a1e8-4578-9424-42b47535ebe2');

INSERT INTO CartaDiCredito (tipo_carta, numero_carta, data_scadenza, cvv, id_utente)
VALUES ('MASTERCARD', '5572712918922466', '2028-12-31', '769', '716f44a5-a1e8-4578-9424-42b47535ebe3');

-- Inserimento di esempio di Parcheggio
INSERT INTO Parcheggio (occupato, riservato, id_veicolo)
VALUES (false, false, NULL),
       (false, false, NULL),
       (false, true, NULL),
       (false, true, NULL);

-- Inserimento di esempio di Costi
INSERT INTO Costi (costo_euro_ora, costo_euro_kw)
VALUES (2.50, 0.89);
