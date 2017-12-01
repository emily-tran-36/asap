package com.ee461lf17.asap;

import android.app.Activity;

import com.google.api.client.extensions.android.http.AndroidHttp;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private static final String MASTER_ID = "1rf4l4FVyEMBKhRdwFEGz-rkl1zcvDImSModS5IerNA0";
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    private GoogleAccountCredential credential;

    private final String accountName;
    private final String masterSheetID;
    private List<String> templateIDs = new ArrayList<>();
    private List<String> accountIDs = new ArrayList<>();
    private List<String> accountNames = new ArrayList<>();
    private List<String> budgetSummaryIDs = new ArrayList<>();
    private List<String> budgetNames = new ArrayList<>();
    private List<String> budgetIDs = new ArrayList<>();

    public Budgets(GoogleAccountCredential mCredential, Activity callingActivity) {
        credential = mCredential;
        //addExpenditure(callingActivity, credential, "1Vl9m-oPg0w4QmXz-29POvEN7FHCl34feXvGqWplpDHw","Shit", 2222, "bullshit", "12/01/2017");
        while(mCredential.getSelectedAccountName() == null) {}
        accountName = mCredential.getSelectedAccountName().toLowerCase();
        masterSheetID = getMasterIDFromSheet(callingActivity);
        //updateTemplateBudgetNameAccountIDs(callingActivity);
        System.out.println("Initialization finished");
    }
    //Pre: Account credential, accountName, and masterID are set
    private void updateTemplateBudgetNameAccountIDs(Activity callingActivity){
        try {
            Sheets sheetsService = createSheetsService(credential);
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(masterSheetID, "a:d");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            for(int i = 1; i < list.size(); ++i) {
                List<Object> l = list.get(i);
                if(1 < list.size() && !l.get(0).equals("") && !l.get(1).equals("")){
                    String budgetSummaryID = (String)l.get(1);
                    sheetsService = createSheetsService(credential);
                    request = sheetsService.spreadsheets().values().get(budgetSummaryID, "b2:b");
                    f = runGetRequestOnSeparateThread(callingActivity, request);
                    List<List<Object>> list2 = f.get();
                    String templateID = (String)list2.get(0).get(0);
                    String latestBudget = "";
                    if(list.size() > 1){
                        latestBudget = (String)list2.get(list2.size() - 1).get(0);
                    }
                    templateIDs.add(templateID);
                    budgetIDs.add(latestBudget);
                    budgetSummaryIDs.add(budgetSummaryID);
                    String budgetName = (String)l.get(0);
                    budgetNames.add(budgetName);
                }
                if(3 < list.size() && !l.get(2).equals("") && !l.get(3).equals("")) {
                    String accountName = (String)l.get(2);
                    accountNames.add(accountName);
                    String accountID = (String)l.get(3);
                    accountIDs.add(accountID);
                }


            }
        } catch(Exception e) {
            System.out.println("something went wrong " + e);
        }
    }
    //Pre: Account credential and accountName are set
    private String getMasterIDFromSheet(Activity callingActivity) {
        try {
            Sheets sheetsService = createSheetsService(credential);
            Sheets.Spreadsheets.Values.Get request = sheetsService.spreadsheets().values()
                    .get(MASTER_ID, "a:b");
            Future<List<List<Object>>> f = runGetRequestOnSeparateThread(callingActivity, request);
            List<List<Object>> list = f.get();
            for(List<Object> l: list) {
                String comp = (String)l.get(0);
                if(comp.toLowerCase().equals(accountName.toLowerCase())){
                    if(l.size() < 2){
                        return createNewMasterSheetFor(accountName);
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
        return createNewMasterSheetFor(accountName);
    }

    //Will add account name to the home sheet, then create a new, empty master sheet,
    //add its ID to the home sheet, and return the ID
    private static String createNewMasterSheetFor(String accountName) {
        //TODO: implement with real code
        return null;
    }


    //Returns a map from budget names to sheets IDs for the given user.
    //Will map to the most recent instance of the budget with the given name
    public static Map<String, String> getUserBudgets(String email) {
        return null;
    }

    //Returns a map from account names to sheets IDs for the given user.
    public static Map<String, String> getUserAccounts(GoogleAccountCredential credential) {
        String email = credential.getSelectedAccountName();
        return null;
    }

    //Adds an expenditure to the budget sheet corresponding to the given ID
    public static void addExpenditure(final Activity callingActivity, GoogleAccountCredential credential, String budgetID, String category,
                                      double expense, String comment, String date) {
        String range = "A1";
        String valueInputOption = "USER_ENTERED";
        String insertDataOption = "INSERT_ROWS";

        ValueRange requestBody = new ValueRange();
        requestBody.set("range", "A1");
        Object[][] ray = {{"","","","","","","","","","","",category,expense,comment,date}};
        requestBody.set("values", new ArrayList<Object>(Arrays.asList(ray)));
        Sheets sheetsService;
        try {
            sheetsService = createSheetsService(credential);
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
    public static void addMoneyToAccount(final Activity callingActivity, GoogleAccountCredential credential, String accountID,
                                         double amount, String comment, String date){
        String range = "A1";
        String valueInputOption = "USER_ENTERED";
        String insertDataOption = "INSERT_ROWS";

        ValueRange requestBody = new ValueRange();
        requestBody.set("range", "A1");
        Object[][] ray = {{"",amount,date,comment}};
        requestBody.set("values", new ArrayList<Object>(Arrays.asList(ray)));
        Sheets sheetsService;
        try {
            sheetsService = createSheetsService(credential);
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
    public static File copyFile(Drive service, String originFileId,
                                 String copyTitle) {
        File copiedFile = new File();
        copiedFile.setName(copyTitle);
        try {
            return service.files().copy(originFileId, copiedFile).execute();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
        return null;
    }

    public static String createFile(Drive service, String originFileID, String copyTitle){
        File newFile = copyFile(service, originFileID, copyTitle);
        return newFile.getId();
    }


    //returns user's current budgets
    public static Future<List<List<String>>> getUserCurrentBudgets(final Activity callingActivity, GoogleAccountCredential credential){
        
        return null;
    }

    //gets the corresponding dates for the user's current budgets
    public static Future<List<List<String>>> getBudgetDates(){
        return null;
    }

    //gets allocated amounts for the user's current budgets
    public static Future<List<List<String>>> getBudgetedAmounts(){
        return null;
    }

    //gets leftover amounts for the user's current budgets
    public static Future<List<List<String>>> getLeftoverAmounts(){
        return null;
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

    private static Sheets createSheetsService(GoogleAccountCredential credential) throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Asap")
                .build();
    }
}