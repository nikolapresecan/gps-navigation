package com.example.gpsnavigacija;

import java.util.List;

public class Korisnik {
    private String ime;
    private String email;

    public Korisnik() {}

    public Korisnik(String ime, String email) {
        this.ime = ime;
        this.email = email;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Ime: " + ime + ", Email: " + email + "\n";
    }
}
