package com.app.data

import android.app.Application
import com.app.data.database.DbOpenHelper
import com.app.data.network.NetworkService
import com.app.data.network.RetrofitHelper
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqlbrite2.SqlBrite
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Singleton which contain the context of the data layer.
 */
object Data {

    var database: BriteDatabase? = null
    var networkService: NetworkService? = null

    /**
     * Create the database and the network service.
     * @param application Application
     */
    fun init(application: Application) {
        createDatabase(application)
        createNetworkService()
    }

    /**
     * Create the network service with the RetrofitHelper class.
     */
    private fun createNetworkService() {
        networkService = RetrofitHelper.createNetworkService(
                RetrofitHelper.createRetrofitBuilder(
                        RetrofitHelper.createHttpClient(
                                RetrofitHelper.createInterceptor()
                        )
                )
        )
    }

    /**
     * Create the database.
     * @param application Application
     */
    private fun createDatabase(application: Application) {
        database = SqlBrite.Builder()
                .logger { message -> Timber.tag("Database").v(message) }
                .build().wrapDatabaseHelper(DbOpenHelper(application), Schedulers.io())
        database?.setLoggingEnabled(true)
    }
}
