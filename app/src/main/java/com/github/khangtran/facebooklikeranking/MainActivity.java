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
import android.support.v7.widget.ScrollingTabContainerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import java.util.Arrays;
import java.util.List;

import khangtran.preferenceshelper.PreferencesHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CallbackManager callbackManager;
    private TextView tvTotalPhoto;
    private TextView progress;
    private TextView albumInfo;
    private EditText edtAlbumUrl;
    private EditText edtTopLike;
    private LinearLayout lnLogin;
    private LinearLayout lnMain;
    private int totalCountPhoto;
    private String top99PhotosPath = "/photos?limit=99";

    private List<Photo> photos = new ArrayList<>();
    private List<Photo> topLikePhotos = new ArrayList<>();
    private List<Photo> topLikeLast = new ArrayList<>();

    private RecyclerView recyclerView;
    private TopLikeAdapter mAdapter;
    private Button btnStartScanning;

    private int topLike;

    private static final String LIST_TOP_LIKE = "LIST_TOP_LIKE";
    private static final String ALBUM_URL = "ALBUM_URL";
    private static final String NUMBER_TOP_LIKE = "NUMBER_TOP_LIKE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesHelper.initHelper(this);
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_main);
        callbackManager = CallbackManager.Factory.create();

        tvTotalPhoto = findViewById(R.id.total_photo);
        progress = findViewById(R.id.progress);
        albumInfo = findViewById(R.id.album_info);
        btnStartScanning = findViewById(R.id.startScanning);
        edtAlbumUrl = findViewById(R.id.edt_album_url);
        edtTopLike = findViewById(R.id.edt_top);

        lnLogin = findViewById(R.id.ln_login);
        lnMain = findViewById(R.id.ln_main_view);

        if (PreferencesHelper.getInstance().contain(ALBUM_URL)) {
            edtAlbumUrl.setText(PreferencesHelper.getInstance().getStringValue(ALBUM_URL, ""));
        }
        if (PreferencesHelper.getInstance().contain(NUMBER_TOP_LIKE)) {
            edtTopLike.setText(PreferencesHelper.getInstance().getIntValue(NUMBER_TOP_LIKE, 0) + "");
        }


        checkLoginToken();

        FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        if (!edtAlbumUrl.getText().toString().equals("")) {
                            getAlbumSize();
                        }
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, first_name, last_name, email, birthday, gender , location");
                request.setParameters(parameters);
                request.executeAsync();

                lnLogin.setVisibility(View.GONE);
                lnMain.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {
                e.printStackTrace();
            }
        };

        LoginButton loginButton = findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");
        loginButton.registerCallback(callbackManager, callback);


        recyclerView = findViewById(R.id.recycler_view);

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

    private void setAlreadyLogin(boolean isLoggedIn) {
        lnLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        lnMain.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkLoginToken();
    }

    private void checkLoginToken() {
        AccessToken token = AccessToken.getCurrentAccessToken();
        if (isNetworkConnected()) {
            if (token != null) {
                btnStartScanning.setEnabled(true);
            }

            setAlreadyLogin(token != null);
        } else {
            btnStartScanning.setEnabled(false);
            Toast.makeText(this, "No internet connection !", Toast.LENGTH_LONG).show();
        }
    }


    public void getAlbumSize() {
        final String albumId = getAlbumIdFromURL();
        if (albumId.equals("")) return;

        Bundle parameters = new Bundle();
        parameters.putString("fields", "count");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + albumId,
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        if (response.getError() == null) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.getRawResponse());
                                String count = jsonObject.getString("count");
                                String id = jsonObject.getString("id");

                                totalCountPhoto = Integer.parseInt(count);
                                tvTotalPhoto.setText("Total count: " + count);

                                getLikeCount("/" + albumId + "/" + top99PhotosPath);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            tvTotalPhoto.setText(response.getError().getErrorMessage());
                        }

                    }
                }
        ).executeAsync();
    }

    public void getLikeCount(final String urlPath) {
        Bundle parameters = new Bundle();
        parameters.putString("limit", "50");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                urlPath,
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response.getRawResponse());
                            JSONArray dataObj = jsonObject.getJSONArray("data");

                            for (int i = 0; i < dataObj.length(); i++) {
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
                                    getLikeCount(urlPath + path);
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
                            if (totalCountInt > topLike) {
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
//        progress.setText(progress.getText() + " DONE SCANNING");

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
                if (topLikeLast != null && topLikeLast.size() > 0) {
                    mAdapter.updateLastLike(topLikeLast);
                }
                topLike = Integer.parseInt(edtTopLike.getText().toString());
                PreferencesHelper.getInstance().setValue(NUMBER_TOP_LIKE, topLike);
                PreferencesHelper.getInstance().setValue(ALBUM_URL, edtAlbumUrl.getText().toString());

                getAlbumSize();
                break;
        }
    }

    // https://www.facebook.com/pg/aeonmalltanphuceladon/photos/?tab=album&album_id=1126955914148390
    private String getAlbumIdFromURL() {
        String url = edtAlbumUrl.getText().toString();
        if (!url.isEmpty()) {
            if (url.contains("album_id=")) {
                int from = url.indexOf("album_id=");
                String albumId = url.substring(from+9);
                return albumId;
            } else if (url.contains("a.")) {
                int from = url.indexOf("a.");

                String idContain = url.substring(from);

                String[] words = idContain.split("\\.");
                return words[1];
            }
        }
        return "";
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
        Type listOfObjects = new TypeToken<List<Photo>>() {
        }.getType();

        String json = PreferencesHelper.getInstance().getStringValue(LIST_TOP_LIKE, "");
        List<Photo> list2 = new Gson().fromJson(json, listOfObjects);

        return list2;
    }

}
