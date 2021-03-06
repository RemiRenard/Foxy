package com.app.foxy.notification.adapter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.app.data.Constants
import com.app.data.model.Notification
import com.app.foxy.R
import com.app.foxy.eventBus.DisplaySnackBarEvent
import com.app.foxy.eventBus.NotificationClickedEvent
import com.app.foxy.eventBus.RequestWriteStoragePermEvent
import com.app.foxy.eventBus.WriteStoragePermResultEvent
import com.app.foxy.friends.requests.FriendsRequestsActivity
import kotlinx.android.synthetic.main.item_notification.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*


/**
 * Adapter used to display notifications.
 */
class NotificationAdapter(val mPresenter: INotificationAdapterPresenter) : RecyclerView.Adapter<NotificationAdapter.ItemViewHolder>(), INotificationAdapterView {

    private var mContext: Context? = null
    private var mNotifications: MutableList<Notification> = ArrayList()
    private val mDatePattern: String = "HH:mm"
    private var mPositionPlaying: Int = -1
    private var mPositionLoading: Int = -1
    private var mPreviousPositionLoading: Int = -1
    private var mPreviousPositionPlaying: Int = -1
    private var mIsPlaying: Boolean = false
    private var mTmpSoundToDl: Notification? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        mContext = parent.context
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        return ItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_notification, parent, false))
    }

    fun onResume() {
        mPresenter.attachView(this)
    }

    fun onPause() {
        mPresenter.detachView()
    }

    override fun itemPlayingCompleted() {
        mPositionPlaying = -1
        mIsPlaying = false
        notifyDataSetChanged()
    }

    override fun itemLoadingCompleted() {
        mPositionPlaying = mPositionLoading
        mPositionLoading = -1
        notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWriteStoragePermResultEvent(event: WriteStoragePermResultEvent) {
        if (event.isGranted) {
            if (mTmpSoundToDl != null) {
                displayInternetMessage()
                mPresenter.downloadSound(mTmpSoundToDl?.song!!, mTmpSoundToDl?.id!!)
                mTmpSoundToDl = null
            }
        } else {
            EventBus.getDefault().post(DisplaySnackBarEvent())
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (mPositionLoading == position) {
            holder.itemView.item_notification_audio_button.visibility = View.INVISIBLE
            holder.itemView.item_notification_progress_bar.visibility = View.VISIBLE
        } else if (mPreviousPositionLoading == position || mPreviousPositionPlaying == position) {
            mPreviousPositionPlaying = -1
            mPreviousPositionLoading = -1
            holder.itemView.item_notification_audio_button.visibility = View.VISIBLE
            holder.itemView.item_notification_audio_button.setImageResource(R.drawable.ic_play_white)
            holder.itemView.item_notification_progress_bar.visibility = View.INVISIBLE

            // Mark notification as read.
            holder.itemView.item_notification_audio_button.setImageResource(R.drawable.ic_play_white)
            EventBus.getDefault().post(NotificationClickedEvent(mNotifications[position].id!!))
            mNotifications[position].isRead = true

        } else if (mPositionPlaying == position) {
            holder.itemView.item_notification_audio_button.visibility = View.VISIBLE
            holder.itemView.item_notification_audio_button.setImageResource(R.drawable.ic_stop_white)
            holder.itemView.item_notification_progress_bar.visibility = View.INVISIBLE
        } else {
            holder.itemView.item_notification_audio_button.visibility = View.VISIBLE
            holder.itemView.item_notification_audio_button.setImageResource(R.drawable.ic_play_white)
            holder.itemView.item_notification_progress_bar.visibility = View.INVISIBLE
        }
        holder.itemView?.item_notification_message?.text = mNotifications[position].message
        holder.itemView?.item_notification_user_time?.text = mContext!!.getString(
                R.string.user_time,
                mNotifications[position].userSource?.username,
                SimpleDateFormat(mDatePattern, Locale.US).format(mNotifications[position].createdAt))
        holder.itemView?.item_notification_layout?.setOnClickListener {
            if (!mIsPlaying) {
                // Mark notification as read.
                EventBus.getDefault().post(NotificationClickedEvent(mNotifications[position].id!!))
                mNotifications[position].isRead = true
                notifyDataSetChanged()
            }
            // Manage the type of notification
            if (mNotifications[position].type.equals("friendRequest")) {
                mContext!!.startActivity(FriendsRequestsActivity.getStartingIntent(mContext!!)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        holder.itemView?.item_notification_layout?.alpha = if (mNotifications[position].isRead) 0.8F else 1F
        if (TextUtils.equals(mNotifications[position].song, Constants.DEFAULT_SONG_LOCATION)
                || TextUtils.equals(mNotifications[position].song, "")) {
            holder.itemView?.item_notification_audio_button?.visibility = View.INVISIBLE
            holder.itemView?.item_notification_download_button?.visibility = View.INVISIBLE
        } else {
            holder.itemView?.item_notification_audio_button?.visibility = View.VISIBLE
            holder.itemView?.item_notification_download_button?.visibility = View.VISIBLE
        }
        // When the use click on "play"
        holder.itemView?.item_notification_audio_button?.setOnClickListener {
            if (!mIsPlaying && position != mPositionLoading && position != mPositionPlaying) {
                mPositionLoading = holder.adapterPosition
                mIsPlaying = true
                mPresenter.playSong(mNotifications[position].song!!)
            } else if (mIsPlaying && position != mPositionPlaying && position != mPositionPlaying) {
                mPresenter.stopSong()
                mPreviousPositionLoading = mPositionLoading
                mPreviousPositionPlaying = mPositionPlaying
                mPositionLoading = holder.adapterPosition
                mIsPlaying = true
                mPresenter.playSong(mNotifications[position].song!!)
            } else if (mIsPlaying) {
                mPositionPlaying = -1
                mIsPlaying = false
                mPresenter.stopSong()
            }
            notifyDataSetChanged()
        }
        holder.itemView?.item_notification_download_button?.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(mContext!!,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                displayInternetMessage()
                mPresenter.downloadSound(mNotifications[position].song!!, mNotifications[position].id!!)
            } else {
                mTmpSoundToDl = mNotifications[position]
                EventBus.getDefault().post(RequestWriteStoragePermEvent())
            }
        }
    }

    private fun displayInternetMessage() {
        val cm = mContext!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm.activeNetworkInfo != null && cm.activeNetworkInfo.isConnectedOrConnecting) {
            Toast.makeText(mContext, R.string.Downloading, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(mContext, R.string.download_add_to_queue, Toast.LENGTH_LONG).show()
        }
    }

    override fun getItemCount(): Int = mNotifications.size

    /**
     * Put data in the recycler view.
     */
    fun setData(notifications: List<Notification>) {
        mNotifications.clear()
        mNotifications.addAll(notifications)
        notifyDataSetChanged()
    }

    /**
     * View holder of the notification item.
     */
    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
