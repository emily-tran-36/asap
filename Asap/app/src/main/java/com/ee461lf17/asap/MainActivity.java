package com.ee461lf17.asap;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    GoogleAccountCredential mCredential;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    //permissions required for Google Sheets access
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    private static final String PREF_ACCOUNT_NAME = "accountName";
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

    static ArrayList<String> budgetList = new ArrayList<String>();
    static ArrayList<String> categoriesList = new ArrayList<String>();
    static ArrayList<String> accountsList = new ArrayList<String>();

    static HashMap<String, String> budgetAmountMap = new HashMap<String, String>();
    static HashMap<String, String> accountAmountMap = new HashMap<String, String>();
    //static HashMap<String, List<String>> expenseMap = new HashMap<String, List<String>>();


    static HashMap<String, HashMap<String, List<String>>> budgetExpenseMap = new HashMap<String, HashMap<String, List<String>>>();
    static HashMap<String, String> budgetAccountMap = new HashMap<String, String>();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        com.github.clans.fab.FloatingActionButton account = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabAccount);
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
                                accountsList.add(Name.getText().toString());
                                accountAmountMap.put(Name.getText().toString(), Amount.getText().toString());

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

                Spinner accounts = popupLayout.findViewById(R.id.spinner4);
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

                                budgetList.add(Name.getText().toString());
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

                Spinner categories = popupLayout.findViewById(R.id.categoryspinner);
                String[] catNames = categoriesList.toArray(new String[0]);
                ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(MainActivity.this,
                        R.layout.spinner_item, catNames);
                catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categories.setAdapter(catAdapter);

                Spinner budgets = popupLayout.findViewById(R.id.budgetspinner);
                String[] budgetNames = budgetList.toArray(new String[0]);
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this,
                R.layout.spinner_item, budgetNames);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                budgets.setAdapter(dataAdapter);

                final EditText Name = (EditText) popupLayout.findViewById(R.id.newExpenseName);
                final EditText Amount = (EditText) popupLayout.findViewById(R.id.newExpenseAmount);
                final Spinner Budget = (Spinner) popupLayout.findViewById(R.id.budgetspinner);
                final String budgetText = Budget.getSelectedItem().toString();
                final Spinner Category = (Spinner) popupLayout.findViewById(R.id.categoryspinner);
                final String categoryText = Category.getSelectedItem().toString();

                ((Button) popupLayout.findViewById(R.id.confirm_new_expense))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {
                                //budgetList.add(Name.getText().toString());

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

                Spinner accounts = popupLayout.findViewById(R.id.spinner3);
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
                                //budgetList.add(Name.getText().toString());

//                                ArrayList<String> tempList = new ArrayList<String>();
//                                tempList.add("Test1");
//                                tempList.add("Test2");

                                //budgetMap.put(Name.getText().toString(),tempList);

                                popup.dismiss();

                            }

                        });

                ((Button) popupLayout.findViewById(R.id.cancel_new_income))
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View arg0) {
                                popup.dismiss();

                            }

                        });
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
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        promptUserToChooseAccount();
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
//        if (!EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
//            EasyPermissions.requestPermissions(
//                    this,
//                    "This app needs to access your Google account (via Contacts).",
//                    REQUEST_PERMISSION_GET_ACCOUNTS,
//                    Manifest.permission.GET_ACCOUNTS);
//        }
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
                budgetList.addAll( Arrays.asList(budgets) );

                // Create ArrayAdapter using the budget list.
                ListAdapter budgetAdapter = new ArrayAdapter<String>(getActivity(), R.layout.simplerow, budgetList);
                final ListView mainListView = (ListView) rootView.findViewById( R.id.mainListView );
                mainListView.setAdapter( budgetAdapter );

                //click on list item
                mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        //if (position == 0) {
                            Intent myIntent = new Intent(view.getContext(), BudgetDetailsActivity.class);
                            String  budgetName    = (String) mainListView.getItemAtPosition(position);

                            myIntent.putExtra("Expense Details", budgetExpenseMap);
                            myIntent.putExtra("Current Budget", budgetName);

                            startActivity(myIntent);
                            //startActivityForResult(myIntent, 0);
                       // }
//                        if (position == 1) {
//                            Intent myIntent = new Intent(view.getContext(), BudgetDetailsActivity.class);
//                            startActivityForResult(myIntent, 0);
//                        }
//                        if (position == 2) {
//                            Intent myIntent = new Intent(view.getContext(), BudgetDetailsActivity.class);
//                            startActivityForResult(myIntent, 0);
//                        }
                    }
                });


            }
            else if(viewNumber == 2){
                String[] accounts = new String[] { "No accounts yet."};
                accountsList.addAll( Arrays.asList(accounts) );
                // Create ArrayAdapter using the budget list.
                ListAdapter accountAdapter = new ArrayAdapter<String>(getActivity(), R.layout.simplerow, accountsList);
                final ListView mainListView = (ListView) rootView.findViewById( R.id.mainListView );
                mainListView.setAdapter( accountAdapter );

                //click on list item
//                mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
//                    public void onItemClick(AdapterView<?> parent, View view,
//                                            int position, long id) {
//                        if (position == 0) {
////                            Intent myIntent = new Intent(view.getContext(), BudgetDetailsActivity.class);
////                            String  accountName    = (String) mainListView.getItemAtPosition(position);
////
////                            myIntent.putExtra("Expense Details", budgetExpenseMap);
////                            myIntent.putExtra("Current Budget", accountName);
//
//                            startActivity(myIntent);
//                            //startActivityForResult(myIntent, 0);
//                        }
//                        if (position == 1) {
//                            Intent myIntent = new Intent(view.getContext(), BudgetDetailsActivity.class);
//                            startActivityForResult(myIntent, 0);
//                        }
//                        if (position == 2) {
//                            Intent myIntent = new Intent(view.getContext(), BudgetDetailsActivity.class);
//                            startActivityForResult(myIntent, 0);
//                        }
//                    }
//                });


            }
            else{
                
            }
            return rootView;
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
}
