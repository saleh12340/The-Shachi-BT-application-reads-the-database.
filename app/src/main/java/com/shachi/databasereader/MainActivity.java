package com.shachi.databasereader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PICK_DB=1001;
    LinearLayout root, content, toolbar;
    SQLiteDatabase db;
    Uri sourceUri;
    String currentTable="";
    EditText search;
    TextView status;

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    TextView tv(String s,float size, int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); t.setPadding(dp(10),dp(7),dp(10),dp(7));
        t.setTextDirection(View.TEXT_DIRECTION_RTL); return t;
    }
    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(15); b.setAllCaps(false);
        b.setMinHeight(dp(48)); return b;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b); getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildShell(); showWelcome();
    }

    void buildShell(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(248,249,251));
        toolbar=new LinearLayout(this); toolbar.setOrientation(LinearLayout.HORIZONTAL); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(6),dp(4),dp(6),dp(4)); toolbar.setBackgroundColor(Color.WHITE);
        Button importBtn=btn("استيراد قاعدة"); importBtn.setOnClickListener(v->pickDatabase());
        toolbar.addView(importBtn,new LinearLayout.LayoutParams(0,dp(52),1));
        Button shareBtn=btn("مشاركة"); shareBtn.setOnClickListener(v->shareCurrent());
        toolbar.addView(shareBtn,new LinearLayout.LayoutParams(0,dp(52),1));
        Button tablesBtn=btn("الجداول"); tablesBtn.setOnClickListener(v->showTables());
        toolbar.addView(tablesBtn,new LinearLayout.LayoutParams(0,dp(52),1));
        root.addView(toolbar);
        status=tv("لم يتم استيراد قاعدة بيانات",14,Color.DKGRAY); status.setBackgroundColor(Color.WHITE);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(42)));
        ScrollView sc=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8),dp(8),dp(8),dp(20)); sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    void showWelcome(){
        content.removeAllViews();
        TextView h=tv("قارئ قاعدة البيانات",24,Color.rgb(21,101,192)); h.setGravity(Gravity.CENTER); content.addView(h,new LinearLayout.LayoutParams(-1,dp(70)));
        TextView p=tv("استورد ملف SQLite من ملفات الجهاز، ثم استعرض الجداول والبيانات كاملة مرتبة من الأحدث إلى الأقدم، مع البحث والفلاتر والمشاركة.",17,Color.DKGRAY);
        p.setGravity(Gravity.CENTER); content.addView(p,new LinearLayout.LayoutParams(-1,dp(120)));
        Button b=btn("📂 اختيار ملف قاعدة البيانات"); b.setOnClickListener(v->pickDatabase()); content.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));
    }

    void pickDatabase(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,PICK_DB);
    }
    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d); if(r!=PICK_DB||c!=RESULT_OK||d==null) return;
        sourceUri=d.getData(); try{
            getContentResolver().takePersistableUriPermission(sourceUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }catch(Exception ignored){}
        try{
            File f=new File(getCacheDir(),"imported.db"); copyUri(sourceUri,f);
            if(db!=null) db.close();
            db=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            status.setText("تم الاستيراد: "+(sourceUri.getLastPathSegment()==null?"قاعدة البيانات":sourceUri.getLastPathSegment()));
            showTables();
        }catch(Exception e){ error("تعذر فتح قاعدة البيانات: "+e.getMessage()); }
    }
    void copyUri(Uri u,File out)throws Exception{
        InputStream in=getContentResolver().openInputStream(u); OutputStream o=new FileOutputStream(out);
        byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)o.write(buf,0,n); in.close();o.close();
    }
    ArrayList<String> tables(){
        ArrayList<String> a=new ArrayList<>(); if(db==null)return a;
        Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null);
        while(c.moveToNext())a.add(c.getString(0)); c.close(); return a;
    }
    void showTables(){
        content.removeAllViews(); if(db==null){showWelcome();return;}
        TextView h=tv("الجداول ("+tables().size()+")",21,Color.rgb(21,101,192)); h.setGravity(Gravity.RIGHT); content.addView(h);
        for(String name:tables()){
            Button b=btn(name); b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v->showTable(name)); content.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));
        }
    }
    void showTable(String table){
        currentTable=table; content.removeAllViews();
        LinearLayout head=new LinearLayout(this); head.setOrientation(LinearLayout.VERTICAL);
        TextView h=tv("جدول: "+table,21,Color.rgb(21,101,192)); head.addView(h);
        search=new EditText(this); search.setHint("بحث في جميع الأعمدة..."); search.setTextSize(16); search.setSingleLine(true);
        search.setGravity(Gravity.RIGHT); search.setPadding(dp(12),0,dp(12),0); head.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout filters=new LinearLayout(this); filters.setOrientation(LinearLayout.HORIZONTAL);
        Button all=btn("الكل"); Button recent=btn("الأحدث"); Button old=btn("الأقدم");
        filters.addView(all,new LinearLayout.LayoutParams(0,dp(48),1));filters.addView(recent,new LinearLayout.LayoutParams(0,dp(48),1));filters.addView(old,new LinearLayout.LayoutParams(0,dp(48),1));
        head.addView(filters); content.addView(head);
        LinearLayout rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); content.addView(rows);
        Runnable reload=()->loadRows(table,search.getText().toString(),rows,true);
        all.setOnClickListener(v->loadRows(table,search.getText().toString(),rows,true));
        recent.setOnClickListener(v->loadRows(table,search.getText().toString(),rows,true));
        old.setOnClickListener(v->loadRows(table,search.getText().toString(),rows,false));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){}public void onTextChanged(CharSequence s,int a,int b,int c){reload.run();}public void afterTextChanged(android.text.Editable e){}});
        reload.run();
    }

    String chooseOrder(String table, Cursor c){
        String[] preferred={"date_","now_","ID","id","max_id"};
        HashSet<String> cols=new HashSet<>(); for(int i=0;i<c.getColumnCount();i++)cols.add(c.getColumnName(i));
        for(String x:preferred)if(cols.contains(x)) return " ORDER BY "+x+" DESC ";
        return "";
    }
    void loadRows(String table,String q,LinearLayout rows,boolean newest){
        rows.removeAllViews();
        try{
            Cursor meta=db.rawQuery("SELECT * FROM \""+table.replace("\"","")+"\" LIMIT 0",null);
            String order=chooseOrder(table,meta); if(!newest && !order.isEmpty()) order=order.replace("DESC","ASC");
            String where=""; ArrayList<String> args=new ArrayList<>();
            if(q!=null&&!q.trim().isEmpty()){
                ArrayList<String> parts=new ArrayList<>();
                for(int i=0;i<meta.getColumnCount();i++){String col=meta.getColumnName(i);parts.add("CAST(\""+col.replace("\"","")+"\" AS TEXT) LIKE ?");args.add("%"+q.trim()+"%");}
                where=" WHERE "+android.text.TextUtils.join(" OR ",parts);
            } meta.close();
            String sql="SELECT * FROM \""+table.replace("\"","")+"\""+where+order;
            Cursor c=db.rawQuery(sql,args.toArray(new String[0]));
            TextView count=tv("عدد النتائج: "+c.getCount(),14,Color.GRAY); rows.addView(count);
            int shown=0;
            while(c.moveToNext()){
                LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setBackgroundColor(Color.WHITE);
                card.setPadding(dp(4),dp(4),dp(4),dp(4));
                StringBuilder s=new StringBuilder();
                for(int i=0;i<c.getColumnCount();i++){ if(i>0)s.append("\n"); s.append(c.getColumnName(i)).append(": ").append(c.isNull(i)?"":c.getString(i)); }
                TextView row=tv(s.toString(),15,Color.DKGRAY); row.setGravity(Gravity.RIGHT); card.addView(row);
                Button share=btn("مشاركة السجل"); share.setOnClickListener(v->shareText(s.toString()));
                card.addView(share,new LinearLayout.LayoutParams(-1,dp(44)));
                rows.addView(card,new LinearLayout.LayoutParams(-1,dp(0),1));
                View sep=new View(this); sep.setBackgroundColor(Color.LTGRAY); rows.addView(sep,new LinearLayout.LayoutParams(-1,dp(6)));
                if(++shown>=5000)break;
            }
            c.close();
            if(shown>=5000) rows.addView(tv("تم عرض أول 5000 سجل فقط لتفادي الضغط على الهاتف. استخدم البحث/الفلاتر.",14,Color.GRAY));
        }catch(Exception e){rows.addView(tv("خطأ في عرض الجدول: "+e.getMessage(),15,Color.RED));}
    }
    void shareCurrent(){ if(currentTable==null||currentTable.isEmpty()){shareText(status.getText().toString());return;} shareText("قاعدة البيانات\nالجدول: "+currentTable+"\nتمت مشاركة البيانات من قارئ قاعدة البيانات.");}
    void shareText(String text){
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,text);
        startActivity(Intent.createChooser(i,"مشاركة البيانات"));
    }
    void error(String e){ new AlertDialog.Builder(this).setTitle("خطأ").setMessage(e).setPositiveButton("حسنًا",null).show(); }
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
