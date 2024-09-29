package entity;

import java.util.ArrayList;
import java.util.List;

public class Coda {
    private List<Ricarica> ricariche;

    public Coda() {
        this.ricariche = new ArrayList<>();
    }

    public void addRicarica(Ricarica ricarica) {
        ricariche.add(ricarica);
    }

    public int calcolaTempoAttesaTotale() {
        int tempoAttesaTotale = 0;
        for (Ricarica ricarica : ricariche) {
            tempoAttesaTotale += ricarica.getDurataRicarica();
        }
        return tempoAttesaTotale;
    }
}
