package com.perfgames.ropelab;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Country hints assist a choice; they never select a language for the player. */
public final class CountryRules {
    private static final Set<String> ISO=new HashSet<>(Arrays.asList(Locale.getISOCountries()));
    public static String normalize(String country){
        if(country==null)return "";
        String code=country.trim().toUpperCase(Locale.ROOT);
        return ISO.contains(code)?code:"";
    }
    public static Hint choose(String network,String sim,String region){
        String code=normalize(network);if(!code.isEmpty())return new Hint(code,"network");
        code=normalize(sim);if(!code.isEmpty())return new Hint(code,"sim");
        code=normalize(region);return new Hint(code,code.isEmpty()?"none":"device");
    }
    public static final class Hint {
        public final String country,source;
        Hint(String country,String source){this.country=country;this.source=source;}
        public int suggestedLanguage(){return "IN".equals(country)?FalleyStrings.HINDI:"ID".equals(country)?FalleyStrings.INDONESIAN:FalleyStrings.ENGLISH;}
    }
    private CountryRules(){}
}
