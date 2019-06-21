package jp.les.kasa.sample.mykotlinapp

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import jp.les.kasa.sample.mykotlinapp.activity.logitem.LogItemActivity
import jp.les.kasa.sample.mykotlinapp.activity.logitem.LogItemActivity.Companion.EXTRA_KEY_DATA
import jp.les.kasa.sample.mykotlinapp.data.LEVEL
import jp.les.kasa.sample.mykotlinapp.data.StepCountLog
import jp.les.kasa.sample.mykotlinapp.data.WEATHER
import jp.les.kasa.sample.mykotlinapp.espresso.atPositionOnView
import jp.les.kasa.sample.mykotlinapp.espresso.withDrawable
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * @date 2019/06/05
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTestI {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext!!

    @Test
    fun addRecordMenuIcon() {
        onView(
            Matchers.allOf(withId(R.id.add_record), withContentDescription("記録を追加"))
        ).check(matches(isDisplayed()))
    }

    @Test
    fun addRecordMenu() {
        // ResultActivityの起動を監視
        val monitor = Instrumentation.ActivityMonitor(
            LogItemActivity::class.java.canonicalName, null, false
        )
        getInstrumentation().addMonitor(monitor)

        // 追加メニューをクリック
        onView(
            Matchers.allOf(withId(R.id.add_record), withContentDescription("記録を追加"))
        ).perform(click())

        // ResultActivityが起動したか確認
        val resultActivity = getInstrumentation().waitForMonitorWithTimeout(monitor, 1000L)
        assertThat(monitor.hits).isEqualTo(1)
        assertThat(resultActivity).isNotNull()

        // 端末戻るボタンで終了を確認
        Espresso.pressBack()
        assertThat(resultActivity.isFinishing).isTrue()
    }

    @Test
    fun addRecordList() {
        // ViewModelのリストに直接追加
        val mainActivity = activityRule.activity

        mainActivity.runOnUiThread {
            mainActivity.viewModel.addStepCount(StepCountLog("2019/06/13", 12345, LEVEL.GOOD))
            mainActivity.viewModel.addStepCount(StepCountLog("2019/06/19", 666, LEVEL.BAD, WEATHER.RAIN))
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // リストの表示確認
        var index = 0

        onView(withId(R.id.log_list))
            // @formatter:off
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(index))
            .check(matches(atPositionOnView(index, withText("12345"), R.id.stepTextView)))
            .check(matches(atPositionOnView(index, withText("2019/06/13"), R.id.dateTextView)))
            .check(matches(atPositionOnView(index,
                withDrawable(R.drawable.ic_sentiment_very_satisfied_pink_24dp), R.id.levelImageView)))
            .check(matches(atPositionOnView(index,
                        withDrawable(R.drawable.ic_wb_sunny_yellow_24dp),R.id.weatherImageView)))
            // @formatter:on
        index = 1
        onView(withId(R.id.log_list))
            // @formatter:off
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(index))
            .check(matches(atPositionOnView(index, withText("666"), R.id.stepTextView)))
            .check(matches(atPositionOnView(index, withText("2019/06/19"), R.id.dateTextView)))
            .check(matches(atPositionOnView(index,
                withDrawable(R.drawable.ic_sentiment_dissatisfied_black_24dp),R.id.levelImageView)))
            .check(matches(atPositionOnView(index,
                        withDrawable(R.drawable.ic_iconmonstr_umbrella_1),R.id.weatherImageView)))
        // @formatter:on
    }

    @Test
    fun onActivityResult() {
        val resultData = Intent().apply {
            putExtra(EXTRA_KEY_DATA, StepCountLog("2019/06/19", 666, LEVEL.BAD, WEATHER.SNOW))
        }

        val monitor = Instrumentation.ActivityMonitor(
            LogItemActivity::class.java.canonicalName, null, false
        )
        getInstrumentation().addMonitor(monitor)

        // 登録画面を起動
        onView(
            Matchers.allOf(withId(R.id.add_record), withContentDescription("記録を追加"))
        ).perform(click())

        val resultActivity = getInstrumentation().waitForMonitorWithTimeout(monitor, 500L)
        resultActivity.setResult(Activity.RESULT_OK, resultData)
        resultActivity.finish()

        // 反映を確認
        val index = 0

        onView(withId(R.id.log_list))
            // @formatter:off
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(index))
            .check(matches(atPositionOnView(index, withText("666"), R.id.stepTextView)))
            .check(matches(atPositionOnView(index, withText("2019/06/19"), R.id.dateTextView)))
            .check(matches(atPositionOnView(index,
                withDrawable(R.drawable.ic_sentiment_dissatisfied_black_24dp), R.id.levelImageView)))
            .check(matches(atPositionOnView(index,
                        withDrawable(R.drawable.ic_grain_gley_24dp),R.id.weatherImageView)))
            // @formatter:on

    }
}
