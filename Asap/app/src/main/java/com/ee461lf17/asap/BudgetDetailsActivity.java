package com.ee461lf17.asap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BudgetDetailsActivity extends AppCompatActivity {

    HashMap<String, HashMap<String, List<String>>> budgetDetailsPassed;
    String budgetNamePassed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_details);
        budgetDetailsPassed = (HashMap<String, HashMap<String, List<String>>>) getIntent().getSerializableExtra("Expense Details");
        budgetNamePassed = getIntent().getStringExtra("Current Budget");


        Set<String> expensesKeySet =  budgetDetailsPassed.keySet();
        for(String s: expensesKeySet){
            HashMap<String, List<String>> curExpense =  budgetDetailsPassed.get(s);
            List<String> curDetails = curExpense.get(s);
            String curBudget = curDetails.get(2);
            if(curBudget.equals(budgetNamePassed)){
                String data1 = curDetails.get(0);
                String data2 = curDetails.get(1);
                TextView textView1 = (TextView) findViewById(R.id.textView6);
                TextView textView2 = (TextView) findViewById(R.id.textView7);
                TextView textView3 = (TextView) findViewById(R.id.textView8);
                TextView textView4 = (TextView) findViewById(R.id.textView9);

                textView1.setText(s);
                textView2.setText(curDetails.get(0));
                textView3.setText(curDetails.get(1));
                textView4.setText(curDetails.get(2));
            }
        }


       View row1 = findViewById(R.id.dataRow1);
        //Send the data from the hashmap to appropriate columns here
    }


}
