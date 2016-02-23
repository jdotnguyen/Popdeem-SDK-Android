/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Popdeem
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.popdeem.sdk.core.api;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jakewharton.retrofit.Ok3Client;
import com.popdeem.sdk.core.PopdeemSDK;
import com.popdeem.sdk.core.api.response.PDBasicResponse;
import com.popdeem.sdk.core.deserializer.PDBrandsDeserializer;
import com.popdeem.sdk.core.deserializer.PDFeedsDeserializer;
import com.popdeem.sdk.core.deserializer.PDRewardsDeserializer;
import com.popdeem.sdk.core.exception.PopdeemSDKNotInitializedException;
import com.popdeem.sdk.core.model.PDBrand;
import com.popdeem.sdk.core.model.PDFeed;
import com.popdeem.sdk.core.model.PDReward;
import com.popdeem.sdk.core.utils.PDUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedString;

/**
 * PDAPIClient is used to access the Popdeem API.
 * You access this class using the {@link #instance()}.
 * <p>
 * Created by mikenolan on 18/02/16.
 * </p>
 */
public class PDAPIClient {

    /**
     * {@link Interceptor} for adding Popdeem API Key to request headers.
     */
    private static final Interceptor PD_API_KEY_INTERCEPTOR = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            return chain.proceed(chain.request().newBuilder()
                    .addHeader(PDAPIConfig.REQUEST_HEADER_API_KEY, PopdeemSDK.getPopdeemAPIKey())
                    .build());
        }
    };


    private static Interceptor PD_USER_TOKEN_INTERCEPTOR = null;
    private static String userToken = null;


    /**
     * Private empty constructor to stop instances being created using "new"
     */
    private PDAPIClient() {
    }


    /**
     * Create an instance of {@link PDAPIClient} to access Popdeem API.
     * <p>
     * Popdeem SDK must be initialized using {@link PopdeemSDK#initializeSDK(Application)} and your Popdeem API Key must be present in your Application's AndroidManifest.xml before creating an instance of {@link PDAPIClient}.
     * </p>
     *
     * @return instance of {@link PDAPIClient}
     */
    public static PDAPIClient instance() {
        if (!PopdeemSDK.isPopdeemSDKInitialized()) {
            throw new PopdeemSDKNotInitializedException("Popdeem SDK is not initialized. Be sure to call PopdeemSDK.initializeSDK(Application application) in your Application class before using the SDK.");
        }
        PDUtils.validateAPIKeyIsPresent();
        if (userToken == null) {
            userToken = PDUtils.getUserToken();
        }
        return new PDAPIClient();
    }


    /**
     * Create a Non Social User. This user will be updated upon a social login.
     *
     * @param uid         Unique Identifier for user
     * @param deviceToken Device Token for GCM Push
     * @param callback    {@link PDAPICallback} for API result
     */
    public void createNonSocialUser(@NonNull String uid, @NonNull String deviceToken, @NonNull final PDAPICallback<PDBasicResponse> callback) {
        PopdeemAPI api = getApiInterface(null, null);
        api.createNonSocialUser("", uid, deviceToken, PDAPIConfig.PLATFORM_VALUE, callback);
    }


    /**
     * Register a user using the Facebook Access Token and Facebook App ID from Login
     * <p>
     * If user is a new user, then a new user will be created
     * If not, user will be loaded
     * In both cases response is a User object
     * </p>
     *
     * @param context             Application {@link Context}
     * @param facebookAccessToken Access Token {@link String} received on successful Facebook login
     * @param facebookUserID      Facebook Application ID received on successful Facebook login
     * @param callback            {@link PDAPICallback} for API result
     */
    public void registerUserWithFacebookAccessToken(@NonNull final Context context, @NonNull final String facebookAccessToken,
                                                    @NonNull final String facebookUserID, @NonNull final PDAPICallback<String> callback) {
//        PDDataManager.setFacebookAccessTokenProperty(context, facebookAccessToken);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(PDAPIConfig.PD_API_ENDPOINT + PDAPIConfig.PD_USERS_PATH);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty(PDAPIConfig.REQUEST_HEADER_API_KEY, PopdeemSDK.getPopdeemAPIKey());

                    ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
                    params.add(new AbstractMap.SimpleEntry<>("user[facebook][access_token]", facebookAccessToken));
                    params.add(new AbstractMap.SimpleEntry<>("user[facebook][id]", facebookUserID));

                    StringBuilder result = new StringBuilder();
                    boolean first = true;

                    for (AbstractMap.SimpleEntry<String, String> map : params) {
                        if (first) {
                            first = false;
                        } else {
                            result.append("&");
                        }

                        result.append(URLEncoder.encode(map.getKey(), "UTF-8"));
                        result.append("=");
                        result.append(URLEncoder.encode(map.getValue(), "UTF-8"));
                    }

                    OutputStream os = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(result.toString());
                    writer.flush();
                    writer.close();
                    os.close();

                    connection.connect();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    final StringBuilder response = new StringBuilder();

                    while ((inputLine = reader.readLine()) != null) {
                        response.append(inputLine);
                    }

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.success(response.toString());
                        }
                    });
                } catch (final IOException e) {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.failure(400, e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }


    /**
     * @param userID
     * @param userToken
     * @param userSecret
     * @param callback   {@link PDAPICallback} for API result
     */
    public void connectWithTwitterAccount(@NonNull String userID, @NonNull String userToken, @NonNull String userSecret, @NonNull final PDAPICallback<JsonObject> callback) {
//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(PDAPIConfig.REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        JsonObject twitterObject = new JsonObject();
        twitterObject.addProperty("social_id", userID);
        twitterObject.addProperty("access_token", userToken);
        twitterObject.addProperty("access_secret", userSecret);

        JsonObject userJson = new JsonObject();
        userJson.add("twitter", twitterObject);

        JsonObject json = new JsonObject();
        json.add("user", userJson);

        TypedInput body = new TypedByteArray("application/json", json.toString().getBytes());

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), null);
        api.connectWithTwitterAccount(body, callback);
    }


    /**
     * Update the Users Location and Device token
     *
     * @param id          - User ID
     * @param deviceToken - Device token
     * @param latitude    - Current latitude for user
     * @param longitude   - Current longitude for user
     * @param callback    {@link PDAPICallback} for API result
     * @deprecated user {@link #updateUserLocationAndDeviceToken(Context, String, String, String, String, PDAPICallback)} instead.
     */
    @Deprecated
    public void updateUserLocationAndDeviceToken(@NonNull String id, @NonNull String deviceToken,
                                                 @NonNull String latitude, @NonNull String longitude,
                                                 @NonNull PDAPICallback<JsonObject> callback) {
//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), null);
        api.updateUserLocationAndDeviceToken(id, PDAPIConfig.PLATFORM_VALUE, deviceToken, latitude, longitude, callback);
    }


    /**
     * Update the Users Location and Device token.
     *
     * @param context     Application {@link Context}
     * @param id          User ID
     * @param deviceToken Device token
     * @param latitude    Current latitude for user
     * @param longitude   Current longitude for user
     * @param callback    {@link PDAPICallback} for API result
     */
    public void updateUserLocationAndDeviceToken(@NonNull final Context context, @NonNull final String id, @NonNull final String deviceToken,
                                                 @NonNull final String latitude, @NonNull final String longitude,
                                                 @NonNull final PDAPICallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(PDAPIConfig.PD_API_ENDPOINT + PDAPIConfig.PD_USERS_PATH + "/" + id);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestMethod("PUT");
                    connection.setConnectTimeout(15000);
                    connection.setRequestProperty(PDAPIConfig.REQUEST_HEADER_API_KEY, PopdeemSDK.getPopdeemAPIKey());
                    connection.setRequestProperty(PDAPIConfig.REQUEST_HEADER_USER_TOKEN, userToken);

                    ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
                    params.add(new AbstractMap.SimpleEntry<>("user[platform]", PDAPIConfig.PLATFORM_VALUE));
                    params.add(new AbstractMap.SimpleEntry<>("user[device_token]", deviceToken));
                    params.add(new AbstractMap.SimpleEntry<>("user[latitude]", latitude));
                    params.add(new AbstractMap.SimpleEntry<>("user[longitude]", longitude));

                    StringBuilder result = new StringBuilder();
                    boolean first = true;

                    for (AbstractMap.SimpleEntry<String, String> map : params) {
                        if (first) {
                            first = false;
                        } else {
                            result.append("&");
                        }

                        result.append(URLEncoder.encode(map.getKey(), "UTF-8"));
                        result.append("=");
                        result.append(URLEncoder.encode(map.getValue(), "UTF-8"));
                    }

                    OutputStream os = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(result.toString());
                    writer.flush();
                    writer.close();
                    os.close();

                    connection.connect();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    final StringBuilder response = new StringBuilder();

                    while ((inputLine = reader.readLine()) != null) {
                        response.append(inputLine);
                    }

                    callback.success(response.toString());
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.success(response.toString());
                        }
                    });
                } catch (final IOException e) {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.failure(400, e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }


    /**
     * Get details for User ID
     * (Method will be public once it is tested)
     *
     * @param id       User ID
     * @param callback {@link PDAPICallback} for API result
     */
    private void getUserDetailsForId(@NonNull String id, @NonNull PDAPICallback<JsonObject> callback) {
//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), null);
        api.getUserDetailsForId(id, callback);
    }


    /**
     * Register user with Twitter
     * (Method will be public once it is tested)
     *
     * @param twitterAccessToken  - Twitter access token
     * @param twitterAccessSecret - Twitter access  secret
     * @param twitterID           - Twitter ID
     * @param screenName          - Twitter user screen name
     * @param callback            {@link PDAPICallback} for API result
     */
    private void registerUserwithTwitterParams(@NonNull String twitterAccessToken, @NonNull String twitterAccessSecret,
                                               @NonNull String twitterID, @NonNull String screenName,
                                               @NonNull PDAPICallback<JsonObject> callback) {
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor();

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), null);
        api.registerUserWithTwitterParams(twitterAccessToken, twitterAccessSecret, twitterID, screenName, callback);
    }


    /**
     * Get Popdeem Friends
     * (Method will be public once it is tested)
     *
     * @param id
     * @param callback {@link PDAPICallback} for API result
     */
    private void getPopdeemFriends(@NonNull String id, @NonNull PDAPICallback<JsonObject> callback) {
//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), null);
        api.getPopdeemFriends(id, callback);
    }


    /**
     * Get Brands
     *
     * @param callback {@link PDAPICallback} for API result
     */
    public void getBrands(@NonNull PDAPICallback<ArrayList<PDBrand>> callback) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PDBrandsDeserializer.BRANDS_TYPE, new PDBrandsDeserializer())
                .create();

//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), new GsonConverter(gson));
        api.getBrands(callback);
    }


    /**
     * @param brandID
     * @param callback {@link PDAPICallback} for API result
     */
    public void getRewardsForBrandID(@NonNull String brandID, @NonNull PDAPICallback<ArrayList<PDReward>> callback) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PDRewardsDeserializer.REWARDS_TYPE, new PDRewardsDeserializer())
                .create();

//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), new GsonConverter(gson));
        api.getRewardsForBrandID(brandID, callback);
    }


    /**
     * Get all rewards
     * <p>
     * Request all rewards which are relevant to user.
     * </p>
     *
     * @param callback {@link PDAPICallback} for API result
     */
    public void getAllRewards(@NonNull final PDAPICallback<ArrayList<PDReward>> callback) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PDRewardsDeserializer.REWARDS_TYPE, new PDRewardsDeserializer())
                .create();

//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), new GsonConverter(gson));
        api.getAllRewards(callback);
    }


    /**
     * Claim a reward and post social action
     *
     * @param context             - @NonNull Application {@link Context}
     * @param facebookAccessToken - @NonNull Facebook Access Token {@link String}
     * @param rewardId            - Reward ID {@link String}
     * @param message             - Message {@link String}
     * @param taggedFriendsNames  - {@link String} {@link ArrayList} of tagged Friends.
     * @param taggedFriendsIds    - {@link String} {@link ArrayList} of tagged Friend IDs.
     * @param image               - {@link android.util.Base64} encoded {@link String}. The image data must be Base64 encoded. If left null, message will only be posted.
     * @param longitude           - Current longitude for user
     * @param latitude            - Current latitude for user
     * @param callback            {@link PDAPICallback} for API result
     */
    public void claimReward(@NonNull final Context context, final String facebookAccessToken, final String twitterAuthToken, final String twitterSecret,
                            @NonNull final String rewardId, @NonNull final String message, ArrayList<String> taggedFriendsNames, ArrayList<String> taggedFriendsIds,
                            final String image, @NonNull final String longitude, @NonNull final String latitude, @NonNull final PDAPICallback<JsonObject> callback) {
        final Handler mainHandler = new Handler(context.getMainLooper());
        final String url = PDAPIConfig.PD_API_ENDPOINT + PDAPIConfig.PD_REWARDS_PATH + "/" + rewardId + "/claim";

        final OkHttpClient client = new OkHttpClient();
        FormBody.Builder bodyBuilder = new FormBody.Builder()
                .add("message", message)
                .add("location[latitude]", latitude)
                .add("location[longitude]", longitude);

        if (facebookAccessToken != null) {
            bodyBuilder.add("facebook[access_token]", facebookAccessToken);
            if (taggedFriendsIds != null && taggedFriendsNames != null && taggedFriendsIds.size() == taggedFriendsNames.size() && taggedFriendsIds.size() > 0 && taggedFriendsNames.size() > 0) {
                for (int i = 0; i < taggedFriendsIds.size(); i++) {
                    String name = taggedFriendsNames.get(i);
                    String id = taggedFriendsIds.get(i);

                    bodyBuilder.add("facebook[associated_account_ids][][name]", name);
                    bodyBuilder.add("facebook[associated_account_ids][][id]", id);
                }
            }
        }
        if (twitterAuthToken != null) {
            bodyBuilder.add("twitter[access_token]", twitterAuthToken);
            bodyBuilder.add("twitter[access_secret]", twitterSecret);
        }

        if (image != null) {
            bodyBuilder.add("file", image);
        }

        RequestBody body = bodyBuilder.build();

        final Request request = new Request.Builder()
                .url(url)
                .addHeader(PDAPIConfig.REQUEST_HEADER_API_KEY, PopdeemSDK.getPopdeemAPIKey())
                .addHeader(PDAPIConfig.REQUEST_HEADER_USER_TOKEN, userToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.failure(RetrofitError.networkError(url, new IOException("Unexpected code " + e)));
            }

            @Override
            public void onResponse(Call call, final okhttp3.Response response) throws IOException {
                // Convert OkHTTP response to Retrofit Response
                final String responseBody = response.body().string();
                final JsonObject object = (JsonObject) new JsonParser().parse(responseBody);
                final TypedInput bodyTypedInput = new TypedString(responseBody);
                final ArrayList<Header> headers = new ArrayList<>();

                Headers responseHeaders = response.headers();
                for (int i = 0; i < responseHeaders.size(); i++) {
                    headers.add(new Header(responseHeaders.name(i), responseHeaders.value(i)));
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.success(object, new Response(response.request().url().toString(), response.networkResponse().code(), response.networkResponse().message(), headers, bodyTypedInput));
                    }
                });
            }
        });
    }


    /**
     * Redeem Reward
     * <p>
     * Redeem a reward in the wallet
     * </p>
     *
     * @param rewardId - Reward ID
     * @param callback {@link PDAPICallback} for API result
     */
    public void redeemReward(String rewardId, @NonNull PDAPICallback<JsonObject> callback) {
//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), null);
        api.redeemReward(rewardId, callback);
    }


    /**
     * Get rewards currently in wallet
     * <p>
     * Request all rewards which are relevant to user.
     * </p>
     *
     * @param callback {@link PDAPICallback} for API result
     */
    public void getRewardsInWallet(@NonNull final PDAPICallback<ArrayList<PDReward>> callback) {
        Gson gson = new GsonBuilder()
//                .registerTypeAdapter(rewardsType, new PDReward.PDRewardDeserializer())
                .registerTypeAdapter(PDRewardsDeserializer.REWARDS_TYPE, new PDRewardsDeserializer())
                .create();

//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), new GsonConverter(gson));
        api.getRewardsInWallet(callback);
    }


    /**
     * Get Popdeem Messages
     * (Method will be public once it is tested)
     *
     * @param callback {@link PDAPICallback} for API result
     */
    public void getPopdeemMessages(@NonNull PDAPICallback<JsonObject> callback) {
//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), null);
        api.getPopdeemMessages(callback);
    }


    /**
     * Get Feeds
     *
     * @param callback {@link PDAPICallback} for API result
     */
    public void getFeeds(@NonNull PDAPICallback<ArrayList<PDFeed>> callback) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PDFeedsDeserializer.FEEDS_TYPE, new PDFeedsDeserializer())
                .create();

//        PDRequestHeader userTokenRequestHeader = new PDRequestHeader(REQUEST_HEADER_USER_TOKEN, PDDataManager.getUserToken(context));
//        RequestInterceptor requestInterceptor = PDAPIClient.getUserTokenInterceptor(userTokenRequestHeader);

        PopdeemAPI api = getApiInterface(getUserTokenInterceptor(), new GsonConverter(gson));
        api.getFeeds("20", callback);
    }


    /**
     * Get User Token Interceptor
     *
     * @return Interceptor with User Token Header or null if no token is saved.
     */
    private Interceptor getUserTokenInterceptor() {
        if (PD_USER_TOKEN_INTERCEPTOR == null) {
            if (userToken == null) {
                userToken = PDUtils.getUserToken();
            }
            if (userToken != null) {
                PD_USER_TOKEN_INTERCEPTOR = new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        return chain.proceed(chain.request().newBuilder()
                                .addHeader(PDAPIConfig.REQUEST_HEADER_USER_TOKEN, userToken)
                                .build());
                    }
                };
            }
        }
        return PD_USER_TOKEN_INTERCEPTOR;
    }


    /**
     * Get Popdeem API interface
     *
     * @param interceptor - {@link Interceptor} used for request
     * @param converter   - {@link Converter} users for request response (optional)
     * @return - {@link PopdeemAPI} used for interacting with API
     */
    private PopdeemAPI getApiInterface(Interceptor interceptor, Converter converter) {
        RestAdapter restAdapter = buildRestAdapter(interceptor, converter);
        return restAdapter.create(PopdeemAPI.class);
    }


    /**
     * Build RestAdapter for request
     *
     * @param interceptor - {@link Interceptor} for request (optional)
     * @param converter   - {@link Converter} for Request (optional)
     * @return - {@link RestAdapter} used to create {@link PopdeemAPI}
     */
    private RestAdapter buildRestAdapter(Interceptor interceptor, Converter converter) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(PD_API_KEY_INTERCEPTOR);

        if (interceptor != null) {
            builder.addInterceptor(interceptor);
        }

        OkHttpClient okHttpClient = builder.build();

        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder()
                .setClient(new Ok3Client(okHttpClient))
                .setEndpoint(PDAPIConfig.PD_API_ENDPOINT)
                .setLogLevel(RestAdapter.LogLevel.FULL);

        if (converter != null) {
            adapterBuilder.setConverter(converter);
        }

        return adapterBuilder.build();
    }

}
