package com.github.khangtran.facebooklikeranking;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import khangtran.preferenceshelper.PreferencesHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CallbackManager callbackManager;
    private TextView tvTotalPhoto;
    private TextView progress;
//    private TextView progressDetail;
    private int totalCountPhoto;
    private String top99PhotosPath = "/1208506309315145/photos?limit=99";

    private List<Photo> photos = new ArrayList<>();
    private List<Photo> topLikePhotos = new ArrayList<>();
    private List<Photo> topLikeLast = new ArrayList<>();

    private RecyclerView recyclerView;
    private TopLikeAdapter mAdapter;
    private Button btnStartScanning;

    private static final String LIST_TOP_LIKE = "LIST_TOP_LIKE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_main);
        callbackManager = CallbackManager.Factory.create();

        tvTotalPhoto = (TextView) findViewById(R.id.total_photo);
        progress = (TextView) findViewById(R.id.progress);
        btnStartScanning = (Button) findViewById(R.id.startScanning);

        PreferencesHelper.initHelper(this);

        AccessToken token;
        token = AccessToken.getCurrentAccessToken();

        if (isNetworkConnected()) {
            if (token != null) {
                getAlbumSize();
                btnStartScanning.setEnabled(true);
            } else {
                btnStartScanning.setEnabled(false);
            }
        } else {
            btnStartScanning.setEnabled(false);
            Toast.makeText(this, "No internet connection !", Toast.LENGTH_LONG).show();
        }
        FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
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

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");
        loginButton.registerCallback(callbackManager, callback);


        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);


        topLikeLast = getListFromPref();
        if (topLikeLast == null) {
            topLikeLast = new ArrayList<>();
        }

//        List<Photo> topLikeLast = new ArrayList<>();

        mAdapter = new TopLikeAdapter(this, topLikePhotos, topLikeLast);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AccessToken token;
        token = AccessToken.getCurrentAccessToken();

        if (isNetworkConnected()) {
            if (token != null) {
                getAlbumSize();
                btnStartScanning.setEnabled(true);
            } else {
                btnStartScanning.setEnabled(false);
            }
        } else {
            btnStartScanning.setEnabled(false);
            Toast.makeText(this, "No internet connection !", Toast.LENGTH_LONG).show();
        }
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
                            tvTotalPhoto.setText("Total count: " + count);

                            getLikeCount(top99PhotosPath);
                        } catch (JSONException e) {

                        }
                    }
                }
        ).executeAsync();
    }

    public void getLikeCount(String path) {
        Bundle parameters = new Bundle();
        parameters.putString("limit", "50");

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

                                    if (photos.size() == totalCountPhoto) {
                                        progress.setTextColor(Color.GREEN);
                                    }
                                }
                            }

                            if (jsonObject.has("paging")) {
                                JSONObject paging = jsonObject.getJSONObject("paging");
                                JSONObject cursors = paging.getJSONObject("cursors");
                                String before = cursors.getString("before");
                                String after = cursors.getString("after");

                                String path = "&after=" + after;

                                if (photos.size() < totalCountPhoto) {
                                    getLikeCount(top99PhotosPath + path);
                                }
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

                            progress.setText("Scan: " + photos.size() + "/" + totalCountPhoto);
                            if (photos.size() == totalCountPhoto) {
                                getDataDone();
                            }

                            int totalCountInt = Integer.parseInt(totalCount);
                            if (totalCountInt > 500) {
                                photo.setTotalCount(totalCountInt);

                                int addIndex = getIndexFromList(photo);

                                topLikePhotos.add(addIndex, photo);

                                mAdapter.notifyItemInserted(addIndex);

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void getDataDone() {
        progress.setText(progress.getText() + " DONE SCANNING");

        PreferencesHelper.getInstance().setValue(LIST_TOP_LIKE, topLikePhotos);
    }

    private int getIndexFromList(Photo photo) {
        for (int i = 0; i < topLikePhotos.size(); i++) {
            if (photo.getTotalCount() > topLikePhotos.get(i).getTotalCount()) {
                return i;
            }
        }
        return topLikePhotos.size();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startScanning:
                photos.clear();
                topLikePhotos.clear();
                mAdapter.notifyDataSetChanged();
                progress.setTextColor(Color.RED);

                topLikeLast = getListFromPref();

                getAlbumSize();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        callbackManager.onActivityResult(requestCode, responseCode, intent);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    private List<Photo> getListFromPref() {
        Type listOfObjects = new TypeToken<List<Photo>>(){}.getType();

        String json = PreferencesHelper.getInstance().getStringValue(LIST_TOP_LIKE, "");
        List<Photo> list2 = new Gson().fromJson(json, listOfObjects);

        return list2;
    }

}
