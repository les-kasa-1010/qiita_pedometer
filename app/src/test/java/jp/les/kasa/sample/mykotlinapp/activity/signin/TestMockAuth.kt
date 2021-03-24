package jp.les.kasa.sample.mykotlinapp.activity.signin

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.internal.firebase_auth.zzff
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.*
import jp.les.kasa.sample.mykotlinapp.data.LoginUserData
import jp.les.kasa.sample.mykotlinapp.utils.AuthProviderI

// FirebaseAuthを提供するプロバイダのテスト用
class MockAuthProvider(app: Application) : AuthProviderI(app) {
    override val user: FirebaseUser?
        get() {
            return if (mockFirebaseUser) MockFirebaseUser()
            else null
        }

    override val userData: LoginUserData
        get() {
            return LoginUserData(
                "ユーザー名",
                "foo@bar.com"
            )
        }

    var mockFirebaseUser = false

    override fun createSignInIntent(context: Context): Intent {
        return Intent(context, MockAuthUIActivity::class.java)
    }

    override fun signOut(context: Context): Task<Void?> {
        return Tasks.forResult(null)
    }

    override fun delete(context: Context): Task<Void?> {
        return Tasks.forResult(null)
    }
}

// FirebaseUserモック
class MockFirebaseUser : FirebaseUser() {
    override fun zzg(): String {
        TODO("Not yet implemented")
    }

    override fun zze(): zzff {
        TODO("Not yet implemented")
    }

    override fun getEmail(): String? {
        TODO("Not yet implemented")
    }

    override fun zzc(): FirebaseApp {
        TODO("Not yet implemented")
    }

    override fun zza(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun zza(p0: MutableList<out UserInfo>): FirebaseUser {
        TODO("Not yet implemented")
    }

    override fun zza(p0: zzff) {
        TODO("Not yet implemented")
    }

    override fun getProviderData(): MutableList<out UserInfo> {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun getMetadata(): FirebaseUserMetadata? {
        TODO("Not yet implemented")
    }

    override fun getMultiFactor(): MultiFactor {
        TODO("Not yet implemented")
    }

    override fun isAnonymous(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPhoneNumber(): String? {
        TODO("Not yet implemented")
    }

    override fun getUid(): String {
        TODO("Not yet implemented")
    }

    override fun isEmailVerified(): Boolean {
        TODO("Not yet implemented")
    }

    override fun zzf(): String {
        TODO("Not yet implemented")
    }

    override fun zzd(): String? {
        TODO("Not yet implemented")
    }

    override fun zzb(): FirebaseUser {
        TODO("Not yet implemented")
    }

    override fun zzb(p0: MutableList<MultiFactorInfo>?) {
        TODO("Not yet implemented")
    }

    override fun getDisplayName(): String? {
        TODO("Not yet implemented")
    }

    override fun getPhotoUrl(): Uri? {
        TODO("Not yet implemented")
    }

    override fun getProviderId(): String {
        TODO("Not yet implemented")
    }
}

class MockAuthUIActivity : AppCompatActivity()
