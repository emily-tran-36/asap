package com.ee461lf17.asap;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AccountDetailsActivity extends AppCompatActivity {


    HashMap<String, HashMap<String, List<String>>> transMap;
    HashMap<String, Integer> amountRemaining;
    String accountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        amountRemaining = (HashMap<String, Integer>) getIntent().getSerializableExtra("Amount Remaining");
        transMap = (HashMap<String, HashMap<String, List<String>>>) getIntent().getSerializableExtra("Transaction Map");
        accountName = getIntent().getStringExtra("Account Name");


        TableLayout tableLayout = (TableLayout) findViewById(R.id.accountDetailTable);
        boolean greyEntry = false;

        Set<String> expensesKeySet =  transMap.keySet();
        for(String s: expensesKeySet){
            if (s.equals(accountName)){
                int index = 0;
                for(int i = 0; i<(transMap.get(s).get(s).size())/2; i++){
                    HashMap<String, List<String>> curAccount =  transMap.get(s);
                    List<String> curTrans = curAccount.get(s);
                    String curSourceName = curTrans.get(index);
                    String curTransAmount = curTrans.get(index+1);
                    Integer amountLeft = amountRemaining.get(s);

                    TextView textView = new TextView(this);
                    TextView textView1 = new TextView(this);
                    TextView textView2 = new TextView(this);

                    TableRow tableRow = new TableRow(this);

                    if(greyEntry){
                        greyEntry = false;
                        tableRow.setBackgroundColor(Color.parseColor("#E8EDEF"));
                    }
                    else{
                        greyEntry = true;
                    }
                    float density = this.getResources().getDisplayMetrics().density;
                    tableRow.setPadding((int)density*5, (int)density*5, (int)density*5, (int)density*5);
                    tableRow.setGravity(Gravity.LEFT);
                    tableRow.setLayoutParams(new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

                    textView.setText(curSourceName);
                    textView1.setText(curTransAmount);
                    if(i+1 == (transMap.get(s).get(s).size()/2)){
                        textView2.setText(amountLeft.toString());
                    }

                    textView.setGravity(Gravity.LEFT);
                    textView1.setGravity(Gravity.LEFT);
                    textView2.setGravity(Gravity.LEFT);

                    textView.setTextSize(16);
                    textView1.setTextSize(16);
                    textView2.setTextSize(16);

                    textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));


                    tableRow.addView(textView);
                    tableRow.addView(textView1);
                    tableRow.addView(textView2);

                    tableLayout.addView(tableRow);

                    index +=2;
                }


            }

        }


    }
}
