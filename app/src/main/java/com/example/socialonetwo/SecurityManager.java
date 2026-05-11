package com.example.socialonetwo;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

/**
 * Handles biometric authentication and related UI effects like blurring.
 */
public class SecurityManager {

    public interface AuthCallback {
        void onAuthenticated();
        void onAuthError(int errorCode, @NonNull CharSequence errString);
    }

    private final AppCompatActivity activity;

    public SecurityManager(AppCompatActivity activity) {
        this.activity = activity;
    }

    /**
     * Displays the biometric authentication prompt.
     */
    public void showBiometricPrompt(AuthCallback callback) {
        BiometricManager biometricManager = BiometricManager.from(activity);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // If biometric is not available or not enrolled, we don't lock the app
            // to avoid locking out the user.
            callback.onAuthenticated();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onAuthError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onAuthenticated();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("SocialOne Authentication")
                .setSubtitle("Authenticate to access the app")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Applies a blur effect to the given layout to obscure content.
     * @param layout The layout to blur.
     * @param apply True to apply the effect, false to remove it.
     */
    public void applyBlurEffect(View layout, boolean apply) {
        if (layout == null) return;

        if (apply) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                layout.setRenderEffect(RenderEffect.createBlurEffect(80f, 80f, Shader.TileMode.CLAMP));
            } else {
                layout.setAlpha(0.1f);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                layout.setRenderEffect(null);
            } else {
                layout.setAlpha(1.0f);
            }
        }
    }
}
