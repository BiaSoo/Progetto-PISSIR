package entity;

import java.time.LocalDateTime;

public class Ricarica {
  private int idRicarica;
  private int idVeicolo;
  private int percentualeRicarica;
  private int durataRicarica;
  private String id_utente;
  private LocalDateTime timestampCreazione;
  private boolean effettuata;
  private int id_posto_auto;

  public Ricarica(String id_utente, int idVeicolo, int percentualeRicarica, int durataRicarica, LocalDateTime timestampCreazione) {
    this.id_utente = id_utente;
    this.idVeicolo = idVeicolo;
    this.percentualeRicarica = percentualeRicarica;
    this.durataRicarica = durataRicarica;
    this.timestampCreazione = timestampCreazione;
  }

  public Ricarica(String id_utente, int durataRicarica, LocalDateTime timestampCreazione) {
    this.id_utente = id_utente;
    this.durataRicarica = durataRicarica;
    this.timestampCreazione = timestampCreazione;
  }

  // Costruttore completo per getAllRicariche
  public Ricarica(int idRicarica, int idVeicolo, int percentualeRicarica, int durataRicarica, String id_utente, LocalDateTime timestampCreazione, boolean effettuata) {
    this.idRicarica = idRicarica;
    this.idVeicolo = idVeicolo;
    this.percentualeRicarica = percentualeRicarica;
    this.durataRicarica = durataRicarica;
    this.id_utente = id_utente;
    this.timestampCreazione = timestampCreazione;
    this.effettuata = effettuata;
  }

  // con id_posto_auto
  public Ricarica(int idRicarica, int idVeicolo, int percentualeRicarica, int durataRicarica, String id_utente, LocalDateTime timestampCreazione, boolean effettuata, int idPostoAuto) {
    this.idRicarica = idRicarica;
    this.idVeicolo = idVeicolo;
    this.percentualeRicarica = percentualeRicarica;
    this.durataRicarica = durataRicarica;
    this.id_utente = id_utente;
    this.timestampCreazione = timestampCreazione;
    this.effettuata = effettuata;
    this.id_posto_auto = idPostoAuto;
  }

  public Ricarica() {

  }

   // Getter e Setter
   public int getPercentualeRicarica() {
    return percentualeRicarica;
  }

  public void setPercentualeRicarica(int percentualeRicarica) {
    this.percentualeRicarica = percentualeRicarica;
  }  

  public int getIdRicarica() {
    return idRicarica;
  }

  public void setIdRicarica(int idRicarica) {
    this.idRicarica = idRicarica;
  }

  public int getIdVeicolo() {
    return idVeicolo;
  }

  public void setIdVeicolo(int idVeicolo) {
    this.idVeicolo = idVeicolo;
  }

  public int getDurataRicarica() {
    return durataRicarica;
  }

  public void setDurataRicarica(int durataRicarica) {
    this.durataRicarica = durataRicarica;
  }

  public LocalDateTime getTimestampCreazione() {
    return timestampCreazione;
  }

  public void setTimestampCreazione(LocalDateTime timestampCreazione) {
    this.timestampCreazione = timestampCreazione;
  }

  public String getId_utente() {
    return id_utente;
  }

  public void setId_utente(String id_utente) {
    this.id_utente = id_utente;
  }

  public boolean isEffettuata() {
    return effettuata;
  }

  public void setEffettuata(boolean effettuata) {
    this.effettuata = effettuata;
  }

  public int getIdPostoAuto() {
    return id_posto_auto;
  }

  public void setIdPostoAuto(int idPostoAuto) {
    this.id_posto_auto = idPostoAuto;
  }
}
