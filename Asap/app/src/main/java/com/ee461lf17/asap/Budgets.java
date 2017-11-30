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
import java.util.Map;

import static com.ee461lf17.asap.MainActivity.REQUEST_AUTHORIZATION;

/**
 * Utilities for managing budgets
 * Created by Paul Cozzi on 11/30/2017.
 */

public class Budgets {
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
            runRequestOnSeparateThread(callingActivity, request);
        } catch (Throwable t) {
            System.out.println("Caught an exception: " + t.toString());
        }
    }
    //copy sheets file
    private static File copyFile(Drive service, String originFileId,
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

    private static String createFile(Drive service, String originFileID, String copyTitle){
        File newFile = copyFile(service, originFileID, copyTitle);
        return newFile.getId();
    }
    private static void runRequestOnSeparateThread(final Activity callingActivity, final Sheets.Spreadsheets.Values.Append request) {
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

    private static Sheets createSheetsService(GoogleAccountCredential credential) throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        // TODO: Change placeholder below to generate authentication credentials. See
        // https://developers.google.com/sheets/quickstart/java#step_3_set_up_the_sample
        //
        // Authorize using one of the following scopes:
        //   "https://www.googleapis.com/auth/drive"
        //   "https://www.googleapis.com/auth/drive.file"
        //   "https://www.googleapis.com/auth/spreadsheets"

        //TODO: Figure out what should be put for ApplicationName
        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Asap")
                .build();
    }
}