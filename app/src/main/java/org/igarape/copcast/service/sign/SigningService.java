package org.igarape.copcast.service.sign;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Message;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import javax.security.auth.x500.X500Principal;

/**
 * Created by martelli on 1/21/16.
 */
public class SigningService {

    private static String TAG = SigningService.class.getCanonicalName();
    private static final String COPCASTKEY = "CopcastKey";
    KeyPairGenerator keyPairGenerator;


    public static void check(final Activity ctx) {

        KeyStore ks = null;
        KeyStore.Entry entry = null;

        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            ILog.e(TAG, "Could not get keystore", e);
        }

        try {
            ks.load(null);
        } catch (Exception e) {
            ILog.e(TAG, "Could not load keys from keystore", e);
        }

        try {
            entry = ks.getEntry(COPCASTKEY, null);
        } catch (Exception e) {
            ILog.e(TAG, "Could not load COPCAST key from keystore", e);
        }

        // COPCAST key is in place. Nothing else to do.
        if (entry != null)
            return;


        final AlertDialog configDialog = new AlertDialog.Builder(new ContextThemeWrapper(ctx, R.style.AlertDialogCustom)).create();

        LayoutInflater inflater = ctx.getLayoutInflater();
        configDialog.setView(inflater.inflate(R.layout.register_signin, null));
        configDialog.setButton(DialogInterface.BUTTON_POSITIVE, ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String url = ((TextView) configDialog.findViewById(R.id.register_url)).getText().toString();
                String username = ((TextView) configDialog.findViewById(R.id.register_username)).getText().toString();
                String pwd = ((TextView) configDialog.findViewById(R.id.register_password)).getText().toString();

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

                NetworkUtils.post(ctx, url, "/registration", register, new HttpResponseCallback() {
                    @Override
                    public void unauthorized() {

                    }

                    @Override
                    public void failure(int statusCode) {

                    }

                    @Override
                    public void noConnection() {

                    }

                    @Override
                    public void badConnection() {

                    }

                    @Override
                    public void badRequest() {

                    }

                    @Override
                    public void badResponse() {

                    }

                    @Override
                    public void success(JSONObject response) {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
                        SharedPreferences.Editor edit = sharedPref.edit();
                        edit.putString(Globals.SERVER_URL, url);
                        edit.commit();

                        Log.d(TAG, response.toString());
                    }
                });

            }
        });

        configDialog.show();
    }

    public static String init(final Activity ctx) {

        try {

            KeyPairGenerator g = null;

            // With API 23 we can use Elliptic Curve cryptography.
            // API versions from 18 to 22 we use RSA.
            // For versions 17 and below the Keystore provider was non-existent.


            // we should never get here, but if someone lowers the minSDK value...
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)) {
                AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(ctx, R.style.AlertDialogCustom)).create();
                alertDialog.setTitle(ctx.getString(R.string.warning));
                alertDialog.setMessage(ctx.getString(R.string.incompatible_version));
                alertDialog.setIcon(R.drawable.button_default);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.w(TAG, "ciente!");
                        ctx.finish();
                    }
                });
                alertDialog.show();
                return null;

            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

                g = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");

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
                    spec.setKeyType(KeyProperties.KEY_ALGORITHM_RSA);
                    spec.setKeySize(2048);
                }

                g.initialize(spec.build());

            } else {

                Log.d(TAG, "ANDROID 23");
                g = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                g.initialize(new KeyGenParameterSpec.Builder(
                        COPCASTKEY,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setDigests(KeyProperties.DIGEST_SHA256,
                                KeyProperties.DIGEST_SHA512)
                        .build());

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

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return null;
    }


    public static String signed(String input) {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            ILog.e(TAG, "Unable to obtain Android Keystore (sign)", e);
        }

        try {
            ks.load(null);
        } catch (Exception e) {
            ILog.e(TAG, "Unable to load Android Keystore (sign)", e);
        }

        KeyStore.Entry entry = null;

        try {
            entry = ks.getEntry(COPCASTKEY, null);
        } catch (Exception e) {
            ILog.e(TAG, "Unable to obtain COPCAST key from Android Keystore (sign)", e);
        }

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
}
