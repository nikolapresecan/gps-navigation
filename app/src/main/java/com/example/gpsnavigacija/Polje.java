package com.example.gpsnavigacija;

import java.io.Serializable;
import java.util.List;

public class Polje implements Serializable {
    private String id;
    private String korisnik_id;
    private String naziv;
    private double povrsina;
    private List<Tocka> tocke;

    public Polje() {}

    public Polje(String id, String korisnik_id, String naziv, double povrsina, List<Tocka> tocke) {
        this.id = id;
        this.korisnik_id = korisnik_id;
        this.naziv = naziv;
        this.povrsina = povrsina;
        this.tocke = tocke;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKorisnik_id() {
        return korisnik_id;
    }

    public void setKorisnik_id(String korisnik_id) {
        this.korisnik_id = korisnik_id;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public double getPovrsina() {
        return povrsina;
    }

    public void setPovrsina(double povrsina) {
        this.povrsina = povrsina;
    }

    public List<Tocka> getTocke() {
        return tocke;
    }

    public void setTocke(List<Tocka> tocke) {
        this.tocke = tocke;
    }
}
