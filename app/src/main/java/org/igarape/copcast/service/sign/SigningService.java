package org.igarape.copcast.service.sign;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.TextView;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.Promise;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Calendar;

import javax.security.auth.x500.X500Principal;

/**
 * Created by martelli on 1/21/16.
 */

public class SigningService {

    private static String TAG = SigningService.class.getCanonicalName();
    private static final String COPCASTKEY = "CopcastKey";

    private static KeyStore loadKeyStore() throws SigningServiceException {

        KeyStore ks;
        String errorMsg;

        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            errorMsg = "Could not get keystore instance";
            ILog.e(TAG, errorMsg, e);
            throw new SigningServiceException(errorMsg);
        }

        try {
            ks.load(null);
            return ks;
        } catch (Exception e) {
            errorMsg = "Could not load keys from keystore";
            ILog.e(TAG, errorMsg, e);
            throw new SigningServiceException(errorMsg);
        }
    }

    public static KeyStore.Entry fetchKey() throws SigningServiceException {

        KeyStore ks = loadKeyStore();
        String errorMsg;

        try {
            return ks.getEntry(COPCASTKEY, null);
        } catch (Exception e) {
            errorMsg = "Could not load COPCAST key from keystore";
            ILog.i(TAG, errorMsg, e);
            return null;
        }
    }


    public static void removeKey() throws SigningServiceException {

        KeyStore ks = loadKeyStore();

        try {
            ks.deleteEntry(COPCASTKEY);
            ILog.d(TAG, "copcastkey deleted");
        } catch (KeyStoreException e) {
            ILog.e(TAG, "Unable to delete Android Keystore (delete)", e);
        }

    }

    public static String init(Context ctx) throws SigningServiceException {

        KeyPairGenerator g = null;

        // With API 23 we can use Elliptic Curve cryptography.
        // API versions from 18 to 22 we use RSA.
        // For versions 17 and below the Keystore provider was non-existent.


        // we should never get here, but if someone lowers the minSDK value...
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)) {
            throw new SigningServiceException("Invalid Android version: "+Build.VERSION.SDK_INT);

        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            try {
                g = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            } catch (Exception e) {
                String errmsg = "Error getting AndroidKeyStore instance";
                ILog.e(TAG, errmsg, e);
                throw new SigningServiceException(errmsg);
            }

            Calendar notBefore = Calendar.getInstance();
            Calendar notAfter = Calendar.getInstance();
            notAfter.add(Calendar.YEAR, 1);
            KeyPairGeneratorSpec.Builder spec = new KeyPairGeneratorSpec.Builder(ctx)
                    .setAlias(COPCASTKEY)
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(notBefore.getTime())
                    .setEndDate(notAfter.getTime())
                    .setSubject(new X500Principal("CN=test"));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    spec.setKeyType(KeyProperties.KEY_ALGORITHM_RSA);
                } catch (NoSuchAlgorithmException e) {
                    String errmsg = "No such algorithm exception";
                    ILog.e(TAG, errmsg, e);
                    throw new SigningServiceException(errmsg);
                }
                spec.setKeySize(2048);
            }

            try {
                g.initialize(spec.build());
            } catch (InvalidAlgorithmParameterException e) {
                String errmsg = "Invalid algorithm parameters exception";
                ILog.e(TAG, errmsg, e);
                throw new SigningServiceException(errmsg);
            }

        } else {

            Log.d(TAG, "ANDROID 23");
            try {
                g = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            } catch (Exception e) {
                String errmsg = "No such algorithm exception";
                ILog.e(TAG, errmsg, e);
                throw new SigningServiceException(errmsg);
            }
            try {
                g.initialize(new KeyGenParameterSpec.Builder(
                        COPCASTKEY,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setDigests(KeyProperties.DIGEST_SHA256,
                                KeyProperties.DIGEST_SHA512)
                        .build());
            } catch (InvalidAlgorithmParameterException e) {
                String errmsg = "Invalid algorithm parameters exception";
                ILog.e(TAG, errmsg, e);
                throw new SigningServiceException(errmsg);
            }

        }

        KeyPair pair = g.generateKeyPair();
        // Instance of signature class with SHA256withECDSA algorithm
//            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
//            ecdsaSign.initSign(pair.getPrivate());


//            KeyPair pair = g.generateKeyPair();
        // Instance of signature class with SHA256withECDSA algorithm
//            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
//            ecdsaSign.initSign(pair.getPrivate());


        Log.e(TAG, "Private Keys is::" + pair.getPublic().getAlgorithm());
        Log.e(TAG, "Public Keys is::" + Base64.encodeToString(pair.getPublic().getEncoded(), Base64.DEFAULT));

//            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
//            ks.load(null);
//            KeyStore.Entry entry = ks.getEntry(COPCASTKEY, null);
//            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
//                Log.w(TAG, "Not an instance of a PrivateKeyEntry");
//                return null;
//            }

//            Signature s = Signature.getInstance("SHA256withECDSA");
//            s.initSign(((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
//            s.update("BOGUS".getBytes());
//            byte[] signature = s.sign();
//
//            Log.e(TAG, "Signature is::" + Base64.encodeToString(signature, Base64.DEFAULT));

        return Base64.encodeToString(pair.getPublic().getEncoded(), Base64.DEFAULT);

//            String msg = "text ecdsa with sha256";//getSHA256(msg)
//            ecdsaSign.update((msg + pair.getPrivate().toString())
//                    .getBytes("UTF-8"));
//
//            byte[] signature = ecdsaSign.sign();
//            System.out.println("Signature is::"
//                    + new BigInteger(1, signature).toString(16));
//
//            // Validation
//            ecdsaSign.initVerify(pair.getPublic());
//            ecdsaSign.update(signature);
//            if (ecdsaSign.verify(signature))
//                System.out.println("valid");
//            else
//                System.out.println("invalid!!!!");
    }

    public static String signature(JSONObject object) throws SigningServiceException {
        String input = object.toString();
        return  signature(input);
    }

    public static String signature(String input) throws SigningServiceException {

        KeyStore.Entry entry = fetchKey();

        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.w(TAG, "Not an instance of a PrivateKeyEntry");
            return null;
        }

        Signature s = null;
        try {
            s = Signature.getInstance("SHA256WITHRSA");
        } catch (NoSuchAlgorithmException e) {
            ILog.e(TAG, "Unknown algorithm SHA256WITHRSA (sign)", e);
        }
        try {
            s.initSign(((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
        } catch (InvalidKeyException e) {
            ILog.e(TAG, "Invalid key (sign)", e);
        }
        try {
            s.update(input.getBytes());
            byte[] signature = s.sign();
            return Base64.encodeToString(signature, Base64.DEFAULT);
        } catch (SignatureException e) {
            ILog.e(TAG, "Unable to sign data", e);
        }

        return null;
    }

    public static void registration(final Context ctx, final String url, String username, String pwd, final Promise promise) throws SigningServiceException {
        Long time = System.currentTimeMillis();
        Log.d(TAG, "Registration started");
        String pubkey = init(ctx);
        TelephonyManager mTelephonyMgr = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = mTelephonyMgr.getSubscriberId();
        String imei = mTelephonyMgr.getDeviceId();
        String simno = mTelephonyMgr.getSimSerialNumber();

        JSONObject register = new JSONObject();
        try {
            register.put("public_key", pubkey);
            register.put("username", username);
            register.put("password", pwd);
            register.put("imei", imei);
            register.put("simid", simno);
            register.put("subsid", imsi);
        } catch (JSONException e) {
            ILog.e(TAG, "Could not write public key in JSON");
        }

        Log.d(TAG, ">> "+url);

        Log.d(TAG, "Registration posting: "+(System.currentTimeMillis()-time));
        NetworkUtils.post(ctx, url, "/registration", register, new HttpResponseCallback() {
            @Override
            public void unauthorized() {
                Log.d(TAG, "unauthorized");
                promise.failure(ctx.getString(R.string.unauthorized_login));
            }

            @Override
            public void failure(int statusCode) {
                Log.d(TAG, "failure");
                promise.failure(ctx.getString(R.string.server_error));
            }

            @Override
            public void noConnection() {
                Log.d(TAG, "no connection");
                promise.failure(ctx.getString(R.string.server_error));
            }

            @Override
            public void badConnection() {
                Log.d(TAG, "bad connection");
                promise.failure(ctx.getString(R.string.server_error));
            }

            @Override
            public void badRequest() {
                Log.d(TAG, "bad request");
                promise.failure(ctx.getString(R.string.server_error));
            }

            @Override
            public void badResponse() {
                Log.d(TAG, "bad response");
                promise.failure(ctx.getString(R.string.server_error));
            }

            @Override
            public void success(JSONObject response) {
                if (response != null)
                    Log.d(TAG, response.toString());
                promise.success(url);
            }
        });
        Log.d(TAG, "Registration sent: " + (System.currentTimeMillis()-time));
    }

}
