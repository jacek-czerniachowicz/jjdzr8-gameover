package com.isa.control;

import com.isa.control.Coin;
import com.isa.control.CoinsList;
import com.isa.control.Data;

import java.util.*;
import java.util.stream.Collectors;

public class Favourite extends CoinsList {

    private final List<Coin> coins;

    public Favourite(List<Coin> coins, int recordsPerPage) {
       super(coins, recordsPerPage);
       this.coins = coins;

    }
    public void AddYourFavouriteToken(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Wpisz szukaną frazę");
        String string = scanner.nextLine().toUpperCase();
        List<Coin> list = Arrays.stream(Data.deserializeCoin()).collect(Collectors.toList());
        List<Coin> temporaryList = new ArrayList<>();
        list.forEach(a->{
            if (a.getSymbol().contains(string)) temporaryList.add(a);
                });

        if (!temporaryList.isEmpty()){
            temporaryList.forEach(n-> {
                System.out.println(n.getSymbol() + " - " + n.getLastPrice() + " USD");
                if(coins.stream().anyMatch(a-> a.getSymbol().equals(n.getSymbol()))) {
                    System.out.println("szukany token jest już na liście ulubione");
                }else {
                    System.out.println("czy chcesz dodać wybrane coiny do ulubionych? [Y/N]");
                    char choice = scanner.nextLine().toUpperCase().charAt(0);
                    if (choice == 'Y') {
                        coins.addAll(temporaryList);
                        Collections.sort(coins);
                        Data.serializer(coins,"favourite.json");
                    }
                }
            });

        }else System.out.println("Nie znaleziono tokenu o takiej nazwie");
    }

    public void removeTokenFromFavouriteList() {
        Scanner scanner = new Scanner(System.in);
        coins.forEach(n -> {
            System.out.println(n.getSymbol() + " - " + n.getLastPrice() + " USD");
            System.out.println("aby usunąć token wpisz 0");
            String flag = scanner.nextLine();
            if (flag.charAt(0) == 0){
                coins.remove(n);
                System.out.println("token został usuniety");
            }
        });
        Data.serializer(coins, "favourite.json");
    }

    public static List<Coin> invocationFavouriteList(){
        List<Coin> favCoinsList;
        try {
            favCoinsList = new ArrayList<>(Arrays.asList(Data.deserializeCoin("favourite.json")));
        }catch (NullPointerException e){
            e.printStackTrace();
            favCoinsList = new ArrayList<>();
        }
        return favCoinsList;
    }
}