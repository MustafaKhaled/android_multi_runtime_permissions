package com.android_multi_runtime_permissions;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity  implements View.OnClickListener{
    private String TAG = "MainActivity";
    public final  String READ_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;
    public final  String CAPTURE_WRITE_IMAGE_PERMISSIONS [] = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int OPEN_GALLERY_REQUEST_CODE= 100;
    private final int READ_PERMISSION_REQUEST_CODE=200;
    private final int CAPTURE_PERMISSION_REQUEST_CODE=300;
    private final int CAMERA_PERMISSION_REQUEST_CODE=400;
    Button single_permission,multi_permission;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        single_permission = (Button) findViewById(R.id.single_permission_button);
        multi_permission = (Button) findViewById(R.id.multi_permission_button);
        imageView = (ImageView) findViewById(R.id.imageView);
        single_permission.setOnClickListener(this);
        multi_permission.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.single_permission_button:{
                if(!isSinglePermissionGranted()){
                    Log.e(TAG,"checkSinglePermission(): Once we find permission is denied we would ask for this permission.");
                    requestPermissions(new String[]{READ_STORAGE_PERMISSION},READ_PERMISSION_REQUEST_CODE);
                }
                else{
                    openGallery();
                    Log.e(TAG,"checkSinglePermission(): Gallery opened");
                }

                break;
            }
            case R.id.multi_permission_button:{
                if(areMultiPermissionGranted()){
                    openCamera();
                    Log.e(TAG,"checkMultiplePermission(): Camera opened");
                }
                else{
                    Log.e(TAG,"checkMultiplePermission(): Once we find permission is denied we would ask for this permission.");
                    request_take_photo_multiple_permissions();

                }
                break;
            }

        }
    }

    /*
     *Here we can receieve the result of opening new gallery Intent, using our request code defined before.
     * This request code used to identify your request, because there maybe many of requests in this activity.
     * For example, if you need to navigate to another intent (e.g. gallery or contacts), we would recieve the result in this method
     * so need a unique key for each request to handle its results.
     * notice: you can assume any request code.
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case OPEN_GALLERY_REQUEST_CODE:{
                if(resultCode == RESULT_OK){
                    Log.e(TAG,"onActivityResult(): result from gallery succeeded");
                    try {
                        Uri selectedImage = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),selectedImage);
                        Log.e(TAG,"onActivityResult(): Save URI selected to Bitmap to set it as imageview source");
                        imageView.setImageBitmap(bitmap);
                    }
                    catch (IOException ex){
                        ex.printStackTrace();
                    }
                }
                break;
            }
            case CAMERA_PERMISSION_REQUEST_CODE:{
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageView.setImageBitmap(imageBitmap);
                    saveImageToExternalStorage(imageBitmap);
                }

                break;
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
     * This function is result of requestpermission() function.
     * When request a permission a promote shown to the user. here we handle the interaction of user with this promote.
     * also using request code we defined with luanching the reqiest, we here handle the result according to request code.
     * READ COMMENTS OF onActivityResult();
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case READ_PERMISSION_REQUEST_CODE:{
                //grantResults array is used to store user response to Allow permissions promote.
                if(grantResults.length>0){
                    if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                        Log.e(TAG,"onRequestPermissionsResult(): The User Allow Read Storage Permission");
                        //Once Allow is pressed, open the gallery.
                        openGallery();
                    }
                    else {
                        Log.e(TAG,"onRequestPermissionsResult(): The User Denied Read Storage Permission");
                        //shouldShowRequestPermission is boolean function returns true if user press deny.
                        //Here you can show how important this permission, by showing a dialog.
                        if(shouldShowRequestPermissionRationale(READ_STORAGE_PERMISSION)){
                            Log.e(TAG,"onRequestPermissionsResult(): The User chose Deny, so shouldShowRequestPermissionRationale is true");
                            //A dialogue now is shown to user to inform about importance of allowing this permission.
                            showMessageOKCancel(getString(R.string.deny_permission),getString(R.string.retry_button_string), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Log.e(TAG, "onClick(): Retry the requesting permission");
                                    requestPermissions(new String[]{READ_STORAGE_PERMISSION},OPEN_GALLERY_REQUEST_CODE);
                                }
                            });
                        }
                        else {
                            //shouldShowRequestPermission is boolean function returns false if user press deny
                            //and check 'Don't ask again'
                            //Which mean that this promote won't show again and permission would still denied until
                            //user go to setting to to enable permissions
                            showMessageOKCancel(getString(R.string.goto_setting_dialog_message),getString(R.string.goto_setting_button_string), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Log.e(TAG, "onClick(): Go to setting.");
                                    goToSettings();
                                }
                            });
                        }
                    }
                }

                break;
            }
            case CAPTURE_PERMISSION_REQUEST_CODE:{
                if(grantResults.length>0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (shouldShowRequestPermissionRationale(permissions[i])) {
                                    showMessageOKCancel(getString(R.string.deny_permission),getString(R.string.retry_button_string), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                Log.e(TAG, "Here we are recall the take photo permission");
                                                request_take_photo_multiple_permissions();
                                            }
                                        }
                                    });
                                    return;
                                } else {
                                    Log.e(TAG, "The user checked don't ask again for the take photo permission");
                                    showMessageOKCancel(getString(R.string.goto_setting_dialog_message),getString(R.string.goto_setting_button_string), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                Log.e(TAG, "Go to settings");
                                                Intent intent = new Intent();
                                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                                intent.setData(uri);
                                                startActivity(intent);
                                            }
                                        }
                                    });
                                    return;
                                }
                            }
                        }
                        //This condition is required, because user maybe accept camera permission and deny Write to storage
                        //permission which would crash the application.
                        //So we should guarantee that all permissions are enabled
                        else if(areMultiPermissionGranted()){
                            openCamera();
                        }
                    }


                }
            }
            break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean isSinglePermissionGranted() {
        Log.e(TAG,"checkSinglePermission(): Check if Read Storage Permission is Granted or not.");
        if(checkSelfPermission(READ_STORAGE_PERMISSION)== PackageManager.PERMISSION_GRANTED){
            Log.e(TAG,"checkSinglePermission(): Read Storage Permission is Granted.");

            return true;
        }
        else {
            Log.e(TAG,"checkSinglePermission(): Read Storage Permission is Denied.");
            return false;
        }
    }

    public boolean areMultiPermissionGranted() {
        Log.e(TAG,"checkMultiplePermission(): Check if Camera Permission and Write External Storage are Granted or not.");
        Log.e(TAG,"checkMultiplePermission(): If a permission is not enabled, request permissions.");

        for(String permission:CAPTURE_WRITE_IMAGE_PERMISSIONS){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    private void openGallery(){
        Intent photo_picker = new Intent(Intent.ACTION_PICK);
        photo_picker.setType("image/*");
        startActivityForResult(photo_picker,OPEN_GALLERY_REQUEST_CODE);
    }

    private void openCamera(){
        Log.e(TAG, "Permission Accepted, ACCESS YOUR PRETTY CAMERA");
        Toast.makeText(this, "Application now takes the permission", Toast.LENGTH_LONG).show();
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_PERMISSION_REQUEST_CODE);
    }
    private void goToSettings(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    private void showMessageOKCancel(String message,String positive_btn_text, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton(positive_btn_text, okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    private void request_take_photo_multiple_permissions(){
        List<String> permissions_needed=new ArrayList<>();
        for (String permission:CAPTURE_WRITE_IMAGE_PERMISSIONS){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                permissions_needed.add(permission);
            }
        }
        requestPermissions(permissions_needed.toArray(new String[permissions_needed.size()]),CAPTURE_PERMISSION_REQUEST_CODE);
    }
    private File saveImageToExternalStorage(Bitmap finalBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        Log.e(TAG,"saveImageToExternalStorage(): Storage Path: "+ root);
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = System.currentTimeMillis()+".jpg";
        Log.e(TAG,"saveImageToExternalStorage(): File name is: "+ fname);
        File file = new File(myDir, fname);
        Log.e(TAG,"saveImageToExternalStorage(): " +file.getAbsolutePath());

        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
        Log.e(TAG,"saveImage(): Image has been saved to storage");
        return file;
    }
}
