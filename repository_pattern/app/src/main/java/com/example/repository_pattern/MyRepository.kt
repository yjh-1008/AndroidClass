package com.example.repository_pattern

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyRepository(context: Context) {
    private val baseURL = "https://api.github.com/"
    private val api = retrofitInit(baseURL)
    private val myDao = MyDatabase.getDatabase(context).myDao

    val repos = myDao.getAll() // LiveData<List<ReposD>>

    suspend fun refreshData() {
        withContext(Dispatchers.IO) {
            val repos = api.contributors("jyheo","android_guides")
            // convert Repo to RepoD
            val repoCs = repos.map {
                RepoD(it.login, it.contributions)
            }
            myDao.insertAll(repoCs)
        }
    }
}