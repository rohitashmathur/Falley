package com.perfgames.ropelab;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

final class DailyShare {
    static File create(Activity activity,Physics run,long day,int controls,int avatar,int language)throws IOException {
        Bitmap bitmap=Bitmap.createBitmap(1080,1350,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawColor(FalleyUi.BG);p.setColor(FalleyUi.PANEL);c.drawRoundRect(64,64,1016,1286,40,40,p);
        label(c,p,"FALLEY",540,190,100,FalleyUi.TEXT);
        label(c,p,tr(language,"DAILY DROP"),540,268,40,FalleyUi.MINT);
        label(c,p,DailyDrop.courseId(day),540,335,30,FalleyUi.MUTED);
        AvatarRenderer.draw(c,540,540,80,-100,0,avatar,0);
        label(c,p,tr(language,run.won?"LANDED!":"FALL ENDED"),540,730,66,run.won?FalleyUi.MINT:0xffff816c);
        label(c,p,tr(language,"SCORE %d",run.score),540,830,54,0xfff6d477);
        label(c,p,String.format(Locale.US,"%.1fs  /  %d",run.elapsed,run.jumpsUsed())+" "+tr(language,"JUMPS"),540,910,34,FalleyUi.TEXT);
        label(c,p,tr(language,controls==2?"MIXED CONTROLS":controls==1?"MOTION":"TOUCH"),540,980,32,FalleyUi.MUTED);
        label(c,p,tr(language,"NO REVIVES"),540,1040,28,FalleyUi.MUTED);
        label(c,p,tr(language,"One course. A fresh drop every day."),540,1170,31,FalleyUi.MINT);
        File dir=new File(activity.getCacheDir(),"falley-share");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Share directory");
        File output=new File(dir,"result.png");try(FileOutputStream stream=new FileOutputStream(output)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IOException("Share image");}finally{bitmap.recycle();}
        return output;
    }
    static void send(Activity activity,Physics run,long day,int controls,int avatar,int language) {
        try {
            create(activity,run,day,controls,avatar,language);
            Uri uri=Uri.parse("content://"+activity.getPackageName()+".share/result.png");
            Intent intent=new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("Falley Daily Drop",uri));
            activity.startActivity(Intent.createChooser(intent,tr(language,"SHARE RESULT")));
        }catch(IOException|ActivityNotFoundException|SecurityException failure){Toast.makeText(activity,tr(language,"Unable to share this result."),Toast.LENGTH_LONG).show();}
    }
    private static String tr(int language,String value,Object...args){return ExpansionStrings.text(language,value,args);}
    private static void label(Canvas c,Paint p,String value,float x,float y,float size,int color){
        p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(size);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);
        if(p.measureText(value)>880)p.setTextSize(size*880/p.measureText(value));c.drawText(value,x,y,p);
    }
    private DailyShare(){}
}
