package com.perfgames.ropelab;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import java.util.Locale;

/** Permission-free coarse hint. No GPS, IP lookup, identifier access or persistence. */
final class CountryDetector {
    static CountryRules.Hint detect(Context context){
        String network="",sim="",region="";
        try {
            TelephonyManager phone=(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            PackageManager pm=context.getPackageManager();
            boolean legacy=pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
            if(phone!=null){
                if(legacy||pm.hasSystemFeature("android.hardware.telephony.radio.access"))
                    try{network=phone.getNetworkCountryIso();}catch(SecurityException|UnsupportedOperationException unavailable){}
                if(legacy||pm.hasSystemFeature("android.hardware.telephony.subscription"))
                    try{sim=phone.getSimCountryIso();}catch(SecurityException|UnsupportedOperationException unavailable){}
            }
        }catch(RuntimeException unavailable){}
        try{region=context.getResources().getConfiguration().getLocales().get(0).getCountry();}catch(RuntimeException unavailable){region=Locale.getDefault().getCountry();}
        return CountryRules.choose(network,sim,region);
    }
    private CountryDetector(){}
}
