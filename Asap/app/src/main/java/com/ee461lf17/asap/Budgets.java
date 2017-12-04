package com.ee461lf17.asap;

import android.app.Activity;
import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import static com.ee461lf17.asap.MainActivity.REQUEST_AUTHORIZATION;

/**
 * Utilities for managing budgets
 * Created by Paul Cozzi on 11/30/2017.
 */

public class Budgets {
    private static final String HOME_ID = "1rf4l4FVyEMBKhRdwFEGz-rkl1zcvDImSModS5IerNA0";
    private static final String BUDGET_TEMPLATE = "1uRJFJxTv9KP-WcI6gpLVcnRUmg-ISAu0rQfuZRkfrpY";
    private static final String ACCOUNT_TEMPLATE = "1JPABuXbvYn4u03_oa5KBf2R6ON2OoN4hoKDgtiTKgX8";
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    private GoogleAccountCredential credential;

    private String userName;
    private String userSheetID;

    private List<String> accountIDs = new ArrayList<>();
    private List<String> accountNames = new ArrayList<>();
    private List<String> budgetNames = new ArrayList<>();
    private List<String> budgetIDs = new ArrayList<>();

    //Boolean flag to indicate if the class is properly initialized or still updating.
    private Boolean isReady = false;


    public Budgets(GoogleAccountCredential mCredential, final Activity callingActivity) {
        credential = mCredential; //Quick assignment can run in the main thread

        //Complicated stuff has to run on separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(credential.getSelectedAccountName() == null) {}
                userName = credential.getSelectedAccountName().toLowerCase();
                userSheetID = getMasterIDFromSheet(callingActivity);
                updateBudgetAccountNameIDs(callingActivity);
                isReady = true;

            }
        }).start();
    }
    public boolean isReady() {
        return isReady;
    }
    //Pre: Account credential, userName, and masterID are set
    private void updateBudgetAccountNameIDs(Activity callingActivity){
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(userSheetID, "a:d");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            for(int i = 1; i < list.size(); ++i) {
                List<Object> l = list.get(i);
                if(1 < l.size() && !l.get(0).equals("") && !l.get(1).equals("")){
                    String budgetID = (String)l.get(1);
                    String budgetName = (String)l.get(0);
                    budgetIDs.add(budgetID);
                    budgetNames.add(budgetName);
                }
                if(3 < l.size() && !l.get(2).equals("") && !l.get(3).equals("")) {
                    String accountName = (String)l.get(2);
                    String accountID = (String)l.get(3);
                    accountNames.add(accountName);
                    accountIDs.add(accountID);
                }


            }
        } catch(Exception e) {
            System.out.println("something went wrong " + e);
        }
    }
    //Pre: Account credential and userName are set
    private String getMasterIDFromSheet(Activity callingActivity) {
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(HOME_ID, "a:b");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            for(List<Object> l: list) {
                String comp = (String)l.get(0);
                if(comp.toLowerCase().equals(userName.toLowerCase())){
                    if(l.size() < 2){
                        return createNewMasterSheetFor(callingActivity, userName);
                    }
                    else {
                        return (String)(l.get(1));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Something went wrong " + e);
            return null;
        }
        return createNewMasterSheetFor(callingActivity, userName);
    }

    //Will add account name to the home sheet, then create a new, empty master sheet,
    //add its ID to the home sheet, and return the ID
    private String createNewMasterSheetFor(Activity callingActivity, String accountName) {
        //TODO: implement with real code
        createNewBudget(callingActivity, accountName + "_master");
        return accountName+ "_master";
    }

    //Adds an expenditure to the budget sheet corresponding to the given ID
    public void addExpenditure(final Activity callingActivity, String budgetName, String category,
                                      double expense, String comment, String date) {
        String budgetID = budgetNameToID(budgetName);
        String range = "A1";
        String valueInputOption = "USER_ENTERED";
        String insertDataOption = "INSERT_ROWS";

        ValueRange requestBody = new ValueRange();
        requestBody.set("range", "A1");
        Object[][] ray = {{"","","","","","",category,expense,comment,date}};
        requestBody.set("values", new ArrayList<Object>(Arrays.asList(ray)));
        Sheets sheetsService;
        try {
            sheetsService = createSheetsService();
            final Sheets.Spreadsheets.Values.Append request =
                    sheetsService.spreadsheets().values().append(budgetID, range, requestBody);
            request.setValueInputOption(valueInputOption);
            request.setInsertDataOption(insertDataOption);
            runAppendRequestOnSeparateThread(callingActivity, request);
        } catch (Throwable t) {
            System.out.println("Caught an exception: " + t.toString());
        }
    }

    //Add positive or negative money
    public void addMoneyToAccount(final Activity callingActivity, String accountName,
                                         double amount, String comment, String date){
        String accountID = accountNameToID(accountName);
        String range = "A1";
        String valueInputOption = "USER_ENTERED";
        String insertDataOption = "INSERT_ROWS";

        ValueRange requestBody = new ValueRange();
        requestBody.set("range", "A1");
        Object[][] ray = {{"",amount,date,comment}};
        requestBody.set("values", new ArrayList<Object>(Arrays.asList(ray)));
        Sheets sheetsService;
        try {
            sheetsService = createSheetsService();
            final Sheets.Spreadsheets.Values.Append request =
                    sheetsService.spreadsheets().values().append(accountID, range, requestBody);
            request.setValueInputOption(valueInputOption);
            request.setInsertDataOption(insertDataOption);
            runAppendRequestOnSeparateThread(callingActivity, request);
        } catch (Throwable t) {
            System.out.println("Caught an exception: " + t.toString());
        }
    }
    //copy sheets file
    public static String copyFile(Activity callingActivity, Drive service, String originFileId,
                                 String copyTitle) {
        File copiedFile = new File();
        copiedFile.setName(copyTitle);
        try {
            File f = service.files().copy(originFileId, copiedFile).execute();
            return f.getId();
        } catch (UserRecoverableAuthIOException e) {
            System.out.println("Trying again for permissions");
            callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        } catch (Exception e) {
            System.out.println("Error");
        }
        return null;
    }

    public static String createFile(Activity callingActivity, Drive service, String originFileID, String copyTitle){
        return copyFile(callingActivity, service, originFileID, copyTitle);
    }



    private static void runAppendRequestOnSeparateThread(final Activity callingActivity, final Sheets.Spreadsheets.Values.Append request) {
        new Thread(new Runnable() {
            boolean retry = true;
            @Override
            public void run() {
                while(retry) {
                    try {
                        request.execute();
                        retry = false;
                    } catch (UserRecoverableAuthIOException e) {
                        System.out.println("Trying again for permissions");
                        callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    } catch (Throwable t) {
                        System.out.println("Was unable to update sheet: " + t);
                        retry = false;
                    }
                }
            }
        }).start();
    }

    private static Future<List<List<Object>>> runGetRequestOnSeparateThread(final Activity callingActivity, final Sheets.Spreadsheets.Values.Get request) {
        Callable<List<List<Object>>> task = new Callable<List<List<Object>>>() {
            @Override
            public List<List<Object>> call() throws Exception {
                boolean retry = true;
                ValueRange response = null;
                while(retry){
                    try{
                        response = request.execute();
                        retry = false;
                        return response.getValues();
                    } catch (UserRecoverableAuthIOException e) {
                        System.out.println("Trying again for permissions");
                        callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    } catch (Throwable e) {
                        retry = false;
                        System.out.println("Was unable to fetch from sheet " + e);
                    }
                }
                return response.getValues();
            }
        };
        return executor.submit(task);
    }

    private Sheets createSheetsService() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Asap")
                .build();
    }

    /**************************
     *                        *
     *  Task Helper Methods   *
     *                        *
     *************************/

    /**
     * Grant write permissions to user
     * @param callingActivity
     * @param email email to give permissions to
     */
    public void addUserToBudget(Activity callingActivity, String email) {
        AddUserToBudgetTask task = new AddUserToBudgetTask(credential, callingActivity, email);
        task.execute();
    }

    /**
     * Create a new budget. Should only be used by the Budgets constructor
     * @param callingActivity
     * @param budgetName name of budget to add
     */
    private void createNewBudget(Activity callingActivity, String budgetName) {
        MakeRequestTaskCreate task = new MakeRequestTaskCreate(credential, callingActivity, userSheetID, budgetName, "1000", "");
        task.execute();
    }

    /**
     * Public version of createNewBudget. Used to copy the "ExampleBudgetSimple" sheet to create a
     * budget
     * @param callingActivity
     * @param budgetName
     */
    public void addNewBudget(Activity callingActivity, String budgetName, String amount, String accountName) {
        String fileIdToCopy = Budgets.BUDGET_TEMPLATE;
        MakeRequestTaskCreate task = new MakeRequestTaskCreate(credential, callingActivity, fileIdToCopy, budgetName, amount, accountName);
        task.execute();
    }

    public void addNewAccount(Activity callingActivity, String accountName, String amount) {
        String fileIdToCopy = ACCOUNT_TEMPLATE;
        MakeRequestTaskAddAccount task = new MakeRequestTaskAddAccount(credential, callingActivity, fileIdToCopy, accountName, amount);
        task.execute();
    }

    /**
     * Copy an existing budget into a new file
     * @param callingActivity
     * @param fileNameToCopy
     * @param newFileName
     */
    public void copyBudgetSheet(Activity callingActivity, String fileNameToCopy, String newFileName) {
        int budgetIndex = budgetNames.indexOf(fileNameToCopy);
        String budgetId = budgetIDs.get(budgetIndex);
        MakeRequestTaskCopy task = new MakeRequestTaskCopy(credential, callingActivity, budgetId, newFileName);
    }

    /**
     * An asynchronous task that adds a new writer to the user sheet
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class AddUserToBudgetTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private String emailToAdd;
        private Activity callingActivity;

        AddUserToBudgetTask(GoogleAccountCredential credential, Activity callingActivity, String emailToAdd) {
            this.emailToAdd = emailToAdd;
            this.callingActivity = callingActivity;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Asap")
                    .build();
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (UserRecoverableAuthIOException e) {
                callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                return null;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         *
         * @return List of Strings describing files, or an empty list if no files
         * found.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Add a user as a writer to the userSheetID
            List<String> permissionResults = new ArrayList<String>();

            JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
                @Override
                public void onFailure(GoogleJsonError e,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    // Handle error
                    System.err.println(e.getMessage());
                }

                @Override
                public void onSuccess(Permission permission,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    System.out.println("Permission ID: " + permission.getId());
                }
            };

            Permission userPermission = new Permission()
                    .setType("user")
                    .setRole("writer")
                    .setEmailAddress(emailToAdd);

            mService.permissions().create(userSheetID, userPermission)
                    .setFields("id")
                    .execute();


            return permissionResults;
        }

        @Override
        protected void onPreExecute() {
            // Fetch email from text field and store it in class
            //emailToAdd = "rob.misasi@utexas.edu"; // TODO: Replace with getter.
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

    /**
     * An asynchronous task that makes a file publicly readable.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeFilePubliclyReadableTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private String fileId;
        private Activity callingActivity;

        MakeFilePubliclyReadableTask(GoogleAccountCredential credential, Activity callingActivity, String fileId) {
            this.fileId = fileId;
            this.callingActivity = callingActivity;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Asap")
                    .build();
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (UserRecoverableAuthIOException e) {
                callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                return null;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         *
         * @return List of Strings describing files, or an empty list if no files
         * found.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Add "anyone" as a reader
            List<String> permissionResults = new ArrayList<String>();

            JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
                @Override
                public void onFailure(GoogleJsonError e,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    // Handle error
                    System.err.println(e.getMessage());
                }

                @Override
                public void onSuccess(Permission permission,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    System.out.println("Permission ID: " + permission.getId());
                }
            };

            Permission publicPermission = new Permission()
                    .setType("anyone")
                    .setRole("reader");
            mService.permissions().create(userSheetID, publicPermission)
                    .setFields("id")
                    .execute();


            return permissionResults;
        }

        @Override
        protected void onPreExecute() {
            // Fetch email from text field and store it in class
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

    private String budgetNameToID(String name) {
        return budgetIDs.get(budgetNames.indexOf(name));
    }

    private String accountNameToID(String name) {
        return accountIDs.get(accountNames.indexOf(name));
    }

    public List<String> getAccountNames(){
        if(isReady){
            return new ArrayList<String>(accountNames);
        }
        else{
            return null;
        }
    }

    public List<String> getBudgetNames(){
        if(isReady){
            return new ArrayList<String>(budgetNames);
        }
        else{
            return null;
        }
    }

    public String getBudgetDate(Activity callingActivity, String budgetName){
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "a2");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return (String)(list.get(0).get(0));
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return "Unknown Date";
    }

    public Double getBudgetAllocatedAmount(Activity callingActivity, String budgetName) {
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "b2");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return (Double)list.get(0).get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public Double getBudgetSpentAmount(Activity callingActivity, String budgetName) {
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "c2");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return (Double)list.get(0).get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public Double getBudgetLeftoverAmount(Activity callingActivity, String budgetName) {
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "d2");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return (Double)list.get(0).get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public List<Object> getAccountInputAmounts(Activity callingActivity, String accountName) {
        String accountID = accountNameToID(accountName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(accountID, "b2:b");
            request.setMajorDimension("COLUMNS");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return list.get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public List<Object> getAccountInputDates(Activity callingActivity, String accountName) {
        String accountID = accountNameToID(accountName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(accountID, "c2:c");
            request.setMajorDimension("COLUMNS");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return list.get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public List<Object> getAccountInputComments(Activity callingActivity, String accountName) {
        String accountID = accountNameToID(accountName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(accountID, "d2:d");
            request.setMajorDimension("COLUMNS");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return list.get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public Double getAccountBalance(Activity callingActivity, String accountName) {
        String accountID = accountNameToID(accountName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(accountID, "e2");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return Double.parseDouble((String)(list.get(0).get(0)));
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public List<Object> getBudgetExpenseCategories(Activity callingActivity, String budgetName) {
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "g3:g");
            request.setMajorDimension("COLUMNS");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return list.get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public List<Object> getBudgetExpenseAmounts(Activity callingActivity, String budgetName) {
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "h3:h");
            request.setMajorDimension("COLUMNS");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return list.get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public List<Object> getBudgetExpenseComments(Activity callingActivity, String budgetName) {
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "i3:i");
            request.setMajorDimension("COLUMNS");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return list.get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;
    }

    public List<Object> getBudgetExpenseDates(Activity callingActivity, String budgetName) {
        String budgetID = budgetNameToID(budgetName);
        try {
            Sheets sheetsService = createSheetsService();
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(budgetID, "j3:j");
            request.setMajorDimension("COLUMNS");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            return list.get(0);
        } catch (Exception e) {
            System.out.println("something went wrong " + e);
        }
        return null;

    }

    //MRT1
    private class MakeRequestTaskCopy extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private String oldFileID = null;
        private String newFileName = null;
        private Activity callingActivity;
        public boolean isDriveServiceNull() {
            return mService == null;
        }

        MakeRequestTaskCopy(GoogleAccountCredential credential, Activity callingActivity, String oldFileID, String newFileName) {
            this.callingActivity = callingActivity;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Asap")
                    .build();
            this.oldFileID = oldFileID;
            this.newFileName = newFileName;
        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (UserRecoverableAuthIOException e) {
                callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                return null;
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
            List<String> fileID = new ArrayList<String>();
            fileID.add(Budgets.copyFile(callingActivity, mService, oldFileID, newFileName));
            //returns a single fileID in a list
            return fileID; //tfw you return a single element list because doInBackground complains
        }

        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            //mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            if (output == null || output.size() == 0) {
                //mOutputText.setText("No results returned.");
            } else {
                //output.add(0, "Data retrieved using the Drive API:");
                //mOutputText.setText(TextUtils.join("\n", output));
            }
        }
    }

    //MRT2
    private class MakeRequestTaskCreate extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Activity callingActivity;
        private Exception mLastError = null;
        private String oldFileID = null;
        private String newFileName = null;
        private String newFileId = null;
        private String amountToAdd = null;
        private String account = null;
        public boolean isDriveServiceNull() {
            return mService == null;
        }

        MakeRequestTaskCreate(GoogleAccountCredential credential, Activity callingActivity, String oldFileID, String newFileName, String amountToAdd, String account) {

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            this.callingActivity = callingActivity;
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            this.oldFileID=oldFileID;
            this.newFileName=newFileName;
            this.amountToAdd = amountToAdd;
            this.account = account;
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Asap")
                    .build();

        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                List<String> fileId = getDataFromApi();
                return fileId;
            } catch (UserRecoverableAuthIOException e) {
                callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                return null;
            }catch (Exception e) {
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
            List<String> fileID = new ArrayList<String>();
            newFileId = Budgets.createFile(callingActivity, mService, oldFileID, newFileName);

            fileID.add(newFileId);

            // Add budget to userSheet
            String range = "A1";
            String valueInputOption = "USER_ENTERED";
            String insertDataOption = "INSERT_ROWS";

            ValueRange addToUserSheetRequestBody = new ValueRange();
            addToUserSheetRequestBody.set("range", "A1");
            String accountStr;
            if(account.equalsIgnoreCase("No accounts yet.")) {

            }
            Object[][] ray = {{newFileName, newFileId}};
            addToUserSheetRequestBody.set("values", new ArrayList<Object>(Arrays.asList(ray)));
            Sheets sheetsService;
            try {
                sheetsService = createSheetsService();
                final Sheets.Spreadsheets.Values.Append request =
                        sheetsService.spreadsheets().values().append(userSheetID, range, addToUserSheetRequestBody);
                request.setValueInputOption(valueInputOption);
                request.setInsertDataOption(insertDataOption);
                runAppendRequestOnSeparateThread(callingActivity, request);
            } catch (Throwable t) {
                System.out.println("Caught an exception: " + t.toString());
            }

            ValueRange insertAmountAndAccountRequestBody = new ValueRange();
            Calendar cal = new GregorianCalendar();
            String date = "'" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);
            insertAmountAndAccountRequestBody.set("range", "A1");
            Object[][] row = {{date ,amountToAdd,"0","0", account}};
            insertAmountAndAccountRequestBody.set("values", new ArrayList<Object>(Arrays.asList(row)));
            try {
                sheetsService = createSheetsService();
                final Sheets.Spreadsheets.Values.Append request =
                        sheetsService.spreadsheets().values().append(newFileId, range, insertAmountAndAccountRequestBody);
                request.setValueInputOption(valueInputOption);
                request.setInsertDataOption(insertDataOption);
                runAppendRequestOnSeparateThread(callingActivity, request);
            } catch (Throwable t) {
                System.out.println("Caught an exception: " + t.toString());
            }

            //returns single FileID stored in List
            return fileID;
        }

        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            //mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            if (output == null || output.size() == 0) {
                System.out.println("No results returned.");
            } else if (newFileId != null){
                budgetIDs.add(newFileId);
                budgetNames.add(newFileName);
            }
        }
    }

    //MRT3
    private class MakeRequestTaskAddAccount extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private String oldFileID = null;
        private String newFileName = null;
        private String newFileID = null;
        private String amount = null;
        private Activity callingActivity;
        public boolean isDriveServiceNull() {
            return mService == null;
        }

        MakeRequestTaskAddAccount(GoogleAccountCredential credential, Activity callingActivity, String oldFileID, String newFileName, String amount) {
            this.callingActivity = callingActivity;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Asap")
                    .build();
            this.oldFileID = oldFileID;
            this.newFileName = newFileName;
            this.amount = amount;
        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (UserRecoverableAuthIOException e) {
                callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                return null;
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
            List<String> fileID = new ArrayList<String>();
            newFileID = Budgets.copyFile(callingActivity, mService, oldFileID, newFileName);
            fileID.add(newFileID);

            // Add budget to userSheet
            String range = "A1";
            String valueInputOption = "USER_ENTERED";
            String insertDataOption = "INSERT_ROWS";

            ValueRange addNewAccountRequestBody = new ValueRange();
            addNewAccountRequestBody.set("range", "A1");
            String accountStr;

            Object[][] ray = {{newFileName, "","","",amount}};
            addNewAccountRequestBody.set("values", new ArrayList<Object>(Arrays.asList(ray)));
            Sheets sheetsService;
            try {
                sheetsService = createSheetsService();
                final Sheets.Spreadsheets.Values.Append request =
                        sheetsService.spreadsheets().values().append(newFileID, range, addNewAccountRequestBody);
                request.setValueInputOption(valueInputOption);
                request.setInsertDataOption(insertDataOption);
                runAppendRequestOnSeparateThread(callingActivity, request);
            } catch (Throwable t) {
                System.out.println("Caught an exception: " + t.toString());
            }
            //returns a single fileID in a list
            return fileID; //tfw you return a single element list because doInBackground complains
        }

        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            //mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            if (output == null || output.size() == 0) {
                //mOutputText.setText("No results returned.");
            } else if (newFileID != null) {
                accountIDs.add(newFileID);
                accountNames.add(newFileName);
            }
        }
    }


}