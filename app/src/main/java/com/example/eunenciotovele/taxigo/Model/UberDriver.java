package com.example.eunenciotovele.taxigo.Model;

public class UberDriver {
    private String email, password, nome, telefone, avatarUrl, rates, carType;

    public UberDriver(){

    }

    public UberDriver(String email, String password, String nome, String telefone, String avatarUrl, String rates, String carType) {
        this.email = email;
        this.password = password;
        this.nome = nome;
        this.telefone = telefone;
        this.avatarUrl = avatarUrl;
        this.rates = rates;
        this.carType = carType;
    }

    public String getRates() {
        return rates;
    }

    public void setRates(String rates) {
        this.rates = rates;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {

        this.email = email;
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) {

        this.password = password;
    }

    public String getNome() {

        return nome;
    }

    public void setNome(String nome) {

        this.nome = nome;
    }

    public String getTelefone() {

        return telefone;
    }

    public void setTelefone(String telefone) {

        this.telefone = telefone;
    }


    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }
}
