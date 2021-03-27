package jp.les.kasa.sample.mykotlinapp.activity.signin

import android.app.Application
import android.app.Instrumentation
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.les.kasa.sample.mykotlinapp.R
import jp.les.kasa.sample.mykotlinapp.ShadowAlertController
import jp.les.kasa.sample.mykotlinapp.ShadowAlertDialog
import jp.les.kasa.sample.mykotlinapp.di.mockModule
import jp.les.kasa.sample.mykotlinapp.shadowOfAlert
import jp.les.kasa.sample.mykotlinapp.utils.AuthProviderI
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowView

@RunWith(AndroidJUnit4::class)
@Config(
    qualifiers = "xlarge-port",
    shadows = [ShadowAlertDialog::class, ShadowAlertController::class]
)
class SignOutActivityTest : AutoCloseKoinTest() {

    lateinit var activity: SignOutActivity

    private val authProvider: AuthProviderI by inject()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private fun getString(resId: Int) = context.applicationContext.getString(resId)

    @Before
    fun setUp() {
        loadKoinModules(mockModule)
    }

    /**
     *   起動直後の表示のテスト<br>
     */
    @Test
    fun layout() {
        ActivityScenario.launch(SignOutActivity::class.java).use { scenario ->
            scenario.onActivity { }
            // サインイン中
            onView(withText(R.string.text_sign_in_now))
                .check(matches(isDisplayed()))
            // クラウドアイコン
            onView(withId(R.id.imageCloudDone))
//            .check(matches(withDrawable(R.drawable.ic_cloud_upload_24dp))) // Tintカラー付けていると使えない
                .check(matches(isDisplayed()))
            // ユーザー名
            onView(withText("ユーザー名")).check(matches(isDisplayed()))
            // メールアドレス
            onView(withText("foo@bar.com")).check(matches(isDisplayed()))
            // 文言
            onView(withText(R.string.text_sign_out_description))
                .check(matches(isDisplayed()))
            // ログアウトボタン
            onView(withText(R.string.label_sign_out))
                .check(matches(isDisplayed()))
            // ローカルデータ変換ボタン
            onView(withText(R.string.label_convert_to_local))
                .check(matches(isDisplayed()))
            // アカウント削除ボタン
            onView(withText(R.string.label_account_delete))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun signOut() {
        ActivityScenario.launch(SignOutActivity::class.java).use { scenario ->
            scenario.onActivity {
                activity = it
            }

            // ResultActivityの起動を監視
            val monitor = Instrumentation.ActivityMonitor(
                SignInActivity::class.java.canonicalName, null, false
            )
            InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

            // ログアウトボタン
            onView(withText(R.string.label_sign_out))
                .perform(click())

            Assertions.assertThat(activity.isFinishing).isTrue()

            // ResultActivityが起動したか確認
            InstrumentationRegistry.getInstrumentation()
                .waitForMonitorWithTimeout(monitor, 1000L)
            Assertions.assertThat(monitor.hits).isEqualTo(1)
        }
    }

    @Test
    fun deleteAccount_cancel() {
        ActivityScenario.launch(SignOutActivity::class.java).use { scenario ->
            scenario.onActivity {}

            onView(withId(R.id.signOutScroll)).perform(swipeUp())
            // アカウント削除ボタン
            onView(withId(R.id.buttonAccountDelete))
                .perform(scrollTo(), click())

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            Assertions.assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            Assertions.assertThat(shadowAlertDialog).isNotNull

            Assertions.assertThat(shadowAlertDialog.message)
                .isEqualTo(getString(R.string.confirm_account_delete_1))

            ShadowView.clickOn(alert.getButton(AlertDialog.BUTTON_NEGATIVE))

            Assertions.assertThat(alert.isShowing).isFalse()

            val alert2 = ShadowAlertDialog.latestAlertDialog!!
            Assertions.assertThat(alert2.isShowing).isTrue()
            val shadowAlertDialog2 = shadowOfAlert(alert2)
            Assertions.assertThat(shadowAlertDialog2).isNotNull

            Assertions.assertThat(shadowAlertDialog2.message)
                .isEqualTo(getString(R.string.confirm_account_delete_2))

            ShadowView.clickOn(alert2.getButton(AlertDialog.BUTTON_NEGATIVE))

            Assertions.assertThat(alert2.isShowing).isFalse()

        }
    }

    @Test
    fun deleteAccount_data_converted() {
        // ResultActivityの起動を監視
        val monitor = Instrumentation.ActivityMonitor(
            SignInActivity::class.java.canonicalName, null, false
        )
        InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

        ActivityScenario.launch(SignOutActivity::class.java).use { scenario ->
            scenario.onActivity {
                activity = it
            }

            onView(withId(R.id.signOutScroll)).perform(swipeUp())
            // アカウント削除ボタン
            onView(withId(R.id.buttonAccountDelete))
                .perform(scrollTo(), click())

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            Assertions.assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            Assertions.assertThat(shadowAlertDialog).isNotNull

            Assertions.assertThat(shadowAlertDialog.message)
                .isEqualTo(getString(R.string.confirm_account_delete_1))

            ShadowView.clickOn(alert.getButton(AlertDialog.BUTTON_POSITIVE))

            Assertions.assertThat(alert.isShowing).isFalse()

            // コンバートしましたに「はい」と答えたので、アカウント削除をし自分は終了した
            Assertions.assertThat(activity.isFinishing).isEqualTo(true)

            // ResultActivityが起動したか確認
            InstrumentationRegistry.getInstrumentation()
                .waitForMonitorWithTimeout(monitor, 1000L)
            Assertions.assertThat(monitor.hits).isEqualTo(1)
        }
    }

    @Test
    fun deleteAccount_anyway() {
        // ResultActivityの起動を監視
        val monitor = Instrumentation.ActivityMonitor(
            SignInActivity::class.java.canonicalName, null, false
        )
        InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

        ActivityScenario.launch(SignOutActivity::class.java).use { scenario ->
            scenario.onActivity {
                activity = it
            }

            onView(withId(R.id.signOutScroll)).perform(swipeUp())
            // アカウント削除ボタン
            onView(withId(R.id.buttonAccountDelete))
                .perform(scrollTo(), click())

            // RobolectricはEspressoがAlertDialogのビューを拾えない・・・
            val alert = ShadowAlertDialog.latestAlertDialog!!
            Assertions.assertThat(alert.isShowing).isTrue()
            val shadowAlertDialog = shadowOfAlert(alert)
            Assertions.assertThat(shadowAlertDialog).isNotNull

            Assertions.assertThat(shadowAlertDialog.message)
                .isEqualTo(getString(R.string.confirm_account_delete_1))

            ShadowView.clickOn(alert.getButton(AlertDialog.BUTTON_NEGATIVE))

            Assertions.assertThat(alert.isShowing).isFalse()

            val alert2 = ShadowAlertDialog.latestAlertDialog!!
            Assertions.assertThat(alert2.isShowing).isTrue()
            val shadowAlertDialog2 = shadowOfAlert(alert2)
            Assertions.assertThat(shadowAlertDialog2).isNotNull

            Assertions.assertThat(shadowAlertDialog2.message)
                .isEqualTo(getString(R.string.confirm_account_delete_2))

            ShadowView.clickOn(alert2.getButton(AlertDialog.BUTTON_POSITIVE))

            Assertions.assertThat(alert2.isShowing).isFalse()

            // アカウント削除をし自分は終了した
            Assertions.assertThat(activity.isFinishing).isEqualTo(true)

            // ResultActivityが起動したか確認
            InstrumentationRegistry.getInstrumentation()
                .waitForMonitorWithTimeout(monitor, 1000L)
            Assertions.assertThat(monitor.hits).isEqualTo(1)
        }
    }
}