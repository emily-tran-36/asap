package com.ee461lf17.asap;


import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    GoogleAccountCredential mCredential;
    Budgets budgetManager;
    private TextView mOutputText;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private Button mCallApiButton;
    ProgressDialog mProgress;

    //permissions required for Google Sheets access
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String BUTTON_TEXT = "Call Drive API";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private DrawerLayout mDrawer;
    private NavigationView nvDrawer;

    //dashboard variables
    static PieChart pieChart ;
    static PieDataSet pieDataSet ;
    static PieData pieData ;
    static ArrayList<Entry> entries ;
    static ArrayList<String> PieEntryLabels ;



    static ArrayList<String> budgetList = new ArrayList<String>();
    static ArrayList<String> categoriesList = new ArrayList<String>();
    static ArrayList<String> accountsList = new ArrayList<String>();

    static HashMap<String, String> budgetAmountMap = new HashMap<String, String>();
    static HashMap<String, String> budgetAccountMap = new HashMap<String, String>();

    static HashMap<String,  HashMap<String,List<String>>> accountTransactionsMap = new HashMap<String,  HashMap<String,List<String>>>();
    static HashMap<String, Integer> accountAmountRemaining = new HashMap<String, Integer>();

    //static HashMap<String, List<String>> expenseMap = new HashMap<String, List<String>>();


    static HashMap<String, HashMap<String, List<String>>> budgetExpenseMap = new HashMap<String, HashMap<String, List<String>>>();

    private static String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        promptUserToChooseAccount();
        budgetManager = new Budgets(mCredential, this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Set up sidenav drawer
        mDrawer = findViewById(R.id.drawer_layout);
        nvDrawer = (NavigationView) findViewById(R.id.navigation);
        setupDrawerContent(nvDrawer);

        // Set up toggle for drawer layout
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.side_nav_open, R.string.side_nav_close);
        mDrawer.setDrawerListener(toggle);
        toggle.syncState();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        categoriesList.add("Rent & Utilities");
        categoriesList.add("Food");
        categoriesList.add("Groceries");
        categoriesList.add("Shopping");
        categoriesList.add("Entertainment");
        categoriesList.add("Investment");
        categoriesList.add("Other");

        final Activity mainActivity = this;

        final com.github.clans.fab.FloatingActionButton sync = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabRefresh);
        sync.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //syncbutton
            }});

        final com.github.clans.fab.FloatingActionButton account = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabAccount);
        account.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.activity_main,null);
                View popupLayout = inflater.inflate(R.layout.popup_new_account,null);
                float density = MainActivity.this.getResources().getDisplayMetrics().density;
                final PopupWindow popup = new PopupWindow(popupLayout, (int)density*240, (int)density*240, true);
                //PopupWindow popup = new PopupWindow((int)density*240, (int)density*285);
                popup.showAtLocation(layout, Gravity.CENTER, 0, 0);

                final EditText Name = (EditText) popupLayout.findViewById(R.id.newAccountName);
                final EditText Amount = (EditText) popupLayout.findViewById(R.id.newAccountAmount);


                ((Button) popupLayout.findViewById(R.id.confirm_new_account))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {

                                if(accountsList.contains("No accounts yet.")){
                                    accountsList.remove("No accounts yet.");
                                }

                                budgetManager.addNewAccount(mainActivity, Name.getText().toString(), Amount.getText().toString());

                                accountsList.add(Name.getText().toString());
                                //HashMap<String,List<String>> curAccountTrans = accountTransactionsMap.get(Name.getText().toString());
                                //List<String> curAccountTranList = curAccountTrans.get(Name.getText().toString());
                               HashMap<String,List<String>> curAccountTrans = new  HashMap<String,List<String>>();
                                List<String> curAccountTranList = new ArrayList<String>();

                                curAccountTranList.add("Create account");
                                curAccountTranList.add(Amount.getText().toString());

                                curAccountTrans.put(Name.getText().toString(),curAccountTranList);

                                accountTransactionsMap.put(Name.getText().toString(), curAccountTrans);

                                accountAmountRemaining.put(Name.getText().toString(),Integer.parseInt(Amount.getText().toString()));

                                popup.dismiss();

                            }

                        });

                ((Button) popupLayout.findViewById(R.id.cancel_new_account))
                        .setOnClickListener(new View.OnClickListener() {
                            public void onClick(View arg0) {
                                popup.dismiss();
                            }
                        });
            };
        });



        com.github.clans.fab.FloatingActionButton budget = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabBudget);
        budget.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.activity_main,null);
                View popupLayout = inflater.inflate(R.layout.popup_new_budget,null);
                float density = MainActivity.this.getResources().getDisplayMetrics().density;
                final PopupWindow popup = new PopupWindow(popupLayout, (int)density*300, (int)density*380, true);
                //PopupWindow popup = new PopupWindow((int)density*240, (int)density*285);
                popup.showAtLocation(layout, Gravity.CENTER, 0, 0);

                final Spinner accounts = popupLayout.findViewById(R.id.spinner4);
                String[] accNames = accountsList.toArray(new String[0]);
                ArrayAdapter<String> accAdapter = new ArrayAdapter<String>(MainActivity.this,
                        R.layout.spinner_item, accNames);
                accAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                accounts.setAdapter(accAdapter);

                final EditText Name = (EditText) popupLayout.findViewById(R.id.newBudgetName);
                final EditText Amount = (EditText) popupLayout.findViewById(R.id.newBudgetAmount);
                final EditText Emails = (EditText) popupLayout.findViewById(R.id.newEmails);


                ((Button) popupLayout.findViewById(R.id.confirm_new_budget))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {
                                if(budgetList.contains("No budgets yet.")){
                                    budgetList.remove("No budgets yet.");
                                }

                                final String accountsText = accounts.getSelectedItem().toString();

                                // Create new budget Name
                                budgetManager.addNewBudget(mainActivity, Name.getText().toString(), Amount.getText().toString(), accountsText);
                                String[] emails = Emails.getText().toString().split(",");
                                for(String s: emails) {
                                    s = s.trim();
                                    if (s == "") {
                                        continue;
                                    }
                                    budgetManager.addUserToBudget(mainActivity, Name.getText().toString(), s);
                                }

                                budgetList.add(Name.getText().toString());

                                budgetAccountMap.put(Name.getText().toString(), accountsText);
                                budgetAmountMap.put(Name.getText().toString(), Amount.getText().toString());
                                popup.dismiss();


                            }

                        });

                ((Button) popupLayout.findViewById(R.id.cancel_new_budget))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {
                                popup.dismiss();
                            }
                        });
            }
        });


        com.github.clans.fab.FloatingActionButton expense = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabExpense);
        expense.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.activity_main,null);
                View popupLayout = inflater.inflate(R.layout.popup_new_expense,null);
                float density = MainActivity.this.getResources().getDisplayMetrics().density;
                final PopupWindow popup = new PopupWindow(popupLayout, (int)density*300, (int)density*380, true);
                //PopupWindow popup = new PopupWindow((int)density*240, (int)density*285);
                popup.showAtLocation(layout, Gravity.CENTER, 0, 0);

                final Spinner categories = (Spinner) popupLayout.findViewById(R.id.categoryspinner);
                String[] catNames = categoriesList.toArray(new String[0]);
                ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(MainActivity.this,
                        R.layout.spinner_item, catNames);
                catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categories.setAdapter(catAdapter);


                final Spinner Budget = (Spinner) popupLayout.findViewById(R.id.budgetspinner);
                String[] budgetNames = budgetList.toArray(new String[0]);
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this,
                R.layout.spinner_item, budgetNames);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                Budget.setAdapter(dataAdapter);

                final EditText Name = (EditText) popupLayout.findViewById(R.id.newExpenseName);
                final EditText Amount = (EditText) popupLayout.findViewById(R.id.newExpenseAmount);
                final Spinner Category = (Spinner) popupLayout.findViewById(R.id.categoryspinner);

                ((Button) popupLayout.findViewById(R.id.confirm_new_expense))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {
                                //budgetList.add(Name.getText().toString());

                                final String categoryText = Category.getSelectedItem().toString();
                                final String budgetText = Budget.getSelectedItem().toString();

                                budgetManager.addExpenditure(mainActivity,budgetText,categoryText,
                                        Double.parseDouble(Amount.getText().toString()),
                                        Name.getText().toString(), "12/4/2017");

                                ArrayList<String> tempList = new ArrayList<String>();
                                tempList.add(Amount.getText().toString());
                                tempList.add(categoryText);
                                tempList.add(budgetText);

                                HashMap<String, List<String>> expenseMap = new HashMap<String, List<String>>();
                                expenseMap.put(Name.getText().toString(),tempList);

                                budgetExpenseMap.put(Name.getText().toString(),expenseMap);
                                popup.dismiss();

                            }

                        });

                ((Button) popupLayout.findViewById(R.id.cancel_new_expense))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {
                                popup.dismiss();

                            }

                        });
            }
        });

        com.github.clans.fab.FloatingActionButton income = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabIncome);
        income.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.activity_main,null);
                View popupLayout = inflater.inflate(R.layout.popup_new_income,null);
                float density = MainActivity.this.getResources().getDisplayMetrics().density;
                final PopupWindow popup = new PopupWindow(popupLayout, (int)density*300, (int)density*310, true);
                //PopupWindow popup = new PopupWindow((int)density*240, (int)density*285);
                popup.showAtLocation(layout, Gravity.CENTER, 0, 0);

                final Spinner accounts = (Spinner) popupLayout.findViewById(R.id.spinner3);
                String[] accNames = accountsList.toArray(new String[0]);
                ArrayAdapter<String> accAdapter = new ArrayAdapter<String>(MainActivity.this,
                        R.layout.spinner_item, accNames);
                accAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                accounts.setAdapter(accAdapter);


                final EditText Name = (EditText) popupLayout.findViewById(R.id.newIncomeName);
                final EditText Amount = (EditText) popupLayout.findViewById(R.id.newIncomeAmount);

                ((Button) popupLayout.findViewById(R.id.confirm_new_income))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {

                                String accountsText = accounts.getSelectedItem().toString();

                                budgetManager.addMoneyToAccount(mainActivity, accountsText, Double.parseDouble(Amount.getText().toString()), Name.getText().toString(), "'12/4/2017");

                                HashMap<String, List<String>> curAccountTrans = accountTransactionsMap.get(accountsText);
                                List<String> curAccountTranList = curAccountTrans.get(accountsText);
                                curAccountTranList.add("Income: " + Name.getText().toString());
                                curAccountTranList.add(Amount.getText().toString());

                                curAccountTrans.put(accountsText, curAccountTranList);
                                accountTransactionsMap.put(accountsText, curAccountTrans);

                                int currentAmountRemaining = accountAmountRemaining.get(accountsText);
                                currentAmountRemaining += Integer.parseInt(Amount.getText().toString());
                                accountAmountRemaining.put(accountsText,currentAmountRemaining);

                                popup.dismiss();

                            }

                        });

                ((Button) popupLayout.findViewById(R.id.cancel_new_income))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {
                                popup.dismiss();

                            }

                        });
//            public void onClick(View view) {
//                Snackbar.make(view, "Running getResultsFromApi", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//                getResultsFromApi();
            }
        });


    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }

                });
    }
     // Use this function to take action on keypress
    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.sync_budgets:
                // Sync the budgets by pulling/pushing changes
                Snackbar.make(nvDrawer, "Sync budget selected", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                break;
            case R.id.select_budget:
                // Trigger event to select a different budget, maybe use a dropdown?
                Snackbar.make(nvDrawer, "Select budget selected", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                break;
            case R.id.user:
                // View user profile, maybe switch account
                Snackbar.make(nvDrawer, "User selected", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                break;
        }
        // Do anything else, maybe close drawer
        //mDrawer.closeDrawers();
        // Initialize credentials and service object.
    }
    //Will ensure google play services are installed and up-to-date
    //Then prompts the user to log into their google account and stores their credentials
    private void promptUserToChooseAccount() {
        //Update Google Play Services
        if(! isGooglePlayServicesAvailable()){
            acquireGooglePlayServices();
        }
        //Choose user account
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
        while(!EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)){}
        String accountName = getPreferences(Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
        } else {
            // Start a dialog from which the user can choose an account
            startActivityForResult(
                    mCredential.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
        }
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            int request = 0;
            String oldFileID = "test";
            String newFileName = "test1";
            switch (request) {
                case 0:
                    MakeRequestTask debug = new MakeRequestTask(mCredential);
                    Boolean flag = debug.isDriveServiceNull();
                    debug.execute();
                    break;
                case 1:

                    break;
                case 2:


                    break;
                case 3:
                    //do not call for now
                    break;
                case 4:
                    //do not call for now
                    break;
                case 5:
                    //do not call for now
                    break;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPermissionsGranted(int i, List<String> list) {

    }

    @Override
    public void onPermissionsDenied(int i, List<String> list) {

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            View chartView = inflater.inflate(R.layout.piechart_main, container, false);
            //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            //textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));

            int viewNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            if(viewNumber == 1){
                //Budget List
                String[] budgets = new String[] { "No budgets yet."};
//                Set<String> budgets = budgetMap.keySet();
//                for(String s: budgets){
//                    budgetList.add(s);
//                }
                if (budgetList.isEmpty()) {
                    budgetList.addAll( Arrays.asList(budgets) );
                }

                // Create ArrayAdapter using the budget list.
                ListAdapter budgetAdapter = new ArrayAdapter<String>(getActivity(), R.layout.simplerow, budgetList);
                final ListView mainListView = (ListView) rootView.findViewById( R.id.mainListView );
                mainListView.setAdapter( budgetAdapter );

                //click on list item
                mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                            Intent myIntent = new Intent(view.getContext(), BudgetDetailsActivity.class);
                            String  budgetName    = (String) mainListView.getItemAtPosition(position);

                            myIntent.putExtra("Expense Details", budgetExpenseMap);
                            myIntent.putExtra("Current Budget", budgetName);

                            startActivity(myIntent);
                    }
                });
                return rootView;

            }
            else if(viewNumber == 2){
                String[] accounts = new String[] { "No accounts yet."};
                if (accountsList.isEmpty()) {
                    accountsList.addAll( Arrays.asList(accounts) );
                }

                // Create ArrayAdapter using the budget list.
                ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), R.layout.simplerow, accountsList);
                final ListView mainListView = (ListView) rootView.findViewById( R.id.mainListView );
                mainListView.setAdapter( listAdapter );

                //click on list item
                mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {

                            Intent myIntent = new Intent(view.getContext(), AccountDetailsActivity.class);
                            String  accountName    = (String) mainListView.getItemAtPosition(position);

                            myIntent.putExtra("Amount Remaining", accountAmountRemaining);
                            myIntent.putExtra("Transaction Map", accountTransactionsMap);
                            myIntent.putExtra("Account Name", accountName);


                        startActivity(myIntent);

                    }
                });

                return rootView;

            }
            else {

                pieChart = (PieChart) chartView.findViewById(R.id.chart1);
                entries = new ArrayList<>();

                PieEntryLabels = new ArrayList<String>();

                AddValuesToPIEENTRY();

                AddValuesToPieEntryLabels();

                pieDataSet = new PieDataSet(entries, "");

                pieData = new PieData(PieEntryLabels, pieDataSet);

                pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);

                pieChart.setData(pieData);

                pieChart.animateY(3000);
                return chartView;
            }
        }
    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "BUDGETS";
                case 1:
                    return "ACCOUNTS";
                case 2:
                    return "DASHBOARD";
            }
            return null;
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    static public void AddValuesToPIEENTRY(){

        entries.add(new BarEntry(2f, 0));
        entries.add(new BarEntry(4f, 1));
        entries.add(new BarEntry(6f, 2));
        entries.add(new BarEntry(8f, 3));
        entries.add(new BarEntry(7f, 4));
        entries.add(new BarEntry(3f, 5));

    }

    static public void AddValuesToPieEntryLabels(){

        PieEntryLabels.add("January");
        PieEntryLabels.add("February");
        PieEntryLabels.add("March");
        PieEntryLabels.add("April");
        PieEntryLabels.add("May");
        PieEntryLabels.add("June");

    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * An asynchronous task that handles the Drive API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;

        public boolean isDriveServiceNull() {
            return mService == null;
        }

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         * @return List of Strings describing files, or an empty list if no files
         *         found.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            List<String> fileInfo = new ArrayList<String>();
            FileList result = mService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            List<File> files = result.getFiles();
            if (files != null) {
                for (File file : files) {
                    fileInfo.add(String.format("%s (%s)\n",
                            file.getName(), file.getId()));
                }
            }
            return fileInfo;
        }

        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            //mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            //mProgress.hide();
            if (output == null || output.size() == 0) {
                //mOutputText.setText("No results returned.");
            } else {
                //output.add(0, "Data retrieved using the Drive API:");
                //mOutputText.setText(TextUtils.join("\n", output));
            }
        }
    }

    //MRT3

    //MRT4

    //MRT5
    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


        /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }
}
