package jp.les.kasa.sample.mykotlinapp.activity.signin

import android.app.Application
import android.app.Instrumentation
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.firebase.ui.auth.ErrorCodes
import jp.les.kasa.sample.mykotlinapp.R
import jp.les.kasa.sample.mykotlinapp.di.MockAuthUIActivity
import jp.les.kasa.sample.mykotlinapp.di.TestAuthProvider
import jp.les.kasa.sample.mykotlinapp.di.testMockModule
import jp.les.kasa.sample.mykotlinapp.utils.AuthProviderI
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.core.StringStartsWith.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class SignInActivityTest : AutoCloseKoinTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    lateinit var activity: SignInActivity

    private val authProvider: AuthProviderI by inject()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private fun getString(resId: Int) = context.applicationContext.getString(resId)
    private fun getString(resId: Int, c: Int) = context.applicationContext.getString(resId, c)

    @Before
    fun setUp() {
        loadKoinModules(testMockModule)
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
    fun signIn() {
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

    /**
     *   ログイン中の場合にサインアウト画面がでるかのテスト
     */
    @Test
    fun moveToSignOut() {
        // モックを作成
        (authProvider as TestAuthProvider).mockFirebaseUser = true
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
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showError(ErrorCodes.EMAIL_MISMATCH_ERROR)
            }

//            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(startsWith(getString(R.string.error_email_mismacth))))
                .check(matches(isDisplayed()))
            onView(
                withText(
                    endsWith(
                        getString(
                            R.string.label_error_code,
                            ErrorCodes.EMAIL_MISMATCH_ERROR
                        )
                    )
                )
            )
                .check(matches(isDisplayed()))
            onView(withText(R.string.close))
                .check(matches(isDisplayed()))
                .perform(click())

            onView(withText(startsWith(getString(R.string.error_email_mismacth))))
                .check(doesNotExist())
        }
    }

    @Test
    fun showError_ERROR_GENERIC_IDP_RECOVERABLE_ERROR() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showError(ErrorCodes.ERROR_GENERIC_IDP_RECOVERABLE_ERROR)
            }
//            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(startsWith(getString(R.string.error_id_provider))))
                .check(matches(isDisplayed()))
            onView(
                withText(
                    endsWith(
                        getString(
                            R.string.label_error_code,
                            ErrorCodes.ERROR_GENERIC_IDP_RECOVERABLE_ERROR
                        )
                    )
                )
            )
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun showError_PROVIDER_ERROR() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showError(ErrorCodes.PROVIDER_ERROR)
            }
//            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(startsWith(getString(R.string.error_id_provider))))
                .check(matches(isDisplayed()))
            onView(
                withText(
                    endsWith(
                        getString(
                            R.string.label_error_code,
                            ErrorCodes.PROVIDER_ERROR
                        )
                    )
                )
            )
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun showError_ERROR_USER_DISABLED() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showError(ErrorCodes.ERROR_USER_DISABLED)
            }
//        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(startsWith(getString(R.string.error_user_disabled))))
                .check(matches(isDisplayed()))
            onView(
                withText(
                    endsWith(
                        getString(
                            R.string.label_error_code,
                            ErrorCodes.ERROR_USER_DISABLED
                        )
                    )
                )
            )
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun showError_NO_NETWORK() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showError(ErrorCodes.NO_NETWORK)
            }
//        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(startsWith(getString(R.string.error_no_netowork))))
                .check(matches(isDisplayed()))
            onView(
                withText(
                    endsWith(
                        getString(
                            R.string.label_error_code,
                            ErrorCodes.NO_NETWORK
                        )
                    )
                )
            )
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun showError_PLAY_SERVICES_UPDATE_CANCELLED() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showError(ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED)
            }
//        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(startsWith(getString(R.string.error_service_update_canceled))))
                .check(matches(isDisplayed()))
            onView(
                withText(
                    endsWith(
                        getString(
                            R.string.label_error_code,
                            ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED
                        )
                    )
                )
            )
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun showError_UNKNOWN() {
        ActivityScenario.launch(SignInActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showError(ErrorCodes.UNKNOWN_ERROR)
            }
//        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(startsWith(getString(R.string.error_unknown))))
                .check(matches(isDisplayed()))
            onView(
                withText(
                    endsWith(
                        getString(
                            R.string.label_error_code,
                            ErrorCodes.UNKNOWN_ERROR
                        )
                    )
                )
            )
                .check(matches(isDisplayed()))
        }
    }
}