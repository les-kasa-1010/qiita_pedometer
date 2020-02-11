package jp.les.kasa.sample.mykotlinapp.activity.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import jp.les.kasa.sample.mykotlinapp.R
import jp.les.kasa.sample.mykotlinapp.activity.logitem.LogItemActivity
import jp.les.kasa.sample.mykotlinapp.activity.share.InstagramShareActivity
import jp.les.kasa.sample.mykotlinapp.activity.share.TwitterShareActivity
import jp.les.kasa.sample.mykotlinapp.data.ShareStatus
import jp.les.kasa.sample.mykotlinapp.data.StepCountLog
import jp.les.kasa.sample.mykotlinapp.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_LOGITEM = 100
        const val REQUEST_CODE_SHARE_TWITTER = 101

        const val RESULT_CODE_DELETE = 10
    }

    val viewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this
        binding.viewmodel = viewModel
        viewPager.adapter = MonthlyPagerAdapter(supportFragmentManager)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return when (it.itemId) {
                R.id.add_record -> {
                    val intent = Intent(this, LogItemActivity::class.java)
                    startActivityForResult(intent, REQUEST_CODE_LOGITEM)
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_CODE_LOGITEM -> {
                onStepCountLogChanged(resultCode, data)
                return
            }

            REQUEST_CODE_SHARE_TWITTER -> {
                // 続けてInstagramにも投稿する
                val intent = Intent(this, InstagramShareActivity::class.java).apply {
                    val log =
                        data!!.getSerializableExtra(LogItemActivity.EXTRA_KEY_DATA) as StepCountLog
                    putExtra(InstagramShareActivity.KEY_STEP_COUNT_DATA, log)
                }
                startActivity(intent)
                return
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onStepCountLogChanged(resultCode: Int, data: Intent?) {
        when (resultCode) {
            RESULT_OK -> {
                val log =
                    data!!.getSerializableExtra(LogItemActivity.EXTRA_KEY_DATA) as StepCountLog
                viewModel.addStepCount(log)
                val shareStatus =
                    data.getSerializableExtra(LogItemActivity.EXTRA_KEY_SHARE_STATUS) as ShareStatus
                if (shareStatus.doPost) {
                    // 共有フラグがONならDB登録完了後に投稿画面へ遷移する
                    if (shareStatus.postTwitter) {
                        val intent = Intent(this, TwitterShareActivity::class.java)
                        intent.putExtra(TwitterShareActivity.KEY_TEXT, log.getShareMessage())
                        if (shareStatus.postInstagram) {
                            intent.putExtra(InstagramShareActivity.KEY_STEP_COUNT_DATA, log)
                            // Instagramもチェックされていれば、戻った後で次に起動するため、結果を受け取る必要がある
                            startActivityForResult(intent, REQUEST_CODE_SHARE_TWITTER)
                        } else {
                            startActivity(intent)
                        }
                    } else if (shareStatus.postInstagram) {
                        val intent = Intent(this, InstagramShareActivity::class.java).apply {
                            putExtra(InstagramShareActivity.KEY_STEP_COUNT_DATA, log)
                        }
                        startActivity(intent)
                    }
                }
            }
            RESULT_CODE_DELETE -> {
                val log =
                    data!!.getSerializableExtra(LogItemActivity.EXTRA_KEY_DATA) as StepCountLog
                viewModel.deleteStepCount(log)
            }
        }
    }
}

class MonthlyPagerAdapter(fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    var items: List<String> = emptyList()

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Fragment = MonthlyPageFragment.newInstance(items[position])

    fun setList(list: List<String>) {
        items = list
    }

    override fun getItemPosition(`object`: Any): Int = PagerAdapter.POSITION_NONE

}
