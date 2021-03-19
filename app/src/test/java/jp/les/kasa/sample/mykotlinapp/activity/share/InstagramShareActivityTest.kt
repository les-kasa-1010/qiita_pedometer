package jp.les.kasa.sample.mykotlinapp.activity.share

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import jp.les.kasa.sample.mykotlinapp.R
import jp.les.kasa.sample.mykotlinapp.TestObserver
import jp.les.kasa.sample.mykotlinapp.data.LEVEL
import jp.les.kasa.sample.mykotlinapp.data.StepCountLog
import jp.les.kasa.sample.mykotlinapp.data.WEATHER
import jp.les.kasa.sample.mykotlinapp.di.mockModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.test.AutoCloseKoinTest

@RunWith(AndroidJUnit4::class)
class InstagramShareActivityTest : AutoCloseKoinTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    var grantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    lateinit var activity: InstagramShareActivity

    val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }

    @Before
    fun setUp() {
        loadKoinModules(mockModule)
    }

    @Test
    fun layout() {
        val intent = Intent(context, InstagramShareActivity::class.java).apply {
            putExtra(
                InstagramShareActivity.KEY_STEP_COUNT_DATA,
                StepCountLog("2019/06/22", 456, LEVEL.BAD, WEATHER.HOT)
            )
        }
        ActivityScenario.launch<InstagramShareActivity>(intent).use {
            onView(withText("2019/06/22")).check(matches(isDisplayed()))
            onView(withText("456")).check(matches(isDisplayed()))
            onView(withText("歩")).check(matches(isDisplayed()))
            onView(withText(R.string.app_copyright)).check(matches(isDisplayed()))
            onView(withText(R.string.label_post)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun post() {
        val intent = Intent(context, InstagramShareActivity::class.java).apply {
            putExtra(
                InstagramShareActivity.KEY_STEP_COUNT_DATA,
                StepCountLog("2019/06/22", 456, LEVEL.BAD, WEATHER.HOT)
            )
        }

        ActivityScenario.launch<InstagramShareActivity>(intent).use { scenario ->
            scenario.onActivity {
                activity = it
            }

            // テスト用の監視
            val testObserver = TestObserver<Uri>(1)
            activity.viewModel.savedBitmapUri.observeForever(testObserver)

            onView(withText(R.string.label_post)).perform(click())

            testObserver.await(10)

            assertThat(activity.viewModel.savedBitmapUri.value).isNotNull()

            activity.viewModel.savedBitmapUri.removeObserver(testObserver)
        }
    }
}