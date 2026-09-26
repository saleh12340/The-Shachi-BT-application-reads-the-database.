package com.shachi.databasereader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PICK_DB=1001;
    LinearLayout root,content,toolbar;
    SQLiteDatabase db;
    Uri sourceUri;
    String currentCustomerId="";
    EditText search;

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,float size,int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); t.setPadding(dp(10),dp(7),dp(10),dp(7));
        t.setTextDirection(View.TEXT_DIRECTION_RTL); return t;
    }
    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(15); b.setAllCaps(false); b.setMinHeight(dp(48)); return b;
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
        Button imp=btn("استيراد"); imp.setOnClickListener(v->pickDatabase()); toolbar.addView(imp,new LinearLayout.LayoutParams(0,dp(52),1));
        Button cus=btn("العملاء"); cus.setOnClickListener(v->showCustomers()); toolbar.addView(cus,new LinearLayout.LayoutParams(0,dp(52),1));
        Button tbl=btn("الجداول"); tbl.setOnClickListener(v->showTables()); toolbar.addView(tbl,new LinearLayout.LayoutParams(0,dp(52),1));
        root.addView(toolbar);
        ScrollView sc=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8),dp(8),dp(8),dp(20)); sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    void showWelcome(){
        content.removeAllViews();
        TextView h=tv("قارئ قاعدة البيانات",24,Color.rgb(21,101,192)); h.setGravity(Gravity.CENTER); content.addView(h,new LinearLayout.LayoutParams(-1,dp(70)));
        TextView p=tv("استورد ملف SQLite ثم اختر العملاء لعرض كل عمليات كل عميل بالاعتماد على رقم ID الحقيقي داخل قاعدة البيانات.",17,Color.DKGRAY);
        p.setGravity(Gravity.CENTER); content.addView(p,new LinearLayout.LayoutParams(-1,dp(120)));
        Button b=btn("📂 اختيار قاعدة البيانات"); b.setOnClickListener(v->pickDatabase()); content.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));
    }
    void pickDatabase(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i,PICK_DB);
    }
    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d); if(r!=PICK_DB||c!=RESULT_OK||d==null)return;
        sourceUri=d.getData(); try{getContentResolver().takePersistableUriPermission(sourceUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        try{
            File f=new File(getCacheDir(),"imported.db"); copyUri(sourceUri,f);
            if(db!=null)db.close(); db=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            showCustomers();
        }catch(Exception e){error("تعذر فتح قاعدة البيانات: "+e.getMessage());}
    }
    void copyUri(Uri u,File out)throws Exception{
        InputStream in=getContentResolver().openInputStream(u); OutputStream o=new FileOutputStream(out);
        byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)o.write(buf,0,n); in.close(); o.close();
    }
    boolean table(String n){Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type IN ('table','view') AND name=?",new String[]{n});boolean x=c.moveToFirst();c.close();return x;}
    String q(String n){return "\""+n.replace("\"","")+"\"";}
    void showCustomers(){
        if(db==null){showWelcome();return;} content.removeAllViews();
        TextView h=tv("العملاء",23,Color.rgb(21,101,192)); h.setGravity(Gravity.RIGHT); content.addView(h);
        search=new EditText(this); search.setHint("بحث باسم العميل أو رقم الهاتف..."); search.setTextSize(16); search.setSingleLine(true); search.setGravity(Gravity.RIGHT);
        content.addView(search,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout filters=new LinearLayout(this); filters.setOrientation(LinearLayout.HORIZONTAL);
        Button all=btn("الكل"), debt=btn("عليه رصيد"), supplier=btn("الموردون");
        filters.addView(all,new LinearLayout.LayoutParams(0,dp(48),1));filters.addView(debt,new LinearLayout.LayoutParams(0,dp(48),1));filters.addView(supplier,new LinearLayout.LayoutParams(0,dp(48),1));
        content.addView(filters);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable load=()->loadCustomers(search.getText().toString(),"all",list);
        all.setOnClickListener(v->loadCustomers(search.getText().toString(),"all",list));
        debt.setOnClickListener(v->loadCustomers(search.getText().toString(),"debt",list));
        supplier.setOnClickListener(v->loadCustomers(search.getText().toString(),"supplier",list));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){load.run();}public void afterTextChanged(android.text.Editable e){}});
        load.run();
    }
    void loadCustomers(String term,String mode,LinearLayout list){
        list.removeAllViews();
        String sql="SELECT c.ID,c.name,c.gsm,c.cus_type_id,(SELECT COUNT(*) FROM transactions t WHERE CAST(t.cus_id AS INTEGER)=c.ID OR CAST(t.t_cus_id AS INTEGER)=c.ID) n,"+
                "(SELECT COALESCE(SUM(CASE WHEN t.t_cus_id=c.ID THEN -CAST(t.out AS REAL) ELSE CAST(t.[in] AS REAL)*CAST(t.out AS REAL) END),0) FROM transactions t WHERE CAST(t.cus_id AS INTEGER)=c.ID OR CAST(t.t_cus_id AS INTEGER)=c.ID) bal "+
                "FROM customers c WHERE 1=1";
        ArrayList<String> args=new ArrayList<>();
        if(term!=null&&!term.trim().isEmpty()){sql+=" AND (c.name LIKE ? OR c.gsm LIKE ?)";args.add("%"+term.trim()+"%");args.add("%"+term.trim()+"%");}
        if(mode.equals("supplier"))sql+=" AND c.cus_type_id=1";
        sql+=" ORDER BY c.name COLLATE NOCASE";
        Cursor c=db.rawQuery(sql,args.toArray(new String[0]));
        TextView count=tv("عدد العملاء: "+c.getCount(),14,Color.GRAY);list.addView(count);
        while(c.moveToNext()){
            int id=c.getInt(0);String name=c.getString(1)==null?"":c.getString(1).trim();String gsm=c.getString(2)==null?"":c.getString(2);
            double bal=c.isNull(5)?0:c.getDouble(5);int n=c.getInt(4);
            if(mode.equals("debt")&&bal<=0)continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackgroundColor(Color.WHITE);card.setPadding(dp(5),dp(5),dp(5),dp(5));
            TextView title=tv(name,19,Color.rgb(25,25,25));title.setText(name+"  (#"+id+")");card.addView(title,new LinearLayout.LayoutParams(-1,dp(48)));
            String balText=String.format(Locale.US,"الرصيد: %,.0f",bal);
            TextView info=tv("الهاتف: "+gsm+"    |    العمليات: "+n+"    |    "+balText,14,Color.DKGRAY);card.addView(info,new LinearLayout.LayoutParams(-1,dp(42)));
            Button open=btn("عرض عمليات العميل");open.setOnClickListener(v->showCustomer(id,name,gsm));card.addView(open,new LinearLayout.LayoutParams(-1,dp(46)));
            list.addView(card,new LinearLayout.LayoutParams(-1,dp(142))); View sep=new View(this);sep.setBackgroundColor(Color.LTGRAY);list.addView(sep,new LinearLayout.LayoutParams(-1,dp(5)));
        }c.close();
    }
    void showCustomer(int id,String name,String gsm){
        currentCustomerId=String.valueOf(id);content.removeAllViews();
        TextView h=tv(name,23,Color.rgb(21,101,192));h.setGravity(Gravity.RIGHT);content.addView(h);
        content.addView(tv("رقم العميل ID: "+id+"    الهاتف: "+gsm,14,Color.DKGRAY));
        Button share=btn("مشاركة كشف حساب العميل");share.setOnClickListener(v->shareCustomer(id,name));content.addView(share);
        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);
        Button newest=btn("الأحدث"), oldest=btn("الأقدم");filters.addView(newest,new LinearLayout.LayoutParams(0,dp(48),1));filters.addView(oldest,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(filters);
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);content.addView(rows);
        newest.setOnClickListener(v->loadCustomerOps(id,rows,true));oldest.setOnClickListener(v->loadCustomerOps(id,rows,false));loadCustomerOps(id,rows,true);
    }
    void loadCustomerOps(int id,LinearLayout rows,boolean newest){
        rows.removeAllViews();
        Cursor c=db.rawQuery("SELECT t.ID,t.cus_id,t.t_cus_id,t.[in],t.out,t.date_,t.now_,t.remarks,t.curr_id,"+
                "CASE WHEN CAST(t.t_cus_id AS INTEGER)=? THEN 1 ELSE 0 END relTarget "+
                "FROM transactions t WHERE CAST(t.cus_id AS INTEGER)=? OR CAST(t.t_cus_id AS INTEGER)=? "+
                "ORDER BY substr(t.date_,7,4)||substr(t.date_,4,2)||substr(t.date_,1,2) "+(newest?"DESC":"ASC")+", t.ID "+(newest?"DESC":"ASC"),new String[]{String.valueOf(id),String.valueOf(id),String.valueOf(id)});
        double running=0;ArrayList<String> cards=new ArrayList<>();
        while(c.moveToNext()){
            double amount=0;try{amount=c.isNull(4)?0:c.getDouble(4);}catch(Exception ignored){}
            int rel=c.getInt(9);
            double delta=rel==1?-amount:(c.getInt(3)==1?amount:-amount);
            running+=delta;
            String date=c.getString(5)==null?"":c.getString(5),time=c.getString(6)==null?"":c.getString(6);
            String rem=c.getString(7)==null?"":c.getString(7);
            String dir=delta>=0?"له":"عليه";
            String text="التاريخ: "+date+"  "+time+"\nالعملية #"+c.getInt(0)+"\nالتفاصيل: "+rem+"\nالمبلغ: "+String.format(Locale.US,"%,.0f",amount)+"\nالحركة: "+dir+"\nالرصيد التراكمي: "+String.format(Locale.US,"%,.0f",running);
            cards.add(text);
        }c.close();
        TextView total=tv("عدد العمليات: "+cards.size(),15,Color.GRAY);rows.addView(total);
        for(String text:cards){
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackgroundColor(Color.WHITE);
            TextView x=tv(text,15,Color.DKGRAY);x.setGravity(Gravity.RIGHT);card.addView(x);
            Button sh=btn("مشاركة العملية");sh.setOnClickListener(v->shareText(text));card.addView(sh,new LinearLayout.LayoutParams(-1,dp(44)));
            rows.addView(card,new LinearLayout.LayoutParams(-1,dp(0),1));View sep=new View(this);sep.setBackgroundColor(Color.LTGRAY);rows.addView(sep,new LinearLayout.LayoutParams(-1,dp(5)));
        }
    }
    void shareCustomer(int id,String name){
        Cursor c=db.rawQuery("SELECT ID,date_,now_,remarks,out,[in] FROM transactions WHERE CAST(cus_id AS INTEGER)=? OR CAST(t_cus_id AS INTEGER)=? ORDER BY ID DESC",new String[]{String.valueOf(id),String.valueOf(id)});
        StringBuilder s=new StringBuilder("كشف حساب العميل\n").append(name).append(" (#").append(id).append(")\n\n");int n=0;
        while(c.moveToNext()&&n<1000){s.append(c.getString(1)).append(" ").append(c.getString(2)).append(" | ").append(c.getString(3)).append(" | ").append(c.getString(4)).append("\n");n++;}c.close();shareText(s.toString());
    }
    void showTables(){
        content.removeAllViews();if(db==null){showWelcome();return;}TextView h=tv("الجداول",22,Color.rgb(21,101,192));content.addView(h);
        Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null);
        while(c.moveToNext()){String n=c.getString(0);Button b=btn(n);b.setGravity(Gravity.RIGHT);b.setOnClickListener(v->showTable(n));content.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));}c.close();
    }
    void showTable(String table){
        content.removeAllViews();TextView h=tv("جدول: "+table,21,Color.rgb(21,101,192));content.addView(h);
        EditText s=new EditText(this);s.setHint("بحث في جميع الأعمدة...");s.setSingleLine(true);s.setTextSize(16);s.setGravity(Gravity.RIGHT);content.addView(s,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);content.addView(rows);
        Runnable load=()->loadRows(table,s.getText().toString(),rows);s.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence a,int b,int c,int d){}public void onTextChanged(CharSequence a,int b,int c,int d){load.run();}public void afterTextChanged(android.text.Editable e){}});load.run();
    }
    void loadRows(String table,String term,LinearLayout rows){
        rows.removeAllViews();String qt=q(table);Cursor meta=db.rawQuery("SELECT * FROM "+qt+" LIMIT 0",null);
        ArrayList<String> cols=new ArrayList<>();for(int i=0;i<meta.getColumnCount();i++)cols.add(meta.getColumnName(i));meta.close();
        String where="";ArrayList<String> args=new ArrayList<>();if(!term.trim().isEmpty()){ArrayList<String> p=new ArrayList<>();for(String col:cols){p.add("CAST("+q(col)+" AS TEXT) LIKE ?");args.add("%"+term.trim()+"%");}where=" WHERE "+android.text.TextUtils.join(" OR ",p);}
        String order=cols.contains("date_")?" ORDER BY "+q("date_")+" DESC":(cols.contains("ID")?" ORDER BY "+q("ID")+" DESC":(cols.contains("id")?" ORDER BY "+q("id")+" DESC":""));
        Cursor c=db.rawQuery("SELECT * FROM "+qt+where+order,args.toArray(new String[0]));rows.addView(tv("عدد النتائج: "+c.getCount(),14,Color.GRAY));int shown=0;
        while(c.moveToNext()&&shown<5000){StringBuilder s=new StringBuilder();for(int i=0;i<c.getColumnCount();i++){if(i>0)s.append("\n");s.append(c.getColumnName(i)).append(": ").append(c.isNull(i)?"":c.getString(i));}
            TextView x=tv(s.toString(),15,Color.DKGRAY);x.setBackgroundColor(Color.WHITE);rows.addView(x);Button sh=btn("مشاركة السجل");final String text=s.toString();sh.setOnClickListener(v->shareText(text));rows.addView(sh,new LinearLayout.LayoutParams(-1,dp(44)));shown++;}
        c.close();
    }
    void shareText(String text){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,text);startActivity(Intent.createChooser(i,"مشاركة البيانات"));}
    void error(String e){new AlertDialog.Builder(this).setTitle("خطأ").setMessage(e).setPositiveButton("حسنًا",null).show();}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
