package com.haze.app.data.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed class FirebaseAuthResult {
    data class CodeSent(val verificationId: String) : FirebaseAuthResult()
    data class VerificationCompleted(val credential: PhoneAuthCredential) : FirebaseAuthResult()
    data class Failed(val errorMessage: String) : FirebaseAuthResult()
}

object FirebaseAuthHelper {
    private const val TAG = "FirebaseAuthHelper"
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun sendVerificationCode(
        activity: Activity,
        phoneNumberWithCountryCode: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: instant SMS verification")
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent: verificationId=$verificationId")
                onCodeSent(verificationId, token)
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumberWithCountryCode)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    suspend fun verifyCode(
        verificationId: String,
        code: String
    ): Result<Boolean> = suspendCancellableCoroutine { continuation ->
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithCredential: successful login uid=${auth.currentUser?.uid}")
                        continuation.resume(Result.success(true))
                    } else {
                        val error = task.exception?.localizedMessage ?: "Invalid or expired OTP. Please try again."
                        Log.e(TAG, "signInWithCredential failed: $error")
                        continuation.resume(Result.failure(Exception(error)))
                    }
                }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }
}
