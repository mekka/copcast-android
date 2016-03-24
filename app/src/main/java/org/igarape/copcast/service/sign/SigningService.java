package org.igarape.copcast.service.sign;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.igarape.copcast.R;
import org.igarape.copcast.promises.HttpPromiseError;
import org.igarape.copcast.promises.PromiseError;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.promises.Promise;
import org.igarape.copcast.promises.PromisePayload;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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


    public static void loadIDs(Context ctx) throws SigningServiceException {
        TelephonyManager mTelephonyMgr = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = mTelephonyMgr.getDeviceId();
        String simno = mTelephonyMgr.getSimSerialNumber();

        if (imei == null)
            throw new SigningServiceException("IMEI is NULL");

        if (simno == null)
            throw new SigningServiceException("SimID is NULL");

        Globals.setImei(imei);
        Globals.setSimid(simno);
        ILog.d(TAG, "IMEI: "+imei);
        ILog.d(TAG, "SimID: "+simno);
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

            ILog.d(TAG, "older Android versions.");

            try {
                g = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
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
                    .setSerialNumber(BigInteger.TEN)
                    .setStartDate(notBefore.getTime())
                    .setEndDate(notAfter.getTime())
                    .setSubject(new X500Principal("CN=copcast"));

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

            Log.d(TAG, "Android 23");
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

        ILog.d(TAG, g.toString());

        KeyPair pair;
        try {
            pair = g.generateKeyPair();
        } catch (Exception e) {
            ctx.startActivity(new Intent("com.android.credentials.UNLOCK"));
            return "falhou" ;
        }

        return Base64.encodeToString(pair.getPublic().getEncoded(), Base64.DEFAULT);
    }

    public static String signature(JSONObject object) throws SigningServiceException {
        String input = object.toString();
        Log.d(TAG, "MD5: " + new String(Hex.encodeHex(DigestUtils.md5(input))));
        return  signature(input);
    }

    public static String signature(String input) throws SigningServiceException {

        KeyStore.Entry entry = fetchKey();

        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.w(TAG, "Not an instance of a PrivateKeyEntry");
            return null;
        }

        String algo = "SHA256WITHRSA";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            algo = "SHA256WITHECDSA";
        }

        Signature s = null;
        try {
            s = Signature.getInstance(algo);
        } catch (NoSuchAlgorithmException e) {
            ILog.e(TAG, "Unknown algorithm "+algo+" (sign)", e);
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
//        String imsi = mTelephonyMgr.getSubscriberId();
//        String imei = mTelephonyMgr.getDeviceId();
//        String simno = mTelephonyMgr.getSimSerialNumber();
//        Globals.setImei(imei);
//        Globals.setSimid(imsi);

        JSONObject register = new JSONObject();
        try {
            register.put("public_key", pubkey);
            register.put("username", username);
            register.put("password", pwd);
            register.put("imei", Globals.getImei());
            register.put("simid", Globals.getSimid());
        } catch (JSONException e) {
            ILog.e(TAG, "Could not write public key in JSON");
        }


        Log.d(TAG, ">> "+url);

        Log.d(TAG, "Registration posting: "+(System.currentTimeMillis()-time));
        NetworkUtils.postToServer(ctx, url, "/registration", register, new Promise() {

            public void error(PromiseError error) {
                switch ((HttpPromiseError) error) {
                    case NOT_AUTHORIZED:
                        promise.error(ctx.getString(R.string.unauthorized_login));
                        break;
                    case FORBIDDEN:
                        promise.error(ctx.getString(R.string.server_error));
                        break;
                    case NO_CONNECTION:
                    case BAD_CONNECTION:
                    case BAD_REQUEST:
                    case BAD_RESPONSE:
                    case FAILURE:
                        promise.error(ctx.getString(R.string.server_error));
                        break;
                }
            }

            @Override
            public void success(PromisePayload promisePayload) {
                promise.success();
            }
        });
        Log.d(TAG, "Registration sent: " + (System.currentTimeMillis() - time));
    }

}
