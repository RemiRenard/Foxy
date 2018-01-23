package org.foxy.foxy.notification.add

import org.foxy.foxy.IPresenter
import java.io.File

/**
 * Interface of the Add notification presenter.
 */
interface IAddNotificationPresenter : IPresenter<IAddNotificationView> {

    fun saveTmpNotification(message: String, keyword: String, type: String, song: String, audioFile: File?)
}
