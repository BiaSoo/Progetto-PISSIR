package entity;

import java.time.LocalDateTime;

public class Transazione {
    private int id_transazione;
    private String id_utente;
    private int id_veicolo;
    private TipoTransazione tipo_transazione;
    private double importo;
    private LocalDateTime data_transazione;

    public Transazione() {}

    public Transazione(String id_utente, int id_veicolo, TipoTransazione tipo_transazione, double importo, LocalDateTime data_transazione) {
        this.id_utente = id_utente;
        this.id_veicolo = id_veicolo;
        this.tipo_transazione = tipo_transazione;
        this.importo = importo;
        this.data_transazione = data_transazione;
    }

    public Transazione(String id_utente, TipoTransazione tipo_transazione, double importo, LocalDateTime data_transazione) {
        this.id_utente = id_utente;
        this.tipo_transazione = tipo_transazione;
        this.importo = importo;
        this.data_transazione = data_transazione;
    }

    public Transazione(TipoTransazione tipo_transazione, double importo, LocalDateTime data_transazione) {
        this.tipo_transazione = tipo_transazione;
        this.importo = importo;
        this.data_transazione = data_transazione;
    }

    public int getId_transazione() {
        return id_transazione;
    }

    public void setId_transazione(int id_transazione) {
        this.id_transazione = id_transazione;
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

    public TipoTransazione getTipo_transazione() {
        return tipo_transazione;
    }

    public void setTipo_transazione(TipoTransazione tipo_transazione) {
        this.tipo_transazione = tipo_transazione;
    }

    public double getImporto() {
        return importo;
    }

    public void setImporto(double importo) {
        this.importo = importo;
    }

    public LocalDateTime getData_transazione() {
        return data_transazione;
    }

    public void setData_transazione(LocalDateTime data_transazione) {
        this.data_transazione = data_transazione;
    }

    public enum TipoTransazione {
        premium,
        sosta,
        ricarica,
        multa,
        pagata
    }
}