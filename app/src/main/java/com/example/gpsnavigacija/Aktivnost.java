package com.example.gpsnavigacija;

import com.google.firebase.database.PropertyName;

import java.util.List;

public class Aktivnost {
    private String datum;
    private String id_korisnika;
    private String id_polja;
    private String id_sirine_grana;
    private String id_tipa_obrade;
    private int obradjena_povrsina;
    private List<Tocka> tocke_obrade;

    public Aktivnost() {}

    public Aktivnost(String datum, String id_korisnika, String id_polja, String id_sirine_grana, String id_tipa_obrade, int obradjena_povrsina, List<Tocka> tocke_obrade) {
        this.datum = datum;
        this.id_korisnika = id_korisnika;
        this.id_polja = id_polja;
        this.id_sirine_grana = id_sirine_grana;
        this.id_tipa_obrade = id_tipa_obrade;
        this.obradjena_povrsina = obradjena_povrsina;
        this.tocke_obrade = tocke_obrade;
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public String getId_korisnika() {
        return id_korisnika;
    }

    public void setId_korisnika(String id_korisnika) {
        this.id_korisnika = id_korisnika;
    }

    public String getId_polja() {
        return id_polja;
    }

    public void setId_polja(String id_polja) {
        this.id_polja = id_polja;
    }

    public String getId_sirine_grana() {
        return id_sirine_grana;
    }

    public void setId_sirine_grana(String id_sirine_grana) {
        this.id_sirine_grana = id_sirine_grana;
    }

    public String getId_tipa_obrade() {
        return id_tipa_obrade;
    }

    public void setId_tipa_obrade(String id_tipa_obrade) {
        this.id_tipa_obrade = id_tipa_obrade;
    }

    public int getObradjena_povrsina() {
        return obradjena_povrsina;
    }

    public void setObradjena_povrsina(int obradjena_povrsina) {
        this.obradjena_povrsina = obradjena_povrsina;
    }

    public List<Tocka> getTocke_obrade() {
        return tocke_obrade;
    }

    public void setTocke_obrade(List<Tocka> tocke_obrade) {
        this.tocke_obrade = tocke_obrade;
    }
}