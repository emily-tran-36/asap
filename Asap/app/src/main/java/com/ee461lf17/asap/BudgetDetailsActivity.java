package com.ee461lf17.asap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.HashMap;
import java.util.List;

public class BudgetDetailsActivity extends AppCompatActivity {

    HashMap<String, HashMap<String, List<String>>> budgetDetailsPassed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_details);
        budgetDetailsPassed = (HashMap<String, HashMap<String, List<String>>>) getIntent().getSerializableExtra("Expense Details");


       View row1 = findViewById(R.id.dataRow1);
        //Send the data from the hashmap to appropriate columns here
    }


}
