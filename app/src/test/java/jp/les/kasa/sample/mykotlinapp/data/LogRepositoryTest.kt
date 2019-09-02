package jp.les.kasa.sample.mykotlinapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogRepositoryTest {

    private lateinit var logDao: LogDao
    private lateinit var repository: LogRepository

    @Before
    fun setUp() {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            LogRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        logDao = database.logDao()
        repository = LogRepository(logDao)
    }

    @Test
    fun getAllLogs() {
        runBlocking {
            repository.insert(StepCountLog("2019/08/30", 12345))
            repository.insert(StepCountLog("2019/08/31", 12345, LEVEL.GOOD, WEATHER.CLOUD))
        }

        val items = repository.allLogs
        items.observeForever {
            assertThat(items.value).isNotEmpty()
            assertThat(items.value!!.size).isEqualTo(2)
            assertThat(items.value!![0]).isEqualToComparingFieldByField(
                StepCountLog("2019/08/30", 12345)
            )
            assertThat(items.value!![1]).isEqualToComparingFieldByField(
                StepCountLog("2019/08/31", 12345, LEVEL.GOOD, WEATHER.CLOUD)
            )
        }
    }

    @Test
    fun insert() {
        runBlocking {
            repository.insert(StepCountLog("2019/08/30", 12345, LEVEL.GOOD, WEATHER.CLOUD))
        }

        val item = logDao.getLog("2019/08/30")
        assertThat(item).isEqualToComparingFieldByField(
            StepCountLog("2019/08/30", 12345, LEVEL.GOOD, WEATHER.CLOUD)
        )
    }

    @Test
    fun update() {
        runBlocking {
            repository.insert(StepCountLog("2019/08/30", 12345, LEVEL.GOOD, WEATHER.CLOUD))
            repository.update(StepCountLog("2019/08/30", 12344))
        }

        val item = logDao.getLog("2019/08/30")
        assertThat(item).isEqualToComparingFieldByField(
            StepCountLog("2019/08/30", 12344, LEVEL.NORMAL, WEATHER.FINE)
        )
    }

    @Test
    fun delete() {
        runBlocking {
            repository.insert(StepCountLog("2019/08/30", 12345))
            repository.delete(StepCountLog("2019/08/30", 12345))
        }

        val item = logDao.getLog("2019/08/30")
        assertThat(item).isNull()
    }

    @Test
    fun deleteAll() {
        runBlocking {
            repository.insert(StepCountLog("2019/08/30", 12345))
            repository.insert(StepCountLog("2019/08/31", 12345, LEVEL.GOOD, WEATHER.CLOUD))
            repository.deleteAll()
        }

        val items = repository.allLogs
        items.observeForever() {
            assertThat(items.value).isEmpty()
        }
    }
}