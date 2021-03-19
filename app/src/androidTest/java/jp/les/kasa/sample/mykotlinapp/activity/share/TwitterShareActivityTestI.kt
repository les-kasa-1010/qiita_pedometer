package jp.les.kasa.sample.mykotlinapp.activity.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.les.kasa.sample.mykotlinapp.R
import jp.les.kasa.sample.mykotlinapp.activity.logitem.LogItemActivity
import jp.les.kasa.sample.mykotlinapp.data.LEVEL
import jp.les.kasa.sample.mykotlinapp.data.ShareStatus
import jp.les.kasa.sample.mykotlinapp.data.StepCountLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest

@RunWith(AndroidJUnit4::class)
class TwitterShareActivityTestI : AutoCloseKoinTest() {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }

    @Test
    fun layout() {
        val text = "2019/06/28 は 12335歩 歩きました。気分は上々。"
        val intent = Intent(context, TwitterShareActivity::class.java).apply {
            putExtra(
                TwitterShareActivity.KEY_TEXT,
                text
            )
        }
        ActivityScenario.launch<TwitterShareActivity>(intent).use {

            onView(withText(text)).check(matches(isDisplayed()))
            onView(withText(R.string.label_tweet))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun edit() {
        val text = "2019/06/28 は 12335歩 歩きました。気分は上々。"
        val intent = Intent(context, TwitterShareActivity::class.java).apply {
            putExtra(
                TwitterShareActivity.KEY_TEXT,
                text
            )
        }
        ActivityScenario.launch<TwitterShareActivity>(intent).use {

            onView(withId(R.id.editText_share_message)).perform(replaceText("テキスト変更"))
            onView(withText("テキスト変更")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun finishWithIntent() {
        val intent = Intent(context, TwitterShareActivity::class.java).apply {
            putExtra(
                LogItemActivity.EXTRA_KEY_DATA,
                StepCountLog("2019/06/13", 12345, LEVEL.GOOD)
            )
            putExtra(
                LogItemActivity.EXTRA_KEY_SHARE_STATUS,
                ShareStatus(true, false, true)
            )
        }

        ActivityScenario.launch<TwitterShareActivity>(intent).use { scenario ->
            scenario.onActivity {
                it.finish()
            }


            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
            val resultData = scenario.result.resultData
            assertThat(resultData).isNotNull()

            val extraData =
                resultData.getSerializableExtra(LogItemActivity.EXTRA_KEY_DATA) as StepCountLog
            assertThat(extraData).isNotNull()
            assertThat(extraData).usingRecursiveComparison()
                .isEqualTo(StepCountLog("2019/06/13", 12345, LEVEL.GOOD))

            val extraData2 =
                resultData.getSerializableExtra(LogItemActivity.EXTRA_KEY_SHARE_STATUS) as ShareStatus
            assertThat(extraData2).isNotNull()
            assertThat(extraData2).usingRecursiveComparison()
                .isEqualTo(ShareStatus(true, false, true))
        }
    }
}