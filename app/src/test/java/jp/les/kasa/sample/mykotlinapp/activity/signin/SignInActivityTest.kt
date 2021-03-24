package jp.les.kasa.sample.mykotlinapp.activity.signin

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdpResponse
import jp.les.kasa.sample.mykotlinapp.R
import jp.les.kasa.sample.mykotlinapp.ShadowAlertController
import jp.les.kasa.sample.mykotlinapp.ShadowAlertDialog
import jp.les.kasa.sample.mykotlinapp.di.mockModule
import jp.les.kasa.sample.mykotlinapp.shadowOfAlert
import jp.les.kasa.sample.mykotlinapp.utils.AuthProviderI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowView

@RunWith(AndroidJUnit4::class)
@Config(
    qualifiers = "xlarge-port",
    shadows = [ShadowAlertDialog::class, ShadowAlertController::class]
)
class SignInActivityTest : AutoCloseKoinTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val authProvider: AuthProviderI by inject()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private fun getString(resId: Int) = context.applicationContext.getString(resId)
    private fun getString(resId: Int, c: Int) = context.applicationContext.getString(resId, c)

    class TestRegistry(private val resultCode: Int, private val errorCode: Int) :
        ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            when (resultCode) {
                // @formatter:off
                Activity.RESULT_CANCELED -> dispatchResult(requestCode, Activity.RESULT_CANCELED, null)
                Activity.RESULT_OK -> dispatchResult(requestCode, Activity.RESULT_OK, null)
                // @formatter:on
                else -> {
                    val exception = FirebaseUiException(errorCode)
                    val resultIntent = IdpResponse.getErrorIntent(exception)
                    dispatchResult(requestCode, Activity.RESULT_CANCELED, resultIntent)
                }
            }
        }
    }

    @Before
    fun setUp() {
        loadKoinModules(mockModule)
    }

    @Test
    fun layout() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }

            // クラウドアイコン
            onView(withId(R.id.imageCloudUpload))
//            .check(matches(withDrawable(R.drawable.ic_cloud_upload_24dp))) // Tintカラー付けていると使えない
                .check(matches(ViewMatchers.isDisplayed()))
            // 文言
            onView(withText(R.string.text_sign_in_description))
                .check(matches(ViewMatchers.isDisplayed()))
            // ログインボタン
            onView(withText(R.string.label_sign_in))
                .check(matches(ViewMatchers.isDisplayed()))
        }
    }

    @Test
    fun signIn_Launch() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }

            // ResultActivityの起動を監視
            val monitor = Instrumentation.ActivityMonitor(
                MockAuthUIActivity::class.java.canonicalName, null, false
            )
            InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

            onView(withText(R.string.label_sign_in)).perform(click())

            // ResultActivityが起動したか確認
            InstrumentationRegistry.getInstrumentation().waitForMonitorWithTimeout(monitor, 1000L)
            assertThat(monitor.hits).isEqualTo(1)
        }
    }

    @Test
    fun signIn() {

        val testRegistry = TestRegistry(Activity.RESULT_OK, 0)
        // 追加のモジュール
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }

            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている
            // TODO 処理を入れたらその確認コードを書く
        }
    }

    /**
     *   ログイン中の場合にサインアウト画面がでるかのテスト
     */
    @Test
    fun moveToSignOut() {
        // モックを作成
        (authProvider as MockAuthProvider).mockFirebaseUser = true
        // ResultActivityの起動を監視
        val monitor = Instrumentation.ActivityMonitor(
            SignOutActivity::class.java.canonicalName, null, false
        )
        InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertThat(it.isFinishing).isTrue()
            }
            // ResultActivityが起動したか確認
            InstrumentationRegistry.getInstrumentation().waitForMonitorWithTimeout(monitor, 1000L)
            assertThat(monitor.hits).isEqualTo(1)
        }
    }

    @Test
    fun showError_EMAIL_MISMATCH_ERROR() {
        // resultCodeはOK/CANCEL以外ならなんでもOK
        val testRegistry = TestRegistry(Activity.RESULT_FIRST_USER, ErrorCodes.EMAIL_MISMATCH_ERROR)
        // 追加のモジュールにセット
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }
            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            assertThat(shadowAlertDialog).isNotNull

            assertThat(shadowAlertDialog.message).startsWith(getString(R.string.error_email_mismatch))
            assertThat(shadowAlertDialog.message).endsWith(
                getString(
                    R.string.label_error_code,
                    ErrorCodes.EMAIL_MISMATCH_ERROR
                )
            )

            ShadowView.clickOn(alert.getButton(AlertDialog.BUTTON_NEUTRAL))

            assertThat(alert.isShowing).isFalse()
        }
    }

    @Test
    fun showError_ERROR_GENERIC_IDP_RECOVERABLE_ERROR() {
        // resultCodeはOK/CANCEL以外ならなんでもOK
        val testRegistry =
            TestRegistry(Activity.RESULT_FIRST_USER, ErrorCodes.ERROR_GENERIC_IDP_RECOVERABLE_ERROR)
        // 追加のモジュールにセット
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }
            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            assertThat(shadowAlertDialog).isNotNull

            assertThat(shadowAlertDialog.message).startsWith(getString(R.string.error_id_provider))
            assertThat(shadowAlertDialog.message).endsWith(
                getString(
                    R.string.label_error_code,
                    ErrorCodes.ERROR_GENERIC_IDP_RECOVERABLE_ERROR
                )
            )
        }
    }

    @Test
    fun showError_PROVIDER_ERROR() {
        // resultCodeはOK/CANCEL以外ならなんでもOK
        val testRegistry = TestRegistry(Activity.RESULT_FIRST_USER, ErrorCodes.PROVIDER_ERROR)
        // 追加のモジュールにセット
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }
            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            assertThat(shadowAlertDialog).isNotNull

            assertThat(shadowAlertDialog.message).startsWith(getString(R.string.error_id_provider))
            assertThat(shadowAlertDialog.message).endsWith(
                getString(
                    R.string.label_error_code,
                    ErrorCodes.PROVIDER_ERROR
                )
            )
        }
    }

    @Test
    fun showError_ERROR_USER_DISABLED() {
        // resultCodeはOK/CANCEL以外ならなんでもOK
        val testRegistry = TestRegistry(Activity.RESULT_FIRST_USER, ErrorCodes.ERROR_USER_DISABLED)
        // 追加のモジュールにセット
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }
            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            assertThat(shadowAlertDialog).isNotNull

            assertThat(shadowAlertDialog.message).startsWith(getString(R.string.error_user_disabled))
            assertThat(shadowAlertDialog.message).endsWith(
                getString(
                    R.string.label_error_code,
                    ErrorCodes.ERROR_USER_DISABLED
                )
            )
        }
    }

    @Test
    fun showError_NO_NETWORK() {
        // resultCodeはOK/CANCEL以外ならなんでもOK
        val testRegistry = TestRegistry(Activity.RESULT_FIRST_USER, ErrorCodes.NO_NETWORK)
        // 追加のモジュールにセット
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }
            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            assertThat(shadowAlertDialog).isNotNull

            assertThat(shadowAlertDialog.message).startsWith(getString(R.string.error_no_network))
            assertThat(shadowAlertDialog.message).endsWith(
                getString(
                    R.string.label_error_code,
                    ErrorCodes.NO_NETWORK
                )
            )
        }
    }

    @Test
    fun showError_PLAY_SERVICES_UPDATE_CANCELLED() {
        // resultCodeはOK/CANCEL以外ならなんでもOK
        val testRegistry =
            TestRegistry(Activity.RESULT_FIRST_USER, ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED)
        // 追加のモジュールにセット
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }
            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            assertThat(shadowAlertDialog).isNotNull

            assertThat(shadowAlertDialog.message).startsWith(getString(R.string.error_service_update_canceled))
            assertThat(shadowAlertDialog.message).endsWith(
                getString(
                    R.string.label_error_code,
                    ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED
                )
            )
        }
    }

    @Test
    fun showError_UNKNOWN() {
        // resultCodeはOK/CANCEL以外ならなんでもOK
        val testRegistry = TestRegistry(Activity.RESULT_FIRST_USER, ErrorCodes.UNKNOWN_ERROR)
        // 追加のモジュールにセット
        val scopedModule = module {
            scope<SignInActivity> {
                scoped(override = true) { testRegistry as ActivityResultRegistry }
            }
        }
        loadKoinModules(scopedModule)

        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity {
            }
            // 認証画面を起動
            onView(withText(R.string.label_sign_in)).perform(click())

            // resultがすぐにディスパッチされている

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            assertThat(shadowAlertDialog).isNotNull

            assertThat(shadowAlertDialog.message).startsWith(getString(R.string.error_unknown))
            assertThat(shadowAlertDialog.message).endsWith(
                getString(
                    R.string.label_error_code,
                    ErrorCodes.UNKNOWN_ERROR
                )
            )
        }
    }
}