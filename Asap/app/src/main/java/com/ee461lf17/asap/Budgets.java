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
    //TODO: update leftover in budget summary
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
    //TODO: update account summary
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
                while(retry){
                    try{
                        ValueRange response = request.execute();
                        return response.getValues();
                    } catch (UserRecoverableAuthIOException e) {
                        System.out.println("Trying again for permissions");
                        callingActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    }
                }
                ValueRange response = request.execute();
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