package com.example.projectdonasi.Connecter;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import org.json.JSONObject;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloudinaryHelper {
    public interface OnUploadCompleteListener {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    public static void uploadImage(@NonNull Context context, @NonNull Uri gambarUri, @NonNull OnUploadCompleteListener listener) {
        new Thread(() -> {
            try {
                String filePath = FileUtils.getPath(context, gambarUri);
                if (filePath == null) {
                    runOnMainThread(context, () -> listener.onFailure("Gagal mengambil file"));
                    return;
                }

                String cloudName = "dmghuocd4";
                String apiKey = "643913637346459";
                String apiSecret = "UuUaTN6bowCvSWnrPz3VLA1KGtI";
                String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
                long timestamp = System.currentTimeMillis() / 1000;
                String signatureRaw = "timestamp=" + timestamp + apiSecret;
                String signature = sha1(signatureRaw);

                MultipartBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", new File(filePath).getName(),
                                RequestBody.create(new File(filePath), MediaType.parse("image/*")))
                        .addFormDataPart("api_key", apiKey)
                        .addFormDataPart("timestamp", String.valueOf(timestamp))
                        .addFormDataPart("signature", signature)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                OkHttpClient client = new OkHttpClient();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String imageUrl = json.getString("secure_url");
                    runOnMainThread(context, () -> listener.onSuccess(imageUrl));
                } else {
                    runOnMainThread(context, () -> listener.onFailure("Gagal upload gambar ke Cloudinary"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnMainThread(context, () -> listener.onFailure("Error: " + e.getMessage()));
            }
        }).start();
    }

    private static void runOnMainThread(Context context, Runnable runnable) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(runnable);
    }

    private static String sha1(String input) {
        try {
            java.security.MessageDigest mDigest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] result = mDigest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
