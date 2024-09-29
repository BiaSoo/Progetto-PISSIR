package entity;

import java.time.LocalDateTime;

public class Prenotazione {

    private static final int TOLLERANZA_MINUTI = 15;

    private int id_prenotazione;
    private String id_utente;
    private int id_veicolo;
    private int id_posto_auto;
    private LocalDateTime tempo_arrivo;
    private long durata_permanenza;
    private int carta_credito;
    private boolean multa_pagata;
    private StatoPrenotazione stato;
    private LocalDateTime timestamp_creazione;
    private LocalDateTime arrivo_effettivo;
    private LocalDateTime uscita_effettiva;

    public Prenotazione(String id_utente, int id_veicolo, int id_posto_auto, LocalDateTime tempo_arrivo, long durata_permanenza, int carta_credito, StatoPrenotazione stato, LocalDateTime timestamp_creazione) {
        this.id_utente = id_utente;
        this.id_veicolo = id_veicolo;
        this.id_posto_auto = id_posto_auto;
        this.tempo_arrivo = tempo_arrivo;
        this.durata_permanenza = durata_permanenza;
        this.carta_credito = carta_credito;
        this.stato = stato;
        this.timestamp_creazione = timestamp_creazione;
    }

    public Prenotazione(int id_prenotazione, String id_utente, int id_veicolo, int id_posto_auto, LocalDateTime tempo_arrivo, long durata_permanenza, int carta_credito, StatoPrenotazione stato) {
        this.id_prenotazione = id_prenotazione;
        this.id_utente = id_utente;
        this.id_veicolo = id_veicolo;
        this.id_posto_auto = id_posto_auto;
        this.tempo_arrivo = tempo_arrivo;
        this.durata_permanenza = durata_permanenza;
        this.carta_credito = carta_credito;
        this.stato = stato;
    }

    // Getter e setter

    public Prenotazione() {
    }

    public Prenotazione(int id_prenotazione, int idPostoAuto, LocalDateTime tempoArrivo, int durataPermanenza, LocalDateTime tempoUscita,
    LocalDateTime timestampCreazione) {
        this.id_prenotazione = id_prenotazione;
        this.id_posto_auto = idPostoAuto;
        this.tempo_arrivo = tempoArrivo;
        this.durata_permanenza = durataPermanenza;
        this.uscita_effettiva = tempoUscita;
        this.timestamp_creazione = timestampCreazione;
    }

    public Prenotazione(int idPostoAuto, LocalDateTime tempoArrivo, int durataPermanenza, LocalDateTime tempoUscita,
        LocalDateTime timestampCreazione) {
        this.id_posto_auto = idPostoAuto;
        this.tempo_arrivo = tempoArrivo;
        this.durata_permanenza = durataPermanenza;
        this.uscita_effettiva = tempoUscita;
        this.timestamp_creazione = timestampCreazione;
    }

    public int getId_prenotazione() {
        return id_prenotazione;
    }

    public void setId_prenotazione(int id_prenotazione) {
        this.id_prenotazione = id_prenotazione;
    }

    public String getId_utente() {
        return id_utente;
    }

    public void setId_utente(String id_utente) {
        this.id_utente = id_utente;
    }

    public int getId_veicolo() {
        return id_veicolo;
    }

    public void setId_veicolo(int id_veicolo) {
        this.id_veicolo = id_veicolo;
    }

    public int getId_posto_auto() {
        return id_posto_auto;
    }

    public void setId_posto_auto(int id_posto_auto) {
        this.id_posto_auto = id_posto_auto;
    }

    public LocalDateTime getTempo_arrivo() {
        return tempo_arrivo;
    }

    public void setTempo_arrivo(LocalDateTime tempo_arrivo) {
        this.tempo_arrivo = tempo_arrivo;
    }

    public long getDurata_permanenza() {
        return durata_permanenza;
    }

    public void setDurata_permanenza(long durata_permanenza) {
        this.durata_permanenza = durata_permanenza;
    }

    public int getCarta_credito() {
        return carta_credito;
    }

    public void setCarta_credito(int carta_credito) {
        this.carta_credito = carta_credito;
    }

    public boolean isMulta_pagata() {
        return multa_pagata;
    }

    public void setMulta_pagata(boolean multa_pagata) {
        this.multa_pagata = multa_pagata;
    }

    public StatoPrenotazione getStato() {
        return stato;
    }

    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
    }

    public LocalDateTime getTempoUscita() {
        return uscita_effettiva;
    }

    public void setTempoUscita(LocalDateTime tempoUscita) {
        this.uscita_effettiva = tempoUscita;
    }

    public LocalDateTime getTimestamp_creazione() {
        return timestamp_creazione;
    }

    public void setTimestamp_creazione(LocalDateTime timestamp_creazione) {
        this.timestamp_creazione = timestamp_creazione;
    }

    public enum StatoPrenotazione {
        attiva,
        cancellata,
        pagata
    }

    public boolean isPresentato() {
        return arrivo_effettivo != null && !arrivo_effettivo.isAfter(tempo_arrivo);
    }

    public boolean isLeftWithinDuration() {
        if (uscita_effettiva == null) {
            return false;
        }
        LocalDateTime tempo_partenza = tempo_arrivo.plusMinutes(durata_permanenza);
        LocalDateTime tempo_massimo_partenza = tempo_partenza.plusMinutes(30 + TOLLERANZA_MINUTI);
        return !uscita_effettiva.isAfter(tempo_massimo_partenza);
    }

}