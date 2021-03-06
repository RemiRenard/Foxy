package com.app.foxy.friends

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.app.data.model.User
import com.app.data.network.ExceptionHandler
import com.app.data.network.apiResponse.FriendsRequestsResponse
import com.app.domain.services.friend.IFriendService
import com.app.foxy.profile.dagger.ProfileScope
import io.reactivex.Observer
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Friends presenter
 */
@ProfileScope
class FriendsPresenter(private val mFriendService: IFriendService, private val mContext: Context) : IFriendsPresenter {

    private var mView: IFriendsView? = null
    private var mCompositeDisposable: CompositeDisposable? = null

    override fun attachView(view: IFriendsView) {
        mCompositeDisposable = CompositeDisposable()
        mView = view
    }

    override fun detachView() {
        mCompositeDisposable?.clear()
        mView = null
    }

    override fun getFriends() {
        //mView?.showProgressBar()
        mFriendService.getFriends().subscribe(object : Observer<List<User>> {
            override fun onSubscribe(@NonNull d: Disposable) {
                mCompositeDisposable?.add(d)
            }

            override fun onNext(@NonNull friends: List<User>) {
                mView?.displayFriends(friends)
            }

            override fun onError(@NonNull e: Throwable) {
                Log.i(javaClass.simpleName, ExceptionHandler.getMessage(e, mContext))
                mView?.hideProgressBar()
            }

            override fun onComplete() {
                mView?.hideProgressBar()
            }
        })
    }

    override fun getFriendsRequests() {
        mFriendService.getFriendsRequests().subscribe(object : Observer<List<FriendsRequestsResponse>> {
            override fun onSubscribe(@NonNull d: Disposable) {
                mCompositeDisposable?.add(d)
            }

            override fun onNext(@NonNull friendsRequests: List<FriendsRequestsResponse>) {
                mView?.displayFriendsRequests(friendsRequests)
            }

            override fun onError(@NonNull e: Throwable) {
                Log.i(javaClass.simpleName, ExceptionHandler.getMessage(e, mContext))
            }

            override fun onComplete() {
                // Do nothing
            }
        })
    }
}