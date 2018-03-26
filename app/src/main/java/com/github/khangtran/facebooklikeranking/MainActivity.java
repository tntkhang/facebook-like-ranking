package com.github.khangtran.facebooklikeranking;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.widget.ShareDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CallbackManager callbackManager;
    private ProfileTracker profileTracker;
    private LoginButton loginButton;
    private String TAG = "MainActivity";
    private String name, surname, imageUrl;
    private TextView tvTotalPhoto;
    private TextView progress;
    private TextView progressDetail;
    private int totalCountPhoto;
    private int detailPhotoCount;

    private int startIndex = 0;
    private String top99PhotosPath = "/1208506309315145/photos?limit=99";

    private List<Photo> photos = new ArrayList<>();
    private List<Photo> topLikePhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FacebookSdk.sdkInitialize(this);
        callbackManager = CallbackManager.Factory.create();

        tvTotalPhoto = (TextView) findViewById(R.id.total_photo);
        progress = (TextView) findViewById(R.id.progress);
        progressDetail = (TextView) findViewById(R.id.progressDetail);

        AccessToken token;
        token = AccessToken.getCurrentAccessToken();

        if (token != null) {
            getAlbumSize();
        }

        FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
//                        Log.e(TAG,object.toString());
//                        Log.e(TAG,response.toString());
//
//                        try {
//                            userId = object.getString("id");
//                            profilePicture = new URL("https://graph.facebook.com/" + userId + "/picture?width=500&height=500");
//                            if(object.has("first_name"))
//                                firstName = object.getString("first_name");
//                            if(object.has("last_name"))
//                                lastName = object.getString("last_name");
//                            if (object.has("email"))
//                                email = object.getString("email");
//                            if (object.has("birthday"))
//                                birthday = object.getString("birthday");
//                            if (object.has("gender"))
//                                gender = object.getString("gender");
//
//                            Intent main = new Intent(LoginActivity.this,MainActivity.class);
//                            main.putExtra("name",firstName);
//                            main.putExtra("surname",lastName);
//                            main.putExtra("imageUrl",profilePicture.toString());
//                            startActivity(main);
//                            finish();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        } catch (MalformedURLException e) {
//                            e.printStackTrace();
//                        }

                        getAlbumSize();
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, first_name, last_name, email, birthday, gender , location");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {
                e.printStackTrace();
            }
        };

        loginButton.setReadPermissions("email");
        loginButton.registerCallback(callbackManager, callback);
    }

    public void getAlbumSize() {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "count");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/1208506309315145",
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response.getRawResponse());
                            String count = jsonObject.getString("count");
                            String id = jsonObject.getString("id");

                            totalCountPhoto = Integer.parseInt(count);
                            tvTotalPhoto.setText("Total count: " + count + "  -- " + id);

                            getTop10LikedCount(top99PhotosPath);
                        } catch (JSONException e) {

                        }
                    }
                }
        ).executeAsync();
    }

    public void getTop10LikedCount(String path) {
        Bundle parameters = new Bundle();
        parameters.putString("limit", "99");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                path,
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response.getRawResponse());
                            JSONArray dataObj = jsonObject.getJSONArray("data");

                            for (int i =0; i < dataObj.length(); i++) {
                                JSONObject eachPhoto = dataObj.getJSONObject(i);
                                String createdTime = eachPhoto.getString("created_time");
                                String name = "";
                                if (eachPhoto.has("name")) {
                                   name = eachPhoto.getString("name");
                                }
                                String id = eachPhoto.getString("id");

                                Photo newPh = new Photo(createdTime, name, id);
                                if (photos.contains(newPh)) {
                                    Log.e("tntkhang", "Contain: " + id);
                                } else {
                                    photos.add(newPh);
                                    getDetail(newPh);
                                }
                            }

                            progress.setText("Scan: " + photos.size() + "/" + totalCountPhoto);

                            if (jsonObject.has("paging")) {
                                JSONObject paging = jsonObject.getJSONObject("paging");
                                JSONObject cursors = paging.getJSONObject("cursors");
                                String before = cursors.getString("before");
                                String after = cursors.getString("after");

                                String path = "&after=" + after;

                                if (photos.size() < totalCountPhoto) {
                                    getTop10LikedCount(top99PhotosPath + path);
                                    Log.e("tntkhang", "getTop10LikedCount next ---- " + top99PhotosPath + path);

                                } else {
                                    Log.e("tntkhang", "ALL DATA WAS GOT " + photos.size());
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "FINISH SCANING", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }
    
    private void getDetail(final Photo photo) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "count");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + photo.getId() + "/likes?summary=total_count",
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response.getRawResponse());
                            JSONObject summary = jsonObject.getJSONObject("summary");
                            String totalCount = summary.getString("total_count");

                            Log.e("tntkhang", "ID: " +  photo.getId() + " - " + totalCount);
                            int totalCountInt = Integer.parseInt(totalCount);
                            if (totalCountInt > 500) {
                                photo.setTotalCount(totalCountInt);
                                topLikePhotos.add(photo);
                                String textShow = "Like: " + totalCountInt + " - https://facebook.com/" + photo.getId() + "\n";
                                progressDetail.setText(progressDetail.getText() + textShow);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.logout:
                LoginManager.getInstance().logOut();
                break;
        }
    }
}
